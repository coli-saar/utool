/*
 * @(#)DomEdge.java created 25.01.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.graph;


/**
 * A representation of a dominance edge, as a pair of a source and 
 * a target node.  This class is meant to be used within a representation
 * of a solved form: A <code>SolvedFormSpec</code> contains a collection of
 * <code>DomEdge</code>s; they are computed by a <code>SolvedFormIterator</code>;
 * and they are then inserted into a dominance graph by using the <code>makeSolvedForm</code>
 * method in <code>DomGraph</code>.<p>
 * 
 * For all other purposes, you probably don't want to use this class.
 * Use <code>Edge</code> instead; you can get the edges of a graph through
 * <code>getAllEdges</code.
 * 
 * @author Alexander Koller
 *
 */
public class DomEdge {
    private String src, tgt;

    /**
     * Get the source node in this pair.
     * 
     * @return the source node
     */
    public String getSrc() {
        return src;
    }

    /**
     * Get the target node in this pair.
     * 
     * @return the target node
     */
    public String getTgt() {
        return tgt;
    }

    /**
     * Constructor which takes a source and target node. 
     * 
     * @param src the source node
     * @param tgt the target node
     */
    public DomEdge(String src, String tgt) {
        this.src = src;
        this.tgt = tgt;
    }
    
    /**
     * Computes a string representation of this pair.
     */
    public String toString() {
        return "[" + src + " <* " + tgt + "]";
    }

	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((src == null) ? 0 : src.hashCode());
		result = PRIME * result + ((tgt == null) ? 0 : tgt.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final DomEdge other = (DomEdge) obj;
		if (src == null) {
			if (other.src != null)
				return false;
		} else if (!src.equals(other.src))
			return false;
		if (tgt == null) {
			if (other.tgt != null)
				return false;
		} else if (!tgt.equals(other.tgt))
			return false;
		return true;
	}
    
    
}
