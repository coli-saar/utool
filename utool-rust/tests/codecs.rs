use utool::{
    CodecError, GraphError, HncGraph, InputCodec, parse_chain, parse_domcon_oz, parse_holesem,
    solve,
};

#[test]
fn input_registry_resolves_every_name_alias_and_suffix() {
    let names = [
        ("domcon-oz", InputCodec::DomconOz),
        ("domcon", InputCodec::DomconOz),
        ("holesem-comsem", InputCodec::HoleSemantics),
        ("holesem", InputCodec::HoleSemantics),
        ("chain", InputCodec::Chain),
    ];
    for (name, expected) in names {
        assert_eq!(InputCodec::from_name(name), Some(expected), "{name}");
    }
    assert_eq!(InputCodec::from_name("unknown"), None);
    assert_eq!(InputCodec::from_name("DOMCON-OZ"), None);

    let suffixes = [
        ("graph.CLLS", InputCodec::DomconOz),
        ("graph.domcon", InputCodec::DomconOz),
        ("graph.oz", InputCodec::DomconOz),
        ("graph.txt", InputCodec::DomconOz),
        ("graph.PL", InputCodec::HoleSemantics),
        ("graph.holesem", InputCodec::HoleSemantics),
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

    assert_eq!(InputCodec::DomconOz.name(), "domcon-oz");
    assert_eq!(InputCodec::HoleSemantics.name(), "holesem-comsem");
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
    assert_eq!(InputCodec::Chain.parse("1").unwrap().nodes().len(), 5);
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
