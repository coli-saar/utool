use utool::{HncGraph, parse_chain, parse_domcon_oz, parse_holesem, solve};

#[test]
fn ports_domcon_chain_and_quotes() {
    let graph =
        parse_domcon_oz("% comment\n[label('x node' 'pick\\'up'(h)) dom(h y) label(y a)]").unwrap();
    let x = graph.node_id("x node").unwrap();
    assert_eq!(graph.node(x).label(), Some("pick'up"));
    assert_eq!(graph.node(x).tree_children().len(), 1);
    assert!(HncGraph::try_from(graph).is_ok());
}

#[test]
fn ports_the_java_pure_chain_generator() {
    let graph = HncGraph::try_from(parse_chain("3").unwrap()).unwrap();
    assert_eq!(graph.parsed().nodes().len(), 13);
    assert_eq!(graph.parsed().dominance_edges().len(), 6);
    assert_eq!(solve(&graph).unwrap().count_solutions(), 5_u8.into());
    assert!(parse_chain("0").is_err());
    assert!(parse_chain("three").is_err());
}

#[test]
fn parses_empty_domcon_graph() {
    assert!(parse_domcon_oz("[]").unwrap().nodes().is_empty());
}

#[test]
fn hole_semantics_ll5_checkpoint_lowers_graph() {
    let graph = parse_holesem(
        "some(_Top,and(hole(_Top),some(_L,and(label(_L),and(pred1(_L,foo,X),leq(_L,_Top))))))",
    )
    .unwrap();
    assert!(graph.nodes().iter().any(|node| node.label() == Some("foo")));
    assert!(graph.nodes().iter().any(|node| node.label() == Some("X")));
    assert!(HncGraph::try_from(graph).is_ok());
}

#[test]
fn hole_semantics_reports_syntax_errors() {
    assert!(parse_holesem("some(_A,and(hole(_A))").is_err());
}
