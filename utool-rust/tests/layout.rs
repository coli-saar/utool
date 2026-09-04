use std::collections::HashSet;
use utool::{EdgeKind, HncGraph, LayoutOptions, Size, layout_graph, parse_domcon_oz};

#[test]
fn lays_out_fragments_without_node_overlap() {
    let graph = HncGraph::try_from(
        parse_domcon_oz("[label(x f(x1 x2)) dom(x1 y) label(y a) dom(x2 z) label(z b)]").unwrap(),
    )
    .unwrap();
    let sizes: Vec<_> = (0..graph.parsed().nodes().len())
        .map(|index| {
            let id = graph
                .parsed()
                .node_id(graph.parsed().nodes()[index].name())
                .unwrap();
            (
                id,
                Size {
                    width: 40.0,
                    height: 20.0,
                },
            )
        })
        .collect();
    let layout = layout_graph(&graph, &sizes, LayoutOptions::default()).unwrap();
    assert_eq!(layout.nodes.len(), graph.parsed().nodes().len());
    assert_eq!(
        layout
            .edges
            .iter()
            .filter(|edge| edge.kind == EdgeKind::Tree)
            .count(),
        2
    );
    assert_eq!(
        layout
            .edges
            .iter()
            .filter(|edge| edge.kind == EdgeKind::Dominance)
            .count(),
        2
    );
    let origins: HashSet<_> = layout
        .nodes
        .iter()
        .map(|node| (node.origin.x as i32, node.origin.y as i32))
        .collect();
    assert_eq!(origins.len(), layout.nodes.len());
    assert!(layout.size.width > 0.0 && layout.size.height > 0.0);
}
