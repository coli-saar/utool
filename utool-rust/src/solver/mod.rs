//! Split-based HNC dominance-graph solving.

use num_bigint::BigUint;
use packed_term_arena::tree::{Tree, TreeArena};
use rusty_alto::{Explicit, ExplicitBuilder, StateId, Symbol};
use std::collections::HashMap;
use thiserror::Error;

use crate::automata_ext::{DfsDerivation, DfsLanguageIterator, DfsLanguagePlan};
use crate::graph::{HncGraph, NodeId};

#[derive(Clone, Debug, PartialEq, Eq, Hash)]
struct BitSet(Vec<u64>);

impl BitSet {
    fn empty(universe: usize) -> Self {
        Self(vec![0; universe.div_ceil(64)])
    }

    fn full(universe: usize) -> Self {
        let mut set = Self(vec![u64::MAX; universe.div_ceil(64)]);
        let excess = set.0.len() * 64 - universe;
        if let Some(last) = set.0.last_mut() {
            *last >>= excess;
        }
        set
    }

    fn contains(&self, index: usize) -> bool {
        self.0[index / 64] & (1 << (index % 64)) != 0
    }

    fn insert(&mut self, index: usize) -> bool {
        let bit = 1 << (index % 64);
        let word = &mut self.0[index / 64];
        let fresh = *word & bit == 0;
        *word |= bit;
        fresh
    }

    fn remove(&mut self, index: usize) {
        self.0[index / 64] &= !(1 << (index % 64));
    }

    fn count(&self) -> usize {
        self.0.iter().map(|word| word.count_ones() as usize).sum()
    }

    fn members(&self) -> impl Iterator<Item = usize> + '_ {
        self.0.iter().enumerate().flat_map(|(word_index, &word)| {
            let mut remaining = word;
            std::iter::from_fn(move || {
                if remaining == 0 {
                    return None;
                }
                let bit = remaining.trailing_zeros() as usize;
                remaining &= remaining - 1;
                Some(word_index * 64 + bit)
            })
        })
    }
}

#[cfg(test)]
mod bit_set_tests {
    use super::BitSet;

    #[test]
    fn masks_padding_above_the_universe() {
        let mut set = BitSet::full(65);
        assert_eq!(set.count(), 65);
        assert_eq!(
            set.members().collect::<Vec<_>>(),
            (0..65).collect::<Vec<_>>()
        );
        set.remove(64);
        assert!(!set.contains(64));
        assert!(set.insert(64));
    }
}

#[derive(Clone, Debug, PartialEq, Eq, Hash)]
struct Subgraph(BitSet);

impl Subgraph {
    fn all(graph: &HncGraph) -> Self {
        Self(BitSet::full(graph.roots().len()))
    }

    fn empty(graph: &HncGraph) -> Self {
        Self(BitSet::empty(graph.roots().len()))
    }

    fn contains(&self, graph: &HncGraph, node: NodeId) -> bool {
        // Splitting only removes whole tree fragments, so every recursive
        // subgraph is a union of fragments and needs one bit per fragment.
        self.0.contains(graph.fragment_of(node))
    }

    fn insert_fragment_of(&mut self, graph: &HncGraph, node: NodeId) {
        self.0.insert(graph.fragment_of(node));
    }

    fn nodes(&self, graph: &HncGraph) -> Vec<NodeId> {
        let mut nodes = self
            .0
            .members()
            .flat_map(|fragment| graph.fragment_nodes(fragment).iter().copied())
            .collect::<Vec<_>>();
        nodes.sort_unstable();
        nodes
    }

    fn node_count(&self, graph: &HncGraph) -> usize {
        self.0
            .members()
            .map(|fragment| graph.fragment_nodes(fragment).len())
            .sum()
    }
}

#[derive(Clone, Debug)]
struct Split {
    subgraph: SubgraphId,
    root: NodeId,
    attachments: Vec<(NodeId, SubgraphId)>,
    substitutions: Vec<(NodeId, NodeId)>,
}

#[derive(Clone, Copy, Debug)]
struct SubgraphId(u32);

#[derive(Clone, Debug)]
struct SplitCandidate {
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
    subgraphs: Vec<Subgraph>,
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

    /// Stream solutions through one stable arena whose fixed-arity edges are updated in place.
    pub fn solutions(&self) -> Solutions<'_> {
        let (arena, handles, hole_slots) = initialize_solution_arena(self);
        Solutions {
            chart: self,
            inner: self.derivations(),
            arena,
            handles,
            hole_slots,
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
                subgraph: self.subgraphs[split.subgraph.0 as usize]
                    .nodes(&self.graph)
                    .into_iter()
                    .map(name)
                    .collect(),
                root: name(split.root),
                attachments: split
                    .attachments
                    .iter()
                    .map(|(dominator, child)| {
                        (
                            name(*dominator),
                            self.subgraphs[child.0 as usize]
                                .nodes(&self.graph)
                                .into_iter()
                                .map(name)
                                .collect(),
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
                chart: self,
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
                subgraphs: Vec::new(),
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
            subgraphs: self.subgraphs.clone(),
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

/// Compact identity of one node in a fully resolved solution.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub struct SolutionNode {
    /// Original graph node identity.
    pub id: NodeId,
}

/// One fully resolved tree borrowing the iterator's reusable arena.
#[derive(Clone, Copy)]
pub struct Solution<'a> {
    chart: &'a Chart,
    arena: &'a TreeArena<SolutionNode>,
    root: Option<Tree>,
}

impl Solution<'_> {
    /// Tree storage.
    #[must_use]
    pub const fn arena(&self) -> &TreeArena<SolutionNode> {
        self.arena
    }

    /// Original graph identity represented by an arena node.
    #[must_use]
    pub fn node_id(&self, tree: Tree) -> NodeId {
        self.arena.get_label(tree).id
    }

    /// External name of an arena node.
    #[must_use]
    pub fn node_name(&self, tree: Tree) -> &str {
        self.chart.graph.node(self.node_id(tree)).name()
    }

    /// Semantic label of an arena node.
    #[must_use]
    pub fn node_label(&self, tree: Tree) -> &str {
        self.chart
            .graph
            .node(self.node_id(tree))
            .label()
            .expect("solution nodes are labeled")
    }

    /// Root handle; absent only for the empty graph's solution.
    #[must_use]
    pub const fn root(&self) -> Option<Tree> {
        self.root
    }

    /// Canonical term using external node names to disambiguate equal labels.
    #[must_use]
    pub fn to_term(&self) -> String {
        fn write(solution: &Solution<'_>, node: Tree, output: &mut String) {
            output.push_str(solution.node_label(node));
            output.push('[');
            output.push_str(solution.node_name(node));
            output.push(']');
            let children = solution.arena.get_children(node);
            if !children.is_empty() {
                output.push('(');
                for (index, child) in children.iter().enumerate() {
                    if index > 0 {
                        output.push(',');
                    }
                    write(solution, *child, output);
                }
                output.push(')');
            }
        }

        let Some(root) = self.root else {
            return String::new();
        };
        let mut output = String::new();
        write(self, root, &mut output);
        output
    }

    /// Serialize the semantic tree using labels only, as expected by Utool's
    /// legacy `term-prolog` and `term-oz` output codecs.
    #[must_use]
    pub fn to_label_term(&self, separator: &str) -> String {
        fn write(solution: &Solution<'_>, node: Tree, separator: &str, output: &mut String) {
            output.push_str(solution.node_label(node));
            let children = solution.arena.get_children(node);
            if !children.is_empty() {
                output.push('(');
                for (index, child) in children.iter().enumerate() {
                    if index > 0 {
                        output.push_str(separator);
                    }
                    write(solution, *child, separator, output);
                }
                output.push(')');
            }
        }
        let Some(root) = self.root else {
            return String::new();
        };
        let mut output = String::new();
        write(self, root, separator, &mut output);
        output
    }
}

/// Streaming solution iterator backed by finite depth-first chart enumeration.
///
/// Every labeled graph node has one stable arena handle. Advancing changes only
/// the child slots corresponding to hole substitutions and the current root.
pub struct Solutions<'a> {
    chart: &'a Chart,
    inner: DfsLanguageIterator<'a>,
    arena: TreeArena<SolutionNode>,
    handles: Vec<Option<Tree>>,
    hole_slots: Vec<Option<(Tree, usize)>>,
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
        update_solution(
            self.chart,
            self.inner.current().expect("advance produced a derivation"),
            self.inner.changed_from(),
            self.current,
            &mut self.arena,
            &self.handles,
            &self.hole_slots,
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
            chart: self.chart,
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
            subgraphs: Vec::new(),
            graph: graph.clone(),
            count: BigUint::from(1_u8),
            empty_solution: true,
        });
    }

    let mut compiler = Compiler::new(graph);
    let top = Subgraph::all(graph);
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
        subgraphs: compiler.subgraphs,
        graph: graph.clone(),
        count,
        empty_solution: false,
    })
}

struct Compiler<'a> {
    graph: &'a HncGraph,
    builder: ExplicitBuilder,
    states: HashMap<Subgraph, StateId>,
    counts: Vec<Option<BigUint>>,
    subgraphs: Vec<Subgraph>,
    splits: Vec<Split>,
}

impl<'a> Compiler<'a> {
    fn new(graph: &'a HncGraph) -> Self {
        Self {
            graph,
            builder: ExplicitBuilder::new(),
            states: HashMap::new(),
            counts: Vec::new(),
            subgraphs: Vec::new(),
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
                    .get(state.0 as usize)
                    .and_then(Option::as_ref)
                    .cloned()
                    .unwrap_or_else(|| BigUint::from(0_u8)),
            ));
        }

        let state = self.builder.new_state();
        assert_eq!(state.0 as usize, self.subgraphs.len());
        self.subgraphs.push(subgraph.clone());
        self.counts.push(None);
        self.states.insert(subgraph.clone(), state);

        let mut total = BigUint::from(0_u8);
        for root_index in 0..self.graph.roots().len() {
            if cancelled() {
                return Err(SolveError::Cancelled);
            }
            let root = self.graph.roots()[root_index];
            if !subgraph.contains(self.graph, root) || indegree_in(self.graph, root, subgraph) != 0
            {
                continue;
            }
            let Some(candidate) = compute_split(self.graph, root, subgraph) else {
                continue;
            };
            let mut child_states = Vec::with_capacity(candidate.attachments.len());
            let mut attachments = Vec::with_capacity(candidate.attachments.len());
            let mut split_count = BigUint::from(1_u8);
            for (dominator, child) in &candidate.attachments {
                let (child_state, child_count) = self.compile(child, cancelled)?;
                child_states.push(child_state);
                attachments.push((*dominator, SubgraphId(child_state.0)));
                split_count *= child_count;
            }
            if split_count == BigUint::from(0_u8) {
                continue;
            }
            let symbol = Symbol(self.splits.len() as u32);
            self.splits.push(Split {
                subgraph: SubgraphId(state.0),
                root: candidate.root,
                attachments,
                substitutions: candidate.substitutions,
            });
            self.builder.add_rule(symbol, child_states, state);
            total += split_count;
        }

        self.counts[state.0 as usize] = Some(total.clone());
        Ok((state, total))
    }
}

fn indegree_in(graph: &HncGraph, node: NodeId, subgraph: &Subgraph) -> usize {
    let tree = graph
        .tree_parent(node)
        .filter(|parent| subgraph.contains(graph, *parent))
        .map_or(0, |_| 1);
    tree + graph
        .incoming_dominance(node)
        .iter()
        .filter(|(_, source)| subgraph.contains(graph, *source))
        .count()
}

fn compute_split(graph: &HncGraph, root: NodeId, subgraph: &Subgraph) -> Option<SplitCandidate> {
    let mut root_traversal = RootFragmentTraversal::new(graph, subgraph);
    if !root_traversal.visit(root) {
        return None;
    }
    let RootFragmentTraversal {
        nodes: root_fragment,
        substitutions,
        ..
    } = root_traversal;

    let mut traversal = SplitTraversal::new(graph, subgraph, &root_fragment, root);
    if !traversal.visit(root, None) || traversal.visited.count() != subgraph.node_count(graph) {
        return None;
    }

    let attachments = traversal
        .wcc_order
        .into_iter()
        .map(|edge_index| {
            let (dominator, _) = graph.parsed().dominance_edges()[edge_index];
            let subgraph = traversal.wccs[edge_index]
                .take()
                .expect("a discovered WCC has members");
            (dominator, subgraph)
        })
        .collect();

    Some(SplitCandidate {
        root,
        attachments,
        substitutions,
    })
}

struct RootFragmentTraversal<'a> {
    graph: &'a HncGraph,
    subgraph: &'a Subgraph,
    nodes: BitSet,
    ancestors: BitSet,
    substitutions: Vec<(NodeId, NodeId)>,
}

impl<'a> RootFragmentTraversal<'a> {
    fn new(graph: &'a HncGraph, subgraph: &'a Subgraph) -> Self {
        let node_count = graph.parsed().nodes().len();
        Self {
            graph,
            subgraph,
            nodes: BitSet::empty(node_count),
            ancestors: BitSet::empty(node_count),
            substitutions: Vec::new(),
        }
    }

    fn visit(&mut self, node: NodeId) -> bool {
        self.nodes.insert(node.index());

        let mut dominance_parent = None;
        for &(_, parent) in self.graph.incoming_dominance(node) {
            if self.subgraph.contains(self.graph, parent)
                && !self.ancestors.contains(parent.index())
                && dominance_parent.replace(parent).is_some()
            {
                return false;
            }
        }

        if let Some(parent) = dominance_parent {
            if !self.graph.node(node).tree_children().is_empty()
                || self.graph.tree_parent(parent).is_some()
            {
                return false;
            }
        }

        let child_count = self.graph.node(node).tree_children().len();
        for index in 0..child_count {
            let child = self.graph.node(node).tree_children()[index];
            if !self.subgraph.contains(self.graph, child) {
                continue;
            }
            if self.nodes.contains(child.index()) {
                return false;
            }
            self.ancestors.insert(node.index());
            let ok = self.visit(child);
            self.ancestors.remove(node.index());
            if !ok {
                return false;
            }
        }

        if let Some(parent) = dominance_parent {
            self.substitutions.push((node, parent));
            if self.nodes.contains(parent.index()) {
                return false;
            }
            self.ancestors.insert(node.index());
            let ok = self.visit(parent);
            self.ancestors.remove(node.index());
            if !ok {
                return false;
            }
        }
        true
    }
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

struct SplitTraversal<'a> {
    graph: &'a HncGraph,
    subgraph: &'a Subgraph,
    root_fragment: &'a BitSet,
    path: BitSet,
    visited: BitSet,
    wcc_order: Vec<usize>,
    wccs: Vec<Option<Subgraph>>,
}

impl<'a> SplitTraversal<'a> {
    fn new(
        graph: &'a HncGraph,
        subgraph: &'a Subgraph,
        root_fragment: &'a BitSet,
        root: NodeId,
    ) -> Self {
        let node_count = graph.parsed().nodes().len();
        let mut path = BitSet::empty(node_count);
        path.insert(root.index());
        Self {
            graph,
            subgraph,
            root_fragment,
            path,
            visited: BitSet::empty(node_count),
            wcc_order: Vec::new(),
            wccs: vec![None; graph.parsed().dominance_edges().len()],
        }
    }

    fn visit(&mut self, node: NodeId, wcc_id: Option<usize>) -> bool {
        if !self.visited.insert(node.index()) {
            return false;
        }
        if !self.root_fragment.contains(node.index()) {
            let id = wcc_id.expect("nodes outside root fragment have a WCC edge");
            if self.wccs[id].is_none() {
                self.wcc_order.push(id);
                self.wccs[id] = Some(Subgraph::empty(self.graph));
            }
            self.wccs[id]
                .as_mut()
                .unwrap()
                .insert_fragment_of(self.graph, node);
        }

        if let Some(parent) = self.graph.tree_parent(node) {
            if !self.traverse(
                node,
                Edge {
                    source: parent,
                    target: node,
                    kind: Kind::Tree,
                },
                wcc_id,
            ) {
                return false;
            }
        }
        let child_count = self.graph.node(node).tree_children().len();
        for index in 0..child_count {
            let child = self.graph.node(node).tree_children()[index];
            if !self.traverse(
                node,
                Edge {
                    source: node,
                    target: child,
                    kind: Kind::Tree,
                },
                wcc_id,
            ) {
                return false;
            }
        }
        let incoming_count = self.graph.incoming_dominance(node).len();
        for index in 0..incoming_count {
            let (edge_index, source) = self.graph.incoming_dominance(node)[index];
            if !self.traverse(
                node,
                Edge {
                    source,
                    target: node,
                    kind: Kind::Dominance(edge_index),
                },
                wcc_id,
            ) {
                return false;
            }
        }
        let outgoing_count = self.graph.outgoing_dominance(node).len();
        for index in 0..outgoing_count {
            let (edge_index, target) = self.graph.outgoing_dominance(node)[index];
            if !self.traverse(
                node,
                Edge {
                    source: node,
                    target,
                    kind: Kind::Dominance(edge_index),
                },
                wcc_id,
            ) {
                return false;
            }
        }
        true
    }

    fn traverse(&mut self, node: NodeId, edge: Edge, wcc_id: Option<usize>) -> bool {
        let neighbor = edge.opposite(node);
        if !self.subgraph.contains(self.graph, neighbor) {
            return true;
        }
        if self.root_fragment.contains(neighbor.index())
            && !self.root_fragment.contains(node.index())
        {
            if !matches!(edge.kind, Kind::Dominance(_))
                || edge.source != neighbor
                || !self.path.contains(neighbor.index())
            {
                return false;
            }
        } else if !self.visited.contains(neighbor.index()) {
            if self.root_fragment.contains(node.index()) {
                if self.root_fragment.contains(neighbor.index()) {
                    self.path.insert(neighbor.index());
                    let ok = self.visit(neighbor, None);
                    self.path.remove(neighbor.index());
                    if !ok {
                        return false;
                    }
                } else {
                    let Kind::Dominance(edge_index) = edge.kind else {
                        return false;
                    };
                    if edge.source != node || !self.visit(neighbor, Some(edge_index)) {
                        return false;
                    }
                }
            } else if !self.visit(neighbor, wcc_id) {
                return false;
            }
        }
        true
    }
}

fn initialize_solution_arena(
    chart: &Chart,
) -> (
    TreeArena<SolutionNode>,
    Vec<Option<Tree>>,
    Vec<Option<(Tree, usize)>>,
) {
    let nodes = chart.graph.parsed().nodes();
    let mut arena = TreeArena::new();
    let mut handles = vec![None; nodes.len()];
    let mut hole_slots = vec![None; nodes.len()];
    if chart.empty_solution {
        return (arena, handles, hole_slots);
    }

    let placeholder_id = nodes
        .iter()
        .enumerate()
        .find(|(_, node)| !node.is_hole() && node.tree_children().is_empty())
        .map(|(index, _)| NodeId::from_index(index))
        .expect("a finite nonempty solution has a labeled leaf");
    let placeholder = arena.add_node(SolutionNode { id: placeholder_id }, Vec::new());
    handles[placeholder_id.index()] = Some(placeholder);

    for (index, node) in nodes.iter().enumerate() {
        let id = NodeId::from_index(index);
        if !node.is_hole() && id != placeholder_id {
            let children = vec![placeholder; node.tree_children().len()];
            handles[index] = Some(arena.add_node(SolutionNode { id }, children));
        }
    }
    for (parent_index, node) in nodes.iter().enumerate() {
        if node.is_hole() {
            continue;
        }
        let parent = handles[parent_index].expect("labeled nodes have arena handles");
        for (child_index, &child) in node.tree_children().iter().enumerate() {
            if nodes[child.index()].is_hole() {
                debug_assert!(hole_slots[child.index()].is_none());
                hole_slots[child.index()] = Some((parent, child_index));
            } else {
                arena.get_children_mut(parent)[child_index] =
                    handles[child.index()].expect("labeled children have arena handles");
            }
        }
    }
    (arena, handles, hole_slots)
}

#[allow(clippy::too_many_arguments)]
fn update_solution(
    chart: &Chart,
    derivation: DfsDerivation<'_>,
    changed_from: usize,
    had_current: bool,
    arena: &mut TreeArena<SolutionNode>,
    handles: &[Option<Tree>],
    hole_slots: &[Option<(Tree, usize)>],
    root: &mut Option<Tree>,
) {
    let first_changed = if had_current { changed_from } else { 0 };
    for frame in first_changed..derivation.len() {
        let node = derivation.node(frame);
        let split = &chart.split_symbols[node.symbol.0 as usize];
        for &(hole, replacement) in &split.substitutions {
            set_hole_child(hole, replacement, arena, handles, hole_slots);
        }
        if let Some((parent, child_index)) = node.parent {
            let parent_symbol = derivation.node(parent).symbol;
            let dominator =
                chart.split_symbols[parent_symbol.0 as usize].attachments[child_index].0;
            set_hole_child(dominator, split.root, arena, handles, hole_slots);
        }
    }

    let top_symbol = derivation.node(0).symbol;
    let top = chart.split_symbols[top_symbol.0 as usize].root;
    *root = handles[top.index()];
}

#[allow(clippy::too_many_arguments)]
fn set_hole_child(
    hole: NodeId,
    replacement: NodeId,
    arena: &mut TreeArena<SolutionNode>,
    handles: &[Option<Tree>],
    hole_slots: &[Option<(Tree, usize)>],
) {
    let (parent, child_index) = hole_slots[hole.index()].expect("every hole has a tree parent");
    arena.get_children_mut(parent)[child_index] =
        handles[replacement.index()].expect("split roots are labeled");
}
