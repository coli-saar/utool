/*
 * @(#)GraphOutputCodec.java created 25.01.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.codec;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;

import de.saar.chorus.domgraph.graph.DomEdge;
import de.saar.chorus.domgraph.graph.DomGraph;

abstract public class GraphOutputCodec extends OutputCodec {
    protected GraphOutputCodec() {
        type = Type.GRAPH;
    }
    
    public void encode_plugging(DomGraph graph, Collection<DomEdge> domedges,
            Writer writer) throws IOException, MalformedDomgraphException {
        throw new MalformedDomgraphException();
    }
}
