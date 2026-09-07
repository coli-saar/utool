//! Java-compatible XML socket server.
//!
//! Each TCP connection carries one `<utool>` request. The server writes one
//! `<result>` or `<error>` response and then closes the connection.

use crate::{
    CodecError, HncGraph, InputCodec, OutputCodec, ParsedGraph, RewriteSystem, filter_chart,
    is_solvable, solve,
};
use quick_xml::{
    Reader, XmlVersion,
    events::{BytesStart, Event},
};
use socket2::{Domain, Protocol, Socket, Type};
use std::{
    fs::File,
    io::{self, BufRead, BufReader, BufWriter, Write},
    net::{IpAddr, Ipv4Addr, SocketAddr, TcpListener, TcpStream},
    path::PathBuf,
    sync::{
        Arc, Mutex, RwLock,
        atomic::{AtomicBool, Ordering},
    },
    thread::{self, JoinHandle},
    time::Duration,
    time::Instant,
};

const IO_ERROR: u8 = 128;
const GRAPH_DRAWING_ERROR: u8 = 130;
const PARSER_CONFIGURATION_ERROR: u8 = 140;
const NO_SUCH_COMMAND: u8 = 141;
const NO_INPUT: u8 = 150;
const NO_INPUT_CODEC: u8 = 151;
const NO_SUCH_INPUT_CODEC: u8 = 152;
const SOLVER_NOT_APPLICABLE: u8 = 153;
const NO_SUCH_OUTPUT_CODEC: u8 = 161;
const FILTER_ERROR: u8 = 170;
const INPUT_PARSE_ERROR: u8 = 192;
const OUTPUT_ERROR: u8 = 224;

/// Destination for optional server traffic logging.
#[derive(Clone, Debug, Default)]
pub enum ServerLogging {
    /// Do not log requests or responses.
    #[default]
    Disabled,
    /// Log to standard error.
    Stderr,
    /// Log to this file, replacing its previous contents.
    File(PathBuf),
}

/// Settings for the XML socket server.
#[derive(Clone, Debug)]
pub struct ServerConfig {
    /// Address to bind. Use localhost to accept only local connections.
    pub bind_address: IpAddr,
    /// TCP port to listen on.
    pub port: u16,
    /// Optional request/response logging.
    pub logging: ServerLogging,
    /// Exercise the solver before accepting connections.
    pub warmup: bool,
}

impl Default for ServerConfig {
    fn default() -> Self {
        Self {
            bind_address: IpAddr::V4(Ipv4Addr::UNSPECIFIED),
            port: 2802,
            logging: ServerLogging::Disabled,
            warmup: false,
        }
    }
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
enum Command {
    Solvable,
    Solve,
    Convert,
    Classify,
    Display,
    Help,
    DisplayCodecs,
    Version,
}

impl Command {
    fn parse(value: &str) -> Option<Self> {
        match value {
            "solvable" => Some(Self::Solvable),
            "solve" => Some(Self::Solve),
            "convert" => Some(Self::Convert),
            "classify" => Some(Self::Classify),
            "display" => Some(Self::Display),
            "help" => Some(Self::Help),
            "display-codecs" => Some(Self::DisplayCodecs),
            "version" => Some(Self::Version),
            _ => None,
        }
    }

    const fn requires_input(self) -> bool {
        matches!(
            self,
            Self::Solvable | Self::Solve | Self::Convert | Self::Classify
        )
    }
}

#[derive(Default)]
struct Request {
    command: Option<Command>,
    help_on: Option<String>,
    output_codec: Option<OutputCodec>,
    nochart: bool,
    limit: usize,
    graph: Option<ParsedGraph>,
    graph_name: Option<String>,
    filters: Vec<FilterRequest>,
}

enum FilterRequest {
    Cached,
    Rules(RewriteSystem),
}

#[derive(Debug)]
struct ServerError {
    code: u8,
    explanation: String,
}

impl ServerError {
    fn new(code: u8, explanation: impl Into<String>) -> Self {
        Self {
            code,
            explanation: explanation.into(),
        }
    }
}

type RuleCache = Arc<RwLock<Option<RewriteSystem>>>;
type Log = Option<Arc<Mutex<Box<dyn Write + Send>>>>;

/// A graph and optional client-supplied name from a `display` request.
pub struct DisplayRequest {
    pub graph: Option<ParsedGraph>,
    pub name: Option<String>,
}

/// Callback used by an embedded desktop server to handle `display` requests.
pub type DisplayHandler = Arc<dyn Fn(DisplayRequest) -> Result<(), String> + Send + Sync>;

/// A running server that can be stopped without terminating its host process.
pub struct ServerHandle {
    address: SocketAddr,
    shutdown: Arc<AtomicBool>,
    thread: Option<JoinHandle<io::Result<()>>>,
}

impl ServerHandle {
    /// Return the actual listening address. This is useful when port zero was requested.
    #[must_use]
    pub const fn address(&self) -> SocketAddr {
        self.address
    }

    /// Ask the accept loop to stop and wait for it to finish.
    ///
    /// # Errors
    ///
    /// Returns an error if the server loop failed or panicked.
    pub fn stop(mut self) -> io::Result<()> {
        self.shutdown.store(true, Ordering::SeqCst);
        self.join()
    }

    /// Wait for the server loop to finish.
    ///
    /// # Errors
    ///
    /// Returns an error if the server loop failed or panicked.
    pub fn wait(mut self) -> io::Result<()> {
        self.join()
    }

    fn join(&mut self) -> io::Result<()> {
        self.thread.take().map_or(Ok(()), |thread| {
            thread
                .join()
                .map_err(|_| io::Error::other("server thread panicked"))?
        })
    }
}

impl Drop for ServerHandle {
    fn drop(&mut self) {
        self.shutdown.store(true, Ordering::SeqCst);
    }
}

/// Listen indefinitely and serve Java Utool-compatible XML requests.
///
/// # Errors
///
/// Returns an I/O error if logging or the listening socket cannot be opened,
/// or if accepting a client fails.
pub fn run(config: ServerConfig) -> io::Result<()> {
    start(config, None)?.wait()
}

/// Start the server in the background, optionally handling `display` requests.
///
/// # Errors
///
/// Returns an error if logging or the listening socket cannot be opened.
pub fn start(
    config: ServerConfig,
    display_handler: Option<DisplayHandler>,
) -> io::Result<ServerHandle> {
    if config.warmup {
        warmup();
    }
    let log: Log = match config.logging {
        ServerLogging::Disabled => None,
        ServerLogging::Stderr => Some(Arc::new(Mutex::new(Box::new(io::stderr())))),
        ServerLogging::File(path) => Some(Arc::new(Mutex::new(Box::new(File::create(path)?)))),
    };
    let bind_address = SocketAddr::new(config.bind_address, config.port);
    let socket = Socket::new(
        Domain::for_address(bind_address),
        Type::STREAM,
        Some(Protocol::TCP),
    )?;
    // Rust's standard listener enables SO_REUSEADDR on some platforms. That
    // permits a wildcard listener and a localhost listener to claim the same
    // port, which makes the desktop report a false-successful startup.
    socket.set_reuse_address(false)?;
    socket.bind(&bind_address.into())?;
    socket.listen(128)?;
    let listener: TcpListener = socket.into();
    listener.set_nonblocking(true)?;
    let address = listener.local_addr()?;
    log_line(&log, &format!("Listening on {address}..."));
    let rules = Arc::new(RwLock::new(None));
    let shutdown = Arc::new(AtomicBool::new(false));
    let thread_shutdown = Arc::clone(&shutdown);
    let thread = thread::spawn(move || {
        while !thread_shutdown.load(Ordering::SeqCst) {
            match listener.accept() {
                Ok((stream, _)) => {
                    let rules = Arc::clone(&rules);
                    let log = log.clone();
                    let display_handler = display_handler.clone();
                    thread::spawn(move || {
                        if let Err(error) =
                            serve_connection(stream, &rules, &log, display_handler.as_ref())
                        {
                            log_line(
                                &log,
                                &format!("I/O error while processing command: {error}"),
                            );
                        }
                    });
                }
                Err(error) if error.kind() == io::ErrorKind::WouldBlock => {
                    thread::sleep(Duration::from_millis(20));
                }
                Err(error) => return Err(error),
            }
        }
        Ok(())
    });
    Ok(ServerHandle {
        address,
        shutdown,
        thread: Some(thread),
    })
}

fn warmup() {
    eprintln!("Warming up the server (2 passes) ... ");
    for pass in 1..=2 {
        eprintln!("  - pass {pass}");
        if let Ok(parsed) = InputCodec::Chain.parse("12")
            && let Ok(graph) = HncGraph::try_from(parsed)
            && let Ok(chart) = solve(&graph)
        {
            let mut solutions = chart.solutions();
            while solutions.advance() {}
        }
    }
    eprintln!("Utool is now warmed up.");
}

fn log_line(log: &Log, message: &str) {
    if let Some(log) = log
        && let Ok(mut destination) = log.lock()
    {
        let _ = writeln!(destination, "{message}");
        let _ = destination.flush();
    }
}

fn serve_connection(
    stream: TcpStream,
    rules: &RuleCache,
    log: &Log,
    display_handler: Option<&DisplayHandler>,
) -> io::Result<()> {
    let peer = stream.peer_addr().ok();
    log_line(log, &format!("Accepted connection from {peer:?}"));
    let mut reader = BufReader::new(stream.try_clone()?);
    let parsed = parse_request(&mut reader);
    let response = match parsed {
        Ok(request) => process_request(request, rules, display_handler),
        Err(error) => error_response(&error),
    };
    log_line(log, &format!("Sent: {}", response.trim_end()));
    let mut writer = BufWriter::new(stream);
    writer.write_all(response.as_bytes())?;
    writer.flush()
}

fn attribute(element: &BytesStart<'_>, name: &[u8]) -> Result<Option<String>, ServerError> {
    for attribute in element.attributes() {
        let attribute = attribute.map_err(|error| {
            ServerError::new(
                INPUT_PARSE_ERROR,
                format!("An error occurred while parsing the input!\n{error}"),
            )
        })?;
        if attribute.key.as_ref() == name {
            return attribute
                .normalized_value(XmlVersion::Implicit1_0)
                .map(|value| Some(value.into_owned()))
                .map_err(|error| {
                    ServerError::new(
                        INPUT_PARSE_ERROR,
                        format!("An XML entity could not be resolved: {error}"),
                    )
                });
        }
    }
    Ok(None)
}

fn parse_request(input: &mut dyn BufRead) -> Result<Request, ServerError> {
    let mut reader = Reader::from_reader(input);
    reader.config_mut().trim_text(true);
    let mut buffer = Vec::new();
    let mut request = Request {
        limit: usize::MAX,
        ..Request::default()
    };
    let mut saw_root = false;
    loop {
        match reader.read_event_into(&mut buffer) {
            Ok(Event::Start(element)) => match element.local_name().as_ref() {
                b"utool" => {
                    if saw_root {
                        return Err(ServerError::new(
                            INPUT_PARSE_ERROR,
                            "An error occurred while parsing the input!",
                        ));
                    }
                    saw_root = true;
                    parse_utool(&element, &mut request)?;
                }
                b"usr" => parse_usr(&element, &mut request)?,
                b"filter" => parse_filter(&element, &mut request)?,
                _ => {}
            },
            Ok(Event::Empty(element)) => match element.local_name().as_ref() {
                b"utool" => {
                    parse_utool(&element, &mut request)?;
                    break;
                }
                b"usr" => parse_usr(&element, &mut request)?,
                b"filter" => parse_filter(&element, &mut request)?,
                _ => {}
            },
            Ok(Event::End(element)) if element.local_name().as_ref() == b"utool" => break,
            Ok(Event::Eof) => {
                return Err(ServerError::new(
                    INPUT_PARSE_ERROR,
                    "An error occurred while parsing the input!\nUnexpected end of input",
                ));
            }
            Ok(_) => {}
            Err(error) => {
                return Err(ServerError::new(
                    INPUT_PARSE_ERROR,
                    format!("An error occurred while parsing the input!\n{error}"),
                ));
            }
        }
        buffer.clear();
    }
    let command = request
        .command
        .ok_or_else(|| ServerError::new(NO_SUCH_COMMAND, "You must specify a command!"))?;
    if command.requires_input() && request.graph.is_none() {
        return Err(ServerError::new(
            NO_INPUT,
            "You must specify an input graph!",
        ));
    }
    Ok(request)
}

fn parse_utool(element: &BytesStart<'_>, request: &mut Request) -> Result<(), ServerError> {
    let command_name = attribute(element, b"cmd")?
        .ok_or_else(|| ServerError::new(NO_SUCH_COMMAND, "You must specify a command!"))?;
    let command = Command::parse(&command_name).ok_or_else(|| {
        ServerError::new(NO_SUCH_COMMAND, format!("Unknown command: {command_name}"))
    })?;
    request.command = Some(command);
    request.help_on = attribute(element, b"on")?;
    request.nochart =
        attribute(element, b"nochart")?.is_some_and(|value| value.eq_ignore_ascii_case("true"));
    if let Some(limit) = attribute(element, b"limit")? {
        request.limit = limit.parse().map_err(|_| {
            ServerError::new(
                PARSER_CONFIGURATION_ERROR,
                "The solution limit must be a nonnegative integer.",
            )
        })?;
    }
    if let Some(name) = attribute(element, b"output-codec")? {
        request.output_codec = Some(OutputCodec::from_name(&name).ok_or_else(|| {
            ServerError::new(
                NO_SUCH_OUTPUT_CODEC,
                format!("Unknown output codec: {name}"),
            )
        })?);
    }
    Ok(())
}

fn parse_usr(element: &BytesStart<'_>, request: &mut Request) -> Result<(), ServerError> {
    let codec_name = attribute(element, b"codec")?.ok_or_else(|| {
        ServerError::new(
            NO_INPUT_CODEC,
            "You must specify an input codec for the USR!",
        )
    })?;
    let source = attribute(element, b"string")?
        .ok_or_else(|| ServerError::new(NO_INPUT, "You must specify an USR!"))?;
    let codec = InputCodec::from_name(&codec_name).ok_or_else(|| {
        ServerError::new(
            NO_SUCH_INPUT_CODEC,
            format!("Unknown input codec: {codec_name}"),
        )
    })?;
    request.graph_name = attribute(element, b"name")?.filter(|name| !name.trim().is_empty());
    request.graph = Some(codec.parse(&source).map_err(|error| {
        let code = if codec == InputCodec::Chain && matches!(&error, CodecError::Semantic(_)) {
            INPUT_PARSE_ERROR + 1
        } else {
            INPUT_PARSE_ERROR
        };
        ServerError::new(
            code,
            format!("A parsing error occurred while reading the input.\n{error}"),
        )
    })?);
    Ok(())
}

fn parse_filter(element: &BytesStart<'_>, request: &mut Request) -> Result<(), ServerError> {
    request
        .filters
        .push(if let Some(rules) = attribute(element, b"rules")? {
            FilterRequest::Rules(RewriteSystem::parse(&rules).map_err(|error| {
                ServerError::new(
                    FILTER_ERROR,
                    format!("An error occurred while reading the filtering rules file!\n{error}"),
                )
            })?)
        } else {
            FilterRequest::Cached
        });
    Ok(())
}

fn process_request(
    request: Request,
    cache: &RuleCache,
    display_handler: Option<&DisplayHandler>,
) -> String {
    match process_request_inner(request, cache, display_handler) {
        Ok(response) => response,
        Err(error) => error_response(&error),
    }
}

fn process_request_inner(
    request: Request,
    cache: &RuleCache,
    display_handler: Option<&DisplayHandler>,
) -> Result<String, ServerError> {
    let command = request.command.expect("validated request has a command");
    let filter_system = resolve_filter(request.filters, cache)?;
    match command {
        Command::Help => return Ok(help_response(request.help_on.as_deref())),
        Command::DisplayCodecs => return Ok(codec_response()),
        Command::Version => return Ok(version_response()),
        Command::Display => {
            if let Some(handler) = display_handler {
                handler(DisplayRequest {
                    graph: request.graph,
                    name: request.graph_name,
                })
                .map_err(|error| {
                    ServerError::new(
                        GRAPH_DRAWING_ERROR,
                        format!("An error occurred while drawing the graph.\n{error}"),
                    )
                })?;
            }
            return Ok("<result code='0' />\n".to_owned());
        }
        _ => {}
    }
    let parsed = request.graph.expect("validated input command has a graph");
    if command == Command::Convert {
        return convert_response(&parsed, request.output_codec);
    }
    if command == Command::Classify {
        return Ok(classify_response(&parsed));
    }
    let fragments = parsed.fragment_count();
    let graph = HncGraph::try_from(parsed).map_err(|error| {
        ServerError::new(
            SOLVER_NOT_APPLICABLE,
            format!("The solver is not applicable to this graph: {error}"),
        )
    })?;
    if command == Command::Solvable && request.nochart {
        let started = Instant::now();
        let solvable = is_solvable(&graph);
        return Ok(format!(
            "<result solvable='{solvable}' fragments='{fragments}' time='{}' />\n",
            elapsed_ms(started)
        ));
    }
    let started = Instant::now();
    let mut chart = solve(&graph).map_err(|error| {
        ServerError::new(
            SOLVER_NOT_APPLICABLE,
            format!("The solver is not applicable to this graph: {error}"),
        )
    })?;
    let chart_ms = elapsed_ms(started);
    let solvable = chart.count_solutions() != 0_u8.into();
    if let Some(system) = filter_system {
        chart = filter_chart(&chart, &system, || false)
            .map_err(|error| ServerError::new(FILTER_ERROR, error.to_string()))?;
    }
    if command == Command::Solvable {
        return Ok(format!(
            "<result solvable='{solvable}' fragments='{fragments}' count='{}' chartsize='{}' time='{chart_ms}' />\n",
            chart.count_solutions(),
            chart.split_count()
        ));
    }
    solve_response(
        &chart,
        solvable,
        fragments,
        chart_ms,
        request.limit,
        request.output_codec,
    )
}

fn resolve_filter(
    requested: Vec<FilterRequest>,
    cache: &RuleCache,
) -> Result<Option<RewriteSystem>, ServerError> {
    let mut selected = None;
    for request in requested {
        selected = Some(match request {
            FilterRequest::Rules(system) => {
                *cache.write().map_err(|_| {
                    ServerError::new(FILTER_ERROR, "The filtering rule cache is unavailable.")
                })? = Some(system.clone());
                system
            }
            FilterRequest::Cached => cache
                .read()
                .map_err(|_| {
                    ServerError::new(FILTER_ERROR, "The filtering rule cache is unavailable.")
                })?
                .clone()
                .ok_or_else(|| {
                    ServerError::new(
                        FILTER_ERROR,
                        "You specified the 'filter' option without specifying the rules.",
                    )
                })?,
        });
    }
    Ok(selected)
}

fn elapsed_ms(started: Instant) -> u128 {
    started.elapsed().as_millis()
}

fn solve_response(
    chart: &crate::Chart,
    solvable: bool,
    fragments: usize,
    chart_ms: u128,
    limit: usize,
    codec: Option<OutputCodec>,
) -> Result<String, ServerError> {
    if !solvable {
        return Ok(format!(
            "<result solvable='false' count='0' fragments='{fragments}' chartsize='{}' time-chart='{chart_ms}' />\n",
            chart.split_count()
        ));
    }
    let extraction_started = Instant::now();
    let mut encoded = Vec::new();
    let mut count = 0_usize;
    let mut solutions = chart.solutions();
    while count < limit && solutions.advance() {
        count += 1;
        if let Some(codec) = codec {
            let mut value = Vec::new();
            codec.write_single_solution_at(&solutions.current().expect("advance produced a solution"), count, &mut value)
                .map_err(|error| ServerError::new(OUTPUT_ERROR, format!("Output of the solved forms of this graph is not supported by this output codec.\n{error}")))?;
            encoded.extend_from_slice(b"  <solution string='");
            escape_xml_bytes(&value, &mut encoded);
            encoded.extend_from_slice(b"' />\n");
        }
    }
    let extraction_ms = elapsed_ms(extraction_started);
    let mut response = format!("<result solvable='true' count='{count}' fragments='{fragments}' chartsize='{}' time-chart='{chart_ms}' time-extraction='{extraction_ms}' >\n", chart.split_count()).into_bytes();
    response.extend(encoded);
    response.extend_from_slice(b"</result>\n");
    String::from_utf8(response).map_err(|error| ServerError::new(IO_ERROR, error.to_string()))
}

fn convert_response(
    graph: &ParsedGraph,
    codec: Option<OutputCodec>,
) -> Result<String, ServerError> {
    let Some(codec) = codec else {
        return Ok("<result />\n".to_owned());
    };
    let encoder = codec.graph_encoder().ok_or_else(|| {
        ServerError::new(
            OUTPUT_ERROR,
            "This graph is not supported by the specified output codec.",
        )
    })?;
    let mut value = Vec::new();
    encoder.write_graph(graph, &mut value).map_err(|error| {
        ServerError::new(
            OUTPUT_ERROR,
            format!("This graph is not supported by the specified output codec.\n{error}"),
        )
    })?;
    let mut response = b"<result usr='".to_vec();
    escape_xml_bytes(&value, &mut response);
    response.extend_from_slice(b"' />\n");
    String::from_utf8(response).map_err(|error| ServerError::new(IO_ERROR, error.to_string()))
}

fn classify_response(graph: &ParsedGraph) -> String {
    let started = Instant::now();
    let weakly_normal = graph.is_weakly_normal();
    let normal = graph.is_normal();
    let compact = graph.is_compact();
    let compactifiable = graph.is_compactifiable();
    let hnc = graph.is_hypernormally_connected();
    let leaf_labelled = graph.is_leaf_labelled();
    let code = u8::from(weakly_normal)
        | (u8::from(normal) << 1)
        | (u8::from(compact) << 2)
        | (u8::from(compactifiable) << 3)
        | (u8::from(hnc) << 4)
        | (u8::from(leaf_labelled) << 5);
    let time = elapsed_ms(started);
    format!(
        "<result code='{code}' time2='{time}' weaklynormal='{weakly_normal}' normal='{normal}' compact='{compact}' compactifiable='{compactifiable}' hypernormallyconnected='{hnc}' leaflabelled='{leaf_labelled}' />\n"
    )
}

fn help_response(on: Option<&str>) -> String {
    let text = match on.and_then(Command::parse) {
        Some(command) => format!(
            "utool {}: {}.\n",
            on.unwrap_or_default(),
            command_description(command)
        ),
        None => "\nUtool is the Swiss Army Knife of Underspecification (Java version).\nFor more information, see github.com/coli-saar/utool/".to_owned(),
    };
    format!("<result help='{}' />\n", escape_xml(&text))
}

const fn command_description(command: Command) -> &'static str {
    match command {
        Command::Solve => "Solve an underspecified description",
        Command::Solvable => "Check solvability without enumerating solutions",
        Command::Convert => "Convert underspecified description from one format to another",
        Command::Classify => "Check whether a description belongs to special classes",
        Command::Display => "Display an underspecified description",
        Command::Help => "Display help on a command",
        Command::DisplayCodecs | Command::Version => "Display information",
    }
}

fn version_response() -> String {
    let version = format!(
        "Utool (The Swiss Army Knife of Underspecification), version {}\n(running in server mode)\nCreated by the CHORUS project, SFB 378, Saarland University\n\n",
        env!("CARGO_PKG_VERSION")
    );
    format!("<result version='{}' />\n", escape_xml(&version))
}

fn codec_response() -> String {
    const INPUTS: &[(&str, &str)] = &[
        ("chain", ""),
        ("domcon-oz", ".clls"),
        ("domgraph-gxl", ".dg.xml"),
        ("holesem-comsem", ".hs.pl"),
        ("mrs-prolog", ".mrs.pl"),
        ("mrs-xml", ".mrs.xml"),
    ];
    const OUTPUTS: &[(&str, &str)] = &[
        ("domcon-oz", ".clls"),
        ("domgraph-gxl", ".dg.xml"),
        ("domgraph-dot", ".dg.dot"),
        ("domgraph-udraw", ".dg.udg"),
        ("plugging-oz", ".plug.oz"),
        ("plugging-lkb", ".lkbplug.lisp"),
        ("term-oz", ".t.oz"),
        ("term-prolog", ".t.pl"),
        ("domgraph-codegen", ".java"),
        ("plugging-groovy", ""),
    ];
    let mut response = String::from("<result>\n");
    for &(name, extension) in INPUTS {
        response.push_str(&codec_element(name, extension, "input"));
    }
    for &(name, extension) in OUTPUTS {
        response.push_str(&codec_element(name, extension, "output"));
    }
    response.push_str("</result>\n");
    response
}

fn codec_element(name: &str, extension: &str, kind: &str) -> String {
    if extension.is_empty() {
        format!("  <codec name='{name}' type='{kind}' />\n")
    } else {
        format!("  <codec name='{name}' extension='{extension}' type='{kind}' />\n")
    }
}

fn error_response(error: &ServerError) -> String {
    format!(
        "<error code='{}' explanation='{}' />\n",
        error.code,
        escape_xml(&error.explanation)
    )
}

fn escape_xml(value: &str) -> String {
    let mut output = Vec::with_capacity(value.len());
    escape_xml_bytes(value.as_bytes(), &mut output);
    String::from_utf8(output).expect("escaping UTF-8 preserves UTF-8")
}

fn escape_xml_bytes(value: &[u8], output: &mut Vec<u8>) {
    for &byte in value {
        output.extend_from_slice(match byte {
            b'&' => b"&amp;",
            b'<' => b"&lt;",
            b'>' => b"&gt;",
            b'\'' => b"&apos;",
            b'\"' => b"&quot;",
            _ => {
                output.push(byte);
                continue;
            }
        });
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::io::{Read, Write};

    fn request(xml: &str, cache: &RuleCache) -> String {
        let mut input = io::Cursor::new(xml.as_bytes());
        match parse_request(&mut input) {
            Ok(request) => process_request(request, cache, None),
            Err(error) => error_response(&error),
        }
    }

    #[test]
    fn solves_filters_and_reuses_rules() {
        let cache = Arc::new(RwLock::new(None));
        let graph = "[label(x1 a(x2 x3)) label(y1 a(y2 y3)) label(z1 foo) label(z2 bar) label(z3 baz) dom(x2 z1) dom(y2 z2) dom(x3 z3) dom(y3 z3)]";
        let unfiltered = request(
            &format!("<utool cmd='solvable'><usr codec='domcon-oz' string='{graph}'/></utool>"),
            &cache,
        );
        assert!(unfiltered.contains("count='2'"), "{unfiltered}");
        let rules = "a#1(X,a#2(Y,Z)) = a#2(Y,a#1(X,Z))";
        let filtered = request(
            &format!(
                "<utool cmd='solvable'><usr codec='domcon-oz' string='{graph}'/><filter rules='{rules}'/></utool>"
            ),
            &cache,
        );
        assert!(filtered.contains("count='1'"), "{filtered}");
        let cached = request(
            &format!(
                "<utool cmd='solvable'><usr codec='domcon-oz' string='{graph}'/><filter/></utool>"
            ),
            &cache,
        );
        assert!(cached.contains("count='1'"), "{cached}");
    }

    #[test]
    fn reports_protocol_errors_as_xml() {
        let cache = Arc::new(RwLock::new(None));
        assert!(request("<utool/>", &cache).starts_with("<error code='141'"));
        let missing = request("<utool cmd='solve'/>", &cache);
        assert!(missing.starts_with("<error code='150'"), "{missing}");
        let no_rules = request(
            "<utool cmd='solvable'><usr codec='domcon-oz' string='[label(x a)]'/><filter/></utool>",
            &cache,
        );
        assert!(no_rules.starts_with("<error code='170'"), "{no_rules}");
    }

    #[test]
    fn supports_the_java_protocol_operations_and_escaping() {
        let cache = Arc::new(RwLock::new(None));
        let graph = "[label(x f(x1)) label(y &apos;&amp;&apos;) dom(x1 y)]";
        let solved = request(
            &format!(
                "<utool cmd='solve' output-codec='term-prolog'><usr codec='domcon-oz' string='{graph}'/></utool>"
            ),
            &cache,
        );
        assert!(solved.contains("solvable='true'"), "{solved}");
        assert!(
            solved.contains("<solution string='f(&amp;)' />"),
            "{solved}"
        );

        let converted = request(
            &format!(
                "<utool cmd='convert' output-codec='domcon-oz'><usr codec='domcon-oz' string='{graph}'/></utool>"
            ),
            &cache,
        );
        assert!(
            converted.contains("label(y &apos;&amp;&apos;)"),
            "{converted}"
        );

        let classified = request(
            &format!("<utool cmd='classify'><usr codec='domcon-oz' string='{graph}'/></utool>"),
            &cache,
        );
        assert!(classified.contains("code='63'"), "{classified}");
        assert!(classified.contains("compactifiable='true'"), "{classified}");

        let nochart = request(
            &format!(
                "<utool cmd='solvable' nochart='true'><usr codec='domcon-oz' string='{graph}'/></utool>"
            ),
            &cache,
        );
        assert!(nochart.contains("solvable='true'"), "{nochart}");
        assert!(!nochart.contains("count="), "{nochart}");
        assert!(!nochart.contains("chartsize="), "{nochart}");

        for response in [
            request("<utool cmd='display-codecs'/>", &cache),
            request("<utool cmd='version'/>", &cache),
            request("<utool cmd='help'/>", &cache),
            request("<utool cmd='display'/>", &cache),
        ] {
            let mut xml = Reader::from_str(&response);
            loop {
                match xml.read_event() {
                    Ok(Event::Eof) => break,
                    Ok(_) => {}
                    Err(error) => panic!("invalid response {response:?}: {error}"),
                }
            }
        }
    }

    #[test]
    fn responds_after_the_closing_tag_without_waiting_for_client_eof() {
        let listener = TcpListener::bind(("127.0.0.1", 0)).unwrap();
        let address = listener.local_addr().unwrap();
        let cache = Arc::new(RwLock::new(None));
        let server_cache = Arc::clone(&cache);
        let server = thread::spawn(move || {
            let (stream, _) = listener.accept().unwrap();
            serve_connection(stream, &server_cache, &None, None).unwrap();
        });
        let mut client = TcpStream::connect(address).unwrap();
        client
            .write_all(
                b"<utool cmd='solvable'><usr codec='domcon-oz' string='[label(x a)]'/></utool>",
            )
            .unwrap();
        client.flush().unwrap();
        let mut response = String::new();
        client.read_to_string(&mut response).unwrap();
        assert!(
            response.starts_with("<result solvable='true'"),
            "{response}"
        );
        server.join().unwrap();
    }

    #[test]
    fn display_requests_are_forwarded_to_an_embedded_handler() {
        let cache = Arc::new(RwLock::new(None));
        let displayed = Arc::new(AtomicBool::new(false));
        let handler_displayed = Arc::clone(&displayed);
        let handler: DisplayHandler = Arc::new(move |request| {
            assert!(request.graph.is_some());
            assert_eq!(request.name.as_deref(), Some("Named graph"));
            handler_displayed.store(true, Ordering::SeqCst);
            Ok(())
        });
        let mut input = io::Cursor::new(
            b"<utool cmd='display'><usr name='Named graph' codec='domcon-oz' string='[label(x a)]'/></utool>",
        );
        let response = process_request(parse_request(&mut input).unwrap(), &cache, Some(&handler));
        assert_eq!(response, "<result code='0' />\n");
        assert!(displayed.load(Ordering::SeqCst));

        let failing: DisplayHandler = Arc::new(|_| Err("window creation failed".to_owned()));
        let mut input = io::Cursor::new(b"<utool cmd='display'/>");
        let response = process_request(parse_request(&mut input).unwrap(), &cache, Some(&failing));
        assert!(response.starts_with("<error code='130'"), "{response}");
    }

    #[test]
    fn background_server_can_be_stopped_and_releases_its_port() {
        let server = start(
            ServerConfig {
                bind_address: Ipv4Addr::LOCALHOST.into(),
                port: 0,
                logging: ServerLogging::Disabled,
                warmup: false,
            },
            None,
        )
        .unwrap();
        let address = server.address();
        let mut client = TcpStream::connect(address).unwrap();
        client.write_all(b"<utool cmd='version'/>").unwrap();
        client.flush().unwrap();
        let mut response = String::new();
        client.read_to_string(&mut response).unwrap();
        assert!(response.starts_with("<result version="), "{response}");

        server.stop().unwrap();
        let rebound = TcpListener::bind(address).unwrap();
        drop(rebound);
    }

    #[test]
    fn wildcard_and_localhost_listeners_cannot_share_a_port() {
        // Use standard listeners to represent another process, including an
        // older Utool whose socket may have address reuse enabled.
        let wildcard = TcpListener::bind((Ipv4Addr::UNSPECIFIED, 0)).unwrap();
        let port = wildcard.local_addr().unwrap().port();
        let conflict = start(
            ServerConfig {
                bind_address: Ipv4Addr::LOCALHOST.into(),
                port,
                logging: ServerLogging::Disabled,
                warmup: false,
            },
            None,
        );
        assert!(
            matches!(conflict, Err(error) if error.kind() == io::ErrorKind::AddrInUse),
            "localhost unexpectedly shared port {port} with a wildcard listener"
        );
        drop(wildcard);

        let localhost = TcpListener::bind((Ipv4Addr::LOCALHOST, 0)).unwrap();
        let port = localhost.local_addr().unwrap().port();
        let conflict = start(
            ServerConfig {
                bind_address: Ipv4Addr::UNSPECIFIED.into(),
                port,
                logging: ServerLogging::Disabled,
                warmup: false,
            },
            None,
        );
        assert!(
            matches!(conflict, Err(error) if error.kind() == io::ErrorKind::AddrInUse),
            "wildcard listener unexpectedly shared port {port} with localhost"
        );
        drop(localhost);
    }
}
