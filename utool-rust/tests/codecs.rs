use utool::{
    CodecError, GraphError, HncGraph, InputCodec, parse_chain, parse_domcon_oz, parse_domgraph_gxl,
    parse_holesem, parse_mrs_prolog, parse_mrs_xml, solve,
};

#[test]
fn input_registry_resolves_every_name_alias_and_suffix() {
    assert_eq!(
        InputCodec::ALL,
        [
            InputCodec::Chain,
            InputCodec::DomconOz,
            InputCodec::DomgraphGxl,
            InputCodec::HoleSemantics,
            InputCodec::MrsProlog,
            InputCodec::MrsXml,
        ]
    );
    let names = [
        ("domcon-oz", InputCodec::DomconOz),
        ("domcon", InputCodec::DomconOz),
        ("domgraph-gxl", InputCodec::DomgraphGxl),
        ("gxl", InputCodec::DomgraphGxl),
        ("holesem-comsem", InputCodec::HoleSemantics),
        ("holesem", InputCodec::HoleSemantics),
        ("mrs-prolog", InputCodec::MrsProlog),
        ("mrs-xml", InputCodec::MrsXml),
        ("chain", InputCodec::Chain),
    ];
    for (name, expected) in names {
        assert_eq!(InputCodec::from_name(name), Some(expected), "{name}");
    }
    assert_eq!(InputCodec::from_name("unknown"), None);
    assert_eq!(InputCodec::from_name("DOMCON-OZ"), None);

    let suffixes = [
        ("graph.CLLS", InputCodec::DomconOz),
        ("graph.DG.XML", InputCodec::DomgraphGxl),
        ("graph.HS.PL", InputCodec::HoleSemantics),
        ("graph.MRS.PL", InputCodec::MrsProlog),
        ("graph.MRS.XML", InputCodec::MrsXml),
    ];
    for (filename, expected) in suffixes {
        assert_eq!(
            InputCodec::from_filename(filename),
            Some(expected),
            "{filename}"
        );
    }
    assert_eq!(InputCodec::from_filename("graph"), None);
    assert_eq!(InputCodec::from_filename("graph.json"), None);
    for ambiguous in [
        "graph.pl",
        "graph.xml",
        "graph.oz",
        "graph.txt",
        "graph.domcon",
    ] {
        assert_eq!(InputCodec::from_filename(ambiguous), None, "{ambiguous}");
    }

    assert_eq!(InputCodec::DomconOz.name(), "domcon-oz");
    assert_eq!(InputCodec::DomgraphGxl.name(), "domgraph-gxl");
    assert_eq!(InputCodec::HoleSemantics.name(), "holesem-comsem");
    assert_eq!(InputCodec::MrsProlog.name(), "mrs-prolog");
    assert_eq!(InputCodec::MrsXml.name(), "mrs-xml");
    assert_eq!(InputCodec::Chain.name(), "chain");
}

#[test]
fn input_registry_dispatches_to_every_parser() {
    assert!(InputCodec::DomconOz.parse("[]").unwrap().nodes().is_empty());
    assert!(
        InputCodec::HoleSemantics
            .parse("some(A,hole(A))")
            .unwrap()
            .nodes()
            .is_empty()
    );
    assert_eq!(
        InputCodec::MrsProlog
            .parse("psoa(h1,e2,[rel('rain',h3,[attrval('ARG0',e2)])],hcons([qeq(h1,h3)]))")
            .unwrap()
            .nodes()
            .len(),
        1
    );
    assert_eq!(InputCodec::MrsXml.parse("<mrs><var vid=\"h1\"/><ep><pred>rain</pred><var vid=\"h3\"/><fvpair><rargname>ARG0</rargname><var vid=\"e2\"/></fvpair></ep><hcons><hi><var vid=\"h1\"/></hi><lo><var vid=\"h3\"/></lo></hcons></mrs>").unwrap().nodes().len(), 1);
    assert_eq!(InputCodec::DomgraphGxl.parse("<gxl xmlns:xlink=\"x\"><graph><node id=\"x\"><type xlink:href=\"leaf\"/><attr name=\"label\"><string>a</string></attr></node></graph></gxl>").unwrap().nodes().len(), 1);
    assert_eq!(InputCodec::Chain.parse("1").unwrap().nodes().len(), 5);
}

#[test]
fn mrs_prolog_matches_java_reference_and_all_repository_examples_are_hnc() {
    let stefan_mrs = r"psoa(h1,e2,
[
 rel('proper_q',h3,
     [ attrval('ARG0',x4),
       attrval('RSTR',h5),
       attrval('BODY',h6)]),
 rel('named_rel',h7,
     [ attrval('ARG0',x4),
       attrval('NAME','Aicke')]),
 rel('proper_q',h8,
     [ attrval('ARG0',x9),
       attrval('RSTR',h10),
       attrval('BODY',h11)]),
 rel('named_rel',h12,
     [ attrval('ARG0',x9),
       attrval('NAME','Aicke')]),
 rel('kennen_rel',h13,
     [ attrval('ARG0',e2),
       attrval('ARG1',x4),
       attrval('ARG2',x9)])],
 hcons([
 qeq(h5,h7),
 qeq(h10,h12)
 ]))";
    let input = stefan_mrs;
    let expected = parse_domcon_oz("[label(h3 proper_q(h5 h6)) label(h7 named_rel) label(h8 proper_q(h10 h11)) label(h12 named_rel) label(h13 kennen_rel) dom(h5 h7) dom(h10 h12) dom(h11 h13) dom(h6 h13)]").unwrap();
    assert_graph_equivalent(&parse_mrs_prolog(input).unwrap(), &expected);

    for input in [
        include_str!("../../src/main/resources/examples/rondane-1.mrs.pl"),
        include_str!("../../src/main/resources/examples/rondane-1262.mrs.pl"),
        include_str!("../../src/main/resources/examples/rondane-1409.mrs.pl"),
        include_str!("../../src/main/resources/examples/rondane-650.mrs.pl"),
        include_str!("../../src/main/resources/examples/rondane-892.mrs.pl"),
        stefan_mrs,
        include_str!("fixtures/rademaker.mrs.pl"),
    ] {
        assert!(HncGraph::try_from(parse_mrs_prolog(input).unwrap()).is_ok());
    }
}

#[test]
fn malformed_mrs_prolog_identifies_expectation_and_source_location() {
    let error =
        parse_mrs_prolog("psoa(h1,e2,\n[ rel('rain_rel',h3 [ attrval('ARG0',e2)]) ],\nhcons([]))")
            .unwrap_err()
            .to_string();

    assert!(error.contains("expected punctuation ','"), "{error}");
    assert!(error.contains("line 2, column"), "{error}");
    assert!(error.contains("rel('rain_rel',h3 ["), "{error}");
    assert!(error.contains('^'), "{error}");
}

#[test]
fn generated_parsers_identify_unexpected_text_and_expected_syntax() {
    for (codec, expected) in [
        (InputCodec::DomconOz, "expected one of: '['"),
        (InputCodec::HoleSemantics, "expected one of: '('"),
    ] {
        let error = codec.parse("not a graph").unwrap_err().to_string();
        assert!(error.contains("unexpected \"not\""), "{codec:?}: {error}");
        assert!(error.contains(expected), "{codec:?}: {error}");
        assert!(error.contains("line 1, column 1"), "{codec:?}: {error}");
        assert!(error.contains("not a graph"), "{codec:?}: {error}");
        assert!(error.contains('^'), "{codec:?}: {error}");
    }
}

#[test]
fn malformed_xml_identifies_line_and_offending_input() {
    for codec in [InputCodec::MrsXml, InputCodec::DomgraphGxl] {
        let error = codec
            .parse("<root>\n  <unexpected>\n</root>")
            .unwrap_err()
            .to_string();
        assert!(error.contains("line 3, column"), "{codec:?}: {error}");
        assert!(error.contains("Offending input"), "{codec:?}: {error}");
        assert!(error.contains("</root>"), "{codec:?}: {error}");
        assert!(!error.contains("near byte"), "{codec:?}: {error}");
    }
}

fn assert_graph_equivalent(actual: &utool::ParsedGraph, expected: &utool::ParsedGraph) {
    let describe = |graph: &utool::ParsedGraph| {
        let nodes: std::collections::BTreeSet<_> = graph
            .nodes()
            .iter()
            .map(|node| {
                (
                    node.name().to_owned(),
                    node.label().map(str::to_owned),
                    node.tree_children()
                        .iter()
                        .map(|child| graph.node(*child).name().to_owned())
                        .collect::<Vec<_>>(),
                )
            })
            .collect();
        let dominance: std::collections::BTreeSet<_> = graph
            .dominance_edges()
            .iter()
            .map(|&(source, target)| {
                (
                    graph.node(source).name().to_owned(),
                    graph.node(target).name().to_owned(),
                )
            })
            .collect();
        (nodes, dominance)
    };
    assert_eq!(describe(actual), describe(expected));
}

#[test]
fn mrs_xml_and_gxl_decode_entities_and_match_equivalent_domcon() {
    let mrs = "<mrs><var vid=\"h1\"/><ep><pred>a&amp;b</pred><var vid=\"h3\"/><fvpair><rargname>ARG0</rargname><var vid=\"e2\"/></fvpair></ep><hcons><hi><var vid=\"h1\"/></hi><lo><var vid=\"h3\"/></lo></hcons></mrs>";
    assert_eq!(
        parse_mrs_xml(mrs).unwrap(),
        parse_domcon_oz("[label(h3 'a&b')]").unwrap()
    );

    let gxl = "<gxl xmlns:xlink=\"x\"><graph><node id=\"r\"><type xlink:href=\"root\"/><attr name=\"label\"><string>a&amp;b</string></attr></node><node id=\"h\"><type xlink:href=\"hole\"/></node><node id=\"x\"><type xlink:href=\"leaf\"/><attr name=\"label\"><string>c</string></attr></node><edge from=\"r\" to=\"h\"><type xlink:href=\"solid\"/></edge><edge from=\"h\" to=\"x\"><type xlink:href=\"dominance\"/></edge></graph></gxl>";
    assert_eq!(
        parse_domgraph_gxl(gxl).unwrap(),
        parse_domcon_oz("[label(r 'a&b'(h)) label(x c) dom(h x)]").unwrap()
    );
}

#[test]
fn domcon_parses_comments_ordered_children_dominance_and_quoted_atoms() {
    let graph = parse_domcon_oz(
        "% comment\n[label('x node' 'pick\\'up'(left right)) dom(left lower) label(lower a)]",
    )
    .unwrap();
    let root = graph.node_id("x node").unwrap();
    let children = graph.node(root).tree_children();
    assert_eq!(graph.node(root).label(), Some("pick'up"));
    assert_eq!(graph.node(children[0]).name(), "left");
    assert_eq!(graph.node(children[1]).name(), "right");
    assert_eq!(
        graph.dominance_edges(),
        &[(
            graph.node_id("left").unwrap(),
            graph.node_id("lower").unwrap()
        )]
    );
}

#[test]
fn domcon_accepts_empty_input_and_reports_syntax_and_graph_errors() {
    assert!(parse_domcon_oz("[]").unwrap().nodes().is_empty());
    assert!(matches!(
        parse_domcon_oz("[label(x)]"),
        Err(CodecError::Syntax(_))
    ));
    assert!(matches!(
        parse_domcon_oz("[label(x a) label(x b)]"),
        Err(CodecError::Graph(GraphError::ConflictingLabel { .. }))
    ));
}

#[test]
fn chain_codec_matches_the_java_generator_for_small_lengths() {
    for (length, nodes, dominance, solutions) in [(1, 5, 2, 1_u32), (2, 9, 4, 2), (3, 13, 6, 5)] {
        let graph = HncGraph::try_from(parse_chain(&length.to_string()).unwrap()).unwrap();
        assert_eq!(graph.parsed().nodes().len(), nodes);
        assert_eq!(graph.parsed().dominance_edges().len(), dominance);
        assert_eq!(solve(&graph).unwrap().count_solutions(), solutions.into());
    }
}

#[test]
fn chain_codec_rejects_every_invalid_length_class() {
    assert!(matches!(parse_chain("0"), Err(CodecError::Semantic(_))));
    for input in [
        "",
        "-1",
        "1.5",
        "three",
        "999999999999999999999999999999999",
    ] {
        assert!(
            matches!(parse_chain(input), Err(CodecError::Syntax(_))),
            "{input:?}"
        );
    }
}

#[test]
fn hole_semantics_lowers_predicates_and_anonymous_values() {
    let graph = parse_holesem(
        r"some(A,some(B,and(label(A),and(label(B),and(pred2(A,'give\'event',B,john),pred1(B,'path\\person',mary))))))",
    )
    .unwrap();
    let a = graph.node_id("A").unwrap();
    let b = graph.node_id("B").unwrap();
    assert_eq!(graph.node(a).label(), Some("give'event"));
    assert_eq!(graph.node(a).tree_children().len(), 2);
    assert_eq!(graph.node(a).tree_children()[0], b);
    assert_eq!(graph.node(b).label(), Some("path\\person"));
    assert!(
        graph
            .nodes()
            .iter()
            .any(|node| node.label() == Some("john"))
    );
    assert!(
        graph
            .nodes()
            .iter()
            .any(|node| node.label() == Some("mary"))
    );
}

#[test]
fn hole_semantics_lowers_every_logical_constructor() {
    for constructor in ["and", "or", "imp", "not", "all", "some", "que", "eq"] {
        let input = format!("some(A,and(label(A),{constructor}(A,x,y)))");
        let graph = parse_holesem(&input).unwrap();
        let root = graph.node_id("A").unwrap();
        assert_eq!(graph.node(root).label(), Some(constructor), "{constructor}");
        assert_eq!(graph.node(root).tree_children().len(), 2, "{constructor}");
    }
}

#[test]
fn hole_semantics_normalizes_dominance_targets_to_fragment_roots() {
    let graph = parse_holesem(
        "some(A,some(C,some(H,some(X,and(label(A),and(label(C),and(hole(H),and(hole(X),and(pred1(A,q,H),and(pred1(C,p,X),leq(X,H)))))))))))",
    )
    .unwrap();
    assert_eq!(
        graph.dominance_edges(),
        &[(graph.node_id("H").unwrap(), graph.node_id("C").unwrap())]
    );
}

#[test]
fn hole_semantics_removes_one_empty_top_fragment() {
    assert!(parse_holesem("some(A,hole(A))").unwrap().nodes().is_empty());
}

#[test]
fn hole_semantics_rejects_invalid_syntax_and_empty_fragment_semantics() {
    assert!(matches!(
        parse_holesem("some(A,and(hole(A))"),
        Err(CodecError::Syntax(_))
    ));
    assert!(matches!(
        parse_holesem("some(A,some(B,and(hole(A),hole(B))))"),
        Err(CodecError::Semantic(message)) if message == "multiple empty top fragments"
    ));
    let nontrivial =
        parse_holesem("some(A,some(B,and(hole(A),and(label(B),and(pred1(B,p,x),leq(A,B))))))");
    assert!(
        matches!(
            nontrivial,
            Err(CodecError::Semantic(ref message)) if message == "nontrivial empty fragment"
        ),
        "{nontrivial:?}"
    );
}

#[test]
fn hole_semantics_checkpoint_is_a_valid_hnc_graph() {
    let graph = parse_holesem(
        "some(_Top,and(hole(_Top),some(_L,and(label(_L),and(pred1(_L,foo,X),leq(_L,_Top))))))",
    )
    .unwrap();
    assert!(graph.nodes().iter().any(|node| node.label() == Some("foo")));
    assert!(graph.nodes().iter().any(|node| node.label() == Some("X")));
    assert!(HncGraph::try_from(graph).is_ok());
}
