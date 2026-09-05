//! Parsing and streaming serialization for dominance graphs and solved forms.
//!
//! [`InputCodec`] selects an eager parser which produces a [`ParsedGraph`].
//! Output uses a different model because solution enumeration is destructive:
//! [`OutputCodec`] discovers a format, [`GraphOutputCodec`] writes one graph,
//! and [`SolutionEncoder`] writes a framed solution sequence without retaining
//! earlier solutions. The legacy [`encode_domcon_oz`] and [`encode_dot`]
//! helpers remain convenient when a complete in-memory [`String`] is desired.

use crate::graph::ParsedGraph;
use thiserror::Error;

mod domcon;
mod holesem;
mod output;

pub use domcon::parse_domcon_oz;
pub use holesem::parse_holesem;
pub use output::{GraphOutputCodec, OutputCodec, SolutionEncoder};

/// Input formats currently supported by the Rust implementation.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum InputCodec {
    /// Oz-style dominance constraints.
    DomconOz,
    /// Prolog-style Hole Semantics.
    HoleSemantics,
    /// Synthetic pure chains used for benchmarks.
    Chain,
}

impl InputCodec {
    /// Resolve a canonical codec name or a frontend alias.
    #[must_use]
    pub fn from_name(name: &str) -> Option<Self> {
        match name {
            "domcon-oz" | "domcon" => Some(Self::DomconOz),
            "holesem-comsem" | "holesem" => Some(Self::HoleSemantics),
            "chain" => Some(Self::Chain),
            _ => None,
        }
    }

    /// Canonical command-line name.
    #[must_use]
    pub const fn name(self) -> &'static str {
        match self {
            Self::DomconOz => "domcon-oz",
            Self::HoleSemantics => "holesem-comsem",
            Self::Chain => "chain",
        }
    }

    /// Infer a codec from a file name. The inference is intentionally shared by
    /// the desktop and CLI frontends.
    #[must_use]
    pub fn from_filename(filename: &str) -> Option<Self> {
        let extension = std::path::Path::new(filename)
            .extension()?
            .to_str()?
            .to_ascii_lowercase();
        match extension.as_str() {
            "pl" | "holesem" => Some(Self::HoleSemantics),
            "clls" | "domcon" | "oz" | "txt" => Some(Self::DomconOz),
            _ => None,
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
            Self::HoleSemantics => parse_holesem(input),
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
