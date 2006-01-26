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

public abstract class PluggingOutputCodec extends OutputCodec {
    protected PluggingOutputCodec() {
        type = Type.PLUGGING;
    }

    public void encode_graph(DomGraph graph, NodeLabels labels, Writer writer)
            throws IOException, MalformedDomgraphException {
        throw new MalformedDomgraphException();
    }
}
