//! Split-based HNC dominance-graph solving.

use num_bigint::BigUint;
use packed_term_arena::tree::{Tree, TreeArena};
use rusty_alto::{Explicit, ExplicitBuilder, StateId, Symbol, TopDownTa};
use std::collections::{HashMap, HashSet};
use std::sync::Arc;
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
struct SplitCandidate {
    root: NodeId,
    attachments: Vec<(NodeId, Subgraph)>,
    substitutions: Vec<(NodeId, NodeId)>,
}

fn build_fragment(
    graph: &HncGraph,
    root: NodeId,
    substitutions: &[(NodeId, NodeId)],
    arena: &mut TreeArena<FragmentNode>,
) -> (Tree, Vec<NodeId>) {
    fn build(
        graph: &HncGraph,
        node: NodeId,
        substitutions: &HashMap<NodeId, NodeId>,
        arena: &mut TreeArena<FragmentNode>,
        sockets: &mut Vec<NodeId>,
    ) -> Tree {
        let graph_node = graph.node(node);
        assert!(
            !graph_node.is_hole(),
            "a fragment context has no hole at its root"
        );
        let children = graph_node
            .tree_children()
            .iter()
            .map(|&child| {
                if graph.node(child).is_hole() {
                    if let Some(&replacement) = substitutions.get(&child) {
                        build(graph, replacement, substitutions, arena, sockets)
                    } else {
                        sockets.push(child);
                        arena.add_node(FragmentNode::Hole(child), Vec::new())
                    }
                } else {
                    build(graph, child, substitutions, arena, sockets)
                }
            })
            .collect();
        arena.add_node(FragmentNode::Node(node), children)
    }

    let substitutions = substitutions.iter().copied().collect::<HashMap<_, _>>();
    let mut sockets = Vec::new();
    let tree = build(graph, root, &substitutions, arena, &mut sockets);
    (tree, sockets)
}

fn format_fragment(arena: &TreeArena<FragmentNode>, tree: Tree, graph: &HncGraph) -> String {
    fn write(arena: &TreeArena<FragmentNode>, tree: Tree, graph: &HncGraph, output: &mut String) {
        match arena.get_label(tree) {
            FragmentNode::Hole(hole) => output.push_str(graph.node(*hole).name()),
            FragmentNode::Node(node) => {
                let graph_node = graph.node(*node);
                output.push_str(graph_node.label().expect("fragment nodes are labeled"));
                let children = arena.get_children(tree);
                if !children.is_empty() {
                    output.push('(');
                    for (index, child) in children.iter().enumerate() {
                        if index > 0 {
                            output.push_str(", ");
                        }
                        write(arena, *child, graph, output);
                    }
                    output.push(')');
                }
            }
        }
    }

    let mut output = String::new();
    write(arena, tree, graph, &mut output);
    output
}

fn fragment_holes(arena: &TreeArena<FragmentNode>, root: Tree) -> Vec<NodeId> {
    fn collect(arena: &TreeArena<FragmentNode>, tree: Tree, holes: &mut Vec<NodeId>) {
        match arena.get_label(tree) {
            FragmentNode::Hole(hole) => holes.push(*hole),
            FragmentNode::Node(_) => {
                for &child in arena.get_children(tree) {
                    collect(arena, child, holes);
                }
            }
        }
    }

    let mut holes = Vec::new();
    collect(arena, root, &mut holes);
    holes
}

/// Label in a node-specific fragment context used as a ranked chart terminal.
#[derive(Clone, Debug, PartialEq, Eq, Hash)]
pub enum FragmentNode {
    /// A labeled node of the source graph.
    Node(NodeId),
    /// An open source-graph hole filled by an automaton child.
    Hole(NodeId),
}

/// Display metadata shared by the rules of one chart state.
#[derive(Clone, Debug, PartialEq, Eq)]
pub struct ChartState {
    /// Dense automaton-state identity, stable for the lifetime of the chart.
    pub state: u32,
    /// Number of rules in the state group.
    pub rule_count: usize,
    /// Nodes of the source subgraph.
    pub subgraph: Vec<String>,
    /// Variant among filtered states with the same source subgraph.
    pub variant: Option<u32>,
}

/// One readable rule in a split chart.
#[derive(Clone, Debug, PartialEq, Eq)]
pub struct ChartRule {
    /// Dense automaton-state identity, stable for the lifetime of the chart.
    pub state: u32,
    /// One-based rule ordinal within the state group.
    pub ordinal: usize,
    /// Complete readable top fragment context.
    pub fragment: String,
    /// Hole and child-subgraph pairs, in fragment-socket order.
    pub assignments: Vec<(String, Vec<String>)>,
}

/// One lazily elaborated range of chart rows and its state definitions.
#[derive(Clone, Debug, PartialEq, Eq)]
pub struct ChartRulePage {
    /// Logical index of the first returned row.
    pub start: usize,
    /// Total number of logical chart rows.
    pub total: usize,
    /// State definitions referenced by `rules`, each included once.
    pub states: Vec<ChartState>,
    /// Complete rules in the requested range.
    pub rules: Vec<ChartRule>,
}

/// Indexing and grouping data owned by a chart view.
pub struct ChartDisplay {
    offsets: Vec<usize>,
    variants: Vec<Option<u32>>,
    subgraph_count: usize,
}

/// Compact tree automaton whose rules are free-root splits.
pub struct FragmentAutomaton {
    automaton: Explicit,
    fragment_arena: Arc<TreeArena<FragmentNode>>,
    fragment_roots: Arc<[Tree]>,
    state_subgraphs: Vec<Subgraph>,
}

impl FragmentAutomaton {
    /// Underlying explicit tree automaton.
    #[must_use]
    pub const fn automaton(&self) -> &Explicit {
        &self.automaton
    }

    /// Shared arena containing every fragment terminal.
    #[must_use]
    pub fn fragment_arena(&self) -> &TreeArena<FragmentNode> {
        &self.fragment_arena
    }

    /// Root of the fragment terminal denoted by `symbol`.
    #[must_use]
    pub fn fragment_root(&self, symbol: Symbol) -> Tree {
        self.fragment_roots[symbol.0 as usize]
    }

    fn source_subgraph(&self, state: StateId) -> &Subgraph {
        &self.state_subgraphs[state.index()]
    }
}

/// A solved dominance graph represented by a fragment automaton.
pub struct Chart {
    fragment_automaton: FragmentAutomaton,
    derivation_plan: DfsLanguagePlan,
    graph: Arc<HncGraph>,
    count: BigUint,
}

/// Compact chart view used by the graph-layout implementation.
pub(crate) struct LayoutChart {
    pub(crate) top_states: Vec<usize>,
    pub(crate) states: Vec<LayoutChartState>,
}

pub(crate) struct LayoutChartState {
    pub(crate) fragments: Vec<NodeId>,
    pub(crate) splits: Vec<LayoutChartSplit>,
}

pub(crate) struct LayoutChartSplit {
    pub(crate) root: NodeId,
    pub(crate) dominators: Vec<NodeId>,
    pub(crate) children: Vec<usize>,
}

impl Chart {
    /// Underlying bottom-up tree automaton.
    #[must_use]
    pub const fn fragment_automaton(&self) -> &FragmentAutomaton {
        &self.fragment_automaton
    }

    /// Number of automaton states (subgraphs).
    #[must_use]
    pub fn state_count(&self) -> usize {
        self.fragment_automaton.automaton.num_states() as usize
    }

    /// Number of split transitions.
    #[must_use]
    pub fn split_count(&self) -> usize {
        self.fragment_automaton.automaton.num_rules()
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
            inner: self.derivation_plan.iter(),
            fragments: make_solution_fragments(self),
            arena,
            handles,
            hole_slots,
            root: None,
            current: false,
        }
    }

    pub fn graph(&self) -> &HncGraph {
        &self.graph
    }

    /// Readable forms of every distinct top fragment used by chart rules.
    #[must_use]
    pub fn top_fragments(&self) -> Vec<String> {
        let mut symbols = self
            .fragment_automaton
            .automaton()
            .rules()
            .map(|rule| rule.symbol)
            .collect::<Vec<_>>();
        symbols.sort_unstable_by_key(|symbol| symbol.0);
        symbols.dedup();
        let mut fragments = symbols
            .into_iter()
            .map(|symbol| {
                format_fragment(
                    self.fragment_automaton.fragment_arena(),
                    self.fragment_automaton.fragment_root(symbol),
                    &self.graph,
                )
            })
            .collect::<Vec<_>>();
        fragments.sort();
        fragments.dedup();
        fragments
    }

    pub(crate) fn layout_chart(&self) -> LayoutChart {
        let automaton = self.fragment_automaton.automaton();
        let arena = self.fragment_automaton.fragment_arena();
        let graph = self.graph();
        let mut top_states = Vec::new();
        automaton.initial_states(&mut |state| top_states.push(state.index()));
        top_states.sort_unstable();

        let states = (0..automaton.num_states())
            .map(|state_index| {
                let state = StateId(state_index);
                let fragments = self
                    .source_subgraph(state)
                    .0
                    .members()
                    .map(|fragment| graph.roots()[fragment])
                    .collect();
                let splits = automaton
                    .rules_topdown(state)
                    .map(|rule| {
                        let terminal = self.fragment_automaton.fragment_root(rule.symbol);
                        let root = match arena.get_label(terminal) {
                            FragmentNode::Node(root) => *root,
                            FragmentNode::Hole(_) => {
                                unreachable!("a chart terminal is rooted in a labeled fragment")
                            }
                        };
                        let dominators = fragment_holes(arena, terminal);
                        LayoutChartSplit {
                            root: graph.roots()[graph.fragment_of(root)],
                            dominators,
                            children: rule.children.iter().map(|state| state.index()).collect(),
                        }
                    })
                    .collect();
                LayoutChartState { fragments, splits }
            })
            .collect();
        LayoutChart { top_states, states }
    }

    fn source_subgraph(&self, state: StateId) -> &Subgraph {
        self.fragment_automaton.source_subgraph(state)
    }

    pub(crate) fn from_filtered_automaton(
        source: &Self,
        automaton: Explicit,
        source_states: &[StateId],
    ) -> Self {
        assert_eq!(automaton.num_states() as usize, source_states.len());
        let subgraphs = source_states
            .iter()
            .map(|state| source.source_subgraph(*state).clone())
            .collect::<Vec<_>>();
        let count = count_automaton(&automaton);
        let derivation_plan = DfsLanguagePlan::new(&automaton)
            .expect("filtered charts retain an acyclic productive state graph");
        Self {
            fragment_automaton: FragmentAutomaton {
                automaton,
                fragment_arena: Arc::clone(&source.fragment_automaton.fragment_arena),
                fragment_roots: Arc::clone(&source.fragment_automaton.fragment_roots),
                state_subgraphs: subgraphs,
            },
            derivation_plan,
            graph: Arc::clone(&source.graph),
            count,
        }
    }

    pub(crate) fn empty_filter_result(source: &Self) -> Self {
        let automaton = ExplicitBuilder::new().build();
        let derivation_plan =
            DfsLanguagePlan::new(&automaton).expect("an empty automaton has no productive cycle");
        Self {
            fragment_automaton: FragmentAutomaton {
                automaton,
                fragment_arena: Arc::clone(&source.fragment_automaton.fragment_arena),
                fragment_roots: Arc::clone(&source.fragment_automaton.fragment_roots),
                state_subgraphs: Vec::new(),
            },
            derivation_plan,
            graph: Arc::clone(&source.graph),
            count: BigUint::from(0_u8),
        }
    }
}

impl ChartDisplay {
    /// Build the display index and filtered-state grouping for `chart`.
    #[must_use]
    pub fn new(chart: &Chart) -> Self {
        let offsets = make_display_offsets(chart.fragment_automaton().automaton());
        let mut totals = HashMap::<&Subgraph, usize>::new();
        for subgraph in &chart.fragment_automaton.state_subgraphs {
            *totals.entry(subgraph).or_default() += 1;
        }
        let mut seen = HashMap::<&Subgraph, u32>::new();
        let variants = chart
            .fragment_automaton
            .state_subgraphs
            .iter()
            .map(|subgraph| {
                if totals[subgraph] == 1 {
                    None
                } else {
                    let variant = seen.entry(subgraph).or_default();
                    *variant += 1;
                    Some(*variant)
                }
            })
            .collect();
        Self {
            offsets,
            variants,
            subgraph_count: totals.len(),
        }
    }

    /// Number of distinct source subgraphs represented by chart states.
    #[must_use]
    pub const fn subgraph_count(&self) -> usize {
        self.subgraph_count
    }

    /// Number of logical rule rows in the display.
    #[must_use]
    pub fn row_count(&self) -> usize {
        self.offsets.last().copied().unwrap_or(0)
    }

    /// Elaborate a stable range of chart-display rows.
    ///
    /// # Panics
    ///
    /// Panics only if the chart contains more states than Alto can represent.
    #[must_use]
    pub fn rule_page(&self, chart: &Chart, start: usize, count: usize) -> ChartRulePage {
        assert_eq!(self.offsets.len(), chart.state_count() + 1);
        let end = start.saturating_add(count).min(self.row_count());
        if start >= end {
            return ChartRulePage {
                start,
                total: self.row_count(),
                states: Vec::new(),
                rules: Vec::new(),
            };
        }
        let name = |node: NodeId| chart.graph.node(node).name().to_owned();
        let mut rows = Vec::with_capacity(end - start);
        let mut states = Vec::new();
        let mut displayed_subgraphs = HashMap::<StateId, Vec<String>>::new();
        let mut displayed_fragments = HashMap::<Symbol, (String, Vec<NodeId>)>::new();
        let mut state_index = self
            .offsets
            .partition_point(|&offset| offset <= start)
            .saturating_sub(1);
        while state_index < chart.state_count() && self.offsets[state_index] < end {
            let state = StateId(u32::try_from(state_index).expect("state count exceeds u32"));
            let state_start = self.offsets[state_index];
            let state_end = self.offsets[state_index + 1];
            states.push(ChartState {
                state: state.0,
                rule_count: state_end - state_start,
                subgraph: chart
                    .source_subgraph(state)
                    .nodes(&chart.graph)
                    .into_iter()
                    .map(name)
                    .collect(),
                variant: self.variants[state.index()],
            });
            let first = start.saturating_sub(state_start);
            let take = end.min(state_end).saturating_sub(state_start + first);
            for (ordinal, rule) in chart
                .fragment_automaton
                .automaton
                .rules_topdown(state)
                .enumerate()
                .skip(first)
                .take(take)
            {
                let (fragment, sockets) =
                    displayed_fragments.entry(rule.symbol).or_insert_with(|| {
                        let fragment_root = chart.fragment_automaton.fragment_root(rule.symbol);
                        (
                            format_fragment(
                                chart.fragment_automaton.fragment_arena(),
                                fragment_root,
                                &chart.graph,
                            ),
                            fragment_holes(
                                chart.fragment_automaton.fragment_arena(),
                                fragment_root,
                            ),
                        )
                    });
                rows.push(ChartRule {
                    state: rule.result.0,
                    ordinal: ordinal + 1,
                    fragment: fragment.clone(),
                    assignments: sockets
                        .iter()
                        .copied()
                        .zip(rule.children.iter().copied())
                        .map(|(hole, child)| {
                            let child_subgraph = displayed_subgraphs
                                .entry(child)
                                .or_insert_with(|| {
                                    chart
                                        .source_subgraph(child)
                                        .nodes(&chart.graph)
                                        .into_iter()
                                        .map(name)
                                        .collect()
                                })
                                .clone();
                            (name(hole), child_subgraph)
                        })
                        .collect(),
                });
            }
            state_index += 1;
        }
        ChartRulePage {
            start,
            total: self.row_count(),
            states,
            rules: rows,
        }
    }
}

fn make_display_offsets(automaton: &Explicit) -> Vec<usize> {
    let mut offsets = Vec::with_capacity(automaton.num_states() as usize + 1);
    offsets.push(0);
    for state in 0..automaton.num_states() {
        let count = automaton.rules_topdown(StateId(state)).count();
        offsets.push(offsets.last().copied().unwrap_or(0) + count);
    }
    offsets
}

fn count_automaton(automaton: &Explicit) -> BigUint {
    fn count_state(
        automaton: &Explicit,
        state: StateId,
        visiting: &mut HashSet<StateId>,
        memo: &mut HashMap<StateId, BigUint>,
    ) -> BigUint {
        if let Some(count) = memo.get(&state) {
            return count.clone();
        }
        assert!(
            visiting.insert(state),
            "a finite chart cannot contain a productive cycle"
        );
        let mut total = BigUint::from(0_u8);
        for rule in automaton.rules_topdown(state) {
            let mut here = BigUint::from(1_u8);
            for &child in rule.children {
                here *= count_state(automaton, child, visiting, memo);
            }
            total += here;
        }
        visiting.remove(&state);
        memo.insert(state, total.clone());
        total
    }

    let mut total = BigUint::from(0_u8);
    let mut visiting = HashSet::new();
    let mut memo = HashMap::new();
    automaton.initial_states(&mut |state| {
        total += count_state(automaton, state, &mut visiting, &mut memo);
    });
    total
}

/// One fully resolved tree borrowing the iterator's reusable arena.
#[derive(Clone, Copy)]
pub struct Solution<'a> {
    chart: &'a Chart,
    arena: &'a TreeArena<NodeId>,
    root: Tree,
}

impl Solution<'_> {
    /// Source graph whose labeled nodes form this solution.
    #[must_use]
    pub fn graph(&self) -> &HncGraph {
        self.chart.graph()
    }

    /// Tree storage.
    #[must_use]
    pub const fn arena(&self) -> &TreeArena<NodeId> {
        self.arena
    }

    /// Original graph identity represented by an arena node.
    #[must_use]
    pub fn node_id(&self, tree: Tree) -> NodeId {
        *self.arena.get_label(tree)
    }

    /// External name of an arena node.
    #[must_use]
    pub fn node_name(&self, tree: Tree) -> &str {
        self.chart.graph.node(self.node_id(tree)).name()
    }

    /// Semantic label of an arena node.
    ///
    /// # Panics
    ///
    /// Panics if `tree` does not represent a labeled solution node.
    #[must_use]
    pub fn node_label(&self, tree: Tree) -> &str {
        self.chart
            .graph
            .node(self.node_id(tree))
            .label()
            .expect("solution nodes are labeled")
    }

    /// Root handle.
    #[must_use]
    pub const fn root(&self) -> Tree {
        self.root
    }

    /// Semantic term using graph labels only.
    #[must_use]
    pub fn to_term(&self) -> String {
        self.to_label_term(",")
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
        let mut output = String::new();
        write(self, self.root, separator, &mut output);
        output
    }
}

/// Streaming solution cursor backed by finite depth-first chart enumeration.
///
/// Every labeled graph node has one stable arena handle. Advancing changes only
/// the child slots represented by fragment sockets and the current root.
pub struct Solutions<'a> {
    chart: &'a Chart,
    inner: DfsLanguageIterator<'a>,
    fragments: Vec<SolutionFragment>,
    arena: TreeArena<NodeId>,
    handles: Vec<Option<Tree>>,
    hole_slots: Vec<Option<(Tree, usize)>>,
    root: Option<Tree>,
    current: bool,
}

impl Solutions<'_> {
    /// Advance to the next solution, invalidating the previous one.
    ///
    /// # Panics
    ///
    /// Panics if the chart violates the solver's internal derivation invariants.
    pub fn advance(&mut self) -> bool {
        if !self.inner.advance() {
            self.current = false;
            return false;
        }
        update_solution(
            &self.fragments,
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

    /// Borrow the current solution until the next mutable cursor access.
    #[must_use]
    pub fn current(&self) -> Option<Solution<'_>> {
        if !self.current {
            return None;
        }
        Some(Solution {
            chart: self.chart,
            arena: &self.arena,
            root: self.root?,
        })
    }
}

/// Solver failure.
#[derive(Debug, Error)]
pub enum SolveError {
    /// Empty dominance graphs do not have a tree-shaped solution.
    #[error("cannot solve an empty dominance graph")]
    EmptyGraph,
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
///
/// # Errors
///
/// Returns an error if the graph cannot be compiled into a valid chart.
pub fn solve(graph: &HncGraph) -> Result<Chart, SolveError> {
    solve_with_cancellation(graph, || false)
}

/// Decide solvability by retaining only the first successful split of each subgraph.
///
/// Unlike [`solve`], this does not construct a chart or count solved forms.
#[must_use]
pub fn is_solvable(graph: &HncGraph) -> bool {
    if graph.parsed().nodes().is_empty() {
        return false;
    }
    SolvabilityCompiler {
        graph,
        memo: HashMap::new(),
    }
    .check(&Subgraph::all(graph))
}

/// Construct a chart, checking `cancelled` between split-expansion steps.
///
/// # Errors
///
/// Returns [`SolveError::Cancelled`] if `cancelled` requests cancellation, or
/// another solver error if chart construction fails.
///
/// # Panics
///
/// Panics if an internal chart-construction invariant is violated.
pub fn solve_with_cancellation(
    graph: &HncGraph,
    cancelled: impl Fn() -> bool,
) -> Result<Chart, SolveError> {
    if cancelled() {
        return Err(SolveError::Cancelled);
    }
    if graph.parsed().nodes().is_empty() {
        return Err(SolveError::EmptyGraph);
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
        fragment_automaton: FragmentAutomaton {
            automaton,
            fragment_arena: Arc::new(compiler.fragment_arena),
            fragment_roots: compiler.fragment_roots.into(),
            state_subgraphs: compiler.subgraphs,
        },
        derivation_plan,
        graph: Arc::new(graph.clone()),
        count,
    })
}

struct Compiler<'a> {
    graph: &'a HncGraph,
    builder: ExplicitBuilder,
    states: HashMap<Subgraph, StateId>,
    counts: Vec<Option<BigUint>>,
    subgraphs: Vec<Subgraph>,
    fragment_arena: TreeArena<FragmentNode>,
    fragment_roots: Vec<Tree>,
    fragment_sockets: Vec<Box<[NodeId]>>,
    fragment_symbols: HashMap<(NodeId, Vec<(NodeId, NodeId)>), Symbol>,
}

struct SolvabilityCompiler<'a> {
    graph: &'a HncGraph,
    memo: HashMap<Subgraph, bool>,
}

impl SolvabilityCompiler<'_> {
    fn check(&mut self, subgraph: &Subgraph) -> bool {
        if let Some(&solvable) = self.memo.get(subgraph) {
            return solvable;
        }

        let graph = self.graph;
        let mut candidates = SplitCandidates::new(graph, subgraph);
        while let Some(candidate) = candidates
            .next(&|| false)
            .expect("solvability checking is never cancelled")
        {
            if candidate
                .attachments
                .iter()
                .all(|(_, child)| self.check(child))
            {
                self.memo.insert(subgraph.clone(), true);
                return true;
            }
        }

        self.memo.insert(subgraph.clone(), false);
        false
    }
}

impl<'a> Compiler<'a> {
    fn new(graph: &'a HncGraph) -> Self {
        Self {
            graph,
            builder: ExplicitBuilder::new(),
            states: HashMap::new(),
            counts: Vec::new(),
            subgraphs: Vec::new(),
            fragment_arena: TreeArena::new(),
            fragment_roots: Vec::new(),
            fragment_sockets: Vec::new(),
            fragment_symbols: HashMap::new(),
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
        let graph = self.graph;
        let mut candidates = SplitCandidates::new(graph, subgraph);
        while let Some(candidate) = candidates.next(cancelled)? {
            let mut children = Vec::with_capacity(candidate.attachments.len());
            let mut split_count = BigUint::from(1_u8);
            for (hole, child) in candidate.attachments {
                let (child_state, child_count) = self.compile(&child, cancelled)?;
                children.push((hole, child_state));
                split_count *= child_count;
            }
            if split_count == BigUint::from(0_u8) {
                continue;
            }

            let mut substitutions = candidate.substitutions.clone();
            substitutions.sort_unstable();
            let fragment_key = (candidate.root, substitutions);
            let symbol = if let Some(&symbol) = self.fragment_symbols.get(&fragment_key) {
                symbol
            } else {
                let symbol = Symbol(
                    u32::try_from(self.fragment_roots.len())
                        .expect("fragment count exceeds symbol capacity"),
                );
                let (root, sockets) = build_fragment(
                    self.graph,
                    candidate.root,
                    &candidate.substitutions,
                    &mut self.fragment_arena,
                );
                self.fragment_roots.push(root);
                self.fragment_sockets.push(sockets.into_boxed_slice());
                self.fragment_symbols.insert(fragment_key, symbol);
                debug_assert_eq!(
                    self.fragment_sockets[symbol.0 as usize].len(),
                    children.len()
                );
                symbol
            };
            let sockets = &self.fragment_sockets[symbol.0 as usize];
            assert_eq!(
                sockets.len(),
                children.len(),
                "every open fragment socket has one child subgraph"
            );
            for (socket_index, &hole) in sockets.iter().enumerate() {
                let child_index = children[socket_index..]
                    .iter()
                    .position(|(dominator, _)| *dominator == hole)
                    .map_or_else(
                        || {
                            panic!(
                                "open fragment hole {} has no child subgraph",
                                self.graph.node(hole).name()
                            )
                        },
                        |offset| socket_index + offset,
                    );
                children.swap(socket_index, child_index);
            }
            let child_states = children
                .into_iter()
                .map(|(_, child_state)| child_state)
                .collect();
            self.builder.add_rule(symbol, child_states, state);
            total += split_count;
        }

        self.counts[state.0 as usize] = Some(total.clone());
        Ok((state, total))
    }
}

struct SplitCandidates<'a> {
    graph: &'a HncGraph,
    subgraph: &'a Subgraph,
    next_root: usize,
}

impl<'a> SplitCandidates<'a> {
    const fn new(graph: &'a HncGraph, subgraph: &'a Subgraph) -> Self {
        Self {
            graph,
            subgraph,
            next_root: 0,
        }
    }

    fn next(
        &mut self,
        cancelled: &impl Fn() -> bool,
    ) -> Result<Option<SplitCandidate>, SolveError> {
        while let Some(&root) = self.graph.roots().get(self.next_root) {
            self.next_root += 1;
            if cancelled() {
                return Err(SolveError::Cancelled);
            }
            if self.subgraph.contains(self.graph, root)
                && indegree_in(self.graph, root, self.subgraph) == 0
                && let Some(candidate) = compute_split(self.graph, root, self.subgraph)
            {
                return Ok(Some(candidate));
            }
        }
        Ok(None)
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

type SolutionArena = (
    TreeArena<NodeId>,
    Vec<Option<Tree>>,
    Vec<Option<(Tree, usize)>>,
);

struct SolutionFragment {
    root: NodeId,
    sockets: Box<[NodeId]>,
    internal_links: Box<[(NodeId, NodeId)]>,
}

fn make_solution_fragments(chart: &Chart) -> Vec<SolutionFragment> {
    fn collect(
        graph: &HncGraph,
        arena: &TreeArena<FragmentNode>,
        tree: Tree,
        sockets: &mut Vec<NodeId>,
        internal_links: &mut Vec<(NodeId, NodeId)>,
    ) {
        match arena.get_label(tree) {
            FragmentNode::Hole(hole) => sockets.push(*hole),
            FragmentNode::Node(parent_node) => {
                for (position, &child) in arena.get_children(tree).iter().enumerate() {
                    let source_child = graph.node(*parent_node).tree_children()[position];
                    if let FragmentNode::Node(child_node) = arena.get_label(child)
                        && graph.node(source_child).is_hole()
                    {
                        internal_links.push((source_child, *child_node));
                    }
                    collect(graph, arena, child, sockets, internal_links);
                }
            }
        }
    }

    let arena = chart.fragment_automaton.fragment_arena();
    chart
        .fragment_automaton
        .fragment_roots
        .iter()
        .copied()
        .map(|tree| {
            let mut sockets = Vec::new();
            let mut internal_links = Vec::new();
            collect(
                chart.graph(),
                arena,
                tree,
                &mut sockets,
                &mut internal_links,
            );
            SolutionFragment {
                root: fragment_root_node(arena, tree),
                sockets: sockets.into_boxed_slice(),
                internal_links: internal_links.into_boxed_slice(),
            }
        })
        .collect()
}

fn initialize_solution_arena(chart: &Chart) -> SolutionArena {
    let nodes = chart.graph.parsed().nodes();
    let mut arena = TreeArena::new();
    let mut handles = vec![None; nodes.len()];
    let mut hole_slots = vec![None; nodes.len()];
    let placeholder_id = nodes
        .iter()
        .enumerate()
        .find(|(_, node)| !node.is_hole() && node.tree_children().is_empty())
        .map(|(index, _)| NodeId::from_index(index))
        .expect("a finite nonempty solution has a labeled leaf");
    let placeholder = arena.add_node(placeholder_id, Vec::new());
    handles[placeholder_id.index()] = Some(placeholder);

    for (index, node) in nodes.iter().enumerate() {
        let id = NodeId::from_index(index);
        if !node.is_hole() && id != placeholder_id {
            let children = vec![placeholder; node.tree_children().len()];
            handles[index] = Some(arena.add_node(id, children));
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
    fragments: &[SolutionFragment],
    derivation: DfsDerivation<'_>,
    changed_from: usize,
    had_current: bool,
    arena: &mut TreeArena<NodeId>,
    handles: &[Option<Tree>],
    hole_slots: &[Option<(Tree, usize)>],
    root: &mut Option<Tree>,
) {
    let first_changed = if had_current { changed_from } else { 0 };
    for frame in first_changed..derivation.len() {
        let node = derivation.node(frame);
        let fragment = &fragments[node.symbol.0 as usize];
        for &(hole, replacement) in &fragment.internal_links {
            set_hole_child(hole, replacement, arena, handles, hole_slots);
        }
        if let Some((parent, child_index)) = node.parent {
            let parent_symbol = derivation.node(parent).symbol;
            let hole = fragments[parent_symbol.0 as usize].sockets[child_index];
            set_hole_child(hole, fragment.root, arena, handles, hole_slots);
        }
    }

    let top_symbol = derivation.node(0).symbol;
    let top = fragments[top_symbol.0 as usize].root;
    *root = handles[top.index()];
}

fn fragment_root_node(arena: &TreeArena<FragmentNode>, tree: Tree) -> NodeId {
    let FragmentNode::Node(node) = arena.get_label(tree) else {
        unreachable!("a fragment context has a labeled root")
    };
    *node
}

#[allow(clippy::too_many_arguments)]
fn set_hole_child(
    hole: NodeId,
    replacement: NodeId,
    arena: &mut TreeArena<NodeId>,
    handles: &[Option<Tree>],
    hole_slots: &[Option<(Tree, usize)>],
) {
    let (parent, child_index) = hole_slots[hole.index()].expect("every hole has a tree parent");
    arena.get_children_mut(parent)[child_index] =
        handles[replacement.index()].expect("split roots are labeled");
}
