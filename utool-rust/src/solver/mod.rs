//! Split-based HNC dominance-graph solving.
//!
//! The solver represents a subproblem as a set of complete tree fragments. For
//! every subproblem it tries each free fragment root, computes the weakly
//! connected components left below that root, and recursively solves those
//! components. A successful split becomes a rule in a finite tree automaton;
//! the automaton therefore represents all solved forms without materializing
//! every solution tree.

use num_bigint::BigUint;
use packed_term_arena::tree::{Tree, TreeArena};
use rusty_alto::{
    Derivation, Explicit, ExplicitBuilder, FiniteLanguageIterator, FiniteLanguagePlan,
    LanguageCardinality, StateId, Symbol, TopDownTa,
};
use std::collections::HashMap;
use std::sync::Arc;
use thiserror::Error;

use crate::graph::{HncGraph, NodeId};

/// Fixed-size bit set used for node and fragment membership during solving.
#[derive(Clone, Debug, PartialEq, Eq, Hash)]
struct BitSet(Vec<u64>);

impl BitSet {
    /// Create an empty set for indices in `0..universe`.
    fn empty(universe: usize) -> Self {
        Self(vec![0; universe.div_ceil(64)])
    }

    /// Create a set containing every index in `0..universe`.
    fn full(universe: usize) -> Self {
        let mut set = Self(vec![u64::MAX; universe.div_ceil(64)]);
        let excess = set.0.len() * 64 - universe;
        if let Some(last) = set.0.last_mut() {
            *last >>= excess;
        }
        set
    }

    /// Return whether `index` belongs to the set.
    fn contains(&self, index: usize) -> bool {
        self.0[index / 64] & (1 << (index % 64)) != 0
    }

    /// Add `index`, returning whether it was absent before the call.
    fn insert(&mut self, index: usize) -> bool {
        let bit = 1 << (index % 64);
        let word = &mut self.0[index / 64];
        let fresh = *word & bit == 0;
        *word |= bit;
        fresh
    }

    /// Remove `index` from the set.
    fn remove(&mut self, index: usize) {
        self.0[index / 64] &= !(1 << (index % 64));
    }

    /// Return the number of indices in the set.
    fn count(&self) -> usize {
        self.0.iter().map(|word| word.count_ones() as usize).sum()
    }

    /// Iterate over the contained indices in ascending order.
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

    /// Ensure the final storage word does not expose indices beyond the universe.
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

/// Solver subproblem represented as a union of complete tree fragments.
#[derive(Clone, Debug, PartialEq, Eq, Hash)]
struct Subgraph(BitSet);

impl Subgraph {
    /// Return the subgraph containing every fragment in `graph`.
    fn all(graph: &HncGraph) -> Self {
        Self(BitSet::full(graph.roots().len()))
    }

    /// Return an empty subgraph over the fragment universe of `graph`.
    fn empty(graph: &HncGraph) -> Self {
        Self(BitSet::empty(graph.roots().len()))
    }

    /// Return whether the complete fragment containing `node` is present.
    fn contains(&self, graph: &HncGraph, node: NodeId) -> bool {
        // Splitting only removes whole tree fragments, so every recursive
        // subgraph is a union of fragments and needs one bit per fragment.
        self.0.contains(graph.fragment_of(node))
    }

    /// Insert the complete fragment containing `node`.
    fn insert_fragment_of(&mut self, graph: &HncGraph, node: NodeId) {
        self.0.insert(graph.fragment_of(node));
    }

    /// Return all graph nodes in this subgraph in identifier order.
    fn nodes(&self, graph: &HncGraph) -> Vec<NodeId> {
        let mut nodes = self
            .0
            .members()
            .flat_map(|fragment| graph.fragment_nodes(fragment).iter().copied())
            .collect::<Vec<_>>();
        nodes.sort_unstable();
        nodes
    }

    /// Return the total number of graph nodes in the selected fragments.
    fn node_count(&self, graph: &HncGraph) -> usize {
        self.0
            .members()
            .map(|fragment| graph.fragment_nodes(fragment).len())
            .sum()
    }
}

/// One valid way to split a subgraph around a free root fragment.
#[derive(Clone, Debug)]
struct SplitCandidate {
    /// Labeled root of the fragment context at the top of the split.
    root: NodeId,
    /// Open hole and child subproblem pairs.
    attachments: Vec<(NodeId, Subgraph)>,
    /// Holes filled directly by roots folded into the top context.
    substitutions: Vec<(NodeId, NodeId)>,
}

/// Build the ranked terminal for one split and return its open holes in tree order.
fn build_fragment(
    graph: &HncGraph,
    root: NodeId,
    substitutions: &[(NodeId, NodeId)],
    arena: &mut TreeArena<FragmentNode>,
) -> (Tree, Vec<NodeId>) {
    /// Recursively copy one labeled fragment, expanding direct substitutions.
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
                // A substituted hole is expanded into the same terminal. An
                // unsubstituted hole remains a socket for an automaton child.
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

    // Hash lookup keeps substitution decisions local to the recursive copy.
    let substitutions = substitutions.iter().copied().collect::<HashMap<_, _>>();
    let mut sockets = Vec::new();
    let tree = build(graph, root, &substitutions, arena, &mut sockets);
    (tree, sockets)
}

/// Format a fragment context as a compact, human-readable term.
fn format_fragment(arena: &TreeArena<FragmentNode>, tree: Tree, graph: &HncGraph) -> String {
    /// Append one fragment subtree to `output`.
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

/// Return the open holes of a fragment context in depth-first tree order.
fn fragment_holes(arena: &TreeArena<FragmentNode>, root: Tree) -> Vec<NodeId> {
    /// Append holes below `tree` to `holes`.
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
    /// Prefix sum of rule counts, indexed by automaton state.
    offsets: Vec<usize>,
    /// Optional one-based variant number for each automaton state.
    variants: Vec<Option<u32>>,
    /// Number of distinct source subgraphs represented by all states.
    subgraph_count: usize,
}

/// Compact tree automaton whose rules are free-root splits.
pub struct FragmentAutomaton {
    /// Explicit automaton whose rules encode productive splits.
    automaton: Explicit,
    /// Shared storage for ranked fragment terminals.
    fragment_arena: Arc<TreeArena<FragmentNode>>,
    /// Terminal tree root indexed by automaton symbol.
    fragment_roots: Arc<[Tree]>,
    /// Source subgraph indexed by automaton state.
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

    /// Return the source subgraph represented by `state`.
    fn source_subgraph(&self, state: StateId) -> &Subgraph {
        &self.state_subgraphs[state.index()]
    }
}

/// A solved dominance graph represented by a fragment automaton.
pub struct Chart {
    /// Automaton containing all productive split rules.
    fragment_automaton: FragmentAutomaton,
    /// Precomputed traversal data for allocation-free derivation iteration.
    derivation_plan: FiniteLanguagePlan,
    /// Source graph shared with derived filtered charts.
    graph: Arc<HncGraph>,
    /// Exact cardinality of the automaton language.
    count: BigUint,
}

/// Compact chart view used by the graph-layout implementation.
pub(crate) struct LayoutChart {
    /// Accepting automaton states, expressed as dense indices.
    pub(crate) top_states: Vec<usize>,
    /// Layout-oriented data for every automaton state.
    pub(crate) states: Vec<LayoutChartState>,
}

/// Source fragments and outgoing splits associated with one layout state.
pub(crate) struct LayoutChartState {
    /// Roots of source fragments contained in this state.
    pub(crate) fragments: Vec<NodeId>,
    /// Productive split alternatives for this state.
    pub(crate) splits: Vec<LayoutChartSplit>,
}

/// Minimal split information needed by the chart layout algorithm.
pub(crate) struct LayoutChartSplit {
    /// Root fragment selected by this split.
    pub(crate) root: NodeId,
    /// Open holes in the split terminal.
    pub(crate) dominators: Vec<NodeId>,
    /// Child state indices in terminal-hole order.
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
        Solutions {
            chart: self,
            inner: self.derivation_plan.iter(),
            fragments: make_solution_fragments(self),
            tree: ReusableSolutionTree::new(self),
            current: false,
        }
    }

    /// Source dominance graph represented by this chart.
    #[must_use]
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

    /// Project the automaton into the smaller representation used for layout.
    pub(crate) fn layout_chart(&self) -> LayoutChart {
        let automaton = self.fragment_automaton.automaton();
        let arena = self.fragment_automaton.fragment_arena();
        let graph = self.graph();
        let mut top_states = Vec::new();
        // Preserve the automaton's accepting states as layout roots.
        automaton.initial_states(&mut |state| top_states.push(state.index()));
        top_states.sort_unstable();

        let states = (0..automaton.num_states())
            .map(|state_index| {
                let state = StateId(state_index);
                // Convert the state's fragment bit set back to graph roots.
                let fragments = self
                    .source_subgraph(state)
                    .0
                    .members()
                    .map(|fragment| graph.roots()[fragment])
                    .collect();
                let splits = automaton
                    .rules_topdown(state)
                    .map(|rule| {
                        // Each automaton rule becomes one layout split. The
                        // terminal supplies its root and ordered open holes.
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

    /// Return the source subgraph represented by `state`.
    fn source_subgraph(&self, state: StateId) -> &Subgraph {
        self.fragment_automaton.source_subgraph(state)
    }

    /// Rebuild a chart around a filtered automaton and its source-state mapping.
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
        let derivation_plan = FiniteLanguagePlan::new(&automaton)
            .expect("filtered charts retain an acyclic productive state graph");
        let count = match derivation_plan.language_cardinality_as::<BigUint>() {
            LanguageCardinality::Finite(count) => count,
            LanguageCardinality::Infinite => {
                panic!("filtered charts have finite derivation languages")
            }
            LanguageCardinality::TooLarge => {
                unreachable!("BigUint cardinality cannot overflow")
            }
        };
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

    /// Construct an empty chart that retains the source graph and fragment arena.
    pub(crate) fn empty_filter_result(source: &Self) -> Self {
        let automaton = ExplicitBuilder::new().build();
        let derivation_plan = FiniteLanguagePlan::new(&automaton)
            .expect("an empty automaton has no productive cycle");
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
        // Prefix sums make state-to-row and row-to-state lookup logarithmic.
        let offsets = make_display_offsets(chart.fragment_automaton().automaton());

        // Filtering can produce several states for the same source subgraph.
        // Count them first, then assign display variants in state order.
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
        // Clamp the requested half-open range to the logical rule table.
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

        // Locate the first overlapping state through the rule-count prefix sum.
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

            // Translate the global page bounds into a slice of this state's rules.
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
                // Formatting terminals and child subgraphs is comparatively
                // expensive, so elaborate each referenced object only once.
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

/// Build prefix sums mapping automaton states to logical display rows.
fn make_display_offsets(automaton: &Explicit) -> Vec<usize> {
    let mut offsets = Vec::with_capacity(automaton.num_states() as usize + 1);
    offsets.push(0);
    for state in 0..automaton.num_states() {
        let count = automaton.rules_topdown(StateId(state)).count();
        offsets.push(offsets.last().copied().unwrap_or(0) + count);
    }
    offsets
}

/// One fully resolved tree borrowing the iterator's reusable arena.
#[derive(Clone, Copy)]
pub struct Solution<'a> {
    /// Owning chart, used to resolve graph metadata.
    chart: &'a Chart,
    /// Reusable tree arena owned by the solution cursor.
    arena: &'a TreeArena<NodeId>,
    /// Root handle of this solved form.
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
        /// Append one solution subtree to `output`.
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
    /// Chart whose language is being enumerated.
    chart: &'a Chart,
    /// Depth-first automaton-language cursor.
    inner: FiniteLanguageIterator<'a>,
    /// Rewiring instructions indexed by terminal symbol.
    fragments: Vec<SolutionFragment>,
    /// Arena and lookup tables reused between solutions.
    tree: ReusableSolutionTree,
    /// Whether the cursor currently points at a solution.
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
        self.tree.apply_derivation(
            &self.fragments,
            self.inner.current().expect("advance produced a derivation"),
            self.inner
                .changed_from()
                .expect("advance reported a changed derivation"),
            self.current,
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
            arena: &self.tree.arena,
            root: self.tree.root?,
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

    // Compile the complete fragment set as the root dynamic-programming state.
    let mut compiler = Compiler::new(graph);
    let top = Subgraph::all(graph);
    let (top_state, count) = compiler.compile(&top, &cancelled)?;
    if count != BigUint::from(0_u8) {
        compiler.builder.add_accepting(top_state);
    }

    // Freeze the chart and precompute the plan used by every solution cursor.
    let automaton = compiler.builder.build();
    let derivation_plan = FiniteLanguagePlan::new(&automaton)
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

/// Memoized compiler from fragment subgraphs to tree-automaton states.
///
/// State IDs, count slots, and `subgraphs` use the same dense index. A count is
/// `None` while that state is being expanded; encountering such a state again
/// denotes a non-productive recursive dependency and contributes zero solved
/// forms, matching the finite-chart semantics.
struct Compiler<'a> {
    /// Validated HNC graph being compiled.
    graph: &'a HncGraph,
    /// Incrementally constructed explicit tree automaton.
    builder: ExplicitBuilder,
    /// Canonical automaton state for every encountered subgraph.
    states: HashMap<Subgraph, StateId>,
    /// Completed solution count by state, or `None` during expansion.
    counts: Vec<Option<BigUint>>,
    /// Source subgraph by dense state index.
    subgraphs: Vec<Subgraph>,
    /// Shared tree storage for fragment terminals.
    fragment_arena: TreeArena<FragmentNode>,
    /// Fragment root by dense terminal-symbol index.
    fragment_roots: Vec<Tree>,
    /// Open holes by dense terminal-symbol index.
    fragment_sockets: Vec<Box<[NodeId]>>,
    /// Intern table from canonical top contexts to terminal symbols.
    fragment_symbols: HashMap<(NodeId, Vec<(NodeId, NodeId)>), Symbol>,
}

/// Lightweight existence checker that stops after the first productive split.
struct SolvabilityCompiler<'a> {
    /// Validated HNC graph being checked.
    graph: &'a HncGraph,
    /// Solvability result for every completed recursive subproblem.
    memo: HashMap<Subgraph, bool>,
}

impl SolvabilityCompiler<'_> {
    /// Return whether `subgraph` has at least one recursively solvable split.
    fn check(&mut self, subgraph: &Subgraph) -> bool {
        // Reuse completed subproblems shared by different split choices.
        if let Some(&solvable) = self.memo.get(subgraph) {
            return solvable;
        }

        // A subgraph is solvable as soon as one candidate has only solvable
        // attachment components; no automaton rules or counts are needed.
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

        // Exhausting all free roots proves this subproblem unsolvable.
        self.memo.insert(subgraph.clone(), false);
        false
    }
}

impl<'a> Compiler<'a> {
    /// Create an empty compiler for `graph`.
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

    /// Compile `subgraph`, returning its automaton state and exact solution count.
    fn compile(
        &mut self,
        subgraph: &Subgraph,
        cancelled: &impl Fn() -> bool,
    ) -> Result<(StateId, BigUint), SolveError> {
        if cancelled() {
            return Err(SolveError::Cancelled);
        }

        // Dynamic programming ensures every distinct fragment set gets one state.
        if let Some(compiled) = self.memoized_subgraph(subgraph) {
            return Ok(compiled);
        }

        // Register before recursion so a cyclic dependency is detected as an
        // in-progress state instead of recursing forever.
        let state = self.begin_subgraph(subgraph);

        // Each productive free-root split contributes one alternative rule;
        // their solution counts add to the language size of this state.
        let mut total = BigUint::from(0_u8);
        let mut candidates = SplitCandidates::new(self.graph, subgraph);
        while let Some(candidate) = candidates.next(cancelled)? {
            if let Some(split) = self.compile_split(candidate, cancelled)? {
                self.builder
                    .add_rule(split.symbol, split.child_states, state);
                total += split.solution_count;
            }
        }

        // Publishing the count marks the memoized state as fully expanded.
        self.finish_subgraph(state, &total);
        Ok((state, total))
    }

    /// Return an existing state and its completed count, or zero while recursive.
    fn memoized_subgraph(&self, subgraph: &Subgraph) -> Option<(StateId, BigUint)> {
        let &state = self.states.get(subgraph)?;
        let count = self.counts[state.index()]
            .clone()
            .unwrap_or_else(|| BigUint::from(0_u8));
        Some((state, count))
    }

    /// Allocate and memoize the state for a subgraph before expanding its splits.
    fn begin_subgraph(&mut self, subgraph: &Subgraph) -> StateId {
        let state = self.builder.new_state();
        assert_eq!(state.index(), self.subgraphs.len());
        self.subgraphs.push(subgraph.clone());
        self.counts.push(None);
        self.states.insert(subgraph.clone(), state);
        state
    }

    /// Compile one candidate into a productive rule, if all children are solvable.
    fn compile_split(
        &mut self,
        candidate: SplitCandidate,
        cancelled: &impl Fn() -> bool,
    ) -> Result<Option<CompiledSplit>, SolveError> {
        // Child components are independent: compile each one and multiply
        // their language sizes to count this split's combinations.
        let mut children = Vec::with_capacity(candidate.attachments.len());
        let mut solution_count = BigUint::from(1_u8);
        for (hole, child) in candidate.attachments {
            let (child_state, child_count) = self.compile(&child, cancelled)?;
            children.push((hole, child_state));
            solution_count *= child_count;
        }

        // A rule with an empty child language can never contribute a solution.
        if solution_count == BigUint::from(0_u8) {
            return Ok(None);
        }

        // Reuse an equivalent ranked terminal, then align recursive states with
        // the terminal's left-to-right holes before emitting the automaton rule.
        let symbol = self.intern_fragment(candidate.root, &candidate.substitutions);
        let child_states = self.children_in_socket_order(symbol, children);
        Ok(Some(CompiledSplit {
            symbol,
            child_states,
            solution_count,
        }))
    }

    /// Return the canonical symbol for a top fragment, creating it when needed.
    fn intern_fragment(&mut self, root: NodeId, substitutions: &[(NodeId, NodeId)]) -> Symbol {
        // Substitution discovery order is irrelevant, so sort it for a stable key.
        let mut canonical_substitutions = substitutions.to_vec();
        canonical_substitutions.sort_unstable();
        let key = (root, canonical_substitutions);
        if let Some(&symbol) = self.fragment_symbols.get(&key) {
            return symbol;
        }

        // Symbols are dense indices into both fragment metadata vectors.
        let symbol = Symbol(
            u32::try_from(self.fragment_roots.len())
                .expect("fragment count exceeds symbol capacity"),
        );
        let (fragment_root, sockets) =
            build_fragment(self.graph, root, substitutions, &mut self.fragment_arena);
        self.fragment_roots.push(fragment_root);
        self.fragment_sockets.push(sockets.into_boxed_slice());
        self.fragment_symbols.insert(key, symbol);
        symbol
    }

    /// Align child states with the depth-first socket order of a fragment symbol.
    fn children_in_socket_order(
        &self,
        symbol: Symbol,
        mut children: Vec<(NodeId, StateId)>,
    ) -> Vec<StateId> {
        let sockets = &self.fragment_sockets[symbol.0 as usize];
        assert_eq!(
            sockets.len(),
            children.len(),
            "every open fragment socket has one child subgraph"
        );

        // Attachments are discovered by graph traversal, whereas automaton
        // children must follow the terminal's tree order. Selection-sort the
        // small attachment list by hole identity to bridge the two orders.
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
        children.into_iter().map(|(_, state)| state).collect()
    }

    /// Store the final solution count for a fully expanded state.
    fn finish_subgraph(&mut self, state: StateId, count: &BigUint) {
        self.counts[state.index()] = Some(count.clone());
    }
}

/// Productive automaton rule obtained by recursively compiling one split.
struct CompiledSplit {
    /// Ranked terminal identifying the split's top fragment context.
    symbol: Symbol,
    /// Child states aligned with the terminal's open-hole order.
    child_states: Vec<StateId>,
    /// Number of solved forms contributed by this rule.
    solution_count: BigUint,
}

/// Lazy iterator over free roots that produce valid splits of one subgraph.
struct SplitCandidates<'a> {
    /// Graph whose fragments are being split.
    graph: &'a HncGraph,
    /// Current recursive subproblem.
    subgraph: &'a Subgraph,
    /// Index of the next fragment root to test.
    next_root: usize,
}

impl<'a> SplitCandidates<'a> {
    /// Start scanning the graph's fragment roots from the beginning.
    const fn new(graph: &'a HncGraph, subgraph: &'a Subgraph) -> Self {
        Self {
            graph,
            subgraph,
            next_root: 0,
        }
    }

    /// Return the next valid split, checking cancellation between root attempts.
    fn next(
        &mut self,
        cancelled: &impl Fn() -> bool,
    ) -> Result<Option<SplitCandidate>, SolveError> {
        while let Some(&root) = self.graph.roots().get(self.next_root) {
            self.next_root += 1;
            if cancelled() {
                return Err(SolveError::Cancelled);
            }
            // Only a fragment in this subproblem with no incoming local edge
            // can be the top fragment of a solved form.
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

/// Count tree and dominance edges entering `node` from within `subgraph`.
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

/// Compute and validate the split induced by choosing `root` as a free root.
///
/// First, [`RootFragmentTraversal`] follows tree edges and admissible direct
/// substitutions to construct the top context. Then [`SplitTraversal`] checks
/// the remaining undirected graph and groups each attached weakly connected
/// component by the dominance edge through which it leaves the top context.
fn compute_split(graph: &HncGraph, root: NodeId, subgraph: &Subgraph) -> Option<SplitCandidate> {
    // Phase 1: grow the top context through tree edges and legal substitutions.
    let mut root_traversal = RootFragmentTraversal::new(graph, subgraph);
    if !root_traversal.visit(root) {
        return None;
    }
    let RootFragmentTraversal {
        nodes: root_fragment,
        substitutions,
        ..
    } = root_traversal;

    // Phase 2: validate all remaining edges and partition nodes below the
    // context into independently solvable attachment components.
    let mut traversal = SplitTraversal::new(graph, subgraph, &root_fragment, root);
    if !traversal.visit(root, None) || traversal.visited.count() != subgraph.node_count(graph) {
        return None;
    }

    // Convert boundary-edge IDs into the hole/component pairs consumed by the compiler.
    let attachments = traversal
        .component_order
        .into_iter()
        .map(|edge_index| {
            let (dominator, _) = graph.parsed().dominance_edges()[edge_index];
            let subgraph = traversal.components[edge_index]
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

/// Traversal that grows the top fragment context of a proposed split.
struct RootFragmentTraversal<'a> {
    /// Complete source graph.
    graph: &'a HncGraph,
    /// Fragments belonging to the current recursive subproblem.
    subgraph: &'a Subgraph,
    /// Graph nodes included in the top context.
    nodes: BitSet,
    /// Current recursion path, used to reject substitution cycles.
    ancestors: BitSet,
    /// Hole-to-fragment-root substitutions folded into the context.
    substitutions: Vec<(NodeId, NodeId)>,
}

impl<'a> RootFragmentTraversal<'a> {
    /// Create an empty traversal over `subgraph`.
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

    /// Extend the root context below `node`, returning false on an invalid split.
    fn visit(&mut self, node: NodeId) -> bool {
        // Every visited node becomes part of the proposed top context.
        self.nodes.insert(node.index());

        // In an HNC graph, at most one dominance predecessor may be folded
        // into this position. An ancestor is ignored because that edge is
        // already satisfied by the partial context being constructed.
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
            // Direct substitution is legal only at a leaf position and may
            // splice in only a whole fragment root.
            if !self.graph.node(node).tree_children().is_empty()
                || self.graph.tree_parent(parent).is_some()
            {
                return false;
            }
        }

        // Ordinary tree children remain in the same top context.
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
            // Fold the unique admissible dominance predecessor into this hole,
            // recording the splice needed to build the terminal later.
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

/// Kind of source-graph edge traversed while validating a split.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
enum SplitEdgeKind {
    /// Ordered tree edge inside a fragment.
    Tree,
    /// Dominance edge, carrying its stable input index.
    Dominance(usize),
}

/// Directed source-graph edge viewed from either endpoint during traversal.
#[derive(Clone, Copy, Debug)]
struct SplitEdge {
    /// Directed edge source.
    source: NodeId,
    /// Directed edge target.
    target: NodeId,
    /// Structural kind of the edge.
    kind: SplitEdgeKind,
}

impl SplitEdge {
    /// Return the endpoint opposite `node`.
    fn opposite(self, node: NodeId) -> NodeId {
        if self.source == node {
            self.target
        } else {
            self.source
        }
    }
}

/// Undirected validation traversal for the components below a split context.
struct SplitTraversal<'a> {
    /// Complete source graph.
    graph: &'a HncGraph,
    /// Fragments belonging to the current recursive subproblem.
    subgraph: &'a Subgraph,
    /// Nodes already assigned to the split's top context.
    root_fragment: &'a BitSet,
    /// Current path inside the root context.
    path: BitSet,
    /// Nodes assigned exactly once during validation.
    visited: BitSet,
    /// Dominance-edge IDs in component discovery order.
    component_order: Vec<usize>,
    /// Component accumulated below each boundary dominance edge.
    components: Vec<Option<Subgraph>>,
}

impl<'a> SplitTraversal<'a> {
    /// Initialize split validation at `root`.
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
            component_order: Vec::new(),
            components: vec![None; graph.parsed().dominance_edges().len()],
        }
    }

    /// Assign `node` and recursively validate all of its incident edges.
    fn visit(&mut self, node: NodeId, component_edge: Option<usize>) -> bool {
        // A node reached twice would join components that the split claims are
        // independent, so reject the candidate immediately.
        if !self.visited.insert(node.index()) {
            return false;
        }

        // Nodes below the top context inherit the boundary edge that identifies
        // their attachment component.
        if !self.root_fragment.contains(node.index()) {
            let id = component_edge.expect("nodes outside root fragment have a WCC edge");
            if self.components[id].is_none() {
                self.component_order.push(id);
                self.components[id] = Some(Subgraph::empty(self.graph));
            }
            self.components[id]
                .as_mut()
                .unwrap()
                .insert_fragment_of(self.graph, node);
        }

        // Treat the graph as undirected for connectivity: inspect the tree
        // parent, tree children, incoming dominance, and outgoing dominance.
        if let Some(parent) = self.graph.tree_parent(node) {
            if !self.traverse(
                node,
                SplitEdge {
                    source: parent,
                    target: node,
                    kind: SplitEdgeKind::Tree,
                },
                component_edge,
            ) {
                return false;
            }
        }
        let child_count = self.graph.node(node).tree_children().len();
        for index in 0..child_count {
            let child = self.graph.node(node).tree_children()[index];
            if !self.traverse(
                node,
                SplitEdge {
                    source: node,
                    target: child,
                    kind: SplitEdgeKind::Tree,
                },
                component_edge,
            ) {
                return false;
            }
        }
        let incoming_count = self.graph.incoming_dominance(node).len();
        for index in 0..incoming_count {
            let (edge_index, source) = self.graph.incoming_dominance(node)[index];
            if !self.traverse(
                node,
                SplitEdge {
                    source,
                    target: node,
                    kind: SplitEdgeKind::Dominance(edge_index),
                },
                component_edge,
            ) {
                return false;
            }
        }
        let outgoing_count = self.graph.outgoing_dominance(node).len();
        for index in 0..outgoing_count {
            let (edge_index, target) = self.graph.outgoing_dominance(node)[index];
            if !self.traverse(
                node,
                SplitEdge {
                    source: node,
                    target,
                    kind: SplitEdgeKind::Dominance(edge_index),
                },
                component_edge,
            ) {
                return false;
            }
        }
        true
    }

    /// Validate and, when necessary, cross one incident edge.
    ///
    /// Edges leaving the root context must be outgoing dominance edges. An edge
    /// returning to the context is valid only when it points from an ancestor
    /// on the current root path; this is precisely the dominance constraint
    /// satisfied by attaching the component at that open hole.
    fn traverse(&mut self, node: NodeId, edge: SplitEdge, component_edge: Option<usize>) -> bool {
        let neighbor = edge.opposite(node);
        // Edges leaving the current recursive subgraph are irrelevant here.
        if !self.subgraph.contains(self.graph, neighbor) {
            return true;
        }

        if self.root_fragment.contains(neighbor.index())
            && !self.root_fragment.contains(node.index())
        {
            // A lower component may point back only to an ancestor in the top
            // context; any other return edge invalidates the separation.
            if !matches!(edge.kind, SplitEdgeKind::Dominance(_))
                || edge.source != neighbor
                || !self.path.contains(neighbor.index())
            {
                return false;
            }
        } else if !self.visited.contains(neighbor.index()) {
            if self.root_fragment.contains(node.index()) {
                if self.root_fragment.contains(neighbor.index()) {
                    // Continue within the top context while tracking the path
                    // against which returning dominance edges are checked.
                    self.path.insert(neighbor.index());
                    let ok = self.visit(neighbor, None);
                    self.path.remove(neighbor.index());
                    if !ok {
                        return false;
                    }
                } else {
                    // Crossing out of the top context starts a new component
                    // and is legal only along an outgoing dominance edge.
                    let SplitEdgeKind::Dominance(edge_index) = edge.kind else {
                        return false;
                    };
                    if edge.source != node || !self.visit(neighbor, Some(edge_index)) {
                        return false;
                    }
                }
            } else {
                // Once below the context, every reachable node stays in the
                // component selected by the original boundary edge.
                if !self.visit(neighbor, component_edge) {
                    return false;
                }
            }
        }
        true
    }
}

/// Precomputed rewiring instructions for one fragment symbol.
struct SolutionFragment {
    /// Labeled graph node at the fragment root.
    root: NodeId,
    /// Open holes in automaton-child order.
    sockets: Box<[NodeId]>,
    /// Hole substitutions contained entirely inside the fragment context.
    internal_links: Box<[(NodeId, NodeId)]>,
}

/// Build the rewiring instructions used while streaming derivations.
fn make_solution_fragments(chart: &Chart) -> Vec<SolutionFragment> {
    /// Collect open sockets and internal substitutions from one fragment tree.
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
                    // A labeled child occupying a source hole is an internal
                    // substitution; an explicit Hole label is collected above
                    // as a socket to be filled by a derivation child.
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

/// Stable arena and lookup tables reused across streamed solution trees.
struct ReusableSolutionTree {
    /// Arena containing one handle for every labeled source node.
    arena: TreeArena<NodeId>,
    /// Source-node ID to stable arena handle.
    handles: Vec<Option<Tree>>,
    /// Source-hole ID to its parent arena handle and child position.
    hole_slots: Vec<Option<(Tree, usize)>>,
    /// Root handle of the current solution.
    root: Option<Tree>,
}

impl ReusableSolutionTree {
    /// Allocate all labeled nodes once and record every rewritable hole slot.
    fn new(chart: &Chart) -> Self {
        let nodes = chart.graph.parsed().nodes();
        let mut arena = TreeArena::new();
        let mut handles = vec![None; nodes.len()];
        let mut hole_slots = vec![None; nodes.len()];

        // TreeArena nodes need concrete child handles at construction time. A
        // labeled leaf is a harmless temporary placeholder because every slot
        // is wired to its real child before a solution is exposed.
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

        // Fixed tree edges never change. Holes are saved as mutable slots and
        // rewired as the depth-first derivation cursor advances.
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

        Self {
            arena,
            handles,
            hole_slots,
            root: None,
        }
    }

    /// Apply the changed suffix of an automaton derivation to the reusable tree.
    fn apply_derivation(
        &mut self,
        fragments: &[SolutionFragment],
        derivation: Derivation<'_>,
        changed_from: usize,
        had_current: bool,
    ) {
        // The DFS cursor reports the earliest changed frame. Earlier frames
        // still describe the same fragment choices and need no rewiring.
        let first_changed = if had_current { changed_from } else { 0 };
        let nodes = derivation.nodes();
        for node in &nodes[first_changed..] {
            let fragment = &fragments[node.symbol().0 as usize];

            // First restore substitutions contained within this fragment.
            for &(hole, replacement) in &fragment.internal_links {
                self.set_hole_child(hole, replacement);
            }

            // Then plug the fragment root into its parent's corresponding socket.
            if let (Some(parent), Some(child_index)) = (node.parent(), node.child_position()) {
                let parent_symbol = nodes[parent].symbol();
                let hole = fragments[parent_symbol.0 as usize].sockets[child_index];
                self.set_hole_child(hole, fragment.root);
            }
        }

        // The first derivation frame determines the complete solution root.
        let top_symbol = nodes[0].symbol();
        let top = fragments[top_symbol.0 as usize].root;
        self.root = self.handles[top.index()];
    }

    /// Point one source-graph hole at a labeled replacement node.
    fn set_hole_child(&mut self, hole: NodeId, replacement: NodeId) {
        let (parent, child_index) =
            self.hole_slots[hole.index()].expect("every hole has a tree parent");
        self.arena.get_children_mut(parent)[child_index] =
            self.handles[replacement.index()].expect("split roots are labeled");
    }
}

/// Return the labeled source node at a fragment context's root.
fn fragment_root_node(arena: &TreeArena<FragmentNode>, tree: Tree) -> NodeId {
    let FragmentNode::Node(node) = arena.get_label(tree) else {
        unreachable!("a fragment context has a labeled root")
    };
    *node
}
