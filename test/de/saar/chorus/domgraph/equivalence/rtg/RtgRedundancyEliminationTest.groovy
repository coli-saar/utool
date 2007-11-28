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
    private EquationSystem eqsys;
    private RegularTreeGrammar<QuantifierMarkedNonterminal> out;
    
    public void setUp() {
        ozcodec = new DomconOzInputCodec();
        graph = new DomGraph();
        labels = new NodeLabels();
        chart = new Chart();
        out = new RegularTreeGrammar<QuantifierMarkedNonterminal>();
        eqsys = makeEqSystem(eqSystemFOL);
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


	private void checkEliminatedSolvedForms(String domcon, List goldSfs) {
	    ozcodec.decode(new StringReader(domcon), graph, labels);
		graph = graph.preprocess();
		ChartSolver.solve(graph,chart);
	    
		RtgRedundancyElimination elim = new RtgRedundancyElimination(graph, labels, eqsys);
		elim.eliminate(chart, out);
		
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
	"</equivalences>";
}