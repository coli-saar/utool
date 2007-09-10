package de.saar.chorus.domgraph.layout.domgraphlayout;


import de.saar.chorus.domgraph.codec.*;
import de.saar.chorus.domgraph.codec.domcon.*;
import de.saar.chorus.domgraph.chart.*;
import de.saar.chorus.domgraph.graph.*;

import de.saar.chorus.domgraph.layout.*;

import java.util.*;

import de.saar.testingtools.*;


class DomGraphLayoutTest extends GroovyTestCase {
	private DomGraph graph;
    private NodeLabels labels;
    
    private Canvas canvas;
    private DomGraphLayout layout;
    private LayoutOptions options;
    
    public void setUp() {
    	graph = new DomGraph();
    	labels = new NodeLabels();
    	canvas = new DummyCanvas();
    	layout = new DomGraphLayout();
    	options = new LayoutOptions(LayoutOptions.LabelType.BOTH, true);
    }
    
    public void testDummy() {
    	assert true;
    }

}
