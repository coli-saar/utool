use std::collections::HashSet;
use utool::{
    EdgeKind, HncGraph, Layout, LayoutOptions, Point, Size, layout_chart, layout_graph,
    layout_java_chart, layout_optimized_chart, parse_domcon_oz, parse_mrs_prolog, solve,
};

const CHART_LAYOUT_CORPUS: &[&str] = &[
    "[label(x f(x1)) dom(x1 y) label(y a)]",
    "[label(x f(x1)) label(y g(y1)) dom(x1 z) dom(y1 z) label(z a)]",
    "[label(x1 every(x2 x3)) label(y1 a(y2 y3)) label(z1 foo) label(z2 bar) label(z3 baz) dom(x2 z1) dom(y2 z2) dom(x3 z3) dom(y3 z3)]",
    "[label(x1 a(x2 x3)) label(y1 a(y2 y3)) label(z1 every(z2 z3)) label(w1 foo) label(w2 foo) label(w3 foo) label(w4 foo) dom(x2 w1) dom(y2 w2) dom(z2 w3) dom(x3 w4) dom(y3 w4) dom(z3 w4)]",
    "[label(x f(x1)) label(y g(y1)) label(z h(z1)) label(w a) dom(x y1) dom(y x1) dom(z y1) dom(y z1) dom(x1 w) dom(y1 w) dom(z1 w)]",
    "[label(x f(x1)) label(y g(y1)) label(z a) dom(x1 z) dom(y1 z) dom(y x1)]",
    "[label(x f(x1 x2)) label(x1 a) label(x2 b)]",
    "[label(x f(x1 x2)) label(x1 a) label(y b) dom(x2 y)]",
    "[label(x a)]",
    include_str!("../../src/main/resources/examples/chain3.clls"),
    include_str!("../../src/main/resources/examples/kallmeyer-romero.clls"),
];

const UNSOLVABLE_CORPUS: &[&str] = &[
    "[label(n0 f(n1 n2)) label(n3 a) label(n4 b) dom(n1 n3) dom(n2 n4) dom(n1 n4)]",
    "[label(n0 f(n1 n2)) label(n3 a) label(n4 b) dom(n1 n3) dom(n2 n4) dom(n2 n3)]",
    "[label(x f(x2 x3)) label(y g(y1)) label(z f(z1)) label(v a) label(w b) dom(x2 y) dom(x2 z) dom(x3 z) dom(y1 v) dom(z1 v) dom(z1 w)]",
    "[label(x f(x2 x3)) label(y g(y1)) label(z f(z1)) label(v a) label(w b) dom(x2 y) dom(x2 z) dom(x3 z) dom(z1 v) dom(y1 v) dom(z1 w)]",
];

fn sizes(graph: &HncGraph) -> Vec<(utool::NodeId, Size)> {
    graph
        .parsed()
        .nodes()
        .iter()
        .map(|node| {
            (
                graph.node_id(node.name()).expect("node is indexed"),
                Size {
                    width: 40.0,
                    height: 30.0,
                },
            )
        })
        .collect()
}

fn proper_cross(a: Point, b: Point, c: Point, d: Point) -> bool {
    let orientation =
        |p: Point, q: Point, r: Point| (q.x - p.x) * (r.y - p.y) - (q.y - p.y) * (r.x - p.x);
    orientation(a, b, c) * orientation(a, b, d) < 0.0
        && orientation(c, d, a) * orientation(c, d, b) < 0.0
}

fn assert_close(actual: f32, expected: f32) {
    assert!(
        (actual - expected).abs() < f32::EPSILON,
        "expected {expected}, got {actual}"
    );
}

fn dominance_crossings(layout: &Layout) -> usize {
    let edges = layout
        .edges
        .iter()
        .filter(|edge| edge.kind == EdgeKind::Dominance)
        .collect::<Vec<_>>();
    edges
        .iter()
        .enumerate()
        .map(|(index, first)| {
            edges[index + 1..]
                .iter()
                .filter(|second| {
                    first.source != second.source
                        && first.source != second.target
                        && first.target != second.source
                        && first.target != second.target
                        && proper_cross(
                            first.points[0],
                            *first.points.last().unwrap(),
                            second.points[0],
                            *second.points.last().unwrap(),
                        )
                })
                .count()
        })
        .sum()
}

fn all_crossings(layout: &Layout) -> usize {
    layout
        .edges
        .iter()
        .enumerate()
        .map(|(index, first)| {
            layout.edges[index + 1..]
                .iter()
                .filter(|second| {
                    first.source != second.source
                        && first.source != second.target
                        && first.target != second.source
                        && first.target != second.target
                        && first.points.windows(2).any(|first_segment| {
                            second.points.windows(2).any(|second_segment| {
                                proper_cross(
                                    first_segment[0],
                                    first_segment[1],
                                    second_segment[0],
                                    second_segment[1],
                                )
                            })
                        })
                })
                .count()
        })
        .sum()
}

fn barycenter_error(graph: &HncGraph, layout: &Layout) -> f32 {
    fn mark(graph: &HncGraph, node: utool::NodeId, fragment: usize, result: &mut [usize]) {
        result[node.index()] = fragment;
        for &child in graph.node(node).tree_children() {
            mark(graph, child, fragment, result);
        }
    }

    let mut fragment_of = vec![usize::MAX; graph.parsed().nodes().len()];
    for (fragment, &root) in graph.roots().iter().enumerate() {
        mark(graph, root, fragment, &mut fragment_of);
    }
    let mut offset_sums = vec![0.0_f32; graph.roots().len()];
    let mut edge_counts = vec![0.0_f32; graph.roots().len()];
    for &(source, target) in graph.parsed().dominance_edges() {
        let source_box = &layout.nodes[source.index()];
        let target_box = &layout.nodes[target.index()];
        let fragment = fragment_of[target.index()];
        offset_sums[fragment] += source_box.origin.x + source_box.size.width / 2.0
            - target_box.origin.x
            - target_box.size.width / 2.0;
        edge_counts[fragment] += 1.0;
    }
    offset_sums
        .into_iter()
        .zip(edge_counts)
        .filter(|(_, count)| *count > 0.0)
        .map(|(sum, count)| sum.abs() / count)
        .sum()
}

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
        .map(|node| (node.origin.x.to_bits(), node.origin.y.to_bits()))
        .collect();
    assert_eq!(origins.len(), layout.nodes.len());
    assert!(layout.size.width > 0.0 && layout.size.height > 0.0);
}

#[test]
fn chart_layout_handles_solver_corpus_without_node_overlap() {
    for input in CHART_LAYOUT_CORPUS {
        let graph = HncGraph::try_from(parse_domcon_oz(input).unwrap()).unwrap();
        let chart = solve(&graph).unwrap();
        let layout = layout_java_chart(&chart, &sizes(&graph), LayoutOptions::default()).unwrap();
        for (index, first) in layout.nodes.iter().enumerate() {
            for second in &layout.nodes[index + 1..] {
                let separated = first.origin.x + first.size.width <= second.origin.x
                    || second.origin.x + second.size.width <= first.origin.x
                    || first.origin.y + first.size.height <= second.origin.y
                    || second.origin.y + second.size.height <= first.origin.y;
                assert!(
                    separated,
                    "overlapping nodes {:?} and {:?} for {input}",
                    first.node, second.node
                );
            }
        }
        for edge in &layout.edges {
            assert!(
                edge.points.last().unwrap().y >= edge.points[0].y,
                "upward edge {:?} -> {:?} for {input}",
                edge.source,
                edge.target
            );
            if edge.kind == EdgeKind::Tree {
                let source = layout
                    .nodes
                    .iter()
                    .find(|node| node.node == edge.source)
                    .unwrap();
                let target = layout
                    .nodes
                    .iter()
                    .find(|node| node.node == edge.target)
                    .unwrap();
                assert_close(target.origin.y - source.origin.y, 57.0);
            }
        }
    }
}

#[test]
fn chart_layout_records_crossing_baseline() {
    let expected = [
        (0, 0),
        (0, 0),
        (1, 0),
        (2, 0),
        (2, 2),
        (0, 0),
        (0, 0),
        (0, 0),
        (0, 0),
        (0, 0),
        (1, 0),
    ];
    assert_eq!(CHART_LAYOUT_CORPUS.len(), expected.len());
    for (input, expected) in CHART_LAYOUT_CORPUS.iter().zip(expected) {
        let graph = HncGraph::try_from(parse_domcon_oz(input).unwrap()).unwrap();
        let measured = sizes(&graph);
        let simple = layout_graph(&graph, &measured, LayoutOptions::default()).unwrap();
        let chart = solve(&graph).unwrap();
        let java_port = layout_java_chart(&chart, &measured, LayoutOptions::default()).unwrap();
        let measured = (
            dominance_crossings(&simple),
            dominance_crossings(&java_port),
        );
        assert_eq!(measured, expected, "crossing regression for {input}");
    }
}

#[test]
fn fallback_layout_handles_unsolvable_graphs_without_overlap_or_upward_edges() {
    let closed_chain = "[label(y0 a0) label(x1 f1(xl1 xr1)) label(y1 a1) label(x2 f2(xl2 xr2)) \
         label(y2 a2) label(x3 f3(xl3 xr3)) dom(xl1 y0) dom(xr1 y1) \
         dom(xl2 y1) dom(xr2 y2) dom(xl3 y2) dom(xr3 y0)]";
    for input in UNSOLVABLE_CORPUS.iter().copied().chain([closed_chain]) {
        let graph = HncGraph::try_from(parse_domcon_oz(input).unwrap()).unwrap();
        let layout = layout_graph(&graph, &sizes(&graph), LayoutOptions::default()).unwrap();
        for (index, left) in layout.nodes.iter().enumerate() {
            for right in &layout.nodes[index + 1..] {
                assert!(
                    left.origin.x + left.size.width <= right.origin.x
                        || right.origin.x + right.size.width <= left.origin.x
                        || left.origin.y + left.size.height <= right.origin.y
                        || right.origin.y + right.size.height <= left.origin.y,
                    "node overlap for {input}"
                );
            }
        }
        assert!(
            layout
                .edges
                .iter()
                .all(|edge| edge.points.last().unwrap().y >= edge.points[0].y),
            "upward edge for {input}"
        );
        for edge in layout
            .edges
            .iter()
            .filter(|edge| edge.kind == EdgeKind::Tree)
        {
            let source = &layout.nodes[edge.source.index()];
            let target = &layout.nodes[edge.target.index()];
            assert_close(target.origin.y - source.origin.y, 57.0);
        }
    }
}

#[test]
fn chart_layout_matches_java_shared_targets_layers_and_crossings() {
    let input = CHART_LAYOUT_CORPUS[3];
    let graph = HncGraph::try_from(parse_domcon_oz(input).unwrap()).unwrap();
    let chart = solve(&graph).unwrap();
    let layout = layout_java_chart(&chart, &sizes(&graph), LayoutOptions::default()).unwrap();
    let origins = layout
        .nodes
        .iter()
        .map(|node| (graph.node(node.node).name(), node.origin))
        .collect::<std::collections::HashMap<_, _>>();

    for root in ["x1", "y1", "z1"] {
        assert_close(origins[root].y, 0.0);
    }
    for hole in ["x2", "x3", "y2", "y3", "z2", "z3"] {
        assert_close(origins[hole].y, 57.0);
    }
    for leaf in ["w1", "w2", "w3"] {
        assert_close(origins[leaf].y, 122.0);
    }
    assert_close(origins["w4"].y, 192.0);
    assert_eq!(dominance_crossings(&layout), 0);
}

#[test]
fn optimized_layout_never_worsens_corpus_crossings() {
    for input in CHART_LAYOUT_CORPUS {
        let graph = HncGraph::try_from(parse_domcon_oz(input).unwrap()).unwrap();
        let measured = sizes(&graph);
        let chart = solve(&graph).unwrap();
        let java = layout_java_chart(&chart, &measured, LayoutOptions::default()).unwrap();
        let optimized =
            layout_optimized_chart(&chart, &measured, LayoutOptions::default()).unwrap();
        assert!(
            all_crossings(&optimized) <= all_crossings(&java),
            "crossing regression for {input}"
        );
        assert!(
            barycenter_error(&graph, &optimized) <= barycenter_error(&graph, &java) + f32::EPSILON,
            "horizontal-balance regression for {input}"
        );
        for (index, left) in optimized.nodes.iter().enumerate() {
            for right in &optimized.nodes[index + 1..] {
                assert!(
                    left.origin.x + left.size.width <= right.origin.x
                        || right.origin.x + right.size.width <= left.origin.x
                        || left.origin.y + left.size.height <= right.origin.y
                        || right.origin.y + right.size.height <= left.origin.y,
                    "node overlap for {input}"
                );
            }
        }
        assert!(
            optimized
                .edges
                .iter()
                .all(|edge| edge.points.last().unwrap().y >= edge.points[0].y),
            "upward edge for {input}"
        );
    }
}

#[test]
fn chart_layout_uses_the_optimized_algorithm_by_default() {
    let graph = HncGraph::try_from(parse_domcon_oz(CHART_LAYOUT_CORPUS[4]).unwrap()).unwrap();
    let chart = solve(&graph).unwrap();
    let measured = sizes(&graph);
    assert_eq!(
        layout_chart(&chart, &measured, LayoutOptions::default()).unwrap(),
        layout_optimized_chart(&chart, &measured, LayoutOptions::default()).unwrap()
    );
}

#[test]
fn optimized_layout_centers_the_shared_lower_target() {
    let input = CHART_LAYOUT_CORPUS[3];
    let graph = HncGraph::try_from(parse_domcon_oz(input).unwrap()).unwrap();
    let measured = sizes(&graph);
    let chart = solve(&graph).unwrap();
    let java = layout_java_chart(&chart, &measured, LayoutOptions::default()).unwrap();
    let optimized = layout_optimized_chart(&chart, &measured, LayoutOptions::default()).unwrap();
    assert!(barycenter_error(&graph, &optimized) < barycenter_error(&graph, &java));
    assert_close(barycenter_error(&graph, &optimized), 0.0);
}

#[test]
fn optimized_layout_centers_rondane_892_named_fragment() {
    let graph = HncGraph::try_from(
        parse_mrs_prolog(include_str!(
            "../../src/main/resources/examples/rondane-892.mrs.pl"
        ))
        .unwrap(),
    )
    .unwrap();
    let measured = graph
        .parsed()
        .nodes()
        .iter()
        .map(|node| {
            let text = node.label().unwrap_or(node.name());
            (
                graph.node_id(node.name()).expect("node is indexed"),
                Size {
                    width: (text.chars().count() as f32 * 8.0 + 28.0).max(54.0),
                    height: 34.0,
                },
            )
        })
        .collect::<Vec<_>>();
    let chart = solve(&graph).unwrap();
    let layout = layout_optimized_chart(&chart, &measured, LayoutOptions::default()).unwrap();
    let target = graph.node_id("h24").unwrap();
    let target_center = layout.nodes[target.index()].origin.x
        + layout.nodes[target.index()].size.width / 2.0;
    let parent_centers = graph
        .parsed()
        .dominance_edges()
        .iter()
        .filter_map(|&(source, edge_target)| {
            (edge_target == target).then(|| {
                layout.nodes[source.index()].origin.x
                    + layout.nodes[source.index()].size.width / 2.0
            })
        })
        .collect::<Vec<_>>();
    assert_eq!(parent_centers.len(), 2);
    let parent_barycenter = parent_centers.iter().sum::<f32>() / parent_centers.len() as f32;
    assert!(
        (target_center - parent_barycenter).abs() < f32::EPSILON,
        "named&_of_p center {target_center}, parent barycenter {parent_barycenter}"
    );
}

#[test]
fn optimized_layout_centers_rondane_650_top_fragment() {
    let graph = HncGraph::try_from(
        parse_mrs_prolog(include_str!(
            "../../src/main/resources/examples/rondane-650.mrs.pl"
        ))
        .unwrap(),
    )
    .unwrap();
    let measured = graph
        .parsed()
        .nodes()
        .iter()
        .map(|node| {
            let text = node.label().unwrap_or(node.name());
            (
                graph.node_id(node.name()).expect("node is indexed"),
                Size {
                    width: (text.chars().count() as f32 * 8.0 + 28.0).max(54.0),
                    height: 34.0,
                },
            )
        })
        .collect::<Vec<_>>();
    let chart = solve(&graph).unwrap();
    let layout = layout_optimized_chart(&chart, &measured, LayoutOptions::default()).unwrap();
    let source = graph.node_id("h3").unwrap();
    let source_center = layout.nodes[source.index()].origin.x
        + layout.nodes[source.index()].size.width / 2.0;
    let target_centers = graph
        .parsed()
        .dominance_edges()
        .iter()
        .filter_map(|&(edge_source, target)| {
            (edge_source == source).then(|| {
                layout.nodes[target.index()].origin.x
                    + layout.nodes[target.index()].size.width / 2.0
            })
        })
        .collect::<Vec<_>>();
    assert!(!target_centers.is_empty());
    let target_barycenter = target_centers.iter().sum::<f32>() / target_centers.len() as f32;
    assert!(
        (source_center - target_barycenter).abs() < 0.01,
        "h3 center {source_center}, child barycenter {target_barycenter}"
    );
}

#[test]
fn default_chart_layout_uses_the_larger_horizontal_fragment_gap() {
    fn mark(graph: &HncGraph, node: utool::NodeId, fragment: usize, result: &mut [usize]) {
        result[node.index()] = fragment;
        for &child in graph.node(node).tree_children() {
            mark(graph, child, fragment, result);
        }
    }
    let graph = HncGraph::try_from(parse_domcon_oz(CHART_LAYOUT_CORPUS[3]).unwrap()).unwrap();
    let chart = solve(&graph).unwrap();
    let layout = layout_chart(&chart, &sizes(&graph), LayoutOptions::default()).unwrap();
    let mut fragment_of = vec![usize::MAX; layout.nodes.len()];
    for (fragment, &root) in graph.roots().iter().enumerate() {
        mark(&graph, root, fragment, &mut fragment_of);
    }
    for (index, left) in layout.nodes.iter().enumerate() {
        for right in &layout.nodes[index + 1..] {
            if fragment_of[left.node.index()] == fragment_of[right.node.index()]
                || left.origin.y + left.size.height <= right.origin.y
                || right.origin.y + right.size.height <= left.origin.y
            {
                continue;
            }
            let gap = if left.origin.x < right.origin.x {
                right.origin.x - left.origin.x - left.size.width
            } else {
                left.origin.x - right.origin.x - right.size.width
            };
            assert!(gap >= 50.0, "fragment gap was only {gap}");
        }
    }
}

#[test]
fn optimized_layout_aligns_chain_leaves() {
    let input = CHART_LAYOUT_CORPUS[9];
    let graph = HncGraph::try_from(parse_domcon_oz(input).unwrap()).unwrap();
    let chart = solve(&graph).unwrap();
    let optimized =
        layout_optimized_chart(&chart, &sizes(&graph), LayoutOptions::default()).unwrap();
    let origins = optimized
        .nodes
        .iter()
        .map(|node| (graph.node(node.node).name(), node.origin))
        .collect::<std::collections::HashMap<_, _>>();
    for leaf in ["y0", "y1", "y2", "y3"] {
        assert_close(origins[leaf].y, 162.0);
    }
}

#[test]
fn chart_layout_rejects_unsolvable_graphs_like_java() {
    for input in UNSOLVABLE_CORPUS {
        let graph = HncGraph::try_from(parse_domcon_oz(input).unwrap()).unwrap();
        let measured = sizes(&graph);
        let chart = solve(&graph).unwrap();
        assert_eq!(
            chart.count_solutions(),
            num_bigint::BigUint::default(),
            "{input}"
        );
        assert_eq!(
            layout_java_chart(&chart, &measured, LayoutOptions::default()),
            Err(utool::LayoutError::UnsolvableGraph),
            "{input}"
        );
        assert_eq!(
            layout_optimized_chart(&chart, &measured, LayoutOptions::default()),
            Err(utool::LayoutError::UnsolvableGraph),
            "{input}"
        );
        assert_eq!(
            layout_chart(&chart, &measured, LayoutOptions::default()),
            Err(utool::LayoutError::UnsolvableGraph),
            "{input}"
        );
    }
}
