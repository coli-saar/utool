package de.saar.chorus.domgraph.chart;

import de.saar.testingtools.*;

import java.util.*;

import de.saar.chorus.domgraph.codec.*;
import de.saar.chorus.domgraph.codec.basic.*;
import de.saar.chorus.domgraph.graph.*;

class SplitComputerTest extends GroovyTestCase {
	private Chart chart;
	private DomGraph graph = new DomGraph();
	private NodeLabels labels = new NodeLabels();
	
	
	public void setUp() {
		chart = new Chart();
	}
	
	
	public void testSolveChain3() {
		InputCodec codec = new Chain();
		codec.decode(codec.getReaderForSpecification("3"), graph, labels);
		
		assert ChartSolver.solve(graph, chart) == true;
	}
	
	private boolean isWccListEqual(Collection gold, List result) {
		return new HashSet(gold.collect { new HashSet(it) }).equals(new HashSet(result));
	}
	
	public void testCompact() {
		// a compact dominance graph (this should be easy)
		TestingTools.decodeDomcon("[label(x f(x1 x2)) dom(x1 y) label(y a) dom(x2 z) label(z a)]",
				graph, labels);
		
		SplitComputer comp = new SplitComputer(graph);
		Split split = comp.computeSplit("x", graph.getAllNodes());
		
		assert split.getAllDominators().equals(new HashSet(["x1","x2"]));
		assert isWccListEqual([["y"]], split.getWccs("x1"));
		assert isWccListEqual([["z"]], split.getWccs("x2"));
	}
	
	public void testNormalNonCompact() {
		// a normal, non-compact dominance graph 
		TestingTools.decodeDomcon("[label(x f(x1 x2)) label(x1 f(x3 x4)) dom(x3 y) label(y a) dom(x2 z) label(z a)]",
				graph, labels);
		
		SplitComputer comp = new SplitComputer(graph);
		Split split = comp.computeSplit("x", graph.getAllNodes());
		
		assert split.getAllDominators().equals(new HashSet(["x2","x3"]));
		assert isWccListEqual([["y"]], split.getWccs("x3"));
		assert isWccListEqual([["z"]], split.getWccs("x2"));
		assert split.getWccs("x1") == null;
		assert split.getWccs("x4") == null;
		
		assert [:].equals(split.getSubstitution());
	}
	
	public void testWeaklyNormalNonCompact() {
		// a weakly normal, non-compact dominance graph
		TestingTools.decodeDomcon("[label(x f(x1 x2)) label(x1 f(x3 x4)) dom(x1 y) label(y a) dom(x2 z) label(z a)]",
				graph, labels);
		
		SplitComputer comp = new SplitComputer(graph);
		Split split = comp.computeSplit("x", graph.getAllNodes());
		
		assert split.getAllDominators().equals(new HashSet(["x1","x2"]));
		assert isWccListEqual([["y"]], split.getWccs("x1"));
		assert isWccListEqual([["z"]], split.getWccs("x2"));
		assert split.getWccs("x3") == null;
		assert split.getWccs("x4") == null;
		
		assert [:].equals(split.getSubstitution());
	}
	
	public void testWeaklyNormalNonCompactDominatingEdges() {
		// a wn non-compact domgraph in which a hole and an ancestor of this hole
		// point into the same wcc
		TestingTools.decodeDomcon("[label(x f(x1 x2)) label(x1 f(x3 x4)) dom(x3 y) dom(x1 y) label(y a) dom(x2 z) label(z a)]",
				graph, labels);
		
		SplitComputer comp = new SplitComputer(graph);
		Split split = comp.computeSplit("x", graph.getAllNodes());
		
		assert split.getAllDominators().equals(new HashSet(["x2","x3"]));
		assert isWccListEqual([["y"]], split.getWccs("x3"));
		assert isWccListEqual([["z"]], split.getWccs("x2"));
		assert split.getWccs("x1") == null;
		assert split.getWccs("x4") == null;
		
		assert [:].equals(split.getSubstitution());
	}
	
	public void testWeaklyNormalNonCompactDisjointDominators() {
		// a wn non-cpt domgraph in which two different holes point into the same wcc
		// (i.e. fragment is not free)
		TestingTools.decodeDomcon("[label(x f(x1 x2)) label(x1 f(x3 x4)) dom(x3 y) dom(x4 y) label(y a) dom(x2 z) label(z a)]",
				graph, labels);
		
		SplitComputer comp = new SplitComputer(graph);
		Split split = comp.computeSplit("x", graph.getAllNodes());
		
		assert split == null;
	}
	
	public void testNonHnc() {
		// a small non-hnc graph
		TestingTools.decodeDomcon("[label(x f(x1)) dom(x1 y) dom(x1 z)]",
				graph, labels);
		
		SplitComputer comp = new SplitComputer(graph);
		Split split = comp.computeSplit("x", graph.getAllNodes());
		
		assert split != null;
		
		assert split.getAllDominators().equals(new HashSet(["x1"]));
		assert isWccListEqual([["y"],["z"]], split.getWccs("x1"));
		
		assert split.getSubstitution().isEmpty();
	}
	

	public void testCrossEdge() {
		TestingTools.decodeDomcon("[label(x f(x1 x2)) label(y g(y1 y2)) dom(y x2) dom(x z1) dom(y z2) dom(y2 z3)]",
				graph, labels);
		
		SplitComputer comp = new SplitComputer(graph);
		Split split = comp.computeSplit("x", graph.getAllNodes());
		
		assert split.getAllDominators().equals(new HashSet(["x","y","y2"]));
		assert isWccListEqual([["z1"]], split.getWccs("x"));
		assert isWccListEqual([["z2"]], split.getWccs("y"));
		assert isWccListEqual([["z3"]], split.getWccs("y2"));
		
		assert split.getSubstitution().equals(["x2":"y"]);
	}
	
	public void testDownwardCrossEdge() {
		TestingTools.decodeDomcon("[label(x f(x1 x2)) label(y g(y1 y2)) dom(y x2)]",
				graph, labels);
		
		SplitComputer comp = new SplitComputer(graph);
		Split split = comp.computeSplit("y", graph.getAllNodes());
		
		assert split.getAllDominators().equals(new HashSet(["y"]));
		assert isWccListEqual([["x","x1","x2"]], split.getWccs("y"));
		assert split.getSubstitution().isEmpty();
	}
	
	public void testCrossEdgeDominating() {
		TestingTools.decodeDomcon("[label(x f(x1 x2)) label(y g(y1 y2)) dom(y x2) label(z1 g(z3)) dom(x z1) dom(y z2) dom(y2 z3)]",
				graph, labels);
		
		SplitComputer comp = new SplitComputer(graph);
		Split split = comp.computeSplit("x", graph.getAllNodes());
		
		assert split.getAllDominators().equals(new HashSet(["y","y2"]));
		assert isWccListEqual([["z2"]], split.getWccs("y"));
		assert isWccListEqual([["z1","z3"]], split.getWccs("y2"));
		
		assert split.getSubstitution().equals(["x2":"y"]);
	}
	
	public void testCrossEdgeDisjoint() {
		TestingTools.decodeDomcon("[label(x f(x1 x2)) label(y g(y1 y2)) dom(y x2) label(z1 g(z3)) dom(x1 z1) dom(y z2) dom(y2 z3)]",
				graph, labels);
		
		SplitComputer comp = new SplitComputer(graph);
		Split split = comp.computeSplit("x", graph.getAllNodes());
		
		assert split == null;
	}
	
	public void testTwoCrossEdges() {
		// x2 has two incoming cross edges => unsolvable
		TestingTools.decodeDomcon("[label(x f(x1 x2)) label(y g(y1 y2)) label(z1 g(z3)) dom(y x2) dom(z1 x2)]",
				graph, labels);
		
		SplitComputer comp = new SplitComputer(graph);
		Split split = comp.computeSplit("x", graph.getAllNodes());
		
		assert split == null;
	}
	
	public void testNonTreeCrossEdges() {
		// y has two cross edges into disjoint nodes => superfragment not a tree, hence unsolvable
		TestingTools.decodeDomcon("[label(x f(x1 x2)) label(y g(y1 y2)) dom(y x1) dom(y x2)]",
				graph, labels);
		
		SplitComputer comp = new SplitComputer(graph);
		Split split = comp.computeSplit("x", graph.getAllNodes());
		
		assert split == null;
	}
	
	public void testIncomingNonCrossEdge() {
		// y has incoming dom edge which is not a cross edge => x is not free
		TestingTools.decodeDomcon("[label(x f(x1 x2)) label(y g(y1 y2)) dom(y x1) label(z h(z1)) dom(z1 y)]",
				graph, labels);
		
		SplitComputer comp = new SplitComputer(graph);
		Split split = comp.computeSplit("x", graph.getAllNodes());
		
		assert split == null;
	}
	
	public void testCrossEdgeFromNonHole() {
		// dom edge out of the middle of y's fragment into a hole of x's fragment
		// => x is not free because this dom edge is equivalent to the non-cross edge (y1,x)
		TestingTools.decodeDomcon("[label(x f(x1 x2)) label(y g(y1 y2)) label(y1 g(y3 y4)) dom(y1 x2)]",
				graph, labels);
		
		SplitComputer comp = new SplitComputer(graph);
		Split split = comp.computeSplit("x", graph.getAllNodes());
		
		assert split == null;
	}
	
	public void testCrossEdgeIntoNonRoot() {
		// dom edge from the root x into the non-hole y1
		// => x is not free because this dom edge is equivalent to the non-cross edge (x,y)
		TestingTools.decodeDomcon("[label(x f(x1 x2)) label(y g(y1 y2)) label(y1 g(y3 y4)) dom(x y1)]",
				graph, labels);
		
		SplitComputer comp = new SplitComputer(graph);
		Split split = comp.computeSplit("y", graph.getAllNodes());
		
		assert split == null;
	}
	
	public void testThreeFragments1() {
		// three interconnected upper fragments; x is free
		TestingTools.decodeDomcon("[label(x f(x1)) label(y g(y1)) label(z h(z1)) label(w a) dom(x y1) dom(y x1) dom(z y1) dom(y z1) dom(x1 w) dom(y1 w) dom(z1 w)]",
				graph, labels);
		
		SplitComputer comp = new SplitComputer(graph);
		Split split = comp.computeSplit("x", graph.getAllNodes());
		
		assert split != null;
		
		assert split.getAllDominators().equals(new HashSet(["z1"]));
		assert isWccListEqual([["w"]], split.getWccs("z1"));
		
		assert split.getSubstitution().equals(["x1":"y", "y1":"z"]);
	}
	
	public void testThreeFragments2() {
		// the same three interconnected upper fragments as in testThreeFragments1; y is not free
		TestingTools.decodeDomcon("[label(x f(x1)) label(y g(y1)) label(z h(z1)) label(w a) dom(x y1) dom(y x1) dom(z y1) dom(y z1) dom(x1 w) dom(y1 w) dom(z1 w)]",
				graph, labels);
		
		SplitComputer comp = new SplitComputer(graph);
		Split split = comp.computeSplit("y", graph.getAllNodes());
		
		assert split == null;
	}
	
	public void testDisjointCrossEdges() {
		// y and z are plugged into disjoint holes of x; thus cross edge from z to y1 can't be satisfied
		TestingTools.decodeDomcon("[label(x f(x1 x2)) label(y g(y1)) label(z h(z1)) dom(y x1) dom(z x2) dom(z y1)]",
				graph, labels);
		
		SplitComputer comp = new SplitComputer(graph);
		Split split = comp.computeSplit("x", graph.getAllNodes());
		
		assert split == null;
	}
	
	public void testChainWithCrossEdgeOk() {
		// chain of length two with cross edge; hole with incoming cross edge is the correct one
		// according to the chain
		TestingTools.decodeDomcon("[label(x f(x1 x2)) label(y g(y1 y2)) dom(x1 w) dom(y1 w) dom(y x1)]",
				graph, labels);
		
		SplitComputer comp = new SplitComputer(graph);
		Split split = comp.computeSplit("x", graph.getAllNodes());
		
		assert split != null;
		
		assert split.getAllDominators().equals(new HashSet(["y1"]));
		assert isWccListEqual([["w"]], split.getWccs("y1"));
		
		assert split.getSubstitution().equals(["x1":"y"]);
	}
	
	public void testChainWithCrossEdgeFail() {
		// chain of length two with cross edge; hole with incoming cross edge is the wrong one
		// according to the chain
		TestingTools.decodeDomcon("[label(x f(x1 x2)) label(y g(y1 y2)) dom(x1 w) dom(y1 w) dom(y x2)]",
				graph, labels);
		
		SplitComputer comp = new SplitComputer(graph);
		Split split = comp.computeSplit("x", graph.getAllNodes());
		
		assert split == null;
	}
	
	public void testRedundantDomEdge() {
		TestingTools.decodeDomcon("[label(x f(x1)) label(y g(y1)) dom(x1 w) dom(y1 w) dom(y x1)]",
				graph, labels);
		
		SplitComputer comp = new SplitComputer(graph);
		Split split = comp.computeSplit("y", graph.getAllNodes());
		
		assert split != null;
		
		assert split.getAllDominators().equals(new HashSet(["y1"])) : "dominators are " + split.getAllDominators();
		assert isWccListEqual([["w","x","x1"]], split.getWccs("y1"));
		
		assert split.getSubstitution().equals([:]);
	}
	
	// TODO cycles
}