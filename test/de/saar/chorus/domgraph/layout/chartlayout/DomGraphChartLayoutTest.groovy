package de.saar.chorus.domgraph.layout.chartlayout;


import de.saar.chorus.domgraph.codec.*;
import de.saar.chorus.domgraph.codec.domcon.*;
import de.saar.chorus.domgraph.chart.*;
import de.saar.chorus.domgraph.graph.*;

import de.saar.chorus.domgraph.layout.*;

import java.util.*;

import de.saar.testingtools.*;


class DomGraphChartLayoutTest extends GroovyTestCase {
	private DomGraph graph;
    private NodeLabels labels;
    
    private Canvas canvas;
    private DomGraphChartLayout layout;
    private LayoutOptions options;
    
    public void setUp() {
    	graph = new DomGraph();
    	labels = new NodeLabels();
    	canvas = new DummyCanvas();
    	layout = new DomGraphChartLayout();
    	options = new LayoutOptions(LayoutOptions.LabelType.BOTH, true);
    }
    
	public void testSolvedFormGraph() {
		checkLayoutReturns("[label(x f(x1)) dom(x1 y) label(y a)]");
	}
	
	public void testNormalGraph() {
		checkLayoutReturns("[label(x f(x1)) label(y g(y1)) dom(x1 z) dom(y1 z) label(z a)]");
	}
	
	public void testUnsolvableGraph() {
		TestingTools.expectException(LayoutException,
			{ checkLayoutReturns("[label(x f(x))]"); }
		);
	}
	
	public void testEmptyFragmentsGraph() {
		TestingTools.expectException(LayoutException,
				{ checkLayoutReturns("[label(x f(x1)) dom(x1 y) dom(y z) label(z a)]"); }
		);
	}
	

	
	
	////////////////////////////////////////////////////////////////////////////
	
	private void checkLayoutReturns(String domcon) {
		TestingTools.decodeDomcon(domcon, graph, labels);
		layout.layout(graph, labels, canvas, options);
		
		assert true;
	}

}
