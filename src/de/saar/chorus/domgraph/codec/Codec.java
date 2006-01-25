/*
 * @(#)ICodec.java created 25.01.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.codec;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;

import de.saar.chorus.domgraph.graph.DomGraph;

public interface Codec {
    public void decode(Reader inputStream, DomGraph graph)
    throws IOException, ParserException, MalformedDomgraphException;
    
    public void encode(DomGraph graph, OutputStream os);
}