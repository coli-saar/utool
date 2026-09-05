//! Relative-normal-form filtering over compact fragment automata.

use crate::automata_ext::trim;
use crate::graph::{HncGraph, NodeId};
use crate::solver::{Chart, FragmentNode};
use packed_term_arena::tree::{Tree, TreeArena};
use rusty_alto::{
    BottomUpTa, DetBottomUpTa, Explicit, ExplicitBuilder, StateId, Symbol, TopDownTa,
};
use std::collections::{HashMap, HashSet, VecDeque};
use thiserror::Error;

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
    /// Whether this is a directed weakening rule.
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
        let mut unique_rules = HashSet::new();
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

#[allow(clippy::too_many_arguments)]
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
    if left.keys().collect::<HashSet<_>>() != right.keys().collect::<HashSet<_>>() {
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

#[derive(Clone, Debug, PartialEq, Eq, Hash, PartialOrd, Ord)]
enum SpecializedPattern {
    Variable(String),
    Node(NodeId, Vec<SpecializedPattern>),
}

#[derive(Clone, Copy, Debug, PartialEq, Eq, Hash)]
enum CttState {
    Neutral,
    Annotation(u32),
}

#[derive(Clone, Debug)]
enum CttLhs {
    Variable(CttState, u32),
    Node(NodeId, Vec<CttLhs>),
}

#[derive(Clone, Debug)]
enum CttRhs {
    Variable(u32),
    Node(NodeId, Vec<CttRhs>),
}

#[derive(Clone, Debug)]
struct CttRule {
    lhs: CttLhs,
    state: CttState,
    rhs: CttRhs,
}

struct Ctt {
    rules: HashMap<CttState, Vec<(u32, CttRule)>>,
    final_state: CttState,
}

fn intern_annotation(name: &str, names: &mut Vec<String>, ids: &mut HashMap<String, u32>) {
    if !ids.contains_key(name) {
        let id = u32::try_from(names.len()).expect("annotation count exceeds u32");
        names.push(name.to_owned());
        ids.insert(name.to_owned(), id);
    }
}

fn build_ctt(
    graph: &HncGraph,
    system: &RewriteSystem,
    cancelled: impl Fn() -> bool + Copy,
) -> Result<Ctt, FilterError> {
    let mut annotation_names = Vec::new();
    let mut annotation_ids = HashMap::new();
    if let Some(name) = system.start_annotation.as_deref() {
        intern_annotation(name, &mut annotation_names, &mut annotation_ids);
    }
    if let Some(name) = system.neutral_annotation.as_deref() {
        intern_annotation(name, &mut annotation_names, &mut annotation_ids);
    }
    for rule in &system.rules {
        if let Some(name) = rule.annotation.as_deref() {
            intern_annotation(name, &mut annotation_names, &mut annotation_ids);
        }
    }
    let mut propagated_annotations = system
        .annotations
        .iter()
        .flat_map(|(parent, by_label)| std::iter::once(parent).chain(by_label.values().flatten()))
        .cloned()
        .collect::<Vec<_>>();
    propagated_annotations.sort();
    propagated_annotations.dedup();
    for annotation in propagated_annotations {
        intern_annotation(&annotation, &mut annotation_names, &mut annotation_ids);
    }
    if annotation_names.is_empty() {
        intern_annotation("*", &mut annotation_names, &mut annotation_ids);
    }

    let start_name = system
        .start_annotation
        .as_deref()
        .unwrap_or(&annotation_names[0]);
    let neutral_name = system.neutral_annotation.as_deref().unwrap_or(start_name);
    let final_state = CttState::Annotation(annotation_ids[start_name]);
    let neutral_annotation = CttState::Annotation(annotation_ids[neutral_name]);
    let mut all_rules = Vec::new();

    append_copy_rules(
        graph,
        system,
        &annotation_names,
        &annotation_ids,
        neutral_annotation,
        cancelled,
        &mut all_rules,
    )?;
    append_rewrite_rules(graph, system, &annotation_ids, cancelled, &mut all_rules)?;

    let mut rules = HashMap::<CttState, Vec<(u32, CttRule)>>::new();
    for (index, rule) in all_rules.into_iter().enumerate() {
        rules.entry(rule.state).or_default().push((
            u32::try_from(index).expect("CTT rule count exceeds u32"),
            rule,
        ));
    }
    Ok(Ctt { rules, final_state })
}

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
        all_rules.push(CttRule {
            lhs: CttLhs::Node(node_id, neutral_children),
            state: CttState::Neutral,
            rhs: CttRhs::Node(node_id, rhs_children.clone()),
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
                    lhs: CttLhs::Node(node_id, lhs_children),
                    state: CttState::Annotation(parent_id),
                    rhs: CttRhs::Node(node_id, rhs_children.clone()),
                });
            }
        }
    }
    Ok(())
}

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
        for (lhs, rhs) in specialize_rule(graph, rule)? {
            let (lhs, rhs) = if rule.oriented || lhs > rhs {
                (lhs, rhs)
            } else if rhs > lhs {
                (rhs, lhs)
            } else {
                continue;
            };
            let annotation_states = if let Some(annotation) = rule.annotation.as_deref() {
                vec![CttState::Annotation(annotation_ids[annotation])]
            } else {
                let mut states = annotation_ids.values().copied().collect::<Vec<_>>();
                states.sort_unstable();
                states.into_iter().map(CttState::Annotation).collect()
            };
            let mut variables = HashMap::new();
            assign_variables(&lhs, &mut variables);
            let lhs = ctt_lhs(&lhs, &variables);
            let rhs = ctt_rhs(&rhs, &variables);
            for state in annotation_states {
                all_rules.push(CttRule {
                    lhs: lhs.clone(),
                    state,
                    rhs: rhs.clone(),
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
        SpecializedPattern::Node(node, children) => CttLhs::Node(
            *node,
            children
                .iter()
                .map(|child| ctt_lhs(child, variables))
                .collect(),
        ),
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

struct NodeExpansion {
    automaton: Explicit,
}

struct ExpansionBuilder {
    builder: ExplicitBuilder,
    bottom_up: HashMap<(Symbol, Vec<StateId>), StateId>,
    top_down: HashMap<(StateId, Symbol), Vec<StateId>>,
}

fn expand_chart(chart: &Chart, cancelled: impl Fn() -> bool) -> Result<NodeExpansion, FilterError> {
    let mut expansion = ExpansionBuilder {
        builder: ExplicitBuilder::new(),
        bottom_up: HashMap::new(),
        top_down: HashMap::new(),
    };
    for expected in 0..chart.fragment_automaton().automaton().num_states() {
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
    Ok(NodeExpansion {
        automaton: expansion.builder.build(),
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

#[derive(Clone, Debug, PartialEq, Eq, Hash)]
enum PreMarker {
    Ctt(CttState),
    Interior {
        rule: u32,
        target: StateId,
        path: Vec<u16>,
    },
}

#[derive(Clone, Debug, PartialEq, Eq, Hash)]
struct PreState {
    target: StateId,
    marker: PreMarker,
}

struct PreBuilder {
    builder: ExplicitBuilder,
    states: HashMap<PreState, StateId>,
    keys: Vec<PreState>,
    rules: HashSet<(Symbol, Vec<StateId>, StateId)>,
}

impl PreBuilder {
    fn new() -> Self {
        Self {
            builder: ExplicitBuilder::new(),
            states: HashMap::new(),
            keys: Vec::new(),
            rules: HashSet::new(),
        }
    }

    fn state(&mut self, key: PreState) -> StateId {
        if let Some(&state) = self.states.get(&key) {
            return state;
        }
        let state = self.builder.new_state();
        assert_eq!(state.index(), self.keys.len());
        self.keys.push(key.clone());
        self.states.insert(key, state);
        state
    }

    fn add_rule(&mut self, symbol: Symbol, children: Vec<StateId>, result: StateId) {
        if self.rules.insert((symbol, children.clone(), result)) {
            self.builder.add_rule(symbol, children, result);
        }
    }
}

fn compute_preimage(
    graph: &HncGraph,
    target: &Explicit,
    ctt: &Ctt,
    cancelled: impl Fn() -> bool,
) -> Result<Explicit, FilterError> {
    let mut output = PreBuilder::new();
    let mut agenda = VecDeque::new();
    let mut seen = HashSet::new();
    target.initial_states(&mut |target_state| {
        let pair = (ctt.final_state, target_state);
        if seen.insert(pair) {
            agenda.push_back(pair);
        }
        let state = output.state(PreState {
            target: target_state,
            marker: PreMarker::Ctt(ctt.final_state),
        });
        output.builder.add_accepting(state);
    });

    while let Some((ctt_state, target_state)) = agenda.pop_front() {
        if cancelled() {
            return Err(FilterError::Cancelled);
        }
        let Some(rules) = ctt.rules.get(&ctt_state) else {
            continue;
        };
        for (rule_id, rule) in rules {
            let mut variables = HashMap::new();
            if !match_rhs(graph, &rule.rhs, target_state, target, &mut variables)? {
                continue;
            }
            let mut leaves = Vec::new();
            decompose_lhs(
                &rule.lhs,
                true,
                &mut Vec::new(),
                *rule_id,
                target_state,
                rule.state,
                &variables,
                target,
                &mut output,
                &mut leaves,
            );
            for pair in leaves {
                if seen.insert(pair) {
                    agenda.push_back(pair);
                }
            }
        }
    }
    Ok(output.builder.build())
}

fn match_rhs(
    graph: &HncGraph,
    rhs: &CttRhs,
    state: StateId,
    target: &Explicit,
    variables: &mut HashMap<u32, StateId>,
) -> Result<bool, FilterError> {
    match rhs {
        CttRhs::Variable(variable) => Ok(variables
            .insert(*variable, state)
            .is_none_or(|known| known == state)),
        CttRhs::Node(node, children) => {
            let symbol = Symbol(u32::try_from(node.index()).expect("node count exceeds u32"));
            let mut matching = Vec::new();
            target.step_topdown(&state, &mut |candidate, child_states| {
                if candidate == symbol {
                    matching.push(child_states.to_vec());
                }
            });
            if matching.len() > 1 {
                return Err(FilterError::NonDeterministicExpansion {
                    direction: "top-down",
                    node: graph.node(*node).name().to_owned(),
                });
            }
            let Some(child_states) = matching.pop() else {
                return Ok(false);
            };
            if child_states.len() != children.len() {
                return Ok(false);
            }
            for (child, child_state) in children.iter().zip(child_states) {
                if !match_rhs(graph, child, child_state, target, variables)? {
                    return Ok(false);
                }
            }
            Ok(true)
        }
    }
}

#[allow(clippy::too_many_arguments)]
fn decompose_lhs(
    lhs: &CttLhs,
    top: bool,
    path: &mut Vec<u16>,
    rule_id: u32,
    root_target: StateId,
    root_ctt: CttState,
    variables: &HashMap<u32, StateId>,
    target: &Explicit,
    output: &mut PreBuilder,
    leaves: &mut Vec<(CttState, StateId)>,
) -> StateId {
    match lhs {
        CttLhs::Variable(state, variable) => {
            let target_state = variables[variable];
            leaves.push((*state, target_state));
            output.state(PreState {
                target: target_state,
                marker: PreMarker::Ctt(*state),
            })
        }
        CttLhs::Node(node, children) => {
            let mut child_states = Vec::with_capacity(children.len());
            let mut child_target_states = Vec::with_capacity(children.len());
            for (index, child) in children.iter().enumerate() {
                path.push(u16::try_from(index).expect("rewrite pattern arity exceeds u16"));
                let child_state = decompose_lhs(
                    child,
                    false,
                    path,
                    rule_id,
                    root_target,
                    root_ctt,
                    variables,
                    target,
                    output,
                    leaves,
                );
                path.pop();
                child_target_states.push(output.keys[child_state.index()].target);
                child_states.push(child_state);
            }
            let symbol = Symbol(u32::try_from(node.index()).expect("node count exceeds u32"));
            let target_state = if top {
                root_target
            } else {
                target
                    .step_det(symbol, &child_target_states)
                    .unwrap_or(root_target)
            };
            let marker = if top {
                PreMarker::Ctt(root_ctt)
            } else {
                PreMarker::Interior {
                    rule: rule_id,
                    target: root_target,
                    path: path.clone(),
                }
            };
            let state = output.state(PreState {
                target: target_state,
                marker,
            });
            output.add_rule(symbol, child_states, state);
            state
        }
    }
}

#[derive(Clone)]
struct DerivedState {
    left: StateId,
    residual: Vec<StateId>,
}

struct DifferenceBuilder {
    builder: ExplicitBuilder,
    states: HashMap<(StateId, Vec<StateId>), StateId>,
    info: Vec<DerivedState>,
    partners: Vec<Vec<StateId>>,
    rules: HashSet<(Symbol, Vec<StateId>, StateId)>,
}

impl DifferenceBuilder {
    fn new(left_states: usize) -> Self {
        Self {
            builder: ExplicitBuilder::new(),
            states: HashMap::new(),
            info: Vec::new(),
            partners: vec![Vec::new(); left_states],
            rules: HashSet::new(),
        }
    }

    fn state(&mut self, left: StateId, residual: Vec<StateId>, accepting: bool) -> StateId {
        let key = (left, residual.clone());
        if let Some(&state) = self.states.get(&key) {
            return state;
        }
        let state = self.builder.new_state();
        assert_eq!(state.index(), self.info.len());
        self.info.push(DerivedState { left, residual });
        self.states.insert(key, state);
        self.partners[left.index()].push(state);
        if accepting {
            self.builder.add_accepting(state);
        }
        state
    }

    fn add_rule(&mut self, symbol: Symbol, children: Vec<StateId>, result: StateId) {
        if self.rules.insert((symbol, children.clone(), result)) {
            self.builder.add_rule(symbol, children, result);
        }
    }
}

fn difference_on_fragments(
    chart: &Chart,
    preimage: &Explicit,
    cancelled: impl Fn() -> bool,
) -> Result<Chart, FilterError> {
    let automaton = chart.fragment_automaton().automaton();
    let rules = bottom_up_rules(automaton)?;
    let mut output = DifferenceBuilder::new(automaton.num_states() as usize);
    let mut left_accepting = vec![false; automaton.num_states() as usize];
    automaton.initial_states(&mut |state| {
        left_accepting[state.index()] = true;
    });

    for (symbol, left_children, left_result) in rules {
        if cancelled() {
            return Err(FilterError::Cancelled);
        }
        let choices = left_children
            .iter()
            .map(|child| output.partners[child.index()].clone())
            .collect::<Vec<_>>();
        enumerate_state_products(&choices, 0, &mut Vec::new(), &mut |derived_children| {
            let fragment_automaton = chart.fragment_automaton();
            let fragment = fragment_automaton.fragment_root(symbol);
            let mut next_socket = 0;
            let residual = evaluate_fragment(
                fragment_automaton.fragment_arena(),
                fragment,
                derived_children,
                &output.info,
                &mut next_socket,
                preimage,
            );
            debug_assert_eq!(next_socket, derived_children.len());
            let accepting = left_accepting[left_result.index()]
                && residual.iter().all(|state| !preimage.is_accepting(state));
            let result = output.state(left_result, residual, accepting);
            output.add_rule(symbol, derived_children.to_vec(), result);
        });
    }

    let untrimmed_sources = output
        .info
        .iter()
        .map(|state| state.left)
        .collect::<Vec<_>>();
    let untrimmed = output.builder.build();
    let trimmed = trim(&untrimmed);
    let source_states = trimmed
        .source_states
        .iter()
        .map(|state| untrimmed_sources[state.index()])
        .collect::<Vec<_>>();
    Ok(Chart::from_filtered_automaton(
        chart,
        trimmed.automaton,
        &source_states,
    ))
}

fn evaluate_fragment(
    fragments: &TreeArena<FragmentNode>,
    tree: Tree,
    derived_children: &[StateId],
    derived_info: &[DerivedState],
    next_socket: &mut usize,
    automaton: &Explicit,
) -> Vec<StateId> {
    match fragments.get_label(tree) {
        FragmentNode::Hole(_) => {
            let child = derived_children[*next_socket];
            *next_socket += 1;
            derived_info[child.index()].residual.clone()
        }
        FragmentNode::Node(node) => {
            let choices = fragments
                .get_children(tree)
                .iter()
                .map(|&child| {
                    evaluate_fragment(
                        fragments,
                        child,
                        derived_children,
                        derived_info,
                        next_socket,
                        automaton,
                    )
                })
                .collect::<Vec<_>>();
            let symbol = Symbol(u32::try_from(node.index()).expect("node count exceeds u32"));
            let mut results = Vec::new();
            enumerate_state_products(&choices, 0, &mut Vec::new(), &mut |states| {
                automaton.step(symbol, states, &mut |state| {
                    results.push(state);
                });
            });
            results.sort_unstable();
            results.dedup();
            results
        }
    }
}

fn enumerate_state_products(
    choices: &[Vec<StateId>],
    index: usize,
    current: &mut Vec<StateId>,
    out: &mut impl FnMut(&[StateId]),
) {
    if index == choices.len() {
        out(current);
        return;
    }
    for &choice in &choices[index] {
        current.push(choice);
        enumerate_state_products(choices, index + 1, current, out);
        current.pop();
    }
}

fn bottom_up_rules(
    automaton: &Explicit,
) -> Result<Vec<(Symbol, Vec<StateId>, StateId)>, FilterError> {
    fn visit(
        automaton: &Explicit,
        state: StateId,
        marks: &mut [u8],
        output: &mut Vec<(Symbol, Vec<StateId>, StateId)>,
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
        output.extend(
            automaton
                .rules_topdown(state)
                .map(|rule| (rule.symbol, rule.children.to_vec(), rule.result)),
        );
        Ok(())
    }

    let mut marks = vec![0; automaton.num_states() as usize];
    let mut output = Vec::new();
    for state in 0..automaton.num_states() {
        visit(automaton, StateId(state), &mut marks, &mut output)?;
    }
    Ok(output)
}

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
    let expansion = expand_chart(chart, cancelled)?;
    let ctt = build_ctt(chart.graph(), system, cancelled)?;
    let preimage = compute_preimage(chart.graph(), &expansion.automaton, &ctt, cancelled)?;
    let preimage = trim(&preimage).automaton;
    difference_on_fragments(chart, &preimage, cancelled)
}

#[cfg(test)]
mod tests {
    use super::*;

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
