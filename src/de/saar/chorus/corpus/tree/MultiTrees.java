/**
 * @file   MultiTrees.java
 * @author Alexander Koller
 * @date   Wed May 21 15:02:07 2003
 * 
 * @brief  A type-safe enumeration of multi-level trees.
 * 
 * 
 */



package de.saar.coli.chorus.corpus.tree;

import java.util.*;


/**
 * A type-safe enumeration of multi-level trees.
 * 
 */
public class MultiTrees extends GenericEnumeration {
    /** 
     * Construct the enumeration from a vector of MultiTree objects.
     * 
     * @param mts a Vector of MultiTree objects.
     * 
     */    
    MultiTrees(Vector mts) {
	super(mts);
    }

    /** 
     * Return the next multi-level tree in the enumeration, as a MultiTree object.
     * 
     * @return the next multi tree in the enumeration.
     */    
    public MultiTree next() {
	return (MultiTree) nextElement();
    }
}
