/**
 * @file   GenericEnumeration.java
 * @author Alexander Koller
 * @date   Wed May 21 14:58:56 2003
 * 
 * @brief  A generic implementation of the Enumeration interface using Vectors.
 * 
 * 
 */


package de.saar.coli.chorus.corpus.tree;

import java.util.*;


/**
 * A generic implementation of java.util.Enumeration using Vectors.
 * 
 */
class GenericEnumeration implements Enumeration {
    /** The objects over which we want to iterate. */
    protected Vector entries;

    /** The index in the entries list at which the next object is located. */
    protected int currentIndex;

    /** The total number of entries in the vector. */
    protected int numEntries;

    /** 
     * Constructor for an empty enumeration.
     * 
     */
    GenericEnumeration() {
	entries = new Vector();
	currentIndex = numEntries = 0;
    }

    /** 
     * Construct an enumeration from a vector.
     * 
     * @param entries a vector over which we want to enumerate.
     * 
     */
    GenericEnumeration(Vector entries) {
	if( entries == null )
	    this.entries = new Vector();
	else
	    this.entries = entries;

	numEntries = entries.size();
	currentIndex = 0;
    }

    /** 
     * Return true if we haven't yet seen all elements in the enumeration.
     * 
     * @return Are there more unseen elements in the enumeration?
     */
    public boolean hasMoreElements() {
	return currentIndex < numEntries;
    }

    /** 
     * Return the next element in the enumeration.
     * 
     * @return The next element.
     */
    public Object nextElement() {
	return entries.get(currentIndex++);
    }

    /** 
     * Return the total number of elements in the enumeration.
     * 
     * @return The total number of elements in the enumeration.
     */
    public int size() {
	return numEntries;
    }
}
