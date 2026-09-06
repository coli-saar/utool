use std::process::Command;
use std::{fs, path::Path};

use quick_xml::{Reader, XmlVersion, events::Event};

fn git_output(arguments: &[&str]) -> Option<String> {
    let output = Command::new("git").args(arguments).output().ok()?;
    output
        .status
        .success()
        .then(|| String::from_utf8_lossy(&output.stdout).trim().to_owned())
}

fn short_sha(sha: &str) -> &str {
    sha.get(..7).unwrap_or(sha)
}

fn generated_build_id() -> String {
    if let (Ok(run), Ok(sha)) = (
        std::env::var("GITHUB_RUN_NUMBER"),
        std::env::var("GITHUB_SHA"),
    ) {
        let attempt = std::env::var("GITHUB_RUN_ATTEMPT").unwrap_or_else(|_| "1".to_owned());
        let run = if attempt == "1" {
            run
        } else {
            format!("{run}.{attempt}")
        };
        return format!("{run}-{}", short_sha(&sha));
    }

    let sha = git_output(&["rev-parse", "--short=7", "HEAD"])
        .filter(|sha| !sha.is_empty())
        .unwrap_or_else(|| "unknown".to_owned());
    let dirty = git_output(&["status", "--porcelain", "--untracked-files=no"])
        .is_some_and(|status| !status.is_empty());
    format!("local-{sha}{}", if dirty { "-dirty" } else { "" })
}

fn generate_builtin_examples() -> Result<(), String> {
    const CATALOGUE: &str = "resources/examples/examples.xml";
    let mut reader = Reader::from_file(CATALOGUE).map_err(|error| error.to_string())?;
    reader.config_mut().trim_text(true);
    let mut buffer = Vec::new();
    let mut examples = Vec::new();

    loop {
        match reader
            .read_event_into(&mut buffer)
            .map_err(|error| error.to_string())?
        {
            Event::Empty(element) | Event::Start(element)
                if element.name().as_ref() == b"example" =>
            {
                let mut filename = None;
                let mut description = None;
                for attribute in element.attributes() {
                    let attribute = attribute.map_err(|error| error.to_string())?;
                    let value = attribute
                        .decoded_and_normalized_value(XmlVersion::default(), reader.decoder())
                        .map_err(|error| error.to_string())?
                        .into_owned();
                    match attribute.key.as_ref() {
                        b"filename" => filename = Some(value),
                        b"description" => description = Some(value),
                        _ => {}
                    }
                }
                let filename = filename.ok_or("example is missing its filename attribute")?;
                let description = description.ok_or_else(|| {
                    format!("example {filename} is missing its description attribute")
                })?;
                let path = Path::new(&filename);
                if path.file_name().and_then(|name| name.to_str()) != Some(filename.as_str()) {
                    return Err(format!(
                        "example filename must be a plain filename: {filename}"
                    ));
                }
                let source = Path::new("resources/examples").join(&filename);
                if !source.is_file() {
                    return Err(format!(
                        "example source does not exist: {}",
                        source.display()
                    ));
                }
                println!("cargo:rerun-if-changed={}", source.display());
                examples.push((filename, description));
            }
            Event::Eof => break,
            _ => {}
        }
        buffer.clear();
    }

    if examples.is_empty() {
        return Err("examples.xml does not contain any examples".to_owned());
    }

    let mut generated = String::from("const BUILTIN_EXAMPLES: &[BuiltinExample] = &[\n");
    for (filename, description) in examples {
        generated.push_str(&format!(
            "    BuiltinExample {{ filename: {filename:?}, description: {description:?}, source: include_str!(concat!(env!(\"CARGO_MANIFEST_DIR\"), \"/resources/examples/\", {filename:?})) }},\n"
        ));
    }
    generated.push_str("];\n");

    let output = Path::new(&std::env::var("OUT_DIR").map_err(|error| error.to_string())?)
        .join("builtin_examples.rs");
    fs::write(output, generated).map_err(|error| error.to_string())?;
    println!("cargo:rerun-if-changed={CATALOGUE}");
    Ok(())
}

fn main() {
    for variable in [
        "UTOOL_BUILD_ID",
        "GITHUB_RUN_NUMBER",
        "GITHUB_RUN_ATTEMPT",
        "GITHUB_SHA",
    ] {
        println!("cargo:rerun-if-env-changed={variable}");
    }
    if let Some(git_head) =
        git_output(&["rev-parse", "--git-path", "HEAD"]).filter(|path| !path.is_empty())
    {
        println!("cargo:rerun-if-changed={git_head}");
    }
    if let Some(git_ref) = git_output(&["symbolic-ref", "-q", "HEAD"])
        .and_then(|reference| git_output(&["rev-parse", "--git-path", &reference]))
        .filter(|path| !path.is_empty())
    {
        println!("cargo:rerun-if-changed={git_ref}");
    }

    let build_id = std::env::var("UTOOL_BUILD_ID").unwrap_or_else(|_| generated_build_id());
    println!("cargo:rustc-env=UTOOL_BUILD_ID={build_id}");
    generate_builtin_examples().expect("failed to generate the built-in example catalogue");
    tauri_build::build();
}
