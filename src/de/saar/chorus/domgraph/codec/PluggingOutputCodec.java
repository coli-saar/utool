/*
 * @(#)PluggingOutputCodec.java created 25.01.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.codec;

import java.io.IOException;
import java.io.Writer;

import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;

/**
 * The abstract base class for plugging output codecs. The constructor of this
 * class sets the codec type to PLUGGING, and <code>encode_graph</code>
 * is implemented with a dummy implementation.
 * 
 * @author Alexander Koller
 *
 */
public abstract class PluggingOutputCodec extends OutputCodec {
    protected PluggingOutputCodec() {
        type = Type.PLUGGING;
    }

    /**
     * A dummy implementation which throws an <code>UnsupportedOperationException</code>.
     */
    public void encode_graph(DomGraph graph, NodeLabels labels, Writer writer)
            throws IOException, MalformedDomgraphException {
        throw new UnsupportedOperationException("Plugging output codec doesn't support output of graphs");
    }
}
