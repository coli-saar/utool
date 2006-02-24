/*
 * @(#)OzTermOutputCodec.java created 25.01.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.codec.term;


/**
 * An output codec for terms in Oz syntax. See 
 * {@link de.saar.chorus.domgraph.codec.term.TermOutputCodec} for more details.<p>
 * 
 * An example output looks as follows:<br/>
 * {@code f(a g(b))}
 * 
 * @author Alexander Koller
 *
 */
public class OzTermOutputCodec extends TermOutputCodec {
    public static String getName() {
        return "term-oz";
    }
    
    public static String getExtension() {
        return ".t.oz";
    }

    public OzTermOutputCodec() {
        super(" ");
    }
}
