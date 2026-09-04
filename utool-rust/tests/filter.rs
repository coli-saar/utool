use utool::{HncGraph, RewriteSystem, filter_chart, parse_domcon_oz, solve};

#[test]
fn filters_the_stronger_every_a_reading() {
    let parsed = parse_domcon_oz(
        "[label(x1 every(x2 x3)) label(y1 a(y2 y3)) label(z1 foo) label(z2 bar) label(z3 baz) dom(x2 z1) dom(y2 z2) dom(x3 z3) dom(y3 z3)]",
    )
    .unwrap();
    let graph = HncGraph::try_from(parsed).unwrap();
    let chart = solve(&graph).unwrap();
    assert_eq!(chart.count_solutions().to_string(), "2");
    let rules = RewriteSystem::parse(
        "start annotation: +\nneutral annotation: 0\n+: a(+,+)\n+: every(-,+)\n[+] a(X,every(Y,Z)) -> every(Y,a(X,Z))",
    )
    .unwrap();
    let filtered = filter_chart(&chart, &rules, || false).unwrap();
    assert_eq!(filtered.count_solutions().to_string(), "1");
    assert_eq!(filtered.solutions().count(), 1);
}

#[test]
fn equivalence_rules_choose_one_representative() {
    let parsed = parse_domcon_oz(
        "[label(x a(x1 x2)) label(y a(y1 y2)) label(p p) label(q q) label(r r) dom(x1 p) dom(y1 q) dom(x2 r) dom(y2 r)]",
    )
    .unwrap();
    let graph = HncGraph::try_from(parsed).unwrap();
    let chart = solve(&graph).unwrap();
    let rules = RewriteSystem::parse("a#1(X,a#2(Y,Z)) = a#2(Y,a#1(X,Z))").unwrap();
    let filtered = filter_chart(&chart, &rules, || false).unwrap();
    assert!(filtered.count_solutions() <= chart.count_solutions());
}
