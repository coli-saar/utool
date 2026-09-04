use super::CodecResult;

/// Parse Domcon/Oz syntax into a graph.
pub fn parse_domcon_oz(input: &str) -> CodecResult {
    crate::domcon_oz_grammar::parse(input)
}
