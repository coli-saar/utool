/**
 * @file   Edges.java
 * @author Alexander Koller
 * @date   Wed May 21 15:02:07 2003
 * 
 * @brief  A type-safe enumeration of edges.
 * 
 * 
 */


package de.saar.chorus.corpus.tree;

import java.util.Vector;


/**
 * A type-safe enumeration of edges. 
 * 
 */
public class Edges extends GenericEnumeration {
    /** 
     * Construct the enumeration from a vector of Edge objects.
     * 
     * @param edges a Vector of Edge objects.
     * 
     */    
    Edges(Vector edges) {
	super(edges);
    }

    /** 
     * Return the next edge in the enumeration, as an Edge object.
     * 
     * @return the next edge in the enumeration.
     */    
    public Edge next() {
	return (Edge) nextElement();
    }
}
