/*
 * @(#)Chain.java created 26.01.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.codec.basic;

import java.io.Reader;

import de.saar.chorus.domgraph.codec.InputCodec;
import de.saar.chorus.domgraph.codec.MalformedDomgraphException;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.EdgeData;
import de.saar.chorus.domgraph.graph.EdgeType;
import de.saar.chorus.domgraph.graph.NodeData;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.domgraph.graph.NodeType;


public class Chain extends InputCodec {
    public void decode(Reader inputStream, DomGraph graph, NodeLabels labels) {
	throw new UnsupportedOperationException();
    }

    
    public void decode(String specification, DomGraph graph, NodeLabels labels)
    throws MalformedDomgraphException {
    	String upper_root, upper_lefthole, upper_righthole;
    	String lower;
    	int length = Integer.parseInt(specification);

    	if( length < 1 ) {
    		throw new MalformedDomgraphException();
    	}
    	
    	
    	graph.clear();
    	
    	lower = "y0";
    	graph.addNode("y0", new NodeData(NodeType.LABELLED, "y0"));
    	labels.addLabel("y0", "a0");
    	
    	for( int i = 1; i <= length; i++ ) {
    		// upper fragment
    		upper_root = "x" + i;
    		upper_lefthole = "xl" + i;
    		upper_righthole = "xr" + i;
    		
    		graph.addNode(upper_root, new NodeData(NodeType.LABELLED, upper_root));
    		labels.addLabel(upper_root, "f" + i);
    		
    		graph.addNode(upper_lefthole, new NodeData(NodeType.UNLABELLED, upper_lefthole));
    		graph.addNode(upper_righthole, new NodeData(NodeType.UNLABELLED, upper_righthole));
    		
    		graph.addEdge(upper_root, upper_lefthole, new EdgeData(EdgeType.TREE, "x-xl-" + i));
    		graph.addEdge(upper_root, upper_righthole, new EdgeData(EdgeType.TREE, "x-xr-" + i));
    		
    		// dominance edge to previous lower fragment
    		graph.addEdge(upper_lefthole, lower, new EdgeData(EdgeType.DOMINANCE, "dom"));
    		
    		// lower fragment
    		lower = "y" + i;
    		graph.addNode(lower, new NodeData(NodeType.LABELLED, lower));
        	labels.addLabel(lower, "a" + i);
    		
    		// dominance edge to new lower fragment
        	graph.addEdge(upper_righthole, lower, new EdgeData(EdgeType.DOMINANCE, "dom"));
    	}
    	
    }

}
