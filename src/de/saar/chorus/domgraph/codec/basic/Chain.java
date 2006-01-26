/*
 * @(#)Chain.java created 26.01.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.codec.basic;

import java.io.IOException;
import java.io.Reader;

import de.saar.chorus.domgraph.codec.InputCodec;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.EdgeData;
import de.saar.chorus.domgraph.graph.EdgeType;
import de.saar.chorus.domgraph.graph.NodeData;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.domgraph.graph.NodeType;
import de.saar.chorus.domgraph.codec.MalformedDomgraphException;
import de.saar.chorus.domgraph.codec.ParserException;


public class Chain extends InputCodec {
    public void decode(Reader inputStream, DomGraph graph, NodeLabels labels) {
	throw new UnsupportedOperationException();
    }

    
    public void decode(String specification, DomGraph graph, NodeLabels labels) {
	int length = Integer.parseInt(specification);

	// TODO etc
    }

}
