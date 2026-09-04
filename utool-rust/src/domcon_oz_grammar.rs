use crate::codec::{CodecError, CodecResult};
use crate::domcon_oz_grammar_trait::{Atom, DomconOzGrammarTrait, Input, OzAtom};
use crate::graph::ParsedGraph;

#[derive(Default)]
pub(crate) struct DomconOzGrammar<'t> {
    input: Option<Input<'t>>,
}

impl<'t> DomconOzGrammarTrait<'t> for DomconOzGrammar<'t> {
    fn input(&mut self, input: &Input<'t>) -> parol_runtime::Result<()> {
        self.input = Some(input.clone());
        Ok(())
    }
}

pub(crate) fn parse(input: &str) -> CodecResult {
    let mut actions = DomconOzGrammar::default();
    crate::domcon_oz_parser::parse(input, "<domcon-oz>", &mut actions)
        .map_err(|error| CodecError::Syntax(error.to_string()))?;
    lower(actions.input.expect("Parol must invoke the start action"))
}

fn lower(input: Input<'_>) -> CodecResult {
    let mut builder = ParsedGraph::builder();
    for item in input.input_list {
        match *item.atom {
            Atom::DomLParenOzAtomOzAtomRParen(atom) => {
                let source = builder.ensure_node(atom_text(&atom.oz_atom));
                let target = builder.ensure_node(atom_text(&atom.oz_atom0));
                builder.add_dominance_edge(source, target);
            }
            Atom::LabelLParenOzAtomOzAtomAtomOptRParen(atom) => {
                let node = builder.ensure_node(atom_text(&atom.oz_atom));
                builder.set_label(node, atom_text(&atom.oz_atom0))?;
                if let Some(children) = atom.atom_opt {
                    let first = builder.ensure_node(atom_text(&children.oz_atom));
                    builder.add_tree_edge(node, first);
                    for child in children.atom_opt_list {
                        let child = builder.ensure_node(atom_text(&child.oz_atom));
                        builder.add_tree_edge(node, child);
                    }
                }
            }
        }
    }
    Ok(builder.finish())
}

fn atom_text(atom: &OzAtom<'_>) -> String {
    let raw = match atom {
        OzAtom::TickLParenDotOrLBracketCircumflexTickRBracketRParenStarTick(value) => value
            .tick_l_paren_dot_or_l_bracket_circumflex_tick_r_bracket_r_paren_star_tick
            .text(),
        OzAtom::LBracketAMinusZRBracketLBracketAMinusZAMinusZ0Minus9UnderscoreULBrace0080RBraceMinusULBrace10ffffRBraceRBracketStar(value) => value
            .l_bracket_a_minus_z_r_bracket_l_bracket_a_minus_z_a_minus_z0_minus9_underscore_u_l_brace0080_r_brace_minus_u_l_brace10ffff_r_brace_r_bracket_star
            .text(),
    };
    unquote(raw)
}

fn unquote(raw: &str) -> String {
    if raw.starts_with('\'') && raw.ends_with('\'') {
        raw[1..raw.len() - 1].replace("\\'", "'")
    } else {
        raw.to_owned()
    }
}
