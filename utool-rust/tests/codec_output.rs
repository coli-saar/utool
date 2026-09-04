use utool::{HncGraph, InputCodec, encode_domcon_oz, encode_dot, parse_domcon_oz};

#[test]
fn domcon_output_round_trips_graph_semantics() {
    let input = "[label(x f(x1)) label(y a) dom(x1 y)]";
    let graph = HncGraph::try_from(parse_domcon_oz(input).unwrap()).unwrap();
    let encoded = encode_domcon_oz(graph.parsed());
    assert_eq!(
        HncGraph::try_from(parse_domcon_oz(&encoded).unwrap()).unwrap(),
        graph
    );
}

#[test]
fn registry_and_dot_export_are_stable() {
    assert_eq!(
        InputCodec::from_filename("example.PL"),
        Some(InputCodec::HoleSemantics)
    );
    assert_eq!(
        InputCodec::from_filename("example.clls"),
        Some(InputCodec::DomconOz)
    );
    let graph = parse_domcon_oz("[label(x a)]").unwrap();
    assert!(encode_dot(&graph).contains("\"x\" [label=\"a\"]"));
}
