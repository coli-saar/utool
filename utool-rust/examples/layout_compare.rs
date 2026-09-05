use std::{fmt::Write, fs, path::Path};
use utool::{
    EdgeKind, HncGraph, Layout, LayoutOptions, Size, layout_graph, layout_java_chart,
    layout_optimized_chart, parse_domcon_oz, solve,
};

const GRAPHS: &[(&str, &str)] = &[
    (
        "two-fragments-one-target",
        "[label(x f(x1)) label(y g(y1)) dom(x1 z) dom(y1 z) label(z a)]",
    ),
    (
        "cross-linked-leaves",
        "[label(x1 every(x2 x3)) label(y1 a(y2 y3)) label(z1 foo) label(z2 bar) label(z3 baz) dom(x2 z1) dom(y2 z2) dom(x3 z3) dom(y3 z3)]",
    ),
    (
        "shared-targets",
        "[label(x1 a(x2 x3)) label(y1 a(y2 y3)) label(z1 every(z2 z3)) label(w1 foo) label(w2 foo) label(w3 foo) label(w4 foo) dom(x2 w1) dom(y2 w2) dom(z2 w3) dom(x3 w4) dom(y3 w4) dom(z3 w4)]",
    ),
    (
        "cyclic-fragments",
        "[label(x f(x1)) label(y g(y1)) label(z h(z1)) label(w a) dom(x y1) dom(y x1) dom(z y1) dom(y z1) dom(x1 w) dom(y1 w) dom(z1 w)]",
    ),
    (
        "nonnormal-shared-target",
        "[label(x f(x1)) label(y g(y1)) label(z a) dom(x1 z) dom(y1 z) dom(y x1)]",
    ),
    (
        "single-dominated-leaf",
        "[label(x f(x1 x2)) label(x1 a) label(y b) dom(x2 y)]",
    ),
    (
        "chain3",
        include_str!("../../src/main/resources/examples/chain3.clls"),
    ),
    (
        "kallmeyer-romero",
        include_str!("../../src/main/resources/examples/kallmeyer-romero.clls"),
    ),
];

fn main() -> Result<(), Box<dyn std::error::Error>> {
    let output = Path::new("target/layout-comparisons");
    fs::create_dir_all(output)?;
    for &(name, input) in GRAPHS {
        let graph = HncGraph::try_from(parse_domcon_oz(input)?)?;
        let sizes = graph
            .parsed()
            .nodes()
            .iter()
            .map(|node| {
                let label = node.label().unwrap_or(node.name());
                let character_count = u16::try_from(label.chars().count()).unwrap_or(u16::MAX);
                (
                    graph.node_id(node.name()).expect("node is indexed"),
                    Size {
                        width: (f32::from(character_count) * 8.0 + 28.0).max(54.0),
                        height: 34.0,
                    },
                )
            })
            .collect::<Vec<_>>();
        let simple = layout_graph(&graph, &sizes, LayoutOptions::default())?;
        let chart = solve(&graph)?;
        let port = layout_java_chart(&chart, &sizes, LayoutOptions::default())?;
        let optimized = layout_optimized_chart(&chart, &sizes, LayoutOptions::default())?;
        fs::write(
            output.join(format!("{name}-simple.svg")),
            svg(&graph, &simple),
        )?;
        fs::write(
            output.join(format!("{name}-java-port.svg")),
            svg(&graph, &port),
        )?;
        fs::write(
            output.join(format!("{name}-optimized.svg")),
            svg(&graph, &optimized),
        )?;
    }
    Ok(())
}

fn svg(graph: &HncGraph, layout: &Layout) -> String {
    let margin = 24.0;
    let mut result = format!(
        r##"<svg xmlns="http://www.w3.org/2000/svg" width="{}" height="{}" viewBox="{} {} {} {}">
<rect x="0" y="0" width="100%" height="100%" fill="#fbfaf7"/>
"##,
        layout.size.width + 2.0 * margin,
        layout.size.height + 2.0 * margin,
        -margin,
        -margin,
        layout.size.width + 2.0 * margin,
        layout.size.height + 2.0 * margin,
    );
    for edge in &layout.edges {
        let points = edge
            .points
            .iter()
            .map(|point| format!("{},{}", point.x, point.y))
            .collect::<Vec<_>>()
            .join(" ");
        let (stroke, dash, width) = match edge.kind {
            EdgeKind::Tree => ("#4c566a", "", 1.8),
            EdgeKind::Dominance if edge.light => ("#b48ead", "5 5", 1.4),
            EdgeKind::Dominance => ("#bf616a", "5 5", 1.8),
        };
        writeln!(
            result,
            r#"<polyline points="{points}" fill="none" stroke="{stroke}" stroke-width="{width}" stroke-dasharray="{dash}"/>"#,
        )
        .unwrap();
    }
    for node in &layout.nodes {
        let graph_node = graph.node(node.node);
        let fill = if graph_node.is_hole() {
            "#eceff4"
        } else {
            "#d8dee9"
        };
        let stroke = if graph_node.is_hole() {
            "#b8c0ca"
        } else {
            "#2e3440"
        };
        let stroke_width = if graph_node.is_hole() { 0.9 } else { 1.0 };
        writeln!(
            result,
            r#"<rect x="{}" y="{}" width="{}" height="{}" rx="7" fill="{}" stroke="{}" stroke-width="{}"/>"#,
            node.origin.x,
            node.origin.y,
            node.size.width,
            node.size.height,
            fill,
            stroke,
            stroke_width,
        )
        .unwrap();
        let label = graph_node.label().unwrap_or(graph_node.name());
        writeln!(
            result,
            r##"<text x="{}" y="{}" text-anchor="middle" dominant-baseline="middle" font-family="sans-serif" font-size="12" fill="#2e3440">{}</text>"##,
            node.origin.x + node.size.width / 2.0,
            node.origin.y + node.size.height / 2.0,
            escape(label),
        )
        .unwrap();
    }
    result.push_str("</svg>\n");
    result
}

fn escape(text: &str) -> String {
    text.replace('&', "&amp;")
        .replace('<', "&lt;")
        .replace('>', "&gt;")
}
