use super::{Layout, LayoutError, LayoutOptions, Point, Size, layout_java_chart, route_edge};
use crate::{
    graph::{HncGraph, NodeId},
    solver::Chart,
};
use std::collections::{HashMap, VecDeque};

#[derive(Clone, Copy, Debug)]
struct Objective {
    overlaps: usize,
    crossings: usize,
    height: f32,
    balance: f32,
    width: f32,
    horizontal_span: f32,
}

/// Improve the Java chart layout with bounded barycentric fragment sweeps.
///
/// The algorithm preserves every fragment's internal drawing and evaluates
/// both Java's chart layers and the smallest nonnegative vertical offsets that
/// point every dominance edge down. It tries a constant number of deterministic
/// fragment orders, compacts each order without node overlap, and returns the
/// lexicographically best result by overlap count, crossings, height,
/// incoming-parent balance, horizontal edge span, and width. A final
/// constrained relaxation moves each lower fragment toward the mean center of
/// its dominance parents, without imposing an order between fragments whose
/// vertical extents do not overlap. Because the number of candidates is
/// constant, its worst-case layout work is quadratic in nodes and edges.
///
/// # Errors
///
/// Returns [`LayoutError::MissingNodeSize`] if any graph node has no measured
/// size, or [`LayoutError::InfeasibleDownwardLayout`] if a positive vertical
/// constraint cycle makes the requested geometry impossible.
pub fn layout_optimized_chart(
    chart: &Chart,
    measured_sizes: &[(NodeId, Size)],
    options: LayoutOptions,
) -> Result<Layout, LayoutError> {
    let baseline = layout_java_chart(chart, measured_sizes, options)?;
    let preserved = FragmentOptimizer::new(
        chart.graph(),
        baseline.clone(),
        options,
        false,
        options.chart_fragment_x_gap,
    )?;
    let mut best = preserved.optimize();
    if let Ok(compactor) = FragmentOptimizer::new(
        chart.graph(),
        baseline,
        options,
        true,
        options.chart_fragment_x_gap,
    ) {
        let compacted = compactor.optimize();
        if preserved
            .objective(&compacted)
            .better_than(preserved.objective(&best))
        {
            best = compacted;
        }
    }
    // Center the actual winning arrangement once. The optimization above can
    // retain its seed when that is the only zero-crossing placement; applying
    // the constraint to that final geometry avoids leaving such a placement
    // completely unbalanced.
    let finalizer = FragmentOptimizer::new(
        chart.graph(),
        best.clone(),
        options,
        false,
        options.chart_fragment_x_gap,
    )?;
    let centered = finalizer.centered();
    if finalizer
        .objective(&centered)
        .better_than(finalizer.objective(&best))
    {
        Ok(centered)
    } else {
        Ok(best)
    }
}

/// Lay out a solved graph with the default chart-backed algorithm.
///
/// This is the application-facing entry point. [`layout_java_chart`] remains
/// available as the faithful Java baseline.
///
/// # Errors
///
/// Returns the same validation and geometry errors as
/// [`layout_optimized_chart`].
pub fn layout_chart(
    chart: &Chart,
    measured_sizes: &[(NodeId, Size)],
    options: LayoutOptions,
) -> Result<Layout, LayoutError> {
    layout_optimized_chart(chart, measured_sizes, options)
}

pub(super) fn optimize_graph_layout(
    graph: &HncGraph,
    seed: Layout,
    options: LayoutOptions,
) -> Result<Layout, LayoutError> {
    match FragmentOptimizer::new(graph, seed.clone(), options, true, options.fragment_x_gap) {
        Ok(optimizer) => Ok(optimizer.optimize()),
        Err(LayoutError::InfeasibleDownwardLayout) => {
            let mut constrained = options;
            constrained.fragment_y_gap = options.node_y_gap;
            Ok(
                FragmentOptimizer::new(graph, seed, constrained, true, options.fragment_x_gap)?
                    .optimize(),
            )
        }
        Err(error) => Err(error),
    }
}

struct FragmentOptimizer<'a> {
    graph: &'a HncGraph,
    baseline: Layout,
    fragment_of: Vec<usize>,
    nodes_by_fragment: Vec<Vec<usize>>,
    local_x: Vec<f32>,
    local_y: Vec<f32>,
    baseline_anchor: Vec<f32>,
    vertical_anchor: Vec<f32>,
    separation: Vec<Vec<f32>>,
}

impl<'a> FragmentOptimizer<'a> {
    fn new(
        graph: &'a HncGraph,
        baseline: Layout,
        options: LayoutOptions,
        compact_vertical: bool,
        horizontal_gap: f32,
    ) -> Result<Self, LayoutError> {
        let fragment_of = (0..graph.parsed().nodes().len())
            .map(|node| graph.fragment_of(NodeId::from_index(node)))
            .collect::<Vec<_>>();
        let mut nodes_by_fragment = vec![Vec::new(); graph.roots().len()];
        for (node, &fragment) in fragment_of.iter().enumerate() {
            nodes_by_fragment[fragment].push(node);
        }
        let origins = baseline
            .nodes
            .iter()
            .map(|node| node.origin)
            .collect::<Vec<_>>();
        let baseline_anchor = graph
            .roots()
            .iter()
            .map(|root| origins[root.index()].x)
            .collect::<Vec<_>>();
        let baseline_y = graph
            .roots()
            .iter()
            .map(|root| origins[root.index()].y)
            .collect::<Vec<_>>();
        let local_x = origins
            .iter()
            .enumerate()
            .map(|(node, origin)| origin.x - baseline_anchor[fragment_of[node]])
            .collect::<Vec<_>>();
        let local_y = origins
            .iter()
            .enumerate()
            .map(|(node, origin)| origin.y - baseline_y[fragment_of[node]])
            .collect::<Vec<_>>();
        let vertical_anchor = if compact_vertical {
            Self::vertical_anchors(
                graph,
                &baseline,
                &fragment_of,
                &local_y,
                options.fragment_y_gap,
                vec![0.0; graph.roots().len()],
            )?
        } else {
            baseline_y
        };
        let separation = Self::separation_matrix(
            &baseline,
            &nodes_by_fragment,
            &local_x,
            &local_y,
            &vertical_anchor,
            horizontal_gap,
        );
        Ok(Self {
            graph,
            baseline,
            fragment_of,
            nodes_by_fragment,
            local_x,
            local_y,
            baseline_anchor,
            vertical_anchor,
            separation,
        })
    }

    fn optimize(&self) -> Layout {
        let mut best = self.apply(&self.baseline_anchor);
        let mut best_objective = self.objective(&best);
        for seed in self.seed_orders() {
            self.try_sweep_path(&seed, &mut best, &mut best_objective);
        }
        best
    }

    fn centered(&self) -> Layout {
        self.apply(&self.balance_incoming(&self.baseline_anchor))
    }

    fn try_sweep_path(&self, order: &[usize], best: &mut Layout, best_objective: &mut Objective) {
        let anchors = self.compact(order);
        self.consider(&anchors, best, best_objective);
        let next_order = self.barycentric_order(&anchors, order);
        self.consider(&self.compact(&next_order), best, best_objective);
    }

    fn consider(&self, anchors: &[f32], best: &mut Layout, best_objective: &mut Objective) {
        let candidate = self.apply(&self.balance_incoming(anchors));
        let candidate_objective = self.objective(&candidate);
        if candidate_objective.better_than(*best_objective) {
            *best = candidate;
            *best_objective = candidate_objective;
        }
    }

    #[allow(clippy::too_many_lines)]
    fn balance_incoming(&self, initial: &[f32]) -> Vec<f32> {
        let mut anchors = initial.to_vec();
        let mut order = (0..anchors.len()).collect::<Vec<_>>();
        order.sort_by(|left, right| {
            anchors[*left]
                .total_cmp(&anchors[*right])
                .then_with(|| left.cmp(right))
        });
        let rank = order
            .iter()
            .enumerate()
            .map(|(rank, &fragment)| (fragment, rank))
            .collect::<HashMap<_, _>>();
        let mut by_height = order.clone();
        by_height.sort_by(|left, right| {
            self.vertical_anchor[*left]
                .total_cmp(&self.vertical_anchor[*right])
                .then_with(|| rank[left].cmp(&rank[right]))
        });

        for &fragment in &by_height {
            let mut desired_sum = 0.0_f32;
            let mut desired_count = 0.0_f32;
            for &(source, target) in self.graph.parsed().dominance_edges() {
                if self.fragment_of[target.index()] != fragment
                    || self.fragment_of[source.index()] == fragment
                {
                    continue;
                }
                let source_fragment = self.fragment_of[source.index()];
                let source_center = anchors[source_fragment]
                    + self.local_x[source.index()]
                    + self.baseline.nodes[source.index()].size.width / 2.0;
                let target_center = self.local_x[target.index()]
                    + self.baseline.nodes[target.index()].size.width / 2.0;
                desired_sum += source_center - target_center;
                desired_count += 1.0;
            }
            if desired_count == 0.0 {
                continue;
            }

            let mut lower = f32::NEG_INFINITY;
            let mut upper = f32::INFINITY;
            for &other in &order {
                if rank[&other] < rank[&fragment] && self.separation[other][fragment] > 0.0 {
                    lower = lower.max(anchors[other] + self.separation[other][fragment]);
                } else if rank[&other] > rank[&fragment] && self.separation[fragment][other] > 0.0 {
                    upper = upper.min(anchors[other] - self.separation[fragment][other]);
                }
            }
            let desired = desired_sum / desired_count;
            anchors[fragment] = if lower <= upper {
                desired.clamp(lower, upper)
            } else {
                f32::midpoint(lower, upper)
            };
        }

        // Source-only fragments (in particular the graph's top fragment)
        // must be centered last: the incoming pass above may have moved all
        // of their dominance children.
        for &fragment in by_height.iter().rev() {
            let has_incoming =
                self.graph
                    .parsed()
                    .dominance_edges()
                    .iter()
                    .any(|&(source, target)| {
                        self.fragment_of[target.index()] == fragment
                            && self.fragment_of[source.index()] != fragment
                    });
            if has_incoming {
                continue;
            }
            let mut desired_sum = 0.0_f32;
            let mut desired_count = 0.0_f32;
            for &(source, target) in self.graph.parsed().dominance_edges() {
                if self.fragment_of[source.index()] != fragment
                    || self.fragment_of[target.index()] == fragment
                {
                    continue;
                }
                let target_center = anchors[self.fragment_of[target.index()]]
                    + self.local_x[target.index()]
                    + self.baseline.nodes[target.index()].size.width / 2.0;
                let source_center = self.local_x[source.index()]
                    + self.baseline.nodes[source.index()].size.width / 2.0;
                desired_sum += target_center - source_center;
                desired_count += 1.0;
            }
            if desired_count == 0.0 {
                continue;
            }
            let mut lower = f32::NEG_INFINITY;
            let mut upper = f32::INFINITY;
            for &other in &order {
                if rank[&other] < rank[&fragment] && self.separation[other][fragment] > 0.0 {
                    lower = lower.max(anchors[other] + self.separation[other][fragment]);
                } else if rank[&other] > rank[&fragment] && self.separation[fragment][other] > 0.0 {
                    upper = upper.min(anchors[other] - self.separation[fragment][other]);
                }
            }
            let desired = desired_sum / desired_count;
            anchors[fragment] = if lower <= upper {
                desired.clamp(lower, upper)
            } else {
                f32::midpoint(lower, upper)
            };
        }
        anchors
    }

    fn seed_orders(&self) -> Vec<Vec<usize>> {
        let by_id = (0..self.nodes_by_fragment.len()).collect::<Vec<_>>();
        vec![by_id]
    }

    fn barycentric_order(&self, anchors: &[f32], previous_order: &[usize]) -> Vec<usize> {
        let mut desired = vec![Vec::new(); anchors.len()];
        for &(source, target) in self.graph.parsed().dominance_edges() {
            let source_fragment = self.fragment_of[source.index()];
            let target_fragment = self.fragment_of[target.index()];
            if source_fragment == target_fragment {
                continue;
            }
            let source_center =
                self.local_x[source.index()] + self.baseline.nodes[source.index()].size.width / 2.0;
            let target_center =
                self.local_x[target.index()] + self.baseline.nodes[target.index()].size.width / 2.0;
            desired[target_fragment].push(anchors[source_fragment] + source_center - target_center);
            desired[source_fragment].push(anchors[target_fragment] + target_center - source_center);
        }
        let previous_rank = previous_order
            .iter()
            .enumerate()
            .map(|(rank, &fragment)| (fragment, rank))
            .collect::<HashMap<_, _>>();
        let scores = desired
            .into_iter()
            .enumerate()
            .map(|(fragment, values)| {
                if values.is_empty() {
                    anchors[fragment]
                } else {
                    let count = u16::try_from(values.len()).unwrap_or(u16::MAX);
                    values.iter().sum::<f32>() / f32::from(count)
                }
            })
            .collect::<Vec<_>>();
        let mut order = previous_order.to_vec();
        order.sort_by(|left, right| {
            scores[*left]
                .total_cmp(&scores[*right])
                .then_with(|| previous_rank[left].cmp(&previous_rank[right]))
                .then_with(|| left.cmp(right))
        });
        order
    }

    fn compact(&self, order: &[usize]) -> Vec<f32> {
        let mut anchors = vec![0.0_f32; order.len()];
        for (rank, &fragment) in order.iter().enumerate() {
            anchors[fragment] = order[..rank]
                .iter()
                .map(|left| anchors[*left] + self.separation[*left][fragment])
                .fold(0.0, f32::max);
        }
        anchors
    }

    fn apply(&self, anchors: &[f32]) -> Layout {
        let mut candidate = self.baseline.clone();
        let mut origins = HashMap::new();
        let mut sizes = HashMap::new();
        for node in &mut candidate.nodes {
            let fragment = self.fragment_of[node.node.index()];
            node.origin.x = anchors[fragment] + self.local_x[node.node.index()];
            node.origin.y = self.vertical_anchor[fragment] + self.local_y[node.node.index()];
            origins.insert(node.node, node.origin);
            sizes.insert(node.node, node.size);
        }
        candidate.edges = candidate
            .edges
            .iter()
            .map(|edge| {
                route_edge(
                    edge.source,
                    edge.target,
                    edge.kind,
                    edge.light,
                    &origins,
                    &sizes,
                )
            })
            .collect();
        normalize(&mut candidate);
        candidate
    }

    fn objective(&self, layout: &Layout) -> Objective {
        let mut offset_sums = vec![0.0_f32; self.nodes_by_fragment.len()];
        let mut edge_counts = vec![0.0_f32; self.nodes_by_fragment.len()];
        for &(source, target) in self.graph.parsed().dominance_edges() {
            let source_box = &layout.nodes[source.index()];
            let target_box = &layout.nodes[target.index()];
            let source_center = source_box.origin.x + source_box.size.width / 2.0;
            let target_center = target_box.origin.x + target_box.size.width / 2.0;
            let target_fragment = self.fragment_of[target.index()];
            offset_sums[target_fragment] += source_center - target_center;
            edge_counts[target_fragment] += 1.0;
        }
        Objective {
            overlaps: overlap_count(layout),
            crossings: crossing_count(layout),
            height: layout.size.height,
            balance: offset_sums
                .into_iter()
                .zip(edge_counts)
                .filter(|(_, count)| *count > 0.0)
                .map(|(sum, count)| sum.abs() / count)
                .sum(),
            width: layout.size.width,
            horizontal_span: layout
                .edges
                .iter()
                .map(|edge| (edge.points[0].x - edge.points.last().unwrap().x).abs())
                .sum(),
        }
    }

    fn separation_matrix(
        layout: &Layout,
        nodes_by_fragment: &[Vec<usize>],
        local_x: &[f32],
        local_y: &[f32],
        vertical_anchor: &[f32],
        gap: f32,
    ) -> Vec<Vec<f32>> {
        let mut result = vec![vec![0.0_f32; nodes_by_fragment.len()]; nodes_by_fragment.len()];
        for left in 0..nodes_by_fragment.len() {
            for right in 0..nodes_by_fragment.len() {
                if left == right {
                    continue;
                }
                for &left_node in &nodes_by_fragment[left] {
                    for &right_node in &nodes_by_fragment[right] {
                        let left_box = &layout.nodes[left_node];
                        let right_box = &layout.nodes[right_node];
                        let left_y = vertical_anchor[left] + local_y[left_node];
                        let right_y = vertical_anchor[right] + local_y[right_node];
                        if vertical_overlap(left_y, left_box.size, right_y, right_box.size) {
                            result[left][right] = result[left][right].max(
                                local_x[left_node] + left_box.size.width - local_x[right_node]
                                    + gap,
                            );
                        }
                    }
                }
            }
        }
        result
    }

    fn vertical_anchors(
        graph: &HncGraph,
        layout: &Layout,
        fragment_of: &[usize],
        local_y: &[f32],
        gap: f32,
        mut anchors: Vec<f32>,
    ) -> Result<Vec<f32>, LayoutError> {
        let fragment_count = graph.roots().len();
        let constraints = graph
            .parsed()
            .dominance_edges()
            .iter()
            .map(|&(source, target)| {
                (
                    fragment_of[source.index()],
                    fragment_of[target.index()],
                    local_y[source.index()] + layout.nodes[source.index()].size.height + gap
                        - local_y[target.index()],
                )
            })
            .collect::<Vec<_>>();
        let mut outgoing = vec![Vec::new(); fragment_count];
        let mut indegree = vec![0_usize; fragment_count];
        for &(source, target, distance) in &constraints {
            if source != target {
                outgoing[source].push((target, distance));
                indegree[target] += 1;
            }
        }

        let initial = anchors.clone();
        let mut queue = indegree
            .iter()
            .enumerate()
            .filter_map(|(fragment, &count)| (count == 0).then_some(fragment))
            .collect::<VecDeque<_>>();
        let mut processed = 0;
        while let Some(source) = queue.pop_front() {
            processed += 1;
            for &(target, distance) in &outgoing[source] {
                anchors[target] = anchors[target].max(anchors[source] + distance);
                indegree[target] -= 1;
                if indegree[target] == 0 {
                    queue.push_back(target);
                }
            }
        }

        if processed != fragment_count {
            anchors = initial;
            for _ in 1..fragment_count {
                let mut changed = false;
                for &(source, target, distance) in &constraints {
                    let required = anchors[source] + distance;
                    if required > anchors[target] {
                        anchors[target] = required;
                        changed = true;
                    }
                }
                if !changed {
                    break;
                }
            }
        }

        for &(source, target, distance) in &constraints {
            if anchors[target] + f32::EPSILON < anchors[source] + distance {
                return Err(LayoutError::InfeasibleDownwardLayout);
            }
        }
        Ok(anchors)
    }
}

impl Objective {
    fn better_than(self, other: Self) -> bool {
        (self.overlaps, self.crossings)
            .cmp(&(other.overlaps, other.crossings))
            .then_with(|| self.height.total_cmp(&other.height))
            .then_with(|| self.balance.total_cmp(&other.balance))
            .then_with(|| self.horizontal_span.total_cmp(&other.horizontal_span))
            .then_with(|| self.width.total_cmp(&other.width))
            .is_lt()
    }
}

fn overlap_count(layout: &Layout) -> usize {
    layout
        .nodes
        .iter()
        .enumerate()
        .map(|(index, left)| {
            layout.nodes[index + 1..]
                .iter()
                .filter(|right| {
                    horizontal_overlap(left.origin.x, left.size, right.origin.x, right.size)
                        && vertical_overlap(left.origin.y, left.size, right.origin.y, right.size)
                })
                .count()
        })
        .sum()
}

fn crossing_count(layout: &Layout) -> usize {
    layout
        .edges
        .iter()
        .enumerate()
        .map(|(index, left)| {
            layout.edges[index + 1..]
                .iter()
                .filter(|right| {
                    left.source != right.source
                        && left.source != right.target
                        && left.target != right.source
                        && left.target != right.target
                        && polylines_cross(&left.points, &right.points)
                })
                .count()
        })
        .sum()
}

fn polylines_cross(left: &[Point], right: &[Point]) -> bool {
    left.windows(2).any(|left_segment| {
        right
            .windows(2)
            .any(|right_segment| proper_cross(left_segment, right_segment))
    })
}

fn proper_cross(left: &[Point], right: &[Point]) -> bool {
    let orientation =
        |p: Point, q: Point, r: Point| (q.x - p.x) * (r.y - p.y) - (q.y - p.y) * (r.x - p.x);
    orientation(left[0], left[1], right[0]) * orientation(left[0], left[1], right[1]) < 0.0
        && orientation(right[0], right[1], left[0]) * orientation(right[0], right[1], left[1]) < 0.0
}

fn horizontal_overlap(left_x: f32, left_size: Size, right_x: f32, right_size: Size) -> bool {
    left_x < right_x + right_size.width && right_x < left_x + left_size.width
}

fn vertical_overlap(left_y: f32, left_size: Size, right_y: f32, right_size: Size) -> bool {
    left_y < right_y + right_size.height && right_y < left_y + left_size.height
}

fn normalize(layout: &mut Layout) {
    let minimum_x = layout
        .nodes
        .iter()
        .map(|node| node.origin.x)
        .fold(0.0_f32, f32::min);
    if minimum_x < 0.0 {
        for node in &mut layout.nodes {
            node.origin.x -= minimum_x;
        }
        for edge in &mut layout.edges {
            for point in &mut edge.points {
                point.x -= minimum_x;
            }
        }
    }
    layout.size.width = layout
        .nodes
        .iter()
        .map(|node| node.origin.x + node.size.width)
        .fold(0.0, f32::max);
    layout.size.height = layout
        .nodes
        .iter()
        .map(|node| node.origin.y + node.size.height)
        .fold(0.0, f32::max);
}
