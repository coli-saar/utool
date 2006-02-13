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


/**
 * The abstract base class for graph output codecs. The constructor of this
 * class sets the codec type to GRAPH, and <code>encode_plugging</code>
 * is implemented with a dummy implementation.
 * 
 * @author Alexander Koller
 *
 */
abstract public class GraphOutputCodec extends OutputCodec {
    protected GraphOutputCodec() {
        type = Type.GRAPH;
    }
    
    /**
     * A dummy implementation which throws an <code>UnsupportedOperationException</code>.
     */
    public void encode_plugging(DomGraph graph, Collection<DomEdge> domedges,
            Writer writer) throws IOException, MalformedDomgraphException {
        throw new UnsupportedOperationException("Graph output codec doesn't support output of pluggings");
    }
}
