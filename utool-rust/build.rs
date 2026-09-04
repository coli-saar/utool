fn main() {
    generate(
        "grammars/domcon_oz.par",
        "domcon_oz_parser.rs",
        "domcon_oz_grammar_trait.rs",
        "DomconOzGrammar",
        "domcon_oz_grammar",
    );
    generate(
        "grammars/holesem.par",
        "holesem_parser.rs",
        "holesem_grammar_trait.rs",
        "HolesemGrammar",
        "holesem_grammar",
    );
}

fn generate(grammar: &str, parser: &str, actions: &str, user_type: &str, module: &str) {
    let mut builder = parol::build::Builder::with_cargo_script_output();
    builder
        .grammar_file(grammar)
        .parser_output_file(parser)
        .actions_output_file(actions)
        .user_type_name(user_type)
        .user_trait_module_name(module)
        .range();
    builder
        .max_lookahead(5)
        .unwrap_or_else(|error| panic!("invalid lookahead for {grammar}: {error}"));
    builder
        .generate_parser()
        .unwrap_or_else(|error| panic!("failed to generate {grammar}: {error}"));

    // Parol emits crate-level clippy attributes in the generated action file.
    // We include that file as a module, where inner attributes are not legal.
    let actions_path = std::path::PathBuf::from(std::env::var_os("OUT_DIR").unwrap()).join(actions);
    let generated = std::fs::read_to_string(&actions_path)
        .unwrap_or_else(|error| panic!("failed to read {}: {error}", actions_path.display()));
    let module_safe = generated
        .lines()
        .filter(|line| !line.starts_with("#![allow(clippy::"))
        .collect::<Vec<_>>()
        .join("\n");
    std::fs::write(&actions_path, module_safe)
        .unwrap_or_else(|error| panic!("failed to rewrite {}: {error}", actions_path.display()));
}
