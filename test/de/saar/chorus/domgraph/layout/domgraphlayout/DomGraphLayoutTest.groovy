package de.saar.chorus.domgraph.layout.domgraphlayout;


import de.saar.chorus.domgraph.codec.*;
import de.saar.chorus.domgraph.codec.domcon.*;
import de.saar.chorus.domgraph.chart.*;
import de.saar.chorus.domgraph.graph.*;

import de.saar.chorus.domgraph.layout.*;

import java.util.*;

import de.saar.testingtools.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

class DomGraphLayoutTest  {
	private DomGraph graph;
    private NodeLabels labels;
    
    private Canvas canvas;
    private DomGraphLayout layout;
    private LayoutOptions options;
    
    @Before
    public void setUp() {
    	graph = new DomGraph();
    	labels = new NodeLabels();
    	canvas = new DummyCanvas();
    	layout = new DomGraphLayout();
    	options = new LayoutOptions(LayoutOptions.LabelType.BOTH, true);
    }
    
    @Test
    public void testDummy() {
    	assert true;
    }

}
