//! Generic tree-automata operations kept independent of Utool graph concepts.
//!
//! This module is the extraction seam for changes that may be proposed to
//! `rusty-alto` itself.

use rusty_alto::{Explicit, ExplicitBuilder, StateId, TopDownTa};
use std::collections::HashSet;

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
    use rusty_alto::{BottomUpTa, ExplicitBuilder, Symbol};

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
}
