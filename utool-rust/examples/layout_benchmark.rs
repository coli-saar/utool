use utool::{
    HncGraph, Layout, LayoutOptions, Point, Size, layout_java_chart, layout_optimized_chart,
    parse_domcon_oz, solve,
};

const CORPUS: &[&str] = &[
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

fn main() -> Result<(), Box<dyn std::error::Error>> {
    let details = std::env::args().any(|argument| argument == "--details");
    let mut java = Metrics::default();
    let mut optimized = Metrics::default();
    for (index, input) in CORPUS.iter().enumerate() {
        let graph = HncGraph::try_from(parse_domcon_oz(input)?)
            .map_err(|error| format!("graph {}: {error}", index + 1))?;
        let sizes = graph
            .parsed()
            .nodes()
            .iter()
            .map(|node| {
                (
                    graph.node_id(node.name()).unwrap(),
                    Size {
                        width: 40.0,
                        height: 30.0,
                    },
                )
            })
            .collect::<Vec<_>>();
        let chart = solve(&graph)?;
        let java_layout = layout_java_chart(&chart, &sizes, LayoutOptions::default())?;
        let optimized_layout = layout_optimized_chart(&chart, &sizes, LayoutOptions::default())?;
        if details {
            eprintln!(
                "graph={} java=({},{},{:.0},{:.0},{:.0}) optimized=({},{},{:.0},{:.0},{:.0})",
                index + 1,
                overlaps(&java_layout),
                crossings(&java_layout),
                barycenter_error(&graph, &java_layout),
                horizontal_span(&java_layout),
                java_layout.size.width,
                overlaps(&optimized_layout),
                crossings(&optimized_layout),
                barycenter_error(&graph, &optimized_layout),
                horizontal_span(&optimized_layout),
                optimized_layout.size.width,
            );
        }
        java.add(&graph, &java_layout);
        optimized.add(&graph, &optimized_layout);
    }
    println!(
        "score={:.0} overlaps={} crossings={} height={:.0} balance={:.0} span={:.0} width={:.0} baseline_score={:.0} baseline_overlaps={} baseline_crossings={} baseline_height={:.0} baseline_balance={:.0} baseline_span={:.0} baseline_width={:.0} graphs={}",
        optimized.score(),
        optimized.overlaps,
        optimized.crossings,
        optimized.height,
        optimized.balance,
        optimized.horizontal_span,
        optimized.width,
        java.score(),
        java.overlaps,
        java.crossings,
        java.height,
        java.balance,
        java.horizontal_span,
        java.width,
        CORPUS.len(),
    );
    Ok(())
}

#[derive(Default)]
struct Metrics {
    overlaps: usize,
    crossings: usize,
    height: f32,
    balance: f32,
    horizontal_span: f32,
    width: f32,
}

impl Metrics {
    fn add(&mut self, graph: &HncGraph, layout: &Layout) {
        self.overlaps += overlaps(layout);
        self.crossings += crossings(layout);
        self.height += layout.size.height;
        self.balance += barycenter_error(graph, layout);
        self.horizontal_span += horizontal_span(layout);
        self.width += layout.size.width;
    }

    fn score(&self) -> f64 {
        f64::from(u32::try_from(self.overlaps).unwrap()) * 1_000_000_000_000.0
            + f64::from(u32::try_from(self.crossings).unwrap()) * 1_000_000_000.0
            + f64::from(self.balance) * 1_000_000.0
            + f64::from(self.horizontal_span) * 1_000.0
            + f64::from(self.width)
    }
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
    let mut offsets = vec![Vec::new(); graph.roots().len()];
    for &(source, target) in graph.parsed().dominance_edges() {
        let source_box = &layout.nodes[source.index()];
        let target_box = &layout.nodes[target.index()];
        let source_center = source_box.origin.x + source_box.size.width / 2.0;
        let target_center = target_box.origin.x + target_box.size.width / 2.0;
        offsets[fragment_of[target.index()]].push(source_center - target_center);
    }
    offsets
        .into_iter()
        .filter(|values| !values.is_empty())
        .map(|values| {
            let count = u16::try_from(values.len()).unwrap_or(u16::MAX);
            values.iter().sum::<f32>().abs() / f32::from(count)
        })
        .sum()
}

fn horizontal_span(layout: &Layout) -> f32 {
    layout
        .edges
        .iter()
        .map(|edge| (edge.points[0].x - edge.points.last().unwrap().x).abs())
        .sum()
}

fn overlaps(layout: &Layout) -> usize {
    layout
        .nodes
        .iter()
        .enumerate()
        .map(|(index, left)| {
            layout.nodes[index + 1..]
                .iter()
                .filter(|right| {
                    left.origin.x < right.origin.x + right.size.width
                        && right.origin.x < left.origin.x + left.size.width
                        && left.origin.y < right.origin.y + right.size.height
                        && right.origin.y < left.origin.y + left.size.height
                })
                .count()
        })
        .sum()
}

fn crossings(layout: &Layout) -> usize {
    layout
        .edges
        .iter()
        .enumerate()
        .map(|(index, left)| {
            layout.edges[index + 1..]
                .iter()
                .filter(|right| {
                    left.source != right.source
                        && left.source != right.target
                        && left.target != right.source
                        && left.target != right.target
                        && left.points.windows(2).any(|left_segment| {
                            right
                                .points
                                .windows(2)
                                .any(|right_segment| proper_cross(left_segment, right_segment))
                        })
                })
                .count()
        })
        .sum()
}

fn proper_cross(left: &[Point], right: &[Point]) -> bool {
    let orientation =
        |p: Point, q: Point, r: Point| (q.x - p.x) * (r.y - p.y) - (q.y - p.y) * (r.x - p.x);
    orientation(left[0], left[1], right[0]) * orientation(left[0], left[1], right[1]) < 0.0
        && orientation(right[0], right[1], left[0]) * orientation(right[0], right[1], left[1]) < 0.0
}
