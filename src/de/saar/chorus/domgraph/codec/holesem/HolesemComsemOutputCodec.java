/*
 * @(#)HolesemComsemOutputCodec.java created 03.02.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.codec.holesem;

import java.io.IOException;
import java.io.Writer;

import de.saar.chorus.domgraph.codec.GraphOutputCodec;
import de.saar.chorus.domgraph.codec.MalformedDomgraphException;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;


/*
 * TODO: Implement this class.
 * It is currently just a placeholder because the implementation
 * of the holesem output codec is so ugly.
 * 
 * see holesem-comsem-parser.yy in utool
 */

public class HolesemComsemOutputCodec extends GraphOutputCodec {
    public static String getName() {
        return "holesem-comsem";
    }
    
    public static String getExtension() {
        return ".hs.pl";
    }

    
    
    public void encode_graph(DomGraph graph, NodeLabels labels, Writer writer)
            throws IOException, MalformedDomgraphException {
        
        if( !graph.isNormal() || !graph.isHypernormallyConnected() ) {
            throw new MalformedDomgraphException();
        }
        
        throw new UnsupportedOperationException();
        // TODO Auto-generated method stub

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
