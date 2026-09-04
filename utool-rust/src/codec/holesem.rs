use super::CodecResult;

/// Parse Hole Semantics syntax into a graph.
pub fn parse_holesem(input: &str) -> CodecResult {
    crate::holesem_grammar::parse(input)
}
