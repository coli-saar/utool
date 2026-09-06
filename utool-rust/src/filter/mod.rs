//! Relative-normal-form filtering over compact fragment automata.
//!
//! A chart denotes a finite language `L` of solved forms.  A rewrite system
//! defines when one solved form is worse than another: `s` is discarded if a
//! permitted one-step rewrite maps `s` to some `t` in `L`.  The result is
//! therefore
//!
//! ```text
//! L - preimage(rewrite, L).
//! ```
//!
//! The implementation computes this language without enumerating solved forms:
//!
//! 1. `expand_chart` replaces fragment terminals by their semantic nodes.
//! 2. `build_ctt` compiles the rewrite system into a context tree transducer
//!    (CTT) that performs exactly one rewrite.
//! 3. `compute_preimage` builds an automaton for chart trees that the CTT can
//!    rewrite into another chart tree.  These are the trees to discard.
//! 4. `difference_on_fragments` subtracts that automaton from the original
//!    chart while evaluating whole fragment terminals at once.
//!
//! The first three stages work over individual graph nodes because rewrite
//! patterns mention those nodes.  The final stage returns to fragment terminals
//! so that the filtered result remains a compact Utool chart.
//!
//! For a code audit, start with `filter_chart` near the end of this module and
//! then read the four sections above in call order. Parsing and static
//! validation are independent of the automata construction.

use crate::automata_ext::GeneratedBuilder;
use crate::graph::{HncGraph, NodeId};
use crate::solver::{Chart, FragmentNode};
use packed_term_arena::tree::{Tree, TreeArena};
use rustc_hash::{FxHashMap, FxHashSet};
use rusty_alto::{
    BottomUpTa, DetBottomUpTa, Explicit, ExplicitBuilder, IndexedBottomUpTa, StateId, Symbol,
    TopDownTa,
};
use smallvec::{SmallVec, smallvec};
use std::collections::{HashMap, VecDeque};
use std::rc::Rc;
use thiserror::Error;

// ---------------------------------------------------------------------------
// Public rewrite language
// ---------------------------------------------------------------------------

/// A first-order rewrite-system term.
#[derive(Clone, Debug, PartialEq, Eq, Hash)]
pub enum Pattern {
    /// A variable, conventionally beginning with an uppercase letter.
    Variable(String),
    /// A semantic constructor occurrence with ordered children.
    Node {
        /// Semantic graph label.
        label: String,
        /// Identity shared by corresponding occurrences on both rule sides.
        occurrence: String,
        /// Ordered arguments.
        children: Vec<Pattern>,
    },
    /// An arbitrary one-node context around the nested pattern.
    Wildcard(Box<Pattern>),
}

/// One weakening or equivalence rule.
#[derive(Clone, Debug, PartialEq, Eq, Hash)]
pub struct RewriteRule {
    /// Required polarity/annotation for an oriented rule.
    pub annotation: Option<String>,
    /// Pattern to replace.
    pub lhs: Pattern,
    /// Replacement template.
    pub rhs: Pattern,
    /// `true` for a directed weakening rule; `false` for an equation whose
    /// direction is chosen by the canonical pattern order.
    pub oriented: bool,
}

/// Parsed rewrite rules and annotation propagation table.
#[derive(Clone, Debug, Default, PartialEq, Eq)]
pub struct RewriteSystem {
    /// Directed and equivalence rules.
    pub rules: Vec<RewriteRule>,
    /// Annotation at the root.
    pub start_annotation: Option<String>,
    /// Annotation used where no propagation rule applies.
    pub neutral_annotation: Option<String>,
    // parent annotation -> constructor label -> child annotations
    annotations: HashMap<String, HashMap<String, Vec<String>>>,
}

/// Rewrite parsing or filtering failure.
#[derive(Debug, Error)]
pub enum FilterError {
    /// Invalid rewrite-system syntax.
    #[error("rewrite syntax on line {line}: {message}")]
    Syntax { line: usize, message: String },
    /// The rewrite formalism requires linear, nondeleting variables.
    #[error("rewrite rule on line {line} is not linear and nondeleting: {message}")]
    InvalidVariables { line: usize, message: String },
    /// Context wildcards must describe the same single surrounding node.
    #[error("invalid context wildcard: {message}")]
    InvalidWildcard { message: String },
    /// An annotation propagation rule has the wrong arity for a graph node.
    #[error("annotation {annotation}:{label} has {actual} children, expected {expected}")]
    AnnotationArity {
        annotation: String,
        label: String,
        expected: usize,
        actual: usize,
    },
    /// The node expansion violated a determinism invariant used by the CTT algorithm.
    #[error("expanded chart is not {direction}-deterministic at node {node}")]
    NonDeterministicExpansion {
        direction: &'static str,
        node: String,
    },
    /// The source chart unexpectedly contains a productive state cycle.
    #[error("filtering requires an acyclic finite chart")]
    CyclicChart,
    /// The caller cancelled filtering.
    #[error("chart filtering was cancelled")]
    Cancelled,
}

// ---------------------------------------------------------------------------
// Parsing and static validation
// ---------------------------------------------------------------------------

#[derive(Clone, Debug)]
enum RawPattern {
    Variable(String),
    Node {
        label: String,
        occurrence: Option<String>,
        children: Vec<RawPattern>,
    },
    Wildcard(Box<RawPattern>),
}

impl RewriteSystem {
    /// Parse the Java Utool rewrite-file surface syntax into indexed patterns.
    ///
    /// # Errors
    ///
    /// Returns a syntax error for malformed rules or ambiguous unindexed
    /// constructor occurrences.
    pub fn parse(input: &str) -> Result<Self, FilterError> {
        let mut system = Self::default();
        for (offset, original) in input.lines().enumerate() {
            let line_number = offset + 1;
            let line = original.split("//").next().unwrap_or("").trim();
            if line.is_empty() {
                continue;
            }
            if let Some(value) = line.strip_prefix("start annotation:") {
                system.start_annotation = Some(value.trim().to_owned());
                continue;
            }
            if let Some(value) = line.strip_prefix("neutral annotation:") {
                system.neutral_annotation = Some(value.trim().to_owned());
                continue;
            }
            let (annotation, rule_text) = if let Some(rest) = line.strip_prefix('[') {
                let end = rest
                    .find(']')
                    .ok_or_else(|| syntax(line_number, "missing ']'"))?;
                (Some(rest[..end].trim().to_owned()), rest[end + 1..].trim())
            } else {
                (None, line)
            };
            if let Some((lhs, rhs)) = rule_text.split_once("->") {
                let (lhs, rhs) = parse_rule_patterns(lhs, rhs, line_number)?;
                system.rules.push(RewriteRule {
                    annotation,
                    lhs,
                    rhs,
                    oriented: true,
                });
                continue;
            }
            if let Some((lhs, rhs)) = rule_text.split_once('=') {
                let (lhs, rhs) = parse_rule_patterns(lhs, rhs, line_number)?;
                system.rules.push(RewriteRule {
                    annotation: None,
                    lhs,
                    rhs,
                    oriented: false,
                });
                continue;
            }
            if let Some((parent, term)) = line.split_once(':') {
                let RawPattern::Node {
                    label, children, ..
                } = parse_raw_pattern(term.trim(), line_number)?
                else {
                    return Err(syntax(
                        line_number,
                        "annotation rule must name a constructor",
                    ));
                };
                let child_annotations = children
                    .into_iter()
                    .map(|child| match child {
                        RawPattern::Node {
                            label, children, ..
                        } if children.is_empty() => Ok(label),
                        _ => Err(syntax(line_number, "invalid child annotation")),
                    })
                    .collect::<Result<Vec<_>, _>>()?;
                system
                    .annotations
                    .entry(parent.trim().to_owned())
                    .or_default()
                    .insert(label, child_annotations);
                continue;
            }
            return Err(syntax(line_number, "expected a rewrite or annotation rule"));
        }
        let mut unique_rules = FxHashSet::default();
        system
            .rules
            .retain(|rule| unique_rules.insert(rule.clone()));
        Ok(system)
    }
}

fn syntax(line: usize, message: &str) -> FilterError {
    FilterError::Syntax {
        line,
        message: message.to_owned(),
    }
}

fn parse_rule_patterns(
    lhs: &str,
    rhs: &str,
    line: usize,
) -> Result<(Pattern, Pattern), FilterError> {
    let lhs = parse_raw_pattern(lhs.trim(), line)?;
    let rhs = parse_raw_pattern(rhs.trim(), line)?;
    let mut occurrence_labels = HashMap::new();
    let mut lhs_labels = HashMap::new();
    let mut next = 1_u32;
    let lhs = index_pattern(
        lhs,
        line,
        true,
        &mut next,
        &mut lhs_labels,
        &mut occurrence_labels,
    )?;
    let rhs = index_pattern(
        rhs,
        line,
        false,
        &mut next,
        &mut HashMap::new(),
        &mut occurrence_labels,
    )?;
    let lhs_occurrences = collect_pattern_occurrences(&lhs);
    for (occurrence, label) in collect_pattern_occurrences(&rhs) {
        if lhs_occurrences.get(&occurrence) != Some(&label) {
            return Err(syntax(
                line,
                "every constructor on the right must identify an occurrence on the left",
            ));
        }
    }
    validate_variables(&lhs, &rhs, line)?;
    Ok((lhs, rhs))
}

fn index_pattern(
    raw: RawPattern,
    line: usize,
    lhs: bool,
    next: &mut u32,
    labels_here: &mut HashMap<String, String>,
    occurrence_labels: &mut HashMap<String, String>,
) -> Result<Pattern, FilterError> {
    match raw {
        RawPattern::Variable(name) => Ok(Pattern::Variable(name)),
        RawPattern::Wildcard(inner) => Ok(Pattern::Wildcard(Box::new(index_pattern(
            *inner,
            line,
            lhs,
            next,
            labels_here,
            occurrence_labels,
        )?))),
        RawPattern::Node {
            label,
            occurrence,
            children,
        } => {
            let occurrence = if let Some(occurrence) = occurrence {
                occurrence
            } else if lhs {
                if labels_here.contains_key(&label) {
                    return Err(syntax(
                        line,
                        &format!("constructor {label} occurs twice without an explicit index"),
                    ));
                }
                let occurrence = format!("_i{next}");
                *next += 1;
                labels_here.insert(label.clone(), occurrence.clone());
                occurrence
            } else {
                let matches = occurrence_labels
                    .iter()
                    .filter(|(_, known)| *known == &label)
                    .map(|(occurrence, _)| occurrence.clone())
                    .collect::<Vec<_>>();
                match matches.as_slice() {
                    [occurrence] => occurrence.clone(),
                    [] => {
                        let occurrence = format!("_r{next}");
                        *next += 1;
                        occurrence
                    }
                    _ => {
                        return Err(syntax(
                            line,
                            &format!(
                                "constructor {label} is ambiguous on the right; use an explicit index"
                            ),
                        ));
                    }
                }
            };
            if let Some(previous) = occurrence_labels.insert(occurrence.clone(), label.clone())
                && previous != label
            {
                return Err(syntax(
                    line,
                    &format!("occurrence #{occurrence} names both {previous} and {label}"),
                ));
            }
            let children = children
                .into_iter()
                .map(|child| index_pattern(child, line, lhs, next, labels_here, occurrence_labels))
                .collect::<Result<_, _>>()?;
            Ok(Pattern::Node {
                label,
                occurrence,
                children,
            })
        }
    }
}

fn collect_pattern_occurrences(pattern: &Pattern) -> HashMap<String, String> {
    fn visit(pattern: &Pattern, out: &mut HashMap<String, String>) {
        match pattern {
            Pattern::Variable(_) => {}
            Pattern::Wildcard(inner) => visit(inner, out),
            Pattern::Node {
                label,
                occurrence,
                children,
            } => {
                out.insert(occurrence.clone(), label.clone());
                for child in children {
                    visit(child, out);
                }
            }
        }
    }
    let mut result = HashMap::new();
    visit(pattern, &mut result);
    result
}

fn validate_variables(lhs: &Pattern, rhs: &Pattern, line: usize) -> Result<(), FilterError> {
    fn counts(pattern: &Pattern, out: &mut HashMap<String, usize>) {
        match pattern {
            Pattern::Variable(name) => *out.entry(name.clone()).or_default() += 1,
            Pattern::Node { children, .. } => {
                for child in children {
                    counts(child, out);
                }
            }
            Pattern::Wildcard(inner) => counts(inner, out),
        }
    }
    let mut left = HashMap::new();
    let mut right = HashMap::new();
    counts(lhs, &mut left);
    counts(rhs, &mut right);
    if left.values().any(|&count| count != 1) || right.values().any(|&count| count != 1) {
        return Err(FilterError::InvalidVariables {
            line,
            message: "each variable must occur once on each side".to_owned(),
        });
    }
    if left.len() != right.len() || left.keys().any(|name| !right.contains_key(name)) {
        return Err(FilterError::InvalidVariables {
            line,
            message: "both sides must contain the same variables".to_owned(),
        });
    }
    Ok(())
}

struct PatternParser<'a> {
    input: &'a str,
    offset: usize,
    line: usize,
}

fn parse_raw_pattern(input: &str, line: usize) -> Result<RawPattern, FilterError> {
    let mut parser = PatternParser {
        input,
        offset: 0,
        line,
    };
    let term = parser.term()?;
    parser.whitespace();
    if parser.offset != input.len() {
        return Err(syntax(line, "unexpected text after term"));
    }
    Ok(term)
}

impl PatternParser<'_> {
    fn term(&mut self) -> Result<RawPattern, FilterError> {
        self.whitespace();
        if self.consume('*') {
            self.expect('[')?;
            let inner = self.term()?;
            self.expect(']')?;
            return Ok(RawPattern::Wildcard(Box::new(inner)));
        }
        let name = self.identifier()?;
        let occurrence = self.consume('#').then(|| self.identifier()).transpose()?;
        self.whitespace();
        if self.consume('(') {
            let mut children = Vec::new();
            self.whitespace();
            if !self.consume(')') {
                loop {
                    children.push(self.term()?);
                    self.whitespace();
                    if self.consume(')') {
                        break;
                    }
                    self.expect(',')?;
                }
            }
            return Ok(RawPattern::Node {
                label: name,
                occurrence,
                children,
            });
        }
        if name.chars().next().is_some_and(char::is_uppercase) && occurrence.is_none() {
            Ok(RawPattern::Variable(name))
        } else {
            Ok(RawPattern::Node {
                label: name,
                occurrence,
                children: Vec::new(),
            })
        }
    }

    fn identifier(&mut self) -> Result<String, FilterError> {
        self.whitespace();
        let start = self.offset;
        while let Some(character) = self.input[self.offset..].chars().next() {
            if character.is_whitespace() || "#(),[]=".contains(character) {
                break;
            }
            self.offset += character.len_utf8();
        }
        if start == self.offset {
            Err(syntax(self.line, "expected an identifier"))
        } else {
            Ok(self.input[start..self.offset].to_owned())
        }
    }

    fn whitespace(&mut self) {
        while let Some(character) = self.input[self.offset..].chars().next() {
            if !character.is_whitespace() {
                break;
            }
            self.offset += character.len_utf8();
        }
    }

    fn consume(&mut self, expected: char) -> bool {
        self.whitespace();
        if self.input[self.offset..].starts_with(expected) {
            self.offset += expected.len_utf8();
            true
        } else {
            false
        }
    }

    fn expect(&mut self, expected: char) -> Result<(), FilterError> {
        self.consume(expected)
            .then_some(())
            .ok_or_else(|| syntax(self.line, &format!("expected '{expected}'")))
    }
}

// ---------------------------------------------------------------------------
// Rewrite transducer construction
// ---------------------------------------------------------------------------

/// A rewrite pattern after constructor occurrences have been assigned to
/// concrete nodes of this graph.
///
/// Patterns use graph-node identity rather than labels from this point on.
/// This makes distinct occurrences of the same predicate distinguishable.
#[derive(Clone, Debug, PartialEq, Eq, Hash, PartialOrd, Ord)]
enum SpecializedPattern {
    Variable(String),
    Node(NodeId, Vec<SpecializedPattern>),
}

/// Control state of the context tree transducer.
///
/// `Annotation(a)` marks the unique path from the root to the rewrite site and
/// carries the annotation required there. `Neutral` copies subtrees outside
/// that path unchanged. Consequently, an accepting CTT run contains one
/// rewrite rule rather than an arbitrary number of rewrites.
#[derive(Clone, Copy, Debug, PartialEq, Eq, Hash)]
enum CttState {
    Neutral,
    Annotation(u32),
}

/// Input pattern of a CTT rule.
///
/// `interior` identifies a constructor inside a compiled left-hand side. It is
/// assigned globally so that preimage states from different rule occurrences
/// cannot be merged accidentally.
#[derive(Clone, Debug)]
enum CttLhs {
    /// Variable recognized under the given child control state, with its dense
    /// variable-slot index.
    Variable(CttState, u32),
    /// Concrete input constructor.
    Node {
        node: NodeId,
        children: Vec<CttLhs>,
        interior: u32,
    },
}

/// Output template of a CTT rule.
#[derive(Clone, Debug)]
enum CttRhs {
    /// Reference to a variable slot bound while matching this template.
    Variable(u32),
    /// Concrete output constructor.
    Node(NodeId, Vec<CttRhs>),
}

#[derive(Clone, Debug)]
struct CttRule {
    /// Input pattern recognized by this rule.
    lhs: CttLhs,
    /// Control state required at the pattern root.
    state: CttState,
    /// Output template produced by this rule.
    rhs: CttRhs,
    /// Number of dense variable slots shared by `lhs` and `rhs`.
    variables: usize,
}

/// A context tree transducer indexed by control state and output-root symbol.
///
/// The output-root index lets preimage construction consider only rules that
/// can produce the current deterministic target transition. Variable-rooted
/// rules use `None` because they can match any target state.
struct Ctt {
    rules: FxHashMap<(CttState, Option<Symbol>), Vec<CttRule>>,
    final_state: CttState,
}

/// Dense representation of the annotation propagation system used by the CTT.
struct AnnotationStates {
    names: Vec<String>,
    ids: HashMap<String, u32>,
    start: CttState,
    neutral: CttState,
}

fn intern_annotation(name: &str, names: &mut Vec<String>, ids: &mut HashMap<String, u32>) {
    if !ids.contains_key(name) {
        let id = u32::try_from(names.len()).expect("annotation count exceeds u32");
        names.push(name.to_owned());
        ids.insert(name.to_owned(), id);
    }
}

impl AnnotationStates {
    fn new(system: &RewriteSystem) -> Self {
        let mut names = Vec::new();
        let mut ids = HashMap::new();
        if let Some(name) = system.start_annotation.as_deref() {
            intern_annotation(name, &mut names, &mut ids);
        }
        if let Some(name) = system.neutral_annotation.as_deref() {
            intern_annotation(name, &mut names, &mut ids);
        }
        for rule in &system.rules {
            if let Some(name) = rule.annotation.as_deref() {
                intern_annotation(name, &mut names, &mut ids);
            }
        }
        let mut propagated = system
            .annotations
            .iter()
            .flat_map(|(parent, by_label)| {
                std::iter::once(parent).chain(by_label.values().flatten())
            })
            .cloned()
            .collect::<Vec<_>>();
        propagated.sort();
        propagated.dedup();
        for name in propagated {
            intern_annotation(&name, &mut names, &mut ids);
        }
        if names.is_empty() {
            intern_annotation("*", &mut names, &mut ids);
        }

        let start_name = system.start_annotation.as_deref().unwrap_or(&names[0]);
        let neutral_name = system.neutral_annotation.as_deref().unwrap_or(start_name);
        let start = CttState::Annotation(ids[start_name]);
        let neutral = CttState::Annotation(ids[neutral_name]);
        Self {
            names,
            ids,
            start,
            neutral,
        }
    }
}

/// Compile graph-specialized rewrite and copy rules into the CTT index.
fn build_ctt(
    graph: &HncGraph,
    system: &RewriteSystem,
    cancelled: impl Fn() -> bool + Copy,
) -> Result<Ctt, FilterError> {
    let annotations = AnnotationStates::new(system);
    let mut all_rules = Vec::new();

    // Copy rules choose a single annotated root-to-rewrite path. Rewrite rules
    // terminate that path by performing the one non-identity step.
    append_copy_rules(
        graph,
        system,
        &annotations.names,
        &annotations.ids,
        annotations.neutral,
        cancelled,
        &mut all_rules,
    )?;
    append_rewrite_rules(graph, system, &annotations.ids, cancelled, &mut all_rules)?;

    let mut next_interior = 0_u32;
    for rule in &mut all_rules {
        assign_interior_ids(&mut rule.lhs, &mut next_interior);
    }
    let mut rules = FxHashMap::<(CttState, Option<Symbol>), Vec<CttRule>>::default();
    for rule in all_rules {
        let root = match &rule.rhs {
            CttRhs::Variable(_) => None,
            CttRhs::Node(node, _) => Some(Symbol(
                u32::try_from(node.index()).expect("node count exceeds u32"),
            )),
        };
        rules.entry((rule.state, root)).or_default().push(rule);
    }
    Ok(Ctt {
        rules,
        final_state: annotations.start,
    })
}

fn assign_interior_ids(lhs: &mut CttLhs, next: &mut u32) {
    let CttLhs::Node {
        children, interior, ..
    } = lhs
    else {
        return;
    };
    *interior = *next;
    *next = next.checked_add(1).expect("CTT interior count exceeds u32");
    for child in children {
        assign_interior_ids(child, next);
    }
}

/// Add identity rules that select and annotate one root-to-rewrite path.
fn append_copy_rules(
    graph: &HncGraph,
    system: &RewriteSystem,
    annotation_names: &[String],
    annotation_ids: &HashMap<String, u32>,
    neutral_annotation: CttState,
    cancelled: impl Fn() -> bool,
    all_rules: &mut Vec<CttRule>,
) -> Result<(), FilterError> {
    for (index, node) in graph.parsed().nodes().iter().enumerate() {
        if cancelled() {
            return Err(FilterError::Cancelled);
        }
        let Some(label) = node.label() else {
            continue;
        };
        let node_id = NodeId::from_index(index);
        let arity = node.tree_children().len();
        let rhs_children = (0..arity)
            .map(|position| {
                CttRhs::Variable(u32::try_from(position).expect("node arity exceeds u32"))
            })
            .collect::<Vec<_>>();
        let neutral_children = (0..arity)
            .map(|position| {
                CttLhs::Variable(
                    CttState::Neutral,
                    u32::try_from(position).expect("node arity exceeds u32"),
                )
            })
            .collect::<Vec<_>>();
        // Neutral control copies the entire subtree without introducing a
        // rewrite site.
        all_rules.push(CttRule {
            lhs: CttLhs::Node {
                node: node_id,
                children: neutral_children,
                interior: u32::MAX,
            },
            state: CttState::Neutral,
            rhs: CttRhs::Node(node_id, rhs_children.clone()),
            variables: arity,
        });

        for parent_name in annotation_names {
            let parent_id = annotation_ids[parent_name];
            let children = if let Some(children) = system
                .annotations
                .get(parent_name)
                .and_then(|by_label| by_label.get(label))
            {
                if children.len() != arity {
                    return Err(FilterError::AnnotationArity {
                        annotation: parent_name.clone(),
                        label: label.to_owned(),
                        expected: arity,
                        actual: children.len(),
                    });
                }
                children
                    .iter()
                    .map(|name| CttState::Annotation(annotation_ids[name]))
                    .collect::<Vec<_>>()
            } else {
                vec![neutral_annotation; arity]
            };
            // Annotated control continues down exactly one child. All siblings
            // switch to neutral control and are copied unchanged.
            for (position, &propagated_state) in children.iter().enumerate() {
                let lhs_children = (0..arity)
                    .map(|child| {
                        CttLhs::Variable(
                            if child == position {
                                propagated_state
                            } else {
                                CttState::Neutral
                            },
                            u32::try_from(child).expect("node arity exceeds u32"),
                        )
                    })
                    .collect();
                all_rules.push(CttRule {
                    lhs: CttLhs::Node {
                        node: node_id,
                        children: lhs_children,
                        interior: u32::MAX,
                    },
                    state: CttState::Annotation(parent_id),
                    rhs: CttRhs::Node(node_id, rhs_children.clone()),
                    variables: arity,
                });
            }
        }
    }
    Ok(())
}

/// Add the non-identity rule that terminates each accepting CTT run.
fn append_rewrite_rules(
    graph: &HncGraph,
    system: &RewriteSystem,
    annotation_ids: &HashMap<String, u32>,
    cancelled: impl Fn() -> bool,
    all_rules: &mut Vec<CttRule>,
) -> Result<(), FilterError> {
    for rule in &system.rules {
        if cancelled() {
            return Err(FilterError::Cancelled);
        }
        let annotation_states = if let Some(annotation) = rule.annotation.as_deref() {
            vec![CttState::Annotation(annotation_ids[annotation])]
        } else {
            let mut states = annotation_ids.values().copied().collect::<Vec<_>>();
            states.sort_unstable();
            states.into_iter().map(CttState::Annotation).collect()
        };
        for (lhs, rhs) in specialize_rule(graph, rule)? {
            // An equation is represented by one stable orientation. The total
            // order is arbitrary but deterministic, so repeated filtering
            // chooses the same representative of each equivalence class.
            let (lhs, rhs) = if rule.oriented || lhs > rhs {
                (lhs, rhs)
            } else if rhs > lhs {
                (rhs, lhs)
            } else {
                continue;
            };
            let mut variables = HashMap::new();
            assign_variables(&lhs, &mut variables);
            let lhs = ctt_lhs(&lhs, &variables);
            let rhs = ctt_rhs(&rhs, &variables);
            for &state in &annotation_states {
                all_rules.push(CttRule {
                    lhs: lhs.clone(),
                    state,
                    rhs: rhs.clone(),
                    variables: variables.len(),
                });
            }
        }
    }
    Ok(())
}

fn assign_variables(pattern: &SpecializedPattern, variables: &mut HashMap<String, u32>) {
    match pattern {
        SpecializedPattern::Variable(name) => {
            let next = u32::try_from(variables.len()).expect("variable count exceeds u32");
            variables.entry(name.clone()).or_insert(next);
        }
        SpecializedPattern::Node(_, children) => {
            for child in children {
                assign_variables(child, variables);
            }
        }
    }
}

fn ctt_lhs(pattern: &SpecializedPattern, variables: &HashMap<String, u32>) -> CttLhs {
    match pattern {
        SpecializedPattern::Variable(name) => CttLhs::Variable(CttState::Neutral, variables[name]),
        SpecializedPattern::Node(node, children) => CttLhs::Node {
            node: *node,
            children: children
                .iter()
                .map(|child| ctt_lhs(child, variables))
                .collect(),
            interior: u32::MAX,
        },
    }
}

fn ctt_rhs(pattern: &SpecializedPattern, variables: &HashMap<String, u32>) -> CttRhs {
    match pattern {
        SpecializedPattern::Variable(name) => CttRhs::Variable(variables[name]),
        SpecializedPattern::Node(node, children) => CttRhs::Node(
            *node,
            children
                .iter()
                .map(|child| ctt_rhs(child, variables))
                .collect(),
        ),
    }
}

fn specialize_rule(
    graph: &HncGraph,
    rule: &RewriteRule,
) -> Result<Vec<(SpecializedPattern, SpecializedPattern)>, FilterError> {
    // Wildcards denote one common surrounding constructor. Expanding them
    // first reduces the remaining work to ordinary first-order patterns.
    let expanded = expand_wildcards(graph, &rule.lhs, &rule.rhs)?;
    let mut result = Vec::new();
    for (lhs, rhs) in expanded {
        let mut occurrences = HashMap::<String, (String, usize)>::new();
        collect_occurrence_shapes(&lhs, &mut occurrences);
        if !rhs_occurrences_match(&rhs, &occurrences) {
            continue;
        }
        let mut occurrences = occurrences.into_iter().collect::<Vec<_>>();
        occurrences.sort_by(|left, right| left.0.cmp(&right.0));
        // A pattern occurrence may be assigned only to graph nodes with the
        // same label and arity. Distinct occurrence names must denote distinct
        // nodes, even when their labels agree.
        let candidates = occurrences
            .iter()
            .map(|(_, (label, arity))| {
                graph
                    .parsed()
                    .nodes()
                    .iter()
                    .enumerate()
                    .filter(|(_, node)| {
                        node.label() == Some(label.as_str()) && node.tree_children().len() == *arity
                    })
                    .map(|(index, _)| NodeId::from_index(index))
                    .collect::<Vec<_>>()
            })
            .collect::<Vec<_>>();
        enumerate_distinct_assignments(&candidates, 0, &mut Vec::new(), &mut |assignment| {
            let mapping = occurrences
                .iter()
                .zip(assignment)
                .map(|((occurrence, _), &node)| (occurrence.clone(), node))
                .collect::<HashMap<_, _>>();
            result.push((
                instantiate_pattern(&lhs, &mapping),
                instantiate_pattern(&rhs, &mapping),
            ));
        });
    }
    Ok(result)
}

fn collect_occurrence_shapes(pattern: &Pattern, out: &mut HashMap<String, (String, usize)>) {
    match pattern {
        Pattern::Variable(_) => {}
        Pattern::Wildcard(inner) => collect_occurrence_shapes(inner, out),
        Pattern::Node {
            label,
            occurrence,
            children,
        } => {
            out.insert(occurrence.clone(), (label.clone(), children.len()));
            for child in children {
                collect_occurrence_shapes(child, out);
            }
        }
    }
}

fn rhs_occurrences_match(
    pattern: &Pattern,
    occurrences: &HashMap<String, (String, usize)>,
) -> bool {
    match pattern {
        Pattern::Variable(_) => true,
        Pattern::Wildcard(inner) => rhs_occurrences_match(inner, occurrences),
        Pattern::Node {
            label,
            occurrence,
            children,
        } => {
            occurrences
                .get(occurrence)
                .is_some_and(|(known_label, arity)| {
                    known_label == label && *arity == children.len()
                })
                && children
                    .iter()
                    .all(|child| rhs_occurrences_match(child, occurrences))
        }
    }
}

fn instantiate_pattern(pattern: &Pattern, mapping: &HashMap<String, NodeId>) -> SpecializedPattern {
    match pattern {
        Pattern::Variable(name) => SpecializedPattern::Variable(name.clone()),
        Pattern::Node {
            occurrence,
            children,
            ..
        } => SpecializedPattern::Node(
            mapping[occurrence],
            children
                .iter()
                .map(|child| instantiate_pattern(child, mapping))
                .collect(),
        ),
        Pattern::Wildcard(_) => unreachable!("wildcards are expanded before specialization"),
    }
}

fn expand_wildcards(
    graph: &HncGraph,
    lhs: &Pattern,
    rhs: &Pattern,
) -> Result<Vec<(Pattern, Pattern)>, FilterError> {
    fn count(pattern: &Pattern) -> usize {
        match pattern {
            Pattern::Variable(_) => 0,
            Pattern::Node { children, .. } => children.iter().map(count).sum(),
            Pattern::Wildcard(inner) => 1 + count(inner),
        }
    }
    let left_wildcards = count(lhs);
    let right_wildcards = count(rhs);
    if left_wildcards == 0 && right_wildcards == 0 {
        return Ok(vec![(lhs.clone(), rhs.clone())]);
    }
    if left_wildcards != 1 || right_wildcards != 1 {
        return Err(FilterError::InvalidWildcard {
            message: "a context rule must contain exactly one wildcard on each side".to_owned(),
        });
    }

    let mut shapes = graph
        .parsed()
        .nodes()
        .iter()
        .filter_map(|node| {
            node.label()
                .map(|label| (label.to_owned(), node.tree_children().len()))
        })
        .collect::<Vec<_>>();
    shapes.sort();
    shapes.dedup();
    let mut result = Vec::new();
    for (label, arity) in shapes {
        for position in 0..arity {
            let variables = (0..arity)
                .map(|child| Pattern::Variable(format!("\0wild_{child}")))
                .collect::<Vec<_>>();
            result.push((
                replace_wildcard(lhs, &label, position, &variables),
                replace_wildcard(rhs, &label, position, &variables),
            ));
        }
    }
    Ok(result)
}

fn replace_wildcard(
    pattern: &Pattern,
    label: &str,
    position: usize,
    variables: &[Pattern],
) -> Pattern {
    match pattern {
        Pattern::Variable(name) => Pattern::Variable(name.clone()),
        Pattern::Node {
            label: node_label,
            occurrence,
            children,
        } => Pattern::Node {
            label: node_label.clone(),
            occurrence: occurrence.clone(),
            children: children
                .iter()
                .map(|child| replace_wildcard(child, label, position, variables))
                .collect(),
        },
        Pattern::Wildcard(inner) => {
            let mut children = variables.to_vec();
            children[position] = replace_wildcard(inner, label, position, variables);
            Pattern::Node {
                label: label.to_owned(),
                occurrence: "\0wild".to_owned(),
                children,
            }
        }
    }
}

fn enumerate_distinct_assignments(
    choices: &[Vec<NodeId>],
    index: usize,
    current: &mut Vec<NodeId>,
    out: &mut impl FnMut(&[NodeId]),
) {
    if index == choices.len() {
        out(current);
        return;
    }
    for &choice in &choices[index] {
        if current.contains(&choice) {
            continue;
        }
        current.push(choice);
        enumerate_distinct_assignments(choices, index + 1, current, out);
        current.pop();
    }
}

// ---------------------------------------------------------------------------
// Fragment-chart expansion
// ---------------------------------------------------------------------------

/// Incrementally constructs the node-level form of the chart.
///
/// The two maps enforce the determinism required by preimage construction:
/// `(symbol, children)` determines one result state, and `(result, symbol)`
/// determines one child tuple.
struct ExpansionBuilder {
    builder: ExplicitBuilder,
    bottom_up: FxHashMap<(Symbol, Vec<StateId>), StateId>,
    top_down: FxHashMap<(StateId, Symbol), Vec<StateId>>,
}

struct NodeExpansion {
    /// Node-level automaton. Original chart states retain their IDs; states for
    /// nodes inside fragments are appended.
    automaton: Explicit,
    /// Deterministic top-down transitions, sorted by symbol within each state.
    top_down: Vec<Vec<(Symbol, Vec<StateId>)>>,
}

/// Expand every fragment transition into node transitions.
fn expand_chart(chart: &Chart, cancelled: impl Fn() -> bool) -> Result<NodeExpansion, FilterError> {
    let mut expansion = ExpansionBuilder {
        builder: ExplicitBuilder::new(),
        bottom_up: FxHashMap::default(),
        top_down: FxHashMap::default(),
    };
    for expected in 0..chart.fragment_automaton().automaton().num_states() {
        // Keeping boundary-state IDs stable lets fragment holes refer directly
        // to the original chart's child states during expansion.
        let state = expansion.builder.new_state();
        assert_eq!(state.0, expected);
    }
    chart
        .fragment_automaton()
        .automaton()
        .initial_states(&mut |state| expansion.builder.add_accepting(state));
    for rule in chart.fragment_automaton().automaton().rules() {
        if cancelled() {
            return Err(FilterError::Cancelled);
        }
        let fragment_automaton = chart.fragment_automaton();
        let fragment = fragment_automaton.fragment_root(rule.symbol);
        let mut next_socket = 0;
        // The expanded root must retain the original rule's result state;
        // interior fragment nodes receive fresh states as needed.
        expansion.expand_tree(
            chart.graph(),
            fragment_automaton.fragment_arena(),
            fragment,
            rule.children,
            &mut next_socket,
            Some(rule.result),
        )?;
        debug_assert_eq!(next_socket, rule.children.len());
    }
    let automaton = expansion.builder.build();
    let mut top_down = vec![Vec::new(); automaton.num_states() as usize];
    for ((state, symbol), children) in expansion.top_down {
        top_down[state.index()].push((symbol, children));
    }
    for rules in &mut top_down {
        rules.sort_unstable_by_key(|(symbol, _)| *symbol);
    }
    Ok(NodeExpansion {
        automaton,
        top_down,
    })
}

impl ExpansionBuilder {
    fn expand_tree(
        &mut self,
        graph: &HncGraph,
        fragments: &TreeArena<FragmentNode>,
        tree: Tree,
        socket_states: &[StateId],
        next_socket: &mut usize,
        required: Option<StateId>,
    ) -> Result<StateId, FilterError> {
        match fragments.get_label(tree) {
            FragmentNode::Hole(_) => {
                let state = socket_states[*next_socket];
                *next_socket += 1;
                Ok(state)
            }
            FragmentNode::Node(node) => {
                let child_states = fragments
                    .get_children(tree)
                    .iter()
                    .map(|&child| {
                        self.expand_tree(graph, fragments, child, socket_states, next_socket, None)
                    })
                    .collect::<Result<Vec<_>, _>>()?;
                let symbol = Symbol(u32::try_from(node.index()).expect("node count exceeds u32"));
                let key = (symbol, child_states.clone());
                let result = if let Some(&known) = self.bottom_up.get(&key) {
                    if required.is_some_and(|required| required != known) {
                        return Err(FilterError::NonDeterministicExpansion {
                            direction: "bottom-up",
                            node: graph.node(*node).name().to_owned(),
                        });
                    }
                    known
                } else {
                    let result = required.unwrap_or_else(|| self.builder.new_state());
                    self.bottom_up.insert(key, result);
                    if let Some(known) =
                        self.top_down.insert((result, symbol), child_states.clone())
                        && known != child_states
                    {
                        return Err(FilterError::NonDeterministicExpansion {
                            direction: "top-down",
                            node: graph.node(*node).name().to_owned(),
                        });
                    }
                    self.builder.add_rule(symbol, child_states, result);
                    result
                };
                Ok(result)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Inverse image of the node chart under the CTT
// ---------------------------------------------------------------------------

/// Distinguishes ordinary CTT configurations from states introduced inside a
/// compiled left-hand side.
#[derive(Clone, Debug, PartialEq, Eq, Hash)]
enum PreMarker {
    /// A configuration at a CTT rule boundary.
    Ctt(CttState),
    /// A constructor internal to one particular left-hand-side match.
    /// `target` is the target state at that match's root.
    Interior { target: StateId, interior: u32 },
}

/// A state of the inverse-image automaton.
///
/// Conceptually this is a product of a target-automaton state and a CTT
/// configuration. `marker` also prevents unrelated interior pattern nodes from
/// being identified merely because they happen to reach the same target state.
#[derive(Clone, Debug, PartialEq, Eq, Hash)]
struct PreState {
    target: StateId,
    marker: PreMarker,
}

#[derive(Default)]
struct PreBuilder {
    builder: GeneratedBuilder,
    states: FxHashMap<PreState, StateId>,
    /// Target component of each generated state, indexed by generated state ID.
    target_by_state: Vec<StateId>,
    /// Preimage rules can be reached through several agenda paths. Suppress
    /// exact duplicates before they enter the generated automaton.
    rules: FxHashSet<(Symbol, SmallVec<[StateId; 2]>, StateId)>,
}

impl PreBuilder {
    /// Intern a preimage state. The flag reports whether it was newly created;
    /// boundary-state callers use it to avoid duplicate agenda entries.
    fn state(&mut self, key: PreState) -> (StateId, bool) {
        if let Some(&state) = self.states.get(&key) {
            return (state, false);
        }
        let state = self.builder.new_state();
        assert_eq!(state.index(), self.target_by_state.len());
        self.target_by_state.push(key.target);
        self.states.insert(key, state);
        (state, true)
    }

    fn add_rule(&mut self, symbol: Symbol, children: SmallVec<[StateId; 2]>, result: StateId) {
        if self.rules.insert((symbol, children.clone(), result)) {
            self.builder.add_rule(symbol, children, result);
        }
    }
}

/// Construct `preimage(ctt, target)` as an unindexed generated automaton.
///
/// The caller trims the result before paying to build transition indexes.
fn compute_preimage(
    target: &NodeExpansion,
    ctt: &Ctt,
    cancelled: impl Fn() -> bool,
) -> Result<GeneratedBuilder, FilterError> {
    let mut output = PreBuilder::default();
    let mut agenda = VecDeque::new();
    target.automaton.initial_states(&mut |target_state| {
        let (state, is_new) = output.state(PreState {
            target: target_state,
            marker: PreMarker::Ctt(ctt.final_state),
        });
        if is_new {
            agenda.push_back((ctt.final_state, target_state));
        }
        output.builder.add_accepting(state);
    });

    // Work backwards from accepting target states. Each agenda item asks which
    // input trees the given CTT control state can map into `target_state`.
    while let Some((ctt_state, target_state)) = agenda.pop_front() {
        if cancelled() {
            return Err(FilterError::Cancelled);
        }
        // Variable-rooted CTT outputs are always candidates. Node-rooted
        // outputs need only be tried for symbols available at the target state.
        for root in std::iter::once(None).chain(
            target.top_down[target_state.index()]
                .iter()
                .map(|(symbol, _)| Some(*symbol)),
        ) {
            let Some(rules) = ctt.rules.get(&(ctt_state, root)) else {
                continue;
            };
            for rule in rules {
                let mut variables: SmallVec<[Option<StateId>; 8]> = smallvec![None; rule.variables];
                if !match_rhs(&rule.rhs, target_state, target, &mut variables) {
                    continue;
                }
                let mut leaves = SmallVec::<[(CttState, StateId); 8]>::new();
                LhsDecomposer {
                    root_target: target_state,
                    root_ctt: rule.state,
                    variables: &variables,
                    target,
                    output: &mut output,
                    leaves: &mut leaves,
                }
                .decompose(&rule.lhs, true);
                agenda.extend(leaves);
            }
        }
    }
    Ok(output.builder)
}

fn match_rhs(
    rhs: &CttRhs,
    state: StateId,
    target: &NodeExpansion,
    variables: &mut [Option<StateId>],
) -> bool {
    // The expanded target is top-down deterministic, so matching an output
    // template never branches: each constructor has at most one child tuple.
    match rhs {
        CttRhs::Variable(variable) => {
            let slot = &mut variables[*variable as usize];
            if let Some(known) = *slot {
                known == state
            } else {
                *slot = Some(state);
                true
            }
        }
        CttRhs::Node(node, children) => {
            let symbol = Symbol(u32::try_from(node.index()).expect("node count exceeds u32"));
            let rules = &target.top_down[state.index()];
            let Ok(index) = rules.binary_search_by_key(&symbol, |(candidate, _)| *candidate) else {
                return false;
            };
            let child_states = &rules[index].1;
            if child_states.len() != children.len() {
                return false;
            }
            for (child, child_state) in children.iter().zip(child_states) {
                if !match_rhs(child, *child_state, target, variables) {
                    return false;
                }
            }
            true
        }
    }
}

struct LhsDecomposer<'a> {
    root_target: StateId,
    root_ctt: CttState,
    variables: &'a [Option<StateId>],
    target: &'a NodeExpansion,
    output: &'a mut PreBuilder,
    leaves: &'a mut SmallVec<[(CttState, StateId); 8]>,
}

impl LhsDecomposer<'_> {
    /// Emit the input side of one successfully matched CTT rule.
    ///
    /// Variables lead back to agenda configurations. Constructors become
    /// transitions of the inverse-image automaton. The return value is the
    /// generated state at the root of `lhs`.
    fn decompose(&mut self, lhs: &CttLhs, top: bool) -> StateId {
        match lhs {
            CttLhs::Variable(state, variable) => {
                let target_state =
                    self.variables[*variable as usize].expect("matched CTT variable");
                let (preimage_state, is_new) = self.output.state(PreState {
                    target: target_state,
                    marker: PreMarker::Ctt(*state),
                });
                if is_new {
                    self.leaves.push((*state, target_state));
                }
                preimage_state
            }
            CttLhs::Node {
                node,
                children,
                interior,
            } => {
                let mut child_states = SmallVec::<[StateId; 2]>::new();
                let mut child_target_states = SmallVec::<[StateId; 4]>::new();
                for child in children {
                    let child_state = self.decompose(child, false);
                    child_target_states.push(self.output.target_by_state[child_state.index()]);
                    child_states.push(child_state);
                }
                let symbol = Symbol(u32::try_from(node.index()).expect("node count exceeds u32"));
                let target_state = if top {
                    self.root_target
                } else {
                    // An input-side constructor need not occur in the output
                    // tree after rewriting. In that case no target transition
                    // exists; the root target plus the unique interior marker
                    // still identifies the temporary pattern state safely.
                    self.target
                        .automaton
                        .step_det(symbol, &child_target_states)
                        .unwrap_or(self.root_target)
                };
                let marker = if top {
                    PreMarker::Ctt(self.root_ctt)
                } else {
                    PreMarker::Interior {
                        target: self.root_target,
                        interior: *interior,
                    }
                };
                let (state, _) = self.output.state(PreState {
                    target: target_state,
                    marker,
                });
                self.output.add_rule(symbol, child_states, state);
                state
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Difference on the original fragment alphabet
// ---------------------------------------------------------------------------

/// Interned identifier for a residual set of preimage states.
#[derive(Clone, Copy, Debug, PartialEq, Eq, Hash)]
struct ResidualId(u32);

impl ResidualId {
    fn index(self) -> usize {
        self.0 as usize
    }
}

#[derive(Default)]
struct ResidualInterner {
    ids: FxHashMap<Rc<[StateId]>, ResidualId>,
    sets: Vec<Rc<[StateId]>>,
}

impl ResidualInterner {
    /// Intern a canonical residual set.
    ///
    /// Callers must provide states in increasing order without duplicates.
    /// Keeping canonicalization at the transition boundary avoids sorting sets
    /// that are merely passed through fragment holes.
    fn intern(&mut self, states: SmallVec<[StateId; 8]>) -> ResidualId {
        debug_assert!(states.windows(2).all(|pair| pair[0] < pair[1]));
        if let Some(&id) = self.ids.get(states.as_slice()) {
            return id;
        }
        let id = ResidualId(u32::try_from(self.sets.len()).expect("residual count exceeds u32"));
        let states = Rc::<[StateId]>::from(states.into_vec());
        self.ids.insert(Rc::clone(&states), id);
        self.sets.push(states);
        id
    }

    fn get(&self, id: ResidualId) -> &[StateId] {
        &self.sets[id.index()]
    }

    fn avoids_accepting(&self, id: ResidualId, automaton: &Explicit) -> bool {
        self.get(id)
            .iter()
            .all(|state| !automaton.is_accepting(state))
    }
}

struct DerivedState {
    /// State in the original fragment chart.
    left: StateId,
    /// All preimage states that recognize the same partial derivation.
    residual: ResidualId,
}

/// Builds states `(left, residual)` of the language difference.
///
/// Such a state is accepting precisely when `left` is accepting in the source
/// chart and no state in `residual` is accepting in the bad-language automaton.
struct DifferenceBuilder {
    builder: GeneratedBuilder,
    states: FxHashMap<(StateId, ResidualId), StateId>,
    state_info: Vec<DerivedState>,
    states_by_left: Vec<Vec<StateId>>,
}

struct SiblingDegrees {
    /// Number of preimage rules containing each state at each child position.
    by_position: Vec<Vec<u32>>,
}

/// For one source-chart state, maps a preimage state to the derived alternatives
/// whose residual contains it.
///
/// Batched evaluation uses the alternative indexes as coordinates in the
/// Cartesian product of a fragment rule's child alternatives.
struct StateIncidence {
    alternatives: FxHashMap<StateId, SmallVec<[u32; 4]>>,
}

impl StateIncidence {
    fn build(
        partners: &[StateId],
        state_info: &[DerivedState],
        residuals: &ResidualInterner,
    ) -> Self {
        let mut alternatives = FxHashMap::default();
        for (index, &derived) in partners.iter().enumerate() {
            let index = u32::try_from(index).expect("partner count exceeds u32");
            for &state in residuals.get(state_info[derived.index()].residual) {
                alternatives
                    .entry(state)
                    .or_insert_with(SmallVec::new)
                    .push(index);
            }
        }
        Self { alternatives }
    }
}

impl SiblingDegrees {
    // Symbol-independent occurrence counts are compact and still distinguish
    // ubiquitous copy states from highly selective interior states. Set size
    // alone is a poor predictor of the corresponding posting-list volume.
    fn build(automaton: &Explicit) -> Self {
        let mut by_position = Vec::<Vec<u32>>::new();
        for rule in automaton.rules() {
            for (position, child) in rule.children.iter().enumerate() {
                if by_position.len() <= position {
                    by_position
                        .resize_with(position + 1, || vec![0; automaton.num_states() as usize]);
                }
                by_position[position][child.index()] =
                    by_position[position][child.index()].saturating_add(1);
            }
        }
        Self { by_position }
    }

    fn best_position(&self, child_state_sets: &[&[StateId]]) -> usize {
        child_state_sets
            .iter()
            .enumerate()
            .min_by_key(|(position, states)| self.cost(*position, states.iter().copied()))
            .map_or(0, |(position, _)| position)
    }

    fn best_incidence_position(&self, choices: &[&StateIncidence]) -> usize {
        choices
            .iter()
            .enumerate()
            .min_by_key(|(position, incidence)| {
                self.cost(*position, incidence.alternatives.keys().copied())
            })
            .map_or(0, |(position, _)| position)
    }

    fn cost(&self, position: usize, states: impl Iterator<Item = StateId>) -> u64 {
        let degrees = self.by_position.get(position);
        states
            .map(|state| {
                degrees
                    .and_then(|degrees| degrees.get(state.index()))
                    .copied()
                    .unwrap_or(0)
            })
            .map(u64::from)
            .sum()
    }
}

impl DifferenceBuilder {
    fn new(left_states: usize) -> Self {
        Self {
            builder: GeneratedBuilder::new(),
            states: FxHashMap::default(),
            state_info: Vec::new(),
            states_by_left: vec![Vec::new(); left_states],
        }
    }

    /// Intern the product state and record its source-chart provenance.
    fn state(&mut self, left: StateId, residual: ResidualId, accepting: bool) -> StateId {
        let key = (left, residual);
        if let Some(&state) = self.states.get(&key) {
            return state;
        }
        let state = self.builder.new_state();
        assert_eq!(state.index(), self.state_info.len());
        self.state_info.push(DerivedState { left, residual });
        self.states.insert(key, state);
        self.states_by_left[left.index()].push(state);
        if accepting {
            self.builder.add_accepting(state);
        }
        state
    }

    fn add_rule(
        &mut self,
        symbol: Symbol,
        children: &[StateId],
        left: StateId,
        residual: ResidualId,
        accepting: bool,
    ) {
        let result = self.state(left, residual, accepting);
        self.builder.add_rule(
            symbol,
            SmallVec::<[StateId; 2]>::from_slice(children),
            result,
        );
    }

    fn finish(self, chart: &Chart) -> Chart {
        // States are created bottom-up from productive source-chart states, so
        // every generated state is productive. Only coaccessibility trimming is
        // needed before rebuilding the indexed Explicit automaton.
        let trimmed = self.builder.trim_assuming_productive();
        let retained_sources = trimmed
            .source_states
            .iter()
            .map(|state| self.state_info[state.index()].left)
            .collect::<Vec<_>>();
        Chart::from_filtered_automaton(chart, trimmed.automaton, &retained_sources)
    }
}

/// Compute `chart - bad_language` while retaining the chart's fragment alphabet.
fn difference_on_fragments(
    chart: &Chart,
    bad_language: &Explicit,
    cancelled: impl Fn() -> bool,
) -> Result<Chart, FilterError> {
    let automaton = chart.fragment_automaton().automaton();
    let state_order = bottom_up_states(automaton)?;
    let mut output = DifferenceBuilder::new(automaton.num_states() as usize);
    let mut residuals = ResidualInterner::default();
    let sibling_degrees = SiblingDegrees::build(bad_language);
    let mut incidences = (0..automaton.num_states())
        .map(|_| None)
        .collect::<Vec<Option<StateIncidence>>>();

    // Bottom-up order is essential: every child's derived alternatives and
    // incidence index must be complete before a parent rule is evaluated.
    for left_result in state_order {
        if cancelled() {
            return Err(FilterError::Cancelled);
        }
        for rule in automaton.rules_topdown(left_result) {
            let child_alternatives = rule
                .children
                .iter()
                .map(|child| output.states_by_left[child.index()].clone())
                .collect::<Vec<_>>();
            let fragment_automaton = chart.fragment_automaton();
            let fragment = fragment_automaton.fragment_root(rule.symbol);
            let left_accepting = automaton.is_accepting(&left_result);
            if let Some(symbol) = batched_fragment_symbol(
                fragment_automaton.fragment_arena(),
                fragment,
                child_alternatives.len(),
            ) {
                // The common case is a single semantic node whose children are
                // exactly the fragment holes. Evaluate all child combinations
                // together so each sibling posting list is scanned only once.
                let child_incidences = rule
                    .children
                    .iter()
                    .map(|child| {
                        incidences[child.index()]
                            .as_ref()
                            .expect("child incidence is complete in bottom-up order")
                    })
                    .collect::<SmallVec<[_; 4]>>();
                BatchedEvaluator {
                    bad_language,
                    sibling_degrees: &sibling_degrees,
                    residuals: &mut residuals,
                    output: &mut output,
                }
                .evaluate(
                    rule.symbol,
                    symbol,
                    &child_alternatives,
                    &child_incidences,
                    left_result,
                    left_accepting,
                );
                continue;
            }
            // Complex fragments may contain several semantic nodes. Evaluate
            // them compositionally for each child-state combination.
            let mut derived_children = Vec::with_capacity(child_alternatives.len());
            enumerate_state_products(
                &child_alternatives,
                0,
                &mut derived_children,
                &mut |derived_children| {
                    let mut next_socket = 0;
                    let residual = FragmentEvaluator {
                        fragments: fragment_automaton.fragment_arena(),
                        state_info: &output.state_info,
                        bad_language,
                        sibling_degrees: &sibling_degrees,
                        residuals: &mut residuals,
                    }
                    .evaluate(fragment, derived_children, &mut next_socket);
                    debug_assert_eq!(next_socket, derived_children.len());
                    output.add_rule(
                        rule.symbol,
                        derived_children,
                        left_result,
                        residual,
                        left_accepting && residuals.avoids_accepting(residual, bad_language),
                    );
                },
            );
        }
        incidences[left_result.index()] = Some(StateIncidence::build(
            &output.states_by_left[left_result.index()],
            &output.state_info,
            &residuals,
        ));
    }
    Ok(output.finish(chart))
}

/// Return the semantic-node symbol when a fragment can use batched evaluation.
///
/// Unary and nullary fragments use the ordinary evaluator: there is no sibling
/// Cartesian product to avoid, so constructing incidence machinery would add
/// overhead without reducing search.
fn batched_fragment_symbol(
    fragments: &TreeArena<FragmentNode>,
    tree: Tree,
    sockets: usize,
) -> Option<Symbol> {
    let FragmentNode::Node(node) = fragments.get_label(tree) else {
        return None;
    };
    let children = fragments.get_children(tree);
    (sockets >= 2
        && children.len() == sockets
        && children
            .iter()
            .all(|&child| matches!(fragments.get_label(child), FragmentNode::Hole(_))))
    .then(|| Symbol(u32::try_from(node.index()).expect("node count exceeds u32")))
}

struct BatchedEvaluator<'a> {
    bad_language: &'a Explicit,
    sibling_degrees: &'a SiblingDegrees,
    residuals: &'a mut ResidualInterner,
    output: &'a mut DifferenceBuilder,
}

impl BatchedEvaluator<'_> {
    /// Evaluate one single-node fragment rule for all derived child tuples.
    ///
    /// A dense `cells` array represents the Cartesian product in row-major
    /// order. Each matching preimage transition contributes its result state to
    /// every cell compatible with its children. We then intern one residual per
    /// cell and emit the corresponding difference rule.
    fn evaluate(
        &mut self,
        rule_symbol: Symbol,
        node_symbol: Symbol,
        child_alternatives: &[Vec<StateId>],
        incidences: &[&StateIncidence],
        left_result: StateId,
        left_accepting: bool,
    ) {
        let cell_count = state_product_count(child_alternatives);
        if cell_count == 0 {
            return;
        }
        let mut strides = SmallVec::<[usize; 4]>::from_elem(1, child_alternatives.len());
        for position in (0..child_alternatives.len().saturating_sub(1)).rev() {
            strides[position] = strides[position + 1] * child_alternatives[position + 1].len();
        }
        let mut cells = (0..cell_count)
            .map(|_| SmallVec::<[StateId; 8]>::new())
            .collect::<Vec<_>>();
        // Start from the position with the smallest estimated posting volume;
        // the remaining children are checked through incidence hash lookups.
        let trigger_position = self.sibling_degrees.best_incidence_position(incidences);
        for trigger in incidences[trigger_position].alternatives.keys() {
            self.bad_language.step_partial(
                node_symbol,
                trigger_position,
                trigger,
                &mut |children, result| {
                    if children.len() != incidences.len() {
                        return;
                    }
                    let mut alternatives = SmallVec::<[&[u32]; 4]>::new();
                    for (child, incidence) in children.iter().zip(incidences) {
                        let Some(indices) = incidence.alternatives.get(child) else {
                            return;
                        };
                        alternatives.push(indices);
                    }
                    accumulate_batched_results(&alternatives, &strides, 0, 0, result, &mut cells);
                },
            );
        }

        let mut cell = 0;
        enumerate_state_products(
            child_alternatives,
            0,
            &mut Vec::new(),
            &mut |derived_children| {
                let mut states = std::mem::take(&mut cells[cell]);
                cell += 1;
                states.sort_unstable();
                states.dedup();
                let residual = self.residuals.intern(states);
                self.output.add_rule(
                    rule_symbol,
                    derived_children,
                    left_result,
                    residual,
                    left_accepting && self.residuals.avoids_accepting(residual, self.bad_language),
                );
            },
        );
        debug_assert_eq!(cell, cell_count);
    }
}

fn accumulate_batched_results(
    alternatives: &[&[u32]],
    strides: &[usize],
    position: usize,
    cell: usize,
    result: StateId,
    cells: &mut [SmallVec<[StateId; 8]>],
) {
    if position == alternatives.len() {
        cells[cell].push(result);
        return;
    }
    for &alternative in alternatives[position] {
        accumulate_batched_results(
            alternatives,
            strides,
            position + 1,
            cell + alternative as usize * strides[position],
            result,
            cells,
        );
    }
}

fn state_product_count<T: AsRef<[StateId]>>(alternatives: &[T]) -> usize {
    alternatives.iter().fold(1_usize, |product, alternatives| {
        product
            .checked_mul(alternatives.as_ref().len())
            .expect("difference product count exceeds usize")
    })
}

struct FragmentEvaluator<'a> {
    fragments: &'a TreeArena<FragmentNode>,
    state_info: &'a [DerivedState],
    bad_language: &'a Explicit,
    sibling_degrees: &'a SiblingDegrees,
    residuals: &'a mut ResidualInterner,
}

impl FragmentEvaluator<'_> {
    /// Evaluate a complex fragment bottom-up, substituting a child residual at
    /// each hole and applying one preimage transition at each semantic node.
    fn evaluate(
        &mut self,
        tree: Tree,
        derived_children: &[StateId],
        next_socket: &mut usize,
    ) -> ResidualId {
        match self.fragments.get_label(tree) {
            FragmentNode::Hole(_) => {
                let child = derived_children[*next_socket];
                *next_socket += 1;
                self.state_info[child.index()].residual
            }
            FragmentNode::Node(node) => {
                let mut child_residuals = SmallVec::<[ResidualId; 4]>::new();
                for &child in self.fragments.get_children(tree) {
                    child_residuals.push(self.evaluate(child, derived_children, next_socket));
                }
                let symbol = Symbol(u32::try_from(node.index()).expect("node count exceeds u32"));
                let result = {
                    let child_state_sets = child_residuals
                        .iter()
                        .map(|&residual| self.residuals.get(residual))
                        .collect::<SmallVec<[_; 4]>>();
                    transition_over_state_sets(
                        symbol,
                        &child_state_sets,
                        self.bad_language,
                        self.sibling_degrees,
                    )
                };
                self.residuals.intern(result)
            }
        }
    }
}

fn transition_over_state_sets(
    symbol: Symbol,
    child_state_sets: &[&[StateId]],
    automaton: &Explicit,
    sibling_degrees: &SiblingDegrees,
) -> SmallVec<[StateId; 8]> {
    debug_assert!(
        child_state_sets
            .iter()
            .all(|states| states.windows(2).all(|pair| pair[0] < pair[1]))
    );
    let mut results = SmallVec::<[StateId; 8]>::new();
    // Exact tuple lookup wins for tiny products; beyond that, enumerate actual
    // rules from the least frequent child position and test their siblings.
    if child_state_sets.len() >= 2 && state_product_count(child_state_sets) > 4 {
        let trigger_position = sibling_degrees.best_position(child_state_sets);
        for trigger in child_state_sets[trigger_position] {
            automaton.step_partial(
                symbol,
                trigger_position,
                trigger,
                &mut |children, result| {
                    if children.len() == child_state_sets.len()
                        && children
                            .iter()
                            .zip(child_state_sets)
                            .all(|(child, allowed)| allowed.binary_search(child).is_ok())
                    {
                        results.push(result);
                    }
                },
            );
        }
    } else {
        enumerate_state_products(child_state_sets, 0, &mut Vec::new(), &mut |states| {
            automaton.step(symbol, states, &mut |state| {
                results.push(state);
            });
        });
    }
    results.sort_unstable();
    results.dedup();
    results
}

fn enumerate_state_products<T: AsRef<[StateId]>>(
    alternatives: &[T],
    index: usize,
    current: &mut Vec<StateId>,
    out: &mut impl FnMut(&[StateId]),
) {
    if index == alternatives.len() {
        out(current);
        return;
    }
    for &choice in alternatives[index].as_ref() {
        current.push(choice);
        enumerate_state_products(alternatives, index + 1, current, out);
        current.pop();
    }
}

fn bottom_up_states(automaton: &Explicit) -> Result<Vec<StateId>, FilterError> {
    // The source chart represents a finite language, hence its productive state
    // dependencies must be acyclic. The DFS both checks this invariant and
    // produces the order required by difference construction.
    fn visit(
        automaton: &Explicit,
        state: StateId,
        marks: &mut [u8],
        output: &mut Vec<StateId>,
    ) -> Result<(), FilterError> {
        match marks[state.index()] {
            2 => return Ok(()),
            1 => return Err(FilterError::CyclicChart),
            _ => {}
        }
        marks[state.index()] = 1;
        for rule in automaton.rules_topdown(state) {
            for &child in rule.children {
                visit(automaton, child, marks, output)?;
            }
        }
        marks[state.index()] = 2;
        output.push(state);
        Ok(())
    }

    let mut marks = vec![0; automaton.num_states() as usize];
    let mut output = Vec::new();
    for state in 0..automaton.num_states() {
        visit(automaton, StateId(state), &mut marks, &mut output)?;
    }
    Ok(output)
}

// ---------------------------------------------------------------------------
// Public operation
// ---------------------------------------------------------------------------

/// Remove chart derivations which have a preferred one-step rewrite target in
/// the same chart language.
///
/// This operation works on automata and does not enumerate solutions.
///
/// # Errors
///
/// Returns an error when the rewrite system is invalid for the graph, the chart
/// violates filtering invariants, or cancellation is requested.
pub fn filter_chart(
    chart: &Chart,
    system: &RewriteSystem,
    cancelled: impl Fn() -> bool + Copy,
) -> Result<Chart, FilterError> {
    if chart.count_solutions() == 0_u8.into() {
        return Ok(Chart::empty_filter_result(chart));
    }
    if cancelled() {
        return Err(FilterError::Cancelled);
    }
    let node_chart = expand_chart(chart, cancelled)?;
    let rewrite = build_ctt(chart.graph(), system, cancelled)?;
    let bad_language = compute_preimage(&node_chart, &rewrite, cancelled)?
        .trim()
        .automaton;
    difference_on_fragments(chart, &bad_language, cancelled)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn sibling_indexed_set_transition_matches_cartesian_transition() {
        let mut builder = ExplicitBuilder::new();
        let left = [
            builder.new_state(),
            builder.new_state(),
            builder.new_state(),
        ];
        let right = [builder.new_state(), builder.new_state()];
        let outside = builder.new_state();
        let results = [builder.new_state(), builder.new_state()];
        let symbol = Symbol(7);
        builder.add_rule(symbol, vec![left[0], right[0]], results[0]);
        builder.add_rule(symbol, vec![left[1], right[1]], results[1]);
        builder.add_rule(symbol, vec![left[2], right[0]], results[0]);
        builder.add_rule(symbol, vec![outside, right[0]], results[1]);
        let automaton = builder.build();
        let choices = vec![left.to_vec(), right.to_vec()];
        let choice_slices = choices.iter().map(Vec::as_slice).collect::<Vec<_>>();
        let degrees = SiblingDegrees::build(&automaton);

        let mut cartesian = Vec::new();
        enumerate_state_products(&choices, 0, &mut Vec::new(), &mut |children| {
            automaton.step(symbol, children, &mut |result| cartesian.push(result));
        });
        cartesian.sort_unstable();
        cartesian.dedup();
        let sibling = transition_over_state_sets(symbol, &choice_slices, &automaton, &degrees);

        assert_eq!(sibling.as_slice(), cartesian);
        assert_eq!(sibling.as_slice(), results);
    }

    #[test]
    fn parsing_suppresses_exact_duplicate_rewrite_rules() {
        let equation = "a#1(X,a#2(Y,Z)) = a#2(Y,a#1(X,Z))";
        let weakening = "[+] a#1(X,a#2(Y,Z)) -> a#2(Y,a#1(X,Z))";
        let system =
            RewriteSystem::parse(&format!("{equation}\n{equation}\n{weakening}\n{weakening}"))
                .unwrap();

        assert_eq!(system.rules.len(), 2);
        assert!(!system.rules[0].oriented);
        assert!(system.rules[1].oriented);
        assert_eq!(system.rules[1].annotation.as_deref(), Some("+"));
    }

    #[test]
    fn parses_utool_rewrite_surface_and_preserves_occurrences() {
        let system = RewriteSystem::parse(
            "start annotation: +\nneutral annotation: 0\n+: a(+,+)\n[+] a(X,every(Y,Z)) -> every(Y,a(X,Z))\na#1(X,a#2(Y,Z)) = a#2(Y,a#1(X,Z))",
        )
        .unwrap();
        assert_eq!(system.rules.len(), 2);
        assert!(system.rules[0].oriented);
        assert!(!system.rules[1].oriented);
        let Pattern::Node { occurrence, .. } = &system.rules[1].lhs else {
            panic!("expected a constructor")
        };
        assert_eq!(occurrence, "1");
    }
}
