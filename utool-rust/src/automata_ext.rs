//! Generic tree-automata operations kept independent of Utool graph concepts.
//!
//! This module is the extraction seam for changes that may be proposed to
//! `rusty-alto` itself.

use rusty_alto::{Explicit, ExplicitBuilder, StateId, Symbol, TopDownTa};
use std::collections::HashSet;
use thiserror::Error;

/// A transition indexed once from an explicit automaton for fast top-down use.
#[derive(Clone, Debug)]
struct DfsRule {
    symbol: Symbol,
    children: Box<[StateId]>,
}

/// One node of the current accepting derivation, in pre-order.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub struct DfsDerivationNode {
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
pub struct DfsDerivation<'a> {
    frames: &'a [DfsFrame],
    rules: &'a [Vec<DfsRule>],
}

impl DfsDerivation<'_> {
    /// Node at a pre-order index.
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
    pub fn nodes(&self) -> impl ExactSizeIterator<Item = DfsDerivationNode> + '_ {
        (0..self.frames.len()).map(|index| self.node(index))
    }

    /// Number of nodes in this derivation.
    #[must_use]
    pub const fn len(&self) -> usize {
        self.frames.len()
    }

    /// Whether this derivation has no nodes.
    #[must_use]
    pub const fn is_empty(&self) -> bool {
        self.frames.is_empty()
    }
}

/// Why an automaton cannot use finite depth-first language enumeration.
#[derive(Clone, Copy, Debug, Error, PartialEq, Eq)]
pub enum DfsLanguageError {
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
/// This is a streaming iterator rather than [`Iterator`]: call [`advance`](Self::advance),
/// inspect [`current`](Self::current), then advance again. This lets the current
/// derivation borrow the iterator's reusable stack without cloning a tree.
pub struct DfsLanguagePlan {
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
    pub fn iter(&self) -> DfsLanguageIterator<'_> {
        DfsLanguageIterator {
            rules: &self.rules,
            accepting: &self.accepting,
            accepting_index: 0,
            pending: Vec::new(),
            agenda_head: EMPTY_AGENDA,
            frames: Vec::new(),
            current: false,
            finished: false,
        }
    }
}

pub struct DfsLanguageIterator<'a> {
    rules: &'a [Vec<DfsRule>],
    accepting: &'a [StateId],
    accepting_index: usize,
    pending: Vec<PendingState>,
    agenda_head: u32,
    frames: Vec<DfsFrame>,
    current: bool,
    finished: bool,
}

impl<'a> DfsLanguageIterator<'a> {
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
            rules: &self.rules,
        })
    }

    fn start_accepting_state(&mut self) -> bool {
        let Some(&state) = self.accepting.get(self.accepting_index) else {
            return false;
        };
        self.accepting_index += 1;
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

/// Result of trimming, including mappings useful for diagnostic provenance.
pub struct Trimmed {
    /// The language-equivalent useful part of the input automaton.
    pub automaton: Explicit,
    /// For each new state, the corresponding source state.
    pub source_states: Vec<StateId>,
}

/// Remove states and transitions which cannot occur in an accepting run.
#[must_use]
pub fn trim(automaton: &Explicit) -> Trimmed {
    let productive = automaton.reachable_states();
    let mut useful = HashSet::new();
    let mut work = Vec::new();
    automaton.initial_states(&mut |state| {
        if productive.contains(state.index()) && useful.insert(state) {
            work.push(state);
        }
    });
    while let Some(parent) = work.pop() {
        for rule in automaton.rules_topdown(parent) {
            if rule
                .children
                .iter()
                .all(|child| productive.contains(child.index()))
            {
                for &child in rule.children {
                    if useful.insert(child) {
                        work.push(child);
                    }
                }
            }
        }
    }

    let mut source_states = useful.into_iter().collect::<Vec<_>>();
    source_states.sort_unstable_by_key(|state| state.index());
    let mut builder = ExplicitBuilder::new();
    let new_states = source_states
        .iter()
        .map(|_| builder.new_state())
        .collect::<Vec<_>>();
    let remap = source_states
        .iter()
        .zip(new_states.iter())
        .map(|(&old, &new)| (old, new))
        .collect::<std::collections::HashMap<_, _>>();
    automaton.initial_states(&mut |old| {
        if let Some(&new) = remap.get(&old) {
            builder.add_accepting(new);
        }
    });
    for rule in automaton.rules() {
        let Some(&result) = remap.get(&rule.result) else {
            continue;
        };
        let Some(children) = rule
            .children
            .iter()
            .map(|child| remap.get(child).copied())
            .collect::<Option<Vec<_>>>()
        else {
            continue;
        };
        builder.add_weighted_rule(rule.symbol, children, result, rule.weight);
    }
    Trimmed {
        automaton: builder.build(),
        source_states,
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
        let mut builder = ExplicitBuilder::new();
        let useful_leaf = builder.new_state();
        let root = builder.new_state();
        let dead_leaf = builder.new_state();
        let unproductive = builder.new_state();
        builder.add_rule(Symbol(0), vec![], useful_leaf);
        builder.add_rule(Symbol(1), vec![useful_leaf], root);
        builder.add_rule(Symbol(2), vec![], dead_leaf);
        builder.add_rule(Symbol(3), vec![unproductive], unproductive);
        builder.add_accepting(root);
        let result = trim(&builder.build());
        assert_eq!(result.automaton.num_states(), 2);
        assert_eq!(result.automaton.num_rules(), 2);
        assert!(result.automaton.is_accepting(&StateId(1)));
        assert_eq!(result.source_states, vec![useful_leaf, root]);
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
