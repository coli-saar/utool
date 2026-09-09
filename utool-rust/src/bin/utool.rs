use std::{
    env,
    fmt::Write as _,
    fs,
    io::{self, BufWriter, Read, Write},
    process::{Command, ExitCode},
    sync::Arc,
    time::{Duration, Instant},
};
use utool::{
    Chart, HncGraph, InputCodec, OutputCodec, ParsedGraph, RewriteSystem, filter_chart,
    is_solvable, solve_shared,
};

const IO_ERROR: u8 = 128;
const NO_INPUT: u8 = 150;
const NO_INPUT_CODEC: u8 = 151;
const NO_SUCH_INPUT_CODEC: u8 = 152;
const SOLVER_NOT_APPLICABLE: u8 = 153;
const NO_SUCH_OUTPUT_CODEC: u8 = 161;
const FILTER_ERROR: u8 = 170;
const PARSE_ERROR: u8 = 192;

#[derive(Clone, Copy, PartialEq, Eq)]
enum Operation {
    Solve,
    Solvable,
    Convert,
    Classify,
    Display,
    Server,
    Help,
}

#[allow(clippy::struct_excessive_bools)]
struct Options {
    input_codec: Option<String>,
    output_codec: Option<String>,
    default_output_codec: String,
    output: Option<String>,
    filter: Option<String>,
    statistics: bool,
    no_output: bool,
    nochart: bool,
    dump_chart: bool,
    limit: Option<usize>,
    help: bool,
    codec_options_help: bool,
    display_codecs: bool,
    version: bool,
    port: Option<u16>,
    logging: LoggingOption,
    positional: Vec<String>,
}

#[derive(Default)]
enum LoggingOption {
    #[default]
    Disabled,
    Stderr,
    File(String),
}

impl Default for Options {
    fn default() -> Self {
        Self {
            input_codec: None,
            output_codec: None,
            default_output_codec: "domcon-oz".to_owned(),
            output: None,
            filter: None,
            statistics: false,
            no_output: false,
            nochart: false,
            dump_chart: false,
            limit: None,
            help: false,
            codec_options_help: false,
            display_codecs: false,
            version: false,
            port: None,
            logging: LoggingOption::Disabled,
            positional: Vec::new(),
        }
    }
}

fn fail(message: impl AsRef<str>, code: u8) -> ExitCode {
    let message = message.as_ref();
    let mut lines = message.lines();
    eprintln!("Error: {}", lines.next().unwrap_or("Utool failed."));
    for line in lines {
        eprintln!("{line}");
    }
    ExitCode::from(code)
}

fn detailed_error(
    summary: &str,
    context: impl IntoIterator<Item = (&'static str, String)>,
    detail: impl std::fmt::Display,
) -> String {
    let mut message = summary.to_owned();
    for (label, value) in context {
        let _ = write!(message, "\n  {label}: {value}");
    }
    let detail = detail.to_string();
    if !detail.trim().is_empty() {
        message.push_str("\n\nDetails:\n");
        for line in detail.lines() {
            message.push_str("  ");
            message.push_str(line);
            message.push('\n');
        }
        message.pop();
    }
    message
}

fn take_value(
    args: &[String],
    index: &mut usize,
    attached: Option<&str>,
    option: &str,
) -> Result<String, String> {
    if let Some(value) = attached {
        return Ok(value.to_owned());
    }
    *index += 1;
    args.get(*index)
        .cloned()
        .ok_or_else(|| format!("Option {option} requires an argument."))
}

fn options(args: &[String]) -> Result<Options, String> {
    let mut result = Options::default();
    let mut index = 0;
    while index < args.len() {
        let argument = &args[index];
        let (name, attached) = argument.strip_prefix("--").map_or(("", None), |long| {
            long.split_once('=')
                .map_or((long, None), |(n, v)| (n, Some(v)))
        });
        match argument.as_str() {
            "-I" => result.input_codec = Some(take_value(args, &mut index, None, "-I")?),
            "-O" => result.output_codec = Some(take_value(args, &mut index, None, "-O")?),
            "-o" => result.output = Some(take_value(args, &mut index, None, "-o")?),
            "-f" => result.filter = Some(take_value(args, &mut index, None, "-f")?),
            "-p" => {
                result.port = Some(
                    take_value(args, &mut index, None, "-p")?
                        .parse()
                        .map_err(|_| "-p requires a TCP port between 0 and 65535".to_owned())?,
                );
            }
            "-l" => {
                let value = args
                    .get(index + 1)
                    .filter(|value| !value.starts_with('-'))
                    .cloned();
                if value.is_some() {
                    index += 1;
                }
                result.logging = value.map_or(LoggingOption::Stderr, LoggingOption::File);
            }
            "-s" => result.statistics = true,
            "-n" => result.no_output = true,
            "-h" => result.help = true,
            "-d" => result.display_codecs = true,
            _ if !name.is_empty() => match name {
                "input-codec" => {
                    result.input_codec =
                        Some(take_value(args, &mut index, attached, "--input-codec")?);
                }
                "output-codec" => {
                    result.output_codec =
                        Some(take_value(args, &mut index, attached, "--output-codec")?);
                }
                "output" => {
                    result.output = Some(take_value(args, &mut index, attached, "--output")?);
                }
                "filter" => {
                    result.filter = Some(take_value(args, &mut index, attached, "--filter")?);
                }
                "port" => {
                    result.port = Some(
                        take_value(args, &mut index, attached, "--port")?
                            .parse()
                            .map_err(|_| {
                                "--port requires a TCP port between 0 and 65535".to_owned()
                            })?,
                    );
                }
                "logging" => {
                    let value = attached.map(str::to_owned).or_else(|| {
                        args.get(index + 1)
                            .filter(|value| !value.starts_with('-'))
                            .cloned()
                    });
                    if attached.is_none() && value.is_some() {
                        index += 1;
                    }
                    result.logging = value.map_or(LoggingOption::Stderr, LoggingOption::File);
                }
                // Accepted for command-line compatibility with Java Utool.
                "warmup" => {}
                "limit" => {
                    result.limit = Some(
                        take_value(args, &mut index, attached, "--limit")?
                            .parse()
                            .map_err(|_| "--limit requires a nonnegative integer".to_owned())?,
                    );
                }
                "input-codec-options" | "output-codec-options" => {
                    let _ = take_value(args, &mut index, attached, argument)?;
                }
                "display-statistics" => result.statistics = true,
                "no-output" => result.no_output = true,
                "nochart" => result.nochart = true,
                "dump-chart" => result.dump_chart = true,
                "help" => result.help = true,
                "help-options" => result.codec_options_help = true,
                "display-codecs" => result.display_codecs = true,
                "version" => result.version = true,
                _ => return Err(format!("Unknown option: {argument}")),
            },
            _ => result.positional.push(argument.clone()),
        }
        index += 1;
    }
    Ok(result)
}

fn operation(name: Option<&String>) -> Option<Operation> {
    match name.map(String::as_str) {
        Some("solve") => Some(Operation::Solve),
        Some("solvable") => Some(Operation::Solvable),
        Some("convert") => Some(Operation::Convert),
        Some("classify") => Some(Operation::Classify),
        Some("display") => Some(Operation::Display),
        Some("server") => Some(Operation::Server),
        Some("help") => Some(Operation::Help),
        _ => None,
    }
}

fn print_help(command: Option<&str>) {
    match command {
        Some(command @ ("solve" | "solvable" | "convert" | "classify")) => {
            let description = match command {
                "solve" => "Solve an underspecified description",
                "solvable" => "Check solvability without enumerating solutions",
                "convert" => "Convert underspecified description from one format to another",
                "classify" => "Check whether a description belongs to special classes",
                _ => unreachable!(),
            };
            eprintln!("utool {command}: {description}.");
            eprintln!("Usage: utool {command} [options] [input-source]");
        }
        Some("display") => {
            eprintln!("utool display: Start the Underspecification Workbench GUI.");
            eprintln!("Usage: utool display [input-source]");
        }
        Some("server") => {
            eprintln!("utool server: Start Utool in server mode.");
            eprintln!("Usage: utool server [options]");
            eprintln!(
                "\nServer options:\n  --port, -p <port>          Accept connections on this port (default: 2802)\n  --logging, -l [filename]   Log traffic to a file, or stderr when omitted"
            );
        }
        Some("help") => {
            eprintln!("utool help: Display help on a command.");
            eprintln!("Usage: utool help [command]");
        }
        Some(_) | None => {
            eprintln!("Usage: utool <subcommand> [options] [args]");
            eprintln!(
                "Type `utool help <subcommand>' for help on a specific subcommand.\n\nAvailable subcommands:\n    solve        Solve an underspecified description.\n    solvable     Check solvability without enumerating solutions.\n    convert      Convert underspecified description from one format to another.\n    classify     Check whether a description belongs to special classes.\n    display      Start the Underspecification Workbench GUI.\n    server       Start Utool in server mode.\n    help         Display help on a command."
            );
        }
    }
}

fn write_empty_solution_list(
    opts: &Options,
    codec: Option<OutputCodec>,
) -> Result<(), (String, u8)> {
    let Some(codec) = codec else {
        return Ok(());
    };
    let mut writer = result_writer(opts)?;
    let mut encoder = codec
        .solution_encoder()
        .expect("solution codec capability was checked above");
    encoder
        .begin(&mut writer)
        .and_then(|()| encoder.finish(&mut writer))
        .and_then(|()| writer.flush())
        .map_err(|error| {
            (
                detailed_error(
                    "Could not write the empty solution list.",
                    [("Output", output_destination(opts))],
                    error,
                ),
                IO_ERROR,
            )
        })
}

fn output_destination(opts: &Options) -> String {
    opts.output
        .clone()
        .unwrap_or_else(|| "standard output".to_owned())
}

fn read_graph(opts: &Options, source: &str) -> Result<ParsedGraph, (String, u8)> {
    let selected = if let Some(name) = &opts.input_codec {
        InputCodec::from_name(name)
            .ok_or_else(|| (format!("Unknown input codec: {name}"), NO_SUCH_INPUT_CODEC))?
    } else {
        InputCodec::from_filename(source).ok_or_else(|| {
            (
                "You must specify an input codec!".to_owned(),
                NO_INPUT_CODEC,
            )
        })?
    };
    let mut text = String::new();
    if selected == InputCodec::Chain {
        text.push_str(source);
    } else if source == "-" {
        io::stdin().read_to_string(&mut text).map_err(|error| {
            (
                detailed_error(
                    "Could not read the input graph from standard input.",
                    [("Codec", selected.name().to_owned())],
                    error,
                ),
                IO_ERROR,
            )
        })?;
    } else {
        text = fs::read_to_string(source).map_err(|error| {
            (
                detailed_error(
                    "Could not read the input graph.",
                    [
                        ("Input", source.to_owned()),
                        ("Codec", selected.name().to_owned()),
                    ],
                    error,
                ),
                IO_ERROR,
            )
        })?;
    }
    selected.parse(&text).map_err(|error| {
        let code =
            if selected == InputCodec::Chain && matches!(&error, utool::CodecError::Semantic(_)) {
                PARSE_ERROR + 1
            } else {
                PARSE_ERROR
            };
        (
            detailed_error(
                "Could not parse the input graph.",
                [
                    ("Input", source.to_owned()),
                    ("Codec", selected.name().to_owned()),
                    ("Input size", format!("{} bytes", text.len())),
                ],
                error,
            ),
            code,
        )
    })
}

fn output_codec(opts: &Options, input_name: Option<&str>) -> Result<OutputCodec, (String, u8)> {
    if let Some(name) = &opts.output_codec {
        return OutputCodec::from_name(name).ok_or_else(|| {
            (
                format!("Unknown output codec: {name}"),
                NO_SUCH_OUTPUT_CODEC,
            )
        });
    }
    if let Some(path) = &opts.output {
        if let Some(codec) = OutputCodec::from_filename(path) {
            return Ok(codec);
        }
    }
    let input_codec = opts
        .input_codec
        .as_deref()
        .and_then(InputCodec::from_name)
        .or_else(|| input_name.and_then(InputCodec::from_filename));
    if let Some(codec) = input_codec.and_then(|codec| OutputCodec::from_name(codec.name())) {
        return Ok(codec);
    }
    OutputCodec::from_name(&opts.default_output_codec).ok_or_else(|| {
        (
            format!(
                "Unknown default output codec in ~/.utool: {}",
                opts.default_output_codec
            ),
            NO_SUCH_OUTPUT_CODEC,
        )
    })
}

fn result_writer(opts: &Options) -> Result<BufWriter<Box<dyn Write>>, (String, u8)> {
    let writer: Box<dyn Write> = if let Some(path) = &opts.output {
        Box::new(fs::File::create(path).map_err(|error| {
            (
                detailed_error(
                    "Could not create the output file.",
                    [("Output", path.clone())],
                    error,
                ),
                IO_ERROR,
            )
        })?)
    } else {
        Box::new(io::stdout())
    };
    Ok(BufWriter::new(writer))
}

fn format_duration(duration: Duration) -> String {
    let nanos = duration.as_nanos();
    if nanos < 1_000 {
        format!("{nanos} ns")
    } else if nanos < 1_000_000 {
        format!("{:.3} µs", duration.as_secs_f64() * 1_000_000.0)
    } else if nanos < 1_000_000_000 {
        format!("{:.3} ms", duration.as_secs_f64() * 1_000.0)
    } else {
        format!("{:.3} s", duration.as_secs_f64())
    }
}

fn report_chart_phase(name: &str, time_label: &str, chart: &Chart, duration: Duration) {
    eprintln!("{name}");
    eprintln!(
        "  Chart size:    {} states, {} rules",
        chart.state_count(),
        chart.rule_count()
    );
    eprintln!("  Language size: {} solutions", chart.count_solutions());
    eprintln!(
        "  {time_label}: {duration}\n",
        duration = format_duration(duration)
    );
}

#[allow(clippy::too_many_lines)]
fn execute(opts: &Options, op: Operation, source: &str) -> Result<u8, (String, u8)> {
    let parsed = read_graph(opts, source)?;
    let solve_output_codec = if op == Operation::Solve && !opts.no_output {
        let codec = output_codec(opts, Some(source))?;
        if !codec.supports_solutions() {
            return Err((
                "This output codec doesn't support the printing of multiple solutions!".to_owned(),
                162,
            ));
        }
        Some(codec)
    } else {
        None
    };
    if op == Operation::Convert {
        let codec = output_codec(opts, Some(source))?;
        let Some(encoder) = codec.graph_encoder() else {
            return Err((
                "This graph is not supported by the specified output codec.".to_owned(),
                225,
            ));
        };
        if !opts.no_output {
            let mut writer = result_writer(opts)?;
            encoder
                .write_graph(&parsed, &mut writer)
                .and_then(|()| writer.flush())
                .map_err(|error| {
                    (
                        detailed_error(
                            "Could not write the converted graph.",
                            [
                                ("Output", output_destination(opts)),
                                ("Codec", codec.name().to_owned()),
                            ],
                            error,
                        ),
                        IO_ERROR,
                    )
                })?;
        }
        return Ok(0);
    }
    if op == Operation::Classify {
        let weakly_normal = parsed.is_weakly_normal();
        let normal = parsed.is_normal();
        let compact = parsed.is_compact();
        let compactifiable = parsed.is_compactifiable();
        let hnc = parsed.is_hypernormally_connected();
        let leaf_labelled = parsed.is_leaf_labelled();
        if opts.statistics {
            eprintln!(
                "The input graph is {}weakly normal.",
                if weakly_normal { "" } else { "not " }
            );
            eprintln!(
                "The input graph is {}normal.",
                if normal { "" } else { "not " }
            );
            eprintln!(
                "The input graph is {}compact.",
                if compact { "" } else { "not " }
            );
            eprintln!(
                "The input graph is {}compactifiable.",
                if compactifiable { "" } else { "not " }
            );
            eprintln!(
                "The graph is {}hypernormally connected.",
                if hnc { "" } else { "not " }
            );
            eprintln!(
                "The graph is {}leaf-labelled.",
                if leaf_labelled { "" } else { "not " }
            );
        }
        return Ok(u8::from(weakly_normal)
            | (u8::from(normal) << 1)
            | (u8::from(compact) << 2)
            | (u8::from(compactifiable) << 3)
            | (u8::from(hnc) << 4)
            | (u8::from(leaf_labelled) << 5));
    }
    let Some(parsed) = parsed.preprocess_for_solver().map_err(|error| {
        (
            detailed_error(
                "The solver is not applicable to the input graph.",
                [("Input", source.to_owned())],
                error,
            ),
            SOLVER_NOT_APPLICABLE,
        )
    })?
    else {
        if opts.statistics {
            eprintln!("The graph has trivially unsolvable dominance edges within fragments.");
        }
        if op == Operation::Solve && !opts.no_output {
            write_empty_solution_list(opts, solve_output_codec)?;
        }
        return Ok(0);
    };
    let graph = Arc::new(HncGraph::try_from(parsed).map_err(|error| {
        (
            detailed_error(
                "The solver is not applicable to the input graph.",
                [("Input", source.to_owned())],
                error,
            ),
            SOLVER_NOT_APPLICABLE,
        )
    })?);
    if op == Operation::Solvable && !opts.statistics && opts.filter.is_none() && !opts.dump_chart {
        return Ok(u8::from(is_solvable(&graph)));
    }
    let started = Instant::now();
    let mut chart = solve_shared(Arc::clone(&graph)).map_err(|error| {
        (
            detailed_error(
                "Could not construct the solution chart.",
                [("Input", source.to_owned())],
                error,
            ),
            SOLVER_NOT_APPLICABLE,
        )
    })?;
    let chart_duration = started.elapsed();
    let solvable = chart.count_solutions() != 0u8.into();
    if opts.statistics {
        report_chart_phase(
            "Chart construction",
            "Time to build chart",
            &chart,
            chart_duration,
        );
    }
    if let Some(path) = &opts.filter {
        let rules = fs::read_to_string(path).map_err(|error| {
            (
                detailed_error(
                    "Could not read the filtering rules.",
                    [("Filter", path.clone())],
                    error,
                ),
                FILTER_ERROR,
            )
        })?;
        let system = RewriteSystem::parse(&rules).map_err(|error| {
            (
                detailed_error(
                    "Could not parse the filtering rules.",
                    [
                        ("Filter", path.clone()),
                        ("Input size", format!("{} bytes", rules.len())),
                    ],
                    error.format_with_source(&rules),
                ),
                FILTER_ERROR,
            )
        })?;
        let filtering_started = Instant::now();
        chart = filter_chart(&chart, &system, || false).map_err(|error| {
            (
                detailed_error(
                    "Could not filter the solution chart.",
                    [("Filter", path.clone())],
                    error,
                ),
                FILTER_ERROR,
            )
        })?;
        if opts.statistics {
            report_chart_phase(
                "Filtering",
                "Time to filter chart",
                &chart,
                filtering_started.elapsed(),
            );
        }
    }
    if opts.statistics {
        eprintln!(
            "Solving graph ... {}.",
            if solvable {
                "it is solvable"
            } else {
                "it is unsolvable"
            }
        );
        eprintln!("Number of solutions: {}\n", chart.count_solutions());
    }
    if !solvable {
        return Ok(0);
    }
    if opts.dump_chart {
        let display = utool::ChartDisplay::new(&chart);
        let page = display.rule_page(&chart, 0, display.row_count());
        for rule in page.rules {
            let state = page
                .states
                .iter()
                .find(|state| state.state == rule.state)
                .expect("every displayed rule defines its state");
            eprintln!("[{}] => {}", state.subgraph.join(", "), rule.fragment);
        }
    }
    if op == Operation::Solve {
        let enumeration_started = Instant::now();
        let mut count = 0_usize;
        let limit = opts.limit.unwrap_or(usize::MAX);
        if let Some(codec) = solve_output_codec {
            let mut writer = result_writer(opts)?;
            let mut encoder = codec
                .solution_encoder()
                .expect("solution codec capability was checked above");
            encoder.begin(&mut writer).map_err(|error| {
                (
                    detailed_error(
                        "Could not begin writing solutions.",
                        [
                            ("Output", output_destination(opts)),
                            ("Codec", codec.name().to_owned()),
                        ],
                        error,
                    ),
                    IO_ERROR,
                )
            })?;
            let mut solutions = chart.solutions();
            while count < limit && solutions.advance() {
                let solution = solutions.current().expect("advance produced a solution");
                encoder
                    .write_solution(&solution, &mut writer)
                    .map_err(|error| {
                        (
                            detailed_error(
                                "Could not write a solution.",
                                [
                                    ("Output", output_destination(opts)),
                                    ("Codec", codec.name().to_owned()),
                                    ("Solution", (count + 1).to_string()),
                                ],
                                error,
                            ),
                            IO_ERROR,
                        )
                    })?;
                count += 1;
            }
            encoder
                .finish(&mut writer)
                .and_then(|()| writer.flush())
                .map_err(|error| {
                    (
                        detailed_error(
                            "Could not finish writing solutions.",
                            [
                                ("Output", output_destination(opts)),
                                ("Codec", codec.name().to_owned()),
                                ("Solutions written", count.to_string()),
                            ],
                            error,
                        ),
                        IO_ERROR,
                    )
                })?;
        } else {
            let mut solutions = chart.solutions();
            while count < limit && solutions.advance() {
                count += 1;
            }
        }
        let enumeration_duration = enumeration_started.elapsed();
        if opts.statistics {
            let solutions_per_second = if enumeration_duration.is_zero() {
                0.0
            } else {
                approximate_f64(count) / enumeration_duration.as_secs_f64()
            };
            eprintln!("Enumerated {count} solutions.");
            eprintln!(
                "Time to enumerate solutions: {} ({solutions_per_second:.0} solutions/sec)",
                format_duration(enumeration_duration),
            );
        }
    }
    Ok(u8::from(solvable))
}

#[allow(clippy::cast_precision_loss)]
fn approximate_f64(value: usize) -> f64 {
    value as f64
}

fn display_program() -> Result<std::path::PathBuf, io::Error> {
    let executable = env::current_exe()?;
    let name = if cfg!(windows) {
        "utool-display.exe"
    } else {
        "utool-display"
    };
    Ok(executable.with_file_name(name))
}

fn launch_display(opts: &Options) -> ExitCode {
    if opts.input_codec.is_some() {
        return fail(
            "The display command accepts filenames and infers their codecs; -I is not supported.",
            NO_INPUT_CODEC,
        );
    }
    let program = match display_program() {
        Ok(program) if program.is_file() => program,
        Ok(program) => {
            return fail(
                format!(
                    "The Utool display companion was not found at {}. Reinstall the complete Utool archive with utool and utool-display side by side.",
                    program.display()
                ),
                SOLVER_NOT_APPLICABLE,
            );
        }
        Err(error) => {
            return fail(
                detailed_error(
                    "Could not locate the Utool executable.",
                    std::iter::empty(),
                    error,
                ),
                IO_ERROR,
            );
        }
    };
    let mut command = Command::new(program);
    if let Some(filter) = &opts.filter {
        command.arg("--filter").arg(filter);
    }
    command.args(opts.positional.iter().skip(1));
    match command.status() {
        Ok(status) => status
            .code()
            .and_then(|code| u8::try_from(code).ok())
            .map_or(ExitCode::FAILURE, ExitCode::from),
        Err(error) => fail(
            detailed_error(
                "Could not start utool-display.",
                [(
                    "Executable",
                    command.get_program().to_string_lossy().into_owned(),
                )],
                error,
            ),
            IO_ERROR,
        ),
    }
}

fn main() -> ExitCode {
    let args = env::args().skip(1).collect::<Vec<_>>();
    let mut opts = match options(&args) {
        Ok(value) => value,
        Err(error) => return fail(error, 140),
    };
    let user_config = match utool::UserConfig::load() {
        Ok(config) => Some(config),
        Err(error) => {
            return fail(
                detailed_error(
                    "Could not load the Utool configuration.",
                    [("Configuration", "~/.utool".to_owned())],
                    error,
                ),
                IO_ERROR,
            );
        }
    };
    if let Some(config) = &user_config {
        opts.default_output_codec = config.codec_preferences().default_output;
    }
    if opts.version {
        eprintln!(
            "Utool (The Swiss Army Knife of Underspecification), version {}",
            env!("CARGO_PKG_VERSION")
        );
        return ExitCode::SUCCESS;
    }
    if opts.codec_options_help {
        eprintln!(
            "utool global options are:\n  --help-options\n  --display-codecs, -d\n  --display-statistics, -s\n  --no-output, -n\n  --filter, -f <filename>\n  --port, -p <port>\n  --logging, -l [filename]\n  --version"
        );
        return ExitCode::SUCCESS;
    }
    if opts.display_codecs {
        println!(
            "Input codecs:\n  chain\n  domcon-oz (.clls)\n  domgraph-gxl (.dg.xml)\n  holesem-comsem (.hs.pl)\n  mrs-prolog (.mrs.pl)\n  mrs-xml (.mrs.xml)\nOutput codecs:\n  domcon-oz (.clls)\n  domgraph-gxl (.dg.xml)\n  domgraph-dot (.dg.dot)\n  domgraph-udraw (.dg.udg)\n  plugging-oz (.plug.oz)\n  plugging-lkb (.lkbplug.lisp)\n  term-prolog (.t.pl)\n  term-oz (.t.oz)\n  domgraph-codegen (.java)\n  plugging-groovy"
        );
        return ExitCode::SUCCESS;
    }
    let op = operation(opts.positional.first());
    if opts.help || op == Some(Operation::Help) || op.is_none() {
        print_help(if opts.help {
            opts.positional.first().map(String::as_str)
        } else if op == Some(Operation::Help) {
            opts.positional.get(1).map(String::as_str)
        } else {
            opts.positional.first().map(String::as_str)
        });
        return ExitCode::SUCCESS;
    }
    let op = op.expect("checked");
    if op == Operation::Server {
        let configured_port = user_config.as_ref().map_or_else(
            || utool::ServerPreferences::default().port,
            |config| config.server_preferences().port,
        );
        let port = opts.port.unwrap_or(configured_port);
        let logging = match &opts.logging {
            LoggingOption::Disabled => utool::server::ServerLogging::Disabled,
            LoggingOption::Stderr => utool::server::ServerLogging::Stderr,
            LoggingOption::File(path) => utool::server::ServerLogging::File(path.into()),
        };
        let config = utool::server::ServerConfig {
            bind_address: std::net::Ipv4Addr::UNSPECIFIED.into(),
            port,
            logging,
            warmup: false,
        };
        return match utool::server::run(config) {
            Ok(()) => ExitCode::SUCCESS,
            Err(error) if error.kind() == io::ErrorKind::AddrInUse => {
                fail(format!("Port {port} is already in use"), 129)
            }
            Err(error) => fail(
                detailed_error(
                    "The Utool server stopped because of an I/O error.",
                    [("Port", port.to_string())],
                    error,
                ),
                129,
            ),
        };
    }
    if op == Operation::Display {
        return launch_display(&opts);
    }
    let Some(source) = opts.positional.get(1) else {
        return fail("This operation requires an input graph.", NO_INPUT);
    };
    match execute(&opts, op, source) {
        Ok(code) => ExitCode::from(code),
        Err((message, code)) => fail(message, code),
    }
}
