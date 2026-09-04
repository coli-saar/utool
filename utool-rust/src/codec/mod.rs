//! Text codecs.

use crate::graph::ParsedGraph;
use thiserror::Error;

mod domcon;
mod holesem;

pub use domcon::parse_domcon_oz;
pub use holesem::parse_holesem;

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
    pub fn parse(self, input: &str) -> CodecResult {
        match self {
            Self::DomconOz => parse_domcon_oz(input),
            Self::HoleSemantics => parse_holesem(input),
            Self::Chain => parse_chain(input),
        }
    }
}

/// Generate the pure chain described by Java Utool's `chain` input codec.
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
#[must_use]
pub fn encode_domcon_oz(graph: &ParsedGraph) -> String {
    let mut constraints = Vec::new();
    for node in graph.nodes() {
        if let Some(label) = node.label() {
            let children = node
                .tree_children()
                .iter()
                .map(|child| graph.node(*child).name())
                .collect::<Vec<_>>()
                .join(" ");
            constraints.push(if children.is_empty() {
                format!("label({} {})", node.name(), label)
            } else {
                format!("label({} {}({children}))", node.name(), label)
            });
        }
    }
    constraints.extend(graph.dominance_edges().iter().map(|(source, target)| {
        format!(
            "dom({} {})",
            graph.node(*source).name(),
            graph.node(*target).name()
        )
    }));
    format!("[{}]", constraints.join(" "))
}

/// Serialize a graph in Graphviz DOT form.
#[must_use]
pub fn encode_dot(graph: &ParsedGraph) -> String {
    fn quoted(value: &str) -> String {
        format!("\"{}\"", value.replace('\\', "\\\\").replace('\"', "\\\""))
    }
    let mut output = String::from("digraph dominance_graph {\n");
    for node in graph.nodes() {
        let label = node.label().unwrap_or(node.name());
        output.push_str(&format!(
            "  {} [label={}];\n",
            quoted(node.name()),
            quoted(label)
        ));
        for child in node.tree_children() {
            output.push_str(&format!(
                "  {} -> {} [style=solid];\n",
                quoted(node.name()),
                quoted(graph.node(*child).name())
            ));
        }
    }
    for (source, target) in graph.dominance_edges() {
        output.push_str(&format!(
            "  {} -> {} [style=dotted];\n",
            quoted(graph.node(*source).name()),
            quoted(graph.node(*target).name())
        ));
    }
    output.push_str("}\n");
    output
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
