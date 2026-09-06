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
    /// GXL dominance graph XML; supports graphs and solutions.
    DomgraphGxl,
    /// uDraw(Graph) term syntax; supports graphs only.
    DomgraphUdraw,
    /// Java source which reconstructs dominance graphs.
    DomgraphCodegen,
    /// Dominance-edge pluggings in Oz syntax.
    PluggingOz,
    /// Dominance-edge pluggings for the LKB MRS solver interface.
    PluggingLkb,
    /// Dominance-edge pluggings in the historical Groovy structure.
    PluggingGroovy,
    /// Prolog term syntax; supports solutions only.
    TermProlog,
    /// Oz term syntax; supports solutions only.
    TermOz,
}

impl OutputCodec {
    /// Every registered output codec, in the order used by frontends.
    pub const ALL: [Self; 10] = [
        Self::DomconOz,
        Self::DomgraphDot,
        Self::DomgraphGxl,
        Self::DomgraphUdraw,
        Self::DomgraphCodegen,
        Self::PluggingOz,
        Self::PluggingLkb,
        Self::PluggingGroovy,
        Self::TermProlog,
        Self::TermOz,
    ];

    /// Resolve a canonical codec name or a short frontend alias.
    #[must_use]
    pub fn from_name(name: &str) -> Option<Self> {
        match name {
            "domcon-oz" | "domcon" => Some(Self::DomconOz),
            "domgraph-dot" | "dot" => Some(Self::DomgraphDot),
            "domgraph-gxl" | "gxl" => Some(Self::DomgraphGxl),
            "domgraph-udraw" | "udraw" => Some(Self::DomgraphUdraw),
            "domgraph-codegen" | "codegen" => Some(Self::DomgraphCodegen),
            "plugging-oz" => Some(Self::PluggingOz),
            "plugging-lkb" => Some(Self::PluggingLkb),
            "plugging-groovy" => Some(Self::PluggingGroovy),
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
        } else if filename.ends_with(".dg.xml") {
            Some(Self::DomgraphGxl)
        } else if filename.ends_with(".dg.udg") {
            Some(Self::DomgraphUdraw)
        } else if filename.ends_with(".plug.oz") {
            Some(Self::PluggingOz)
        } else if filename.ends_with(".lkbplug.lisp") {
            Some(Self::PluggingLkb)
        } else if filename.ends_with(".t.pl") {
            Some(Self::TermProlog)
        } else if filename.ends_with(".t.oz") {
            Some(Self::TermOz)
        } else if filename.ends_with(".java") {
            Some(Self::DomgraphCodegen)
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
            Self::DomgraphGxl => "domgraph-gxl",
            Self::DomgraphUdraw => "domgraph-udraw",
            Self::DomgraphCodegen => "domgraph-codegen",
            Self::PluggingOz => "plugging-oz",
            Self::PluggingLkb => "plugging-lkb",
            Self::PluggingGroovy => "plugging-groovy",
            Self::TermProlog => "term-prolog",
            Self::TermOz => "term-oz",
        }
    }

    /// Whether this format can serialize a parsed graph.
    #[must_use]
    pub const fn supports_graph(self) -> bool {
        !matches!(self, Self::TermProlog | Self::TermOz)
    }

    /// Whether this format can serialize a sequence of solved forms.
    #[must_use]
    pub const fn supports_solutions(self) -> bool {
        !matches!(self, Self::DomgraphDot | Self::DomgraphUdraw)
    }

    /// Return the graph encoder when this format supports graph conversion.
    #[must_use]
    pub fn graph_encoder(self) -> Option<&'static dyn GraphOutputCodec> {
        match self {
            Self::DomconOz => Some(&DOMCON_GRAPH_ENCODER),
            Self::DomgraphDot => Some(&DOT_GRAPH_ENCODER),
            Self::DomgraphGxl => Some(&GXL_GRAPH_ENCODER),
            Self::DomgraphUdraw => Some(&UDRAW_GRAPH_ENCODER),
            Self::DomgraphCodegen => Some(&CODEGEN_GRAPH_ENCODER),
            Self::PluggingOz => Some(&PLUGGING_OZ_GRAPH_ENCODER),
            Self::PluggingLkb => Some(&PLUGGING_LKB_GRAPH_ENCODER),
            Self::PluggingGroovy => Some(&PLUGGING_GROOVY_GRAPH_ENCODER),
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
            Self::DomgraphGxl => Some(Box::new(LegacySolutionEncoder::new(LegacyKind::Gxl))),
            Self::DomgraphCodegen => {
                Some(Box::new(LegacySolutionEncoder::new(LegacyKind::Codegen)))
            }
            Self::PluggingOz => Some(Box::new(LegacySolutionEncoder::new(LegacyKind::PluggingOz))),
            Self::PluggingLkb => Some(Box::new(LegacySolutionEncoder::new(
                LegacyKind::PluggingLkb,
            ))),
            Self::PluggingGroovy => Some(Box::new(LegacySolutionEncoder::new(
                LegacyKind::PluggingGroovy,
            ))),
            Self::DomgraphDot | Self::DomgraphUdraw => None,
        }
    }

    /// Serialize one solved form without the framing used for a solution sequence.
    ///
    /// This is the representation used by the legacy XML server's individual
    /// `solution` attributes.
    ///
    /// # Errors
    ///
    /// Returns an error if the selected codec cannot materialize or write the
    /// solved form.
    pub fn write_single_solution(
        self,
        solution: &Solution<'_>,
        output: &mut dyn Write,
    ) -> io::Result<()> {
        self.write_single_solution_at(solution, 1, output)
    }

    /// Serialize one solved form using `ordinal` for formats which number outputs.
    ///
    /// # Errors
    ///
    /// Returns an error if the selected codec cannot materialize or write the
    /// solved form.
    pub fn write_single_solution_at(
        self,
        solution: &Solution<'_>,
        ordinal: usize,
        output: &mut dyn Write,
    ) -> io::Result<()> {
        match self {
            Self::DomconOz => {
                output.write_all(b"[")?;
                let mut first = true;
                write_solution_node(solution, solution.root(), &mut first, output)?;
                let mut stack = vec![solution.root()];
                while let Some(tree) = stack.pop() {
                    for &child in solution.arena().get_children(tree) {
                        write_solution_node(solution, child, &mut first, output)?;
                        stack.push(child);
                    }
                }
                output.write_all(b"]\n")
            }
            Self::TermProlog => write_label_term(solution, solution.root(), ",", output),
            Self::TermOz => write_label_term(solution, solution.root(), " ", output),
            Self::DomgraphGxl => write_gxl_graph(&materialize_solution(solution)?, output),
            Self::DomgraphCodegen => {
                write_codegen_graph(&materialize_solution(solution)?, ordinal, output)
            }
            Self::PluggingOz => {
                write_plugging(&solution_pluggings(solution), PluggingStyle::Oz, output)
            }
            Self::PluggingLkb => {
                write_plugging(&solution_pluggings(solution), PluggingStyle::Lkb, output)
            }
            Self::PluggingGroovy => {
                write_plugging(&solution_pluggings(solution), PluggingStyle::Groovy, output)
            }
            Self::DomgraphDot | Self::DomgraphUdraw => {
                let Some(encoder) = self.graph_encoder() else {
                    return Err(io::Error::new(
                        io::ErrorKind::Unsupported,
                        "graph codec has no graph encoder",
                    ));
                };
                encoder.write_graph(&materialize_solution(solution)?, output)
            }
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
struct GxlGraphEncoder;
struct UdrawGraphEncoder;
struct CodegenGraphEncoder;
struct PluggingGraphEncoder(PluggingStyle);

static DOMCON_GRAPH_ENCODER: DomconGraphEncoder = DomconGraphEncoder;
static DOT_GRAPH_ENCODER: DotGraphEncoder = DotGraphEncoder;
static GXL_GRAPH_ENCODER: GxlGraphEncoder = GxlGraphEncoder;
static UDRAW_GRAPH_ENCODER: UdrawGraphEncoder = UdrawGraphEncoder;
static CODEGEN_GRAPH_ENCODER: CodegenGraphEncoder = CodegenGraphEncoder;
static PLUGGING_OZ_GRAPH_ENCODER: PluggingGraphEncoder = PluggingGraphEncoder(PluggingStyle::Oz);
static PLUGGING_LKB_GRAPH_ENCODER: PluggingGraphEncoder = PluggingGraphEncoder(PluggingStyle::Lkb);
static PLUGGING_GROOVY_GRAPH_ENCODER: PluggingGraphEncoder =
    PluggingGraphEncoder(PluggingStyle::Groovy);

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

fn xml(value: &str) -> String {
    value
        .replace('&', "&amp;")
        .replace('<', "&lt;")
        .replace('>', "&gt;")
        .replace('"', "&quot;")
        .replace('\'', "&apos;")
}

impl GraphOutputCodec for GxlGraphEncoder {
    fn write_graph(&self, graph: &ParsedGraph, output: &mut dyn Write) -> io::Result<()> {
        output.write_all(
            b"<?xml version=\"1.0\"?>\n<gxl xmlns:xlink=\"http://www.w3.org/1999/xlink\">\n",
        )?;
        write_gxl_graph(graph, output)?;
        output.write_all(b"</gxl>\n")
    }
}

fn write_gxl_graph(graph: &ParsedGraph, output: &mut dyn Write) -> io::Result<()> {
    output.write_all(b"   <graph id=\"utool-graph\" edgeids=\"true\" hypergraph=\"false\" edgemode=\"directed\">\n")?;
    for node in graph.nodes() {
        writeln!(output, "      <node id=\"{}\">", xml(node.name()))?;
        if let Some(label) = node.label() {
            let kind = if node.tree_children().is_empty() {
                "leaf"
            } else {
                "root"
            };
            writeln!(output, "         <type xlink:href=\"{kind}\" />")?;
            writeln!(
                output,
                "         <attr name=\"label\"><string>{}</string></attr>",
                xml(label)
            )?;
        } else {
            output.write_all(b"         <type xlink:href=\"hole\" />\n")?;
        }
        output.write_all(b"      </node>\n")?;
    }
    let mut edge = 0;
    for node in graph.nodes() {
        for child in node.tree_children() {
            write_gxl_edge(
                node.name(),
                graph.node(*child).name(),
                "solid",
                edge,
                output,
            )?;
            edge += 1;
        }
    }
    for &(source, target) in graph.dominance_edges() {
        write_gxl_edge(
            graph.node(source).name(),
            graph.node(target).name(),
            "dominance",
            edge,
            output,
        )?;
        edge += 1;
    }
    output.write_all(b"   </graph>\n")
}

fn write_gxl_edge(
    source: &str,
    target: &str,
    kind: &str,
    id: usize,
    output: &mut dyn Write,
) -> io::Result<()> {
    writeln!(
        output,
        "      <edge from=\"{}\" to=\"{}\" id=\"edge{id}\">",
        xml(source),
        xml(target)
    )?;
    writeln!(output, "        <type xlink:href=\"{kind}\" />")?;
    output.write_all(b"      </edge>\n")
}

impl GraphOutputCodec for UdrawGraphEncoder {
    fn write_graph(&self, graph: &ParsedGraph, output: &mut dyn Write) -> io::Result<()> {
        let mut incoming = vec![0_usize; graph.nodes().len()];
        for node in graph.nodes() {
            for child in node.tree_children() {
                incoming[child.index()] += 1;
            }
        }
        for &(_, target) in graph.dominance_edges() {
            incoming[target.index()] += 1;
        }
        let roots: Vec<_> = incoming
            .iter()
            .enumerate()
            .filter_map(|(i, degree)| (*degree == 0).then_some(crate::graph::NodeId::from_index(i)))
            .collect();
        output.write_all(b"[")?;
        let mut seen = std::collections::HashSet::new();
        for (index, root) in roots.into_iter().enumerate() {
            if index > 0 {
                output.write_all(b",")?;
            }
            write_udraw_node(graph, root, &mut seen, output)?;
        }
        output.write_all(b"]\n")
    }
}

fn write_udraw_node(
    graph: &ParsedGraph,
    node: crate::graph::NodeId,
    seen: &mut std::collections::HashSet<crate::graph::NodeId>,
    output: &mut dyn Write,
) -> io::Result<()> {
    let data = graph.node(node);
    if !seen.insert(node) {
        return write!(output, "r(\"{}\")", data.name());
    }
    write!(
        output,
        "l(\"{}\",n(\"\",[a(\"OBJECT\",\"{}",
        data.name(),
        data.name()
    )?;
    if let Some(label) = data.label() {
        write!(output, ":{label}")?;
    }
    output.write_all(b"\")],[")?;
    let mut first = true;
    for &child in data.tree_children() {
        if !first {
            output.write_all(b",")?;
        }
        first = false;
        output.write_all(b"e(\"\",[a(\"EDGEPATTERN\",\"solid\")],")?;
        write_udraw_node(graph, child, seen, output)?;
        output.write_all(b")")?;
    }
    for &(source, target) in graph.dominance_edges() {
        if source != node {
            continue;
        }
        if !first {
            output.write_all(b",")?;
        }
        first = false;
        output.write_all(b"e(\"\",[a(\"EDGEPATTERN\",\"dotted\")],")?;
        write_udraw_node(graph, target, seen, output)?;
        output.write_all(b")")?;
    }
    output.write_all(b"]))")
}

impl GraphOutputCodec for CodegenGraphEncoder {
    fn write_graph(&self, graph: &ParsedGraph, output: &mut dyn Write) -> io::Result<()> {
        output.write_all(b"class DomgraphGenerator {\n")?;
        write_codegen_graph(graph, 1, output)?;
        output.write_all(b"\n}\n")
    }
}

fn java_string(value: &str) -> String {
    value
        .replace('\\', "\\\\")
        .replace('"', "\\\"")
        .replace('\n', "\\n")
        .replace('\r', "\\r")
}

fn write_codegen_graph(
    graph: &ParsedGraph,
    method: usize,
    output: &mut dyn Write,
) -> io::Result<()> {
    writeln!(
        output,
        "\n    public void makeGraph{method}(DomGraph graph, NodeLabels labels) {{"
    )?;
    output.write_all(b"        graph.clear();\n        labels.clear();\n\n")?;
    for node in graph.nodes() {
        let kind = if node.is_hole() {
            "UNLABELLED"
        } else {
            "LABELLED"
        };
        writeln!(
            output,
            "        graph.addNode(\"{}\", new NodeData(NodeType.{kind}));",
            java_string(node.name())
        )?;
        if let Some(label) = node.label() {
            writeln!(
                output,
                "        labels.addLabel(\"{}\", \"{}\");",
                java_string(node.name()),
                java_string(label)
            )?;
        }
    }
    output.write_all(b"\n")?;
    for node in graph.nodes() {
        for child in node.tree_children() {
            writeln!(
                output,
                "        graph.addEdge(\"{}\", \"{}\", new EdgeData(EdgeType.TREE));",
                java_string(node.name()),
                java_string(graph.node(*child).name())
            )?;
        }
    }
    for &(source, target) in graph.dominance_edges() {
        writeln!(
            output,
            "        graph.addEdge(\"{}\", \"{}\", new EdgeData(EdgeType.DOMINANCE));",
            java_string(graph.node(source).name()),
            java_string(graph.node(target).name())
        )?;
    }
    output.write_all(b"    }\n")
}

#[derive(Clone, Copy)]
enum PluggingStyle {
    Oz,
    Lkb,
    Groovy,
}

impl GraphOutputCodec for PluggingGraphEncoder {
    fn write_graph(&self, graph: &ParsedGraph, output: &mut dyn Write) -> io::Result<()> {
        let edges: Vec<_> = graph
            .dominance_edges()
            .iter()
            .map(|&(source, target)| (graph.node(source).name(), graph.node(target).name()))
            .collect();
        write_plugging(&edges, self.0, output)
    }
}

fn write_plugging(
    edges: &[(&str, &str)],
    style: PluggingStyle,
    output: &mut dyn Write,
) -> io::Result<()> {
    match style {
        PluggingStyle::Oz => {
            output.write_all(b"[")?;
            for (index, (source, target)) in edges.iter().enumerate() {
                if index > 0 {
                    output.write_all(b" ")?;
                }
                output.write_all(b"plug(")?;
                write_oz_atom(source, output)?;
                output.write_all(b" ")?;
                write_oz_atom(target, output)?;
                output.write_all(b")")?;
            }
            output.write_all(b"]\n")
        }
        PluggingStyle::Lkb => {
            output.write_all(b"( ")?;
            for (index, (source, target)) in edges.iter().enumerate() {
                if index > 0 {
                    output.write_all(b" ")?;
                }
                let source = source.get(1..).unwrap_or(source);
                let target = target.get(1..).unwrap_or(target);
                write!(
                    output,
                    "({source} {source} {target}) ({target} {source} {target})"
                )?;
            }
            output.write_all(b")\n")
        }
        PluggingStyle::Groovy => {
            output.write_all(b"[[")?;
            for (index, (source, target)) in edges.iter().enumerate() {
                if index > 0 {
                    output.write_all(b", ")?;
                }
                write!(output, "[\"{source}\", \"{target}\"]")?;
            }
            output.write_all(b"],[:]]")
        }
    }
}

#[derive(Clone, Copy)]
enum LegacyKind {
    Gxl,
    Codegen,
    PluggingOz,
    PluggingLkb,
    PluggingGroovy,
}

struct LegacySolutionEncoder {
    kind: LegacyKind,
    scratch: Vec<u8>,
    encoded_names: Vec<Vec<u8>>,
    written: usize,
}

impl LegacySolutionEncoder {
    const fn new(kind: LegacyKind) -> Self {
        Self {
            kind,
            scratch: Vec::new(),
            encoded_names: Vec::new(),
            written: 0,
        }
    }
}

impl SolutionEncoder for LegacySolutionEncoder {
    fn begin(&mut self, output: &mut dyn Write) -> io::Result<()> {
        self.scratch.clear();
        self.encoded_names.clear();
        self.written = 0;
        match self.kind {
            LegacyKind::Gxl => output.write_all(
                b"<?xml version=\"1.0\"?>\n<gxl xmlns:xlink=\"http://www.w3.org/1999/xlink\">\n",
            ),
            LegacyKind::Codegen => output.write_all(b"class DomgraphGenerator {\n"),
            LegacyKind::PluggingOz | LegacyKind::PluggingGroovy => {
                output.write_all(b"%%  autogenerated by Utool\n[")
            }
            LegacyKind::PluggingLkb => output.write_all(b"("),
        }
    }

    fn write_solution(
        &mut self,
        solution: &Solution<'_>,
        output: &mut dyn Write,
    ) -> io::Result<()> {
        if self.written > 0 {
            match self.kind {
                LegacyKind::Gxl => output.write_all(b"\n")?,
                LegacyKind::PluggingGroovy => output.write_all(b",\n")?,
                _ => {}
            }
        }
        match self.kind {
            LegacyKind::Gxl => write_gxl_graph(&materialize_solution(solution)?, output)?,
            LegacyKind::Codegen => {
                write_codegen_graph(&materialize_solution(solution)?, self.written + 1, output)?;
            }
            LegacyKind::PluggingOz | LegacyKind::PluggingLkb | LegacyKind::PluggingGroovy => {
                let style = match self.kind {
                    LegacyKind::PluggingOz => PluggingStyle::Oz,
                    LegacyKind::PluggingLkb => PluggingStyle::Lkb,
                    LegacyKind::PluggingGroovy => PluggingStyle::Groovy,
                    LegacyKind::Gxl | LegacyKind::Codegen => unreachable!(),
                };
                if self.encoded_names.is_empty() {
                    self.encoded_names = solution
                        .graph()
                        .parsed()
                        .nodes()
                        .iter()
                        .map(|node| encode_plugging_name(node.name(), style))
                        .collect();
                }
                self.scratch.clear();
                append_solution_pluggings(
                    solution,
                    solution.root(),
                    style,
                    &self.encoded_names,
                    &mut self.scratch,
                );
                output.write_all(&self.scratch)?;
            }
        }
        self.written += 1;
        Ok(())
    }

    fn finish(&mut self, output: &mut dyn Write) -> io::Result<()> {
        match self.kind {
            LegacyKind::Gxl => output.write_all(b"</gxl>\n"),
            LegacyKind::Codegen => output.write_all(b"\n}\n"),
            LegacyKind::PluggingOz | LegacyKind::PluggingGroovy => output.write_all(b"]"),
            LegacyKind::PluggingLkb => output.write_all(b")"),
        }
    }
}

fn encode_plugging_name(value: &str, style: PluggingStyle) -> Vec<u8> {
    match style {
        PluggingStyle::Oz => encode_oz_atom(value),
        PluggingStyle::Lkb => value.get(1..).unwrap_or(value).as_bytes().to_vec(),
        PluggingStyle::Groovy => value.as_bytes().to_vec(),
    }
}

fn append_solution_pluggings(
    solution: &Solution<'_>,
    tree: Tree,
    style: PluggingStyle,
    encoded_names: &[Vec<u8>],
    output: &mut Vec<u8>,
) {
    match style {
        PluggingStyle::Oz => output.push(b'['),
        PluggingStyle::Lkb => output.extend_from_slice(b"( "),
        PluggingStyle::Groovy => output.extend_from_slice(b"[["),
    }
    let mut first = true;
    append_solution_plugging_edges(solution, tree, style, encoded_names, &mut first, output);
    match style {
        PluggingStyle::Oz => output.extend_from_slice(b"]\n"),
        PluggingStyle::Lkb => output.extend_from_slice(b")\n"),
        PluggingStyle::Groovy => output.extend_from_slice(b"],[:]]"),
    }
}

fn append_solution_plugging_edges(
    solution: &Solution<'_>,
    tree: Tree,
    style: PluggingStyle,
    encoded_names: &[Vec<u8>],
    first: &mut bool,
    output: &mut Vec<u8>,
) {
    let original = solution.graph().node(solution.node_id(tree));
    let resolved = solution.arena().get_children(tree);
    for (&source_child, &resolved_child) in original.tree_children().iter().zip(resolved) {
        let source_node = solution.graph().node(source_child);
        if source_node.is_hole() {
            if !*first {
                output.extend_from_slice(match style {
                    PluggingStyle::Groovy => b", ",
                    PluggingStyle::Oz | PluggingStyle::Lkb => b" ",
                });
            }
            *first = false;
            let source = &encoded_names[source_child.index()];
            let target = &encoded_names[solution.node_id(resolved_child).index()];
            match style {
                PluggingStyle::Oz => {
                    output.extend_from_slice(b"plug(");
                    output.extend_from_slice(source);
                    output.push(b' ');
                    output.extend_from_slice(target);
                    output.push(b')');
                }
                PluggingStyle::Lkb => {
                    output.push(b'(');
                    output.extend_from_slice(source);
                    output.push(b' ');
                    output.extend_from_slice(source);
                    output.push(b' ');
                    output.extend_from_slice(target);
                    output.extend_from_slice(b") (");
                    output.extend_from_slice(target);
                    output.push(b' ');
                    output.extend_from_slice(source);
                    output.push(b' ');
                    output.extend_from_slice(target);
                    output.push(b')');
                }
                PluggingStyle::Groovy => {
                    output.extend_from_slice(b"[\"");
                    output.extend_from_slice(source);
                    output.extend_from_slice(b"\", \"");
                    output.extend_from_slice(target);
                    output.extend_from_slice(b"\"]");
                }
            }
        }
        append_solution_plugging_edges(
            solution,
            resolved_child,
            style,
            encoded_names,
            first,
            output,
        );
    }
}

fn materialize_solution(solution: &Solution<'_>) -> io::Result<ParsedGraph> {
    fn visit(
        solution: &Solution<'_>,
        tree: Tree,
        builder: &mut crate::graph::GraphBuilder,
    ) -> io::Result<crate::graph::NodeId> {
        let node = builder.ensure_node(solution.node_name(tree));
        builder
            .set_label(node, solution.node_label(tree))
            .map_err(io::Error::other)?;
        for &child_tree in solution.arena().get_children(tree) {
            let child = visit(solution, child_tree, builder)?;
            builder.add_tree_edge(node, child);
        }
        Ok(node)
    }
    let mut builder = crate::graph::GraphBuilder::default();
    visit(solution, solution.root(), &mut builder)?;
    Ok(builder.finish())
}

fn solution_pluggings<'a>(solution: &'a Solution<'a>) -> Vec<(&'a str, &'a str)> {
    fn visit<'a>(solution: &'a Solution<'a>, tree: Tree, output: &mut Vec<(&'a str, &'a str)>) {
        let original = solution.graph().node(solution.node_id(tree));
        let resolved = solution.arena().get_children(tree);
        for (&source_child, &resolved_child) in original.tree_children().iter().zip(resolved) {
            let source = solution.graph().node(source_child);
            if source.is_hole() {
                output.push((source.name(), solution.node_name(resolved_child)));
            }
            visit(solution, resolved_child, output);
        }
    }
    let mut output = Vec::new();
    visit(solution, solution.root(), &mut output);
    output
}

#[derive(Default)]
struct DomconSolutionEncoder {
    stack: Vec<Tree>,
    scratch: Vec<u8>,
    encoded_names: Vec<Vec<u8>>,
    encoded_labels: Vec<Vec<u8>>,
    written: usize,
}

impl SolutionEncoder for DomconSolutionEncoder {
    fn begin(&mut self, output: &mut dyn Write) -> io::Result<()> {
        self.stack.clear();
        self.scratch.clear();
        self.encoded_names.clear();
        self.encoded_labels.clear();
        self.written = 0;
        output.write_all(b"%%  autogenerated by Utool\n[\n")
    }

    fn write_solution(
        &mut self,
        solution: &Solution<'_>,
        output: &mut dyn Write,
    ) -> io::Result<()> {
        if self.encoded_names.is_empty() {
            self.encoded_names = solution
                .graph()
                .parsed()
                .nodes()
                .iter()
                .map(|node| encode_oz_atom(node.name()))
                .collect();
            self.encoded_labels = solution
                .graph()
                .parsed()
                .nodes()
                .iter()
                .map(|node| node.label().map_or_else(Vec::new, encode_oz_atom))
                .collect();
        }

        self.scratch.clear();
        self.scratch.push(b'[');
        let root = solution.root();
        let mut first = true;
        append_solution_node(
            solution,
            root,
            &mut first,
            &self.encoded_names,
            &self.encoded_labels,
            &mut self.scratch,
        );
        self.stack.clear();
        self.stack.push(root);
        while let Some(tree) = self.stack.pop() {
            for &child in solution.arena().get_children(tree) {
                append_solution_node(
                    solution,
                    child,
                    &mut first,
                    &self.encoded_names,
                    &self.encoded_labels,
                    &mut self.scratch,
                );
                self.stack.push(child);
            }
        }
        self.scratch.extend_from_slice(b"]\n");
        output.write_all(&self.scratch)?;
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

fn encode_oz_atom(value: &str) -> Vec<u8> {
    let mut encoded = Vec::with_capacity(value.len() + 2);
    let mut characters = value.chars();
    let bare = characters
        .next()
        .is_some_and(|first| first.is_ascii_lowercase())
        && characters.all(|character| character.is_alphanumeric() || character == '_');
    if bare {
        encoded.extend_from_slice(value.as_bytes());
        return encoded;
    }

    encoded.push(b'\'');
    for character in value.chars() {
        if matches!(character, '\\' | '\'') {
            encoded.push(b'\\');
        }
        let mut bytes = [0; 4];
        encoded.extend_from_slice(character.encode_utf8(&mut bytes).as_bytes());
    }
    encoded.push(b'\'');
    encoded
}

fn append_solution_node(
    solution: &Solution<'_>,
    tree: Tree,
    first: &mut bool,
    encoded_names: &[Vec<u8>],
    encoded_labels: &[Vec<u8>],
    output: &mut Vec<u8>,
) {
    if *first {
        *first = false;
    } else {
        output.push(b' ');
    }
    let node = solution.node_id(tree).index();
    output.extend_from_slice(b"label(");
    output.extend_from_slice(&encoded_names[node]);
    output.push(b' ');
    output.extend_from_slice(&encoded_labels[node]);
    let children = solution.arena().get_children(tree);
    if !children.is_empty() {
        output.push(b'(');
        for (index, child) in children.iter().enumerate() {
            if index > 0 {
                output.push(b' ');
            }
            output.extend_from_slice(&encoded_names[solution.node_id(*child).index()]);
        }
        output.push(b')');
    }
    output.push(b')');
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
    scratch: Vec<u8>,
    written: bool,
}

impl TermSolutionEncoder {
    const fn prolog() -> Self {
        Self {
            argument_separator: ",",
            solution_separator: b",\n",
            scratch: Vec::new(),
            written: false,
        }
    }

    const fn oz() -> Self {
        Self {
            argument_separator: " ",
            solution_separator: b" \n",
            scratch: Vec::new(),
            written: false,
        }
    }
}

impl SolutionEncoder for TermSolutionEncoder {
    fn begin(&mut self, output: &mut dyn Write) -> io::Result<()> {
        self.scratch.clear();
        self.written = false;
        output.write_all(b"[")
    }

    fn write_solution(
        &mut self,
        solution: &Solution<'_>,
        output: &mut dyn Write,
    ) -> io::Result<()> {
        self.scratch.clear();
        if self.written {
            self.scratch.extend_from_slice(self.solution_separator);
        }
        append_label_term(
            solution,
            solution.root(),
            self.argument_separator.as_bytes(),
            &mut self.scratch,
        );
        output.write_all(&self.scratch)?;
        self.written = true;
        Ok(())
    }

    fn finish(&mut self, output: &mut dyn Write) -> io::Result<()> {
        output.write_all(b"]")
    }
}

fn append_label_term(solution: &Solution<'_>, tree: Tree, separator: &[u8], output: &mut Vec<u8>) {
    output.extend_from_slice(solution.node_label(tree).as_bytes());
    let children = solution.arena().get_children(tree);
    if !children.is_empty() {
        output.push(b'(');
        for (index, child) in children.iter().enumerate() {
            if index > 0 {
                output.extend_from_slice(separator);
            }
            append_label_term(solution, *child, separator, output);
        }
        output.push(b')');
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
