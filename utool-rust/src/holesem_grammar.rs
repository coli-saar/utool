use crate::codec::{CodecError, CodecResult};
use crate::graph::{GraphBuilder, NodeId, ParsedGraph};
use crate::holesem_grammar_trait::{
    Atom, HolesemGrammarTrait, Input, LogicalConstant, Term, Value, Variable,
};
use std::collections::HashSet;

#[derive(Default)]
pub(crate) struct HolesemGrammar<'t> {
    input: Option<Input<'t>>,
}

impl<'t> HolesemGrammarTrait<'t> for HolesemGrammar<'t> {
    fn input(&mut self, input: &Input<'t>) -> parol_runtime::Result<()> {
        self.input = Some(input.clone());
        Ok(())
    }
}

pub(crate) fn parse(input: &str) -> CodecResult {
    let mut actions = HolesemGrammar::default();
    crate::holesem_parser::parse(input, "<holesem>", &mut actions)
        .map_err(|error| CodecError::Syntax(error.to_string()))?;
    lower(actions.input.expect("Parol must invoke the start action"))
}

struct Lowering {
    builder: GraphBuilder,
    declared: HashSet<String>,
    pending_dominance: Vec<(NodeId, NodeId)>,
    next_anonymous: usize,
}

impl Lowering {
    fn new() -> Self {
        Self {
            builder: ParsedGraph::builder(),
            declared: HashSet::new(),
            pending_dominance: Vec::new(),
            next_anonymous: 1,
        }
    }

    fn lower_term(&mut self, term: &Term<'_>) -> Result<(), CodecError> {
        match term {
            Term::AndLParenTermCommaTermRParen(value) => {
                self.lower_term(&value.term)?;
                self.lower_term(&value.term0)?;
            }
            Term::SomeLParenVariableCommaTermRParen(value) => {
                self.lower_term(&value.term)?;
            }
            Term::HoleLParenVariableRParen(value) => {
                let name = variable_text(&value.variable);
                self.declared.insert(name.clone());
                self.builder.ensure_node(name);
            }
            Term::LabelLParenVariableRParen(value) => {
                let name = variable_text(&value.variable);
                self.declared.insert(name.clone());
                self.builder.ensure_node(name);
            }
            Term::LeqLParenVariableCommaVariableRParen(value) => {
                let lower = self.variable_value(&value.variable)?;
                let upper = self.variable_value(&value.variable0)?;
                self.pending_dominance.push((upper, lower));
            }
            Term::Pred1LParenVariableCommaAtomCommaValueRParen(value) => {
                let node = self.variable_value(&value.variable)?;
                self.builder.set_label(node, atom_text(&value.atom))?;
                let child = self.value(&value.value)?;
                self.builder.add_tree_edge(node, child);
            }
            Term::Pred2LParenVariableCommaAtomCommaValueCommaValueRParen(value) => {
                let node = self.variable_value(&value.variable)?;
                self.builder.set_label(node, atom_text(&value.atom))?;
                let first = self.value(&value.value)?;
                let second = self.value(&value.value0)?;
                self.builder.add_tree_edge(node, first);
                self.builder.add_tree_edge(node, second);
            }
            Term::LogicalConstantLParenVariableCommaValueTermListRParen(value) => {
                let node = self.variable_value(&value.variable)?;
                self.builder
                    .set_label(node, logical_constant_text(&value.logical_constant))?;
                let first = self.value(&value.value)?;
                self.builder.add_tree_edge(node, first);
                for argument in &value.term_list {
                    let child = self.value(&argument.value)?;
                    self.builder.add_tree_edge(node, child);
                }
            }
        }
        Ok(())
    }

    fn variable_value(&mut self, variable: &Variable<'_>) -> Result<NodeId, CodecError> {
        let name = variable_text(variable);
        if self.declared.contains(&name) {
            Ok(self.builder.ensure_node(name))
        } else {
            self.anonymous(name)
        }
    }

    fn value(&mut self, value: &Value<'_>) -> Result<NodeId, CodecError> {
        match value {
            Value::Variable(variable) => self.variable_value(&variable.variable),
            Value::Atom(atom) => self.anonymous(atom_text(&atom.atom)),
        }
    }

    fn anonymous(&mut self, label: String) -> Result<NodeId, CodecError> {
        let name = format!("hs{}", self.next_anonymous);
        self.next_anonymous += 1;
        let node = self.builder.ensure_node(name);
        self.builder.set_label(node, label)?;
        Ok(node)
    }

    fn finish(mut self) -> CodecResult {
        for (source, target) in self.pending_dominance {
            self.builder.add_dominance_edge(source, target);
        }
        let mut graph = self.builder.finish();
        graph.normalize_dominance_targets()?;

        let parents = graph.tree_parents()?;
        let empty: Vec<_> = graph
            .nodes()
            .iter()
            .enumerate()
            .filter_map(|(index, node)| {
                let id = NodeId::from_index(index);
                (node.is_hole() && parents[index].is_none() && node.tree_children().is_empty())
                    .then_some(id)
            })
            .collect();
        if empty.len() > 1 {
            return Err(CodecError::Semantic(
                "multiple empty top fragments".to_owned(),
            ));
        }
        if let Some(&empty) = empty.first() {
            if graph
                .dominance_edges()
                .iter()
                .any(|(_, target)| *target == empty)
            {
                return Err(CodecError::Semantic("nontrivial empty fragment".to_owned()));
            }
            graph = graph.remove_node(empty);
        }
        Ok(graph)
    }
}

fn lower(input: Input<'_>) -> CodecResult {
    let mut lowering = Lowering::new();
    lowering.lower_term(&input.term)?;
    lowering.finish()
}

fn variable_text(variable: &Variable<'_>) -> String {
    variable.variable.text().to_owned()
}

fn atom_text(atom: &Atom<'_>) -> String {
    let raw = match atom {
        Atom::TickLParenDotOrLBracketCircumflexTickRBracketRParenStarTick(value) => value
            .tick_l_paren_dot_or_l_bracket_circumflex_tick_r_bracket_r_paren_star_tick
            .text(),
        Atom::LBracketAMinusZRBracketLBracketAMinusZAMinusZ0Minus9UnderscoreTickAmpRBracketStar(value) => value
            .l_bracket_a_minus_z_r_bracket_l_bracket_a_minus_z_a_minus_z0_minus9_underscore_tick_amp_r_bracket_star
            .text(),
    };
    if raw.starts_with('\'') && raw.ends_with('\'') {
        raw[1..raw.len() - 1].to_owned()
    } else {
        raw.to_owned()
    }
}

fn logical_constant_text(value: &LogicalConstant<'_>) -> &'static str {
    match value {
        LogicalConstant::And(_) => "and",
        LogicalConstant::Or(_) => "or",
        LogicalConstant::Imp(_) => "imp",
        LogicalConstant::Not(_) => "not",
        LogicalConstant::All(_) => "all",
        LogicalConstant::Some(_) => "some",
        LogicalConstant::Que(_) => "que",
        LogicalConstant::Eq(_) => "eq",
    }
}
