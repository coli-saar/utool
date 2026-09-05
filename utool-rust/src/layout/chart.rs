use super::{
    EdgeKind, Layout, LayoutEdge, LayoutError, LayoutOptions, NodeBox, Point, Size, route_edge,
    tree_reachable,
};
use crate::{
    graph::{HncGraph, NodeId},
    solver::{Chart, LayoutChart},
};
use std::collections::{HashMap, HashSet};

#[derive(Clone)]
struct Fragment {
    root: NodeId,
    nodes: Vec<NodeId>,
    holes: Vec<NodeId>,
    incoming: Vec<usize>,
    outgoing: Vec<usize>,
    width: f32,
    height: f32,
}

#[derive(Clone)]
struct FragmentBox {
    fragments: Vec<usize>,
    children: Vec<(HashSet<usize>, FragmentBox)>,
    x: Vec<Option<f32>>,
    next_x: Vec<f32>,
    width: f32,
}

#[derive(Clone, Copy)]
struct Extent {
    left: f32,
    right: f32,
}

#[derive(Clone)]
struct Shape(Vec<Extent>);

struct ChartLayouter<'a> {
    graph: &'a HncGraph,
    chart: LayoutChart,
    sizes: HashMap<NodeId, Size>,
    options: LayoutOptions,
    fragment_of: Vec<usize>,
    fragments: Vec<Fragment>,
    state_fragments: Vec<Vec<usize>>,
    state_for_fragments: HashMap<Vec<usize>, usize>,
    local: HashMap<NodeId, Point>,
    relative_to_parent: HashMap<NodeId, f32>,
    levels: Vec<usize>,
    layer_count: usize,
    one_hole: HashSet<usize>,
    leaf_parent: HashMap<usize, NodeId>,
    light_edges: HashSet<usize>,
}

/// Lay out a solved graph with the fragment-box algorithm used by Java's
/// `DomGraphChartLayout`.
///
/// The chart determines fragment layers and recursive boxes. The input graph
/// must be the graph from which `chart` was constructed.
///
/// # Errors
///
/// Returns [`LayoutError::UnsolvableGraph`] if the chart has no solved forms, as
/// Java's `DomGraphChartLayout` does, or [`LayoutError::MissingNodeSize`] if any
/// graph node has no measured size.
pub fn layout_java_chart(
    chart: &Chart,
    measured_sizes: &[(NodeId, Size)],
    options: LayoutOptions,
) -> Result<Layout, LayoutError> {
    if chart.count_solutions() == num_bigint::BigUint::default() {
        return Err(LayoutError::UnsolvableGraph);
    }
    let graph = chart.graph();
    let sizes: HashMap<_, _> = measured_sizes.iter().copied().collect();
    for index in 0..graph.parsed().nodes().len() {
        let node = NodeId::from_index(index);
        if !sizes.contains_key(&node) {
            return Err(LayoutError::MissingNodeSize(node));
        }
    }

    Ok(ChartLayouter::new(graph, chart.layout_chart(), sizes, options).run())
}

impl<'a> ChartLayouter<'a> {
    fn new(
        graph: &'a HncGraph,
        chart: LayoutChart,
        sizes: HashMap<NodeId, Size>,
        options: LayoutOptions,
    ) -> Self {
        let mut fragment_of = vec![usize::MAX; graph.parsed().nodes().len()];
        let mut fragments = Vec::with_capacity(graph.roots().len());
        let mut local = HashMap::new();
        let mut relative_to_parent = HashMap::new();
        for (fragment, &root) in graph.roots().iter().enumerate() {
            let mut nodes = Vec::new();
            collect_fragment_nodes(graph, root, &mut nodes);
            for &node in &nodes {
                fragment_of[node.index()] = fragment;
            }
            let mut holes = Vec::new();
            collect_holes(graph, root, &mut holes);
            let (width, height) = layout_fragment_contour(
                graph,
                root,
                &sizes,
                options,
                &mut local,
                &mut relative_to_parent,
            );
            fragments.push(Fragment {
                root,
                nodes,
                holes,
                incoming: Vec::new(),
                outgoing: Vec::new(),
                width,
                height,
            });
        }
        for (edge, &(source, target)) in graph.parsed().dominance_edges().iter().enumerate() {
            let source_fragment = fragment_of[source.index()];
            let target_fragment = fragment_of[target.index()];
            fragments[source_fragment].outgoing.push(edge);
            fragments[target_fragment].incoming.push(edge);
        }
        let state_fragments = chart
            .states
            .iter()
            .map(|state| {
                let mut fragments = state
                    .fragments
                    .iter()
                    .map(|root| fragment_of[root.index()])
                    .collect::<Vec<_>>();
                fragments.sort_unstable();
                fragments.dedup();
                fragments
            })
            .collect::<Vec<_>>();
        let mut state_for_fragments = HashMap::new();
        for (state, fragments) in state_fragments.iter().enumerate() {
            state_for_fragments
                .entry(fragments.clone())
                .or_insert(state);
        }
        Self {
            graph,
            chart,
            sizes,
            options,
            fragment_of,
            levels: vec![0; fragments.len()],
            layer_count: 0,
            fragments,
            state_fragments,
            state_for_fragments,
            local,
            relative_to_parent,
            one_hole: HashSet::new(),
            leaf_parent: HashMap::new(),
            light_edges: HashSet::new(),
        }
    }

    fn run(mut self) -> Layout {
        self.classify_one_hole_fragments();
        self.compute_layers();
        let fragment_y = self.compute_fragment_y();
        let mut fragment_x = vec![0.0; self.fragments.len()];
        let mut x_offset = 0.0_f32;
        let mut right_border = 0.0_f32;

        let top_states = self.chart.top_states.clone();
        for state in top_states {
            let free = self.free_fragments(state);
            let fragments = self.state_fragments(state).to_vec();
            let fragment_box = self.make_fragment_box(fragments, &free);
            for &fragment in &fragment_box.fragments {
                if let Some(relative_x) = fragment_box.x[fragment] {
                    fragment_x[fragment] = relative_x + x_offset;
                    right_border =
                        right_border.max(fragment_x[fragment] + self.fragments[fragment].width);
                }
            }
            x_offset = right_border + self.options.fragment_x_gap;
        }

        let top_fragments: Vec<_> = (0..self.fragments.len())
            .filter(|fragment| {
                self.levels[*fragment] == 0 && !self.leaf_parent.contains_key(fragment)
            })
            .collect();
        if top_fragments.len() == 1 {
            let top = top_fragments[0];
            fragment_x[top] = right_border / 2.0 - self.fragments[top].width / 2.0;
            self.light_edges
                .extend(self.fragments[top].outgoing.iter().copied());
        }

        for (&leaf, &source_hole) in &self.leaf_parent {
            let parent = self.fragment_of[source_hole.index()];
            let parent_offset =
                node_center_x(self.fragments[parent].root, &self.local, &self.sizes);
            let leaf_offset = node_center_x(self.fragments[leaf].root, &self.local, &self.sizes);
            fragment_x[leaf] =
                fragment_x[parent] + self.relative_to_parent[&source_hole] + parent_offset
                    - leaf_offset;
        }

        self.finish(&fragment_x, &fragment_y)
    }

    fn classify_one_hole_fragments(&mut self) {
        for fragment in 0..self.fragments.len() {
            let holes = self.fragments[fragment].holes.clone();
            let child_width = if holes.len() == 1 {
                let outgoing = self.outgoing_from_node(holes[0]);
                (outgoing.len() == 1).then(|| {
                    let child = self.edge(outgoing[0]).1;
                    let child_fragment = self.fragment_of[child.index()];
                    if self.fragment_degree(child_fragment) == 1 {
                        self.fragments[child_fragment].width
                    } else {
                        self.sizes[&holes[0]].width
                    }
                })
            } else if holes.len() == 2 {
                let outgoing = self.outgoing_from_node(holes[0]);
                if outgoing.len() == 1 {
                    let child = self.edge(outgoing[0]).1;
                    let child_fragment = self.fragment_of[child.index()];
                    if self.fragment_degree(child_fragment) == 1 {
                        self.leaf_parent.insert(child_fragment, holes[0]);
                        Some(self.fragments[child_fragment].width)
                    } else {
                        None
                    }
                } else {
                    None
                }
            } else {
                None
            };
            if let Some(child_width) = child_width {
                self.one_hole.insert(fragment);
                self.fragments[fragment].width += child_width / 2.0 - 15.0;
            }
        }
    }

    fn compute_layers(&mut self) {
        let mut visited = HashSet::new();
        let top_states = self.chart.top_states.clone();
        for state in top_states {
            self.fill_layer(state, 0, &mut visited);
        }
        self.layer_count = self
            .levels
            .iter()
            .copied()
            .max()
            .map_or(0, |level| level + 1);
    }

    fn fill_layer(&mut self, state: usize, layer: usize, visited: &mut HashSet<usize>) {
        let chart_state = &self.chart.states[state];
        if chart_state.splits.is_empty() {
            for &root in &chart_state.fragments {
                let fragment = self.fragment_of[root.index()];
                self.levels[fragment] = self.levels[fragment].max(layer);
            }
            return;
        }

        let splits = chart_state
            .splits
            .iter()
            .map(|split| (split.root, split.dominators.clone(), split.children.clone()))
            .collect::<Vec<_>>();
        let mut recent = HashSet::new();
        let mut child_states = Vec::new();
        for (root, dominators, children) in splits {
            let root_fragment = self.fragment_of[root.index()];
            if visited.insert(root_fragment) {
                recent.insert(root_fragment);
                self.levels[root_fragment] = self.levels[root_fragment].max(layer);
                for dominator in dominators {
                    let fragment = self.fragment_of[dominator.index()];
                    recent.insert(fragment);
                    self.levels[fragment] = self.levels[fragment].max(layer);
                }
                child_states.extend(children);
            }
        }
        child_states.sort_unstable();
        child_states.dedup();
        for child in child_states {
            let remaining = self
                .state_fragments(child)
                .iter()
                .copied()
                .filter(|fragment| !recent.contains(fragment))
                .collect::<HashSet<_>>();
            for component in self.fragment_components(&remaining) {
                if let Some(component_state) = self.matching_state(&component) {
                    self.fill_layer(component_state, layer + 1, visited);
                } else {
                    for fragment in component {
                        self.levels[fragment] = self.levels[fragment].max(layer + 1);
                    }
                }
            }
        }
    }

    fn compute_fragment_y(&self) -> Vec<f32> {
        let mut result = vec![0.0; self.fragments.len()];
        let mut y = 0.0_f32;
        let mut accumulated_height = 0.0_f32;
        for layer in 0..self.layer_count {
            for (fragment, fragment_y) in result.iter_mut().enumerate() {
                if self.levels[fragment] == layer && !self.leaf_parent.contains_key(&fragment) {
                    *fragment_y = y;
                    accumulated_height = accumulated_height.max(self.fragments[fragment].height);
                }
            }
            let leaf_extent = self
                .leaf_parent
                .keys()
                .filter(|fragment| self.levels[**fragment] == layer + 1)
                .map(|fragment| self.fragments[*fragment].height)
                .fold(0.0, f32::max);
            y += accumulated_height + leaf_extent + self.options.fragment_y_gap;
        }
        for (&leaf, &source_hole) in &self.leaf_parent {
            let parent = self.fragment_of[source_hole.index()];
            result[leaf] = result[parent] + self.fragments[parent].height + 35.0;
        }
        result
    }

    fn free_fragments(&self, state: usize) -> HashSet<usize> {
        let chart_state = &self.chart.states[state];
        if chart_state.splits.is_empty() {
            return chart_state
                .fragments
                .iter()
                .map(|root| self.fragment_of[root.index()])
                .collect();
        }
        chart_state
            .splits
            .iter()
            .map(|split| split.root)
            .map(|root| self.fragment_of[root.index()])
            .collect()
    }

    fn state_fragments(&self, state: usize) -> &[usize] {
        &self.state_fragments[state]
    }

    fn matching_state(&self, fragments: &[usize]) -> Option<usize> {
        self.state_for_fragments.get(fragments).copied()
    }

    fn make_fragment_box(&self, fragments: Vec<usize>, free: &HashSet<usize>) -> FragmentBox {
        let mut result = FragmentBox::new(fragments, self.fragments.len(), self.layer_count);

        if self.is_forest(&result.fragments) {
            self.tree_layout_box(&mut result);
            return result;
        }

        let remaining = result
            .fragments
            .iter()
            .copied()
            .filter(|fragment| !free.contains(fragment))
            .collect::<HashSet<_>>();
        for component in self.fragment_components(&remaining) {
            let matching_state = self.matching_state(&component);
            let child_free = matching_state.map_or_else(
                || component.iter().copied().collect(),
                |state| self.free_fragments(state),
            );
            let child_box = self.make_fragment_box(component, &child_free);
            result.children.push((child_free, child_box));
        }

        if result.fragments.len() == 1 {
            result.x[result.fragments[0]] = Some(0.0);
            result.width = self.fragments[result.fragments[0]].width;
            return result;
        }

        let mut candidates = result
            .fragments
            .iter()
            .copied()
            .filter(|fragment| {
                (self.fragments[*fragment].incoming.len() == 1 || free.contains(fragment))
                    && (self.fragments[*fragment].outgoing.is_empty()
                        || self.one_hole.contains(fragment))
            })
            .collect::<Vec<_>>();
        if candidates.is_empty() {
            candidates.clone_from(&result.fragments);
        }
        candidates.sort_unstable();

        let mut best_cost = usize::MAX;
        let mut best = result.clone();
        for candidate in candidates {
            let mut trial = result.clone();
            trial.clear();
            let mut visited = HashSet::new();
            let cost = self.box_dfs(&mut trial, 0.0, &mut visited, candidate, None);
            if cost < best_cost {
                best_cost = cost;
                best = trial;
            }
        }
        best
    }

    fn is_forest(&self, fragments: &[usize]) -> bool {
        fragments.iter().all(|fragment| {
            self.fragments[*fragment]
                .incoming
                .iter()
                .filter(|edge| self.edge(**edge).1 == self.fragments[*fragment].root)
                .count()
                <= 1
        })
    }

    fn tree_layout_box(&self, result: &mut FragmentBox) {
        let allowed: HashSet<_> = result.fragments.iter().copied().collect();
        let root = result.fragments.iter().copied().find(|fragment| {
            self.fragments[*fragment]
                .incoming
                .iter()
                .all(|edge| !allowed.contains(&self.fragment_of[self.edge(*edge).0.index()]))
        });
        if let Some(root) = root {
            let mut visited = HashSet::new();
            self.tree_layout_dfs(result, root, &allowed, &mut visited, 0.0);
        }
    }

    fn tree_layout_dfs(
        &self,
        result: &mut FragmentBox,
        current: usize,
        allowed: &HashSet<usize>,
        visited: &mut HashSet<usize>,
        start: f32,
    ) -> f32 {
        if !visited.insert(current) {
            return start;
        }
        let mut children = self.child_fragments(current, allowed, visited);
        children.sort_unstable();
        let mut right = start + self.fragments[current].width;
        let mut child_start = start;
        for child in children {
            right = self.tree_layout_dfs(result, child, allowed, visited, child_start);
            child_start = right + self.options.fragment_x_gap;
        }
        let layer = self.levels[current];
        let x = (f32::midpoint(start, right) - self.fragments[current].width / 2.0)
            .max(result.next_x[layer]);
        let border = x + self.fragments[current].width;
        result.next_x[layer] = border;
        result.x[current] = Some(x);
        result.width = result.width.max(border);
        border
    }

    #[allow(clippy::too_many_lines)] // Kept structurally parallel to Java's baseline DFS.
    fn box_dfs(
        &self,
        result: &mut FragmentBox,
        x: f32,
        visited: &mut HashSet<usize>,
        current: usize,
        last: Option<usize>,
    ) -> usize {
        let mut crossings = last.map_or(0, |previous| self.entry_crossing(current, previous));
        if visited.contains(&current) {
            return crossings;
        }
        let allowed = result.fragments.iter().copied().collect::<HashSet<_>>();
        let mut parents = Vec::new();
        let mut weak_normal_parents = Vec::new();
        let mut current_x = x;

        if let Some(child_index) = result.box_for_fragment(current) {
            let child_box = result.children[child_index].1.clone();
            let child_fragments = Self::sorted_box_fragments(&child_box);
            let mut first = true;
            for fragment in child_fragments {
                if first {
                    first = false;
                    if self.one_hole.contains(&fragment) {
                        current_x = result.next_x[self.levels[fragment]];
                    } else {
                        current_x += child_box.x[fragment].unwrap_or(0.0);
                    }
                }
                if visited.insert(fragment) {
                    let position = child_box.x[fragment].unwrap_or(0.0) + current_x;
                    result.x[fragment] = Some(position);
                    let layer = self.levels[fragment];
                    result.next_x[layer] =
                        position + self.fragments[fragment].width + self.options.fragment_x_gap;
                }
                for &edge in &self.fragments[fragment].incoming {
                    let parent = self.fragment_of[self.edge(edge).0.index()];
                    if allowed.contains(&parent)
                        && !visited.contains(&parent)
                        && !child_box.fragments.contains(&parent)
                    {
                        parents.push(parent);
                    }
                }
            }
        } else {
            visited.insert(current);
            let layer = self.levels[current];
            let position = if self.one_hole.contains(&current) {
                result.next_x[layer]
            } else {
                x.max(result.next_x[layer])
            };
            result.x[current] = Some(position);
            result.next_x[layer] =
                position + self.fragments[current].width + self.options.fragment_x_gap;

            for &edge in &self.fragments[current].incoming {
                let (source, target) = self.edge(edge);
                let parent = self.fragment_of[source.index()];
                if allowed.contains(&parent) && !visited.contains(&parent) {
                    if self.graph.node(target).is_hole() {
                        weak_normal_parents.push(parent);
                    } else {
                        parents.push(parent);
                    }
                }
            }
        }

        current_x = result.x[current].unwrap_or(current_x);
        let mut next_x = current_x + self.fragments[current].width + self.options.fragment_x_gap;
        let mut right_x = if self.fragments[current].holes.len() == 1 {
            current_x
        } else {
            next_x
        };
        if parents.len() > 1 {
            self.sort_by_outdegree(&mut parents);
        }
        for parent in parents {
            crossings += self.box_dfs(result, next_x, visited, parent, Some(current));
            next_x = result.x[parent].unwrap_or(next_x)
                + self.fragments[parent].width
                + self.options.fragment_x_gap;
        }

        let mut children = self.child_fragments(current, &allowed, visited);
        children.sort_unstable();
        for child in children {
            if visited.contains(&child) {
                continue;
            }
            if let Some(child_index) = result.box_for_fragment(child) {
                let child_box = result.children[child_index].1.clone();
                let child_fragments = Self::sorted_box_fragments(&child_box);
                let mut child_box_parents = Vec::new();
                let mut child_roots = Vec::new();
                let mut first = true;
                for fragment in child_fragments {
                    if first {
                        first = false;
                        if self.one_hole.contains(&fragment) && !visited.contains(&fragment) {
                            right_x = result.next_x[self.levels[fragment]];
                        }
                    }
                    if visited.insert(fragment) {
                        let layer = self.levels[fragment];
                        let position = (child_box.x[fragment].unwrap_or(0.0) + right_x)
                            .max(result.next_x[layer]);
                        result.x[fragment] = Some(position);
                        result.next_x[layer] =
                            position + self.fragments[fragment].width + self.options.fragment_x_gap;
                    }
                    for &edge in &self.fragments[fragment].incoming {
                        let parent = self.fragment_of[self.edge(edge).0.index()];
                        if !visited.contains(&parent)
                            && allowed.contains(&parent)
                            && !child_box.fragments.contains(&parent)
                        {
                            child_box_parents.push(parent);
                            child_roots.push(fragment);
                        }
                    }
                }
                right_x = result.x[child].unwrap_or(right_x)
                    + self.fragments[child].width
                    + self.options.fragment_x_gap;
                self.sort_by_outdegree(&mut child_box_parents);
                for (parent, real_root) in child_box_parents.into_iter().zip(child_roots) {
                    crossings += self.box_dfs(result, right_x, visited, parent, Some(real_root));
                    right_x = result.next_x[self.levels[parent]];
                }
            } else {
                crossings += self.box_dfs(result, right_x, visited, child, Some(current));
                right_x = result.x[child].unwrap_or(right_x)
                    + self.fragments[child].width
                    + self.options.fragment_x_gap;
            }
        }
        result.width = right_x - self.options.fragment_x_gap;

        for parent in weak_normal_parents {
            if !visited.contains(&parent) {
                crossings += self.box_dfs(result, right_x, visited, parent, Some(current));
            }
        }
        crossings
    }

    fn sorted_box_fragments(fragment_box: &FragmentBox) -> Vec<usize> {
        let mut fragments = fragment_box.fragments.clone();
        fragments.sort_by(|left, right| {
            fragment_box.x[*left]
                .unwrap_or(0.0)
                .total_cmp(&fragment_box.x[*right].unwrap_or(0.0))
                .then_with(|| left.cmp(right))
        });
        fragments
    }

    fn sort_by_outdegree(&self, fragments: &mut [usize]) {
        fragments.sort_by_key(|fragment| {
            (
                !self.one_hole.contains(fragment),
                self.fragments[*fragment].outgoing.len(),
                *fragment,
            )
        });
    }

    fn entry_crossing(&self, current: usize, previous: usize) -> usize {
        for (hole_index, &hole) in self.fragments[current].holes.iter().enumerate() {
            if self
                .outgoing_from_node(hole)
                .iter()
                .any(|edge| self.fragment_of[self.edge(*edge).1.index()] == previous)
            {
                return usize::from(hole_index > 0);
            }
        }
        0
    }

    fn child_fragments(
        &self,
        current: usize,
        allowed: &HashSet<usize>,
        visited: &HashSet<usize>,
    ) -> Vec<usize> {
        let mut result = self.fragments[current]
            .outgoing
            .iter()
            .map(|edge| self.fragment_of[self.edge(*edge).1.index()])
            .filter(|fragment| allowed.contains(fragment) && !visited.contains(fragment))
            .collect::<Vec<_>>();
        result.sort_unstable();
        result.dedup();
        result
    }

    fn fragment_degree(&self, fragment: usize) -> usize {
        self.fragments[fragment].incoming.len() + self.fragments[fragment].outgoing.len()
    }

    fn fragment_components(&self, remaining: &HashSet<usize>) -> Vec<Vec<usize>> {
        let mut unseen = remaining.clone();
        let mut components = Vec::new();
        while let Some(&start) = unseen.iter().min() {
            unseen.remove(&start);
            let mut stack = vec![start];
            let mut component = Vec::new();
            while let Some(fragment) = stack.pop() {
                component.push(fragment);
                for &edge in self.fragments[fragment]
                    .incoming
                    .iter()
                    .chain(&self.fragments[fragment].outgoing)
                {
                    let (source, target) = self.edge(edge);
                    let source = self.fragment_of[source.index()];
                    let target = self.fragment_of[target.index()];
                    let neighbour = if source == fragment { target } else { source };
                    if unseen.remove(&neighbour) {
                        stack.push(neighbour);
                    }
                }
            }
            component.sort_unstable();
            components.push(component);
        }
        components
    }

    fn outgoing_from_node(&self, node: NodeId) -> Vec<usize> {
        self.graph
            .parsed()
            .dominance_edges()
            .iter()
            .enumerate()
            .filter_map(|(edge, &(source, _))| (source == node).then_some(edge))
            .collect()
    }

    fn edge(&self, edge: usize) -> (NodeId, NodeId) {
        self.graph.parsed().dominance_edges()[edge]
    }

    fn finish(&self, fragment_x: &[f32], fragment_y: &[f32]) -> Layout {
        let mut origins = HashMap::new();
        let mut minimum_x = 0.0_f32;
        for fragment in 0..self.fragments.len() {
            for &node in &self.fragments[fragment].nodes {
                let local = self.local[&node];
                let origin = Point {
                    x: fragment_x[fragment] + local.x,
                    y: fragment_y[fragment] + local.y,
                };
                minimum_x = minimum_x.min(origin.x);
                origins.insert(node, origin);
            }
        }
        if minimum_x < 0.0 {
            for origin in origins.values_mut() {
                origin.x -= minimum_x;
            }
        }

        let nodes = (0..self.graph.parsed().nodes().len())
            .map(|index| {
                let node = NodeId::from_index(index);
                NodeBox {
                    node,
                    origin: origins[&node],
                    size: self.sizes[&node],
                }
            })
            .collect::<Vec<_>>();
        let mut edges = Vec::<LayoutEdge>::new();
        for (parent_index, node) in self.graph.parsed().nodes().iter().enumerate() {
            let parent = NodeId::from_index(parent_index);
            for &child in node.tree_children() {
                edges.push(route_edge(
                    parent,
                    child,
                    EdgeKind::Tree,
                    false,
                    &origins,
                    &self.sizes,
                ));
            }
        }
        for (edge, &(source, target)) in self.graph.parsed().dominance_edges().iter().enumerate() {
            edges.push(route_edge(
                source,
                target,
                EdgeKind::Dominance,
                self.light_edges.contains(&edge) || tree_reachable(self.graph, source, target),
                &origins,
                &self.sizes,
            ));
        }
        let width = nodes
            .iter()
            .map(|node| node.origin.x + node.size.width)
            .fold(0.0, f32::max);
        let height = nodes
            .iter()
            .map(|node| node.origin.y + node.size.height)
            .fold(0.0, f32::max);
        Layout {
            nodes,
            edges,
            size: Size { width, height },
        }
    }
}

impl FragmentBox {
    fn new(fragments: Vec<usize>, total_fragments: usize, layers: usize) -> Self {
        Self {
            fragments,
            children: Vec::new(),
            x: vec![None; total_fragments],
            next_x: vec![0.0; layers.max(1)],
            width: 0.0,
        }
    }

    fn clear(&mut self) {
        self.x.fill(None);
        self.next_x.fill(0.0);
        self.width = 0.0;
    }

    fn box_for_fragment(&self, fragment: usize) -> Option<usize> {
        self.children
            .iter()
            .position(|(roots, _)| roots.contains(&fragment))
    }
}

fn collect_fragment_nodes(graph: &HncGraph, node: NodeId, result: &mut Vec<NodeId>) {
    result.push(node);
    for &child in graph.node(node).tree_children() {
        collect_fragment_nodes(graph, child, result);
    }
}

fn collect_holes(graph: &HncGraph, node: NodeId, result: &mut Vec<NodeId>) {
    if graph.node(node).is_hole() {
        result.push(node);
    } else {
        for &child in graph.node(node).tree_children() {
            collect_holes(graph, child, result);
        }
    }
}

fn node_center_x(
    node: NodeId,
    positions: &HashMap<NodeId, Point>,
    sizes: &HashMap<NodeId, Size>,
) -> f32 {
    positions[&node].x + sizes[&node].width / 2.0
}

fn layout_fragment_contour(
    graph: &HncGraph,
    root: NodeId,
    sizes: &HashMap<NodeId, Size>,
    options: LayoutOptions,
    positions: &mut HashMap<NodeId, Point>,
    relative_to_parent: &mut HashMap<NodeId, f32>,
) -> (f32, f32) {
    let shape = fragment_shape(graph, root, sizes, options.node_x_gap, relative_to_parent);
    let (left, right) = shape.bounding_box();
    let horizontal_offset = -left;
    let level_height = fragment_max_height(graph, root, sizes);
    let level_step = level_height + options.node_y_gap;
    place_fragment_nodes(
        graph,
        root,
        sizes,
        relative_to_parent,
        horizontal_offset,
        level_step,
        0.0,
        0.0,
        positions,
    );
    let mut holes = Vec::new();
    collect_holes(graph, root, &mut holes);
    let height = holes
        .iter()
        .map(|hole| positions[hole].y + level_height)
        .fold(level_height, f32::max);
    (right - left, height)
}

fn fragment_shape(
    graph: &HncGraph,
    node: NodeId,
    sizes: &HashMap<NodeId, Size>,
    separation: f32,
    relative_x: &mut HashMap<NodeId, f32>,
) -> Shape {
    let half_width = sizes[&node].width / 2.0;
    let node_extent = Extent {
        left: -half_width,
        right: half_width,
    };
    let children = graph.node(node).tree_children();
    if children.is_empty() {
        relative_x.insert(node, 0.0);
        return Shape(vec![node_extent]);
    }

    let child_shapes = children
        .iter()
        .map(|&child| fragment_shape(graph, child, sizes, separation, relative_x))
        .collect::<Vec<_>>();
    let (mut subtree, offsets) = merge_shape_list(&child_shapes, separation);
    subtree.0[0].left += half_width;
    subtree.0[0].right -= half_width;
    for (&child, offset) in children.iter().zip(offsets) {
        relative_x.insert(child, offset);
    }
    let mut result = Vec::with_capacity(subtree.0.len() + 1);
    result.push(node_extent);
    result.extend(subtree.0);
    Shape(result)
}

#[allow(clippy::too_many_arguments)]
fn place_fragment_nodes(
    graph: &HncGraph,
    node: NodeId,
    sizes: &HashMap<NodeId, Size>,
    relative_x: &HashMap<NodeId, f32>,
    horizontal_offset: f32,
    level_step: f32,
    axis_x: f32,
    y: f32,
    positions: &mut HashMap<NodeId, Point>,
) {
    positions.insert(
        node,
        Point {
            x: axis_x + horizontal_offset - sizes[&node].width / 2.0,
            y,
        },
    );
    for &child in graph.node(node).tree_children() {
        place_fragment_nodes(
            graph,
            child,
            sizes,
            relative_x,
            horizontal_offset,
            level_step,
            axis_x + relative_x[&child],
            y + level_step,
            positions,
        );
    }
}

fn fragment_max_height(graph: &HncGraph, root: NodeId, sizes: &HashMap<NodeId, Size>) -> f32 {
    let mut nodes = Vec::new();
    collect_fragment_nodes(graph, root, &mut nodes);
    nodes
        .into_iter()
        .map(|node| sizes[&node].height)
        .fold(0.0, f32::max)
}

fn shape_alpha(left: &Shape, right: &Shape, separation: f32) -> f32 {
    let mut alpha = separation;
    let mut right_extent = 0.0;
    let mut left_extent = 0.0;
    for (left_level, right_level) in left.0.iter().zip(&right.0) {
        right_extent += left_level.right;
        left_extent += right_level.left;
        alpha = alpha.max(right_extent - left_extent + separation);
    }
    alpha
}

fn merge_shapes(left: &Shape, right: &Shape, alpha: f32) -> Shape {
    if left.0.is_empty() {
        return right.clone();
    }
    if right.0.is_empty() {
        return left.clone();
    }

    let mut result = vec![Extent {
        left: left.0[0].left,
        right: right.0[0].right + alpha,
    }];
    let mut backoff_left = left.0[0].right - alpha - right.0[0].right;
    let mut backoff_right = right.0[0].left + alpha - left.0[0].left;
    let shared_depth = left.0.len().min(right.0.len());
    for depth in 1..shared_depth {
        result.push(Extent {
            left: left.0[depth].left,
            right: right.0[depth].right,
        });
        backoff_left += left.0[depth].right - right.0[depth].right;
        backoff_right += right.0[depth].left - left.0[depth].left;
    }
    if left.0.len() > shared_depth {
        let mut first = left.0[shared_depth];
        first.right += backoff_left;
        result.push(first);
        result.extend_from_slice(&left.0[shared_depth + 1..]);
    } else if right.0.len() > shared_depth {
        let mut first = right.0[shared_depth];
        first.left += backoff_right;
        result.push(first);
        result.extend_from_slice(&right.0[shared_depth + 1..]);
    }
    Shape(result)
}

fn merge_shape_list(shapes: &[Shape], separation: f32) -> (Shape, Vec<f32>) {
    if shapes.len() == 1 {
        return (shapes[0].clone(), vec![0.0]);
    }

    let count = shapes.len();
    let mut alpha_left = vec![0.0; count];
    let mut alpha_right = vec![0.0; count];
    let mut width = 0.0;
    let mut current_left = shapes[0].clone();
    let mut current_right = shapes[count - 1].clone();
    for index in 1..count {
        let next_left = &shapes[index];
        let next_alpha_left = shape_alpha(&current_left, next_left, separation);
        current_left = merge_shapes(&current_left, next_left, next_alpha_left);
        alpha_left[index] = next_alpha_left - width;
        width = next_alpha_left;

        let right_index = count - 1 - index;
        let next_right = &shapes[right_index];
        let next_alpha_right = shape_alpha(next_right, &current_right, separation);
        current_right = merge_shapes(next_right, &current_right, next_alpha_right);
        alpha_right[count - index] = next_alpha_right;
    }

    let half_width = (width / 2.0).floor();
    current_right.0[0].left -= half_width;
    current_right.0[0].right -= half_width;
    let mut offset = -half_width;
    let mut offsets = vec![offset];
    for index in 1..count {
        offset += f32::midpoint(alpha_left[index], alpha_right[index]).floor();
        offsets.push(offset);
    }
    (current_right, offsets)
}

impl Shape {
    fn bounding_box(&self) -> (f32, f32) {
        let mut running_left = 0.0_f32;
        let mut running_right = 0.0_f32;
        let mut left = 0.0_f32;
        let mut right = 0.0_f32;
        for extent in &self.0 {
            running_left += extent.left;
            running_right += extent.right;
            left = left.min(running_left);
            right = right.max(running_right);
        }
        (left, right)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::codec::parse_domcon_oz;

    #[test]
    fn contour_positions_match_java_fixture() {
        let graph =
            HncGraph::try_from(parse_domcon_oz("[label(r f(a b)) label(a g(c d))]").unwrap())
                .unwrap();
        let sizes = graph
            .parsed()
            .nodes()
            .iter()
            .map(|node| {
                (
                    graph.node_id(node.name()).unwrap(),
                    Size {
                        width: 40.0,
                        height: 30.0,
                    },
                )
            })
            .collect::<HashMap<_, _>>();
        let mut positions = HashMap::new();
        let mut relative_to_parent = HashMap::new();
        layout_fragment_contour(
            &graph,
            graph.node_id("r").unwrap(),
            &sizes,
            LayoutOptions::default(),
            &mut positions,
            &mut relative_to_parent,
        );

        for (name, expected) in [
            ("r", Point { x: 54.0, y: 0.0 }),
            ("a", Point { x: 27.0, y: 57.0 }),
            ("b", Point { x: 82.0, y: 57.0 }),
            ("c", Point { x: 0.0, y: 114.0 }),
            ("d", Point { x: 55.0, y: 114.0 }),
        ] {
            assert_eq!(positions[&graph.node_id(name).unwrap()], expected);
        }
    }
}
