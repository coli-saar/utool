/**
 * @file   Nodes.java
 * @author Alexander Koller
 * @date   Wed May 21 15:02:07 2003
 * 
 * @brief  A type-safe enumeration of nodes.
 * 
 * 
 */

package de.saar.chorus.corpus.tree;

import java.util.Vector;


/**
 * A type-safe enumeration of nodes.
 * 
 */
public class Nodes extends GenericEnumeration {
    /** 
     * Construct the enumeration from a vector of Node objects.
     * 
     * @param nodes a Vector of Node objects.
     * 
     */
    Nodes(Vector nodes) {
	super(nodes);
    }

    /** 
     * Construct an enumeration containing the targets of an enumeration of Edges.
     *
     * The nodes in this enumeration will be the target nodes of the Edge objects
     * listed in outEdges, in the same order.
     * 
     * @param outEdges a Vector of Edge objects.
     * 
     */
    Nodes(Edges outEdges) {
	entries = new Vector(outEdges.size());

	if( outEdges != null ) {
	    while( outEdges.hasMoreElements() )
		entries.add(outEdges.next().getTarget());
	}

	numEntries = entries.size();
	currentIndex = 0;
    }

    
    /** 
     * Return the next node in the enumeration, as a Node object.
     * 
     * @return the next node in the enumeration.
     */    
    public Node next() {
	return (Node) nextElement();
    }
}
