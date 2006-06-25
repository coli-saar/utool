/*
 * @(#)InputCodec.java created 25.01.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.codec;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

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
    public abstract void decode(Reader reader, DomGraph graph, NodeLabels labels)
    throws IOException, ParserException, MalformedDomgraphException;
    
    
    /**
     * Determines a <code>Reader</code> from which the USR specified
     * by the <code>spec</code> will be read. In this default implementation,
     * <code>spec</code> is a filename, and the reader will be a
     * <code>FileReader</code> for this file. In the special case where
     * <code>spec</code> starts with the prefix <code>ex:</code>, the rest
     * of the string is interpreted as a filename relative to the directories
     * <code>projects/Domgraph/examples</code> and <code>examples</code>, which
     * may be anywhere on the classpath (or in the Jar). 
     * 
     * @param spec a filename
     * @return a reader for reading from this file
     * @throws IOException no file with the given name exists
     */
    public Reader getReaderForSpecification(String spec)
    throws IOException {
        if( spec.startsWith("ex:")) {
            // load an example input file that was packaged with the Jar
            String filename = spec.substring(3);
            ClassLoader loader = getClass().getClassLoader();
            InputStream istream = loader.getResourceAsStream("projects/Domgraph/examples/" + filename);
            
            if( istream == null ) {
                istream = loader.getResourceAsStream("examples/" + filename);
            }
            
            if( istream == null ) {
                throw new IOException("Couldn't find an example file with name " + filename);
            } else {
                return new InputStreamReader(istream);
            }
        } else {
            return new FileReader(spec);
        }
    }
}
