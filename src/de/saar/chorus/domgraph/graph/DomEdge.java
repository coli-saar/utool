/*
 * @(#)DomEdge.java created 25.01.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.graph;

public class DomEdge {
    private String src, tgt;

    public String getSrc() {
        return src;
    }

    public String getTgt() {
        return tgt;
    }

    public DomEdge(String src, String tgt) {
        super();
        // TODO Auto-generated constructor stub
        this.src = src;
        this.tgt = tgt;
    }
    
    public String toString() {
        return "[" + src + " <* " + tgt + "]";
    }
}
