/*
 * @(#)InputCodec.java created 25.01.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.codec;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;

/**
 * The abstract base class for all input codecs. Derived classes should
 * implement the method <code>decode</code> and set the codec's name and
 * extension in the constructor.
 * 
 * @author Alexander Koller
 *
 */
public abstract class InputCodec {
    /**
     * Reads an USR representation from a <code>Reader</code>. This method
     * converts the USR into an equivalent labelled dominance graph and
     * stores this graph in a <code>DomGraph</code> and the labels in
     * a <code>NodeLabels</code> object. You must implement this
     * method in every concrete input codec.<p>
     * 
     * The graph and labels objects passed to this method need not be
     * empty; it is the responsibility of this method to clear them first.
     * 
     * @param reader the reader from which the USR is read
     * @param graph the dominance graph into which the USR is converted
     * @param labels the node labels of the labelled dominance graph
     * @throws IOException if an I/O error occurred while reading from <code>reader</code>
     * @throws ParserException if a syntactic error occurred while parsing the USR
     * @throws MalformedDomgraphException if a semantic error occurred, i.e. the USR 
     * cannot be converted into a dominance graph
     */
    protected abstract void decode(Reader reader, DomGraph graph, NodeLabels labels)
    throws IOException, ParserException, MalformedDomgraphException;
    
    /**
     * Reads an USR from a file and converts it to a labelled dominance graph.
     * This method opens the file with the given name and passes the reader
     * on to <code>decode</code>. You may pass the string <code>"-"</code>
     * instead of a filename; this will read the USR from standard input.
     * 
     * @param filename a filename, or <code>"-"</code> for reading from stdin
     * @param graph the dominance graph into which the USR is converted
     * @param labels the node labels of the labelled dominance graph
     * @throws IOException if an I/O error occurred while reading from the file
     * @throws ParserException if a syntactic error occurred while parsing the USR
     * @throws MalformedDomgraphException if a semantic error occurred, i.e. the USR 
     * cannot be converted into a dominance graph
     */
    public void decodeFile(String filename, DomGraph graph, NodeLabels labels)
    throws IOException, ParserException, MalformedDomgraphException {
        if( "-".equals(filename)) {
            // from stdin
            decode(new InputStreamReader(System.in), graph, labels);
        } else {
            decode(new FileReader(filename), graph, labels);
        }
    }
    
    /**
     * Reads an USR from a string and converts it to a labelled dominance graph.
     * This creates a <code>StringReader</code> and passes it on to the
     * <code>decode</code> method.
     * 
     * @param usr a string containing an USR
     * @param graph the dominance graph into which the USR is converted
     * @param labels the node labels of the labelled dominance graph
     * @throws IOException if an I/O error occurred while reading from the string
     * @throws ParserException if a syntactic error occurred while parsing the USR
     * @throws MalformedDomgraphException if a semantic error occurred, i.e. the USR 
     * cannot be converted into a dominance graph
     */
    public void decodeString(String usr, DomGraph graph, NodeLabels labels)
    throws IOException, ParserException, MalformedDomgraphException {
        decode(new StringReader(usr), graph, labels);
    }

}
