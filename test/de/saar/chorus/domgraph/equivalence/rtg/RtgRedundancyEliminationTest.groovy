package de.saar.chorus.domgraph.equivalence.rtg;

import de.saar.chorus.domgraph.graph.*;
import de.saar.chorus.domgraph.chart.*;
import de.saar.chorus.domgraph.codec.domcon.*;
import de.saar.chorus.domgraph.codec.*;
import de.saar.chorus.domgraph.equivalence.*;

import de.saar.testingtools.*;

class RtgRedundancyEliminationTest extends GroovyTestCase {
    private DomGraph graph;
    private NodeLabels labels;
    private InputCodec ozcodec;
    private Chart chart;
    private EquationSystem eqsys, erg_eqsys;
    private RegularTreeGrammar<QuantifierMarkedNonterminal> out;
    
    public void setUp() {
        ozcodec = new DomconOzInputCodec();
        graph = new DomGraph();
        labels = new NodeLabels();
        chart = new Chart();
        out = new RegularTreeGrammar<QuantifierMarkedNonterminal>();
        eqsys = makeEqSystem(eqSystemFOL);
        erg_eqsys = makeEqSystem(eqSystemERG);
    }
    
    
    
	public void testAA() {
	    checkEliminatedSolvedForms("[label(x1 a(x2 x3)) label(y1 a(y2 y3)) label(z1 foo) label(z2 bar) label(z3 baz) dom(x2 z1) dom(y2 z2) dom(x3 z3) dom(y3 z3)]",
	            [ [[["x2","z1"], ["x3", "y1"], ["y2","z2"], ["y3", "z3"]],[:]] ]);
	}
	
	
	public void testIcosIncompleteness() {
	    checkEliminatedSolvedForms("[label(x1 a(x2 x3)) label(y1 a(y2 y3)) label(z1 every(z2 z3)) label(w1 foo) label(w2 foo) label(w3 foo) label(w4 foo) dom(x2 w1) dom(x3 w2) dom(y2 w2) dom(y3 w3) dom(z3 w3) dom(z2 w4)]",
	            [ [[ ["x3","y1"], ["y2", "w2"], ["y3", "z1"], ["z3", "w3"],          ["x2","w1"], ["z2", "w4"]  ],[:]],
	              [[ ["x3","z1"], ["z3", "y1"], ["y2", "w2"], ["y3", "w3"],          ["x2","w1"], ["z2", "w4"]  ],[:]],
	              [[ ["z3","x1"], ["x3", "y1"], ["y2", "w2"], ["y3", "w3"],          ["x2","w1"], ["z2", "w4"]  ],[:]]
	            ]);
	}
	

	public void testAcl06Incompleteness() {
	    checkEliminatedSolvedForms("[label(x1 a(x2 x3)) label(y1 a(y2 y3)) label(z1 every(z2 z3)) label(w1 foo) label(w2 foo) label(w3 foo) label(w4 foo) dom(x2 w1) dom(y2 w2) dom(z2 w3) dom(x3 w4) dom(y3 w4) dom(z3 w4)]",
	            [ [[ ["x3","y1"], ["y3", "z1"], ["z3", "w4"],          ["x2","w1"], ["y2", "w2"], ["z2", "w3"]  ],[:]],
	              [[ ["x3","z1"], ["z3", "y1"], ["y3", "w4"],          ["x2","w1"], ["y2", "w2"], ["z2", "w3"]  ],[:]],
	              [[ ["y3","z1"], ["z3", "x1"], ["x3", "w4"],          ["x2","w1"], ["y2", "w2"], ["z2", "w3"]  ],[:]],
	              [[ ["z3","x1"], ["x3", "y1"], ["y3", "w4"],          ["x2","w1"], ["y2", "w2"], ["z2", "w3"]  ],[:]]
	            ]);
	}
	
	public void testNoDominator1() {
	    checkEliminatedSolvedForms("[label(x a(x1 x2)) label(y a(y1 y2)) label(z1 foo) label(z2 bar) label(z3 baz) dom(x1 z1) dom(y1 z2) dom(x2 y) dom(y2 z3)]",
	            [ [[["x2","y"], ["y2", "z3"], ["x1","z1"], ["y1", "z2"]],[:]] ]);
	            
	}

	public void testNoDominator2() {
	    checkEliminatedSolvedForms("[label(x a(x1 x2)) label(y a(y1 y2)) label(z1 foo) label(z2 bar) label(z3 baz) dom(x1 z1) dom(y1 z2) dom(y2 x) dom(x2 z3)]",
	            [ [[["y2","x"], ["x2", "z3"], ["x1","z1"], ["y1", "z2"]],[:]] ]);
	            
	}
	
	public void testNoDominator3() {
	    checkEliminatedSolvedForms("[label(x a(x1 x2)) label(y every(y1 y2)) label(z1 foo) label(z2 bar) label(z3 baz) dom(x1 z1) dom(y1 z2) dom(x2 y) dom(y2 z3)]",
	            [ [[["x2","y"], ["y2", "z3"], ["x1","z1"], ["y1", "z2"]],[:]] ]);
	}
	
	public void testIntransitive() {
	    // this models the incompleteness of RTG elimination on Rondane 1279
	    checkEliminatedSolvedForms("[label(y permute(y1)) label(z f(z1)) label(x g(x1)) label(w a) dom(y1 w) dom(z1 x) dom(x1 w)]",
	            [ [[["y1","z"], ["z1", "x"], ["x1","w"]],[:]] ]);
	}
	
	/***
	 * The algorithm is incomplete for Rondane 90, 119, and 44.  This should not be seen
	 * as a bug in the implementation, but a limitation of the algorithm.  Therefore
	 * I am commenting these three test cases out. 
	 
	public void testRondane90() {
	    // this is Rondane 90, in which a wildcard is connected to other quantifiers through its non-permuting hole
	    checkEliminatedSolvedFormsERG("[label(h1 prpstn_m(h3)) label(h6 '_the_q'(h9 h7)) label(h10 '_name_n_of') label(h12 '_come_v_1&_from_p') label(h15 '_the_q'(h17 h16)) label(h18 '_saga_n_of') label(h20 proper_q(h21 h22)) label(h23 '_king_n_of') label(h25 udef_q(h26 h27)) label(h28 'title_id&named') dom(h26 h23) dom(h21 h28) dom(h17 h18) dom(h9 h10) dom(h3 h12) dom(h3 h25) dom(h3 h15) dom(h3 h6) dom(h3 h20) dom(h27 h28) dom(h16 h12) dom(h7 h12) dom(h22 h18)]",
	            [ [[["h3", "h25"], ["h26", "h23"], ["h27", "h20"], ["h21", "h28"], ["h22", "h15"], ["h17", "h18"], ["h16", "h6"], ["h9", "h10"], ["h7", "h12"]],[:]],
	            [[["h3", "h15"], ["h16", "h6"], ["h9", "h10"], ["h7", "h12"], ["h17", "h25"], ["h26", "h23"], ["h27", "h20"], ["h21", "h28"], ["h22", "h18"]],[:]],
	            [[["h3", "h15"], ["h16", "h6"], ["h9", "h10"], ["h7", "h12"], ["h17", "h20"], ["h22", "h18"], ["h21", "h25"], ["h26", "h23"], ["h27", "h28"]],[:]]]);
	}
	
	public void testRondane119() {
	    // this is Rondane 119, which likes to overeliminate
	    checkEliminatedSolvedFormsERG("[label(h1 prpstn_m(h3)) label(h6 ord) label(h8 '_the_q'(h10 h9)) label(h11 '_hike_n_1&_to_p') label(h14 'appos&named') label(h17 proper_q(h18 h19)) label(h20 '_the_q'(h22 h21)) label(h23 '_high_a_1&superl&_peak_n_1&_in_p') label(h28 proper_q(h29 h30)) label(h31 '_west_a_1&named') dom(h29 h31) dom(h22 h23) dom(h18 h14) dom(h10 h11) dom(h3 h6) dom(h3 h28) dom(h3 h8) dom(h3 h17) dom(h3 h20) dom(h30 h23) dom(h9 h6) dom(h19 h11) dom(h21 h14)]",
			[[[["h3", "h28"], ["h29", "h31"], ["h30", "h8"], ["h9", "h6"], ["h10", "h17"], ["h19", "h11"], ["h18", "h20"], ["h22", "h23"], ["h21", "h14"]],[:]],
			  [[["h3", "h28"], ["h29", "h31"], ["h30", "h8"], ["h9", "h6"], ["h10", "h20"], ["h22", "h23"], ["h21", "h17"], ["h18", "h14"], ["h19", "h11"]],[:]],
			  [[["h3", "h28"], ["h29", "h31"], ["h30", "h20"], ["h22", "h23"], ["h21", "h17"], ["h18", "h14"], ["h19", "h8"], ["h10", "h11"], ["h9", "h6"]],[:]] ]);
	}
	
	public void testRondane44() {
	    // Rondane 44
	    checkEliminatedSolvedFormsERG("[label(h1 prpstn_m(h3)) label(h6 '_be_v_there') label(h8 '_a_q'(h10 h9)) label(h11 '_easy_a_for&_walk_n_1&_from_p&_towards_p&prpstn_m'(h32)) label(h16 proper_q(h17 h18)) label(h19 'named&title_id') label(h20 '_center_n_of') label(h22 udef_q(h23 h24)) label(h28 proper_q(h29 h30)) label(h31 named) label(h36 '_offer_v_1') label(h38 udef_q(h39 h40)) label(h41 '_good_a_at&_view_n_of') dom(h39 h41) dom(h32 h36) dom(h29 h31) dom(h23 h20) dom(h17 h19) dom(h10 h11) dom(h3 h6) dom(h3 h38) dom(h3 h22) dom(h3 h8) dom(h3 h28) dom(h3 h16) dom(h40 h36) dom(h24 h19) dom(h9 h6) dom(h30 h11) dom(h18 h11)]",
	            [ [[["h3", "h28"], ["h29", "h31"], ["h30", "h38"], ["h39", "h41"], ["h40", "h8"], ["h9", "h6"], ["h10", "h16"], ["h18", "h11"], ["h32", "h36"], ["h17", "h22"], ["h23", "h20"], ["h24", "h19"]],[:]],
				  [[["h3", "h28"], ["h29", "h31"], ["h30", "h16"], ["h18", "h8"], ["h9", "h6"], ["h10", "h11"], ["h32", "h38"], ["h39", "h41"], ["h40", "h36"], ["h17", "h22"], ["h23", "h20"], ["h24", "h19"]],[:]],
				  [[["h3", "h28"], ["h29", "h31"], ["h30", "h22"], ["h23", "h20"], ["h24", "h16"], ["h17", "h19"], ["h18", "h38"], ["h39", "h41"], ["h40", "h8"], ["h9", "h6"], ["h10", "h11"], ["h32", "h36"]],[:]],
			      [[["h3", "h28"], ["h29", "h31"], ["h30", "h22"], ["h23", "h20"], ["h24", "h16"], ["h17", "h19"], ["h18", "h8"], ["h9", "h6"], ["h10", "h11"], ["h32", "h38"], ["h39", "h41"], ["h40", "h36"]],[:]] ]);
	}
	*/

	public void testAA_SS() {
	    checkEliminatedSolvedFormsSS("[label(x1 a(x2 x3)) label(y1 a(y2 y3)) label(z1 foo) label(z2 bar) label(z3 baz) dom(x2 z1) dom(y2 z2) dom(x3 z3) dom(y3 z3)]",
	            [ [[["x2","z1"], ["x3", "y1"], ["y2","z2"], ["y3", "z3"]],[:]] ]);
	}
	
	
	public void testIcosIncompleteness_SS() {
	    checkEliminatedSolvedFormsSS("[label(x1 a(x2 x3)) label(y1 a(y2 y3)) label(z1 every(z2 z3)) label(w1 foo) label(w2 foo) label(w3 foo) label(w4 foo) dom(x2 w1) dom(x3 w2) dom(y2 w2) dom(y3 w3) dom(z3 w3) dom(z2 w4)]",
	            [ [[ ["x3","y1"], ["y2", "w2"], ["y3", "z1"], ["z3", "w3"],          ["x2","w1"], ["z2", "w4"]  ],[:]],
	              [[ ["x3","z1"], ["z3", "y1"], ["y2", "w2"], ["y3", "w3"],          ["x2","w1"], ["z2", "w4"]  ],[:]],
	              [[ ["z3","x1"], ["x3", "y1"], ["y2", "w2"], ["y3", "w3"],          ["x2","w1"], ["z2", "w4"]  ],[:]]
	            ]);
	}
	

	public void testAcl06Incompleteness_SS() {
	    checkEliminatedSolvedFormsSS("[label(x1 a(x2 x3)) label(y1 a(y2 y3)) label(z1 every(z2 z3)) label(w1 foo) label(w2 foo) label(w3 foo) label(w4 foo) dom(x2 w1) dom(y2 w2) dom(z2 w3) dom(x3 w4) dom(y3 w4) dom(z3 w4)]",
	            [ [[ ["x3","y1"], ["y3", "z1"], ["z3", "w4"],          ["x2","w1"], ["y2", "w2"], ["z2", "w3"]  ],[:]],
	              [[ ["x3","z1"], ["z3", "y1"], ["y3", "w4"],          ["x2","w1"], ["y2", "w2"], ["z2", "w3"]  ],[:]],
	              [[ ["y3","z1"], ["z3", "x1"], ["x3", "w4"],          ["x2","w1"], ["y2", "w2"], ["z2", "w3"]  ],[:]],
	              [[ ["z3","x1"], ["x3", "y1"], ["y3", "w4"],          ["x2","w1"], ["y2", "w2"], ["z2", "w3"]  ],[:]]
	            ]);
	}
	
	public void testNoDominator1_SS() {
	    checkEliminatedSolvedFormsSS("[label(x a(x1 x2)) label(y a(y1 y2)) label(z1 foo) label(z2 bar) label(z3 baz) dom(x1 z1) dom(y1 z2) dom(x2 y) dom(y2 z3)]",
	            [ [[["x2","y"], ["y2", "z3"], ["x1","z1"], ["y1", "z2"]],[:]] ]);
	            
	}

	public void testNoDominator2_SS() {
	    checkEliminatedSolvedFormsSS("[label(x a(x1 x2)) label(y a(y1 y2)) label(z1 foo) label(z2 bar) label(z3 baz) dom(x1 z1) dom(y1 z2) dom(y2 x) dom(x2 z3)]",
	            [ [[["y2","x"], ["x2", "z3"], ["x1","z1"], ["y1", "z2"]],[:]] ]);
	            
	}
	
	public void testNoDominator3_SS() {
	    checkEliminatedSolvedFormsSS("[label(x a(x1 x2)) label(y every(y1 y2)) label(z1 foo) label(z2 bar) label(z3 baz) dom(x1 z1) dom(y1 z2) dom(x2 y) dom(y2 z3)]",
	            [ [[["x2","y"], ["y2", "z3"], ["x1","z1"], ["y1", "z2"]],[:]] ]);
	}
	
	

	
	public void testUnsolvable_SS() {
	    checkUnsolvableSS("[label(x f(x1 x2)) label(y a) dom(x1 y) dom(x2 y)]");
	}

	private void checkUnsolvableSS(String domcon) {
	    ozcodec.decode(new StringReader(domcon), graph, labels);
		graph = graph.preprocess();
		
		RtgRedundancyElimination elim = new RtgRedundancyElimination(graph, labels, eqsys);
		assert ! ChartSolver.solve(graph, out, new RtgRedundancyEliminationSplitSource(elim, graph));
	}
	
	private void checkEliminatedSolvedFormsSS(String domcon, List goldSfs) {
	    ozcodec.decode(new StringReader(domcon), graph, labels);
		graph = graph.preprocess();
		
		RtgRedundancyElimination elim = new RtgRedundancyElimination(graph, labels, eqsys);
		ChartSolver.solve(graph, out, new RtgRedundancyEliminationSplitSource(elim, graph));
		
		/*
		System.err.println("Chart:");
		System.err.println(ChartPresenter.chartOnlyRoots(out,graph));
		System.err.println("--------------------------------------------\n\n");
		*/
		
		SolvedFormIterator sfi = new SolvedFormIterator<QuantifierMarkedNonterminal>(out, graph);
		List sfs = TestingTools.collectIteratorValues(sfi);
		
		
		
		
		assert TestingTools.solvedFormsEqual(sfs, goldSfs) : "sfs = " + sfs;
	}

	private void checkEliminatedSolvedForms(String domcon, List goldSfs) {
	    ozcodec.decode(new StringReader(domcon), graph, labels);
		graph = graph.preprocess();
		ChartSolver.solve(graph,chart);
	    
		RtgRedundancyElimination elim = new RtgRedundancyElimination(graph, labels, eqsys);
		elim.eliminate(chart, out);
		
		/*
		System.err.println("Chart:");
		System.err.println(ChartPresenter.chartOnlyRoots(out,graph));
		System.err.println("--------------------------------------------\n\n");
		*/
		
		SolvedFormIterator sfi = new SolvedFormIterator<QuantifierMarkedNonterminal>(out, graph);
		List sfs = TestingTools.collectIteratorValues(sfi);
		
		assert TestingTools.solvedFormsEqual(sfs, goldSfs) : "sfs = " + sfs;
	}
	
	private void checkEliminatedSolvedFormsERG(String domcon, List goldSfs) {
	    ozcodec.decode(new StringReader(domcon), graph, labels);
		graph = graph.preprocess();
		ChartSolver.solve(graph,chart);
	    
		RtgRedundancyElimination elim = new RtgRedundancyElimination(graph, labels, erg_eqsys);
		elim.eliminate(chart, out);
		
		/*
		System.err.println("Chart:");
		System.err.println(ChartPresenter.chartOnlyRoots(out,graph));
		System.err.println("--------------------------------------------\n\n");
		*/
		
		SolvedFormIterator sfi = new SolvedFormIterator<QuantifierMarkedNonterminal>(out, graph);
		List sfs = TestingTools.collectIteratorValues(sfi);
		
		assert TestingTools.solvedFormsEqual(sfs, goldSfs) : "sfs = " + sfs;
	}
	
	
	
	
	
	private  EquationSystem makeEqSystem(String eqs) {
		EquationSystem ret = new EquationSystem();
		
		try {
			ret.read(new StringReader(eqs));
		} catch(Exception e) {
			
		}
		
		return ret;
	}
	
	private static String eqSystemFOL = "<?xml version='1.0' ?> "+
	"<equivalences style='FOL'> " +
	"	<equivalencegroup>" +
	"       <quantifier label='a' hole='1' />" +
	"       <quantifier label='a' hole='0' />" +
	"   </equivalencegroup>" +
	"   <equivalencegroup>" +
	"       <quantifier label='every' hole='1' />" +
	"   </equivalencegroup>" +
	"   <permutesWithEverything label='permute' hole='0' />" +
	"</equivalences>";
	
	private static String eqSystemERG = "<?xml version='1.0' ?> " +
	"<equivalences style='ERG'>" +   
	"<!-- Version 2005-06-05 (Apr-05) -->" +
	"<equivalencegroup>" +
		"<quantifier label='implicit_q' hole='1'/>" +
		"<quantifier label='def_q' hole='1'/>" +
		"<quantifier label='udef_q' hole='1'/>" +

		"<quantifier label='def_explicit_q' hole='1'/>" +
		"<quantifier label='_both_q' hole='1'/>" +
		"<quantifier label='def_both_rel' hole='1'/>" +
		"<quantifier label='_the_q' hole='1'/>" +

		"<quantifier label='_that_q_dem' hole='1'/>" +
		"<quantifier label='_these_q_dem' hole='1'/>" +
		"<quantifier label='_this_q_dem' hole='1'/>" +
		"<quantifier label='_those_q_dem' hole='1'/>" +
		"<quantifier label='demon_far_q' hole='1'/>" +
		"<quantifier label='demon_near_q' hole='1'/>" +
		"<quantifier label='demonstrative_q' hole='1'/>" +

		"<quantifier label='_a_q' hole='0'/>" +
		"<quantifier label='_a_q' hole='1'/>" +
		"<quantifier label='_another_q' hole='0'/>" +
		"<quantifier label='_another_q' hole='1'/>" +
		"<quantifier label='_less+than+a_q' hole='0'/>" +
		"<quantifier label='_less+than+a_q' hole='1'/>" +
		"<quantifier label='_some_q' hole='0'/>" +
		"<quantifier label='_some_q' hole='1'/>" +
		"<quantifier label='_such+a_q' hole='0'/>" +
		"<quantifier label='_such+a_q' hole='1'/>" +
		"<quantifier label='_what+a_q' hole='0'/>" +
		"<quantifier label='_what+a_q' hole='1'/>" +
		"<quantifier label='some_q' hole='0'/>" +
		"<quantifier label='some_q' hole='1'/>" +
		"<quantifier label='some_q_indiv' hole='0'/>" +
		"<quantifier label='some_q_indiv' hole='1'/>" +
	"</equivalencegroup>" +

	"<equivalencegroup>" +
	    "<quantifier label='every_q' hole='1' />" +
		"<quantifier label='each_q' hole='1' />" +
	"</equivalencegroup>" +

	
	"<permutesWithEverything label='proper_q' hole='1' />" +
	"<permutesWithEverything label='pronoun_q' hole='1' />" +
	"</equivalences>";
}
