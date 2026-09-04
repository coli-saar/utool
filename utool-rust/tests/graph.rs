use std::collections::BTreeSet;
use utool::{GraphBuilder, GraphError, HncGraph, ParsedGraph, parse_domcon_oz};

fn names(graph: &ParsedGraph, component: &[utool::NodeId]) -> BTreeSet<String> {
    component
        .iter()
        .map(|node| graph.node(*node).name().to_owned())
        .collect()
}

#[test]
fn ports_weak_component_cases() {
    let graph =
        parse_domcon_oz("[label(a f(b c)) dom(b d) dom(e a) label(c g(f)) dom(h g)]").unwrap();
    let components: BTreeSet<_> = graph
        .weakly_connected_components()
        .iter()
        .map(|component| names(&graph, component))
        .collect();
    assert_eq!(components.len(), 2);
    assert!(components.contains(&BTreeSet::from([
        "a".to_owned(),
        "b".to_owned(),
        "c".to_owned(),
        "d".to_owned(),
        "e".to_owned(),
        "f".to_owned(),
    ])));
    assert!(components.contains(&BTreeSet::from(["g".to_owned(), "h".to_owned()])));
}

#[test]
fn rejects_multiple_tree_parents() {
    let mut builder = GraphBuilder::default();
    let x = builder.ensure_node("x");
    let y = builder.ensure_node("y");
    let z = builder.ensure_node("z");
    builder.set_label(x, "f").unwrap();
    builder.set_label(y, "g").unwrap();
    builder.add_tree_edge(x, z);
    builder.add_tree_edge(y, z);
    assert!(matches!(
        HncGraph::try_from(builder.finish()),
        Err(GraphError::MultipleTreeParents { .. })
    ));
}

#[test]
fn ports_hnc_and_non_hnc_examples() {
    let hnc = parse_domcon_oz(
        "[label(n0 f(n1 n2)) label(n3 a) label(n4 b) dom(n1 n3) dom(n2 n4) dom(n1 n4)]",
    )
    .unwrap();
    assert!(HncGraph::try_from(hnc).is_ok());

    let not_hnc = parse_domcon_oz(
        "[label(n0 f(n1 n2 n2b)) label(n3 a) label(n4 b) label(n5 c) dom(n1 n3) dom(n1 n4) dom(n2 n5) dom(n2b n5)]",
    )
    .unwrap();
    assert_eq!(
        HncGraph::try_from(not_hnc).unwrap_err(),
        GraphError::NotHypernormallyConnected
    );
}

#[test]
fn empty_graph_is_hnc() {
    assert!(HncGraph::try_from(ParsedGraph::default()).is_ok());
}
