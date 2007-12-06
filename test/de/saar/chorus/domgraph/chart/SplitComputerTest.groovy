package de.saar.chorus.domgraph.chart;

import de.saar.testingtools.*;

import java.util.*;

import de.saar.chorus.domgraph.codec.*;
import de.saar.chorus.domgraph.codec.basic.*;
import de.saar.chorus.domgraph.graph.*;

class SplitComputerTest extends GroovyTestCase {
	private Chart chart = new Chart();
	private DomGraph graph = new DomGraph();
	private NodeLabels labels = new NodeLabels();
	

	public void testSolveChain3() {
		InputCodec codec = new Chain();
		codec.decode(codec.getReaderForSpecification("3"), graph, labels);
		
		assert ChartSolver.solve(graph, chart) == true;
	}
	
	public void testCompact() {
		// a compact dominance graph (this should be easy)
		checkSplit("x", "[label(x f(x1 x2)) dom(x1 y) label(y a) dom(x2 z) label(z a)]",
				["x1":[["y"]], "x2":[["z"]]], [:]);
	}
	
	public void testNormalNonCompact() {
		// a normal, non-compact dominance graph
		checkSplit("x", "[label(x f(x1 x2)) label(x1 f(x3 x4)) dom(x3 y) label(y a) dom(x2 z) label(z a)]",
				["x2":[["z"]], "x3":[["y"]]], [:]);
	}
	
	public void testWeaklyNormalNonCompact() {
		// a weakly normal, non-compact dominance graph
		checkSplit("x", "[label(x f(x1 x2)) label(x1 f(x3 x4)) dom(x1 y) label(y a) dom(x2 z) label(z a)]",
				["x1":[["y"]], "x2":[["z"]]], [:]);
	}
	
	public void testWeaklyNormalNonCompactDominatingEdges() {
		// a wn non-compact domgraph in which a hole and an ancestor of this hole
		// point into the same wcc
		checkSplit("x", "[label(x f(x1 x2)) label(x1 f(x3 x4)) dom(x3 y) dom(x1 y) label(y a) dom(x2 z) label(z a)]",
				["x2":[["z"]], "x3":[["y"]]], [:]);
	}
	
	public void testWeaklyNormalNonCompactDisjointDominators() {
		// a wn non-cpt domgraph in which two different holes point into the same wcc
		// (i.e. fragment is not free)
		checkNoSplit("x", "[label(x f(x1 x2)) label(x1 f(x3 x4)) dom(x3 y) dom(x4 y) label(y a) dom(x2 z) label(z a)]");
	}
	
	public void testNonHnc() {
		// a small non-hnc graph
		checkSplit("x", "[label(x f(x1)) dom(x1 y) dom(x1 z)]",
				["x1":[["y"],["z"]]], [:]);
	}
	

	public void testCrossEdge() {
		checkSplit("x", "[label(x f(x1 x2)) label(y g(y1 y2)) dom(y x2) dom(x z1) dom(y z2) dom(y2 z3)]",
				["x":[["z1"]], "y":[["z2"]], "y2":[["z3"]]], 
				["x2":"y"]);
	}
	
	public void testDownwardCrossEdge() {
		checkSplit("y", "[label(x f(x1 x2)) label(y g(y1 y2)) dom(y x2)]",
				["y":[["x","x1","x2"]]], [:]);
	}
	
	public void testCrossEdgeDominating() {
		checkSplit("x", "[label(x f(x1 x2)) label(y g(y1 y2)) dom(y x2) label(z1 g(z3)) dom(x z1) dom(y z2) dom(y2 z3)]",
				["y":[["z2"]], "y2":[["z1","z3"]]], ["x2":"y"]);
	}
	
	public void testCrossEdgeDisjoint() {
		checkNoSplit("x", "[label(x f(x1 x2)) label(y g(y1 y2)) dom(y x2) label(z1 g(z3)) dom(x1 z1) dom(y z2) dom(y2 z3)]");
	}
	
	public void testTwoCrossEdges() {
		// x2 has two incoming cross edges => unsolvable
		checkNoSplit("x", "[label(x f(x1 x2)) label(y g(y1 y2)) label(z1 g(z3)) dom(y x2) dom(z1 x2)]");
	}
	
	public void testNonTreeCrossEdges() {
		// y has two cross edges into disjoint nodes => superfragment not a tree, hence unsolvable
		checkNoSplit("x", "[label(x f(x1 x2)) label(y g(y1 y2)) dom(y x1) dom(y x2)]");
	}
	
	public void testIncomingNonCrossEdge() {
		// y has incoming dom edge which is not a cross edge => x is not free
		checkNoSplit("x", "[label(x f(x1 x2)) label(y g(y1 y2)) dom(y x1) label(z h(z1)) dom(z1 y)]");\
	}
	
	public void testCrossEdgeFromNonHole() {
		// dom edge out of the middle of y's fragment into a hole of x's fragment
		// => x is not free because this dom edge is equivalent to the non-cross edge (y1,x)
		checkNoSplit("x", "[label(x f(x1 x2)) label(y g(y1 y2)) label(y1 g(y3 y4)) dom(y1 x2)]");
	}
	
	public void testCrossEdgeIntoNonRoot() {
		// dom edge from the root x into the non-hole y1
		// => x is not free because this dom edge is equivalent to the non-cross edge (x,y)
		checkNoSplit("y", "[label(x f(x1 x2)) label(y g(y1 y2)) label(y1 g(y3 y4)) dom(x y1)]");
	}
	
	public void testThreeFragments1() {
		// three interconnected upper fragments; x is free
		checkSplit("x", "[label(x f(x1)) label(y g(y1)) label(z h(z1)) label(w a) dom(x y1) dom(y x1) dom(z y1) dom(y z1) dom(x1 w) dom(y1 w) dom(z1 w)]",
				["z1":[["w"]]], ["x1":"y", "y1":"z"]);
	}
	
	public void testThreeFragments2() {
		// the same three interconnected upper fragments as in testThreeFragments1; y is not free
		checkNoSplit("y", "[label(x f(x1)) label(y g(y1)) label(z h(z1)) label(w a) dom(x y1) dom(y x1) dom(z y1) dom(y z1) dom(x1 w) dom(y1 w) dom(z1 w)]");
	}
	
	public void testDisjointCrossEdges() {
		// y and z are plugged into disjoint holes of x; thus cross edge from z to y1 can't be satisfied
		checkNoSplit("x", "[label(x f(x1 x2)) label(y g(y1)) label(z h(z1)) dom(y x1) dom(z x2) dom(z y1)]");
	}
	
	public void testChainWithCrossEdgeOk() {
		// chain of length two with cross edge; hole with incoming cross edge is the correct one
		// according to the chain
		checkSplit("x", "[label(x f(x1 x2)) label(y g(y1 y2)) dom(x1 w) dom(y1 w) dom(y x1)]",
				["y1":[["w"]]], ["x1":"y"]);
	}
	
	public void testChainWithCrossEdgeFail() {
		// chain of length two with cross edge; hole with incoming cross edge is the wrong one
		// according to the chain
		checkNoSplit("x", "[label(x f(x1 x2)) label(y g(y1 y2)) dom(x1 w) dom(y1 w) dom(y x2)]");
	}
	
	public void testRedundantDomEdge() {
		checkSplit("y", "[label(x f(x1)) label(y g(y1)) dom(x1 w) dom(y1 w) dom(y x1)]",
				["y1":[["w","x","x1"]]], [:]);
	}

	// TODO cycles
	
	
	
	
	/**** utility methods ****/
	
	private void checkSplit(String root, String domcon, Map wccs, Map substitution) {
		TestingTools.decodeDomcon(domcon, graph, labels);
		
		SplitComputer comp = new SubgraphSplitComputer(graph);
		Split split = comp.computeSplit(root, new SubgraphNonterminal(graph.getAllNodes()));
		
		assert split != null;
		
		assert new HashSet(split.getAllDominators()).equals(wccs.keySet());
		
		for( dominator in wccs.keySet() ) {
			assert isWccListEqual(wccs.get(dominator), split.getWccs(dominator));
		}
		
		assert split.getSubstitution().equals(substitution);
	}
	
	private void checkNoSplit(String root, String domcon) {
		TestingTools.decodeDomcon(domcon, graph, labels);
		
		SplitComputer comp = new SubgraphSplitComputer(graph);
		Split split = comp.computeSplit(root, new SubgraphNonterminal(graph.getAllNodes()));
		
		assert split == null;
	}

	private boolean isWccListEqual(Collection gold, List result) {
		return new HashSet(gold.collect { new HashSet(it) }).equals(new HashSet(result));
	}
}