use serde::{Deserialize, Serialize};
use serde_json::{Value, json};
use std::{
    collections::HashMap,
    ffi::OsString,
    fs,
    net::{IpAddr, Ipv4Addr, UdpSocket},
    path::{Path, PathBuf},
    sync::{
        Arc, Mutex,
        atomic::{AtomicBool, AtomicU64, Ordering},
    },
    time::{Instant, SystemTime, UNIX_EPOCH},
};
use tauri::{
    Emitter, Manager, WebviewUrl, WebviewWindow, WebviewWindowBuilder, WindowEvent,
    menu::{
        CheckMenuItemBuilder, MenuBuilder, MenuItemBuilder, MenuItemKind, Submenu, SubmenuBuilder,
    },
};
use utool::{
    Chart, ChartDisplay, EdgeKind, HncGraph, InputCodec, LayoutError, LayoutOptions, OutputCodec,
    Point, RewriteSystem, ServerPreferences, Size, Solution, UserConfig, filter_chart, layout_chart,
    layout_graph, solve_with_cancellation,
};

const SERVER_ACTION_ID: &str = "server-action";
const SERVER_AUTOSTART_ID: &str = "server-autostart";
const SERVER_MENU_ID: &str = "server-menu";

#[derive(Clone, Serialize)]
#[serde(rename_all = "camelCase")]
struct ServerStatusView {
    state: &'static str,
    address: Option<String>,
    tooltip: String,
    notice: Option<String>,
}

impl ServerStatusView {
    fn stopped() -> Self {
        Self {
            state: "stopped",
            address: None,
            tooltip: "Server stopped".to_owned(),
            notice: None,
        }
    }

    fn error(error: &str) -> Self {
        Self {
            state: "error",
            address: None,
            tooltip: format!("Server error: {error}"),
            notice: None,
        }
    }
}

enum ServerPhase {
    Stopped {
        notice: Option<String>,
    },
    Starting {
        address: String,
    },
    Running {
        address: String,
        handle: utool::server::ServerHandle,
    },
    Stopping {
        address: String,
    },
    Failed {
        message: String,
    },
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
enum ServerPhaseKind {
    Stopped,
    Starting,
    Running,
    Stopping,
    Failed,
}

impl ServerPhaseKind {
    const fn can_transition_to(self, next: Self) -> bool {
        matches!(
            (self, next),
            (Self::Stopped | Self::Failed, Self::Starting)
                | (Self::Starting, Self::Stopped | Self::Running | Self::Failed)
                | (Self::Running, Self::Stopping)
                | (Self::Stopping, Self::Stopped | Self::Failed)
        )
    }
}

impl ServerPhase {
    const fn kind(&self) -> ServerPhaseKind {
        match self {
            Self::Stopped { .. } => ServerPhaseKind::Stopped,
            Self::Starting { .. } => ServerPhaseKind::Starting,
            Self::Running { .. } => ServerPhaseKind::Running,
            Self::Stopping { .. } => ServerPhaseKind::Stopping,
            Self::Failed { .. } => ServerPhaseKind::Failed,
        }
    }

    fn status(&self) -> ServerStatusView {
        match self {
            Self::Stopped { notice } => ServerStatusView {
                notice: notice.clone(),
                ..ServerStatusView::stopped()
            },
            Self::Starting { address } => ServerStatusView {
                state: "starting",
                address: Some(address.clone()),
                tooltip: format!("Starting server at {address}"),
                notice: None,
            },
            Self::Running { address, .. } => ServerStatusView {
                state: "running",
                address: Some(address.clone()),
                tooltip: format!("Server running at {address}"),
                notice: None,
            },
            Self::Stopping { address } => ServerStatusView {
                state: "stopping",
                address: Some(address.clone()),
                tooltip: format!("Stopping server at {address}"),
                notice: None,
            },
            Self::Failed { message } => ServerStatusView::error(message),
        }
    }
}

struct ServerState {
    config: Mutex<UserConfig>,
    phase: Mutex<ServerPhase>,
}

impl ServerState {
    fn load() -> Result<Self, String> {
        Ok(Self {
            config: Mutex::new(UserConfig::load().map_err(|error| error.to_string())?),
            phase: Mutex::new(ServerPhase::Stopped { notice: None }),
        })
    }

    fn preferences(&self) -> Result<ServerPreferences, String> {
        Ok(self
            .config
            .lock()
            .map_err(|_| "server preferences are unavailable")?
            .server_preferences())
    }

    fn update_preferences(
        &self,
        update: impl FnOnce(&mut ServerPreferences),
    ) -> Result<(), String> {
        let mut config = self
            .config
            .lock()
            .map_err(|_| "server preferences are unavailable")?;
        let mut preferences = config.server_preferences();
        update(&mut preferences);
        config.set_server_preferences(&preferences);
        config.save().map_err(|error| error.to_string())
    }
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct ServerDialogView {
    port: u16,
    accept_non_local: bool,
    local_address: String,
    ethernet_address: String,
}

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

struct BuiltinExample {
    filename: &'static str,
    description: &'static str,
    source: &'static str,
}

include!(concat!(env!("OUT_DIR"), "/builtin_examples.rs"));

#[derive(Clone, Serialize)]
#[serde(rename_all = "camelCase")]
struct ExampleSummaryView {
    id: &'static str,
    filename: &'static str,
    codec: &'static str,
    description: &'static str,
}

fn example_summary(example: &'static BuiltinExample) -> Result<ExampleSummaryView, String> {
    let codec = InputCodec::from_filename(example.filename).ok_or_else(|| {
        format!(
            "cannot infer a codec for built-in example {}",
            example.filename
        )
    })?;
    Ok(ExampleSummaryView {
        id: example.filename,
        filename: example.filename,
        codec: codec.name(),
        description: example.description,
    })
}

fn builtin_example(id: &str) -> Result<&'static BuiltinExample, String> {
    BUILTIN_EXAMPLES
        .iter()
        .find(|example| example.filename == id)
        .ok_or_else(|| format!("unknown built-in example: {id}"))
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
    initial_filter: Mutex<Option<StartupFilterView>>,
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
    window: WebviewWindow,
    startup: tauri::State<'_, StartupState>,
    documents: tauri::State<'_, DocumentState>,
) -> Result<Option<StartupFilterView>, String> {
    if let Some(filter) = documents
        .resources(window.label())?
        .initial_filter
        .lock()
        .map_err(|_| "initial filter state is unavailable")?
        .clone()
    {
        return Ok(Some(filter));
    }
    startup.filter.clone()
}

#[tauri::command]
fn app_info() -> AppInfoView {
    AppInfoView {
        version: env!("CARGO_PKG_VERSION"),
        build_id: env!("UTOOL_BUILD_ID"),
    }
}

#[tauri::command]
fn list_examples() -> Result<Vec<ExampleSummaryView>, String> {
    BUILTIN_EXAMPLES.iter().map(example_summary).collect()
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

fn set_menu_item_enabled(
    items: &[MenuItemKind<tauri::Wry>],
    id: &str,
    enabled: bool,
) -> Result<bool, String> {
    for item in items {
        if item.id().0 == id
            && let Some(item) = item.as_menuitem()
        {
            item.set_enabled(enabled)
                .map_err(|error| error.to_string())?;
            return Ok(true);
        }
        if let Some(submenu) = item.as_submenu()
            && set_menu_item_enabled(
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
            set_menu_item_enabled(&items, &id, enabled)?;
        }
    }
    let svg_enabled = match view.as_str() {
        "graph" => has_document,
        "solutions" => has_solution,
        _ => false,
    };
    set_menu_item_enabled(&items, "export-svg", svg_enabled)?;
    set_menu_item_enabled(&items, "copy-svg", svg_enabled)?;
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

fn create_graph_window(
    request: &OpenWindowRequest,
    app: &tauri::AppHandle,
    state: &DocumentState,
) -> Result<(), String> {
    let started = Instant::now();
    let graph = parse_graph(&request.input, &request.codec)?;
    create_graph_window_from_graph(graph, &request.title, None, started, app, state)
}

fn create_graph_window_from_graph(
    graph: HncGraph,
    title: &str,
    initial_filter: Option<StartupFilterView>,
    started: Instant,
    app: &tauri::AppHandle,
    state: &DocumentState,
) -> Result<(), String> {
    let drawing = graph_view(&graph, None)?;
    let elapsed_ms = started.elapsed().as_secs_f64() * 1000.0;
    let document_id = state.next_id.fetch_add(1, Ordering::Relaxed) + 1;
    let label = format!("graph-{document_id}");
    let resources = state.resources(&label)?;
    *resources
        .initial_filter
        .lock()
        .map_err(|_| "initial filter state is unavailable")? = initial_filter;
    *resources
        .document
        .lock()
        .map_err(|_| "document state is unavailable")? = Some((
        document_id,
        Document {
            graph,
            title: title.to_owned(),
            drawing,
            elapsed_ms,
        },
    ));
    let create_result =
        WebviewWindowBuilder::new(app, &label, WebviewUrl::App("index.html".into()))
            .title(format!("{title} — Utool"))
            .inner_size(1200.0, 800.0)
            .min_inner_size(800.0, 560.0)
            .build()
            .map_err(|error| error.to_string());
    if create_result.is_err() {
        state.remove_window(&label);
    }
    create_result
        .map(|_| refresh_window_menu(app))
        .and_then(|result| result)
}

fn ethernet_address() -> Option<Ipv4Addr> {
    let socket = UdpSocket::bind((Ipv4Addr::UNSPECIFIED, 0)).ok()?;
    socket.connect(("8.8.8.8", 80)).ok()?;
    match socket.local_addr().ok()?.ip() {
        IpAddr::V4(address) if !address.is_loopback() => Some(address),
        _ => None,
    }
}

fn server_submenu(app: &tauri::AppHandle) -> Result<Submenu<tauri::Wry>, String> {
    app.menu()
        .and_then(|menu| menu.get(SERVER_MENU_ID))
        .and_then(|item| match item {
            MenuItemKind::Submenu(menu) => Some(menu),
            _ => None,
        })
        .ok_or_else(|| "Server menu is unavailable".to_owned())
}

fn update_server_menu(app: &tauri::AppHandle, phase: &ServerPhase) -> Result<(), String> {
    let menu = server_submenu(app)?;
    let item = menu
        .get(SERVER_ACTION_ID)
        .and_then(|item| match item {
            MenuItemKind::MenuItem(item) => Some(item),
            _ => None,
        })
        .ok_or_else(|| "Server action menu item is unavailable".to_owned())?;
    let (text, enabled) = match phase {
        ServerPhase::Stopped { .. } | ServerPhase::Failed { .. } => ("Start server…", true),
        ServerPhase::Starting { .. } => ("Starting server…", false),
        ServerPhase::Running { .. } => ("Stop server", true),
        ServerPhase::Stopping { .. } => ("Stopping server…", false),
    };
    item.set_text(text).map_err(|error| error.to_string())?;
    item.set_enabled(enabled).map_err(|error| error.to_string())
}

fn publish_server_phase(app: &tauri::AppHandle, phase: &ServerPhase) -> ServerStatusView {
    if let Err(error) = update_server_menu(app, phase) {
        eprintln!("Could not synchronize the Server menu: {error}");
    }
    let status = phase.status();
    let _ = app.emit("server-status-changed", &status);
    status
}

fn transition_server(
    app: &tauri::AppHandle,
    servers: &ServerState,
    phase: ServerPhase,
) -> Result<ServerStatusView, String> {
    let mut current = servers
        .phase
        .lock()
        .map_err(|_| "server state is unavailable")?;
    if !current.kind().can_transition_to(phase.kind()) {
        return Err(format!(
            "invalid server state transition: {:?} to {:?}",
            current.kind(),
            phase.kind()
        ));
    }
    *current = phase;
    Ok(publish_server_phase(app, &current))
}

fn focus_graph_window(app: &tauri::AppHandle) -> Result<(), String> {
    preferred_graph_window(app)
        .ok_or_else(|| "no graph window is available".to_owned())
        .and_then(|window| activate_window(app, window.label()))
}

fn preferred_graph_window(app: &tauri::AppHandle) -> Option<WebviewWindow> {
    focused_window(app)
        .filter(|window| is_graph_window_label(window.label()))
        .or_else(|| app.get_webview_window("main"))
        .or_else(|| {
            app.webview_windows()
                .into_values()
                .find(|window| is_graph_window_label(window.label()))
        })
}

fn selected_server_address(port: u16, accept_non_local: bool) -> String {
    let host = if accept_non_local {
        ethernet_address().map_or_else(|| "0.0.0.0".to_owned(), |address| address.to_string())
    } else {
        Ipv4Addr::LOCALHOST.to_string()
    };
    format!("{host}:{port}")
}

fn port_in_use_message(port: u16) -> String {
    format!("Port {port} is already in use")
}

fn server_start_error(address: &str, error: &std::io::Error) -> String {
    match error.kind() {
        std::io::ErrorKind::PermissionDenied => format!(
            "Could not start the server at {address}: permission to use this address was denied."
        ),
        _ => format!("Could not start the server at {address}: {error}"),
    }
}

fn start_server(
    port: u16,
    accept_non_local: bool,
    app: &tauri::AppHandle,
    servers: &ServerState,
) -> Result<ServerStatusView, String> {
    let requested_address = selected_server_address(port, accept_non_local);
    {
        let mut phase = servers
            .phase
            .lock()
            .map_err(|_| "server state is unavailable")?;
        match &*phase {
            ServerPhase::Stopped { .. } | ServerPhase::Failed { .. } => {
                *phase = ServerPhase::Starting {
                    address: requested_address.clone(),
                };
                publish_server_phase(app, &phase);
            }
            ServerPhase::Starting { .. } => {
                return Err("Server startup is already in progress.".to_owned());
            }
            ServerPhase::Running { address, .. } => {
                return Err(format!(
                    "The server is already running at {address}. Stop it before starting it on another port."
                ));
            }
            ServerPhase::Stopping { .. } => {
                return Err("The server is still stopping. Try again in a moment.".to_owned());
            }
        }
    }

    let display_app = app.clone();
    let display_handler: utool::server::DisplayHandler =
        Arc::new(move |request: utool::server::DisplayRequest| {
            if let Some(graph) = request.graph {
                let graph = HncGraph::try_from(graph).map_err(|error| error.to_string())?;
                let state = display_app.state::<DocumentState>();
                let title = request.name.as_deref().unwrap_or("Graph from server");
                let initial_filter = request.filter_rules.map(|rewrite_system| StartupFilterView {
                    rewrite_system,
                    filename: request
                        .filter_name
                        .unwrap_or_else(|| "Server filter".to_owned()),
                });
                create_graph_window_from_graph(
                    graph,
                    title,
                    initial_filter,
                    Instant::now(),
                    &display_app,
                    &state,
                )
            } else {
                focus_graph_window(&display_app)
            }
        });
    let bind_address = if accept_non_local {
        Ipv4Addr::UNSPECIFIED
    } else {
        Ipv4Addr::LOCALHOST
    };
    if let Err(error) = servers.update_preferences(|preferences| {
        preferences.port = port;
        preferences.accept_non_local = accept_non_local;
    }) {
        let message = format!("Could not save the server settings: {error}");
        let _ = transition_server(
            app,
            servers,
            ServerPhase::Failed {
                message: message.clone(),
            },
        );
        return Err(message);
    }
    let handle = match utool::server::start(
        utool::server::ServerConfig {
            bind_address: bind_address.into(),
            port,
            logging: utool::server::ServerLogging::Disabled,
            warmup: false,
        },
        Some(display_handler),
    ) {
        Ok(handle) => handle,
        Err(error) => {
            if error.kind() == std::io::ErrorKind::AddrInUse {
                let message = port_in_use_message(port);
                let _ = transition_server(
                    app,
                    servers,
                    ServerPhase::Stopped {
                        notice: Some(message.clone()),
                    },
                );
                return Err(message);
            }
            let message = server_start_error(&requested_address, &error);
            let _ = transition_server(
                app,
                servers,
                ServerPhase::Failed {
                    message: message.clone(),
                },
            );
            return Err(message);
        }
    };
    let actual_port = handle.address().port();
    let address = selected_server_address(actual_port, accept_non_local);
    transition_server(app, servers, ServerPhase::Running { address, handle })
}

fn stop_server(app: &tauri::AppHandle, servers: &ServerState) -> Result<(), String> {
    let (handle, address) = {
        let mut phase = servers
            .phase
            .lock()
            .map_err(|_| "server state is unavailable")?;
        let previous = std::mem::replace(&mut *phase, ServerPhase::Stopped { notice: None });
        match previous {
            ServerPhase::Running { address, handle } => {
                *phase = ServerPhase::Stopping {
                    address: address.clone(),
                };
                publish_server_phase(app, &phase);
                (handle, address)
            }
            ServerPhase::Stopped { notice } => {
                *phase = ServerPhase::Stopped { notice };
                return Ok(());
            }
            other @ ServerPhase::Failed { .. } => {
                *phase = other;
                return Err("The server is not running.".to_owned());
            }
            other @ ServerPhase::Starting { .. } => {
                *phase = other;
                return Err("The server is still starting.".to_owned());
            }
            other @ ServerPhase::Stopping { .. } => {
                *phase = other;
                return Err("The server is already stopping.".to_owned());
            }
        }
    };
    if let Err(error) = handle.stop() {
        let message = format!("Could not stop the server at {address}: {error}");
        let _ = transition_server(
            app,
            servers,
            ServerPhase::Failed {
                message: message.clone(),
            },
        );
        return Err(message);
    }
    transition_server(app, servers, ServerPhase::Stopped { notice: None })?;
    Ok(())
}

fn handle_server_action(app: &tauri::AppHandle) -> Result<(), String> {
    let servers = app.state::<ServerState>();
    let action = {
        let phase = servers
            .phase
            .lock()
            .map_err(|_| "server state is unavailable")?;
        match &*phase {
            ServerPhase::Stopped { .. } | ServerPhase::Failed { .. } => "start",
            ServerPhase::Running { .. } => "stop",
            ServerPhase::Starting { .. } | ServerPhase::Stopping { .. } => {
                return Ok(());
            }
        }
    };
    match action {
        "stop" => stop_server(app, &servers),
        _ => {
            let window = preferred_graph_window(app)
                .ok_or_else(|| "no graph window is available".to_owned())?;
            window
                .emit_to(window.label(), "menu-start-server", ())
                .map_err(|error| error.to_string())
        }
    }
}

fn update_server_autostart(app: &tauri::AppHandle) -> Result<(), String> {
    let servers = app.state::<ServerState>();
    let checked = server_submenu(app)?
        .get(SERVER_AUTOSTART_ID)
        .and_then(|item| match item {
            MenuItemKind::Check(item) => Some(item),
            _ => None,
        })
        .ok_or_else(|| "server autostart menu is unavailable".to_owned())?
        .is_checked()
        .map_err(|error| error.to_string())?;
    servers.update_preferences(|preferences| preferences.start_on_launch = checked)
}

#[tauri::command]
fn server_dialog_info(state: tauri::State<'_, ServerState>) -> Result<ServerDialogView, String> {
    let preferences = state.preferences()?;
    Ok(ServerDialogView {
        port: preferences.port,
        accept_non_local: preferences.accept_non_local,
        local_address: "localhost".to_owned(),
        ethernet_address: ethernet_address()
            .map_or_else(|| "Unavailable".to_owned(), |address| address.to_string()),
    })
}

#[tauri::command]
fn server_status(state: tauri::State<'_, ServerState>) -> Result<ServerStatusView, String> {
    state
        .phase
        .lock()
        .map(|phase| phase.status())
        .map_err(|_| "server state is unavailable".to_owned())
}

#[tauri::command]
fn clear_server_notice(state: tauri::State<'_, ServerState>) -> Result<(), String> {
    let mut phase = state
        .phase
        .lock()
        .map_err(|_| "server state is unavailable".to_owned())?;
    if let ServerPhase::Stopped { notice } = &mut *phase {
        *notice = None;
    }
    Ok(())
}

#[tauri::command]
fn start_desktop_server(
    port: u16,
    accept_non_local: bool,
    app: tauri::AppHandle,
    state: tauri::State<'_, ServerState>,
) -> Result<ServerStatusView, String> {
    start_server(port, accept_non_local, &app, &state)
}

fn input_codec_label(codec: InputCodec) -> &'static str {
    match codec {
        InputCodec::Chain => "Generated Chain",
        InputCodec::DomconOz => "Domcon/Oz",
        InputCodec::DomgraphGxl => "Domgraph GXL",
        InputCodec::HoleSemantics => "Hole Semantics",
        InputCodec::MrsProlog => "MRS Prolog",
        InputCodec::MrsXml => "MRS XML",
    }
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
    let result = create_graph_window(&request, &app, &state);
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
fn open_example_window(
    id: String,
    window: WebviewWindow,
    app: tauri::AppHandle,
    state: tauri::State<'_, DocumentState>,
) -> Result<(), String> {
    let started = Instant::now();
    let result = (|| {
        let example = builtin_example(&id)?;
        let summary = example_summary(example)?;
        create_graph_window(
            &OpenWindowRequest {
                input: example.source.to_owned(),
                codec: summary.codec.to_owned(),
                title: example.filename.to_owned(),
                filename: format!("builtin: {}", example.filename),
            },
            &app,
            &state,
        )
    })();
    state.record(
        &app,
        window.label(),
        "Open built-in example",
        json!({ "example": id }),
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

fn is_graph_window_label(label: &str) -> bool {
    label == "main" || label.starts_with("graph-")
}

fn close_all_graph_windows(app: &tauri::AppHandle) -> Result<(), String> {
    let mut windows: Vec<_> = app
        .webview_windows()
        .into_values()
        .filter(|window| is_graph_window_label(window.label()))
        .collect();
    // Keep the original window until last so closing all behaves consistently
    // on platforms that terminate an application when its last window closes.
    windows.sort_by_key(|window| window.label() == "main");
    for window in windows {
        window.close().map_err(|error| error.to_string())?;
    }
    Ok(())
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
            list_examples,
            load_document,
            current_document,
            open_graph_window,
            open_example_window,
            build_chart,
            cancel_chart,
            chart_rows,
            solution_at,
            filter_chart_command,
            export_document,
            export_solution,
            set_output_context,
            event_entries,
            report_client_action,
            server_dialog_info,
            server_status,
            clear_server_notice,
            start_desktop_server
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
            app.manage(ServerState::load().map_err(std::io::Error::other)?);
            let server_preferences = app
                .state::<ServerState>()
                .preferences()
                .map_err(std::io::Error::other)?;
            let open = MenuItemBuilder::with_id("open", "Open…")
                .accelerator("CmdOrCtrl+O")
                .build(app)?;
            let open_example =
                MenuItemBuilder::with_id("open-example", "Open Example…").build(app)?;
            let close = MenuItemBuilder::with_id("close", "Close")
                .accelerator("CmdOrCtrl+W")
                .build(app)?;
            let close_all = MenuItemBuilder::with_id("close-all", "Close All")
                .accelerator("CmdOrCtrl+Alt+W")
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
            let mut paste_as = SubmenuBuilder::new(app, "Paste as");
            for codec in InputCodec::ALL {
                let mut item = MenuItemBuilder::with_id(
                    format!("paste-{}", codec.name()),
                    input_codec_label(codec),
                );
                if codec == InputCodec::DomconOz {
                    item = item.accelerator("CmdOrCtrl+V");
                }
                paste_as = paste_as.item(&item.build(app)?);
            }
            let paste_as = paste_as.build()?;
            let file = SubmenuBuilder::new(app, "File")
                .item(&open)
                .item(&open_example)
                .separator()
                .item(&export_as)
                .separator()
                .item(&close)
                .item(&close_all)
                .build()?;
            let edit = SubmenuBuilder::new(app, "Edit")
                .item(&copy_as)
                .item(&paste_as)
                .build()?;
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
            let server_action =
                MenuItemBuilder::with_id(SERVER_ACTION_ID, "Start server…").build(app)?;
            let server_autostart =
                CheckMenuItemBuilder::with_id(SERVER_AUTOSTART_ID, "Start server on launch")
                    .checked(server_preferences.start_on_launch)
                    .build(app)?;
            let server_menu = SubmenuBuilder::with_id(app, SERVER_MENU_ID, "Server")
                .item(&server_action)
                .separator()
                .item(&server_autostart)
                .build()?;
            let window_menu = SubmenuBuilder::with_id(app, WINDOW_MENU_ID, "Window")
                .text("event-log", "Event Log…")
                .separator()
                .minimize()
                .separator()
                .build()?;
            let menu = MenuBuilder::new(app)
                .items(&[
                    &application,
                    &file,
                    &edit,
                    &view,
                    &server_menu,
                    &window_menu,
                ])
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
                            SERVER_ACTION_ID => handle_server_action(app),
                            SERVER_AUTOSTART_ID => update_server_autostart(app),
                            "event-log" => show_event_log(app).map_err(|error| error.to_string()),
                            "close" => target.as_ref().map_or(Ok(()), |window| {
                                window.close().map_err(|error| error.to_string())
                            }),
                            "close-all" => close_all_graph_windows(app),
                            _ => target.as_ref().map_or(Ok(()), |window| {
                                window
                                    .emit_to(window.label(), &format!("menu-{id}"), ())
                                    .map_err(|error| error.to_string())
                            }),
                        };
                        let action = match id {
                            "open" => "Open graph",
                            "open-example" => "Choose built-in example",
                            "close" => "Close window",
                            "close-all" => "Close all graph windows",
                            "event-log" => "Open Event Log",
                            SERVER_ACTION_ID => "Start or stop server",
                            SERVER_AUTOSTART_ID => "Change server launch preference",
                            "export-svg" => "Choose Export SVG",
                            id if id.starts_with("export-") => "Choose graph/solution export",
                            id if id.starts_with("copy-") => "Choose graph/solution copy",
                            id if id.starts_with("paste-") => "Paste graph from clipboard",
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
            if server_preferences.start_on_launch {
                let servers = app.state::<ServerState>();
                if let Err(error) = start_server(
                    server_preferences.port,
                    server_preferences.accept_non_local,
                    app.handle(),
                    &servers,
                ) {
                    eprintln!("Could not start Utool server on launch: {error}");
                }
            }
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
    fn close_all_targets_document_windows_only() {
        assert!(is_graph_window_label("main"));
        assert!(is_graph_window_label("graph-1"));
        assert!(is_graph_window_label("graph-pasted"));
        assert!(!is_graph_window_label("event-log"));
        assert!(!is_graph_window_label("graphical-tool"));
    }

    #[test]
    fn server_phase_transitions_are_explicit_and_closed() {
        use ServerPhaseKind::{Failed, Running, Starting, Stopped, Stopping};

        let legal = [
            (Stopped, Starting),
            (Starting, Stopped),
            (Starting, Running),
            (Starting, Failed),
            (Running, Stopping),
            (Stopping, Stopped),
            (Stopping, Failed),
            (Failed, Starting),
        ];
        for from in [Stopped, Starting, Running, Stopping, Failed] {
            for to in [Stopped, Starting, Running, Stopping, Failed] {
                assert_eq!(
                    from.can_transition_to(to),
                    legal.contains(&(from, to)),
                    "unexpected transition {from:?} -> {to:?}"
                );
            }
        }
    }

    #[test]
    fn address_in_use_error_is_short_and_exact() {
        assert_eq!(port_in_use_message(2802), "Port 2802 is already in use");
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

    #[test]
    fn all_builtin_examples_have_unique_names_and_parse() {
        let mut names = std::collections::HashSet::new();
        assert!(!BUILTIN_EXAMPLES.is_empty());
        for example in BUILTIN_EXAMPLES {
            assert!(
                names.insert(example.filename),
                "duplicate example {}",
                example.filename
            );
            let summary = example_summary(example).unwrap();
            parse_graph(example.source, summary.codec).unwrap_or_else(|error| {
                panic!(
                    "built-in example {} does not parse: {error}",
                    example.filename
                )
            });
        }
    }
}
