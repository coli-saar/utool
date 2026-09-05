use serde::Serialize;
use std::{
    collections::HashMap,
    sync::{
        Arc, Mutex,
        atomic::{AtomicU64, Ordering},
    },
};
use tauri::{
    Emitter,
    menu::{MenuBuilder, SubmenuBuilder},
};
use utool::{
    Chart, ChartDisplay, EdgeKind, HncGraph, InputCodec, LayoutOptions, OutputCodec, Point,
    RewriteSystem, Size, Solution, filter_chart, layout_graph, solve_with_cancellation,
};

struct Document {
    graph: HncGraph,
}

struct StoredChart {
    chart: Chart,
    display: ChartDisplay,
}

#[derive(Default)]
struct DocumentState {
    documents: Arc<Mutex<HashMap<u64, Document>>>,
    charts: Arc<Mutex<HashMap<u64, StoredChart>>>,
    next_id: AtomicU64,
    generation: Arc<AtomicU64>,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct NodeView {
    id: usize,
    name: String,
    label: Option<String>,
    hole: bool,
    x: f32,
    y: f32,
    width: f32,
    height: f32,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct EdgeView {
    source: usize,
    target: usize,
    kind: &'static str,
    points: Vec<Point>,
    light: bool,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct GraphView {
    nodes: Vec<NodeView>,
    edges: Vec<EdgeView>,
    width: f32,
    height: f32,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct LoadedDocumentView {
    document_id: u64,
    graph: GraphView,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct ChartView {
    chart_id: u64,
    solution_count: String,
    state_count: usize,
    subgraph_count: usize,
    split_count: usize,
    display_row_count: usize,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct ChartRuleView {
    state: u32,
    ordinal: usize,
    fragment: String,
    assignments: Vec<(String, Vec<String>)>,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct ChartStateView {
    state: u32,
    rule_count: usize,
    subgraph: Vec<String>,
    variant: Option<u32>,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct ChartRowPage {
    start: usize,
    total: usize,
    states: Vec<ChartStateView>,
    rows: Vec<ChartRuleView>,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct SolutionView {
    term: String,
    nodes: Vec<SolutionNodeView>,
    edges: Vec<(usize, usize)>,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct SolutionNodeView {
    id: usize,
    name: String,
    label: String,
}

fn parse_graph(input: &str, codec: &str) -> Result<HncGraph, String> {
    let codec = InputCodec::from_name(codec)
        .ok_or_else(|| format!("unsupported input codec: {codec}"))?;
    let parsed = codec.parse(input).map_err(|error| error.to_string())?;
    HncGraph::try_from(parsed).map_err(|error| error.to_string())
}

fn graph_view(graph: &HncGraph) -> Result<GraphView, String> {
    let sizes: Vec<_> = graph
        .parsed()
        .nodes()
        .iter()
        .map(|node| {
            let text = node.label().unwrap_or(node.name());
            (
                graph
                    .node_id(node.name())
                    .expect("node is indexed"),
                Size {
                    width: (text.chars().count() as f32 * 8.0 + 28.0).max(54.0),
                    height: 34.0,
                },
            )
        })
        .collect();
    let layout =
        layout_graph(graph, &sizes, LayoutOptions::default()).map_err(|error| error.to_string())?;
    let nodes = layout
        .nodes
        .iter()
        .map(|positioned| {
            let node = graph.node(positioned.node);
            NodeView {
                id: positioned.node.index(),
                name: node.name().to_owned(),
                label: node.label().map(str::to_owned),
                hole: node.is_hole(),
                x: positioned.origin.x,
                y: positioned.origin.y,
                width: positioned.size.width,
                height: positioned.size.height,
            }
        })
        .collect();
    let edges = layout
        .edges
        .into_iter()
        .map(|edge| EdgeView {
            source: edge.source.index(),
            target: edge.target.index(),
            kind: match edge.kind {
                EdgeKind::Tree => "tree",
                EdgeKind::Dominance => "dominance",
            },
            points: edge.points,
            light: edge.light,
        })
        .collect();
    Ok(GraphView {
        nodes,
        edges,
        width: layout.size.width + 40.0,
        height: layout.size.height + 40.0,
    })
}

fn solution_view(solution: &Solution) -> SolutionView {
    let mut nodes = Vec::new();
    let mut edges = Vec::new();
    let root = solution.root();
    let mut stack = vec![root];
    while let Some(tree) = stack.pop() {
        let node = solution.node_id(tree);
        nodes.push(SolutionNodeView {
            id: node.index(),
            name: solution.node_name(tree).to_owned(),
            label: solution.node_label(tree).to_owned(),
        });
        for child in solution.arena().get_children(tree) {
            edges.push((node.index(), solution.node_id(*child).index()));
            stack.push(*child);
        }
    }
    SolutionView {
        term: solution.to_term(),
        nodes,
        edges,
    }
}

#[tauri::command]
fn load_document(
    input: String,
    codec: String,
    state: tauri::State<'_, DocumentState>,
) -> Result<LoadedDocumentView, String> {
    let graph = parse_graph(&input, &codec)?;
    let drawing = graph_view(&graph)?;
    let document_id = state.next_id.fetch_add(1, Ordering::Relaxed) + 1;
    state
        .documents
        .lock()
        .map_err(|_| "document state is unavailable")?
        .insert(document_id, Document { graph });
    Ok(LoadedDocumentView {
        document_id,
        graph: drawing,
    })
}

#[tauri::command]
async fn build_chart(
    document_id: u64,
    state: tauri::State<'_, DocumentState>,
) -> Result<ChartView, String> {
    let graph = state
        .documents
        .lock()
        .map_err(|_| "document state is unavailable")?
        .get(&document_id)
        .ok_or("document is no longer open")?
        .graph
        .clone();
    let documents = Arc::clone(&state.documents);
    let charts = Arc::clone(&state.charts);
    let chart_id = state.next_id.fetch_add(1, Ordering::Relaxed) + 1;
    let generation = Arc::clone(&state.generation);
    let job = generation.fetch_add(1, Ordering::SeqCst) + 1;
    tauri::async_runtime::spawn_blocking(move || {
        let chart = solve_with_cancellation(&graph, || generation.load(Ordering::Relaxed) != job)
            .map_err(|error| error.to_string())?;
        if generation.load(Ordering::SeqCst) != job {
            return Err("chart construction was cancelled".to_owned());
        }
        let stored = StoredChart {
            display: ChartDisplay::new(&chart),
            chart,
        };
        let response = chart_view(chart_id, &stored);
        if !documents
            .lock()
            .map_err(|_| "document state is unavailable")?
            .contains_key(&document_id)
        {
            return Err("document is no longer open".to_owned());
        }
        charts
            .lock()
            .map_err(|_| "chart state is unavailable")?
            .insert(chart_id, stored);
        Ok(response)
    })
    .await
    .map_err(|error| format!("solver task failed: {error}"))?
}

#[tauri::command]
fn cancel_chart(state: tauri::State<'_, DocumentState>) {
    state.generation.fetch_add(1, Ordering::SeqCst);
}

#[tauri::command]
fn solution_at(
    chart_id: u64,
    index: usize,
    state: tauri::State<'_, DocumentState>,
) -> Result<Option<SolutionView>, String> {
    let charts = state
        .charts
        .lock()
        .map_err(|_| "chart state is unavailable")?;
    let chart = charts
        .get(&chart_id)
        .ok_or("chart is no longer available")?;
    let mut solutions = chart.chart.solutions();
    for _ in 0..=index {
        if !solutions.advance() {
            return Ok(None);
        }
    }
    Ok(Some(solution_view(&solutions.current().unwrap())))
}

#[tauri::command]
fn export_document(
    document_id: u64,
    format: String,
    state: tauri::State<'_, DocumentState>,
) -> Result<String, String> {
    let documents = state
        .documents
        .lock()
        .map_err(|_| "document state is unavailable")?;
    let graph = &documents
        .get(&document_id)
        .ok_or("document is no longer open")?
        .graph;
    let codec = OutputCodec::from_name(&format)
        .ok_or_else(|| format!("unsupported output format: {format}"))?;
    let encoder = codec
        .graph_encoder()
        .ok_or_else(|| format!("output format does not support graphs: {}", codec.name()))?;
    let mut output = Vec::new();
    encoder
        .write_graph(graph.parsed(), &mut output)
        .map_err(|error| error.to_string())?;
    String::from_utf8(output).map_err(|error| error.to_string())
}

fn chart_view(chart_id: u64, stored: &StoredChart) -> ChartView {
    let chart = &stored.chart;
    ChartView {
        chart_id,
        solution_count: chart.count_solutions().to_string(),
        state_count: chart.state_count(),
        subgraph_count: stored.display.subgraph_count(),
        split_count: chart.split_count(),
        display_row_count: stored.display.row_count(),
    }
}

#[tauri::command]
fn chart_rows(
    chart_id: u64,
    start: usize,
    count: usize,
    state: tauri::State<'_, DocumentState>,
) -> Result<ChartRowPage, String> {
    const MAX_PAGE_SIZE: usize = 256;
    let charts = state
        .charts
        .lock()
        .map_err(|_| "chart state is unavailable")?;
    let chart = charts
        .get(&chart_id)
        .ok_or("chart is no longer available")?;
    let count = count.min(MAX_PAGE_SIZE);
    let page = chart.display.rule_page(&chart.chart, start, count);
    Ok(ChartRowPage {
        start: page.start,
        total: page.total,
        states: page
            .states
            .into_iter()
            .map(|definition| ChartStateView {
                state: definition.state,
                rule_count: definition.rule_count,
                subgraph: definition.subgraph,
                variant: definition.variant,
            })
            .collect(),
        rows: page
            .rules
            .into_iter()
            .map(|rule| ChartRuleView {
                state: rule.state,
                ordinal: rule.ordinal,
                fragment: rule.fragment,
                assignments: rule.assignments,
            })
            .collect(),
    })
}

#[tauri::command]
async fn filter_chart_command(
    chart_id: u64,
    rewrite_system: String,
    state: tauri::State<'_, DocumentState>,
) -> Result<ChartView, String> {
    let system = RewriteSystem::parse(&rewrite_system).map_err(|error| error.to_string())?;
    let charts = Arc::clone(&state.charts);
    let generation = Arc::clone(&state.generation);
    let result_id = state.next_id.fetch_add(1, Ordering::Relaxed) + 1;
    let job = generation.fetch_add(1, Ordering::SeqCst) + 1;
    tauri::async_runtime::spawn_blocking(move || {
        let guard = charts.lock().map_err(|_| "chart state is unavailable")?;
        let source = guard.get(&chart_id).ok_or("chart is no longer available")?;
        let filtered = filter_chart(&source.chart, &system, || {
            generation.load(Ordering::Relaxed) != job
        })
        .map_err(|error| error.to_string())?;
        drop(guard);
        let stored = StoredChart {
            display: ChartDisplay::new(&filtered),
            chart: filtered,
        };
        let response = chart_view(result_id, &stored);
        charts
            .lock()
            .map_err(|_| "chart state is unavailable")?
            .insert(result_id, stored);
        Ok(response)
    })
    .await
    .map_err(|error| format!("filter task failed: {error}"))?
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .manage(DocumentState::default())
        .plugin(tauri_plugin_dialog::init())
        .plugin(tauri_plugin_fs::init())
        .invoke_handler(tauri::generate_handler![
            load_document,
            build_chart,
            cancel_chart,
            chart_rows,
            solution_at,
            filter_chart_command,
            export_document
        ])
        .setup(|app| {
            let application = SubmenuBuilder::new(app, "Utool")
                .text("about", "About Utool")
                .separator()
                .quit()
                .build()?;
            let file = SubmenuBuilder::new(app, "File")
                .text("open", "Open…")
                .separator()
                .text("export-svg", "Export SVG…")
                .text("export-domcon", "Export Domcon/Oz…")
                .text("export-dot", "Export Graphviz DOT…")
                .build()?;
            let solver = SubmenuBuilder::new(app, "Solver")
                .text("build-chart", "Build Chart")
                .text("filter-chart", "Filter Chart…")
                .text("show-solution", "Show First Solution")
                .build()?;
            let menu = MenuBuilder::new(app)
                .items(&[&application, &file, &solver])
                .build()?;
            app.set_menu(menu)?;
            app.on_menu_event(|app, event| {
                let _ = app.emit(&format!("menu-{}", event.id().0), ());
            });
            Ok(())
        })
        .run(tauri::generate_context!())
        .expect("failed to run Utool");
}
