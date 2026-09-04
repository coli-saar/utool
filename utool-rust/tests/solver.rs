use num_bigint::BigUint;
use utool::{HncGraph, SolveError, parse_chain, parse_domcon_oz, solve, solve_with_cancellation};

fn solve_text(input: &str) -> utool::Chart {
    let parsed = parse_domcon_oz(input).unwrap();
    let graph = HncGraph::try_from(parsed).unwrap();
    solve(&graph).unwrap()
}

#[test]
fn solves_a_single_fragment() {
    let chart = solve_text("[label(x f(y z)) label(y a) label(z b)]");
    assert_eq!(chart.count_solutions(), BigUint::from(1_u8));
    let terms: Vec<_> = chart
        .solutions()
        .map(|solution| solution.to_term())
        .collect();
    assert_eq!(terms, ["f[x](a[y],b[z])"]);
}

#[test]
fn solves_compact_graph_into_split_automaton() {
    let chart = solve_text("[label(x f(x1 x2)) dom(x1 y) label(y a) dom(x2 z) label(z b)]");
    assert_eq!(chart.count_solutions(), BigUint::from(1_u8));
    assert!(chart.state_count() >= 3);
    assert!(chart.split_count() >= 3);
    let rules = chart.rules();
    assert_eq!(rules.len(), chart.split_count());
    assert!(rules.iter().any(|rule| rule.root == "x"));
    assert!(rules.iter().any(|rule| {
        rule.attachments
            .iter()
            .any(|(dominator, child)| dominator == "x1" && child == &["y"])
    }));
    assert_eq!(
        chart.solutions().next().unwrap().to_term(),
        "f[x](a[y],b[z])"
    );
}

#[test]
fn ports_two_cross_edge_solutions() {
    let chart =
        solve_text("[label(x f(x1)) label(y g(y1)) label(z a) dom(x1 z) dom(y1 z) dom(y x1)]");
    assert_eq!(chart.count_solutions(), BigUint::from(2_u8));
    let mut terms: Vec<_> = chart
        .solutions()
        .map(|solution| solution.to_term())
        .collect();
    terms.sort();
    assert_eq!(terms, ["f[x](g[y](a[z]))", "g[y](f[x](a[z]))"]);
}

#[test]
fn empty_graph_has_one_empty_solution() {
    let chart = solve_text("[]");
    assert_eq!(chart.count_solutions(), BigUint::from(1_u8));
    assert_eq!(chart.solutions().next().unwrap().root(), None);
}

#[test]
fn hnc_does_not_imply_solvable() {
    let chart =
        solve_text("[label(n0 f(n1 n2)) label(n3 a) label(n4 b) dom(n1 n3) dom(n2 n4) dom(n1 n4)]");
    assert_eq!(chart.count_solutions(), BigUint::from(0_u8));
    assert_eq!(chart.solutions().count(), 0);
}

#[test]
fn ports_three_upper_fragments() {
    let chart = solve_text(
        "[label(x f(x1)) label(y g(y1)) label(z h(z1)) label(w a) dom(x y1) dom(y x1) dom(z y1) dom(y z1) dom(x1 w) dom(y1 w) dom(z1 w)]",
    );
    assert_eq!(chart.count_solutions(), BigUint::from(2_u8));
    let mut terms: Vec<_> = chart
        .solutions()
        .map(|solution| solution.to_term())
        .collect();
    terms.sort();
    assert_eq!(terms, ["f[x](g[y](h[z](a[w])))", "h[z](g[y](f[x](a[w])))",]);
}

#[test]
fn chart_construction_can_be_cancelled() {
    let parsed = parse_domcon_oz("[label(x a)]").unwrap();
    let graph = HncGraph::try_from(parsed).unwrap();
    assert!(matches!(
        solve_with_cancellation(&graph, || true),
        Err(SolveError::Cancelled)
    ));
}

#[test]
fn dfs_and_sorted_enumerators_agree_on_chain_charts() {
    for length in 1..=8 {
        let graph = HncGraph::try_from(parse_chain(&length.to_string()).unwrap()).unwrap();
        let chart = solve(&graph).unwrap();
        let sorted_count = chart.automaton().sorted_language().count();
        let mut dfs = chart.derivations();
        let mut dfs_count = 0;
        while dfs.advance() {
            dfs_count += 1;
        }
        assert_eq!(dfs_count, sorted_count, "chain {length}");
        assert_eq!(BigUint::from(dfs_count), chart.count_solutions());
    }
}

#[test]
fn random_access_uses_the_same_solution_order() {
    let graph = HncGraph::try_from(parse_chain("5").unwrap()).unwrap();
    let chart = solve(&graph).unwrap();
    let expected = chart
        .solutions()
        .map(|solution| solution.to_term())
        .collect::<Vec<_>>();
    for &index in &[0, 1, 7, expected.len() - 1] {
        assert_eq!(
            chart.solutions().nth(index).unwrap().to_term(),
            expected[index]
        );
    }
    assert!(chart.solutions().nth(expected.len()).is_none());
}
