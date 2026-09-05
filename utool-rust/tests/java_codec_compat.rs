use std::collections::BTreeSet;
use std::io::Write;
use std::path::{Path, PathBuf};
use std::process::{Command, Stdio};
use utool::{
    OutputCodec, ParsedGraph, parse_domcon_oz, parse_domgraph_gxl, parse_mrs_prolog, parse_mrs_xml,
};

type NodeSignature = (String, Option<String>, Vec<String>);
type GraphSignature = (BTreeSet<NodeSignature>, BTreeSet<(String, String)>);

const STEFAN_MRS: &str = r"psoa(h1,e2,
[
 rel('proper_q',h3,
     [ attrval('ARG0',x4),
       attrval('RSTR',h5),
       attrval('BODY',h6)]),
 rel('named_rel',h7,
     [ attrval('ARG0',x4),
       attrval('NAME','Aicke')]),
 rel('proper_q',h8,
     [ attrval('ARG0',x9),
       attrval('RSTR',h10),
       attrval('BODY',h11)]),
 rel('named_rel',h12,
     [ attrval('ARG0',x9),
       attrval('NAME','Aicke')]),
 rel('kennen_rel',h13,
     [ attrval('ARG0',e2),
       attrval('ARG1',x4),
       attrval('ARG2',x9)])],
 hcons([
 qeq(h5,h7),
 qeq(h10,h12)
 ]))";

fn repository() -> PathBuf {
    Path::new(env!("CARGO_MANIFEST_DIR"))
        .parent()
        .unwrap()
        .to_owned()
}

fn java_utool(arguments: &[&str]) -> Option<String> {
    let jar = repository().join("target/utool-3.4.1-SNAPSHOT-jar-with-dependencies.jar");
    if !jar.is_file() {
        return None;
    }
    let output = Command::new("java")
        .arg("-jar")
        .arg(jar)
        .args(arguments)
        .output()
        .ok()?;
    assert!(
        output.status.success() || output.status.code() == Some(1),
        "{}",
        String::from_utf8_lossy(&output.stderr)
    );
    Some(String::from_utf8(output.stdout).unwrap())
}

fn java_utool_stdin(arguments: &[&str], input: &str) -> Option<String> {
    let jar = repository().join("target/utool-3.4.1-SNAPSHOT-jar-with-dependencies.jar");
    if !jar.is_file() {
        return None;
    }
    let mut child = Command::new("java")
        .arg("-jar")
        .arg(jar)
        .args(arguments)
        .stdin(Stdio::piped())
        .stdout(Stdio::piped())
        .stderr(Stdio::piped())
        .spawn()
        .ok()?;
    child
        .stdin
        .take()
        .unwrap()
        .write_all(input.as_bytes())
        .unwrap();
    let output = child.wait_with_output().unwrap();
    assert!(
        output.status.success(),
        "{}",
        String::from_utf8_lossy(&output.stderr)
    );
    Some(String::from_utf8(output.stdout).unwrap())
}

fn signature(graph: &ParsedGraph) -> GraphSignature {
    let nodes = graph
        .nodes()
        .iter()
        .map(|node| {
            (
                node.name().to_owned(),
                node.label().map(str::to_owned),
                node.tree_children()
                    .iter()
                    .map(|child| graph.node(*child).name().to_owned())
                    .collect(),
            )
        })
        .collect();
    let dominance = graph
        .dominance_edges()
        .iter()
        .map(|&(source, target)| {
            (
                graph.node(source).name().to_owned(),
                graph.node(target).name().to_owned(),
            )
        })
        .collect();
    (nodes, dominance)
}

#[test]
fn every_repository_mrs_prolog_example_matches_java_domcon_conversion() {
    let root = repository();
    let files = [
        "utool-rust/tests/fixtures/rademaker.mrs.pl",
        "src/main/resources/examples/rondane-1.mrs.pl",
        "src/main/resources/examples/rondane-1262.mrs.pl",
        "src/main/resources/examples/rondane-1409.mrs.pl",
        "src/main/resources/examples/rondane-650.mrs.pl",
        "src/main/resources/examples/rondane-892.mrs.pl",
    ];
    for relative in files {
        let path = root.join(relative);
        let Some(java) = java_utool(&[
            "convert",
            "-I",
            "mrs-prolog",
            "-O",
            "domcon-oz",
            path.to_str().unwrap(),
        ]) else {
            return;
        };
        let java_graph =
            parse_domcon_oz(java.lines().skip(1).collect::<String>().as_str()).unwrap();
        let rust_graph = parse_mrs_prolog(&std::fs::read_to_string(path).unwrap()).unwrap();
        assert_eq!(signature(&rust_graph), signature(&java_graph), "{relative}");
    }

    let Some(java) = java_utool_stdin(
        &["convert", "-I", "mrs-prolog", "-O", "domcon-oz", "-"],
        STEFAN_MRS,
    ) else {
        return;
    };
    let java_graph = parse_domcon_oz(java.lines().skip(1).collect::<String>().as_str()).unwrap();
    assert_eq!(
        signature(&parse_mrs_prolog(STEFAN_MRS).unwrap()),
        signature(&java_graph)
    );
}

#[test]
fn rust_gxl_input_reads_java_gxl_output_without_graph_changes() {
    let root = repository();
    for relative in [
        "src/main/resources/examples/chain3.clls",
        "src/main/resources/examples/thatwould.clls",
        "src/main/resources/examples/kallmeyer-romero.clls",
    ] {
        let path = root.join(relative);
        let source = parse_domcon_oz(&std::fs::read_to_string(&path).unwrap()).unwrap();
        let Some(java_gxl) = java_utool(&[
            "convert",
            "-I",
            "domcon-oz",
            "-O",
            "domgraph-gxl",
            path.to_str().unwrap(),
        ]) else {
            return;
        };
        let decoded = parse_domgraph_gxl(&java_gxl).unwrap();
        assert_eq!(signature(&decoded), signature(&source), "{relative}");

        let mut rust_gxl = Vec::new();
        OutputCodec::DomgraphGxl
            .graph_encoder()
            .unwrap()
            .write_graph(&source, &mut rust_gxl)
            .unwrap();
        let Some(java_domcon) = java_utool_stdin(
            &["convert", "-I", "domgraph-gxl", "-O", "domcon-oz", "-"],
            &String::from_utf8(rust_gxl).unwrap(),
        ) else {
            return;
        };
        let java_decoded =
            parse_domcon_oz(java_domcon.lines().skip(1).collect::<String>().as_str()).unwrap();
        assert_eq!(signature(&java_decoded), signature(&source), "{relative}");
    }
}

#[test]
fn mrs_xml_matches_java_for_quantifiers_binding_and_entities() {
    let xml = concat!(
        "<mrs><var vid=\"h1\"/>",
        "<ep><pred>every_q</pred><var vid=\"h3\"/>",
        "<fvpair><rargname>ARG0</rargname><var vid=\"x4\"/></fvpair>",
        "<fvpair><rargname>RSTR</rargname><var vid=\"h5\"/></fvpair>",
        "<fvpair><rargname>BODY</rargname><var vid=\"h6\"/></fvpair></ep>",
        "<ep><pred>person&amp;named</pred><var vid=\"h7\"/>",
        "<fvpair><rargname>ARG0</rargname><var vid=\"x4\"/></fvpair></ep>",
        "<ep><pred>sleep</pred><var vid=\"h8\"/>",
        "<fvpair><rargname>ARG0</rargname><var vid=\"e2\"/></fvpair>",
        "<fvpair><rargname>ARG1</rargname><var vid=\"x4\"/></fvpair></ep>",
        "<hcons><hi><var vid=\"h1\"/></hi><lo><var vid=\"h3\"/></lo></hcons>",
        "<hcons><hi><var vid=\"h5\"/></hi><lo><var vid=\"h7\"/></lo></hcons></mrs>",
    );
    let Some(java) = java_utool_stdin(&["convert", "-I", "mrs-xml", "-O", "domcon-oz", "-"], xml)
    else {
        return;
    };
    let reference = parse_domcon_oz(java.lines().skip(1).collect::<String>().as_str()).unwrap();
    assert_eq!(
        signature(&parse_mrs_xml(xml).unwrap()),
        signature(&reference)
    );
}

#[test]
fn remaining_graph_output_codecs_match_java_references() {
    let path = repository().join("src/main/resources/examples/chain3.clls");
    let graph = parse_domcon_oz(&std::fs::read_to_string(&path).unwrap()).unwrap();
    let cases = [
        ("domgraph-udraw", OutputCodec::DomgraphUdraw, false),
        ("plugging-oz", OutputCodec::PluggingOz, true),
        ("plugging-lkb", OutputCodec::PluggingLkb, false),
        ("plugging-groovy", OutputCodec::PluggingGroovy, true),
    ];
    for (name, codec, java_header) in cases {
        let Some(java) = java_utool(&[
            "convert",
            "-I",
            "domcon-oz",
            "-O",
            name,
            path.to_str().unwrap(),
        ]) else {
            return;
        };
        let java = if java_header {
            java.lines().skip(1).collect::<Vec<_>>().join("\n")
        } else {
            java
        };
        let mut rust = Vec::new();
        codec
            .graph_encoder()
            .unwrap()
            .write_graph(&graph, &mut rust)
            .unwrap();
        assert_eq!(
            String::from_utf8(rust).unwrap().trim_end(),
            java.trim_end(),
            "{name}"
        );
    }

    let Some(java) = java_utool(&[
        "convert",
        "-I",
        "domcon-oz",
        "-O",
        "domgraph-codegen",
        path.to_str().unwrap(),
    ]) else {
        return;
    };
    let mut rust = Vec::new();
    OutputCodec::DomgraphCodegen
        .graph_encoder()
        .unwrap()
        .write_graph(&graph, &mut rust)
        .unwrap();
    let generated_lines = |text: &str| -> BTreeSet<String> {
        text.lines()
            .map(str::trim)
            .filter(|line| line.starts_with("graph.add") || line.starts_with("labels.add"))
            .map(str::to_owned)
            .collect()
    };
    assert_eq!(
        generated_lines(&String::from_utf8(rust).unwrap()),
        generated_lines(&java)
    );
}

fn plugging_solutions(output: &str) -> Vec<BTreeSet<(String, String)>> {
    output
        .lines()
        .filter(|line| line.contains("plug("))
        .map(|line| {
            let mut edges = BTreeSet::new();
            let mut rest = line;
            while let Some(start) = rest.find("plug(") {
                rest = &rest[start + 5..];
                let end = rest.find(')').unwrap();
                let mut endpoints = rest[..end].split_whitespace();
                edges.insert((
                    endpoints.next().unwrap().to_owned(),
                    endpoints.next().unwrap().to_owned(),
                ));
                rest = &rest[end + 1..];
            }
            edges
        })
        .collect()
}

#[test]
fn plugging_solution_edges_match_java_for_every_chain3_solution() {
    let path = repository().join("src/main/resources/examples/chain3.clls");
    let Some(java) = java_utool(&[
        "solve",
        "-I",
        "domcon-oz",
        "-O",
        "plugging-oz",
        path.to_str().unwrap(),
    ]) else {
        return;
    };
    let rust = Command::new(env!("CARGO_BIN_EXE_utool"))
        .args([
            "solve",
            "-I",
            "domcon-oz",
            "-O",
            "plugging-oz",
            path.to_str().unwrap(),
        ])
        .output()
        .unwrap();
    assert_eq!(rust.status.code(), Some(1));
    assert_eq!(
        plugging_solutions(&String::from_utf8(rust.stdout).unwrap()),
        plugging_solutions(&java),
    );
}
