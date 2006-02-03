/*
 * @(#)DomconUdrawOutputCodec.java created 03.02.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.codec.domcon;

import java.io.IOException;
import java.io.Writer;

import de.saar.chorus.domgraph.codec.GraphOutputCodec;
import de.saar.chorus.domgraph.codec.MalformedDomgraphException;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;



/*
 * TODO: Implement this class.
 * 
 * It is currently just a placeholder because I don't entirely
 * understand the old output codec.
 * 
 * see davinci-codec.cc in utool
 */

public class DomconUdrawOutputCodec extends GraphOutputCodec {
    public DomconUdrawOutputCodec() {
        super();
        
        setName("domcon-udraw");
        setExtension(".dc.udg");
    }

    @Override
    public void encode_graph(DomGraph graph, NodeLabels labels, Writer writer)
            throws IOException, MalformedDomgraphException {
        
        throw new UnsupportedOperationException();

    }

    @Override
    public void print_header(Writer writer) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void print_footer(Writer writer) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void print_start_list(Writer writer) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void print_end_list(Writer writer) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void print_list_separator(Writer writer) throws IOException {
        // TODO Auto-generated method stub

    }

}
