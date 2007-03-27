package de.saar.chorus.domgraph.chart;

import de.saar.chorus.domgraph.codec.*;
import de.saar.chorus.domgraph.codec.basic.*;
import de.saar.chorus.domgraph.graph.*;

class SplitComputerTest extends GroovyTestCase {
	private Chart chart;
	
	public void setUp() {
		chart = new Chart();
	}

	
	
	public void testChain3() {
		InputCodec codec = new Chain();
		DomGraph graph = new DomGraph();
		NodeLabels labels = new NodeLabels();
		
		codec.decode(codec.getReaderForSpecification("3"), graph, labels);
		
		assert ChartSolver.solve(graph, chart) == true;
	}

}