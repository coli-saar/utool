/*
 * @(#)PrologTermOutputCodec.java created 25.01.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.codec.term;

public class PrologTermOutputCodec extends TermOutputCodec {
    public PrologTermOutputCodec() {
        super(",");
        setName("term-prolog");
        setExtension(".t.pl");
    }
    
}
