//! Generic tree-automata operations kept independent of Utool graph concepts.
//!
//! This module is the extraction seam for changes that may be proposed to
//! `rusty-alto` itself.

use rusty_alto::{Explicit, ExplicitBuilder, StateId, Symbol, TopDownTa};
use smallvec::SmallVec;
use thiserror::Error;

/// A transition indexed once from an explicit automaton for fast top-down use.
#[derive(Clone, Debug)]
struct DfsRule {
    symbol: Symbol,
    children: Box<[StateId]>,
}

/// One node of the current accepting derivation, in pre-order.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub(crate) struct DfsDerivationNode {
    /// State recognized at this node.
    pub state: StateId,
    /// Transition symbol selected at this node.
    pub symbol: Symbol,
    /// Parent node and this node's child position; absent at the root.
    pub parent: Option<(usize, usize)>,
    /// Number of children selected by the transition.
    pub arity: usize,
}

/// Borrowed view of the current derivation of a [`DfsLanguageIterator`].
///
/// The view is invalidated by the iterator's next call to [`advance`](DfsLanguageIterator::advance).
#[derive(Clone, Copy, Debug)]
pub(crate) struct DfsDerivation<'a> {
    frames: &'a [DfsFrame],
    rules: &'a [Vec<DfsRule>],
}

impl DfsDerivation<'_> {
    /// Node at a pre-order index.
    ///
    /// # Panics
    ///
    /// Panics if `index` is outside this derivation.
    #[must_use]
    pub fn node(&self, index: usize) -> DfsDerivationNode {
        let frame = &self.frames[index];
        let rule = &self.rules[frame.state.index()][frame.rule_index as usize];
        DfsDerivationNode {
            state: frame.state,
            symbol: rule.symbol,
            parent: (frame.parent != NO_PARENT)
                .then_some((frame.parent as usize, frame.child_index as usize)),
            arity: rule.children.len(),
        }
    }

    /// Nodes in root-first, left-to-right pre-order.
    #[must_use]
    #[cfg(test)]
    pub fn nodes(&self) -> impl ExactSizeIterator<Item = DfsDerivationNode> + '_ {
        (0..self.frames.len()).map(|index| self.node(index))
    }

    /// Number of nodes in this derivation.
    #[must_use]
    pub const fn len(&self) -> usize {
        self.frames.len()
    }
}

/// Why an automaton cannot use finite depth-first language enumeration.
#[derive(Clone, Copy, Debug, Error, PartialEq, Eq)]
pub(crate) enum DfsLanguageError {
    /// A productive cycle is reachable from an accepting state, so the
    /// language may be infinite and Java-style finite backtracking is unsafe.
    #[error("productive cycle reachable through state {state:?}")]
    ProductiveCycle {
        /// A state on the detected cycle.
        state: StateId,
    },
}

#[derive(Clone, Copy, Debug)]
struct PendingState {
    state: StateId,
    parent: u32,
    child_index: u32,
    next: u32,
}

const NO_PARENT: u32 = u32::MAX;
const EMPTY_AGENDA: u32 = u32::MAX;

#[derive(Clone, Debug)]
struct DfsFrame {
    state: StateId,
    rule_index: u32,
    parent: u32,
    child_index: u32,
    agenda_before: u32,
    pending_checkpoint: u32,
}

/// Fast unsorted enumeration of an explicit automaton's finite language.
///
/// This implements the assumptions made by Java Utool's solved-form iterator:
/// only productive rules are considered, productive state dependencies must
/// be acyclic, transition weights and k-best ordering are ignored, and one
/// mutable DFS stack is reused between results. The same accepted tree can be
/// returned more than once if it has multiple accepting runs.
///
/// This is a streaming iterator rather than [`Iterator`]: call
/// [`advance`](DfsLanguageIterator::advance), inspect
/// [`current`](DfsLanguageIterator::current), then advance again. This lets the current
/// derivation borrow the iterator's reusable stack without cloning a tree.
pub(crate) struct DfsLanguagePlan {
    rules: Vec<Vec<DfsRule>>,
    accepting: Vec<StateId>,
}

impl DfsLanguagePlan {
    /// Precompute immutable data for repeated finite-language enumeration.
    ///
    /// # Errors
    ///
    /// Returns [`DfsLanguageError::ProductiveCycle`] when a productive cycle
    /// is reachable from an accepting state.
    ///
    /// # Panics
    ///
    /// Panics if the automaton reports more states than fit in its `u32` state IDs.
    pub fn new(automaton: &Explicit) -> Result<Self, DfsLanguageError> {
        let productive = automaton.reachable_states();
        let mut accepting = Vec::new();
        automaton.initial_states(&mut |state| {
            if productive.contains(state.index()) {
                accepting.push(state);
            }
        });

        let mut rules = Vec::with_capacity(automaton.num_states() as usize);
        for index in 0..automaton.num_states() as usize {
            let state = StateId(u32::try_from(index).expect("state count is stored as u32"));
            rules.push(
                automaton
                    .rules_topdown(state)
                    .filter(|rule| {
                        rule.children
                            .iter()
                            .all(|child| productive.contains(child.index()))
                    })
                    .map(|rule| DfsRule {
                        symbol: rule.symbol,
                        children: rule.children.into(),
                    })
                    .collect(),
            );
        }
        ensure_acyclic(&accepting, &rules)?;
        Ok(Self { rules, accepting })
    }

    /// Start a fresh iterator over this plan.
    #[must_use]
    #[allow(clippy::iter_not_returning_iterator)]
    pub fn iter(&self) -> DfsLanguageIterator<'_> {
        DfsLanguageIterator {
            rules: &self.rules,
            accepting: &self.accepting,
            accepting_index: 0,
            pending: Vec::new(),
            agenda_head: EMPTY_AGENDA,
            frames: Vec::new(),
            changed_from: 0,
            current: false,
            finished: false,
        }
    }
}

pub(crate) struct DfsLanguageIterator<'a> {
    rules: &'a [Vec<DfsRule>],
    accepting: &'a [StateId],
    accepting_index: usize,
    pending: Vec<PendingState>,
    agenda_head: u32,
    frames: Vec<DfsFrame>,
    changed_from: usize,
    current: bool,
    finished: bool,
}

impl DfsLanguageIterator<'_> {
    /// Move to the next accepting derivation.
    pub fn advance(&mut self) -> bool {
        if self.finished {
            return false;
        }
        if !self.current {
            if !self.start_accepting_state() {
                self.finished = true;
                return false;
            }
        } else if !self.backtrack() {
            self.finished = true;
            self.current = false;
            return false;
        }
        self.descend();
        self.current = true;
        true
    }

    /// Borrow the current accepting derivation.
    #[must_use]
    pub fn current(&self) -> Option<DfsDerivation<'_>> {
        self.current.then_some(DfsDerivation {
            frames: &self.frames,
            rules: self.rules,
        })
    }

    /// First pre-order frame whose rule differs from the preceding derivation.
    #[must_use]
    pub const fn changed_from(&self) -> usize {
        self.changed_from
    }

    fn start_accepting_state(&mut self) -> bool {
        let Some(&state) = self.accepting.get(self.accepting_index) else {
            return false;
        };
        self.accepting_index += 1;
        self.changed_from = 0;
        self.pending.clear();
        self.agenda_head = EMPTY_AGENDA;
        self.push_pending(state, NO_PARENT, 0);
        true
    }

    fn descend(&mut self) {
        while let Some(pending) = self.pop_pending() {
            let frame_index = self.frames.len();
            self.frames.push(DfsFrame {
                state: pending.state,
                rule_index: 0,
                parent: pending.parent,
                child_index: pending.child_index,
                agenda_before: self.agenda_head,
                pending_checkpoint: u32::try_from(self.pending.len())
                    .expect("a derivation fits in u32"),
            });
            self.push_children(frame_index);
        }
    }

    fn push_children(&mut self, frame_index: usize) {
        let frame = &self.frames[frame_index];
        let children = &self.rules[frame.state.index()][frame.rule_index as usize].children;
        let parent = u32::try_from(frame_index).expect("a derivation fits in u32");
        for (child_index, &state) in children.iter().enumerate().rev() {
            self.push_pending(
                state,
                parent,
                u32::try_from(child_index).expect("rule arity fits in u32"),
            );
        }
    }

    fn backtrack(&mut self) -> bool {
        while let Some(frame_index) = self.frames.len().checked_sub(1) {
            let frame = &self.frames[frame_index];
            if frame.rule_index as usize + 1 < self.rules[frame.state.index()].len() {
                self.changed_from = frame_index;
                self.agenda_head = frame.agenda_before;
                self.pending.truncate(frame.pending_checkpoint as usize);
                self.frames[frame_index].rule_index += 1;
                self.push_children(frame_index);
                return true;
            }
            self.frames.pop();
        }
        self.start_accepting_state()
    }

    fn push_pending(&mut self, state: StateId, parent: u32, child_index: u32) {
        let index = u32::try_from(self.pending.len()).expect("a derivation fits in u32");
        self.pending.push(PendingState {
            state,
            parent,
            child_index,
            next: self.agenda_head,
        });
        self.agenda_head = index;
    }

    fn pop_pending(&mut self) -> Option<PendingState> {
        if self.agenda_head == EMPTY_AGENDA {
            return None;
        }
        let pending = self.pending[self.agenda_head as usize];
        self.agenda_head = pending.next;
        Some(pending)
    }
}

fn ensure_acyclic(accepting: &[StateId], rules: &[Vec<DfsRule>]) -> Result<(), DfsLanguageError> {
    fn visit(
        state: StateId,
        rules: &[Vec<DfsRule>],
        marks: &mut [u8],
    ) -> Result<(), DfsLanguageError> {
        match marks[state.index()] {
            1 => return Err(DfsLanguageError::ProductiveCycle { state }),
            2 => return Ok(()),
            _ => {}
        }
        marks[state.index()] = 1;
        for rule in &rules[state.index()] {
            for &child in &rule.children {
                visit(child, rules, marks)?;
            }
        }
        marks[state.index()] = 2;
        Ok(())
    }

    let mut marks = vec![0; rules.len()];
    for &state in accepting {
        visit(state, rules, &mut marks)?;
    }
    Ok(())
}

// ---------------------------------------------------------------------------
// Construction-aware automaton trimming
// ---------------------------------------------------------------------------

/// Result of trimming, including the state mapping needed by filtered charts.
pub(crate) struct Trimmed {
    /// The language-equivalent useful part of the staged automaton.
    pub(crate) automaton: Explicit,
    /// For each new state, the corresponding source state.
    pub(crate) source_states: Vec<StateId>,
}

struct GeneratedRule {
    symbol: Symbol,
    children: SmallVec<[StateId; 2]>,
    result: StateId,
}

/// A compact staging builder for generated automata.
///
/// Filtering constructs large intermediate automata and immediately trims
/// them. Building [`Explicit`] first would create bottom-up and top-down indexes
/// for rules that trimming then discards. This builder stores each rule once,
/// computes the useful state set over that storage, and builds [`Explicit`]
/// only for the surviving rules.
#[derive(Default)]
pub(crate) struct GeneratedBuilder {
    next_state: u32,
    accepting: Vec<StateId>,
    rules: Vec<GeneratedRule>,
}

impl GeneratedBuilder {
    pub(crate) fn new() -> Self {
        Self::default()
    }

    pub(crate) fn new_state(&mut self) -> StateId {
        let state = StateId(self.next_state);
        self.next_state = self
            .next_state
            .checked_add(1)
            .expect("state count exceeds u32");
        state
    }

    pub(crate) fn add_accepting(&mut self, state: StateId) {
        debug_assert!(state.0 < self.next_state);
        self.accepting.push(state);
    }

    pub(crate) fn add_rule(
        &mut self,
        symbol: Symbol,
        children: impl Into<SmallVec<[StateId; 2]>>,
        result: StateId,
    ) {
        let children = children.into();
        debug_assert!(result.0 < self.next_state);
        debug_assert!(children.iter().all(|child| child.0 < self.next_state));
        self.rules.push(GeneratedRule {
            symbol,
            children,
            result,
        });
    }

    /// Remove both unproductive states and states that cannot reach acceptance.
    pub(crate) fn trim(self) -> Trimmed {
        let productive = self.productive_states();
        self.trim_with_productive_states(&productive)
    }

    /// Remove states that cannot reach acceptance when every generated state is
    /// known to be productive.
    ///
    /// The fragment-difference construction satisfies this precondition because
    /// it creates states in bottom-up order and only as results of rules whose
    /// children already have productive derivations.
    pub(crate) fn trim_assuming_productive(self) -> Trimmed {
        let productive = vec![true; self.next_state as usize];
        self.trim_with_productive_states(&productive)
    }

    fn trim_with_productive_states(self, productive: &[bool]) -> Trimmed {
        let useful = self.useful_states(productive);
        self.retain_useful(&useful)
    }

    fn productive_states(&self) -> Vec<bool> {
        let state_count = self.next_state as usize;
        let mut productive = vec![false; state_count];
        // `missing_children[r]` counts occurrences, not distinct child states.
        // Recording one mention per occurrence makes a rule such as f(q,q) fire
        // after q becomes productive without a separate duplicate-child case.
        let mut missing_children = self
            .rules
            .iter()
            .map(|rule| rule.children.len())
            .collect::<Vec<_>>();
        let mut mentions = vec![Vec::new(); state_count];
        let mut work = Vec::new();
        for (rule_index, rule) in self.rules.iter().enumerate() {
            if rule.children.is_empty() {
                if !productive[rule.result.index()] {
                    productive[rule.result.index()] = true;
                    work.push(rule.result);
                }
                continue;
            }
            for &child in &rule.children {
                mentions[child.index()].push(rule_index);
            }
        }
        while let Some(state) = work.pop() {
            for &rule_index in &mentions[state.index()] {
                let rule = &self.rules[rule_index];
                missing_children[rule_index] -= 1;
                if missing_children[rule_index] == 0 && !productive[rule.result.index()] {
                    productive[rule.result.index()] = true;
                    work.push(rule.result);
                }
            }
        }
        productive
    }

    fn useful_states(&self, productive: &[bool]) -> Vec<bool> {
        let state_count = self.next_state as usize;
        let mut by_result = vec![Vec::new(); state_count];
        for (rule_index, rule) in self.rules.iter().enumerate() {
            by_result[rule.result.index()].push(rule_index);
        }
        // Starting at productive accepting states, follow productive rules
        // top-down. The reached states are exactly the productive states that
        // participate in some accepting run.
        let mut useful = vec![false; state_count];
        let mut work = Vec::new();
        for &state in &self.accepting {
            if productive[state.index()] && !useful[state.index()] {
                useful[state.index()] = true;
                work.push(state);
            }
        }
        while let Some(parent) = work.pop() {
            for &rule_index in &by_result[parent.index()] {
                let rule = &self.rules[rule_index];
                if rule.children.iter().all(|child| productive[child.index()]) {
                    for &child in &rule.children {
                        if !useful[child.index()] {
                            useful[child.index()] = true;
                            work.push(child);
                        }
                    }
                }
            }
        }
        useful
    }

    fn retain_useful(self, useful: &[bool]) -> Trimmed {
        let state_count = self.next_state as usize;
        let source_states = useful
            .iter()
            .enumerate()
            .filter(|(_, keep)| **keep)
            .map(|(index, _)| StateId(u32::try_from(index).expect("state count is stored as u32")))
            .collect::<Vec<_>>();
        // Compact state IDs in increasing source-state order. `source_states`
        // is the inverse mapping used to restore chart metadata afterward.
        let mut builder = ExplicitBuilder::new();
        let mut remap = vec![None; state_count];
        for &old in &source_states {
            remap[old.index()] = Some(builder.new_state());
        }
        for old in self.accepting {
            if let Some(new) = remap[old.index()] {
                builder.add_accepting(new);
            }
        }
        for rule in self.rules {
            let Some(result) = remap[rule.result.index()] else {
                continue;
            };
            let Some(children) = rule
                .children
                .into_iter()
                .map(|child| remap[child.index()])
                .collect::<Option<SmallVec<[StateId; 2]>>>()
            else {
                continue;
            };
            builder.add_rule(rule.symbol, children.into_vec(), result);
        }
        Trimmed {
            automaton: builder.build(),
            source_states,
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use packed_term_arena::tree::{Tree, TreeArena};
    use rusty_alto::{BottomUpTa, ExplicitBuilder, Symbol};

    fn show_tree(arena: &TreeArena<Symbol>, tree: Tree) -> String {
        let children = arena
            .get_children(tree)
            .iter()
            .map(|&child| show_tree(arena, child))
            .collect::<Vec<_>>();
        if children.is_empty() {
            format!("{}", arena.get_label(tree).0)
        } else {
            format!("{}({})", arena.get_label(tree).0, children.join(","))
        }
    }

    fn show_derivation(derivation: DfsDerivation<'_>) -> String {
        fn visit(nodes: &[DfsDerivationNode], cursor: &mut usize) -> String {
            let node = nodes[*cursor];
            *cursor += 1;
            let children = (0..node.arity)
                .map(|_| visit(nodes, cursor))
                .collect::<Vec<_>>();
            if children.is_empty() {
                format!("{}", node.symbol.0)
            } else {
                format!("{}({})", node.symbol.0, children.join(","))
            }
        }

        let nodes = derivation.nodes().collect::<Vec<_>>();
        let mut cursor = 0;
        let result = visit(&nodes, &mut cursor);
        assert_eq!(cursor, nodes.len());
        result
    }

    fn assert_same_language(automaton: &Explicit) {
        let mut sorted = automaton.sorted_language();
        let mut expected = Vec::new();
        while let Some(tree) = sorted.next() {
            expected.push(show_tree(sorted.arena(), tree.tree()));
        }
        expected.sort();

        let plan = DfsLanguagePlan::new(automaton).unwrap();
        let mut dfs = plan.iter();
        let mut actual = Vec::new();
        while dfs.advance() {
            actual.push(show_derivation(dfs.current().unwrap()));
        }
        actual.sort();
        assert_eq!(actual, expected);
    }

    #[test]
    fn removes_unproductive_and_non_accepting_branches() {
        let mut builder = GeneratedBuilder::new();
        let useful_leaf = builder.new_state();
        let root = builder.new_state();
        let dead_leaf = builder.new_state();
        let unproductive = builder.new_state();
        builder.add_rule(Symbol(0), vec![], useful_leaf);
        builder.add_rule(Symbol(1), vec![useful_leaf, useful_leaf], root);
        builder.add_rule(Symbol(2), vec![], dead_leaf);
        builder.add_rule(Symbol(3), vec![unproductive], unproductive);
        builder.add_accepting(root);
        let result = builder.trim();
        assert_eq!(result.automaton.num_states(), 2);
        assert_eq!(result.automaton.num_rules(), 2);
        assert!(result.automaton.is_accepting(&StateId(1)));
        assert_eq!(result.source_states, vec![useful_leaf, root]);
    }

    #[test]
    fn productive_trim_matches_general_trim_when_all_states_are_productive() {
        fn generated() -> GeneratedBuilder {
            let mut builder = GeneratedBuilder::new();
            let useful_leaf = builder.new_state();
            let root = builder.new_state();
            let dead_leaf = builder.new_state();
            builder.add_rule(Symbol(0), vec![], useful_leaf);
            builder.add_rule(Symbol(1), vec![useful_leaf, useful_leaf], root);
            builder.add_rule(Symbol(2), vec![], dead_leaf);
            builder.add_accepting(root);
            builder
        }
        let general = generated().trim();
        let specialized = generated().trim_assuming_productive();
        assert_eq!(specialized.source_states, general.source_states);
        assert_eq!(specialized.automaton.num_states(), 2);
        assert_eq!(specialized.automaton.num_rules(), 2);
    }

    #[test]
    fn dfs_matches_sorted_language_as_a_multiset() {
        let mut builder = ExplicitBuilder::new();
        let leaf = builder.new_state();
        let other_leaf = builder.new_state();
        let root = builder.new_state();
        builder.add_weighted_rule(Symbol(0), vec![], leaf, 0.1);
        builder.add_weighted_rule(Symbol(1), vec![], leaf, 0.9);
        builder.add_rule(Symbol(4), vec![], other_leaf);
        builder.add_rule(Symbol(2), vec![leaf, leaf], root);
        builder.add_rule(Symbol(3), vec![other_leaf], root);
        builder.add_accepting(root);
        builder.add_accepting(leaf);
        assert_same_language(&builder.build());
    }

    #[test]
    fn dfs_preserves_ambiguous_accepting_runs() {
        let mut builder = ExplicitBuilder::new();
        let left = builder.new_state();
        let right = builder.new_state();
        builder.add_rule(Symbol(0), vec![], left);
        builder.add_rule(Symbol(0), vec![], right);
        builder.add_accepting(left);
        builder.add_accepting(right);
        assert_same_language(&builder.build());
    }

    #[test]
    fn dfs_reports_the_deterministic_backtracking_frame() {
        let mut builder = ExplicitBuilder::new();
        let leaf = builder.new_state();
        let root = builder.new_state();
        builder.add_rule(Symbol(0), vec![], leaf);
        builder.add_rule(Symbol(1), vec![], leaf);
        builder.add_rule(Symbol(2), vec![leaf, leaf], root);
        builder.add_accepting(root);
        let automaton = builder.build();
        let plan = DfsLanguagePlan::new(&automaton).unwrap();
        let mut dfs = plan.iter();
        let mut changed = Vec::new();
        while dfs.advance() {
            changed.push(dfs.changed_from());
        }
        assert_eq!(changed, [0, 2, 1, 2]);
    }

    #[test]
    fn dfs_rejects_productive_cycles() {
        let mut builder = ExplicitBuilder::new();
        let state = builder.new_state();
        builder.add_rule(Symbol(0), vec![], state);
        builder.add_rule(Symbol(1), vec![state], state);
        builder.add_accepting(state);
        assert_eq!(
            DfsLanguagePlan::new(&builder.build()).err(),
            Some(DfsLanguageError::ProductiveCycle { state })
        );
    }

    #[test]
    fn dfs_ignores_unproductive_cycles() {
        let mut builder = ExplicitBuilder::new();
        let root = builder.new_state();
        let dead = builder.new_state();
        builder.add_rule(Symbol(0), vec![], root);
        builder.add_rule(Symbol(1), vec![dead], dead);
        builder.add_accepting(root);
        assert_same_language(&builder.build());
    }
}
