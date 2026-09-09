//! Parsing and streaming serialization for dominance graphs and solved forms.
//!
//! [`InputCodec`] selects an eager parser which produces a [`ParsedGraph`].
//! Output uses a different model because solution enumeration is destructive:
//! [`OutputCodec`] discovers a format, [`GraphOutputCodec`] writes one graph,
//! and [`SolutionEncoder`] writes a framed solution sequence without retaining
//! earlier solutions. The legacy [`encode_domcon_oz`] and [`encode_dot`]
//! helpers remain convenient when a complete in-memory [`String`] is desired.

use crate::graph::ParsedGraph;
use parol_runtime::{ParolError, ParserError};
use thiserror::Error;

mod domcon;
mod gxl;
mod holesem;
mod mrs;
mod output;

pub use domcon::parse_domcon_oz;
pub use gxl::parse_domgraph_gxl;
pub use holesem::parse_holesem;
pub use mrs::{
    PVariablePolicy, parse_mrs_prolog, parse_mrs_prolog_with_policy, parse_mrs_xml,
    parse_mrs_xml_with_policy,
};
pub use output::{GraphOutputCodec, OutputCodec, SolutionEncoder};

/// Input formats currently supported by the Rust implementation.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum InputCodec {
    /// Oz-style dominance constraints.
    DomconOz,
    /// GXL representation of a dominance graph.
    DomgraphGxl,
    /// Prolog-style Hole Semantics.
    HoleSemantics,
    /// DELPH-IN/LKB Prolog-style Minimal Recursion Semantics.
    MrsProlog,
    /// XML Minimal Recursion Semantics.
    MrsXml,
    /// Synthetic pure chains used for benchmarks.
    Chain,
}

impl InputCodec {
    /// Every input codec exposed by the command-line and desktop frontends.
    pub const ALL: [Self; 6] = [
        Self::Chain,
        Self::DomconOz,
        Self::DomgraphGxl,
        Self::HoleSemantics,
        Self::MrsProlog,
        Self::MrsXml,
    ];

    /// Resolve a canonical codec name or a frontend alias.
    #[must_use]
    pub fn from_name(name: &str) -> Option<Self> {
        match name {
            "domcon-oz" | "domcon" => Some(Self::DomconOz),
            "domgraph-gxl" | "gxl" => Some(Self::DomgraphGxl),
            "holesem-comsem" | "holesem" => Some(Self::HoleSemantics),
            "mrs-prolog" => Some(Self::MrsProlog),
            "mrs-xml" => Some(Self::MrsXml),
            "chain" => Some(Self::Chain),
            _ => None,
        }
    }

    /// Canonical command-line name.
    #[must_use]
    pub const fn name(self) -> &'static str {
        match self {
            Self::DomconOz => "domcon-oz",
            Self::DomgraphGxl => "domgraph-gxl",
            Self::HoleSemantics => "holesem-comsem",
            Self::MrsProlog => "mrs-prolog",
            Self::MrsXml => "mrs-xml",
            Self::Chain => "chain",
        }
    }

    /// Infer a codec from a file name. The inference is intentionally shared by
    /// the desktop and CLI frontends.
    #[must_use]
    #[allow(clippy::case_sensitive_file_extension_comparisons)]
    pub fn from_filename(filename: &str) -> Option<Self> {
        let filename = filename.to_ascii_lowercase();
        if filename.ends_with(".mrs.pl") {
            Some(Self::MrsProlog)
        } else if filename.ends_with(".hs.pl") {
            Some(Self::HoleSemantics)
        } else if filename.ends_with(".mrs.xml") {
            Some(Self::MrsXml)
        } else if filename.ends_with(".dg.xml") {
            Some(Self::DomgraphGxl)
        } else if filename.ends_with(".clls") {
            Some(Self::DomconOz)
        } else {
            None
        }
    }

    /// Parse input into a graph.
    ///
    /// # Errors
    ///
    /// Returns a codec error if the input is syntactically or semantically invalid.
    pub fn parse(self, input: &str) -> CodecResult {
        match self {
            Self::DomconOz => parse_domcon_oz(input),
            Self::DomgraphGxl => parse_domgraph_gxl(input),
            Self::HoleSemantics => parse_holesem(input),
            Self::MrsProlog => parse_mrs_prolog(input),
            Self::MrsXml => parse_mrs_xml(input),
            Self::Chain => parse_chain(input),
        }
    }
}

/// Generate the pure chain described by Java Utool's `chain` input codec.
///
/// # Errors
///
/// Returns a codec error unless `input` is a positive integer.
pub fn parse_chain(input: &str) -> CodecResult {
    let length = input
        .parse::<usize>()
        .map_err(|_| CodecError::Syntax("chain length must be an integer".to_owned()))?;
    if length == 0 {
        return Err(CodecError::Semantic(
            "You must specify a numeric chain length of at least 1!".to_owned(),
        ));
    }
    let mut builder = crate::GraphBuilder::default();
    let mut lower = builder.ensure_node("y0");
    builder.set_label(lower, "a0")?;
    for index in 1..=length {
        let root = builder.ensure_node(format!("x{index}"));
        let left = builder.ensure_node(format!("xl{index}"));
        let right = builder.ensure_node(format!("xr{index}"));
        builder.set_label(root, format!("f{index}"))?;
        builder.add_tree_edge(root, left);
        builder.add_tree_edge(root, right);
        builder.add_dominance_edge(left, lower);
        lower = builder.ensure_node(format!("y{index}"));
        builder.set_label(lower, format!("a{index}"))?;
        builder.add_dominance_edge(right, lower);
    }
    Ok(builder.finish())
}

/// Serialize a graph as canonical Domcon/Oz constraints.
///
/// This convenience function buffers the complete result. Use
/// [`OutputCodec::graph_encoder`] to write directly to an output stream.
#[must_use]
pub fn encode_domcon_oz(graph: &ParsedGraph) -> String {
    encode_graph_to_string(OutputCodec::DomconOz, graph)
}

/// Serialize a graph in Graphviz DOT form.
///
/// This convenience function buffers the complete result. Use
/// [`OutputCodec::graph_encoder`] to write directly to an output stream.
#[must_use]
pub fn encode_dot(graph: &ParsedGraph) -> String {
    encode_graph_to_string(OutputCodec::DomgraphDot, graph)
}

fn encode_graph_to_string(codec: OutputCodec, graph: &ParsedGraph) -> String {
    let mut output = Vec::new();
    codec
        .graph_encoder()
        .expect("selected codec supports graphs")
        .write_graph(graph, &mut output)
        .expect("writing to a Vec cannot fail");
    String::from_utf8(output).expect("text codecs emit UTF-8")
}

/// A syntax or semantic codec error.
#[derive(Debug, Error)]
pub enum CodecError {
    /// Parser-generator syntax error.
    #[error("syntax error: {0}")]
    Syntax(String),
    /// Parsed syntax cannot be lowered to a graph.
    #[error(transparent)]
    Graph(#[from] crate::graph::GraphError),
    /// Codec-specific semantic error.
    #[error("invalid codec input: {0}")]
    Semantic(String),
}

/// Common result type for graph codecs.
pub type CodecResult = Result<ParsedGraph, CodecError>;

pub(crate) fn format_parol_error(error: &ParolError, input: &str) -> String {
    let ParolError::ParserError(ParserError::SyntaxErrors { entries }) = error else {
        return error.to_string();
    };
    entries
        .iter()
        .map(|entry| {
            let location = entry
                .unexpected_tokens
                .first()
                .map_or(entry.error_location.as_ref(), |token| &token.token);
            let found = input
                .get(location.range())
                .filter(|text| !text.is_empty())
                .map_or_else(|| "end of input".to_owned(), |text| format!("{text:?}"));
            let expected = entry
                .expected_tokens
                .iter()
                .map(|token| readable_expected_token(token))
                .collect::<Vec<_>>()
                .join(", ");
            let expectation = if expected.is_empty() {
                format!("unexpected {found}")
            } else {
                format!("unexpected {found}; expected one of: {expected}")
            };
            source_diagnostic(
                input,
                location.start(),
                location.start_line as usize,
                location.start_column as usize,
                &expectation,
            )
        })
        .collect::<Vec<_>>()
        .join("\n\n")
}

pub(crate) fn format_source_error(input: &str, byte: usize, message: &str) -> String {
    source_diagnostic(input, byte, 0, 0, message)
}

fn readable_expected_token(token: &str) -> String {
    match token {
        "LParen" => "'('".to_owned(),
        "RParen" => "')'".to_owned(),
        "LBracket" => "'['".to_owned(),
        "RBracket" => "']'".to_owned(),
        "Comma" => "','".to_owned(),
        "Tick" => "apostrophe".to_owned(),
        other if other.chars().all(char::is_alphabetic) => {
            format!("'{}'", other.to_ascii_lowercase())
        }
        other => other.to_owned(),
    }
}

fn source_diagnostic(
    input: &str,
    byte: usize,
    reported_line: usize,
    reported_column: usize,
    message: &str,
) -> String {
    let byte = byte.min(input.len());
    let prefix = &input[..byte];
    let line = if reported_line != 0 {
        reported_line
    } else {
        prefix.bytes().filter(|byte| *byte == b'\n').count() + 1
    };
    let line_start = prefix.rfind('\n').map_or(0, |offset| offset + 1);
    let column = if reported_column != 0 {
        reported_column
    } else {
        input[line_start..byte].chars().count() + 1
    };
    let line_end = input[line_start..]
        .find(['\r', '\n'])
        .map_or(input.len(), |offset| line_start + offset);
    let source_line = &input[line_start..line_end];
    let characters = source_line.chars().collect::<Vec<_>>();
    let target = column.saturating_sub(1).min(characters.len());
    let excerpt_start = target.saturating_sub(50);
    let excerpt_end = (target + 50).min(characters.len());
    let has_prefix = excerpt_start != 0;
    let has_suffix = excerpt_end != characters.len();
    let excerpt = format!(
        "{}{}{}",
        if has_prefix { "…" } else { "" },
        characters[excerpt_start..excerpt_end]
            .iter()
            .collect::<String>(),
        if has_suffix { "…" } else { "" },
    );
    let caret_column = target - excerpt_start + usize::from(has_prefix);
    format!(
        "{message} at line {line}, column {column}\n\nOffending input:\n  {excerpt}\n  {}^",
        " ".repeat(caret_column)
    )
}
