fn main() {
    println!("cargo:rerun-if-env-changed=UTOOL_BUILD_ID");
    let build_id = std::env::var("UTOOL_BUILD_ID").unwrap_or_else(|_| {
        std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .expect("system clock is before the Unix epoch")
            .as_secs()
            .to_string()
    });
    println!("cargo:rustc-env=UTOOL_BUILD_ID={build_id}");
    tauri_build::build();
}
