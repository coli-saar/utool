use std::io::{self, Write};
use utool::{
    HncGraph, OutputCodec, SolutionEncoder, encode_domcon_oz, encode_dot, parse_chain,
    parse_domcon_oz, solve,
};

#[test]
fn output_registry_resolves_every_name_alias_and_suffix() {
    let names = [
        ("domcon-oz", OutputCodec::DomconOz),
        ("domcon", OutputCodec::DomconOz),
        ("domgraph-dot", OutputCodec::DomgraphDot),
        ("dot", OutputCodec::DomgraphDot),
        ("domgraph-gxl", OutputCodec::DomgraphGxl),
        ("gxl", OutputCodec::DomgraphGxl),
        ("domgraph-udraw", OutputCodec::DomgraphUdraw),
        ("udraw", OutputCodec::DomgraphUdraw),
        ("domgraph-codegen", OutputCodec::DomgraphCodegen),
        ("codegen", OutputCodec::DomgraphCodegen),
        ("plugging-oz", OutputCodec::PluggingOz),
        ("plugging-lkb", OutputCodec::PluggingLkb),
        ("plugging-groovy", OutputCodec::PluggingGroovy),
        ("term-prolog", OutputCodec::TermProlog),
        ("term-oz", OutputCodec::TermOz),
    ];
    for (name, expected) in names {
        assert_eq!(OutputCodec::from_name(name), Some(expected), "{name}");
    }
    assert_eq!(OutputCodec::from_name("unknown"), None);
    assert_eq!(OutputCodec::from_name("TERM-OZ"), None);

    let suffixes = [
        ("result.CLLS", OutputCodec::DomconOz),
        ("result.DG.DOT", OutputCodec::DomgraphDot),
        ("result.DG.XML", OutputCodec::DomgraphGxl),
        ("result.DG.UDG", OutputCodec::DomgraphUdraw),
        ("result.PLUG.OZ", OutputCodec::PluggingOz),
        ("result.LKBPLUG.LISP", OutputCodec::PluggingLkb),
        ("result.T.PL", OutputCodec::TermProlog),
        ("result.T.OZ", OutputCodec::TermOz),
        ("result.JAVA", OutputCodec::DomgraphCodegen),
    ];
    for (filename, expected) in suffixes {
        assert_eq!(
            OutputCodec::from_filename(filename),
            Some(expected),
            "{filename}"
        );
    }
    for filename in [
        "result",
        "result.dot",
        "result.pl",
        "result.oz",
        "result.json",
    ] {
        assert_eq!(OutputCodec::from_filename(filename), None, "{filename}");
    }
}

#[test]
fn output_registry_reports_all_names_and_capabilities() {
    let cases = [
        (OutputCodec::DomconOz, "domcon-oz", true, true),
        (OutputCodec::DomgraphDot, "domgraph-dot", true, false),
        (OutputCodec::DomgraphGxl, "domgraph-gxl", true, true),
        (OutputCodec::DomgraphUdraw, "domgraph-udraw", true, false),
        (OutputCodec::DomgraphCodegen, "domgraph-codegen", true, true),
        (OutputCodec::PluggingOz, "plugging-oz", true, true),
        (OutputCodec::PluggingLkb, "plugging-lkb", true, true),
        (OutputCodec::PluggingGroovy, "plugging-groovy", true, true),
        (OutputCodec::TermProlog, "term-prolog", false, true),
        (OutputCodec::TermOz, "term-oz", false, true),
    ];
    for (codec, name, graph, solutions) in cases {
        assert_eq!(codec.name(), name);
        assert_eq!(codec.supports_graph(), graph);
        assert_eq!(codec.supports_solutions(), solutions);
        assert_eq!(codec.graph_encoder().is_some(), graph);
        assert_eq!(codec.solution_encoder().is_some(), solutions);
    }
}

#[test]
fn legacy_graph_outputs_match_java_shapes_and_gxl_round_trips() {
    let graph = parse_domcon_oz("[label(x f(h1)) label(h2 'a&b') dom(h1 h2)]").unwrap();
    let encode = |codec: OutputCodec| {
        let mut output = Vec::new();
        codec
            .graph_encoder()
            .unwrap()
            .write_graph(&graph, &mut output)
            .unwrap();
        String::from_utf8(output).unwrap()
    };
    let gxl = encode(OutputCodec::DomgraphGxl);
    assert_eq!(utool::parse_domgraph_gxl(&gxl).unwrap(), graph);
    assert!(gxl.contains("a&amp;b"));
    let udraw = encode(OutputCodec::DomgraphUdraw);
    assert!(udraw.contains("EDGEPATTERN\",\"solid"));
    assert!(udraw.contains("EDGEPATTERN\",\"dotted"));
    assert_eq!(encode(OutputCodec::PluggingOz), "[plug(h1 h2)]\n");
    assert_eq!(encode(OutputCodec::PluggingLkb), "( (1 1 2) (2 1 2))\n");
    assert_eq!(
        encode(OutputCodec::PluggingGroovy),
        "[[[\"h1\", \"h2\"]],[:]]"
    );
    let code = encode(OutputCodec::DomgraphCodegen);
    assert!(code.contains("graph.addNode(\"x\", new NodeData(NodeType.LABELLED));"));
    assert!(code.contains("new EdgeData(EdgeType.DOMINANCE)"));
}

#[test]
fn domcon_graph_output_is_canonical_and_matches_buffering_wrapper() {
    let graph = parse_domcon_oz("[dom(h y) label(x f(h z)) label(y a)]").unwrap();
    let expected = "[label(y a) label(x f(h z)) dom(h y)]";
    assert_eq!(encode_domcon_oz(&graph), expected);

    let mut streamed = Vec::new();
    OutputCodec::DomconOz
        .graph_encoder()
        .unwrap()
        .write_graph(&graph, &mut streamed)
        .unwrap();
    assert_eq!(streamed, expected.as_bytes());
}

#[test]
fn domcon_graph_output_round_trips_empty_and_nontrivial_graphs() {
    for input in [
        "[]",
        "[label(x f(x1 x2)) label(y a) dom(x1 y) dom(x2 z)]",
        "[label('x node' 'pick\\'up'('left\\\\branch')) dom('left\\\\branch' lower)]",
    ] {
        let graph = parse_domcon_oz(input).unwrap();
        let encoded = encode_domcon_oz(&graph);
        assert_eq!(parse_domcon_oz(&encoded).unwrap(), graph, "{encoded}");
    }
}

#[test]
fn dot_graph_output_is_exact_escaped_and_matches_buffering_wrapper() {
    let graph =
        parse_domcon_oz(r#"[label('x "node"' 'path\\label'(child)) dom(child 'x "node"')]"#)
            .unwrap();
    let expected = concat!(
        "digraph dominance_graph {\n",
        "  \"x \\\"node\\\"\" [label=\"path\\\\label\"];\n",
        "  \"x \\\"node\\\"\" -> \"child\" [style=solid];\n",
        "  \"child\" [label=\"child\"];\n",
        "  \"child\" -> \"x \\\"node\\\"\" [style=dotted];\n",
        "}\n",
    );
    assert_eq!(encode_dot(&graph), expected);

    let mut streamed = Vec::new();
    OutputCodec::DomgraphDot
        .graph_encoder()
        .unwrap()
        .write_graph(&graph, &mut streamed)
        .unwrap();
    assert_eq!(streamed, expected.as_bytes());
}

#[test]
fn every_solution_codec_encodes_an_empty_sequence() {
    assert_eq!(encode_no_solutions(OutputCodec::TermProlog), b"[]");
    assert_eq!(encode_no_solutions(OutputCodec::TermOz), b"[]");
    assert_eq!(
        encode_no_solutions(OutputCodec::DomconOz),
        b"%%  autogenerated by Utool\n[\n\n]\n"
    );
}

#[test]
fn every_solution_codec_encodes_one_solution_exactly() {
    assert_eq!(encode_chain(OutputCodec::TermProlog, 1), b"[f1(a0,a1)]");
    assert_eq!(encode_chain(OutputCodec::TermOz, 1), b"[f1(a0 a1)]");
    assert_eq!(
        encode_chain(OutputCodec::DomconOz, 1),
        b"%%  autogenerated by Utool\n[\n[label(x1 f1(y0 y1)) label(y0 a0) label(y1 a1)]\n]\n"
    );
}

#[test]
fn term_solution_codecs_delimit_multiple_solutions_exactly() {
    assert_eq!(
        encode_chain(OutputCodec::TermProlog, 2),
        b"[f1(a0,f2(a1,a2)),\nf2(f1(a0,a1),a2)]"
    );
    assert_eq!(
        encode_chain(OutputCodec::TermOz, 2),
        b"[f1(a0 f2(a1 a2)) \nf2(f1(a0 a1) a2)]"
    );
}

#[test]
fn domcon_solution_codec_delimits_multiple_solutions_exactly() {
    assert_eq!(
        encode_chain(OutputCodec::DomconOz, 2),
        concat!(
            "%%  autogenerated by Utool\n",
            "[\n",
            "[label(x1 f1(y0 x2)) label(y0 a0) label(x2 f2(y1 y2)) label(y1 a1) label(y2 a2)]\n",
            "[label(x2 f2(x1 y2)) label(x1 f1(y0 y1)) label(y2 a2) label(y0 a0) label(y1 a1)]\n",
            "]\n",
        )
        .as_bytes()
    );
}

#[test]
fn solution_encoder_can_be_reused_after_begin_resets_its_state() {
    for codec in [
        OutputCodec::DomconOz,
        OutputCodec::DomgraphGxl,
        OutputCodec::DomgraphCodegen,
        OutputCodec::PluggingOz,
        OutputCodec::PluggingLkb,
        OutputCodec::PluggingGroovy,
        OutputCodec::TermProlog,
        OutputCodec::TermOz,
    ] {
        let mut encoder = codec.solution_encoder().unwrap();
        let first = encode_chain_with(&mut *encoder, 1);
        assert!(!first.is_empty());

        let mut empty = Vec::new();
        encoder.begin(&mut empty).unwrap();
        encoder.finish(&mut empty).unwrap();
        assert_eq!(empty, encode_no_solutions(codec), "{}", codec.name());
    }
}

#[test]
fn graph_codecs_propagate_writer_errors() {
    let graph = parse_domcon_oz("[label(x a)]").unwrap();
    for codec in [
        OutputCodec::DomconOz,
        OutputCodec::DomgraphDot,
        OutputCodec::DomgraphGxl,
        OutputCodec::DomgraphUdraw,
        OutputCodec::DomgraphCodegen,
        OutputCodec::PluggingOz,
        OutputCodec::PluggingLkb,
        OutputCodec::PluggingGroovy,
    ] {
        let error = codec
            .graph_encoder()
            .unwrap()
            .write_graph(&graph, &mut FailingWriter)
            .unwrap_err();
        assert_eq!(error.kind(), io::ErrorKind::Other, "{}", codec.name());
    }
}

#[test]
fn solution_codecs_propagate_errors_from_every_lifecycle_method() {
    for codec in [
        OutputCodec::DomconOz,
        OutputCodec::DomgraphGxl,
        OutputCodec::DomgraphCodegen,
        OutputCodec::PluggingOz,
        OutputCodec::PluggingLkb,
        OutputCodec::PluggingGroovy,
        OutputCodec::TermProlog,
        OutputCodec::TermOz,
    ] {
        let mut encoder = codec.solution_encoder().unwrap();
        assert_eq!(
            encoder.begin(&mut FailingWriter).unwrap_err().kind(),
            io::ErrorKind::Other
        );

        let graph = HncGraph::try_from(parse_chain("1").unwrap()).unwrap();
        let chart = solve(&graph).unwrap();
        let mut solutions = chart.solutions();
        assert!(solutions.advance());
        assert_eq!(
            encoder
                .write_solution(&solutions.current().unwrap(), &mut FailingWriter)
                .unwrap_err()
                .kind(),
            io::ErrorKind::Other
        );
        assert_eq!(
            encoder.finish(&mut FailingWriter).unwrap_err().kind(),
            io::ErrorKind::Other
        );
    }
}

fn encode_no_solutions(codec: OutputCodec) -> Vec<u8> {
    let mut encoder = codec.solution_encoder().unwrap();
    let mut output = Vec::new();
    encoder.begin(&mut output).unwrap();
    encoder.finish(&mut output).unwrap();
    output
}

fn encode_chain(codec: OutputCodec, length: usize) -> Vec<u8> {
    let mut encoder = codec.solution_encoder().unwrap();
    encode_chain_with(&mut *encoder, length)
}

fn encode_chain_with(encoder: &mut dyn SolutionEncoder, length: usize) -> Vec<u8> {
    let graph = HncGraph::try_from(parse_chain(&length.to_string()).unwrap()).unwrap();
    let chart = solve(&graph).unwrap();
    let mut solutions = chart.solutions();
    let mut output = Vec::new();
    encoder.begin(&mut output).unwrap();
    while solutions.advance() {
        encoder
            .write_solution(&solutions.current().unwrap(), &mut output)
            .unwrap();
    }
    encoder.finish(&mut output).unwrap();
    output
}

struct FailingWriter;

impl Write for FailingWriter {
    fn write(&mut self, _buffer: &[u8]) -> io::Result<usize> {
        Err(io::Error::other("deliberate test failure"))
    }

    fn flush(&mut self) -> io::Result<()> {
        Err(io::Error::other("deliberate test failure"))
    }
}
