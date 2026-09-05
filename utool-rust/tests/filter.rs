use utool::{
    FilterError, HncGraph, RewriteSystem, filter_chart, parse_chain, parse_domcon_oz,
    parse_mrs_prolog, solve,
};

const STEFAN_MRS: &str = r"
psoa(h1,e2,
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
 ]))
";

const STEFAN_EQUIVALENCES: &str = r"
// Auto-converted from the utool 3.1-era XML equivalence format
// (source: equivalences.xml) to the utool 3.2+ RewritingSystemParser syntax.

start annotation: +
neutral annotation: 0

// equivalencegroup: def_q#1, def_q#1, udef_q#1, demonstrative_q#1, demonstrative_q#1, some_q#0, some_q#1, one_q#0, one_q#1
def_q#1(A,def_q#2(Y0,Y1)) = def_q#2(Y0,def_q#1(A,Y1))
def_q#1(A,def_q#2(Y0,Y1)) = def_q#2(Y0,def_q#1(A,Y1))
def_q#1(A,udef_q#2(Y0,Y1)) = udef_q#2(Y0,def_q#1(A,Y1))
def_q#1(A,demonstrative_q#2(Y0,Y1)) = demonstrative_q#2(Y0,def_q#1(A,Y1))
def_q#1(A,demonstrative_q#2(Y0,Y1)) = demonstrative_q#2(Y0,def_q#1(A,Y1))
def_q#1(A,some_q#2(Y0,Y1)) = some_q#2(def_q#1(A,Y0),Y1)
def_q#1(A,some_q#2(Y0,Y1)) = some_q#2(Y0,def_q#1(A,Y1))
def_q#1(A,one_q#2(Y0,Y1)) = one_q#2(def_q#1(A,Y0),Y1)
def_q#1(A,one_q#2(Y0,Y1)) = one_q#2(Y0,def_q#1(A,Y1))
def_q#1(A,def_q#2(Y0,Y1)) = def_q#2(Y0,def_q#1(A,Y1))
def_q#1(A,def_q#2(Y0,Y1)) = def_q#2(Y0,def_q#1(A,Y1))
def_q#1(A,udef_q#2(Y0,Y1)) = udef_q#2(Y0,def_q#1(A,Y1))
def_q#1(A,demonstrative_q#2(Y0,Y1)) = demonstrative_q#2(Y0,def_q#1(A,Y1))
def_q#1(A,demonstrative_q#2(Y0,Y1)) = demonstrative_q#2(Y0,def_q#1(A,Y1))
def_q#1(A,some_q#2(Y0,Y1)) = some_q#2(def_q#1(A,Y0),Y1)
def_q#1(A,some_q#2(Y0,Y1)) = some_q#2(Y0,def_q#1(A,Y1))
def_q#1(A,one_q#2(Y0,Y1)) = one_q#2(def_q#1(A,Y0),Y1)
def_q#1(A,one_q#2(Y0,Y1)) = one_q#2(Y0,def_q#1(A,Y1))
udef_q#1(A,def_q#2(Y0,Y1)) = def_q#2(Y0,udef_q#1(A,Y1))
udef_q#1(A,def_q#2(Y0,Y1)) = def_q#2(Y0,udef_q#1(A,Y1))
udef_q#1(A,udef_q#2(Y0,Y1)) = udef_q#2(Y0,udef_q#1(A,Y1))
udef_q#1(A,demonstrative_q#2(Y0,Y1)) = demonstrative_q#2(Y0,udef_q#1(A,Y1))
udef_q#1(A,demonstrative_q#2(Y0,Y1)) = demonstrative_q#2(Y0,udef_q#1(A,Y1))
udef_q#1(A,some_q#2(Y0,Y1)) = some_q#2(udef_q#1(A,Y0),Y1)
udef_q#1(A,some_q#2(Y0,Y1)) = some_q#2(Y0,udef_q#1(A,Y1))
udef_q#1(A,one_q#2(Y0,Y1)) = one_q#2(udef_q#1(A,Y0),Y1)
udef_q#1(A,one_q#2(Y0,Y1)) = one_q#2(Y0,udef_q#1(A,Y1))
demonstrative_q#1(A,def_q#2(Y0,Y1)) = def_q#2(Y0,demonstrative_q#1(A,Y1))
demonstrative_q#1(A,def_q#2(Y0,Y1)) = def_q#2(Y0,demonstrative_q#1(A,Y1))
demonstrative_q#1(A,udef_q#2(Y0,Y1)) = udef_q#2(Y0,demonstrative_q#1(A,Y1))
demonstrative_q#1(A,demonstrative_q#2(Y0,Y1)) = demonstrative_q#2(Y0,demonstrative_q#1(A,Y1))
demonstrative_q#1(A,demonstrative_q#2(Y0,Y1)) = demonstrative_q#2(Y0,demonstrative_q#1(A,Y1))
demonstrative_q#1(A,some_q#2(Y0,Y1)) = some_q#2(demonstrative_q#1(A,Y0),Y1)
demonstrative_q#1(A,some_q#2(Y0,Y1)) = some_q#2(Y0,demonstrative_q#1(A,Y1))
demonstrative_q#1(A,one_q#2(Y0,Y1)) = one_q#2(demonstrative_q#1(A,Y0),Y1)
demonstrative_q#1(A,one_q#2(Y0,Y1)) = one_q#2(Y0,demonstrative_q#1(A,Y1))
demonstrative_q#1(A,def_q#2(Y0,Y1)) = def_q#2(Y0,demonstrative_q#1(A,Y1))
demonstrative_q#1(A,def_q#2(Y0,Y1)) = def_q#2(Y0,demonstrative_q#1(A,Y1))
demonstrative_q#1(A,udef_q#2(Y0,Y1)) = udef_q#2(Y0,demonstrative_q#1(A,Y1))
demonstrative_q#1(A,demonstrative_q#2(Y0,Y1)) = demonstrative_q#2(Y0,demonstrative_q#1(A,Y1))
demonstrative_q#1(A,demonstrative_q#2(Y0,Y1)) = demonstrative_q#2(Y0,demonstrative_q#1(A,Y1))
demonstrative_q#1(A,some_q#2(Y0,Y1)) = some_q#2(demonstrative_q#1(A,Y0),Y1)
demonstrative_q#1(A,some_q#2(Y0,Y1)) = some_q#2(Y0,demonstrative_q#1(A,Y1))
demonstrative_q#1(A,one_q#2(Y0,Y1)) = one_q#2(demonstrative_q#1(A,Y0),Y1)
demonstrative_q#1(A,one_q#2(Y0,Y1)) = one_q#2(Y0,demonstrative_q#1(A,Y1))
some_q#1(def_q#2(Y0,Y1),A) = def_q#2(Y0,some_q#1(Y1,A))
some_q#1(def_q#2(Y0,Y1),A) = def_q#2(Y0,some_q#1(Y1,A))
some_q#1(udef_q#2(Y0,Y1),A) = udef_q#2(Y0,some_q#1(Y1,A))
some_q#1(demonstrative_q#2(Y0,Y1),A) = demonstrative_q#2(Y0,some_q#1(Y1,A))
some_q#1(demonstrative_q#2(Y0,Y1),A) = demonstrative_q#2(Y0,some_q#1(Y1,A))
some_q#1(some_q#2(Y0,Y1),A) = some_q#2(some_q#1(Y0,A),Y1)
some_q#1(some_q#2(Y0,Y1),A) = some_q#2(Y0,some_q#1(Y1,A))
some_q#1(one_q#2(Y0,Y1),A) = one_q#2(some_q#1(Y0,A),Y1)
some_q#1(one_q#2(Y0,Y1),A) = one_q#2(Y0,some_q#1(Y1,A))
some_q#1(A,def_q#2(Y0,Y1)) = def_q#2(Y0,some_q#1(A,Y1))
some_q#1(A,def_q#2(Y0,Y1)) = def_q#2(Y0,some_q#1(A,Y1))
some_q#1(A,udef_q#2(Y0,Y1)) = udef_q#2(Y0,some_q#1(A,Y1))
some_q#1(A,demonstrative_q#2(Y0,Y1)) = demonstrative_q#2(Y0,some_q#1(A,Y1))
some_q#1(A,demonstrative_q#2(Y0,Y1)) = demonstrative_q#2(Y0,some_q#1(A,Y1))
some_q#1(A,some_q#2(Y0,Y1)) = some_q#2(Y0,some_q#1(A,Y1))
some_q#1(A,one_q#2(Y0,Y1)) = one_q#2(some_q#1(A,Y0),Y1)
some_q#1(A,one_q#2(Y0,Y1)) = one_q#2(Y0,some_q#1(A,Y1))
one_q#1(def_q#2(Y0,Y1),A) = def_q#2(Y0,one_q#1(Y1,A))
one_q#1(def_q#2(Y0,Y1),A) = def_q#2(Y0,one_q#1(Y1,A))
one_q#1(udef_q#2(Y0,Y1),A) = udef_q#2(Y0,one_q#1(Y1,A))
one_q#1(demonstrative_q#2(Y0,Y1),A) = demonstrative_q#2(Y0,one_q#1(Y1,A))
one_q#1(demonstrative_q#2(Y0,Y1),A) = demonstrative_q#2(Y0,one_q#1(Y1,A))
one_q#1(some_q#2(Y0,Y1),A) = some_q#2(one_q#1(Y0,A),Y1)
one_q#1(some_q#2(Y0,Y1),A) = some_q#2(Y0,one_q#1(Y1,A))
one_q#1(one_q#2(Y0,Y1),A) = one_q#2(one_q#1(Y0,A),Y1)
one_q#1(one_q#2(Y0,Y1),A) = one_q#2(Y0,one_q#1(Y1,A))
one_q#1(A,def_q#2(Y0,Y1)) = def_q#2(Y0,one_q#1(A,Y1))
one_q#1(A,def_q#2(Y0,Y1)) = def_q#2(Y0,one_q#1(A,Y1))
one_q#1(A,udef_q#2(Y0,Y1)) = udef_q#2(Y0,one_q#1(A,Y1))
one_q#1(A,demonstrative_q#2(Y0,Y1)) = demonstrative_q#2(Y0,one_q#1(A,Y1))
one_q#1(A,demonstrative_q#2(Y0,Y1)) = demonstrative_q#2(Y0,one_q#1(A,Y1))
one_q#1(A,some_q#2(Y0,Y1)) = some_q#2(one_q#1(A,Y0),Y1)
one_q#1(A,some_q#2(Y0,Y1)) = some_q#2(Y0,one_q#1(A,Y1))
one_q#1(A,one_q#2(Y0,Y1)) = one_q#2(Y0,one_q#1(A,Y1))

// equivalencegroup: every_q#1, each_q#1
every_q#1(A,every_q#2(Y0,Y1)) = every_q#2(Y0,every_q#1(A,Y1))
every_q#1(A,each_q#2(Y0,Y1)) = each_q#2(Y0,every_q#1(A,Y1))
each_q#1(A,every_q#2(Y0,Y1)) = every_q#2(Y0,each_q#1(A,Y1))
each_q#1(A,each_q#2(Y0,Y1)) = each_q#2(Y0,each_q#1(A,Y1))

// permutesWithEverything: proper_q#1
proper_q(A,*[Y]) = *[proper_q(A,Y)]

// permutesWithEverything: pronoun_q#1
pronoun_q(A,*[Y]) = *[pronoun_q(A,Y)]

";

fn solution_terms(chart: &utool::Chart) -> Vec<String> {
    let mut terms = Vec::new();
    let mut solutions = chart.solutions();
    while solutions.advance() {
        terms.push(solutions.current().unwrap().to_term());
    }
    terms.sort();
    terms
}

#[test]
fn filters_to_the_stronger_every_solution() {
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
    let mut solutions = filtered.solutions();
    assert!(solutions.advance());
    assert!(!solutions.advance());
    assert_eq!(solution_terms(&filtered), ["every(foo,a(bar,baz))"]);
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

    let filtered_again = filter_chart(&filtered, &rules, || false).unwrap();
    assert_eq!(solution_terms(&filtered_again), solution_terms(&filtered));
}

#[test]
fn an_empty_rewrite_system_preserves_the_compact_language() {
    let rules = RewriteSystem::parse("").unwrap();
    for length in 1..=9 {
        let graph = HncGraph::try_from(parse_chain(&length.to_string()).unwrap()).unwrap();
        let chart = solve(&graph).unwrap();
        let filtered = filter_chart(&chart, &rules, || false).unwrap();
        assert_eq!(
            filtered.count_solutions(),
            chart.count_solutions(),
            "chain {length}"
        );
        assert_eq!(
            solution_terms(&filtered),
            solution_terms(&chart),
            "chain {length}"
        );
    }
}

#[test]
fn filtering_honors_cancellation_before_expansion() {
    let graph = HncGraph::try_from(parse_chain("4").unwrap()).unwrap();
    let chart = solve(&graph).unwrap();
    assert!(matches!(
        filter_chart(&chart, &RewriteSystem::default(), || true),
        Err(FilterError::Cancelled)
    ));
}

#[test]
fn context_wildcards_filter_below_one_unchanged_parent() {
    let parsed = parse_domcon_oz(
        "[label(w wrap(w1)) label(x1 every(x2 x3)) label(y1 a(y2 y3)) label(z1 foo) label(z2 bar) label(z3 baz) dom(w1 x1) dom(x2 z1) dom(y2 z2) dom(x3 z3) dom(y3 z3)]",
    )
    .unwrap();
    let graph = HncGraph::try_from(parsed).unwrap();
    let chart = solve(&graph).unwrap();
    assert_eq!(chart.count_solutions().to_string(), "3");
    let rules = RewriteSystem::parse("*[a(X,every(Y,Z))] -> *[every(Y,a(X,Z))]").unwrap();
    let filtered = filter_chart(&chart, &rules, || false).unwrap();
    assert_eq!(filtered.count_solutions().to_string(), "2");
    assert_eq!(
        solution_terms(&filtered),
        ["a(bar,wrap(every(foo,baz)))", "wrap(every(foo,a(bar,baz)))",]
    );
}

#[test]
fn stefan_eigennamen_equivalences_reduce_two_scopings_to_one() {
    let parsed = parse_mrs_prolog(STEFAN_MRS).unwrap();
    let graph = HncGraph::try_from(parsed).unwrap();
    let chart = solve(&graph).unwrap();
    assert_eq!(chart.count_solutions().to_string(), "2");

    let rules = RewriteSystem::parse(STEFAN_EQUIVALENCES).unwrap();
    let filtered = filter_chart(&chart, &rules, || false).unwrap();
    assert_eq!(filtered.count_solutions().to_string(), "1");
    assert_eq!(solution_terms(&filtered).len(), 1);
}
