/*
 * @(#)OutputCodec.java created 25.01.2006
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
import de.saar.chorus.domgraph.graph.NodeLabels;

public abstract class OutputCodec {
    static enum Type {
        GRAPH, PLUGGING
    }
    
    
    public void encode(DomGraph graph, Collection<DomEdge> domedges,
                        NodeLabels labels, Writer writer)
    throws IOException, MalformedDomgraphException {
        switch(getType()) {
        case GRAPH:
            if( domedges != null ) {
                graph.setDominanceEdges(domedges);
            }
            
            encode_graph(graph, labels, writer);
            break;
            
        case PLUGGING:
            encode_plugging(graph, domedges, writer);
            break;
        }
        
    }
    
    
    /** abstract printing methods -- implement these **/
    
    abstract public void encode_graph(DomGraph graph, NodeLabels labels, Writer writer)
    throws IOException, MalformedDomgraphException;
    
    abstract public void encode_plugging(DomGraph graph, Collection<DomEdge> domedges, Writer writer)
    throws IOException, MalformedDomgraphException;
    
    abstract public void print_header(Writer writer)
    throws IOException;
    
    abstract public void print_footer(Writer writer)
    throws IOException;
    
    abstract public void print_start_list(Writer writer)
    throws IOException;
    
    abstract public void print_end_list(Writer writer)
    throws IOException;
    
    abstract public void print_list_separator(Writer writer)
    throws IOException;
    
    
    /** administrative data **/
    protected Type type;
    protected String name, extension;
    
    public Type getType() {
        return type;
    }

    protected void setName(String name) {
        this.name = name;
    }
    
    protected void setExtension(String extension) {
        this.extension = extension;
    }
    

    public String getName() {
        return name;
    }
    
    public String getExtension() {
        return extension;
    }
    

}
