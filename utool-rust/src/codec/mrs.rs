use super::{CodecError, CodecResult};
use crate::graph::{GraphBuilder, NodeId};
use quick_xml::events::{BytesStart, Event};
use quick_xml::{Reader, XmlVersion};
use std::collections::{BTreeMap, BTreeSet, HashMap, HashSet, VecDeque};

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
enum ValueKind {
    Handle,
    Variable,
    InstanceOrHandle,
    Other,
}

/// Policy for a `p` variable that structural MRS constraints cannot refine.
#[derive(Clone, Copy, Debug, Default, PartialEq, Eq)]
pub enum PVariablePolicy {
    /// Treat unresolved `p` values as unexpressed instance arguments.
    #[default]
    ErgCompatible,
    /// Reject unresolved `p` values instead of applying the ERG convention.
    Strict,
}

#[derive(Clone, Debug)]
struct Value {
    text: String,
    kind: ValueKind,
}

#[derive(Clone, Debug)]
struct Relation {
    label: String,
    handle: String,
    attrs: Vec<(String, Value)>,
}

#[derive(Clone, Debug)]
struct Mrs {
    top: String,
    relations: Vec<Relation>,
    qeqs: Vec<(String, String)>,
}

#[derive(Clone, Debug, PartialEq, Eq)]
enum Token {
    Word(String),
    Quoted(String),
    Punct(char),
}

struct Parser {
    tokens: Vec<Token>,
    positions: Vec<usize>,
    offset: usize,
    input: String,
}

impl Parser {
    fn new(input: &str) -> Result<Self, CodecError> {
        let mut tokens = Vec::new();
        let mut positions = Vec::new();
        let mut chars = input.char_indices().peekable();
        while let Some((start, ch)) = chars.next() {
            if ch.is_whitespace() {
                continue;
            }
            if ch == '%' {
                while let Some((_, next)) = chars.peek() {
                    if matches!(next, '\n' | '\r') {
                        break;
                    }
                    chars.next();
                }
                continue;
            }
            if matches!(ch, '(' | ')' | '[' | ']' | ',') {
                tokens.push(Token::Punct(ch));
                positions.push(start);
                continue;
            }
            if ch == '\'' {
                let mut value = String::new();
                let mut closed = false;
                for (_, next) in chars.by_ref() {
                    if next == '\'' {
                        closed = true;
                        break;
                    }
                    value.push(next);
                }
                if !closed {
                    return Err(CodecError::Syntax(source_diagnostic(
                        input,
                        start,
                        "unterminated quoted symbol; add a closing apostrophe",
                    )));
                }
                tokens.push(Token::Quoted(value));
                positions.push(start);
                continue;
            }
            let mut value = String::from(ch);
            while let Some((_, next)) = chars.peek() {
                if next.is_whitespace() || matches!(next, '(' | ')' | '[' | ']' | ',' | '%' | '\'')
                {
                    break;
                }
                value.push(*next);
                chars.next();
            }
            tokens.push(Token::Word(value));
            positions.push(start);
        }
        Ok(Self {
            tokens,
            positions,
            offset: 0,
            input: input.to_owned(),
        })
    }

    fn parse(mut self) -> Result<Mrs, CodecError> {
        self.keyword("psoa")?;
        self.punct('(')?;
        let top = self.handle()?;
        self.punct(',')?;
        self.value()?;
        self.punct(',')?;
        self.punct('[')?;
        let mut relations = Vec::new();
        if !self.peek_punct(']') {
            loop {
                relations.push(self.relation()?);
                if !self.consume_punct(',') {
                    break;
                }
            }
        }
        self.punct(']')?;
        self.punct(',')?;
        self.keyword("hcons")?;
        self.punct('(')?;
        self.punct('[')?;
        let mut qeqs = Vec::new();
        if !self.peek_punct(']') {
            loop {
                match self.word()?.as_str() {
                    "qeq" | "geq" => {}
                    found => return self.error(format!("expected qeq or geq, found {found:?}")),
                }
                self.punct('(')?;
                let high = self.handle()?;
                self.punct(',')?;
                let low = self.handle()?;
                self.punct(')')?;
                qeqs.push((high, low));
                if !self.consume_punct(',') {
                    break;
                }
            }
        }
        self.punct(']')?;
        self.punct(')')?;
        self.punct(')')?;
        if self.offset != self.tokens.len() {
            return self.error("unexpected text after MRS");
        }
        Ok(Mrs {
            top,
            relations,
            qeqs,
        })
    }

    fn relation(&mut self) -> Result<Relation, CodecError> {
        self.keyword("rel")?;
        self.punct('(')?;
        let label = self.symbol()?;
        self.punct(',')?;
        let handle = self.handle()?;
        self.punct(',')?;
        self.punct('[')?;
        let mut attrs = Vec::new();
        if !self.peek_punct(']') {
            loop {
                self.keyword("attrval")?;
                self.punct('(')?;
                let name = self.symbol()?;
                self.punct(',')?;
                let value = self.value()?;
                self.punct(')')?;
                attrs.push((name, value));
                if !self.consume_punct(',') {
                    break;
                }
            }
        }
        self.punct(']')?;
        self.punct(')')?;
        Ok(Relation {
            label,
            handle,
            attrs,
        })
    }

    fn value(&mut self) -> Result<Value, CodecError> {
        match self.tokens.get(self.offset).cloned() {
            Some(Token::Quoted(text)) => {
                self.offset += 1;
                Ok(Value {
                    text,
                    kind: ValueKind::Other,
                })
            }
            Some(Token::Word(text)) => {
                self.offset += 1;
                let kind = value_kind(&text);
                Ok(Value { text, kind })
            }
            found => self.error(format!("expected MRS value, found {found:?}")),
        }
    }

    fn handle(&mut self) -> Result<String, CodecError> {
        let value = self.word()?;
        if numbered(&value, 'h') || numbered(&value, 'p') {
            Ok(value)
        } else {
            self.error(format!("expected handle, found {value:?}"))
        }
    }

    fn symbol(&mut self) -> Result<String, CodecError> {
        match self.tokens.get(self.offset).cloned() {
            Some(Token::Quoted(value)) => {
                self.offset += 1;
                Ok(value)
            }
            Some(Token::Word(value)) if value == "*TOP*" => {
                self.offset += 1;
                Ok(value)
            }
            found => self.error(format!("expected quoted symbol, found {found:?}")),
        }
    }

    fn keyword(&mut self, expected: &str) -> Result<(), CodecError> {
        let found = self.word()?;
        if found == expected {
            Ok(())
        } else {
            self.error(format!("expected {expected:?}, found {found:?}"))
        }
    }

    fn word(&mut self) -> Result<String, CodecError> {
        match self.tokens.get(self.offset).cloned() {
            Some(Token::Word(value)) => {
                self.offset += 1;
                Ok(value)
            }
            found => self.error(format!("expected identifier, found {found:?}")),
        }
    }

    fn punct(&mut self, expected: char) -> Result<(), CodecError> {
        if self.consume_punct(expected) {
            Ok(())
        } else {
            self.error(format!(
                "expected punctuation {expected:?}, found {:?}",
                self.tokens.get(self.offset)
            ))
        }
    }

    fn consume_punct(&mut self, expected: char) -> bool {
        if self.peek_punct(expected) {
            self.offset += 1;
            true
        } else {
            false
        }
    }

    fn peek_punct(&self, expected: char) -> bool {
        self.tokens.get(self.offset) == Some(&Token::Punct(expected))
    }

    fn error<T>(&self, message: impl Into<String>) -> Result<T, CodecError> {
        let position = self
            .positions
            .get(self.offset)
            .copied()
            .unwrap_or(self.input.len());
        Err(CodecError::Syntax(source_diagnostic(
            &self.input,
            position,
            &format!("{} (token {})", message.into(), self.offset + 1),
        )))
    }
}

fn source_diagnostic(input: &str, byte: usize, message: &str) -> String {
    let prefix = &input[..byte.min(input.len())];
    let line = prefix.bytes().filter(|byte| *byte == b'\n').count() + 1;
    let line_start = prefix.rfind('\n').map_or(0, |offset| offset + 1);
    let column = input[line_start..byte.min(input.len())].chars().count() + 1;
    let source_line = input[line_start..]
        .split_once(['\r', '\n'])
        .map_or(&input[line_start..], |(text, _)| text);
    let caret_padding = " ".repeat(column.saturating_sub(1));
    format!("{message} at line {line}, column {column}\n\n  {source_line}\n  {caret_padding}^")
}

fn numbered(value: &str, prefix: char) -> bool {
    value
        .strip_prefix(prefix)
        .is_some_and(|tail| !tail.is_empty() && tail.chars().all(|ch| ch.is_ascii_digit()))
}

fn value_kind(value: &str) -> ValueKind {
    if numbered(value, 'h') {
        ValueKind::Handle
    } else if numbered(value, 'x') {
        ValueKind::Variable
    } else if numbered(value, 'p') {
        ValueKind::InstanceOrHandle
    } else {
        ValueKind::Other
    }
}

#[derive(Default)]
struct RawGraph {
    names: Vec<String>,
    ids: HashMap<String, usize>,
    labels: Vec<Option<String>>,
    children: Vec<Vec<usize>>,
    dominance: Vec<(usize, usize)>,
}

impl RawGraph {
    fn node(&mut self, name: &str) -> usize {
        if let Some(&id) = self.ids.get(name) {
            return id;
        }
        let id = self.names.len();
        self.names.push(name.to_owned());
        self.ids.insert(name.to_owned(), id);
        self.labels.push(None);
        self.children.push(Vec::new());
        id
    }

    fn tree(&mut self, source: &str, target: &str) {
        let source = self.node(source);
        let target = self.node(target);
        self.children[source].push(target);
    }

    fn dom(&mut self, source: &str, target: &str) {
        let source = self.node(source);
        let target = self.node(target);
        self.dominance.push((source, target));
    }

    fn parents(&self) -> Result<Vec<Option<usize>>, CodecError> {
        let mut parents = vec![None; self.names.len()];
        for (source, children) in self.children.iter().enumerate() {
            for &target in children {
                if parents[target].replace(source).is_some() {
                    return Err(CodecError::Semantic(format!(
                        "node {:?} has multiple tree parents",
                        self.names[target]
                    )));
                }
            }
        }
        Ok(parents)
    }

    fn reachable(&self, source: usize, target: usize) -> bool {
        let mut seen = vec![false; self.names.len()];
        let mut todo = vec![source];
        while let Some(node) = todo.pop() {
            if node == target {
                return true;
            }
            if std::mem::replace(&mut seen[node], true) {
                continue;
            }
            todo.extend(self.children[node].iter().copied());
            todo.extend(
                self.dominance
                    .iter()
                    .filter_map(|&(from, to)| (from == node).then_some(to)),
            );
        }
        false
    }

    fn hypernormally_reachable_avoiding(
        &self,
        source: usize,
        target: usize,
        avoid: &HashSet<usize>,
    ) -> bool {
        fn visit(
            graph: &RawGraph,
            node: usize,
            target: usize,
            visited: &mut HashSet<usize>,
            previous_was_upward_dominance: bool,
        ) -> bool {
            if !visited.insert(node) {
                return false;
            }
            if node == target {
                return true;
            }

            for &child in &graph.children[node] {
                if visit(graph, child, target, visited, false) {
                    return true;
                }
            }
            for (parent, children) in graph.children.iter().enumerate() {
                if children.contains(&node) && visit(graph, parent, target, visited, false) {
                    return true;
                }
            }
            for &(from, to) in &graph.dominance {
                if from == node
                    && !previous_was_upward_dominance
                    && visit(graph, to, target, visited, false)
                {
                    return true;
                }
                if to == node && visit(graph, from, target, visited, true) {
                    return true;
                }
            }
            false
        }

        let mut visited = avoid.clone();
        visit(self, source, target, &mut visited, false)
    }

    fn remove(&mut self, removed: usize) {
        self.names.remove(removed);
        self.labels.remove(removed);
        self.children.remove(removed);
        for children in &mut self.children {
            children.retain(|child| *child != removed);
            for child in children {
                *child -= usize::from(*child > removed);
            }
        }
        self.dominance
            .retain(|&(a, b)| a != removed && b != removed);
        for (a, b) in &mut self.dominance {
            *a -= usize::from(*a > removed);
            *b -= usize::from(*b > removed);
        }
        self.ids = self
            .names
            .iter()
            .enumerate()
            .map(|(i, n)| (n.clone(), i))
            .collect();
    }

    fn finish(self) -> CodecResult {
        let mut builder = GraphBuilder::default();
        let ids: Vec<NodeId> = self
            .names
            .iter()
            .map(|name| builder.ensure_node(name))
            .collect();
        for (index, label) in self.labels.into_iter().enumerate() {
            if let Some(label) = label {
                builder.set_label(ids[index], label)?;
            }
        }
        for (source, children) in self.children.into_iter().enumerate() {
            for target in children {
                builder.add_tree_edge(ids[source], ids[target]);
            }
        }
        for (source, target) in self.dominance {
            builder.add_dominance_edge(ids[source], ids[target]);
        }
        Ok(builder.finish())
    }
}

fn constrain_p(
    refinements: &mut HashMap<String, ValueKind>,
    value: &str,
    kind: ValueKind,
) -> Result<(), CodecError> {
    if !numbered(value, 'p') {
        return Ok(());
    }
    if let Some(previous) = refinements.insert(value.to_owned(), kind) {
        if previous != kind {
            return Err(CodecError::Semantic(format!(
                "p variable {value:?} occurs in both handle and instance positions"
            )));
        }
    }
    Ok(())
}

fn p_refinements(mrs: &Mrs) -> Result<HashMap<String, ValueKind>, CodecError> {
    let mut refinements = HashMap::new();
    constrain_p(&mut refinements, &mrs.top, ValueKind::Handle)?;
    for relation in &mrs.relations {
        constrain_p(&mut refinements, &relation.handle, ValueKind::Handle)?;
        for (name, value) in &relation.attrs {
            if name == "ARG0" {
                constrain_p(&mut refinements, &value.text, ValueKind::Variable)?;
            } else if matches!(name.as_str(), "RSTR" | "BODY") {
                constrain_p(&mut refinements, &value.text, ValueKind::Handle)?;
            }
        }
    }
    for (high, low) in &mrs.qeqs {
        constrain_p(&mut refinements, high, ValueKind::Handle)?;
        constrain_p(&mut refinements, low, ValueKind::Handle)?;
    }
    Ok(refinements)
}

fn lower(mrs: Mrs, p_policy: PVariablePolicy) -> CodecResult {
    let p_refinements = p_refinements(&mrs)?;
    let mut graph = RawGraph::default();
    let mut binders = HashMap::<String, usize>::new();
    let mut bound = BTreeMap::<String, BTreeSet<String>>::new();

    for relation in mrs.relations {
        let node = graph.node(&relation.handle);
        graph.labels[node] = Some(match graph.labels[node].take() {
            Some(previous) => format!("{previous}&{}", relation.label),
            None => relation.label,
        });
        let attrs: BTreeMap<_, _> = relation.attrs.into_iter().collect();
        if let (Some(rstr), Some(body)) = (attrs.get("RSTR"), attrs.get("BODY")) {
            graph.tree(&relation.handle, &rstr.text);
            graph.tree(&relation.handle, &body.text);
            let variable = attrs
                .get("ARG0")
                .ok_or_else(|| CodecError::Semantic("MRS quantifier has no ARG0".to_owned()))?;
            if binders.insert(variable.text.clone(), node).is_some() {
                return Err(CodecError::Semantic(format!(
                    "variable {:?} is used by distinct quantifiers",
                    variable.text
                )));
            }
            if attrs
                .keys()
                .any(|name| !matches!(name.as_str(), "RSTR" | "BODY" | "ARG0"))
            {
                return Err(CodecError::Semantic("illegal quantifier syntax".to_owned()));
            }
        } else {
            for (name, value) in attrs {
                if matches!(name.as_str(), "TPC" | "PSV") {
                    continue;
                }
                match value.kind {
                    ValueKind::Handle => graph.tree(&relation.handle, &value.text),
                    ValueKind::Variable => {
                        bound
                            .entry(value.text)
                            .or_default()
                            .insert(relation.handle.clone());
                    }
                    ValueKind::InstanceOrHandle => match p_refinements.get(&value.text) {
                        Some(ValueKind::Handle) => graph.tree(&relation.handle, &value.text),
                        Some(ValueKind::Variable) => {
                            bound
                                .entry(value.text)
                                .or_default()
                                .insert(relation.handle.clone());
                        }
                        Some(ValueKind::InstanceOrHandle | ValueKind::Other) => unreachable!(),
                        None if p_policy == PVariablePolicy::ErgCompatible => {}
                        None => {
                            return Err(CodecError::Semantic(format!(
                                "unresolved p variable {:?}; use the ErgCompatible policy or provide type information",
                                value.text
                            )));
                        }
                    },
                    ValueKind::Other => {}
                }
            }
        }
    }
    for (high, low) in mrs.qeqs {
        graph.dom(&high, &low);
    }

    for (variable, occurrences) in bound {
        let binder = *binders
            .get(&variable)
            .ok_or_else(|| CodecError::Semantic(format!("free variable {variable:?}")))?;
        let parents = graph.parents()?;
        for occurrence_name in occurrences {
            let occurrence = graph.ids[&occurrence_name];
            if graph.reachable(binder, occurrence) {
                continue;
            }
            let mut root = occurrence;
            while let Some(parent) = parents[root] {
                root = parent;
            }
            graph.dominance.push((binder, root));
        }
    }

    set_top(&mut graph, &mrs.top)?;
    remove_empty_top(&mut graph)?;
    normalize(&mut graph)?;
    validate_lowered_graph(graph.finish()?)
}

fn validate_lowered_graph(graph: crate::graph::ParsedGraph) -> CodecResult {
    if !graph.is_weakly_normal() {
        return Err(CodecError::Semantic(
            "converted MRS graph is not weakly normal".to_owned(),
        ));
    }
    let mut failures = Vec::new();
    if !graph.is_normal() {
        failures.push("normal");
    }
    if !graph.is_leaf_labelled() {
        failures.push("leaf-labelled");
    }
    if !graph.is_hypernormally_connected() {
        failures.push("hypernormally connected");
    }
    if failures.is_empty() {
        Ok(graph)
    } else {
        Err(CodecError::Semantic(format!(
            "converted MRS graph is not {}",
            failures.join(", not ")
        )))
    }
}

#[allow(clippy::too_many_lines)]
fn set_top(graph: &mut RawGraph, top_name: &str) -> Result<(), CodecError> {
    let Some(&top) = graph.ids.get(top_name) else {
        return Ok(());
    };
    let parents = graph.parents()?;
    let mut fragment = HashSet::new();
    let mut todo = vec![top];
    while let Some(node) = todo.pop() {
        if fragment.insert(node) {
            todo.extend(graph.children[node].iter().copied());
        }
    }
    let holes: Vec<_> = fragment
        .iter()
        .copied()
        .filter(|&n| graph.labels[n].is_none())
        .collect();
    let mut dom_in = vec![0; graph.names.len()];
    for &(_, target) in &graph.dominance {
        dom_in[target] += 1;
    }
    let roots: Vec<_> = parents
        .iter()
        .enumerate()
        .filter_map(|(n, p)| p.is_none().then_some(n))
        .collect();
    if holes.len() == 1 {
        let hole = holes[0];
        for root in roots {
            if root != top && dom_in[root] == 0 {
                graph.dominance.push((hole, root));
            }
        }
    } else {
        let outside: HashSet<_> = (0..graph.names.len())
            .filter(|node| !fragment.contains(node))
            .collect();
        let mut adjacency = vec![Vec::new(); graph.names.len()];
        for (source, children) in graph.children.iter().enumerate() {
            for &target in children {
                if outside.contains(&source) && outside.contains(&target) {
                    adjacency[source].push(target);
                    adjacency[target].push(source);
                }
            }
        }
        for &(source, target) in &graph.dominance {
            if outside.contains(&source) && outside.contains(&target) {
                adjacency[source].push(target);
                adjacency[target].push(source);
            }
        }
        let mut seen = HashSet::new();
        let mut components = Vec::new();
        for &start in &outside {
            if !seen.insert(start) {
                continue;
            }
            let mut component = Vec::new();
            let mut queue = VecDeque::from([start]);
            while let Some(node) = queue.pop_front() {
                component.push(node);
                for &next in &adjacency[node] {
                    if seen.insert(next) {
                        queue.push_back(next);
                    }
                }
            }
            components.push(component);
        }
        for component in components {
            let members: HashSet<_> = component.iter().copied().collect();
            let component_roots: Vec<_> = component
                .iter()
                .copied()
                .filter(|&node| parents[node].is_none_or(|parent| !members.contains(&parent)))
                .collect();
            for node in component {
                let sources: Vec<_> = graph
                    .dominance
                    .iter()
                    .filter_map(|&(source, target)| {
                        (target == node && holes.contains(&source)).then_some(source)
                    })
                    .collect();
                for source in sources {
                    for &root in &component_roots {
                        if !graph.dominance.iter().any(|&(_, target)| target == root) {
                            graph.dominance.push((source, root));
                        }
                    }
                }
            }
        }
        for node in outside {
            let tree_in = parents[node].is_some();
            let dom_in = graph.dominance.iter().any(|&(_, target)| target == node);
            if !tree_in && !dom_in {
                graph.dominance.push((top, node));
            }
        }
    }
    Ok(())
}

fn remove_empty_top(graph: &mut RawGraph) -> Result<(), CodecError> {
    let parents = graph.parents()?;
    let mut empty = None;
    for (node, parent) in parents.iter().enumerate() {
        if graph.labels[node].is_none() && graph.children[node].is_empty() && parent.is_none() {
            let indegree = graph
                .dominance
                .iter()
                .filter(|(_, target)| *target == node)
                .count();
            if indegree > 0 {
                return Err(CodecError::Semantic(format!(
                    "nontrivial empty fragment at {:?}",
                    graph.names[node]
                )));
            }
            if empty.replace(node).is_some() {
                return Err(CodecError::Semantic(
                    "multiple empty top fragments".to_owned(),
                ));
            }
        }
    }
    if let Some(node) = empty {
        graph.remove(node);
    }
    Ok(())
}

fn normalize(graph: &mut RawGraph) -> Result<(), CodecError> {
    let parents = graph.parents()?;
    let roots: Vec<_> = parents
        .iter()
        .enumerate()
        .filter_map(|(n, p)| p.is_none().then_some(n))
        .collect();
    for root in roots {
        let outgoing: Vec<_> = graph
            .dominance
            .iter()
            .copied()
            .filter(|(source, _)| *source == root)
            .collect();
        if outgoing.is_empty() {
            continue;
        }
        for first in 0..outgoing.len() {
            for second in (first + 1)..outgoing.len() {
                let avoid = HashSet::from([root]);
                if !graph.hypernormally_reachable_avoiding(
                    outgoing[first].1,
                    outgoing[second].1,
                    &avoid,
                ) {
                    return Err(CodecError::Semantic(format!(
                        "dominance children {:?} and {:?} of root {:?} are not hypernormally connected",
                        graph.names[outgoing[first].1],
                        graph.names[outgoing[second].1],
                        graph.names[root]
                    )));
                }
            }
        }
        let mut fragment = HashSet::new();
        let mut todo = VecDeque::from([root]);
        while let Some(node) = todo.pop_front() {
            if fragment.insert(node) {
                todo.extend(graph.children[node].iter().copied());
            }
        }
        let open: Vec<_> = fragment
            .iter()
            .copied()
            .filter(|&node| {
                graph.labels[node].is_none()
                    && !graph.dominance.iter().any(|(source, _)| *source == node)
            })
            .collect();
        if open.len() == 1 {
            let hole = open[0];
            graph.dominance.retain(|(source, _)| *source != root);
            graph
                .dominance
                .extend(outgoing.into_iter().map(|(_, target)| (hole, target)));
        }
    }
    let mut unique = HashSet::new();
    graph.dominance.retain(|edge| unique.insert(*edge));
    Ok(())
}

/// Parse DELPH-IN/LKB Prolog-style MRS and apply Java Utool's default
/// `normalisation=nets,labelStyle=plain` conversion.
///
/// # Errors
///
/// Returns an error if the Prolog term is malformed or the converted graph is
/// not a valid normalized MRS dominance graph.
pub fn parse_mrs_prolog(input: &str) -> CodecResult {
    parse_mrs_prolog_with_policy(input, PVariablePolicy::default())
}

/// Parse Prolog-style MRS with an explicit unresolved-`p` policy.
///
/// # Errors
///
/// Returns an error if the term is malformed, a `p` value violates the
/// selected policy, or conversion does not produce a valid normalized graph.
pub fn parse_mrs_prolog_with_policy(input: &str, p_policy: PVariablePolicy) -> CodecResult {
    lower(Parser::new(input)?.parse()?, p_policy)
}

fn xml_attribute(element: &BytesStart<'_>, name: &[u8]) -> Result<Option<String>, CodecError> {
    for attribute in element.attributes() {
        let attribute = attribute.map_err(|error| CodecError::Syntax(error.to_string()))?;
        if attribute.key.as_ref() == name {
            return Ok(Some(
                attribute
                    .normalized_value(XmlVersion::Implicit1_0)
                    .map_err(|error| CodecError::Syntax(error.to_string()))?
                    .into_owned(),
            ));
        }
    }
    Ok(None)
}

/// Parse the XML MRS representation accepted by Java Utool and apply the same
/// default MRS-to-dominance-graph conversion as [`parse_mrs_prolog`].
///
/// # Errors
///
/// Returns an error if the XML is malformed or the converted graph is not a
/// valid normalized MRS dominance graph.
#[allow(clippy::too_many_lines)]
pub fn parse_mrs_xml(input: &str) -> CodecResult {
    parse_mrs_xml_with_policy(input, PVariablePolicy::default())
}

/// Parse XML MRS with an explicit unresolved-`p` policy.
///
/// # Errors
///
/// Returns an error if the XML is malformed, a `p` value violates the
/// selected policy, or conversion does not produce a valid normalized graph.
#[allow(clippy::too_many_lines)]
pub fn parse_mrs_xml_with_policy(input: &str, p_policy: PVariablePolicy) -> CodecResult {
    let mut reader = Reader::from_str(input);
    reader.config_mut().trim_text(false);
    let mut stack = Vec::<String>::new();
    let mut text = String::new();
    let mut top = String::new();
    let mut handle = String::new();
    let mut label = String::new();
    let mut attr = String::new();
    let mut value = Value {
        text: String::new(),
        kind: ValueKind::Other,
    };
    let mut high = String::new();
    let mut low = String::new();
    let mut attrs = Vec::<(String, Value)>::new();
    let mut relations = Vec::<Relation>::new();
    let mut qeqs = Vec::<(String, String)>::new();

    loop {
        match reader.read_event() {
            Ok(Event::Start(element)) => {
                let name = String::from_utf8_lossy(element.local_name().as_ref()).into_owned();
                if name == "ep" {
                    attrs.clear();
                } else if name == "var" {
                    let variable = xml_attribute(&element, b"vid")?
                        .ok_or_else(|| CodecError::Syntax("MRS XML var has no vid".to_owned()))?;
                    let parsed = Value {
                        kind: value_kind(&variable),
                        text: variable,
                    };
                    match stack.last().map(String::as_str) {
                        Some("mrs") => top = parsed.text,
                        Some("ep") => handle = parsed.text,
                        Some("fvpair") => value = parsed,
                        Some("hi") => high = parsed.text,
                        Some("lo") => low = parsed.text,
                        _ => {}
                    }
                }
                text.clear();
                stack.push(name);
            }
            Ok(Event::Empty(element)) => {
                let name = String::from_utf8_lossy(element.local_name().as_ref()).into_owned();
                if name == "var" {
                    let variable = xml_attribute(&element, b"vid")?
                        .ok_or_else(|| CodecError::Syntax("MRS XML var has no vid".to_owned()))?;
                    let parsed = Value {
                        kind: value_kind(&variable),
                        text: variable,
                    };
                    match stack.last().map(String::as_str) {
                        Some("mrs") => top = parsed.text,
                        Some("ep") => handle = parsed.text,
                        Some("fvpair") => value = parsed,
                        Some("hi") => high = parsed.text,
                        Some("lo") => low = parsed.text,
                        _ => {}
                    }
                }
            }
            Ok(Event::Text(chars)) => {
                text.push_str(
                    &chars
                        .decode()
                        .map_err(|error| CodecError::Syntax(error.to_string()))?,
                );
            }
            Ok(Event::GeneralRef(reference)) => {
                super::gxl::append_reference(&reference, &mut text)?;
            }
            Ok(Event::End(element)) => {
                let name = String::from_utf8_lossy(element.local_name().as_ref()).into_owned();
                match name.as_str() {
                    "ep" => relations.push(Relation {
                        label: label.clone(),
                        handle: handle.clone(),
                        attrs: attrs.clone(),
                    }),
                    "hcons" => qeqs.push((high.clone(), low.clone())),
                    "fvpair" => attrs.push((attr.clone(), value.clone())),
                    "pred" => label.clone_from(&text),
                    "rargname" => attr.clone_from(&text),
                    "constant" => {
                        value = Value {
                            text: text.clone(),
                            kind: ValueKind::Other,
                        }
                    }
                    _ => {}
                }
                stack.pop();
                text.clear();
            }
            Ok(Event::Eof) => break,
            Ok(_) => {}
            Err(error) => {
                return Err(CodecError::Syntax(super::format_source_error(
                    input,
                    usize::try_from(reader.error_position()).unwrap_or(usize::MAX),
                    &format!("invalid MRS XML: {error}"),
                )));
            }
        }
    }
    if top.is_empty() {
        return Err(CodecError::Syntax("MRS XML has no top handle".to_owned()));
    }
    lower(
        Mrs {
            top,
            relations,
            qeqs,
        },
        p_policy,
    )
}
