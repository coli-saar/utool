use serde::{Deserialize, Serialize};
use serde_json::{Value, json};
use std::{
    collections::HashMap,
    ffi::OsString,
    fs,
    path::{Path, PathBuf},
    sync::{
        Arc, Mutex,
        atomic::{AtomicBool, AtomicU64, Ordering},
    },
    time::{Instant, SystemTime, UNIX_EPOCH},
};
use tauri::{
    Emitter, Manager, WebviewUrl, WebviewWindow, WebviewWindowBuilder, WindowEvent,
    menu::{MenuBuilder, MenuItemBuilder, MenuItemKind, Submenu, SubmenuBuilder},
};
use utool::{
    Chart, ChartDisplay, EdgeKind, HncGraph, InputCodec, LayoutError, LayoutOptions, OutputCodec,
    Point, RewriteSystem, Size, Solution, filter_chart, layout_chart, layout_graph,
    solve_with_cancellation,
};

#[derive(Debug, PartialEq, Eq)]
struct StartupArguments {
    graphs: Vec<PathBuf>,
    filter: Option<PathBuf>,
}

#[derive(Clone, Serialize)]
#[serde(rename_all = "camelCase")]
struct StartupDocumentView {
    input: String,
    codec: String,
    title: String,
    filename: String,
}

#[derive(Clone, Serialize)]
#[serde(rename_all = "camelCase")]
struct StartupFilterView {
    rewrite_system: String,
    filename: String,
}

#[derive(Clone, Serialize)]
#[serde(rename_all = "camelCase")]
struct AppInfoView {
    version: &'static str,
    build_id: &'static str,
}

struct StartupState {
    documents: Mutex<Option<Result<Vec<StartupDocumentView>, String>>>,
    filter: Result<Option<StartupFilterView>, String>,
}

fn parse_startup_arguments(
    args: impl IntoIterator<Item = OsString>,
) -> Result<StartupArguments, String> {
    let args = args.into_iter().collect::<Vec<_>>();
    let mut graphs = Vec::new();
    let mut filter = None;
    let mut positional_only = false;
    let mut index = 0;
    while index < args.len() {
        let argument = &args[index];
        if !positional_only && argument == "--" {
            positional_only = true;
        } else if !positional_only && (argument == "-f" || argument == "--filter") {
            index += 1;
            let value = args.get(index).ok_or_else(|| {
                format!("{} requires a filter filename", argument.to_string_lossy())
            })?;
            filter = Some(PathBuf::from(value));
        } else if !positional_only
            && let Some(argument) = argument.to_str()
            && let Some(value) = argument.strip_prefix("--filter=")
        {
            if value.is_empty() {
                return Err("--filter requires a filter filename".to_owned());
            }
            filter = Some(PathBuf::from(value));
        } else if !positional_only && argument.to_string_lossy().starts_with('-') {
            // Finder used to add a process serial number when starting an app.
            // Accept it defensively, but reject other flags so typos are visible.
            if !argument.to_string_lossy().starts_with("-psn_") {
                return Err(format!(
                    "unknown Utool desktop option: {}",
                    argument.to_string_lossy()
                ));
            }
        } else {
            graphs.push(PathBuf::from(argument));
        }
        index += 1;
    }
    Ok(StartupArguments { graphs, filter })
}

fn startup_state(args: impl IntoIterator<Item = OsString>) -> StartupState {
    let parsed = parse_startup_arguments(args);
    let documents = parsed.as_ref().map_or_else(
        |error| Err(error.clone()),
        |arguments| {
            arguments
                .graphs
                .iter()
                .map(|path| {
                    let filename = path.to_string_lossy().into_owned();
                    let title = path.file_name().map_or_else(
                        || filename.clone(),
                        |name| name.to_string_lossy().into_owned(),
                    );
                    let codec = InputCodec::from_filename(&filename).ok_or_else(|| {
                        format!("cannot infer an input codec from graph filename: {filename}")
                    })?;
                    let input = fs::read_to_string(path)
                        .map_err(|error| format!("could not read graph {filename}: {error}"))?;
                    Ok(StartupDocumentView {
                        input,
                        codec: codec.name().to_owned(),
                        title,
                        filename,
                    })
                })
                .collect()
        },
    );
    let filter = parsed.and_then(|arguments| {
        arguments.filter.map_or(Ok(None), |path| {
            let filename = path.to_string_lossy().into_owned();
            fs::read_to_string(&path)
                .map(|rewrite_system| {
                    Some(StartupFilterView {
                        rewrite_system,
                        filename: filename.clone(),
                    })
                })
                .map_err(|error| format!("could not read filter {filename}: {error}"))
        })
    });
    StartupState {
        documents: Mutex::new(Some(documents)),
        filter,
    }
}

struct Document {
    graph: HncGraph,
    title: String,
    drawing: GraphView,
    elapsed_ms: f64,
}

struct StoredChart {
    chart: Chart,
    display: ChartDisplay,
    source: String,
}

#[derive(Default)]
struct WindowResources {
    document: Mutex<Option<(u64, Document)>>,
    charts: Arc<Mutex<HashMap<u64, Arc<StoredChart>>>>,
    jobs: Arc<Mutex<HashMap<String, Arc<AtomicBool>>>>,
}

#[derive(Default)]
struct DocumentState {
    windows: Mutex<HashMap<String, Arc<WindowResources>>>,
    events: Mutex<Vec<EventEntry>>,
    next_id: AtomicU64,
    next_event_id: AtomicU64,
}

impl DocumentState {
    fn resources(&self, label: &str) -> Result<Arc<WindowResources>, String> {
        let mut windows = self
            .windows
            .lock()
            .map_err(|_| "window state is unavailable")?;
        Ok(Arc::clone(windows.entry(label.to_owned()).or_default()))
    }

    fn remove_window(&self, label: &str) {
        let resources = self
            .windows
            .lock()
            .ok()
            .and_then(|mut windows| windows.remove(label));
        if let Some(resources) = resources {
            if let Ok(mut jobs) = resources.jobs.lock() {
                for cancelled in jobs.values() {
                    cancelled.store(true, Ordering::SeqCst);
                }
                jobs.clear();
            }
            if let Ok(mut charts) = resources.charts.lock() {
                charts.clear();
            }
            if let Ok(mut document) = resources.document.lock() {
                *document = None;
            }
        }
    }

    fn window_title(&self, app: &tauri::AppHandle, window_label: &str) -> String {
        self.windows
            .lock()
            .ok()
            .and_then(|windows| windows.get(window_label).cloned())
            .and_then(|resources| {
                resources.document.lock().ok().and_then(|document| {
                    document
                        .as_ref()
                        .map(|(_, document)| document.title.clone())
                })
            })
            .or_else(|| {
                app.get_webview_window(window_label)
                    .and_then(|window| window.title().ok())
            })
            .unwrap_or_else(|| window_label.to_owned())
    }

    fn record(
        &self,
        app: &tauri::AppHandle,
        window_label: impl Into<String>,
        action: impl Into<String>,
        arguments: Value,
        started: Instant,
        error: Option<String>,
    ) {
        let window_label = window_label.into();
        let window_title = self.window_title(app, &window_label);
        let event = EventEntry {
            id: self.next_event_id.fetch_add(1, Ordering::Relaxed) + 1,
            timestamp_ms: SystemTime::now()
                .duration_since(UNIX_EPOCH)
                .unwrap_or_default()
                .as_millis(),
            window_title,
            action: action.into(),
            arguments,
            elapsed_ms: started.elapsed().as_secs_f64() * 1000.0,
            status: if error.is_some() {
                EventStatus::Error
            } else {
                EventStatus::Success
            },
            error,
        };
        if let Ok(mut events) = self.events.lock() {
            events.push(event.clone());
        }
        let _ = app.emit("event-log-updated", &event);
    }
}

#[derive(Clone, Serialize)]
#[serde(rename_all = "camelCase")]
struct EventEntry {
    id: u64,
    timestamp_ms: u128,
    window_title: String,
    action: String,
    arguments: Value,
    elapsed_ms: f64,
    status: EventStatus,
    error: Option<String>,
}

#[derive(Clone, Copy, Serialize)]
#[serde(rename_all = "lowercase")]
enum EventStatus {
    Success,
    Error,
}

#[derive(Clone, Serialize)]
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

#[derive(Clone, Serialize)]
#[serde(rename_all = "camelCase")]
struct EdgeView {
    source: usize,
    target: usize,
    kind: &'static str,
    points: Vec<Point>,
    light: bool,
}

#[derive(Clone, Serialize)]
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
    title: String,
    graph: GraphView,
    elapsed_ms: f64,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct ChartView {
    chart_id: u64,
    elapsed_ms: f64,
    solution_count: String,
    state_count: usize,
    subgraph_count: usize,
    split_count: usize,
    display_row_count: usize,
    top_fragments: Vec<String>,
    graph: Option<GraphView>,
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
    elapsed_ms: f64,
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
    let codec =
        InputCodec::from_name(codec).ok_or_else(|| format!("unsupported input codec: {codec}"))?;
    let parsed = codec.parse(input).map_err(|error| error.to_string())?;
    HncGraph::try_from(parsed).map_err(|error| error.to_string())
}

fn display_filename(path: &str) -> String {
    Path::new(path)
        .file_name()
        .and_then(|name| name.to_str())
        .unwrap_or(path)
        .to_owned()
}

fn graph_view(graph: &HncGraph, chart: Option<&Chart>) -> Result<GraphView, String> {
    let sizes: Vec<_> = graph
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
        .collect();
    let options = LayoutOptions::default();
    let layout = if let Some(chart) = chart {
        match layout_chart(chart, &sizes, options) {
            Ok(layout) => layout,
            Err(LayoutError::UnsolvableGraph) => {
                layout_graph(graph, &sizes, options).map_err(|error| error.to_string())?
            }
            Err(error) => return Err(error.to_string()),
        }
    } else {
        layout_graph(graph, &sizes, options).map_err(|error| error.to_string())?
    };
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

fn solution_view(solution: &Solution, elapsed_ms: f64) -> SolutionView {
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
        elapsed_ms,
        nodes,
        edges,
    }
}

#[tauri::command]
fn take_startup_documents(
    state: tauri::State<'_, StartupState>,
) -> Result<Vec<StartupDocumentView>, String> {
    state
        .documents
        .lock()
        .map_err(|_| "startup arguments are unavailable".to_owned())?
        .take()
        .unwrap_or_else(|| Ok(Vec::new()))
}

#[tauri::command]
fn startup_filter(
    state: tauri::State<'_, StartupState>,
) -> Result<Option<StartupFilterView>, String> {
    state.filter.clone()
}

#[tauri::command]
fn app_info() -> AppInfoView {
    AppInfoView {
        version: env!("CARGO_PKG_VERSION"),
        build_id: env!("UTOOL_BUILD_ID"),
    }
}

#[tauri::command]
fn load_document(
    input: String,
    codec: String,
    title: String,
    window: WebviewWindow,
    app: tauri::AppHandle,
    state: tauri::State<'_, DocumentState>,
) -> Result<LoadedDocumentView, String> {
    let started = Instant::now();
    let arguments =
        json!({ "graph": title, "format": codec, "input size": format!("{} bytes", input.len()) });
    let result = (|| {
        let graph = parse_graph(&input, &codec)?;
        let drawing = graph_view(&graph, None)?;
        let elapsed_ms = started.elapsed().as_secs_f64() * 1000.0;
        let document_id = state.next_id.fetch_add(1, Ordering::Relaxed) + 1;
        let resources = state.resources(window.label())?;
        if let Ok(jobs) = resources.jobs.lock() {
            for cancelled in jobs.values() {
                cancelled.store(true, Ordering::SeqCst);
            }
        }
        resources
            .charts
            .lock()
            .map_err(|_| "chart state is unavailable")?
            .clear();
        resources
            .jobs
            .lock()
            .map_err(|_| "job state is unavailable")?
            .clear();
        *resources
            .document
            .lock()
            .map_err(|_| "document state is unavailable")? = Some((
            document_id,
            Document {
                graph,
                title: title.clone(),
                drawing: drawing.clone(),
                elapsed_ms,
            },
        ));
        Ok(LoadedDocumentView {
            document_id,
            title,
            graph: drawing,
            elapsed_ms,
        })
    })();
    state.record(
        &app,
        window.label(),
        "Open graph",
        arguments,
        started,
        result.as_ref().err().cloned(),
    );
    result
}

#[tauri::command]
fn current_document(
    window: WebviewWindow,
    state: tauri::State<'_, DocumentState>,
) -> Result<Option<LoadedDocumentView>, String> {
    let resources = state.resources(window.label())?;
    let document = resources
        .document
        .lock()
        .map_err(|_| "document state is unavailable")?;
    Ok(document
        .as_ref()
        .map(|(document_id, document)| LoadedDocumentView {
            document_id: *document_id,
            title: document.title.clone(),
            graph: document.drawing.clone(),
            elapsed_ms: document.elapsed_ms,
        }))
}

#[tauri::command]
async fn build_chart(
    document_id: u64,
    job_id: String,
    window: WebviewWindow,
    app: tauri::AppHandle,
    state: tauri::State<'_, DocumentState>,
) -> Result<ChartView, String> {
    let started = Instant::now();
    let resources = state.resources(window.label())?;
    let graph = resources
        .document
        .lock()
        .map_err(|_| "document state is unavailable")?
        .as_ref()
        .filter(|(id, _)| *id == document_id)
        .ok_or("document is no longer open")?
        .1
        .graph
        .clone();
    let document = Arc::clone(&resources);
    let charts = Arc::clone(&resources.charts);
    let jobs = Arc::clone(&resources.jobs);
    let chart_id = state.next_id.fetch_add(1, Ordering::Relaxed) + 1;
    let cancelled = Arc::new(AtomicBool::new(false));
    resources
        .jobs
        .lock()
        .map_err(|_| "job state is unavailable")?
        .insert(job_id.clone(), Arc::clone(&cancelled));
    let result = tauri::async_runtime::spawn_blocking(move || {
        let result = (|| {
            let started = Instant::now();
            let chart = solve_with_cancellation(&graph, || cancelled.load(Ordering::Relaxed))
                .map_err(|error| error.to_string())?;
            let elapsed_ms = started.elapsed().as_secs_f64() * 1000.0;
            if cancelled.load(Ordering::SeqCst) {
                return Err("chart construction was cancelled".to_owned());
            }
            let stored = Arc::new(StoredChart {
                display: ChartDisplay::new(&chart),
                chart,
                source: "Original chart".to_owned(),
            });
            let response = chart_view(chart_id, &stored, elapsed_ms, Some(&graph))?;
            if document
                .document
                .lock()
                .map_err(|_| "document state is unavailable")?
                .as_ref()
                .is_none_or(|(id, _)| *id != document_id)
            {
                return Err("document is no longer open".to_owned());
            }
            charts
                .lock()
                .map_err(|_| "chart state is unavailable")?
                .insert(chart_id, stored);
            Ok(response)
        })();
        if let Ok(mut active) = jobs.lock() {
            active.remove(&job_id);
        }
        result
    })
    .await
    .map_err(|error| format!("solver task failed: {error}"))?;
    state.record(
        &app,
        window.label(),
        "Build chart",
        json!({ "chart": "Original chart" }),
        started,
        result.as_ref().err().cloned(),
    );
    result
}

#[tauri::command]
fn cancel_chart(
    job_id: String,
    window: WebviewWindow,
    app: tauri::AppHandle,
    state: tauri::State<'_, DocumentState>,
) {
    let started = Instant::now();
    let resources = state.resources(window.label());
    if let Ok(resources) = resources
        && let Ok(jobs) = resources.jobs.lock()
        && let Some(cancelled) = jobs.get(&job_id)
    {
        cancelled.store(true, Ordering::SeqCst);
    }
    state.record(
        &app,
        window.label(),
        "Cancel chart job",
        json!({ "operation": "Chart computation" }),
        started,
        None,
    );
}

#[tauri::command]
async fn solution_at(
    chart_id: u64,
    index: usize,
    window: WebviewWindow,
    app: tauri::AppHandle,
    state: tauri::State<'_, DocumentState>,
) -> Result<Option<SolutionView>, String> {
    let started = Instant::now();
    let resources = state.resources(window.label())?;
    let chart = Arc::clone(
        resources
            .charts
            .lock()
            .map_err(|_| "chart state is unavailable")?
            .get(&chart_id)
            .ok_or("chart is no longer available")?,
    );
    let chart_source = chart.source.clone();
    let result = tauri::async_runtime::spawn_blocking(move || {
        let started = Instant::now();
        let mut solutions = chart.chart.solutions();
        for _ in 0..=index {
            if !solutions.advance() {
                return Ok(None);
            }
        }
        let elapsed_ms = started.elapsed().as_secs_f64() * 1000.0;
        Ok(Some(solution_view(
            &solutions.current().unwrap(),
            elapsed_ms,
        )))
    })
    .await
    .map_err(|error| format!("solution task failed: {error}"))?;
    state.record(
        &app,
        window.label(),
        "Compute solution",
        json!({ "chart": chart_source, "solution": index + 1 }),
        started,
        result.as_ref().err().cloned(),
    );
    result
}

#[tauri::command]
fn export_document(
    document_id: u64,
    format: String,
    filename: String,
    window: WebviewWindow,
    app: tauri::AppHandle,
    state: tauri::State<'_, DocumentState>,
) -> Result<String, String> {
    let started = Instant::now();
    let result = (|| {
        let resources = state.resources(window.label())?;
        let document = resources
            .document
            .lock()
            .map_err(|_| "document state is unavailable")?;
        let graph = &document
            .as_ref()
            .filter(|(id, _)| *id == document_id)
            .ok_or("document is no longer open")?
            .1
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
    })();
    state.record(
        &app,
        window.label(),
        "Export graph",
        json!({ "format": format, "filename": display_filename(&filename) }),
        started,
        result.as_ref().err().cloned(),
    );
    result
}

fn set_output_menu_enabled(
    items: &[MenuItemKind<tauri::Wry>],
    id: &str,
    enabled: bool,
) -> Result<bool, String> {
    for item in items {
        if item.id().0 == id {
            if let Some(item) = item.as_menuitem() {
                item.set_enabled(enabled)
                    .map_err(|error| error.to_string())?;
                return Ok(true);
            }
        }
        if let Some(submenu) = item.as_submenu()
            && set_output_menu_enabled(
                &submenu.items().map_err(|error| error.to_string())?,
                id,
                enabled,
            )?
        {
            return Ok(true);
        }
    }
    Ok(false)
}

#[tauri::command]
fn set_output_context(
    view: String,
    has_document: bool,
    has_solution: bool,
    app: tauri::AppHandle,
) -> Result<(), String> {
    let menu = app.menu().ok_or("application menu is unavailable")?;
    let items = menu.items().map_err(|error| error.to_string())?;
    for codec in OutputCodec::ALL {
        let enabled = match view.as_str() {
            "graph" => has_document && codec.supports_graph(),
            "solutions" => has_solution && codec.supports_solutions(),
            _ => false,
        };
        for prefix in ["export", "copy"] {
            let id = format!("{prefix}-{}", codec.name());
            set_output_menu_enabled(&items, &id, enabled)?;
        }
    }
    let svg_enabled = match view.as_str() {
        "graph" => has_document,
        "solutions" => has_solution,
        _ => false,
    };
    set_output_menu_enabled(&items, "export-svg", svg_enabled)?;
    set_output_menu_enabled(&items, "copy-svg", svg_enabled)?;
    Ok(())
}

#[tauri::command]
async fn export_solution(
    chart_id: u64,
    index: usize,
    format: String,
    filename: String,
    window: WebviewWindow,
    app: tauri::AppHandle,
    state: tauri::State<'_, DocumentState>,
) -> Result<String, String> {
    let started = Instant::now();
    let resources = state.resources(window.label())?;
    let chart = Arc::clone(
        resources
            .charts
            .lock()
            .map_err(|_| "chart state is unavailable")?
            .get(&chart_id)
            .ok_or("chart is no longer available")?,
    );
    let codec = OutputCodec::from_name(&format)
        .ok_or_else(|| format!("unsupported output format: {format}"))?;
    if !codec.supports_solutions() {
        return Err(format!(
            "output format does not support solutions: {}",
            codec.name()
        ));
    }
    let result = tauri::async_runtime::spawn_blocking(move || {
        let mut solutions = chart.chart.solutions();
        for _ in 0..=index {
            if !solutions.advance() {
                return Err(format!("solution {} is no longer available", index + 1));
            }
        }
        let mut output = Vec::new();
        codec
            .write_single_solution_at(&solutions.current().unwrap(), index + 1, &mut output)
            .map_err(|error| error.to_string())?;
        String::from_utf8(output).map_err(|error| error.to_string())
    })
    .await
    .map_err(|error| format!("solution export task failed: {error}"))?;
    state.record(
        &app,
        window.label(),
        "Export solution",
        json!({ "format": format, "solution": index + 1, "filename": display_filename(&filename) }),
        started,
        result.as_ref().err().cloned(),
    );
    result
}

fn chart_view(
    chart_id: u64,
    stored: &StoredChart,
    elapsed_ms: f64,
    graph: Option<&HncGraph>,
) -> Result<ChartView, String> {
    let chart = &stored.chart;
    Ok(ChartView {
        chart_id,
        elapsed_ms,
        solution_count: chart.count_solutions().to_string(),
        state_count: chart.state_count(),
        subgraph_count: stored.display.subgraph_count(),
        split_count: chart.split_count(),
        display_row_count: stored.display.row_count(),
        top_fragments: chart.top_fragments(),
        graph: graph
            .map(|graph| graph_view(graph, Some(chart)))
            .transpose()?,
    })
}

#[tauri::command]
fn chart_rows(
    chart_id: u64,
    start: usize,
    count: usize,
    window: WebviewWindow,
    app: tauri::AppHandle,
    state: tauri::State<'_, DocumentState>,
) -> Result<ChartRowPage, String> {
    let started = Instant::now();
    const MAX_PAGE_SIZE: usize = 256;
    let result = (|| {
        let resources = state.resources(window.label())?;
        let charts = resources
            .charts
            .lock()
            .map_err(|_| "chart state is unavailable")?;
        let chart = Arc::clone(
            charts
                .get(&chart_id)
                .ok_or("chart is no longer available")?,
        );
        let chart_source = chart.source.clone();
        drop(charts);
        let count = count.min(MAX_PAGE_SIZE);
        let page = chart.display.rule_page(&chart.chart, start, count);
        Ok((
            ChartRowPage {
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
            },
            chart_source,
        ))
    })();
    let chart_source = result
        .as_ref()
        .ok()
        .map_or_else(|| "Unknown chart".to_owned(), |(_, source)| source.clone());
    let page_result = result.map(|(page, _)| page);
    state.record(
        &app,
        window.label(),
        "Load chart rows",
        json!({ "chart": chart_source, "rules": format!("{}–{}", start + 1, start.saturating_add(count)) }),
        started,
        page_result.as_ref().err().cloned(),
    );
    page_result
}

#[tauri::command]
async fn filter_chart_command(
    chart_id: u64,
    rewrite_system: String,
    filename: String,
    job_id: String,
    window: WebviewWindow,
    app: tauri::AppHandle,
    state: tauri::State<'_, DocumentState>,
) -> Result<ChartView, String> {
    let started = Instant::now();
    let filter_name = display_filename(&filename);
    let resources = state.resources(window.label())?;
    let charts = Arc::clone(&resources.charts);
    let source = Arc::clone(
        charts
            .lock()
            .map_err(|_| "chart state is unavailable")?
            .get(&chart_id)
            .ok_or("chart is no longer available")?,
    );
    let source_name = source.source.clone();
    let system = match RewriteSystem::parse(&rewrite_system).map_err(|error| error.to_string()) {
        Ok(system) => system,
        Err(error) => {
            state.record(
                &app,
                window.label(),
                "Apply chart filter",
                json!({ "chart": source_name, "filter": filter_name }),
                started,
                Some(error.clone()),
            );
            return Err(error);
        }
    };
    let jobs = Arc::clone(&resources.jobs);
    let result_id = state.next_id.fetch_add(1, Ordering::Relaxed) + 1;
    let result_source = format!("Filtered by {filter_name}");
    let stored_source = result_source.clone();
    let cancelled = Arc::new(AtomicBool::new(false));
    resources
        .jobs
        .lock()
        .map_err(|_| "job state is unavailable")?
        .insert(job_id.clone(), Arc::clone(&cancelled));
    let result = tauri::async_runtime::spawn_blocking(move || {
        let result = (|| {
            let started = Instant::now();
            let filtered =
                filter_chart(&source.chart, &system, || cancelled.load(Ordering::Relaxed))
                    .map_err(|error| error.to_string())?;
            let elapsed_ms = started.elapsed().as_secs_f64() * 1000.0;
            if cancelled.load(Ordering::SeqCst) {
                return Err("chart filtering was cancelled".to_owned());
            }
            let stored = Arc::new(StoredChart {
                display: ChartDisplay::new(&filtered),
                chart: filtered,
                source: stored_source,
            });
            let response = chart_view(result_id, &stored, elapsed_ms, None)?;
            charts
                .lock()
                .map_err(|_| "chart state is unavailable")?
                .insert(result_id, stored);
            Ok(response)
        })();
        if let Ok(mut active) = jobs.lock() {
            active.remove(&job_id);
        }
        result
    })
    .await
    .map_err(|error| format!("filter task failed: {error}"))?;
    state.record(
        &app,
        window.label(),
        "Apply chart filter",
        json!({ "source chart": source_name, "filter": filter_name, "result chart": result_source }),
        started,
        result.as_ref().err().cloned(),
    );
    result
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct OpenWindowRequest {
    input: String,
    codec: String,
    title: String,
    filename: String,
}

#[tauri::command]
fn open_graph_window(
    request: OpenWindowRequest,
    window: WebviewWindow,
    app: tauri::AppHandle,
    state: tauri::State<'_, DocumentState>,
) -> Result<(), String> {
    let started = Instant::now();
    let arguments = json!({
        "filename": display_filename(&request.filename),
        "format": request.codec,
        "input size": format!("{} bytes", request.input.len()),
    });
    let result = (|| {
        let graph = parse_graph(&request.input, &request.codec)?;
        let drawing = graph_view(&graph, None)?;
        let elapsed_ms = started.elapsed().as_secs_f64() * 1000.0;
        let document_id = state.next_id.fetch_add(1, Ordering::Relaxed) + 1;
        let label = format!("graph-{document_id}");
        let resources = state.resources(&label)?;
        *resources
            .document
            .lock()
            .map_err(|_| "document state is unavailable")? = Some((
            document_id,
            Document {
                graph,
                title: request.title.clone(),
                drawing,
                elapsed_ms,
            },
        ));
        let create_result =
            WebviewWindowBuilder::new(&app, &label, WebviewUrl::App("index.html".into()))
                .title(format!("{} — Utool", request.title))
                .inner_size(1200.0, 800.0)
                .min_inner_size(800.0, 560.0)
                .build()
                .map_err(|error| error.to_string());
        if create_result.is_err() {
            state.remove_window(&label);
        }
        create_result
            .map(|_| refresh_window_menu(&app))
            .and_then(|result| result)
    })();
    state.record(
        &app,
        window.label(),
        "Open graph in new window",
        arguments,
        started,
        result.as_ref().err().cloned(),
    );
    result
}

#[tauri::command]
fn event_entries(state: tauri::State<'_, DocumentState>) -> Result<Vec<EventEntry>, String> {
    state
        .events
        .lock()
        .map(|events| events.clone())
        .map_err(|_| "event log is unavailable".to_owned())
}

#[tauri::command]
fn report_client_action(
    action: String,
    arguments: Value,
    error: Option<String>,
    elapsed_ms: f64,
    window: WebviewWindow,
    app: tauri::AppHandle,
    state: tauri::State<'_, DocumentState>,
) {
    let started = Instant::now()
        .checked_sub(std::time::Duration::from_secs_f64(
            (elapsed_ms / 1000.0).max(0.0),
        ))
        .unwrap_or_else(Instant::now);
    state.record(&app, window.label(), action, arguments, started, error);
}

fn focused_window(app: &tauri::AppHandle) -> Option<WebviewWindow> {
    app.webview_windows()
        .into_values()
        .find(|window| window.is_focused().unwrap_or(false))
}

const WINDOW_MENU_ID: &str = "window-menu";
const WINDOW_ITEM_PREFIX: &str = "activate-window:";

fn window_menu(app: &tauri::AppHandle) -> Result<Submenu<tauri::Wry>, String> {
    app.menu()
        .and_then(|menu| menu.get(WINDOW_MENU_ID))
        .and_then(|item| match item {
            MenuItemKind::Submenu(submenu) => Some(submenu),
            _ => None,
        })
        .ok_or_else(|| "Window menu is unavailable".to_owned())
}

fn remove_window_menu_item(app: &tauri::AppHandle, label: &str) -> Result<(), String> {
    let menu = window_menu(app)?;
    let id = format!("{WINDOW_ITEM_PREFIX}{label}");
    if let Some(item) = menu.get(&id) {
        menu.remove(&item).map_err(|error| error.to_string())?;
    }
    Ok(())
}

fn refresh_window_menu(app: &tauri::AppHandle) -> Result<(), String> {
    let menu = window_menu(app)?;
    for item in menu.items().map_err(|error| error.to_string())? {
        if item.id().0.starts_with(WINDOW_ITEM_PREFIX) {
            menu.remove(&item).map_err(|error| error.to_string())?;
        }
    }

    let mut windows: Vec<_> = app.webview_windows().into_values().collect();
    windows.sort_by(|left, right| {
        let left_title = left.title().unwrap_or_else(|_| left.label().to_owned());
        let right_title = right.title().unwrap_or_else(|_| right.label().to_owned());
        left_title
            .cmp(&right_title)
            .then_with(|| left.label().cmp(right.label()))
    });
    for window in windows {
        let title = window.title().unwrap_or_else(|_| window.label().to_owned());
        let item =
            MenuItemBuilder::with_id(format!("{WINDOW_ITEM_PREFIX}{}", window.label()), title)
                .build(app)
                .map_err(|error| error.to_string())?;
        menu.append(&item).map_err(|error| error.to_string())?;
    }
    Ok(())
}

fn activate_window(app: &tauri::AppHandle, label: &str) -> Result<(), String> {
    let window = app
        .get_webview_window(label)
        .ok_or_else(|| format!("window is no longer open: {label}"))?;
    window.show().map_err(|error| error.to_string())?;
    window.unminimize().map_err(|error| error.to_string())?;
    window.set_focus().map_err(|error| error.to_string())
}

fn show_event_log(app: &tauri::AppHandle) -> Result<(), tauri::Error> {
    if let Some(window) = app.get_webview_window("event-log") {
        window.show()?;
        window.set_focus()?;
        let _ = refresh_window_menu(app);
        return Ok(());
    }
    WebviewWindowBuilder::new(
        app,
        "event-log",
        WebviewUrl::App("index.html?view=event-log".into()),
    )
    .title("Event Log — Utool")
    .inner_size(920.0, 640.0)
    .min_inner_size(640.0, 360.0)
    .build()?;
    let _ = refresh_window_menu(app);
    Ok(())
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    let startup = startup_state(std::env::args_os().skip(1));
    tauri::Builder::default()
        .manage(DocumentState::default())
        .manage(startup)
        .plugin(tauri_plugin_dialog::init())
        .plugin(tauri_plugin_fs::init())
        .plugin(tauri_plugin_clipboard_manager::init())
        .invoke_handler(tauri::generate_handler![
            take_startup_documents,
            startup_filter,
            app_info,
            load_document,
            current_document,
            open_graph_window,
            build_chart,
            cancel_chart,
            chart_rows,
            solution_at,
            filter_chart_command,
            export_document,
            export_solution,
            set_output_context,
            event_entries,
            report_client_action
        ])
        .on_window_event(|window, event| {
            if matches!(event, WindowEvent::Destroyed) {
                let state = window.state::<DocumentState>();
                let started = Instant::now();
                state.record(
                    window.app_handle(),
                    window.label(),
                    "Close window",
                    json!({}),
                    started,
                    None,
                );
                state.remove_window(window.label());
                let _ = remove_window_menu_item(window.app_handle(), window.label());
            } else if matches!(event, WindowEvent::Focused(true)) {
                let _ = refresh_window_menu(window.app_handle());
            }
        })
        .setup(|app| {
            let open = MenuItemBuilder::with_id("open", "Open…")
                .accelerator("CmdOrCtrl+O")
                .build(app)?;
            let close = MenuItemBuilder::with_id("close", "Close")
                .accelerator("CmdOrCtrl+W")
                .build(app)?;
            let graph_view = MenuItemBuilder::with_id("view-graph", "Graph")
                .accelerator("CmdOrCtrl+1")
                .build(app)?;
            let chart_view = MenuItemBuilder::with_id("view-chart", "Chart")
                .accelerator("CmdOrCtrl+2")
                .build(app)?;
            let solutions_view = MenuItemBuilder::with_id("view-solutions", "Solutions")
                .accelerator("CmdOrCtrl+3")
                .build(app)?;
            let zoom_in = MenuItemBuilder::with_id("zoom-in", "Zoom In")
                .accelerator("CmdOrCtrl+=")
                .build(app)?;
            let zoom_out = MenuItemBuilder::with_id("zoom-out", "Zoom Out")
                .accelerator("CmdOrCtrl+-")
                .build(app)?;
            let actual_size = MenuItemBuilder::with_id("actual-size", "Actual Size")
                .accelerator("CmdOrCtrl+0")
                .build(app)?;
            let application = SubmenuBuilder::new(app, "Utool")
                .text("about", "About Utool")
                .separator()
                .quit()
                .build()?;
            let mut export_as = SubmenuBuilder::new(app, "Export As");
            let mut copy_as = SubmenuBuilder::new(app, "Copy As");
            for codec in OutputCodec::ALL {
                let label = match codec {
                    OutputCodec::DomconOz => "Domcon/Oz",
                    OutputCodec::DomgraphDot => "Graphviz DOT",
                    OutputCodec::DomgraphGxl => "Domgraph GXL",
                    OutputCodec::DomgraphUdraw => "uDraw(Graph)",
                    OutputCodec::DomgraphCodegen => "Java Code",
                    OutputCodec::PluggingOz => "Plugging/Oz",
                    OutputCodec::PluggingLkb => "LKB Plugging",
                    OutputCodec::PluggingGroovy => "Groovy Plugging",
                    OutputCodec::TermProlog => "Prolog Term",
                    OutputCodec::TermOz => "Oz Term",
                };
                export_as = export_as.text(format!("export-{}", codec.name()), format!("{label}…"));
                copy_as = copy_as.text(format!("copy-{}", codec.name()), label);
            }
            let export_as = export_as
                .separator()
                .text("export-svg", "SVG Image…")
                .build()?;
            let copy_as = copy_as.separator().text("copy-svg", "SVG Image").build()?;
            let file = SubmenuBuilder::new(app, "File")
                .item(&open)
                .separator()
                .item(&export_as)
                .separator()
                .item(&close)
                .build()?;
            let edit = SubmenuBuilder::new(app, "Edit").item(&copy_as).build()?;
            let view = SubmenuBuilder::new(app, "View")
                .item(&graph_view)
                .item(&chart_view)
                .item(&solutions_view)
                .separator()
                .item(&zoom_in)
                .item(&zoom_out)
                .item(&actual_size)
                .separator()
                .text("fit-window", "Fit to Window")
                .build()?;
            let window_menu = SubmenuBuilder::with_id(app, WINDOW_MENU_ID, "Window")
                .text("event-log", "Event Log…")
                .separator()
                .minimize()
                .separator()
                .build()?;
            let menu = MenuBuilder::new(app)
                .items(&[&application, &file, &edit, &view, &window_menu])
                .build()?;
            app.set_menu(menu)?;
            refresh_window_menu(app.handle()).map_err(std::io::Error::other)?;
            app.on_menu_event(|app, event| {
                let started = Instant::now();
                let id = event.id().0.as_str();
                let target = focused_window(app);
                let label = target.as_ref().map_or_else(
                    || "application".to_owned(),
                    |window| window.label().to_owned(),
                );
                let state = app.state::<DocumentState>();
                let (action, arguments, result) =
                    if let Some(target_label) = id.strip_prefix(WINDOW_ITEM_PREFIX) {
                        (
                            "Activate window".to_owned(),
                            json!({ "window": state.window_title(app, target_label) }),
                            activate_window(app, target_label),
                        )
                    } else {
                        let result = match id {
                            "event-log" => show_event_log(app).map_err(|error| error.to_string()),
                            "close" => target.as_ref().map_or(Ok(()), |window| {
                                window.close().map_err(|error| error.to_string())
                            }),
                            _ => target.as_ref().map_or(Ok(()), |window| {
                                window
                                    .emit(&format!("menu-{id}"), ())
                                    .map_err(|error| error.to_string())
                            }),
                        };
                        let action = match id {
                            "open" => "Open graph",
                            "close" => "Close window",
                            "event-log" => "Open Event Log",
                            "export-svg" => "Choose Export SVG",
                            id if id.starts_with("export-") => "Choose graph/solution export",
                            id if id.starts_with("copy-") => "Choose graph/solution copy",
                            "view-graph" => "Show graph view",
                            "view-chart" => "Show chart view",
                            "view-solutions" => "Show solutions view",
                            "zoom-in" => "Zoom in",
                            "zoom-out" => "Zoom out",
                            "actual-size" => "Use actual size",
                            "fit-window" => "Fit graph to window",
                            "about" => "Show About Utool",
                            _ => id,
                        };
                        (action.to_owned(), json!({}), result)
                    };
                state.record(app, label, action, arguments, started, result.err());
            });
            Ok(())
        })
        .run(tauri::generate_context!())
        .expect("failed to run Utool");
}

#[cfg(test)]
mod tests {
    use super::*;

    fn arguments(values: &[&str]) -> Vec<OsString> {
        values.iter().map(OsString::from).collect()
    }

    #[test]
    fn startup_arguments_accept_graphs_and_filter_forms() {
        assert_eq!(
            parse_startup_arguments(arguments(&["-f", "rules.txt", "one.clls", "two.mrs.pl"]))
                .unwrap(),
            StartupArguments {
                graphs: vec![PathBuf::from("one.clls"), PathBuf::from("two.mrs.pl")],
                filter: Some(PathBuf::from("rules.txt")),
            }
        );
        assert_eq!(
            parse_startup_arguments(arguments(&["--filter=rules.txt", "--", "-graph.clls"]))
                .unwrap(),
            StartupArguments {
                graphs: vec![PathBuf::from("-graph.clls")],
                filter: Some(PathBuf::from("rules.txt")),
            }
        );
    }

    #[test]
    fn startup_arguments_report_missing_filter_and_unknown_options() {
        assert_eq!(
            parse_startup_arguments(arguments(&["-f"])).unwrap_err(),
            "-f requires a filter filename"
        );
        assert_eq!(
            parse_startup_arguments(arguments(&["--bogus"])).unwrap_err(),
            "unknown Utool desktop option: --bogus"
        );
    }

    #[test]
    fn resources_are_isolated_and_jobs_are_cancelled_on_window_removal() {
        let state = DocumentState::default();
        let first = state.resources("graph-1").unwrap();
        let second = state.resources("graph-2").unwrap();
        assert!(!Arc::ptr_eq(&first, &second));

        let cancelled = Arc::new(AtomicBool::new(false));
        first
            .jobs
            .lock()
            .unwrap()
            .insert("solver".to_owned(), Arc::clone(&cancelled));
        state.remove_window("graph-1");

        assert!(cancelled.load(Ordering::SeqCst));
        assert!(first.jobs.lock().unwrap().is_empty());
        assert!(first.charts.lock().unwrap().is_empty());
        assert!(first.document.lock().unwrap().is_none());
        let windows = state.windows.lock().unwrap();
        assert!(!windows.contains_key("graph-1"));
        assert!(windows.contains_key("graph-2"));
    }
}
