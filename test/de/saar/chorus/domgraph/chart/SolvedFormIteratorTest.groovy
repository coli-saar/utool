package de.saar.chorus.domgraph.chart;

import de.saar.testingtools.*;

import java.util.*;

import de.saar.chorus.domgraph.codec.*;
import de.saar.chorus.domgraph.codec.basic.*;
import de.saar.chorus.domgraph.graph.*;

class SolvedFormIteratorTest extends GroovyTestCase {
	private Chart chart = new Chart();
	private DomGraph graph = new DomGraph();
	private NodeLabels labels = new NodeLabels();
	
	
	public void testSmall() {
		checkSolvedForms("[label(x f(x1)) dom(x1 y) label(y a)]", [ [[["x1","y"]],[:]] ]);
	}
	
	public void testCrossEdge() {
		checkSolvedForms("[label(x f(x1)) label(y g(y1)) label(z a) dom(x1 z) dom(y1 z) dom(y x1)]",
				[ [[["y1","x"],["x1","z"]],[:]], [[["y1","z"]],["x1":"y"]] ] );
	}

	public void testThreeUpperFragments() {
		checkSolvedForms("[label(x f(x1)) label(y g(y1)) label(z h(z1)) label(w a) dom(x y1) dom(y x1) dom(z y1) dom(y z1) dom(x1 w) dom(y1 w) dom(z1 w)]",
				[ [[["z1","w"]],["x1":"y", "y1":"z"]], [[["x1","w"]],["z1":"y","y1":"x"]] ]);
	}
	
	public void testFourUpperFragments() {
		checkUnsolvable("[label(x f(x1)) label(y g(y1)) label(z h(z1)) label(v k(v1)) label(w a) dom(x y1) dom(y x1) dom(z y1) dom(y z1) dom(v y1) dom(y v1) dom(x1 w) dom(y1 w) dom(z1 w) dom(v1 w)]");
	}
	
	public void testChainWithCrossEdge1() {
		// chain with cross edge into connecting hole => two solved forms
		checkSolvedForms("[label(x f(x1 x2)) label(y g(y1 y2)) dom(x2 z) dom(y1 z) dom(y x2)]",
				[ [[["y1","x"],["x2","z"]],[:]], [[["y1","z"]],["x2":"y"]] ]);
	}
	
	public void testChainWithCrossEdge2() {
		// chain with cross edge into non-connecting hole => unsolvable
		checkSolvedForms("[label(x f(x1 x2)) label(y g(y1 y2)) dom(x2 z) dom(y1 z) dom(y x1)]",
				[ [[["y1","x"],["x2","z"]],[:]] ]);
	}
	
	public void testNonCompact1() {
		// a really simple non-compact graph
		checkSolvedForms("[label(x f(x1 x2)) label(x1 a) label(x2 b)]",
				[ [[],[:]] ]);
	}
	
	public void testNonCompact2() {
		// a non-compact graph that is almost as simple, but doesn't get the same NPE in SFI
		checkSolvedForms("[label(x f(x1 x2)) label(x1 a) label(y b) dom(x2 y)]",
				[ [[["x2","y"]],[:]] ]);
	}
	
	public void testPreprocessing1() {
		// a graph with a dom edge from an internal node to a labelled leaf; this
		// edge should be replaced by one from the internal node to the root
		checkSolvedForms("[label(x1 f(x2)) label(x2 g(x3)) label(y1 h(y2)) label(y2 a) dom(x2 y2)]",
				[ [[["x2","y1"]],[:]] ]);
	}
	
	public void testPreprocessing2() {
		// a graph with a trivial dominance edge
		checkSolvedForms("[label(x1 f(x2)) label(x2 g(x3)) label(x3 h(x4)) label(x4 a) dom(x2 x3)]",
				[ [[],[:]] ]);
	}
	
	public void testPreprocessing3() {
		// a graph with a trivially unsolvable dominance edge
		checkUnsolvable("[label(x1 f(x2 x3)) label(x2 a) label(x3 b) dom(x2 x3)]");
	}
	
	public void testNonTreeFragmentTwoInEdges() {
		// a graph with a node that has two incoming tree edges
		checkUnsolvable("[label(x1 f(x2 x3)) label(x2 g(x4)) label(x3 g(x4)) label(x4 a)]");
	}
	
	public void testNonTreeFragmentCycle() {
		// a graph with a cyclic fragment
		checkUnsolvable("[label(x1 f(x2)) label(x2 g(x1))]");
	}
	
	public void testEmptyChart1() {
		checkSolvedForms("[label(x a)]",
				[ [[],[:]] ]);
	}
	
	public void testEmptyChart2() {
		// two small connected components
		checkSolvedForms("[label(x a) label(y b)]",
				[ [[],[:]] ]);
	}
	
	/**** utility methods ****/
	
	private void checkSolvedForms(String domcon, List goldSolvedForms) {
		TestingTools.decodeDomcon(domcon, graph, labels);
		
		assert ChartSolver.solve(graph, chart) == true;
		
		SolvedFormIterator sfi = new SolvedFormIterator(chart, graph);
		List sfs = TestingTools.collectIteratorValues(sfi);
		
		assert solvedFormsEqual(sfs, goldSolvedForms) : "sfs = " + sfs;
	}
	
	private void checkUnsolvable(String domcon) {
		TestingTools.decodeDomcon(domcon, graph, labels);
		assert ChartSolver.solve(graph, chart) == false;
	}
	

	
	// Compare two lists of solved forms. The first (result) is a list of SolvedFormSpecs
	// as returned by a SolvedFormIterator. The second (gold) has the following form:
	//    gold     -> List(sf)
	//    sf       -> [List(domedge), substitution)
	//    domedge  -> [source,target]
	//    substitution -> map(string->string)
	private boolean solvedFormsEqual(List result, List gold) {
		List goldSolvedFormSpecs = gold.collect { sf ->
			List domEdges = sf.get(0).collect { new DomEdge(it.get(0), it.get(1)) };
			Map substitution = sf.get(1);
			
			new SolvedFormSpec(domEdges,substitution);
		};
		
		if( result.size() != gold.size() ) {
			return false;
		}
		
		for( spec in goldSolvedFormSpecs ) {
			if( result.find { 
				  (new HashSet(it.getDomEdges()).equals(new HashSet(spec.getDomEdges()))) && it.getSubstitution().equals(spec.getSubstitution()) 
				} == null ) {
				return false;
			}
		}
		
		return true;
	}
	
}
