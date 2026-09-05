//! Streaming output codecs.
//!
//! Output has two distinct source types. Graph codecs serialize one
//! [`ParsedGraph`], whereas solution codecs serialize a sequence produced by
//! the destructive chart iterator. [`OutputCodec`] is the shared registry for
//! names, filename inference, and capability checks. [`GraphOutputCodec`] and
//! [`SolutionEncoder`] define the two encoding contracts.
//!
//! A solution encoder is stateful because delimiters belong to the sequence,
//! not to an individual solution. Call [`SolutionEncoder::begin`] once, then
//! [`SolutionEncoder::write_solution`] for each current solution, and finally
//! [`SolutionEncoder::finish`]. Encoders retain only reusable traversal state;
//! they never retain a solution after `write_solution` returns.

use crate::graph::ParsedGraph;
use crate::solver::Solution;
use packed_term_arena::tree::Tree;
use std::io::{self, Write};

/// A registered output format.
///
/// The enum provides stable names and capability discovery. Use
/// [`Self::graph_encoder`] for a graph conversion and
/// [`Self::solution_encoder`] for destructive solution enumeration.
/// The registry is intentionally closed: external code may implement either
/// encoder trait, but doing so does not add a variant or filename mapping here.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum OutputCodec {
    /// Oz-style dominance constraints; supports graphs and solutions.
    DomconOz,
    /// Graphviz DOT; supports graphs only.
    DomgraphDot,
    /// Prolog term syntax; supports solutions only.
    TermProlog,
    /// Oz term syntax; supports solutions only.
    TermOz,
}

impl OutputCodec {
    /// Resolve a canonical codec name or a short frontend alias.
    #[must_use]
    pub fn from_name(name: &str) -> Option<Self> {
        match name {
            "domcon-oz" | "domcon" => Some(Self::DomconOz),
            "domgraph-dot" | "dot" => Some(Self::DomgraphDot),
            "term-prolog" => Some(Self::TermProlog),
            "term-oz" => Some(Self::TermOz),
            _ => None,
        }
    }

    /// Infer an output codec from the conventional compound filename suffix.
    #[must_use]
    // The complete filename is normalized before checking compound suffixes.
    #[allow(clippy::case_sensitive_file_extension_comparisons)]
    pub fn from_filename(filename: &str) -> Option<Self> {
        let filename = filename.to_ascii_lowercase();
        if filename.ends_with(".dg.dot") {
            Some(Self::DomgraphDot)
        } else if filename.ends_with(".t.pl") {
            Some(Self::TermProlog)
        } else if filename.ends_with(".t.oz") {
            Some(Self::TermOz)
        } else if filename.ends_with(".clls") {
            Some(Self::DomconOz)
        } else {
            None
        }
    }

    /// Canonical command-line name.
    #[must_use]
    pub const fn name(self) -> &'static str {
        match self {
            Self::DomconOz => "domcon-oz",
            Self::DomgraphDot => "domgraph-dot",
            Self::TermProlog => "term-prolog",
            Self::TermOz => "term-oz",
        }
    }

    /// Whether this format can serialize a parsed graph.
    #[must_use]
    pub const fn supports_graph(self) -> bool {
        matches!(self, Self::DomconOz | Self::DomgraphDot)
    }

    /// Whether this format can serialize a sequence of solved forms.
    #[must_use]
    pub const fn supports_solutions(self) -> bool {
        matches!(self, Self::DomconOz | Self::TermProlog | Self::TermOz)
    }

    /// Return the graph encoder when this format supports graph conversion.
    #[must_use]
    pub fn graph_encoder(self) -> Option<&'static dyn GraphOutputCodec> {
        match self {
            Self::DomconOz => Some(&DOMCON_GRAPH_ENCODER),
            Self::DomgraphDot => Some(&DOT_GRAPH_ENCODER),
            Self::TermProlog | Self::TermOz => None,
        }
    }

    /// Construct a fresh stateful solution encoder when this format supports
    /// solution sequences.
    #[must_use]
    pub fn solution_encoder(self) -> Option<Box<dyn SolutionEncoder>> {
        match self {
            Self::DomconOz => Some(Box::new(DomconSolutionEncoder::default())),
            Self::TermProlog => Some(Box::new(TermSolutionEncoder::prolog())),
            Self::TermOz => Some(Box::new(TermSolutionEncoder::oz())),
            Self::DomgraphDot => None,
        }
    }
}

/// Serialize a single parsed graph to a byte stream.
///
/// Implementations must finish the graph before returning and must not retain
/// `graph` or `output`.
pub trait GraphOutputCodec: Send + Sync {
    /// Write one complete graph.
    ///
    /// # Errors
    ///
    /// Returns an error reported by `output`.
    fn write_graph(&self, graph: &ParsedGraph, output: &mut dyn Write) -> io::Result<()>;
}

/// Serialize a sequence of destructively enumerated solutions.
///
/// `begin`, zero or more calls to `write_solution`, and `finish` form one
/// output document. A codec may reuse internal buffers between solutions, but
/// it must not retain a [`Solution`] because the iterator mutates its arena on
/// the next advance.
///
/// An encoder instance represents one active output document. Callers should
/// not interleave documents through the same instance.
pub trait SolutionEncoder {
    /// Start a new output document and reset reusable encoder state.
    ///
    /// # Errors
    ///
    /// Returns an error reported by `output`.
    fn begin(&mut self, output: &mut dyn Write) -> io::Result<()>;

    /// Write the current solution completely before the iterator advances.
    ///
    /// # Errors
    ///
    /// Returns an error reported by `output`.
    fn write_solution(&mut self, solution: &Solution<'_>, output: &mut dyn Write)
    -> io::Result<()>;

    /// Close the output document, including the empty-sequence representation.
    ///
    /// # Errors
    ///
    /// Returns an error reported by `output`.
    fn finish(&mut self, output: &mut dyn Write) -> io::Result<()>;
}

struct DomconGraphEncoder;
struct DotGraphEncoder;

static DOMCON_GRAPH_ENCODER: DomconGraphEncoder = DomconGraphEncoder;
static DOT_GRAPH_ENCODER: DotGraphEncoder = DotGraphEncoder;

impl GraphOutputCodec for DomconGraphEncoder {
    fn write_graph(&self, graph: &ParsedGraph, output: &mut dyn Write) -> io::Result<()> {
        output.write_all(b"[")?;
        let mut first = true;
        for node in graph.nodes() {
            let Some(label) = node.label() else {
                continue;
            };
            write_separator(&mut first, output)?;
            output.write_all(b"label(")?;
            write_oz_atom(node.name(), output)?;
            output.write_all(b" ")?;
            write_oz_atom(label, output)?;
            write_children(graph, node.tree_children(), output)?;
            output.write_all(b")")?;
        }
        for &(source, target) in graph.dominance_edges() {
            write_separator(&mut first, output)?;
            output.write_all(b"dom(")?;
            write_oz_atom(graph.node(source).name(), output)?;
            output.write_all(b" ")?;
            write_oz_atom(graph.node(target).name(), output)?;
            output.write_all(b")")?;
        }
        output.write_all(b"]")
    }
}

fn write_separator(first: &mut bool, output: &mut dyn Write) -> io::Result<()> {
    if *first {
        *first = false;
        Ok(())
    } else {
        output.write_all(b" ")
    }
}

fn write_children(
    graph: &ParsedGraph,
    children: &[crate::graph::NodeId],
    output: &mut dyn Write,
) -> io::Result<()> {
    if children.is_empty() {
        return Ok(());
    }
    output.write_all(b"(")?;
    for (index, child) in children.iter().enumerate() {
        if index > 0 {
            output.write_all(b" ")?;
        }
        write_oz_atom(graph.node(*child).name(), output)?;
    }
    output.write_all(b")")
}

fn write_oz_atom(value: &str, output: &mut dyn Write) -> io::Result<()> {
    let mut characters = value.chars();
    let bare = characters
        .next()
        .is_some_and(|first| first.is_ascii_lowercase())
        && characters.all(|character| character.is_alphanumeric() || character == '_');
    if bare {
        return output.write_all(value.as_bytes());
    }

    output.write_all(b"'")?;
    for character in value.chars() {
        if matches!(character, '\\' | '\'') {
            output.write_all(b"\\")?;
        }
        write!(output, "{character}")?;
    }
    output.write_all(b"'")
}

impl GraphOutputCodec for DotGraphEncoder {
    fn write_graph(&self, graph: &ParsedGraph, output: &mut dyn Write) -> io::Result<()> {
        output.write_all(b"digraph dominance_graph {\n")?;
        for node in graph.nodes() {
            let label = node.label().unwrap_or(node.name());
            writeln!(
                output,
                "  {} [label={}];",
                quoted(node.name()),
                quoted(label)
            )?;
            for child in node.tree_children() {
                writeln!(
                    output,
                    "  {} -> {} [style=solid];",
                    quoted(node.name()),
                    quoted(graph.node(*child).name())
                )?;
            }
        }
        for &(source, target) in graph.dominance_edges() {
            writeln!(
                output,
                "  {} -> {} [style=dotted];",
                quoted(graph.node(source).name()),
                quoted(graph.node(target).name())
            )?;
        }
        output.write_all(b"}\n")
    }
}

fn quoted(value: &str) -> String {
    format!("\"{}\"", value.replace('\\', "\\\\").replace('\"', "\\\""))
}

#[derive(Default)]
struct DomconSolutionEncoder {
    stack: Vec<Tree>,
    written: usize,
}

impl SolutionEncoder for DomconSolutionEncoder {
    fn begin(&mut self, output: &mut dyn Write) -> io::Result<()> {
        self.stack.clear();
        self.written = 0;
        output.write_all(b"%%  autogenerated by Utool\n[\n")
    }

    fn write_solution(
        &mut self,
        solution: &Solution<'_>,
        output: &mut dyn Write,
    ) -> io::Result<()> {
        output.write_all(b"[")?;
        if let Some(root) = solution.root() {
            let mut first = true;
            write_solution_node(solution, root, &mut first, output)?;
            self.stack.clear();
            self.stack.push(root);
            while let Some(tree) = self.stack.pop() {
                for &child in solution.arena().get_children(tree) {
                    write_solution_node(solution, child, &mut first, output)?;
                    self.stack.push(child);
                }
            }
        }
        output.write_all(b"]\n")?;
        self.written += 1;
        Ok(())
    }

    fn finish(&mut self, output: &mut dyn Write) -> io::Result<()> {
        if self.written == 0 {
            output.write_all(b"\n")?;
        }
        output.write_all(b"]\n")
    }
}

fn write_solution_node(
    solution: &Solution<'_>,
    tree: Tree,
    first: &mut bool,
    output: &mut dyn Write,
) -> io::Result<()> {
    write_separator(first, output)?;
    output.write_all(b"label(")?;
    write_oz_atom(solution.node_name(tree), output)?;
    output.write_all(b" ")?;
    write_oz_atom(solution.node_label(tree), output)?;
    let children = solution.arena().get_children(tree);
    if !children.is_empty() {
        output.write_all(b"(")?;
        for (index, child) in children.iter().enumerate() {
            if index > 0 {
                output.write_all(b" ")?;
            }
            write_oz_atom(solution.node_name(*child), output)?;
        }
        output.write_all(b")")?;
    }
    output.write_all(b")")
}

struct TermSolutionEncoder {
    argument_separator: &'static str,
    solution_separator: &'static [u8],
    written: bool,
}

impl TermSolutionEncoder {
    const fn prolog() -> Self {
        Self {
            argument_separator: ",",
            solution_separator: b",\n",
            written: false,
        }
    }

    const fn oz() -> Self {
        Self {
            argument_separator: " ",
            solution_separator: b" \n",
            written: false,
        }
    }
}

impl SolutionEncoder for TermSolutionEncoder {
    fn begin(&mut self, output: &mut dyn Write) -> io::Result<()> {
        self.written = false;
        output.write_all(b"[")
    }

    fn write_solution(
        &mut self,
        solution: &Solution<'_>,
        output: &mut dyn Write,
    ) -> io::Result<()> {
        if self.written {
            output.write_all(self.solution_separator)?;
        }
        if let Some(root) = solution.root() {
            write_label_term(solution, root, self.argument_separator, output)?;
        }
        self.written = true;
        Ok(())
    }

    fn finish(&mut self, output: &mut dyn Write) -> io::Result<()> {
        output.write_all(b"]")
    }
}

fn write_label_term(
    solution: &Solution<'_>,
    tree: Tree,
    separator: &str,
    output: &mut dyn Write,
) -> io::Result<()> {
    output.write_all(solution.node_label(tree).as_bytes())?;
    let children = solution.arena().get_children(tree);
    if !children.is_empty() {
        output.write_all(b"(")?;
        for (index, child) in children.iter().enumerate() {
            if index > 0 {
                output.write_all(separator.as_bytes())?;
            }
            write_label_term(solution, *child, separator, output)?;
        }
        output.write_all(b")")?;
    }
    Ok(())
}
