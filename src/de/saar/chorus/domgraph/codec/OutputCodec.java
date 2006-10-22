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


/**
 * The abstract base class for all output codecs. Derived classes should
 * implement either the method <code>encode_graph</code> or the method
 * <code>encode_plugging</code>, depending on whether the codec is a graph
 * or a plugging output codec.<p>
 * 
 * There are two types of output codecs. Graph output codecs compute a
 * representation of a complete graph, using the node labels and the
 * structure of the graph. On the other hand, plugging output codecs
 * only encode the dominance edges of a solved form.
 * 
 * @author Alexander Koller
 *
 */

public abstract class OutputCodec {
    /**
     * The two types of output codecs.
     *
     */
    public static enum Type {
        GRAPH, PLUGGING
    }
    
    public static final int ERROR_NULL_PLUGGING = 30;
    
    /**
     * Encodes a dominance graph into a string representation for
     * this output codec. The dominance graph is defined by the
     * arguments <code>graph</code> and <code>labels</code>. If 
     * the codec is a graph codec and a non-<code>null</code> 
     * value is passed in the <code>domedges</code> argument,
     * then the dominance edges in the graph are replaced by
     * the given dominance edges before the graph is output.
     * If the codec is a plugging codec, then the codec simply
     * outputs the dominance edges; passing <code>null</code>
     * as the <code>domedges</code> argument is illegal in
     * this case.<p>
     * 
     * The USR is written to the <code>writer</code>.<p>
     * 
     * @param graph the dominance graph
     * @param domedges a collection of <code>DomEdge</code> objects
     * @param labels the node labels for this dominance graph
     * @param writer the writer to which the encoded USR will be written
     * @throws IOException if an I/O error occurred while writing to the 
     * <code>writer</code>
     * @throws MalformedDomgraphException if the graph or plugging cannot 
     * be encoded by this codec
     * @throws NullPointerException if the <code>domedge</code>
     * argument is <code>null</code> and the codec is a plugging codec.
     */
    public abstract void encode(DomGraph graph, NodeLabels labels, Writer writer) 
    throws IOException, MalformedDomgraphException;
        
/*        
        switch(getType()) {
        case GRAPH:
            DomGraph copy = (DomGraph) graph.clone();
            
            if( domedges != null ) {
                copy.setDominanceEdges(domedges);
            }
            
            encode_graph(copy, labels, writer);
            break;
            
        case PLUGGING:
            if( domedges == null ) { 
                throw new NullPointerException();
            }
            
            encode_plugging(graph, domedges, writer);
            break;
        }
        
    }
  */  
    
    /** abstract printing methods -- implement these **/
    
    /**
     * Implement this method for a graph output codec. This method
     * gets a labelled dominance graph and should write its USR
     * representation to the <code>writer</code>.  
     * 
     * @param graph a dominance graph
     * @param labels the node labels
     * @param writer the writer to which the USR should be written
     * @throws IOException if an I/O error occurred while writing to the 
     * <code>writer</code>
     * @throws MalformedDomgraphException if the graph cannot 
     * be encoded by this codec
     */
    //abstract public void encode_graph(DomGraph graph, NodeLabels labels, Writer writer)
    //throws IOException, MalformedDomgraphException;
    
    /**
     * Implement this method for a plugging output codec. This method
     * gets a dominance graph and a list of dominance edges, and should
     * write these dominance edges to the <code>writer</code>.  
     * 
     * @param graph a dominance graph
     * @param domedges a collection of dominance edges 
     * @param writer the writer to which the plugging should be written
     * @throws IOException if an I/O error occurred while writing to the 
     * <code>writer</code>
     * @throws MalformedDomgraphException if the plugging cannot 
     * be encoded by this codec
     */
    //abstract public void encode_plugging(DomGraph graph, Collection<DomEdge> domedges, Writer writer)
    //throws IOException, MalformedDomgraphException;
    
    /**
     * Prints a header at the beginning of a file to which the USR
     * is written. 
     * 
     * @param writer the writer
     * @throws IOException if an I/O error occurred
     */
    abstract public void print_header(Writer writer)
    throws IOException;
    
    /**
     * Prints a footer at the end of a file to which the USR
     * is written. 
     * 
     * @param writer the writer
     * @throws IOException if an I/O error occurred
     */
    abstract public void print_footer(Writer writer)
    throws IOException;
    
    /**
     * Prints the beginning of a list in the concrete syntax
     * which the USR uses. This method is called after <code>print_header</code>,
     * but before any of the USRs. It is only called if we print 
     * more than one graph (e.g. in the solve command, but not the convert command).
     * 
     * @param writer the writer
     * @throws IOException if an I/O error occurred
     */
    abstract public void print_start_list(Writer writer)
    throws IOException;
    
    /**
     * Prints the end of a list in the concrete syntax
     * which the USR uses. This method is called before <code>print_footer</code>,
     * but after any of the USRs. It is only called if we print 
     * more than one graph (e.g. in the solve command, but not the convert command).
     * 
     * @param writer the writer
     * @throws IOException if an I/O error occurred
     */
    abstract public void print_end_list(Writer writer)
    throws IOException;
    
    /**
     * Prints the separator for separating different items
     * of a list in the concrete syntax which the USR uses.
     * 
     * @param writer the writer
     * @throws IOException if an I/O error occurred
     */
    abstract public void print_list_separator(Writer writer)
    throws IOException;
    
    
    /** administrative data **/
    protected static Type type;
    
    /**
     * Gets the type of the output codec.
     * 
     * @return the type
     */
    public static Type getType() {
        return type;
    }

    
    /****************
     * UNIT TESTS:
     *  - null domedges argument for encode()
     */
}
