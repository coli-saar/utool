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
		TestingTools.decodeDomcon("[label(x f(x1 x2)) dom(x1 y) label(y a) dom(x2 z) label(z a)]",
				graph, labels);
		
		SplitComputer comp = new SplitComputer(graph);
		Split split = comp.computeSplit("x", graph.getAllNodes());
		
		assert isWccListEqual([["y"]], split.getWccs("x1"));
		assert isWccListEqual([["z"]], split.getWccs("x2"));
	}
	
	public void testNormalNonCompact() {
		TestingTools.decodeDomcon("[label(x f(x1 x2)) label(x1 f(x3 x4)) dom(x3 y) label(y a) dom(x2 z) label(z a)]",
				graph, labels);
		
		SplitComputer comp = new SplitComputer(graph);
		Split split = comp.computeSplit("x", graph.getAllNodes());
		
		assert isWccListEqual([["y"]], split.getWccs("x3"));
		assert isWccListEqual([["z"]], split.getWccs("x2"));
		assert split.getWccs("x1") == null;
		assert split.getWccs("x4") == null;
	}
	
	public void testWeaklyNormalNonCompact() {
		TestingTools.decodeDomcon("[label(x f(x1 x2)) label(x1 f(x3 x4)) dom(x1 y) label(y a) dom(x2 z) label(z a)]",
				graph, labels);
		
		SplitComputer comp = new SplitComputer(graph);
		Split split = comp.computeSplit("x", graph.getAllNodes());
		
		assert isWccListEqual([["y"]], split.getWccs("x1"));
		assert isWccListEqual([["z"]], split.getWccs("x2"));
		assert split.getWccs("x3") == null;
		assert split.getWccs("x4") == null;
	}
	
	public void testWeaklyNormalNonCompactDominatingEdges() {
		TestingTools.decodeDomcon("[label(x f(x1 x2)) label(x1 f(x3 x4)) dom(x3 y) dom(x1 y) label(y a) dom(x2 z) label(z a)]",
				graph, labels);
		
		SplitComputer comp = new SplitComputer(graph);
		Split split = comp.computeSplit("x", graph.getAllNodes());
		
		assert isWccListEqual([["y"]], split.getWccs("x3"));
		assert isWccListEqual([["z"]], split.getWccs("x2"));
		assert split.getWccs("x1") == null;
		assert split.getWccs("x4") == null;
	}
	
	public void testWeaklyNormalNonCompactDisjointDominators() {
		TestingTools.decodeDomcon("[label(x f(x1 x2)) label(x1 f(x3 x4)) dom(x3 y) dom(x4 y) label(y a) dom(x2 z) label(z a)]",
				graph, labels);
		
		SplitComputer comp = new SplitComputer(graph);
		Split split = comp.computeSplit("x", graph.getAllNodes());
		
		assert split == null;
	}

	

}