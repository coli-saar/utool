package de.saar.chorus.layout.chartlayout;

import de.saar.testingtools.*;
import java.util.*;
import de.saar.chorus.domgraph.graph.*;
import de.saar.chorus.domgraph.chart.*;
import de.saar.chorus.ubench.*;
import de.saar.chorus.layout.chartlayout.*;
import de.saar.chorus.layout.treelayout.*;
import de.saar.chorus.jgraph.*;

import org.jgraph.util.JGraphUtilities;
import org.jgraph.graph.*;


class DomGraphChartLayoutTest extends GroovyTestCase {
	
	private Chart chart = new Chart();
	private DomGraph graph = new DomGraph();
	private NodeLabels labels = new NodeLabels();
	private JDomGraph jdomgraph = null;
	
	
	
	public void testCompact() {
		// a compact dominance graph (this should be easy)
		checkGraphLayout(  "[label(x f(x1 x2)) dom(x1 y) label(y a) dom(x2 z) label(z a)]");
	}
	
	public void testNormalNonCompact() {
		// a normal, non-compact dominance graph
		checkGraphLayout(  "[label(x f(x1 x2)) label(x1 f(x3 x4)) dom(x3 y) label(y a) dom(x2 z) label(z a)]");
	}
	
	public void testWeaklyNormalNonCompact() {
		// a weakly normal, non-compact dominance graph
		checkGraphLayout(  "[label(x f(x1 x2)) label(x1 f(x3 x4)) dom(x1 y) label(y a) dom(x2 z) label(z a)]");
	}
	
	public void testWeaklyNormalNonCompactDominatingEdges() {
		// a wn non-compact domgraph in which a hole and an ancestor of this hole
		// point into the same wcc
		checkGraphLayout(  "[label(x f(x1 x2)) label(x1 f(x3 x4)) dom(x3 y) dom(x1 y) label(y a) dom(x2 z) label(z a)]");
	}
	
	public void testWeaklyNormalNonCompactDisjointDominators() {
		// a wn non-cpt domgraph in which two different holes point into the same wcc
		// (i.e. fragment is not free)
		checkGraphLayout(  "[label(x f(x1 x2)) label(x1 f(x3 x4)) dom(x3 y) dom(x4 y) label(y a) dom(x2 z) label(z a)]");
	}
	
	public void testNonHnc() {
		// a small non-hnc graph
		checkGraphLayout(  "[label(x f(x1)) dom(x1 y) dom(x1 z)]");
		
	}
	
	
	public void testDownwardCrossEdge() {
		checkGraphLayout(  "[label(x f(x1 x2)) label(y g(y1 y2)) dom(y x2)]");
	}
	
	public void testCrossEdgeDominating() {
		checkGraphLayout(  "[label(x f(x1 x2)) label(y g(y1 y2)) dom(y x2) label(z1 g(z3)) dom(x z1) dom(y z2) dom(y2 z3)]");
	}
	
	public void testCrossEdgeDisjoint() {
		checkGraphLayout(  "[label(x f(x1 x2)) label(y g(y1 y2)) dom(y x2) label(z1 g(z3)) dom(x1 z1) dom(y z2) dom(y2 z3)]");
	}
	
	public void testTwoCrossEdges() {
		// x2 has two incoming cross edges => unsolvable
		checkGraphLayout(  "[label(x f(x1 x2)) label(y g(y1 y2)) label(z1 g(z3)) dom(y x2) dom(z1 x2)]");
	}
	
	public void testNonTreeCrossEdges() {
		// y has two cross edges into disjoint nodes => superfragment not a tree, hence unsolvable
		checkGraphLayout(  "[label(x f(x1 x2)) label(y g(y1 y2)) dom(y x1) dom(y x2)]");
	}
	
	public void testIncomingNonCrossEdge() {
		// y has incoming dom edge which is not a cross edge => x is not free
		checkGraphLayout(  "[label(x f(x1 x2)) label(y g(y1 y2)) dom(y x1) label(z h(z1)) dom(z1 y)]");
	}
	
	public void testCrossEdgeFromNonHole() {
		// dom edge out of the middle of y's fragment into a hole of x's fragment
		// => x is not free because this dom edge is equivalent to the non-cross edge (y1,x)
		checkGraphLayout(  "[label(x f(x1 x2)) label(y g(y1 y2)) label(y1 g(y3 y4)) dom(y1 x2)]");
	}
	
	public void testCrossEdgeIntoNonRoot() {
		// dom edge from the root x into the non-hole y1
		// => x is not free because this dom edge is equivalent to the non-cross edge (x,y)
		checkGraphLayout(  "[label(x f(x1 x2)) label(y g(y1 y2)) label(y1 g(y3 y4)) dom(x y1)]");
	}
	
	public void testThreeFragments1() {
		// three interconnected upper fragments; x is free
		checkGraphLayout(  "[label(x f(x1)) label(y g(y1)) label(z h(z1)) label(w a) dom(x y1) dom(y x1) dom(z y1) dom(y z1) dom(x1 w) dom(y1 w) dom(z1 w)]");
	}
	
	public void testThreeFragments2() {
		// the same three interconnected upper fragments as in testThreeFragments1; y is not free
		checkGraphLayout(  "[label(x f(x1)) label(y g(y1)) label(z h(z1)) label(w a) dom(x y1) dom(y x1) dom(z y1) dom(y z1) dom(x1 w) dom(y1 w) dom(z1 w)]");
		
	}
	
	public void testDisjointCrossEdges() {
		// y and z are plugged into disjoint holes of x; thus cross edge from z to y1 can't be satisfied
		checkGraphLayout(  "[label(x f(x1 x2)) label(y g(y1)) label(z h(z1)) dom(y x1) dom(z x2) dom(z y1)]");
	}
	
	public void testChainWithCrossEdgeOk() {
		// chain of length two with cross edge; hole with incoming cross edge is the correct one
		// according to the chain
		checkGraphLayout("[label(x f(x1 x2)) label(y g(y1 y2)) dom(x1 w) dom(y1 w) dom(y x1)]");
	}
	
	public void testChainWithCrossEdgeFail() {
		// chain of length two with cross edge; hole with incoming cross edge is the wrong one
		// according to the chain
		checkGraphLayout( "[label(x f(x1 x2)) label(y g(y1 y2)) dom(x1 w) dom(y1 w) dom(y x2)]");
	}
	
	public void testRedundantDomEdge() {
		checkGraphLayout(  "[label(x f(x1)) label(y g(y1)) dom(x1 w) dom(y1 w) dom(y x1)]");
	}
	
	public void testSmall() {
		checkGraphLayout("[label(x f(x1)) dom(x1 y) label(y a)]");
	}
	
	public void testCrossEdge() {
		checkGraphLayout("[label(x f(x1)) label(y g(y1)) label(z a) dom(x1 z) dom(y1 z) dom(y x1)]");
	}

	public void testThreeUpperFragments() {

		checkGraphLayout("[label(x f(x1)) label(y g(y1)) label(z h(z1)) label(w a) dom(x y1)" +
        "dom(y x1) dom(z y1) dom(y z1) dom(x1 w) dom(y1 w) dom(z1 w)]");
	}
	
	
	
	public void testChainWithCrossEdge1() {
		// chain with cross edge into connecting hole => two solved forms
		checkGraphLayout("[label(x f(x1 x2)) label(y g(y1 y2)) dom(x2 z) dom(y1 z) dom(y x2)]");
	}
	
	public void testChainWithCrossEdge2() {
		// chain with cross edge into non-connecting hole => unsolvable
		checkGraphLayout("[label(x f(x1 x2)) label(y g(y1 y2)) dom(x2 z) dom(y1 z) dom(y x1)]");
	}
	
	
	private void checkGraphLayout(String domcon) {
		TestingTools.decodeDomcon(domcon, graph, labels);
		DomGraphTConverter conv = new DomGraphTConverter(graph, labels);
		jdomgraph = conv.getJDomGraph();
		jdomgraph.computeFragments();
		
		checkAllFragments(jdomgraph)
		
		ChartSolver.solve(graph, chart);
		try { 
			JGraphUtilities.applyLayout(jdomgraph, new DomGraphChartLayout(jdomgraph, chart, graph));
			assert true;
		} catch (Exception e) {
			assert false : graph.toString();
		}
	}
	
	private void checkAllFragments(JDomGraph dg) {
		
		for(Fragment frag : dg.getFragments()) {
			checkFragment(frag, frag.getRoot());
		}
	}
	
	private boolean checkFragment(Fragment frag, DefaultGraphCell root) {

		
		DomGraphChartLayout layout = new DomGraphChartLayout(jdomgraph, chart, graph);
		
		// computing the x-positions, dependent on the _direct_
		// parent

		try {
			GraphLayoutCursor layCursor = new GraphLayoutCursor(root, layout, jdomgraph, frag.getNodes());
			PostOrderNodeVisitor postVisitor = new PostOrderNodeVisitor(layCursor);
			postVisitor.run();
			assert true;
		} catch (Exception e) {
			assert false: "[Post-Order] fragment: " + frag + " root: " + frag + e.getMessage();
		}
		
		try{
			// another DFS computes the y- and x-positions relativ to the
			// _root_
			GraphDrawingCursor drawCursor = new GraphDrawingCursor(root, layout, jdomgraph, frag.getNodes());
			PreOrderNodeVisitor preVisitor = new PreOrderNodeVisitor(drawCursor);
			preVisitor.run();
			assert true;
		} catch (Exception e) {
			assert false: "[Pre-Order] fragment: " + frag + " root: " + frag + e.getMessage();
		}
		
	}

}