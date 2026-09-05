use std::{
    env, fs,
    io::{self, BufWriter, Read, Write},
    process::ExitCode,
    time::{Duration, Instant},
};
use utool::{HncGraph, InputCodec, OutputCodec, RewriteSystem, filter_chart, is_solvable, solve};

const IO_ERROR: u8 = 128;
const NO_INPUT: u8 = 150;
const NO_INPUT_CODEC: u8 = 151;
const NO_SUCH_INPUT_CODEC: u8 = 152;
const SOLVER_NOT_APPLICABLE: u8 = 153;
const NO_OUTPUT_CODEC: u8 = 160;
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

#[derive(Default)]
#[allow(clippy::struct_excessive_bools)]
struct Options {
    input_codec: Option<String>,
    output_codec: Option<String>,
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
    positional: Vec<String>,
}

fn fail(message: impl AsRef<str>, code: u8) -> ExitCode {
    eprintln!("{}", message.as_ref());
    ExitCode::from(code)
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
    if let Some(command) = command {
        eprintln!(
            "utool {command}: {}.",
            match command {
                "solve" => "Solve an underspecified description",
                "solvable" => "Check solvability without enumerating solutions",
                "convert" => "Convert underspecified description from one format to another",
                "classify" => "Check whether a description belongs to special classes",
                _ => "Unknown command",
            }
        );
        eprintln!("Usage: utool {command} [options] [input-source]");
    } else {
        eprintln!("Usage: utool <subcommand> [options] [args]");
        eprintln!(
            "Type `utool help <subcommand>' for help on a specific subcommand.\n\nAvailable subcommands:\n    solve        Solve an underspecified description.\n    solvable     Check solvability without enumerating solutions.\n    convert      Convert underspecified description from one format to another.\n    classify     Check whether a description belongs to special classes.\n    display      Start the Underspecification Workbench GUI.\n    server       Start Utool in server mode.\n    help         Display help on a command."
        );
    }
}

fn read_graph(opts: &Options, source: &str) -> Result<HncGraph, (String, u8)> {
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
        io::stdin()
            .read_to_string(&mut text)
            .map_err(|e| (e.to_string(), IO_ERROR))?;
    } else {
        text = fs::read_to_string(source).map_err(|e| {
            (
                format!("An I/O error occurred while reading the input.\n{e}"),
                IO_ERROR,
            )
        })?;
    }
    let parsed = selected.parse(&text).map_err(|e| {
        let code = if selected == InputCodec::Chain && matches!(&e, utool::CodecError::Semantic(_))
        {
            PARSE_ERROR + 1
        } else {
            PARSE_ERROR
        };
        (
            format!("A parsing error occurred while reading the input.\n{e}"),
            code,
        )
    })?;
    HncGraph::try_from(parsed).map_err(|e| {
        (
            format!("A semantic error occurred while decoding the graph.\n{e}"),
            PARSE_ERROR,
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
    if opts.input_codec.as_deref() == Some("domcon-oz")
        || input_name.and_then(OutputCodec::from_filename) == Some(OutputCodec::DomconOz)
    {
        return Ok(OutputCodec::DomconOz);
    }
    Err((
        "You must specify an output codec for this operation!".to_owned(),
        NO_OUTPUT_CODEC,
    ))
}

fn result_writer(opts: &Options) -> Result<BufWriter<Box<dyn Write>>, (String, u8)> {
    let writer: Box<dyn Write> = if let Some(path) = &opts.output {
        Box::new(fs::File::create(path).map_err(|e| (e.to_string(), IO_ERROR))?)
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

#[allow(clippy::too_many_lines)]
fn execute(opts: &Options, op: Operation, source: &str) -> Result<u8, (String, u8)> {
    let graph = read_graph(opts, source)?;
    let solve_output_codec = if op == Operation::Solve && !opts.no_output {
        let codec = output_codec(opts, Some(source))?;
        if !codec.supports_solutions() {
            return Err((
                "This output codec doesn't support the printing of multiple solved forms!"
                    .to_owned(),
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
                .write_graph(graph.parsed(), &mut writer)
                .and_then(|()| writer.flush())
                .map_err(|e| (e.to_string(), IO_ERROR))?;
        }
        return Ok(0);
    }
    if op == Operation::Classify {
        let root = |node| graph.tree_parent(node).is_none();
        let weakly_normal = graph
            .parsed()
            .dominance_edges()
            .iter()
            .all(|(source, target)| graph.node(*source).is_hole() || root(*target));
        let normal = weakly_normal
            && graph
                .parsed()
                .dominance_edges()
                .iter()
                .all(|(source, _)| graph.node(*source).is_hole());
        let compact = graph
            .parsed()
            .nodes()
            .iter()
            .all(|node| node.label().is_none() || graph.node_id(node.name()).is_some_and(root));
        let compactifiable = graph
            .parsed()
            .dominance_edges()
            .iter()
            .all(|(source, _)| graph.node(*source).is_hole() || root(*source));
        let mut has_outgoing_dominance = vec![false; graph.parsed().nodes().len()];
        for &(source, _) in graph.parsed().dominance_edges() {
            has_outgoing_dominance[source.index()] = true;
        }
        let leaf_labelled = graph
            .parsed()
            .nodes()
            .iter()
            .enumerate()
            .all(|(index, node)| node.label().is_some() || has_outgoing_dominance[index]);
        return Ok(u8::from(weakly_normal)
            | (u8::from(normal) << 1)
            | (u8::from(compact) << 2)
            | (u8::from(compactifiable) << 3)
            | 16
            | (u8::from(leaf_labelled) << 5));
    }
    if op == Operation::Solvable && !opts.statistics && opts.filter.is_none() && !opts.dump_chart {
        return Ok(u8::from(is_solvable(&graph)));
    }
    let started = Instant::now();
    let mut chart = solve(&graph).map_err(|e| (e.to_string(), SOLVER_NOT_APPLICABLE))?;
    let chart_duration = started.elapsed();
    if let Some(path) = &opts.filter {
        let rules = fs::read_to_string(path).map_err(|e| {
            (
                format!("An error occurred while reading the filtering rules file!\n{e}"),
                FILTER_ERROR,
            )
        })?;
        let system = RewriteSystem::parse(&rules).map_err(|e| (e.to_string(), FILTER_ERROR))?;
        chart =
            filter_chart(&chart, &system, || false).map_err(|e| (e.to_string(), FILTER_ERROR))?;
    }
    let solvable = chart.count_solutions() != 0u8.into();
    if opts.statistics {
        eprintln!(
            "Solving graph ... {}.",
            if solvable {
                "it is solvable"
            } else {
                "it is unsolvable"
            }
        );
        eprintln!("Splits in chart: {}", chart.split_count());
        eprintln!("Time to build chart: {}", format_duration(chart_duration));
        eprintln!("Number of solved forms: {}\n", chart.count_solutions());
    }
    if opts.dump_chart {
        for rule in chart.rules() {
            eprintln!("[{}] => root {}", rule.subgraph.join(", "), rule.root);
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
            encoder
                .begin(&mut writer)
                .map_err(|e| (e.to_string(), IO_ERROR))?;
            let mut solutions = chart.solutions();
            while count < limit && solutions.advance() {
                let solution = solutions.current().expect("advance produced a solution");
                encoder
                    .write_solution(&solution, &mut writer)
                    .map_err(|e| (e.to_string(), IO_ERROR))?;
                count += 1;
            }
            encoder
                .finish(&mut writer)
                .and_then(|()| writer.flush())
                .map_err(|e| (e.to_string(), IO_ERROR))?;
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
            eprintln!("Enumerated {count} solved forms.");
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

fn main() -> ExitCode {
    let args = env::args().skip(1).collect::<Vec<_>>();
    let opts = match options(&args) {
        Ok(value) => value,
        Err(error) => return fail(error, 140),
    };
    if opts.version {
        eprintln!(
            "Utool (The Swiss Army Knife of Underspecification), version {}",
            env!("CARGO_PKG_VERSION")
        );
        return ExitCode::SUCCESS;
    }
    if opts.codec_options_help {
        eprintln!(
            "utool global options are:\n  --help-options\n  --display-codecs, -d\n  --display-statistics, -s\n  --no-output, -n\n  --filter, -f <filename>\n  --version"
        );
        return ExitCode::SUCCESS;
    }
    if opts.display_codecs {
        println!(
            "Input codecs:\n  chain\n  domcon-oz (.clls)\n  holesem-comsem (.hs.pl)\nOutput codecs:\n  domcon-oz (.clls)\n  domgraph-dot (.dg.dot)\n  term-prolog (.t.pl)\n  term-oz (.t.oz)"
        );
        return ExitCode::SUCCESS;
    }
    let op = operation(opts.positional.first());
    if opts.help || op == Some(Operation::Help) || op.is_none() {
        print_help(if op == Some(Operation::Help) {
            opts.positional.get(1).map(String::as_str)
        } else {
            opts.positional.first().map(String::as_str)
        });
        return ExitCode::SUCCESS;
    }
    let op = op.expect("checked");
    if matches!(op, Operation::Display | Operation::Server) {
        return fail(
            "This command is not available in this binary yet.",
            SOLVER_NOT_APPLICABLE,
        );
    }
    let Some(source) = opts.positional.get(1) else {
        return fail("This operation requires an input graph.", NO_INPUT);
    };
    match execute(&opts, op, source) {
        Ok(code) => ExitCode::from(code),
        Err((message, code)) => fail(message, code),
    }
}
