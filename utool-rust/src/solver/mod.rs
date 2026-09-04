//! Split-based HNC dominance-graph solving.

use num_bigint::BigUint;
use packed_term_arena::tree::{Tree, TreeArena};
use rusty_alto::{Explicit, ExplicitBuilder, SortedLanguageIterator, StateId, Symbol};
use std::collections::{BTreeSet, HashMap, HashSet};
use thiserror::Error;

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

    /// Lazily enumerate independently owned solutions.
    pub fn solutions(&self) -> Solutions<'_> {
        Solutions {
            chart: self,
            inner: self.automaton.sorted_language(),
            returned_empty: false,
        }
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
        mut keep: impl FnMut(&Solution) -> bool,
        cancelled: impl Fn() -> bool,
    ) -> Result<Self, SolveError> {
        if self.empty_solution {
            let retained = keep(&Solution::empty());
            return Ok(Self {
                automaton: ExplicitBuilder::new().build(),
                split_symbols: Vec::new(),
                graph: self.graph.clone(),
                count: BigUint::from(u8::from(retained)),
                empty_solution: retained,
            });
        }

        let mut builder = ExplicitBuilder::new();
        let mut interned: HashMap<(Symbol, Vec<StateId>), StateId> = HashMap::new();
        let mut count = BigUint::from(0_u8);
        let mut language = self.automaton.sorted_language();
        while let Some(derivation) = language.next() {
            if cancelled() {
                return Err(SolveError::Cancelled);
            }
            let solution = materialize_solution(self, language.arena(), derivation.tree());
            if keep(&solution) {
                let root = copy_derivation(
                    language.arena(),
                    derivation.tree(),
                    &mut builder,
                    &mut interned,
                );
                builder.add_accepting(root);
                count += BigUint::from(1_u8);
            }
        }
        Ok(Self {
            automaton: builder.build(),
            split_symbols: self.split_symbols.clone(),
            graph: self.graph.clone(),
            count,
            empty_solution: false,
        })
    }
}

fn copy_derivation(
    arena: &TreeArena<Symbol>,
    tree: Tree,
    builder: &mut ExplicitBuilder,
    interned: &mut HashMap<(Symbol, Vec<StateId>), StateId>,
) -> StateId {
    let children = arena
        .get_children(tree)
        .iter()
        .map(|child| copy_derivation(arena, *child, builder, interned))
        .collect::<Vec<_>>();
    let symbol = *arena.get_label(tree);
    let key = (symbol, children.clone());
    if let Some(&state) = interned.get(&key) {
        return state;
    }
    let state = builder.new_state();
    builder.add_rule(symbol, children, state);
    interned.insert(key, state);
    state
}

/// One node in a fully resolved solution.
#[derive(Clone, Debug, PartialEq, Eq)]
pub struct SolutionNode {
    /// Original graph node identity.
    pub id: NodeId,
    /// External node name.
    pub name: String,
    /// Semantic label.
    pub label: String,
}

/// One fully resolved, independently owned tree.
#[derive(Debug)]
pub struct Solution {
    arena: TreeArena<SolutionNode>,
    root: Option<Tree>,
}

impl Solution {
    fn empty() -> Self {
        Self {
            arena: TreeArena::new(),
            root: None,
        }
    }

    /// Tree storage.
    #[must_use]
    pub const fn arena(&self) -> &TreeArena<SolutionNode> {
        &self.arena
    }

    /// Root handle; absent only for the empty graph's solution.
    #[must_use]
    pub const fn root(&self) -> Option<Tree> {
        self.root
    }

    /// Canonical term using external node names to disambiguate equal labels.
    #[must_use]
    pub fn to_term(&self) -> String {
        fn write(arena: &TreeArena<SolutionNode>, node: Tree, output: &mut String) {
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
        write(&self.arena, root, &mut output);
        output
    }

    /// Serialize the semantic tree using labels only, as expected by Utool's
    /// legacy `term-prolog` and `term-oz` output codecs.
    #[must_use]
    pub fn to_label_term(&self, separator: &str) -> String {
        fn write(
            arena: &TreeArena<SolutionNode>,
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
        write(&self.arena, root, separator, &mut output);
        output
    }
}

/// Lazy solution iterator backed by `rusty-alto` language enumeration.
pub struct Solutions<'a> {
    chart: &'a Chart,
    inner: SortedLanguageIterator<'a>,
    returned_empty: bool,
}

impl Iterator for Solutions<'_> {
    type Item = Solution;

    fn next(&mut self) -> Option<Self::Item> {
        if self.chart.empty_solution {
            if self.returned_empty {
                return None;
            }
            self.returned_empty = true;
            return Some(Solution::empty());
        }
        let derivation = self.inner.next()?;
        Some(materialize_solution(
            self.chart,
            self.inner.arena(),
            derivation.tree(),
        ))
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
        return Ok(Chart {
            automaton: ExplicitBuilder::new().build(),
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

    Ok(Chart {
        automaton: compiler.builder.build(),
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

fn materialize_solution(
    chart: &Chart,
    derivation_arena: &TreeArena<Symbol>,
    derivation_root: Tree,
) -> Solution {
    let mut plugging = HashMap::new();
    collect_plugging(chart, derivation_arena, derivation_root, &mut plugging);
    let top_symbol = *derivation_arena.get_label(derivation_root);
    let top = chart.split_symbols[top_symbol.0 as usize].root;
    let mut arena = TreeArena::new();
    let mut active = HashSet::new();
    let root = build_solution_node(chart, top, &plugging, &mut arena, &mut active);
    Solution {
        arena,
        root: Some(root),
    }
}

fn collect_plugging(
    chart: &Chart,
    arena: &TreeArena<Symbol>,
    tree: Tree,
    plugging: &mut HashMap<NodeId, NodeId>,
) {
    let symbol = *arena.get_label(tree);
    let split = &chart.split_symbols[symbol.0 as usize];
    plugging.extend(split.substitutions.iter().copied());
    for ((dominator, _), child) in split.attachments.iter().zip(arena.get_children(tree)) {
        let child_symbol = *arena.get_label(*child);
        let child_root = chart.split_symbols[child_symbol.0 as usize].root;
        plugging.insert(*dominator, child_root);
        collect_plugging(chart, arena, *child, plugging);
    }
}

fn build_solution_node(
    chart: &Chart,
    mut node: NodeId,
    plugging: &HashMap<NodeId, NodeId>,
    arena: &mut TreeArena<SolutionNode>,
    active: &mut HashSet<NodeId>,
) -> Tree {
    while chart.graph.node(node).is_hole() {
        node = *plugging.get(&node).unwrap_or_else(|| {
            panic!(
                "solution leaves hole {} unplugged",
                chart.graph.node(node).name()
            )
        });
    }
    assert!(active.insert(node), "solution derivation contains a cycle");
    let children = chart
        .graph
        .node(node)
        .tree_children()
        .iter()
        .map(|child| build_solution_node(chart, *child, plugging, arena, active))
        .collect();
    active.remove(&node);
    let original = chart.graph.node(node);
    arena.add_node(
        SolutionNode {
            id: node,
            name: original.name().to_owned(),
            label: original
                .label()
                .expect("non-hole node must be labeled")
                .to_owned(),
        },
        children,
    )
}
