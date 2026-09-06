use std::process::Command;

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
    tauri_build::build();
}
