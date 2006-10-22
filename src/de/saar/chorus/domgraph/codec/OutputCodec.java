/*
 * @(#)OutputCodec.java created 25.01.2006
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
 * The abstract base class for all output codecs. Derived classes 
 * implement the method <code>encode</code>, which is responsible
 * for writing an encoding of the labelled dominance graph to
 * a writer.
 * 
 * @author Alexander Koller
 *
 */

public abstract class OutputCodec {
    /**
     * Encodes a dominance graph into a string representation for
     * this output codec. The dominance graph is defined by the
     * arguments <code>graph</code> and <code>labels</code>. 
     * The USR is written to the <code>writer</code>.<p>
     * 
     * @param graph the dominance graph
     * @param labels the node labels for this dominance graph
     * @param writer the writer to which the encoded USR will be written
     * @throws IOException if an I/O error occurred while writing to the 
     * <code>writer</code>
     * @throws MalformedDomgraphException if the graph cannot 
     * be encoded by this codec
     */
    public abstract void encode(DomGraph graph, NodeLabels labels, Writer writer) 
    throws IOException, MalformedDomgraphException;


    
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
    

    
    /****************
     * UNIT TESTS:
     */
}
