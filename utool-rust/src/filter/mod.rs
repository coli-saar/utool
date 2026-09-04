//! Relative-normal-form filtering for finite HNC charts.

use crate::{Chart, Solution, SolveError};
use std::collections::{HashMap, HashSet};
use thiserror::Error;

/// A first-order rewrite-system term.
#[derive(Clone, Debug, PartialEq, Eq, Hash)]
pub enum Pattern {
    /// A variable, conventionally beginning with an uppercase letter.
    Variable(String),
    /// A labelled term with ordered children.
    Node(String, Vec<Pattern>),
    /// A context wildcard. Parsed for compatibility but not yet executable.
    Wildcard(Box<Pattern>),
}

/// One weakening or equivalence rule.
#[derive(Clone, Debug, PartialEq, Eq)]
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
    annotations: HashMap<(String, String), Vec<String>>,
}

/// Rewrite parsing or filtering failure.
#[derive(Debug, Error)]
pub enum FilterError {
    /// Invalid rewrite-system syntax.
    #[error("rewrite syntax on line {line}: {message}")]
    Syntax { line: usize, message: String },
    /// Context wildcards require the future CTT implementation.
    #[error("context wildcards are not supported by the finite-chart filter")]
    ContextWildcard,
    /// Chart construction was cancelled or failed.
    #[error(transparent)]
    Chart(#[from] SolveError),
}

impl RewriteSystem {
    /// Parse the Java Utool rewrite-file surface syntax.
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
                system.rules.push(RewriteRule {
                    annotation,
                    lhs: parse_pattern(lhs.trim(), line_number)?,
                    rhs: parse_pattern(rhs.trim(), line_number)?,
                    oriented: true,
                });
                continue;
            }
            if let Some((lhs, rhs)) = rule_text.split_once('=') {
                system.rules.push(RewriteRule {
                    annotation: None,
                    lhs: parse_pattern(lhs.trim(), line_number)?,
                    rhs: parse_pattern(rhs.trim(), line_number)?,
                    oriented: false,
                });
                continue;
            }
            if let Some((parent, term)) = line.split_once(':') {
                let Pattern::Node(label, children) = parse_pattern(term.trim(), line_number)?
                else {
                    return Err(syntax(
                        line_number,
                        "annotation rule must name a constructor",
                    ));
                };
                let child_annotations = children
                    .into_iter()
                    .map(|child| match child {
                        Pattern::Node(name, children) if children.is_empty() => Ok(name),
                        _ => Err(syntax(line_number, "invalid child annotation")),
                    })
                    .collect::<Result<Vec<_>, _>>()?;
                system
                    .annotations
                    .insert((parent.trim().to_owned(), label), child_annotations);
                continue;
            }
            return Err(syntax(line_number, "expected a rewrite or annotation rule"));
        }
        if system
            .rules
            .iter()
            .any(|rule| contains_wildcard(&rule.lhs) || contains_wildcard(&rule.rhs))
        {
            return Err(FilterError::ContextWildcard);
        }
        Ok(system)
    }
}

fn syntax(line: usize, message: &str) -> FilterError {
    FilterError::Syntax {
        line,
        message: message.to_owned(),
    }
}

fn contains_wildcard(pattern: &Pattern) -> bool {
    match pattern {
        Pattern::Wildcard(_) => true,
        Pattern::Node(_, children) => children.iter().any(contains_wildcard),
        Pattern::Variable(_) => false,
    }
}

struct PatternParser<'a> {
    input: &'a str,
    offset: usize,
    line: usize,
}

fn parse_pattern(input: &str, line: usize) -> Result<Pattern, FilterError> {
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
    fn term(&mut self) -> Result<Pattern, FilterError> {
        self.whitespace();
        if self.consume('*') {
            self.expect('[')?;
            let inner = self.term()?;
            self.expect(']')?;
            return Ok(Pattern::Wildcard(Box::new(inner)));
        }
        let name = self.identifier()?;
        if self.consume('#') {
            let _ = self.identifier()?;
        }
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
            return Ok(Pattern::Node(name, children));
        }
        if name.chars().next().is_some_and(char::is_uppercase) {
            Ok(Pattern::Variable(name))
        } else {
            Ok(Pattern::Node(name, Vec::new()))
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
struct GroundTerm {
    label: String,
    children: Vec<GroundTerm>,
}

fn solution_term(solution: &Solution) -> Option<GroundTerm> {
    fn convert(solution: &Solution, tree: packed_term_arena::tree::Tree) -> GroundTerm {
        GroundTerm {
            label: solution.node_label(tree).to_owned(),
            children: solution
                .arena()
                .get_children(tree)
                .iter()
                .map(|child| convert(solution, *child))
                .collect(),
        }
    }
    solution.root().map(|root| convert(solution, root))
}

/// Remove Solutions which rewrite to another Solution in the same chart.
pub fn filter_chart(
    chart: &Chart,
    system: &RewriteSystem,
    cancelled: impl Fn() -> bool + Copy,
) -> Result<Chart, FilterError> {
    let mut language = HashSet::new();
    let mut solutions = chart.solutions();
    while solutions.advance() {
        if let Some(term) =
            solution_term(&solutions.current().expect("advance produced a solution"))
        {
            language.insert(term);
        }
    }
    chart
        .select_solutions(
            |solution| {
                solution_term(solution).is_none_or(|term| {
                    !has_better_rewrite(
                        &term,
                        system.start_annotation.as_deref(),
                        system,
                        &language,
                    )
                })
            },
            cancelled,
        )
        .map_err(FilterError::from)
}

fn has_better_rewrite(
    term: &GroundTerm,
    annotation: Option<&str>,
    system: &RewriteSystem,
    language: &HashSet<GroundTerm>,
) -> bool {
    rewrites(term, annotation, system)
        .into_iter()
        .any(|(candidate, oriented)| {
            language.contains(&candidate) && (oriented || candidate < *term)
        })
}

fn rewrites(
    term: &GroundTerm,
    annotation: Option<&str>,
    system: &RewriteSystem,
) -> Vec<(GroundTerm, bool)> {
    let mut output = Vec::new();
    for rule in &system.rules {
        let annotation_matches = !rule.oriented || rule.annotation.as_deref() == annotation;
        if annotation_matches {
            if let Some(candidate) = apply_at_root(term, &rule.lhs, &rule.rhs) {
                output.push((candidate, rule.oriented));
            }
            if !rule.oriented {
                if let Some(candidate) = apply_at_root(term, &rule.rhs, &rule.lhs) {
                    output.push((candidate, false));
                }
            }
        }
    }
    let child_annotations = annotation.and_then(|parent| {
        system
            .annotations
            .get(&(parent.to_owned(), term.label.clone()))
    });
    for (index, child) in term.children.iter().enumerate() {
        let child_annotation = child_annotations
            .and_then(|items| items.get(index))
            .map(String::as_str)
            .or(system.neutral_annotation.as_deref());
        for (replacement, oriented) in rewrites(child, child_annotation, system) {
            let mut candidate = term.clone();
            candidate.children[index] = replacement;
            output.push((candidate, oriented));
        }
    }
    output
}

fn apply_at_root(term: &GroundTerm, lhs: &Pattern, rhs: &Pattern) -> Option<GroundTerm> {
    let mut bindings = HashMap::new();
    matches(lhs, term, &mut bindings)
        .then(|| instantiate(rhs, &bindings))
        .flatten()
}

fn matches(
    pattern: &Pattern,
    term: &GroundTerm,
    bindings: &mut HashMap<String, GroundTerm>,
) -> bool {
    match pattern {
        Pattern::Variable(name) => match bindings.get(name) {
            Some(bound) => bound == term,
            None => {
                bindings.insert(name.clone(), term.clone());
                true
            }
        },
        Pattern::Node(label, children) => {
            label == &term.label
                && children.len() == term.children.len()
                && children
                    .iter()
                    .zip(&term.children)
                    .all(|(pattern, child)| matches(pattern, child, bindings))
        }
        Pattern::Wildcard(_) => false,
    }
}

fn instantiate(pattern: &Pattern, bindings: &HashMap<String, GroundTerm>) -> Option<GroundTerm> {
    match pattern {
        Pattern::Variable(name) => bindings.get(name).cloned(),
        Pattern::Node(label, children) => Some(GroundTerm {
            label: label.clone(),
            children: children
                .iter()
                .map(|child| instantiate(child, bindings))
                .collect::<Option<Vec<_>>>()?,
        }),
        Pattern::Wildcard(_) => None,
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn parses_utool_rewrite_surface() {
        let system = RewriteSystem::parse(
            "start annotation: +\nneutral annotation: 0\n+: a(+,+)\n[+] a(X,every(Y,Z)) -> every(Y,a(X,Z))\na#1(X,a#2(Y,Z)) = a#2(Y,a#1(X,Z))",
        )
        .unwrap();
        assert_eq!(system.rules.len(), 2);
        assert!(system.rules[0].oriented);
        assert!(!system.rules[1].oriented);
    }
}
