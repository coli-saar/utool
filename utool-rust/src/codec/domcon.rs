use super::CodecResult;

/// Parse Domcon/Oz syntax into a graph.
///
/// # Errors
///
/// Returns a codec error if the input is syntactically or semantically invalid.
pub fn parse_domcon_oz(input: &str) -> CodecResult {
    crate::domcon_oz_grammar::parse(input)
}
