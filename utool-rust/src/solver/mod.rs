//! Split-based HNC dominance-graph solving.

use num_bigint::BigUint;
use packed_term_arena::tree::{Tree, TreeArena, TreeArenaCheckpoint};
use rusty_alto::{Explicit, ExplicitBuilder, StateId, Symbol};
use std::collections::{BTreeSet, HashMap, HashSet};
use thiserror::Error;

use crate::automata_ext::{DfsDerivation, DfsLanguageIterator, DfsLanguagePlan};
use crate::graph::{HncGraph, NodeId};

#[derive(Clone, Debug, PartialEq, Eq, Hash)]
struct Subgraph(Vec<NodeId>);

impl Subgraph {
    fn new(nodes: impl IntoIterator<Item = NodeId>) -> Self {
        let mut nodes: Vec<_> = nodes.into_iter().collect();
        nodes.sort_unstable();
        nodes.dedup();
        Self(nodes)
    }

    fn contains(&self, node: NodeId) -> bool {
        self.0.binary_search(&node).is_ok()
    }
}

#[derive(Clone, Debug)]
struct Split {
    subgraph: Subgraph,
    root: NodeId,
    attachments: Vec<(NodeId, Subgraph)>,
    substitutions: Vec<(NodeId, NodeId)>,
}

/// One readable rule in a split chart.
#[derive(Clone, Debug, PartialEq, Eq)]
pub struct ChartRule {
    /// Nodes of the left-hand-side subgraph.
    pub subgraph: Vec<String>,
    /// Root fragment selected by this split.
    pub root: String,
    /// Dominator and child-subgraph pairs, in automaton-child order.
    pub attachments: Vec<(String, Vec<String>)>,
    /// Hole-to-root substitutions made inside the root fragment.
    pub substitutions: Vec<(String, String)>,
}

/// Compact tree automaton whose rules are free-root splits.
pub struct Chart {
    automaton: Explicit,
    derivation_plan: DfsLanguagePlan,
    split_symbols: Vec<Split>,
    graph: HncGraph,
    count: BigUint,
    empty_solution: bool,
}

impl Chart {
    /// Underlying bottom-up tree automaton.
    #[must_use]
    pub const fn automaton(&self) -> &Explicit {
        &self.automaton
    }

    /// Number of automaton states (subgraphs).
    #[must_use]
    pub fn state_count(&self) -> usize {
        self.automaton.num_states() as usize
    }

    /// Number of split transitions.
    #[must_use]
    pub const fn split_count(&self) -> usize {
        self.split_symbols.len()
    }

    /// Exact number of solutions.
    #[must_use]
    pub fn count_solutions(&self) -> BigUint {
        self.count.clone()
    }

    /// Stream solutions through one reusable tree arena.
    pub fn solutions(&self) -> Solutions<'_> {
        Solutions {
            chart: self,
            inner: self.derivations(),
            plugging: vec![None; self.graph.parsed().nodes().len()],
            active: vec![0; self.graph.parsed().nodes().len()],
            arena: TreeArena::new(),
            frame_checkpoints: Vec::new(),
            handles: vec![None; self.graph.parsed().nodes().len()],
            allocated_nodes: Vec::new(),
            frame_for_root: vec![None; self.graph.parsed().nodes().len()],
            root: None,
            current: false,
            returned_empty: false,
        }
    }

    /// Enumerate the chart's split derivations without materializing Solutions.
    pub fn derivations(&self) -> DfsLanguageIterator<'_> {
        self.derivation_plan.iter()
    }

    /// Rules of this chart in the same order as the automaton transitions.
    #[must_use]
    pub fn rules(&self) -> Vec<ChartRule> {
        let name = |node: NodeId| self.graph.node(node).name().to_owned();
        self.split_symbols
            .iter()
            .map(|split| ChartRule {
                subgraph: split.subgraph.0.iter().copied().map(name).collect(),
                root: name(split.root),
                attachments: split
                    .attachments
                    .iter()
                    .map(|(dominator, child)| {
                        (
                            name(*dominator),
                            child.0.iter().copied().map(name).collect(),
                        )
                    })
                    .collect(),
                substitutions: split
                    .substitutions
                    .iter()
                    .map(|(hole, root)| (name(*hole), name(*root)))
                    .collect(),
            })
            .collect()
    }

    /// Select Solutions and compile their derivations into another compact chart.
    ///
    /// This is an exact finite-language operation. Shared derivation subtrees are
    /// interned into shared automaton states in the result.
    pub fn select_solutions(
        &self,
        mut keep: impl FnMut(&Solution<'_>) -> bool,
        cancelled: impl Fn() -> bool,
    ) -> Result<Self, SolveError> {
        if self.empty_solution {
            let arena = TreeArena::new();
            let retained = keep(&Solution {
                arena: &arena,
                root: None,
            });
            let automaton = ExplicitBuilder::new().build();
            let derivation_plan = DfsLanguagePlan::new(&automaton)
                .expect("an empty automaton has no productive cycles");
            return Ok(Self {
                automaton,
                derivation_plan,
                split_symbols: Vec::new(),
                graph: self.graph.clone(),
                count: BigUint::from(u8::from(retained)),
                empty_solution: retained,
            });
        }

        let mut builder = ExplicitBuilder::new();
        let mut interned: HashMap<(Symbol, Vec<StateId>), StateId> = HashMap::new();
        let mut count = BigUint::from(0_u8);
        let mut solutions = self.solutions();
        while solutions.advance() {
            if cancelled() {
                return Err(SolveError::Cancelled);
            }
            let solution = solutions.current().expect("advance produced a solution");
            if keep(&solution) {
                let derivation = solutions
                    .current_derivation()
                    .expect("a nonempty solution has a derivation");
                let root = copy_derivation(derivation, &mut builder, &mut interned);
                builder.add_accepting(root);
                count += BigUint::from(1_u8);
            }
        }
        let automaton = builder.build();
        let derivation_plan = DfsLanguagePlan::new(&automaton)
            .expect("selected solver charts retain the acyclic state graph");
        Ok(Self {
            automaton,
            derivation_plan,
            split_symbols: self.split_symbols.clone(),
            graph: self.graph.clone(),
            count,
            empty_solution: false,
        })
    }
}

fn copy_derivation(
    derivation: DfsDerivation<'_>,
    builder: &mut ExplicitBuilder,
    interned: &mut HashMap<(Symbol, Vec<StateId>), StateId>,
) -> StateId {
    let nodes = derivation.nodes().collect::<Vec<_>>();
    let mut child_states = nodes
        .iter()
        .map(|node| vec![None; node.arity])
        .collect::<Vec<_>>();
    let mut states = vec![None; nodes.len()];
    for index in (0..nodes.len()).rev() {
        let node = nodes[index];
        let children = std::mem::take(&mut child_states[index])
            .into_iter()
            .map(|state| state.expect("children precede parents in reverse pre-order"))
            .collect::<Vec<_>>();
        let key = (node.symbol, children.clone());
        let state = if let Some(&state) = interned.get(&key) {
            state
        } else {
            let state = builder.new_state();
            builder.add_rule(node.symbol, children, state);
            interned.insert(key, state);
            state
        };
        states[index] = Some(state);
        if let Some((parent, child_index)) = node.parent {
            child_states[parent][child_index] = Some(state);
        }
    }
    states[0].expect("a derivation has a root")
}

/// One node in a fully resolved solution.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub struct SolutionNode<'a> {
    /// Original graph node identity.
    pub id: NodeId,
    /// External node name.
    pub name: &'a str,
    /// Semantic label.
    pub label: &'a str,
}

/// One fully resolved tree borrowing the iterator's reusable arena.
#[derive(Clone, Copy, Debug)]
pub struct Solution<'a> {
    arena: &'a TreeArena<SolutionNode<'a>>,
    root: Option<Tree>,
}

impl Solution<'_> {
    /// Tree storage.
    #[must_use]
    pub const fn arena(&self) -> &TreeArena<SolutionNode<'_>> {
        self.arena
    }

    /// Root handle; absent only for the empty graph's solution.
    #[must_use]
    pub const fn root(&self) -> Option<Tree> {
        self.root
    }

    /// Canonical term using external node names to disambiguate equal labels.
    #[must_use]
    pub fn to_term(&self) -> String {
        fn write(arena: &TreeArena<SolutionNode<'_>>, node: Tree, output: &mut String) {
            let data = arena.get_label(node);
            output.push_str(&data.label);
            output.push('[');
            output.push_str(&data.name);
            output.push(']');
            let children = arena.get_children(node);
            if !children.is_empty() {
                output.push('(');
                for (index, child) in children.iter().enumerate() {
                    if index > 0 {
                        output.push(',');
                    }
                    write(arena, *child, output);
                }
                output.push(')');
            }
        }

        let Some(root) = self.root else {
            return String::new();
        };
        let mut output = String::new();
        write(self.arena, root, &mut output);
        output
    }

    /// Serialize the semantic tree using labels only, as expected by Utool's
    /// legacy `term-prolog` and `term-oz` output codecs.
    #[must_use]
    pub fn to_label_term(&self, separator: &str) -> String {
        fn write(
            arena: &TreeArena<SolutionNode<'_>>,
            node: Tree,
            separator: &str,
            output: &mut String,
        ) {
            let data = arena.get_label(node);
            output.push_str(&data.label);
            let children = arena.get_children(node);
            if !children.is_empty() {
                output.push('(');
                for (index, child) in children.iter().enumerate() {
                    if index > 0 {
                        output.push_str(separator);
                    }
                    write(arena, *child, separator, output);
                }
                output.push(')');
            }
        }
        let Some(root) = self.root else {
            return String::new();
        };
        let mut output = String::new();
        write(self.arena, root, separator, &mut output);
        output
    }
}

/// Streaming solution iterator backed by finite depth-first chart enumeration.
pub struct Solutions<'a> {
    chart: &'a Chart,
    inner: DfsLanguageIterator<'a>,
    plugging: Vec<Option<NodeId>>,
    active: Vec<u8>,
    arena: TreeArena<SolutionNode<'a>>,
    frame_checkpoints: Vec<Option<TreeArenaCheckpoint>>,
    handles: Vec<Option<Tree>>,
    allocated_nodes: Vec<NodeId>,
    frame_for_root: Vec<Option<usize>>,
    root: Option<Tree>,
    current: bool,
    returned_empty: bool,
}

impl Solutions<'_> {
    /// Advance to the next solved form, invalidating the previous one.
    pub fn advance(&mut self) -> bool {
        if self.chart.empty_solution {
            if self.returned_empty {
                self.current = false;
                return false;
            }
            self.returned_empty = true;
            self.root = None;
            self.current = true;
            return true;
        }
        if !self.inner.advance() {
            self.current = false;
            return false;
        }
        rebuild_solution(
            self.chart,
            self.inner.current().expect("advance produced a derivation"),
            self.inner.changed_from(),
            self.current,
            &mut self.plugging,
            &mut self.active,
            &mut self.arena,
            &mut self.frame_checkpoints,
            &mut self.handles,
            &mut self.allocated_nodes,
            &mut self.frame_for_root,
            &mut self.root,
        );
        self.current = true;
        true
    }

    /// Skip `n` derivations and advance to the following solved form.
    pub fn advance_by(&mut self, n: usize) -> bool {
        if self.chart.empty_solution {
            if n == 0 && !self.returned_empty {
                self.returned_empty = true;
                self.root = None;
                self.current = true;
                return true;
            }
            self.returned_empty = true;
            self.current = false;
            return false;
        }
        if n > 0 && self.current {
            self.arena.clear();
            self.frame_checkpoints.clear();
            self.handles.fill(None);
            self.allocated_nodes.clear();
            self.current = false;
        }
        for _ in 0..n {
            if !self.inner.advance() {
                self.current = false;
                return false;
            }
        }
        self.advance()
    }

    /// Borrow the current solved form until the next mutable iterator access.
    #[must_use]
    pub fn current(&self) -> Option<Solution<'_>> {
        self.current.then_some(Solution {
            arena: &self.arena,
            root: self.root,
        })
    }

    fn current_derivation(&self) -> Option<DfsDerivation<'_>> {
        self.current.then(|| self.inner.current()).flatten()
    }
}

/// Solver failure.
#[derive(Debug, Error)]
pub enum SolveError {
    /// The accepted HNC graph unexpectedly has multiple components.
    #[error("HNC graph has {0} weakly connected components")]
    Disconnected(usize),
    /// Internal split metadata did not form a complete plugging.
    #[error("invalid split derivation: {0}")]
    InvalidDerivation(String),
    /// The caller cancelled chart construction.
    #[error("chart construction was cancelled")]
    Cancelled,
}

/// Construct a chart using the free-fragment split algorithm.
pub fn solve(graph: &HncGraph) -> Result<Chart, SolveError> {
    solve_with_cancellation(graph, || false)
}

/// Construct a chart, checking `cancelled` between split-expansion steps.
pub fn solve_with_cancellation(
    graph: &HncGraph,
    cancelled: impl Fn() -> bool,
) -> Result<Chart, SolveError> {
    if cancelled() {
        return Err(SolveError::Cancelled);
    }
    if graph.parsed().nodes().is_empty() {
        let automaton = ExplicitBuilder::new().build();
        let derivation_plan =
            DfsLanguagePlan::new(&automaton).expect("an empty automaton has no productive cycles");
        return Ok(Chart {
            automaton,
            derivation_plan,
            split_symbols: Vec::new(),
            graph: graph.clone(),
            count: BigUint::from(1_u8),
            empty_solution: true,
        });
    }

    let components = graph.parsed().weakly_connected_components();
    if components.len() != 1 {
        return Err(SolveError::Disconnected(components.len()));
    }

    let mut compiler = Compiler::new(graph);
    let top = Subgraph::new(components[0].iter().copied());
    let (top_state, count) = compiler.compile(&top, &cancelled)?;
    if count != BigUint::from(0_u8) {
        compiler.builder.add_accepting(top_state);
    }

    let automaton = compiler.builder.build();
    let derivation_plan = DfsLanguagePlan::new(&automaton)
        .expect("solver charts have an acyclic productive state graph");
    Ok(Chart {
        automaton,
        derivation_plan,
        split_symbols: compiler.splits,
        graph: graph.clone(),
        count,
        empty_solution: false,
    })
}

struct Compiler<'a> {
    graph: &'a HncGraph,
    builder: ExplicitBuilder,
    states: HashMap<Subgraph, StateId>,
    counts: HashMap<Subgraph, BigUint>,
    splits: Vec<Split>,
}

impl<'a> Compiler<'a> {
    fn new(graph: &'a HncGraph) -> Self {
        Self {
            graph,
            builder: ExplicitBuilder::new(),
            states: HashMap::new(),
            counts: HashMap::new(),
            splits: Vec::new(),
        }
    }

    fn compile(
        &mut self,
        subgraph: &Subgraph,
        cancelled: &impl Fn() -> bool,
    ) -> Result<(StateId, BigUint), SolveError> {
        if cancelled() {
            return Err(SolveError::Cancelled);
        }
        if let Some(&state) = self.states.get(subgraph) {
            return Ok((
                state,
                self.counts
                    .get(subgraph)
                    .cloned()
                    .unwrap_or_else(|| BigUint::from(0_u8)),
            ));
        }

        let state = self.builder.new_state();
        self.states.insert(subgraph.clone(), state);

        let mut total = BigUint::from(0_u8);
        for split in compute_splits(self.graph, subgraph) {
            if cancelled() {
                return Err(SolveError::Cancelled);
            }
            let mut child_states = Vec::with_capacity(split.attachments.len());
            let mut split_count = BigUint::from(1_u8);
            for (_, child) in &split.attachments {
                let (child_state, child_count) = self.compile(child, cancelled)?;
                child_states.push(child_state);
                split_count *= child_count;
            }
            if split_count == BigUint::from(0_u8) {
                continue;
            }
            let symbol = Symbol(self.splits.len() as u32);
            self.splits.push(split);
            self.builder.add_rule(symbol, child_states, state);
            total += split_count;
        }

        self.counts.insert(subgraph.clone(), total.clone());
        Ok((state, total))
    }
}

fn compute_splits(graph: &HncGraph, subgraph: &Subgraph) -> Vec<Split> {
    subgraph
        .0
        .iter()
        .copied()
        .filter(|&node| indegree_in(graph, node, subgraph) == 0)
        .filter_map(|root| compute_split(graph, root, subgraph))
        .collect()
}

fn indegree_in(graph: &HncGraph, node: NodeId, subgraph: &Subgraph) -> usize {
    let tree = graph
        .tree_parent(node)
        .filter(|parent| subgraph.contains(*parent))
        .map_or(0, |_| 1);
    tree + graph
        .parsed()
        .dominance_edges()
        .iter()
        .filter(|(source, target)| *target == node && subgraph.contains(*source))
        .count()
}

fn compute_split(graph: &HncGraph, root: NodeId, subgraph: &Subgraph) -> Option<Split> {
    let mut root_fragment = HashSet::new();
    let mut ancestors = HashSet::new();
    let mut substitutions = Vec::new();
    if !root_fragment_dfs(
        graph,
        root,
        subgraph,
        &mut root_fragment,
        &mut ancestors,
        &mut substitutions,
    ) {
        return None;
    }

    let mut visited = HashSet::new();
    let mut path = HashSet::from([root]);
    let mut wcc_order = Vec::new();
    let mut wccs: HashMap<usize, BTreeSet<NodeId>> = HashMap::new();
    if !split_dfs(
        graph,
        root,
        subgraph,
        &root_fragment,
        None,
        &mut path,
        &mut visited,
        &mut wcc_order,
        &mut wccs,
    ) || visited.len() != subgraph.0.len()
    {
        return None;
    }

    let attachments = wcc_order
        .into_iter()
        .map(|edge_index| {
            let (dominator, _) = graph.parsed().dominance_edges()[edge_index];
            let nodes = wccs.remove(&edge_index).unwrap_or_default();
            (dominator, Subgraph::new(nodes))
        })
        .collect();

    Some(Split {
        subgraph: subgraph.clone(),
        root,
        attachments,
        substitutions,
    })
}

fn root_fragment_dfs(
    graph: &HncGraph,
    node: NodeId,
    subgraph: &Subgraph,
    nodes: &mut HashSet<NodeId>,
    ancestors: &mut HashSet<NodeId>,
    substitutions: &mut Vec<(NodeId, NodeId)>,
) -> bool {
    nodes.insert(node);
    let tree_children: Vec<_> = graph
        .node(node)
        .tree_children()
        .iter()
        .copied()
        .filter(|child| subgraph.contains(*child))
        .collect();
    let dominance_parents: Vec<_> = graph
        .parsed()
        .dominance_edges()
        .iter()
        .filter_map(|(source, target)| {
            (*target == node && subgraph.contains(*source) && !ancestors.contains(source))
                .then_some(*source)
        })
        .collect();

    if !dominance_parents.is_empty() {
        if dominance_parents.len() > 1 || !tree_children.is_empty() {
            return false;
        }
        if dominance_parents
            .iter()
            .any(|parent| graph.tree_parent(*parent).is_some())
        {
            return false;
        }
    }

    for child in tree_children {
        if nodes.contains(&child) {
            return false;
        }
        ancestors.insert(node);
        let ok = root_fragment_dfs(graph, child, subgraph, nodes, ancestors, substitutions);
        ancestors.remove(&node);
        if !ok {
            return false;
        }
    }

    for parent in dominance_parents {
        substitutions.push((node, parent));
        if nodes.contains(&parent) {
            return false;
        }
        ancestors.insert(node);
        let ok = root_fragment_dfs(graph, parent, subgraph, nodes, ancestors, substitutions);
        ancestors.remove(&node);
        if !ok {
            return false;
        }
    }
    true
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
enum Kind {
    Tree,
    Dominance(usize),
}

#[derive(Clone, Copy, Debug)]
struct Edge {
    source: NodeId,
    target: NodeId,
    kind: Kind,
}

impl Edge {
    fn opposite(self, node: NodeId) -> NodeId {
        if self.source == node {
            self.target
        } else {
            self.source
        }
    }
}

fn adjacent_edges(graph: &HncGraph, node: NodeId) -> Vec<Edge> {
    let mut result = Vec::new();
    if let Some(parent) = graph.tree_parent(node) {
        result.push(Edge {
            source: parent,
            target: node,
            kind: Kind::Tree,
        });
    }
    for &child in graph.node(node).tree_children() {
        result.push(Edge {
            source: node,
            target: child,
            kind: Kind::Tree,
        });
    }
    for (index, &(source, target)) in graph.parsed().dominance_edges().iter().enumerate() {
        if target == node {
            result.push(Edge {
                source,
                target,
                kind: Kind::Dominance(index),
            });
        }
    }
    for (index, &(source, target)) in graph.parsed().dominance_edges().iter().enumerate() {
        if source == node {
            result.push(Edge {
                source,
                target,
                kind: Kind::Dominance(index),
            });
        }
    }
    result
}

#[allow(clippy::too_many_arguments)]
fn split_dfs(
    graph: &HncGraph,
    node: NodeId,
    subgraph: &Subgraph,
    root_fragment: &HashSet<NodeId>,
    wcc_id: Option<usize>,
    path: &mut HashSet<NodeId>,
    visited: &mut HashSet<NodeId>,
    wcc_order: &mut Vec<usize>,
    wccs: &mut HashMap<usize, BTreeSet<NodeId>>,
) -> bool {
    if !visited.insert(node) {
        return false;
    }
    if !root_fragment.contains(&node) {
        let id = wcc_id.expect("nodes outside root fragment have a WCC edge");
        if !wccs.contains_key(&id) {
            wcc_order.push(id);
        }
        wccs.entry(id).or_default().insert(node);
    }

    for edge in adjacent_edges(graph, node) {
        let neighbor = edge.opposite(node);
        if !subgraph.contains(neighbor) {
            continue;
        }
        if root_fragment.contains(&neighbor) && !root_fragment.contains(&node) {
            if !matches!(edge.kind, Kind::Dominance(_))
                || edge.source != neighbor
                || !path.contains(&neighbor)
            {
                return false;
            }
        } else if !visited.contains(&neighbor) {
            if root_fragment.contains(&node) {
                if root_fragment.contains(&neighbor) {
                    path.insert(neighbor);
                    let ok = split_dfs(
                        graph,
                        neighbor,
                        subgraph,
                        root_fragment,
                        None,
                        path,
                        visited,
                        wcc_order,
                        wccs,
                    );
                    path.remove(&neighbor);
                    if !ok {
                        return false;
                    }
                } else {
                    let Kind::Dominance(edge_index) = edge.kind else {
                        return false;
                    };
                    if edge.source != node
                        || !split_dfs(
                            graph,
                            neighbor,
                            subgraph,
                            root_fragment,
                            Some(edge_index),
                            path,
                            visited,
                            wcc_order,
                            wccs,
                        )
                    {
                        return false;
                    }
                }
            } else if !split_dfs(
                graph,
                neighbor,
                subgraph,
                root_fragment,
                wcc_id,
                path,
                visited,
                wcc_order,
                wccs,
            ) {
                return false;
            }
        }
    }
    true
}

#[allow(clippy::too_many_arguments)]
fn rebuild_solution<'a>(
    chart: &'a Chart,
    derivation: DfsDerivation<'_>,
    changed_from: usize,
    had_current: bool,
    plugging: &mut [Option<NodeId>],
    active: &mut [u8],
    arena: &mut TreeArena<SolutionNode<'a>>,
    frame_checkpoints: &mut Vec<Option<TreeArenaCheckpoint>>,
    handles: &mut [Option<Tree>],
    allocated_nodes: &mut Vec<NodeId>,
    frame_for_root: &mut [Option<usize>],
    root: &mut Option<Tree>,
) {
    if had_current {
        let checkpoint = frame_checkpoints[changed_from]
            .expect("every derivation frame has an arena checkpoint");
        arena.rewind(checkpoint);
        while allocated_nodes.len() > arena.len() {
            let node = allocated_nodes.pop().unwrap();
            handles[node.index()] = None;
        }
    }

    plugging.fill(None);
    collect_plugging(chart, derivation, plugging);
    frame_for_root.fill(None);
    if had_current {
        frame_checkpoints.truncate(derivation.len());
        frame_checkpoints.resize(derivation.len(), None);
        for checkpoint in frame_checkpoints.iter_mut().skip(changed_from + 1) {
            *checkpoint = None;
        }
    } else {
        frame_checkpoints.clear();
        frame_checkpoints.resize(derivation.len(), None);
    }
    for (frame, node) in derivation.nodes().enumerate() {
        let split_root = chart.split_symbols[node.symbol.0 as usize].root;
        assert!(
            frame_for_root[split_root.index()].replace(frame).is_none(),
            "a fragment root occurs once in a derivation"
        );
    }

    let top_symbol = derivation.node(0).symbol;
    let top = chart.split_symbols[top_symbol.0 as usize].root;
    *root = Some(build_solution_node_reusing(
        chart,
        top,
        plugging,
        active,
        arena,
        frame_checkpoints,
        handles,
        allocated_nodes,
        frame_for_root,
    ));
    debug_assert_eq!(allocated_nodes.len(), arena.len());
}

fn collect_plugging(chart: &Chart, derivation: DfsDerivation<'_>, plugging: &mut [Option<NodeId>]) {
    for node in derivation.nodes() {
        let split = &chart.split_symbols[node.symbol.0 as usize];
        for &(hole, root) in &split.substitutions {
            plugging[hole.index()] = Some(root);
        }
        if let Some((parent, child_index)) = node.parent {
            let parent_symbol = derivation.node(parent).symbol;
            let dominator =
                chart.split_symbols[parent_symbol.0 as usize].attachments[child_index].0;
            plugging[dominator.index()] = Some(split.root);
        }
    }
}

#[allow(clippy::too_many_arguments)]
fn build_solution_node_reusing<'a>(
    chart: &'a Chart,
    mut node: NodeId,
    plugging: &[Option<NodeId>],
    active: &mut [u8],
    arena: &mut TreeArena<SolutionNode<'a>>,
    frame_checkpoints: &mut [Option<TreeArenaCheckpoint>],
    handles: &mut [Option<Tree>],
    allocated_nodes: &mut Vec<NodeId>,
    frame_for_root: &[Option<usize>],
) -> Tree {
    while chart.graph.node(node).is_hole() {
        node = plugging[node.index()].unwrap_or_else(|| {
            panic!(
                "solution leaves hole {} unplugged",
                chart.graph.node(node).name()
            )
        });
    }
    if let Some(tree) = handles[node.index()] {
        return tree;
    }
    if let Some(frame) = frame_for_root[node.index()] {
        frame_checkpoints[frame].get_or_insert_with(|| arena.checkpoint());
    }
    assert_eq!(
        active[node.index()],
        0,
        "solution derivation contains a cycle"
    );
    active[node.index()] = 1;
    let children = chart
        .graph
        .node(node)
        .tree_children()
        .iter()
        .map(|&child| {
            build_solution_node_reusing(
                chart,
                child,
                plugging,
                active,
                arena,
                frame_checkpoints,
                handles,
                allocated_nodes,
                frame_for_root,
            )
        })
        .collect();
    active[node.index()] = 0;
    let original = chart.graph.node(node);
    let tree = arena.add_node(
        SolutionNode {
            id: node,
            name: original.name(),
            label: original.label().expect("non-hole node must be labeled"),
        },
        children,
    );
    handles[node.index()] = Some(tree);
    allocated_nodes.push(node);
    tree
}
