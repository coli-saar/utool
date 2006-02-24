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


/**
 * An input codec that generates pure chains. Pure chains
 * are normal dominance graphs in which upper and lower fragments
 * alternate in a zig-zag shape (see e.g. <a href="http://www.coli.uni-saarland.de/~koller/showpaper.php?id=thesis">
 * Koller's Ph.D. thesis</a>, Chapter 6.4). They correspond to
 * the predicate-argument structure of (possibly nested) NPs
 * and (possibly transitive) verbs, and as such are a convenient
 * type of graphs for benchmarks.<p>
 * 
 * The {@link Chain#decodeFile(String, DomGraph, NodeLabels) decodeFile} and 
 * {@link Chain#decodeString(String, DomGraph, NodeLabels) decodeString} methods of this
 * input codec are both implemented in such a way that they parse
 * their string argument as a number, and then generate the pure
 * chain of that length. This differs from the specification of
 * an ordinary input codec, which interprets these methods as
 * instructions to read an USR from a file or string. The 
 * {@link Chain#decode(Reader, DomGraph, NodeLabels) decode}
 * method for reading a USR from a reader is not supported by this
 * codec.
 * 
 * @author Alexander Koller
 *
 */
public class Chain extends InputCodec {
    public static String getName() {
        return "chain";
    }
    
    public static String getExtension() {
        return null;
    }
    
    
    public void decode(Reader inputStream, DomGraph graph, NodeLabels labels) {
	throw new UnsupportedOperationException();
    }

    
    public void decodeFile(String lengthAsString, DomGraph graph, NodeLabels labels)
    throws MalformedDomgraphException {
        makeChain(lengthAsString, graph, labels);
    }

    public void decodeString(String lengthAsString, DomGraph graph, NodeLabels labels)
    throws MalformedDomgraphException {
        makeChain(lengthAsString, graph, labels);
    }

    private void makeChain(String lengthAsString, DomGraph graph, NodeLabels labels)
    throws MalformedDomgraphException {
    	String upper_root, upper_lefthole, upper_righthole;
    	String lower;
    	int length;
        
        try {
            length = Integer.parseInt(lengthAsString);
        } catch(NumberFormatException e) {
            throw new MalformedDomgraphException(e);
        }

    	if( length < 1 ) {
    		throw new MalformedDomgraphException("You must specify a numeric chain length of at least 1!");
    	}
    	
    	
    	graph.clear();
    	
    	lower = "y0";
    	graph.addNode("y0", new NodeData(NodeType.LABELLED));
    	labels.addLabel("y0", "a0");
    	
    	for( int i = 1; i <= length; i++ ) {
    		// upper fragment
    		upper_root = "x" + i;
    		upper_lefthole = "xl" + i;
    		upper_righthole = "xr" + i;
    		
    		graph.addNode(upper_root, new NodeData(NodeType.LABELLED));
    		labels.addLabel(upper_root, "f" + i);
    		
    		graph.addNode(upper_lefthole, new NodeData(NodeType.UNLABELLED));
    		graph.addNode(upper_righthole, new NodeData(NodeType.UNLABELLED));
    		
    		graph.addEdge(upper_root, upper_lefthole, new EdgeData(EdgeType.TREE));
    		graph.addEdge(upper_root, upper_righthole, new EdgeData(EdgeType.TREE));
    		
    		// dominance edge to previous lower fragment
    		graph.addEdge(upper_lefthole, lower, new EdgeData(EdgeType.DOMINANCE));
    		
    		// lower fragment
    		lower = "y" + i;
    		graph.addNode(lower, new NodeData(NodeType.LABELLED));
        	labels.addLabel(lower, "a" + i);
    		
    		// dominance edge to new lower fragment
        	graph.addEdge(upper_righthole, lower, new EdgeData(EdgeType.DOMINANCE));
    	}
    	
    }

}
