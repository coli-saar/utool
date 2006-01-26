/*
 * @(#)OzTermOutputCodec.java created 25.01.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.codec.term;

public class OzTermOutputCodec extends TermOutputCodec {
    public OzTermOutputCodec() {
        super(" ");
        setName("term-oz");
        setExtension(".t.oz");
    }
}
