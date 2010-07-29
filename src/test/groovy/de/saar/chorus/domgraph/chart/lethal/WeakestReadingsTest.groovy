/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.saar.chorus.domgraph.chart.lethal


import de.saar.chorus.domgraph.graph.*;
import de.saar.chorus.domgraph.chart.*;
import de.saar.chorus.domgraph.codec.domcon.*;
import de.saar.chorus.domgraph.codec.*;

import de.saar.testingtools.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 *
 * @author koller
 */

@RunWith(value=Parameterized.class)
class WeakestReadingsTest {
    private DomGraph graph;
    private NodeLabels labels;
    private InputCodec ozcodec;
    private Chart chart;
    private RelativeNormalFormsComputer rnfc;
    private List goldSfs;
    private String id;


    @Parameters
    public static data() {
        return [p("EA", "[label(x1 every(x2 x3)) label(y1 a(y2 y3)) label(z1 foo) label(z2 bar) label(z3 baz) dom(x2 z1) dom(y2 z2) dom(x3 z3) dom(y3 z3)]",
                    rewriteSystemFol, [ [[["x2","z1"], ["x3", "y1"], ["y2","z2"], ["y3", "z3"]],[:]] ]),

                // rules c+ and c- from Alexander's thesis (sensitivity to annotations)
                p("thesis c-", "[label(x1 every(x2 x3)) label(y1 every(y2 y3)) label(a1 not(a2)) label(z1 b) label(z2 b) label(z3 b) dom(a2 x1) dom(a2 y1) dom(x2 z1) dom(x3 z2) dom(y2 z2) dom(y3 z3)]",
                    rewriteSystemFol, [ [[["a2", "x1"], ["x2", "z1"], ["x3", "y1"], ["y2", "z2"], ["y3", "z3"]],[:]] ]),

                p("thesis c+", "[label(x1 every(x2 x3)) label(y1 every(y2 y3)) label(z1 b) label(z2 b) label(z3 b) dom(x2 z1) dom(x3 z2) dom(y2 z2) dom(y3 z3)]",
                    rewriteSystemFol, [ [[["y3", "z3"], ["y2", "x1"], ["x2", "z1"], ["x3", "z2"]],[:]] ]),

                // some test cases for weakening that can use equivalence permutations as well
                p("only equiv", "[label(x1 every(x2 x3)) label(y1 every(y2 y3)) label(y3 every(y4 y5)) label(z1 foo) label(z2 bar) label(z3 baz) label(z4 bazz) dom(x2 z1) dom(y2 z2) dom(x3 z3) dom(y4 z4) dom(y5 z3)]",
                    rewriteSystemFol, [[[["x2", "z1"], ["x3", "y1"], ["y2", "z2"], ["y4", "z4"], ["y5", "z3"]],[:]], [[["y2", "z2"], ["y4", "z4"], ["y5", "x1"], ["x2", "z1"], ["x3", "z3"]],[:]]]),

                p("null equiv", "[label(x1 every(x2 x3)) label(y1 a(y2 y3)) label(y3 every(y4 y5)) label(z1 foo) label(z2 bar) label(z3 baz) label(z4 bazz) dom(x2 z1) dom(y2 z2) dom(x3 z3) dom(y4 z4) dom(y5 z3)]",
                    rewriteSystemFolNoEq, [[[["x2", "z1"], ["x3", "y1"], ["y2", "z2"], ["y4", "z4"], ["y5", "z3"]],[:]], [[["y2", "z2"], ["y4", "z4"], ["y5", "x1"], ["x2", "z1"], ["x3", "z3"]],[:]]]),

                p("AA", "[label(x1 a(x2 x3)) label(y1 a(y2 y3)) label(z1 foo) label(z2 bar) label(z3 baz) dom(x2 z1) dom(y2 z2) dom(x3 z3) dom(y3 z3)]",
                    eqSystemFol, [ [[["x2","z1"], ["x3", "y1"], ["y2","z2"], ["y3", "z3"]],[:]] ]),

		p("IcosIncompleteness", "[label(x1 a(x2 x3)) label(y1 a(y2 y3)) label(z1 every(z2 z3)) label(w1 foo) label(w2 foo) label(w3 foo) label(w4 foo) dom(x2 w1) dom(x3 w2) dom(y2 w2) dom(y3 w3) dom(z3 w3) dom(z2 w4)]",
                    eqSystemFol,  [ [[ ["x3","y1"], ["y2", "w2"], ["y3", "z1"], ["z3", "w3"],          ["x2","w1"], ["z2", "w4"]  ],[:]],
						[[ ["x3","z1"], ["z3", "y1"], ["y2", "w2"], ["y3", "w3"],          ["x2","w1"], ["z2", "w4"]  ],[:]],
						[[ ["z3","x1"], ["x3", "y1"], ["y2", "w2"], ["y3", "w3"],          ["x2","w1"], ["z2", "w4"]  ],[:]]
						]),

		p("Acl06Incompleteness", "[label(x1 a(x2 x3)) label(y1 a(y2 y3)) label(z1 every(z2 z3)) label(w1 foo) label(w2 foo) label(w3 foo) label(w4 foo) dom(x2 w1) dom(y2 w2) dom(z2 w3) dom(x3 w4) dom(y3 w4) dom(z3 w4)]",
                    eqSystemFol, [ [[ ["x3","y1"], ["y3", "z1"], ["z3", "w4"],          ["x2","w1"], ["y2", "w2"], ["z2", "w3"]  ],[:]],
						[[ ["x3","z1"], ["z3", "y1"], ["y3", "w4"],          ["x2","w1"], ["y2", "w2"], ["z2", "w3"]  ],[:]],
						[[ ["y3","z1"], ["z3", "x1"], ["x3", "w4"],          ["x2","w1"], ["y2", "w2"], ["z2", "w3"]  ],[:]],
						[[ ["z3","x1"], ["x3", "y1"], ["y3", "w4"],          ["x2","w1"], ["y2", "w2"], ["z2", "w3"]  ],[:]]
						]),

		p("NoDominator1", "[label(x a(x1 x2)) label(y a(y1 y2)) label(z1 foo) label(z2 bar) label(z3 baz) dom(x1 z1) dom(y1 z2) dom(x2 y) dom(y2 z3)]",
                    eqSystemFol, [ [[["x2","y"], ["y2", "z3"], ["x1","z1"], ["y1", "z2"]],[:]] ]),

		p("NoDominator2", "[label(x a(x1 x2)) label(y a(y1 y2)) label(z1 foo) label(z2 bar) label(z3 baz) dom(x1 z1) dom(y1 z2) dom(y2 x) dom(x2 z3)]",
                    eqSystemFol, [ [[["y2","x"], ["x2", "z3"], ["x1","z1"], ["y1", "z2"]],[:]] ]),

		p("NoDominator3", "[label(x a(x1 x2)) label(y every(y1 y2)) label(z1 foo) label(z2 bar) label(z3 baz) dom(x1 z1) dom(y1 z2) dom(x2 y) dom(y2 z3)]",
                    eqSystemFol, [ [[["x2","y"], ["y2", "z3"], ["x1","z1"], ["y1", "z2"]],[:]] ]),

		// this models the incompleteness of RTG elimination on Rondane 1279
		p("Intransitive", "[label(y permute(y1)) label(z f(z1)) label(x g(x1)) label(w a) dom(y1 w) dom(z1 x) dom(x1 w)]",
                    eqSystemFol,  [ [[["z1", "x"], ["x1","y"], ["y1","w"]],[:]] ])




            /** test cases for weakening for non-compact fragments don't work in ACL10 any more, because
             *  strong readings can't be rewritten into weak readings _in one step_.

                // weakening in non-compact fragments
                p("EEA", "[label(x1 every(x2 x3)) label(y1 a(y2 y3)) label(y3 a(y4 y5)) label(z1 foo) label(z2 bar) label(z3 baz) label(z4 bazz) dom(x2 z1) dom(y2 z2) dom(x3 z3) dom(y4 z4) dom(y5 z3)]",
                    rewriteSystemFol,[ [[["x2","z1"], ["x3", "y1"], ["y2","z2"], ["y4","z4"], ["y5", "z3"]],[:]] ]),

                p("weakening + equiv", "[label(x1 every(x2 x3)) label(y1 a(y2 y3)) label(y3 every(y4 y5)) label(z1 foo) label(z2 bar) label(z3 baz) label(z4 bazz) dom(x2 z1) dom(y2 z2) dom(x3 z3) dom(y4 z4) dom(y5 z3)]",
                    rewriteSystemFol, [ [[["x2","z1"], ["x3", "y1"], ["y2","z2"], ["y4","z4"], ["y5", "z3"]],[:]] ]),

                // The following two test cases check that annotations are taken into account correctly when checking
                // permutability of non-compact fragments: The negation gets permuted up _first_, and further rewrites
                // between f and g must be checked in negative polarity.
                p("non-cpt polarity 1", "[label(x1 not(x2)) label(x2 g(x3)) label(y1 f(y2)) label(z b) dom(x3 z) dom(y2 z)]",
                    rewriteSystemFG,  [[[["x3", "y1"], ["y2", "z"]],[:]] ]),

                p("non-cpt polarity 2", "[label(x1 not(x2)) label(x2 f(x3)) label(y1 g(y2)) label(z b) dom(x3 z) dom(y2 z)]",
                    rewriteSystemFG, [[[["x3", "y1"], ["y2", "z"]],[:]], [[["y2", "x1"], ["x3", "z"]],[:]]])



                // from equivalence test suite:  ( prepareFOL -> p(..., eqSystemFol, ...) )

                prepareFOL("not compact", "[label(x a(x1 x2)) label(x2 a(x3 x4)) label(y a(y1 y2)) label(z1 b) label(z2 b) label(z3 b) label(z4 b) dom(x1 z1) dom(x3 z2) dom(x4 z3) dom(y2 z3) dom(y1 z4)]",
				[ [[["x1", "z1"], ["x3", "z2"], ["x4", "y"], ["y1","z4"], ["y2","z3"]], [:]]]),

		prepareFOL("not compact/wildcard in lower", "[label(x f(x1 x2)) label(x2 g(x3 x4)) label(y permute(y1)) label(z1 b) label(z2 b) label(z3 b) dom(x1 z1) dom(x3 z2) dom(x4 z3) dom(y1 z3)]",
				[ [[["y1", "x"], ["x3", "z2"], ["x4", "z3"], ["x1","z1"]], [:]]]),

		prepareFOL("not compact/wildcard in upper", "[label(x a(x1 x2)) label(x2 permute(x4)) label(y a(y1 y2)) label(z1 b) label(z3 b) label(z4 b) dom(x1 z1) dom(x4 z3) dom(y2 z3) dom(y1 z4)]",
				[ [[["x1", "z1"], ["x4", "y"], ["y1","z4"], ["y2","z3"]], [:]]]),






                // eq system is not converted yet
                prepareERG("Rondane-1", "[label(h1 prpstn_m(h3)) label(h6 '_the_q'(h9 h7)) label(h10 '_well+known_a_1') label(h12 '_and_c&compound&_valley_n_of'(h10 h14)) label(h14 '_historic_a_1') label(h18 proper_q(h19 h20)) label(h21 named) label(h23 '_be_v_id&_once_a_1') label(h26 'part_of&card') label(h28 udef_q(h29 h30)) label(h32 '_the_q'(h34 h33)) label(h35 '_main_a_1&_route_n_1&_between_p') label(h39 '_the_q'(h41 h40)) label(h42 '_eastern_a_1') label(h44 '_and_c&_part_n_1&_of_p'(h42 h46)) label(h46 '_western_a_1') label(h50 proper_q(h51 h52)) label(h53 named) dom(h51 h53) dom(h41 h44) dom(h34 h35) dom(h29 h26) dom(h19 h21) dom(h9 h12) dom(h3 h23) dom(h3 h39) dom(h3 h28) dom(h3 h18) dom(h3 h32) dom(h3 h50) dom(h3 h6) dom(h40 h35) dom(h30 h23) dom(h20 h12) dom(h33 h26) dom(h52 h44) dom(h7 h23)]",
				[[[["h3", "h18"], ["h19", "h21"], ["h20", "h50"], ["h51", "h53"], ["h52", "h39"], ["h41", "h44"], ["h40", "h28"], ["h30", "h6"], ["h9", "h12"], ["h7", "h23"], ["h29", "h32"], ["h34", "h35"], ["h33", "h26"]],[:]],
						[[["h3", "h18"], ["h19", "h21"], ["h20", "h50"], ["h51", "h53"], ["h52", "h39"], ["h41", "h44"], ["h40", "h32"], ["h34", "h35"], ["h33", "h28"], ["h29", "h26"], ["h30", "h6"], ["h9", "h12"], ["h7", "h23"]],[:]],
						[[["h3", "h18"], ["h19", "h21"], ["h20", "h50"], ["h51", "h53"], ["h52", "h28"], ["h30", "h6"], ["h9", "h12"], ["h7", "h23"], ["h29", "h39"], ["h41", "h44"], ["h40", "h32"], ["h34", "h35"], ["h33", "h26"]],[:]],
						[[["h3", "h18"], ["h19", "h21"], ["h20", "h50"], ["h51", "h53"], ["h52", "h28"], ["h30", "h6"], ["h9", "h12"], ["h7", "h23"], ["h29", "h32"], ["h33", "h26"], ["h34", "h39"], ["h41", "h44"], ["h40", "h35"]],[:]],
						[[["h3", "h18"], ["h19", "h21"], ["h20", "h50"], ["h51", "h53"], ["h52", "h32"], ["h33", "h28"], ["h29", "h26"], ["h30", "h6"], ["h9", "h12"], ["h7", "h23"], ["h34", "h39"], ["h41", "h44"], ["h40", "h35"]],[:]]]),




             * The algorithm is incomplete for Rondane 90, 119, and 44.  This should not be seen
	 * as a bug in the implementation, but a limitation of the algorithm.  Therefore
	 * I am commenting these three test cases out.
	 *
	 @Test
	 public void testRondane90() {
	 // this is Rondane 90, in which a wildcard is connected to other quantifiers through its non-permuting hole
	 checkEliminatedSolvedFormsERG("[label(h1 prpstn_m(h3)) label(h6 '_the_q'(h9 h7)) label(h10 '_name_n_of') label(h12 '_come_v_1&_from_p') label(h15 '_the_q'(h17 h16)) label(h18 '_saga_n_of') label(h20 proper_q(h21 h22)) label(h23 '_king_n_of') label(h25 udef_q(h26 h27)) label(h28 'title_id&named') dom(h26 h23) dom(h21 h28) dom(h17 h18) dom(h9 h10) dom(h3 h12) dom(h3 h25) dom(h3 h15) dom(h3 h6) dom(h3 h20) dom(h27 h28) dom(h16 h12) dom(h7 h12) dom(h22 h18)]",
	 [ [[["h3", "h25"], ["h26", "h23"], ["h27", "h20"], ["h21", "h28"], ["h22", "h15"], ["h17", "h18"], ["h16", "h6"], ["h9", "h10"], ["h7", "h12"]],[:]],
	 [[["h3", "h15"], ["h16", "h6"], ["h9", "h10"], ["h7", "h12"], ["h17", "h25"], ["h26", "h23"], ["h27", "h20"], ["h21", "h28"], ["h22", "h18"]],[:]],
	 [[["h3", "h15"], ["h16", "h6"], ["h9", "h10"], ["h7", "h12"], ["h17", "h20"], ["h22", "h18"], ["h21", "h25"], ["h26", "h23"], ["h27", "h28"]],[:]]]);
	 }
	 @Test
	 public void testRondane119() {
	 // this is Rondane 119, which likes to overeliminate
	 checkEliminatedSolvedFormsERG("[label(h1 prpstn_m(h3)) label(h6 ord) label(h8 '_the_q'(h10 h9)) label(h11 '_hike_n_1&_to_p') label(h14 'appos&named') label(h17 proper_q(h18 h19)) label(h20 '_the_q'(h22 h21)) label(h23 '_high_a_1&superl&_peak_n_1&_in_p') label(h28 proper_q(h29 h30)) label(h31 '_west_a_1&named') dom(h29 h31) dom(h22 h23) dom(h18 h14) dom(h10 h11) dom(h3 h6) dom(h3 h28) dom(h3 h8) dom(h3 h17) dom(h3 h20) dom(h30 h23) dom(h9 h6) dom(h19 h11) dom(h21 h14)]",
	 [[[["h3", "h28"], ["h29", "h31"], ["h30", "h8"], ["h9", "h6"], ["h10", "h17"], ["h19", "h11"], ["h18", "h20"], ["h22", "h23"], ["h21", "h14"]],[:]],
	 [[["h3", "h28"], ["h29", "h31"], ["h30", "h8"], ["h9", "h6"], ["h10", "h20"], ["h22", "h23"], ["h21", "h17"], ["h18", "h14"], ["h19", "h11"]],[:]],
	 [[["h3", "h28"], ["h29", "h31"], ["h30", "h20"], ["h22", "h23"], ["h21", "h17"], ["h18", "h14"], ["h19", "h8"], ["h10", "h11"], ["h9", "h6"]],[:]] ]);
	 }
	 @Test
	 public void testRondane44() {
	 // Rondane 44
	 checkEliminatedSolvedFormsERG("[label(h1 prpstn_m(h3)) label(h6 '_be_v_there') label(h8 '_a_q'(h10 h9)) label(h11 '_easy_a_for&_walk_n_1&_from_p&_towards_p&prpstn_m'(h32)) label(h16 proper_q(h17 h18)) label(h19 'named&title_id') label(h20 '_center_n_of') label(h22 udef_q(h23 h24)) label(h28 proper_q(h29 h30)) label(h31 named) label(h36 '_offer_v_1') label(h38 udef_q(h39 h40)) label(h41 '_good_a_at&_view_n_of') dom(h39 h41) dom(h32 h36) dom(h29 h31) dom(h23 h20) dom(h17 h19) dom(h10 h11) dom(h3 h6) dom(h3 h38) dom(h3 h22) dom(h3 h8) dom(h3 h28) dom(h3 h16) dom(h40 h36) dom(h24 h19) dom(h9 h6) dom(h30 h11) dom(h18 h11)]",
	 [ [[["h3", "h28"], ["h29", "h31"], ["h30", "h38"], ["h39", "h41"], ["h40", "h8"], ["h9", "h6"], ["h10", "h16"], ["h18", "h11"], ["h32", "h36"], ["h17", "h22"], ["h23", "h20"], ["h24", "h19"]],[:]],
	 [[["h3", "h28"], ["h29", "h31"], ["h30", "h16"], ["h18", "h8"], ["h9", "h6"], ["h10", "h11"], ["h32", "h38"], ["h39", "h41"], ["h40", "h36"], ["h17", "h22"], ["h23", "h20"], ["h24", "h19"]],[:]],
	 [[["h3", "h28"], ["h29", "h31"], ["h30", "h22"], ["h23", "h20"], ["h24", "h16"], ["h17", "h19"], ["h18", "h38"], ["h39", "h41"], ["h40", "h8"], ["h9", "h6"], ["h10", "h11"], ["h32", "h36"]],[:]],
	 [[["h3", "h28"], ["h29", "h31"], ["h30", "h22"], ["h23", "h20"], ["h24", "h16"], ["h17", "h19"], ["h18", "h8"], ["h9", "h6"], ["h10", "h11"], ["h32", "h38"], ["h39", "h41"], ["h40", "h36"]],[:]] ]);
	 }
                    */

        ];
    }


    @Test
    public void testWeakestSolvedForms() {
        Chart chart = new Chart();

        System.err.println("\ntest for " + id + ":");

        graph = graph.preprocess();
        chart.clear();
        ChartSolver.solve(graph,chart);

        RegularTreeGrammar<DecoratedNonterminal<SubgraphNonterminal,String>> reduced = rnfc.reduceToChart(chart, graph, labels);

        SolvedFormIterator sfi = new SolvedFormIterator<DecoratedNonterminal<SubgraphNonterminal,String>>(reduced, graph);
        List sfs = TestingTools.collectIteratorValues(sfi);

        assert TestingTools.solvedFormsEqual(sfs, goldSfs) : "[" + id + "] found: " + sfs + ", expected: " + goldSfs;
    }



    WeakestReadingsTest(id, graphstr, intendedSolvedForms, rewriteSystem) {
        ozcodec = new DomconOzInputCodec();
        graph = new DomGraph();
        labels = new NodeLabels();

        ozcodec.decode(new StringReader(graphstr), graph, labels);


        RewritingSystemParser parser = new RewritingSystemParser();
        RewriteSystem weakening = new RewriteSystem(true);
        RewriteSystem equivalence = new RewriteSystem(false);
        Annotator ann = new Annotator();
        parser.read(new StringReader(rewriteSystem), weakening, equivalence, ann);

        rnfc = new RelativeNormalFormsComputer(ann);
        rnfc.addRewriteSystem(weakening);
        rnfc.addRewriteSystem(equivalence, new EquivalenceRulesComparator());

        goldSfs = intendedSolvedForms;
        this.id = id;
    }


    static Object[] p(id, graphstr, ruleSystem, intendedSolvedForms) {
        return (Object[]) [id, graphstr, intendedSolvedForms, ruleSystem];
    }


    public static String rewriteSystemFol = '''
        // annotator
        start annotation: +
        neutral annotation: 0
        +: a(+,+)
        -: a(-,-)
        +: every(-,+)
        -: every(+,-)
        +: not(-)
        -: not(+)

        // weakening
        [+] a(X,every(Y,Z)) -> every(Y, a(X,Z))
        [-] every(X,a(Y,Z)) -> a(Y, every(X,Z))
        [+] not(a(X,Y)) -> a(X, not(Y))
        [+] every(X, not(Y)) -> not(every(X,Y))
        [-] every(every(X,Y),Z) -> every(X,every(Y,Z))
        [+] every(X,every(Y,Z)) -> every(every(X,Y),Z)

        // equivalence
        a(X,a(Y,Z)) = a(Y,a(X,Z)) // 1-1
        a(X,a(Y,Z)) = a(a(X,Y),Z) // 1-0
        a(a(X,Y),Z) = a(a(X,Z),Y) // 0-0
        every(X,every(Y,Z)) = every(Y,every(X,Z))
''';

    public static String rewriteSystemFolNoEq = '''
        // annotator
        start annotation: +
        neutral annotation: 0
        +: a(+,+)
        -: a(-,-)
        +: every(-,+)
        -: every(+,-)
        +: not(-)
        -: not(+)

        // weakening
        [+] a(X,every(Y,Z)) -> every(Y, a(X,Z))
        [-] every(X,a(Y,Z)) -> a(Y, every(X,Z))
        [+] not(a(X,Y)) -> a(X, not(Y))
        [+] every(X, not(Y)) -> not(every(X,Y))
        [-] every(every(X,Y),Z) -> every(X,every(Y,Z))
        [+] every(X,every(Y,Z)) -> every(every(X,Y),Z)
''';


    public static String eqSystemFol = '''
        // annotator
        start annotation: +
        neutral annotation: 0
        +: a(+,+)
        -: a(-,-)
        +: every(-,+)
        -: every(+,-)
        +: not(-)
        -: not(+)

        // equivalence
        a(X,a(Y,Z)) = a(Y,a(X,Z)) // 1-1
        a(X,a(Y,Z)) = a(a(X,Y),Z) // 1-0
        a(a(X,Y),Z) = a(a(X,Z),Y) // 0-0
        every(X,every(Y,Z)) = every(Y,every(X,Z))

        *[permute(P)] = permute(*[P])
''';


    public static String rewriteSystemFG = '''
        start annotation: +
        neutral annotation: 0
        +: not(-)
        -: not(+)
        +:f(+)
        -:f(-)
        +:g(+)
        -:g(-)

        [+] f(not(X)) -> not(f(X))
        [+] g(not(X)) -> not(g(X))
        [+] g(f(X)) -> f(g(X))
        [-] f(g(X)) -> g(f(X))''';



    /* -- need to convert this



    public static String eqSystemERG = '''<?xml version="1.0" ?>
	<equivalences style="ERG">
	<!-- Version 2005-06-05 (Apr-05) -->
	<equivalencegroup>
		<quantifier label="implicit_q" hole="1"/>
		<quantifier label="def_q" hole="1"/>
		<quantifier label="udef_q" hole="1"/>

		<quantifier label="def_explicit_q" hole="1"/>
		<quantifier label="_both_q" hole="1"/>
		<quantifier label="def_both_rel" hole="1"/>
		<quantifier label="_the_q" hole="1"/>

		<quantifier label="_that_q_dem" hole="1"/>
		<quantifier label="_these_q_dem" hole="1"/>
		<quantifier label="_this_q_dem" hole="1"/>
		<quantifier label="_those_q_dem" hole="1"/>
		<quantifier label="demon_far_q" hole="1"/>
		<quantifier label="demon_near_q" hole="1"/>
		<quantifier label="demonstrative_q" hole="1"/>

		<quantifier label="_a_q" hole="0"/>
		<quantifier label="_a_q" hole="1"/>
		<quantifier label="_another_q" hole="0"/>
		<quantifier label="_another_q" hole="1"/>
		<quantifier label="_less+than+a_q" hole="0"/>
		<quantifier label="_less+than+a_q" hole="1"/>
		<quantifier label="_some_q" hole="0"/>
		<quantifier label="_some_q" hole="1"/>
		<quantifier label="_such+a_q" hole="0"/>
		<quantifier label="_such+a_q" hole="1"/>
		<quantifier label="_what+a_q" hole="0"/>
		<quantifier label="_what+a_q" hole="1"/>
		<quantifier label="some_q" hole="0"/>
		<quantifier label="some_q" hole="1"/>
		<quantifier label="some_q_indiv" hole="0"/>
		<quantifier label="some_q_indiv" hole="1"/>
	</equivalencegroup>

	<equivalencegroup>
	    <quantifier label="every_q" hole="1" />
		<quantifier label="each_q" hole="1" />
	</equivalencegroup>


	<permutesWithEverything label="proper_q" hole="1" />
	<permutesWithEverything label="pronoun_q" hole="1" />
	</equivalences>
	''';

    */
}

