use num_bigint::BigUint;
use std::collections::HashSet;
use utool::{
    ChartDisplay, HncGraph, SolveError, is_solvable, parse_chain, parse_domcon_oz, solve,
    solve_with_cancellation,
};

fn solve_text(input: &str) -> utool::Chart {
    let parsed = parse_domcon_oz(input).unwrap();
    let graph = HncGraph::try_from(parsed).unwrap();
    solve(&graph).unwrap()
}

fn solution_terms(chart: &utool::Chart) -> Vec<String> {
    let mut terms = Vec::new();
    let mut solutions = chart.solutions();
    while solutions.advance() {
        terms.push(
            solutions
                .current()
                .expect("advance produced a solution")
                .to_term(),
        );
    }
    terms
}

fn solution_term_at(chart: &utool::Chart, index: usize) -> Option<String> {
    let mut solutions = chart.solutions();
    for _ in 0..=index {
        if !solutions.advance() {
            return None;
        }
    }
    Some(solutions.current().unwrap().to_term())
}

fn assert_complete_solution_tree(solution: &utool::Solution<'_>) {
    let expected = solution
        .graph()
        .parsed()
        .nodes()
        .iter()
        .filter(|node| !node.is_hole())
        .map(|node| solution.graph().node_id(node.name()).unwrap())
        .collect::<HashSet<_>>();
    let mut visited = HashSet::new();
    let mut stack = vec![solution.root()];
    while let Some(tree) = stack.pop() {
        let node = solution.node_id(tree);
        assert!(visited.insert(node), "node {node:?} occurs more than once");
        assert_eq!(
            solution.arena().get_children(tree).len(),
            solution.graph().node(node).tree_children().len(),
            "arity changed for {node:?}"
        );
        stack.extend(solution.arena().get_children(tree).iter().copied());
    }
    assert_eq!(visited, expected);
}

#[test]
fn solves_a_single_fragment() {
    let chart = solve_text("[label(x f(y z)) label(y a) label(z b)]");
    assert_eq!(chart.count_solutions(), BigUint::from(1_u8));
    let terms = solution_terms(&chart);
    assert_eq!(terms, ["f(a,b)"]);
}

#[test]
fn solves_compact_graph_into_rule_automaton() {
    let chart = solve_text("[label(x f(x1 x2)) dom(x1 y) label(y a) dom(x2 z) label(z b)]");
    assert_eq!(chart.count_solutions(), BigUint::from(1_u8));
    assert!(chart.state_count() >= 3);
    assert!(chart.rule_count() >= 3);
    let display = ChartDisplay::new(&chart);
    let rules = display.rule_page(&chart, 0, display.row_count()).rules;
    assert_eq!(rules.len(), chart.rule_count());
    assert!(rules.iter().any(|rule| rule.fragment.starts_with("f(")));
    assert!(rules.iter().any(|rule| {
        rule.assignments
            .iter()
            .any(|(hole, child)| hole == "x1" && child == &["y"])
    }));
    assert_eq!(solution_term_at(&chart, 0).unwrap(), "f(a,b)");
}

#[test]
fn ports_two_cross_edge_solutions() {
    let chart =
        solve_text("[label(x f(x1)) label(y g(y1)) label(z a) dom(x1 z) dom(y1 z) dom(y x1)]");
    assert_eq!(chart.count_solutions(), BigUint::from(2_u8));
    let mut terms = solution_terms(&chart);
    terms.sort();
    assert_eq!(terms, ["f(g(a))", "g(f(a))"]);
}

#[test]
fn rejects_an_empty_graph() {
    let graph = HncGraph::try_from(parse_domcon_oz("[]").unwrap()).unwrap();
    assert!(matches!(solve(&graph), Err(SolveError::EmptyGraph)));
    assert!(!is_solvable(&graph));
}

#[test]
fn hnc_does_not_imply_solvable() {
    let chart =
        solve_text("[label(n0 f(n1 n2)) label(n3 a) label(n4 b) dom(n1 n3) dom(n2 n4) dom(n1 n4)]");
    assert_eq!(chart.count_solutions(), BigUint::from(0_u8));
    assert!(!chart.solutions().advance());
}

#[test]
fn closed_chain_is_hnc_but_unsolvable() {
    let parsed = parse_domcon_oz(
        "[label(y0 a0) label(x1 f1(xl1 xr1)) label(y1 a1) \
         label(x2 f2(xl2 xr2)) label(y2 a2) label(x3 f3(xl3 xr3)) \
         dom(xl1 y0) dom(xr1 y1) dom(xl2 y1) dom(xr2 y2) \
         dom(xl3 y2) dom(xr3 y0)]",
    )
    .unwrap();
    let graph = HncGraph::try_from(parsed).unwrap();
    assert!(!is_solvable(&graph));
    assert_eq!(
        solve(&graph).unwrap().count_solutions(),
        BigUint::from(0_u8)
    );
}

#[test]
fn existence_solver_agrees_with_chart_construction() {
    for length in 1..=8 {
        let graph = HncGraph::try_from(parse_chain(&length.to_string()).unwrap()).unwrap();
        assert_eq!(
            is_solvable(&graph),
            solve(&graph).unwrap().count_solutions() != 0_u8.into()
        );
    }

    let graph = HncGraph::try_from(
        parse_domcon_oz(
            "[label(n0 f(n1 n2)) label(n3 a) label(n4 b) dom(n1 n3) dom(n2 n4) dom(n1 n4)]",
        )
        .unwrap(),
    )
    .unwrap();
    assert!(!is_solvable(&graph));
}

#[test]
fn ports_three_upper_fragments() {
    let chart = solve_text(
        "[label(x f(x1)) label(y g(y1)) label(z h(z1)) label(w a) dom(x y1) dom(y x1) dom(z y1) dom(y z1) dom(x1 w) dom(y1 w) dom(z1 w)]",
    );
    assert_eq!(chart.count_solutions(), BigUint::from(2_u8));
    let mut terms = solution_terms(&chart);
    terms.sort();
    assert_eq!(terms, ["f(g(h(a)))", "h(g(f(a)))",]);
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
fn solution_enumerator_and_automaton_agree_on_chain_charts() {
    for length in 1..=8 {
        let graph = HncGraph::try_from(parse_chain(&length.to_string()).unwrap()).unwrap();
        let chart = solve(&graph).unwrap();
        let sorted_count = chart
            .fragment_automaton()
            .automaton()
            .sorted_language()
            .count();
        let mut solutions = chart.solutions();
        let mut solution_count = 0;
        while solutions.advance() {
            solution_count += 1;
        }
        assert_eq!(solution_count, sorted_count, "chain {length}");
        assert_eq!(BigUint::from(solution_count), chart.count_solutions());
    }
}

#[test]
fn restarting_and_advancing_preserves_solution_order() {
    let graph = HncGraph::try_from(parse_chain("5").unwrap()).unwrap();
    let chart = solve(&graph).unwrap();
    let expected = solution_terms(&chart);
    for &index in &[0, 1, 7, expected.len() - 1] {
        assert_eq!(solution_term_at(&chart, index).unwrap(), expected[index]);
    }
    assert!(solution_term_at(&chart, expected.len()).is_none());
}

#[test]
fn streamed_solutions_reuse_one_stable_arena() {
    let graph = HncGraph::try_from(parse_chain("7").unwrap()).unwrap();
    let chart = solve(&graph).unwrap();
    let mut solutions = chart.solutions();
    let mut solution_size = None;
    let mut count = 0_usize;
    while solutions.advance() {
        let solution = solutions.current().unwrap();
        assert_complete_solution_tree(&solution);
        let arena_size = solution.arena().len();
        assert_eq!(*solution_size.get_or_insert(arena_size), arena_size);
        count += 1;
    }
    assert_eq!(BigUint::from(count), chart.count_solutions());
}

#[test]
fn every_enumerated_chain_solution_is_one_complete_tree() {
    for length in 1..=8 {
        let graph = HncGraph::try_from(parse_chain(&length.to_string()).unwrap()).unwrap();
        let chart = solve(&graph).unwrap();
        let mut solutions = chart.solutions();
        while solutions.advance() {
            assert_complete_solution_tree(&solutions.current().unwrap());
        }
    }
}

#[test]
fn chart_rows_are_stable_across_lazy_pages() {
    let graph = HncGraph::try_from(parse_chain("8").unwrap()).unwrap();
    let chart = solve(&graph).unwrap();
    let display = ChartDisplay::new(&chart);
    let expected = display.rule_page(&chart, 0, display.row_count()).rules;
    let mut paged = Vec::new();
    for start in (0..display.row_count()).step_by(3) {
        let page = display.rule_page(&chart, start, 3);
        assert!(
            page.states
                .iter()
                .all(|state| { page.rules.iter().any(|rule| rule.state == state.state) })
        );
        assert!(
            page.rules
                .iter()
                .all(|rule| { page.states.iter().any(|state| state.state == rule.state) })
        );
        paged.extend(page.rules);
    }
    assert_eq!(paged, expected);
    assert_eq!(expected.len(), display.row_count());
    assert!(expected.iter().all(|row| !row.fragment.contains(": ")));
}

#[test]
fn top_fragments_enumerate_distinct_rule_symbols_without_paging() {
    let chart =
        solve_text("[label(x f(x1)) label(y g(y1)) label(z a) dom(x1 z) dom(y1 z) dom(y x1)]");
    let display = ChartDisplay::new(&chart);
    let mut from_rows = display
        .rule_page(&chart, 0, display.row_count())
        .rules
        .into_iter()
        .map(|rule| rule.fragment)
        .collect::<Vec<_>>();
    from_rows.sort();
    from_rows.dedup();
    assert_eq!(chart.top_fragments(), from_rows);
    assert!(chart.top_fragments().contains(&"f(g(y1))".to_owned()));
}

#[test]
fn rondane_top_fragments_cover_every_displayed_rule() {
    let graph = HncGraph::try_from(
        utool::parse_mrs_prolog(include_str!(
            "../../src/main/resources/examples/rondane-892.mrs.pl"
        ))
        .unwrap(),
    )
    .unwrap();
    let chart = solve(&graph).unwrap();
    let display = ChartDisplay::new(&chart);
    let top_fragments = chart.top_fragments();
    let page = display.rule_page(&chart, 0, display.row_count());
    assert!(
        page.rules
            .iter()
            .all(|rule| top_fragments.contains(&rule.fragment)),
        "top-fragment metadata omitted a displayed substituted fragment"
    );
}
