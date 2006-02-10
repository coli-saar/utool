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

public abstract class InputCodec {
    public abstract void decode(Reader inputStream, DomGraph graph, NodeLabels labels)
    throws IOException, ParserException, MalformedDomgraphException;
    
    public void decodeFile(String specification, DomGraph graph, NodeLabels labels)
    throws IOException, ParserException, MalformedDomgraphException {
        if( "-".equals(specification)) {
            // from stdin
            decode(new InputStreamReader(System.in), graph, labels);
        } else {
            decode(new FileReader(specification), graph, labels);
        }
    }
    
    public void decodeString(String usr, DomGraph graph, NodeLabels labels)
    throws IOException, ParserException, MalformedDomgraphException {
        decode(new StringReader(usr), graph, labels);
    }

    public String getName() {
        return name;
    }
    
    public String getExtension() {
        return extension;
    }
    
    
    
    protected String name;
    protected String extension;
    
    protected void setName(String name) {
        this.name = name;
    }
    
    protected void setExtension(String extension) {
        this.extension = extension;
    }
    
}
