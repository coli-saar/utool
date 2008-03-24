package de.saar.chorus.domgraph.chart;

import de.saar.testingtools.*;

import java.util.*;

import de.saar.chorus.domgraph.codec.*;
import de.saar.chorus.domgraph.codec.basic.*;
import de.saar.chorus.domgraph.graph.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

class SolvedFormIteratorTest  {
	private Chart chart = new Chart();
	private DomGraph graph = new DomGraph();
	private NodeLabels labels = new NodeLabels();
	
	@Test
	public void testSmall() {
		// a normal graph in solved form
		checkSolvedForms("[label(x f(x1)) dom(x1 y) label(y a)]", [ [[["x1","y"]],[:]] ]);
	}
	
	@Test
	public void testCrossEdge() {
		// a non weakly normal graph with one cross edge
		checkSolvedForms("[label(x f(x1)) label(y g(y1)) label(z a) dom(x1 z) dom(y1 z) dom(y x1)]",
				[ [[["y1","x"],["x1","z"]],[:]], [[["y1","z"]],["x1":"y"]] ] );
	}

	@Test
	public void testThreeUpperFragments() {
		// a non weakly normal graph with three upper fragments connected by cross edges
		checkSolvedForms("[label(x f(x1)) label(y g(y1)) label(z h(z1)) label(w a) dom(x y1) dom(y x1) dom(z y1) dom(y z1) dom(x1 w) dom(y1 w) dom(z1 w)]",
				[ [[["z1","w"]],["x1":"y", "y1":"z"]], [[["x1","w"]],["z1":"y","y1":"x"]] ]);
	}
	
	@Test
	public void testFourUpperFragments() {
		// a non weakly normal graph with four upper fragments connected by cross edges (unsolvable)
		checkUnsolvable("[label(x f(x1)) label(y g(y1)) label(z h(z1)) label(v k(v1)) label(w a) dom(x y1) dom(y x1) dom(z y1) dom(y z1) dom(v y1) dom(y v1) dom(x1 w) dom(y1 w) dom(z1 w) dom(v1 w)]");
	}
	
	@Test
	public void testChainWithCrossEdge1() {
		// chain with cross edge into connecting hole => two solved forms
		checkSolvedForms("[label(x f(x1 x2)) label(y g(y1 y2)) dom(x2 z) dom(y1 z) dom(y x2)]",
				[ [[["y1","x"],["x2","z"]],[:]], [[["y1","z"]],["x2":"y"]] ]);
	}
	
	@Test
	public void testChainWithCrossEdge2() {
		// chain with cross edge into non-connecting hole => unsolvable
		checkSolvedForms("[label(x f(x1 x2)) label(y g(y1 y2)) dom(x2 z) dom(y1 z) dom(y x1)]",
				[ [[["y1","x"],["x2","z"]],[:]] ]);
	}
	
	@Test
	public void testNonCompact1() {
		// a really simple non-compact graph
		checkSolvedForms("[label(x f(x1 x2)) label(x1 a) label(x2 b)]",
				[ [[],[:]] ]);
	}
	
	@Test
	public void testNonCompact2() {
		// a non-compact graph that is almost as simple, but doesn't get the same NPE in SFI
		checkSolvedForms("[label(x f(x1 x2)) label(x1 a) label(y b) dom(x2 y)]",
				[ [[["x2","y"]],[:]] ]);
	}
	
	@Test
	public void testPreprocessing1() {
		// a graph with a dom edge from an internal node to a labelled leaf; this
		// edge should be replaced by one from the internal node to the root
		checkSolvedForms("[label(x1 f(x2)) label(x2 g(x3)) label(y1 h(y2)) label(y2 a) dom(x2 y2)]",
				[ [[["x2","y1"]],[:]] ]);
	}
	
	@Test
	public void testPreprocessing2() {
		// a graph with a trivial dominance edge
		checkSolvedForms("[label(x1 f(x2)) label(x2 g(x3)) label(x3 h(x4)) label(x4 a) dom(x2 x3)]",
				[ [[],[:]] ]);
	}
	
	@Test
	public void testPreprocessing3() {
		// a graph with a trivially unsolvable dominance edge
		checkUnsolvable("[label(x1 f(x2 x3)) label(x2 a) label(x3 b) dom(x2 x3)]");
	}
	
	@Test
	public void testPreprocessing4() {
		checkUnsolvable("[label(x f(x1 x2)) dom(x1 x)]");
	}
	
	@Test
	public void testPreprocessing5() {
		TestingTools.decodeDomcon("[label(x f(x1 x2)) dom(x1 x2)]", graph, labels);
		TestingTools.expectException(DomGraph.PreprocessingException,
				{ graph.preprocess(); } );
	}
	
	
	
	
	@Test
	public void testNonTreeFragmentTwoInEdges() {
		// a graph with a node that has two incoming tree edges
		checkUnsolvable("[label(x1 f(x2 x3)) label(x2 g(x4)) label(x3 g(x4)) label(x4 a)]");
	}
	
	@Test
	public void testNonTreeFragmentCycle() {
		// a graph with a cyclic fragment
		checkUnsolvable("[label(x1 f(x2)) label(x2 g(x1))]");
	}
	
	@Test
	public void testEmptyChart1() {
		// a graph consisting only of a single fragment
		checkSolvedForms("[label(x a)]",
				[ [[],[:]] ]);
	}
	
	@Test
	public void testEmptyChart2() {
		// two small connected components
		checkSolvedForms("[label(x a) label(y b)]",
				[ [[],[:]] ]);
	}
	
	@Test
	public void testEmptyGraph() {
		// the empty graph has a single, trivial solved form
		checkSolvedForms("[]", [ [[],[:]] ]);
	}
	
	@Test
	public void testEmptyFragment() {
		// a graph with an empty fragment
		TestingTools.expectException(SolverNotApplicableException,
				{  checkUnsolvable("[label(x f(x1)) dom(x1 x2) dom(x2 y) label(y a)]") }
		);
	}
	
	@Test
	public void testWellFormed() {
		// a non-well-formed graph in the sense of Bodirsky et al. 04
		checkUnsolvable("[label(x a) label(y b) dom(x y)]");
	}
	
	@Test
	public void testWellFormed2() {
		// a non-well-formed graph in the sense of Bodirsky et al. 04
		checkUnsolvable("[label(x f(x1 x2)) label(x1 a1) label(x2 a2) label(y b) dom(x y)]");
	}
	
	
	/**** utility methods ****/
	
	private void checkSolvedForms(String domcon, List goldSolvedForms) {
		TestingTools.decodeDomcon(domcon, graph, labels);
		TestingTools.checkSolvedForms(graph, goldSolvedForms);
	}
	
	private void checkUnsolvable(String domcon) {
		TestingTools.decodeDomcon(domcon, graph, labels);
		TestingTools.checkUnsolvable(graph);
	}
	
}
