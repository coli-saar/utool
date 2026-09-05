use super::CodecResult;

/// Parse Hole Semantics syntax into a graph.
///
/// # Errors
///
/// Returns a codec error if the input is syntactically or semantically invalid.
pub fn parse_holesem(input: &str) -> CodecResult {
    crate::holesem_grammar::parse(input)
}
