/**
 * @file   Tree.java
 * @author Alexander Koller
 * @date   Wed May 21 14:18:19 2003
 * 
 * @brief  A tree with node and edge labels.
 * 
 */


package de.saar.coli.chorus.corpus.tree;

import java.util.*;
import java.io.*;
import electric.xml.Element;
import electric.xml.Child;
import electric.xml.Children;



/**
 * A tree with node and edge labels.
 *
 * The tree can be constructed from an Electric XML element and a
 * hashtable of words. It supports some basic tree functions, such as
 * finding the root, or computing the least upper bound of two nodes.
 * 
 */
public class Tree {
    /** A hash table that maps the IDs of internal nodes to Node objects. */
    protected Hashtable internal;

    /** A hash table that maps the IDs of leaves (= words) to Word objects. */
    protected Words words;

    /** The ID of the root of this tree. */
    protected String rootNodeID;

    /** The MultiTree for the sentence to which this Tree belongs. */
    protected MultiTree container;



    /**
     * Construct a tree from an element that's suitable for this purpose.
     * The prototypical examples are graph/nonterminals and topology.
     * A hashtable of word leaves is passed in the second argument.
     *
     * @param e an XML element from which to construct the tree.
     * @param words a hashtable that maps ids to Word objects.
     * @param container the MultiTree for the sentence to which this tree belongs (can safely be null).
     */
    Tree(Element e, Words words, MultiTree container) {
	this.container = container;
	internal = new Hashtable();
	rootNodeID = null;

	this.words = new Words(words);

	// Hunt for name of the root node. If we can't find one, default later
	// to the first node in the list.
	if( e.hasAttribute("root") )
	    rootNodeID = e.getAttribute("root");
	else if( (e.getParent() != null) && (((Element) e.getParent()).hasAttribute("root")) )
	    rootNodeID = ((Element) e.getParent()).getAttribute("root");

	// Iterate over nodes and add them to the hashtable.
	for( Children children = e.getChildren();
	     children.hasMoreElements(); ) {
	    Object child = children.nextElement();

	    if( child.getClass() == Element.class ) {
		Element childAsEl = (Element) child;
		Node node = new Node(childAsEl);

		internal.put(node.getID(), node);

		if( rootNodeID == null )
		    rootNodeID = node.getID();
	    }
	}

	// Iterate over nodes and add edges
	for( Children children = e.getChildren();
	     children.hasMoreElements(); ) {
	    Object child = children.next();

	    if( child.getClass() == Element.class ) {
		Element childAsEl = (Element) child;
		Node source = lookupNodeFromElement(childAsEl, "id");

		for( Children outedges = childAsEl.getChildren();
		     outedges.hasMoreElements(); ) {
		    Object target = outedges.nextElement();

		    if( target.getClass() == Element.class ) {
			Element targetAsEl = (Element) target;
			source.addOutEdge(new Edge(source, 
						   lookupNodeFromElement(targetAsEl, "idref"), 
						   targetAsEl));
		    }
		}
	    }
	}
    }

    /** 
     * Return the MultiTree for the sentence that this tree belongs to.
     *
     * @return the sentence.
     */    
    public MultiTree getSentence() {
	return container;
    }

    /** 
     * The root of the tree, or null if the tree has no nodes.
     *
     * @internal Will also return null when called from within the constructor,
     * because the root node and the node hashtable haven't been initialized yet
     * at that point.
     * 
     * @return the root of the tree.
     */
    public Node getRoot() {
	if( rootNodeID == null )
	    return null;
	else
	    return nodeForID(rootNodeID);
    }

    /** 
     * Is the node with the given id a leaf?
     * 
     * @param id the ID of a node in this tree.
     * 
     * @return Is this node a leaf?
     */
    protected boolean isLeaf(String id) {
	return words.containsEntryForId(id);
    }

    /** 
     * Is the node with the given id an internal node in this tree?
     * 
     * @param id the ID of a node in this tree.
     * 
     * @return Is this an internal node?
     */
    protected boolean isInternal(String id) {
	return internal.containsKey(id);
    }

    /** 
     * A string representation of the tree.
     * 
     * @return a string representation of the tree.
     */
    public String toString() {
	Node root = getRoot();

	if( root == null )
	    return "<tree with uninitialized root>";
	else
	    return root.toString();
    }

    /** 
     * Look up the node in this tree that corresponds to the given node ID.
     * 
     * @param id the ID of a node in this tree.
     * 
     * @return the node, or null if id isn't the ID of any node in this tree.
     */
    public Node nodeForID(String id) {
	Object inInternal = null;

	if( internal != null ) 
	    inInternal = internal.get(id);

	if( inInternal == null ) {
	    if( words == null )
		return null;
	    else
		return words.get(id);
	}
	else {
	    return (Node) inInternal;
	}
    }	

    /** 
     * Compute the least upper bound of the nodes in n.
     *
     * The least upper bound is the lowest node in the tree that dominates
     * every node in n. Such a node is guaranteed to exist because it's a tree.
     * If not all nodes in n are nodes in the same tree, this method returns
     * unspecified results.
     * 
     * @param n an array of nodes from the same tree.
     * 
     * @return the least upper bound of all nodes in n.
     *
     * @internal WARNING: argument type might change, depending on what kind of
     * node collection is easy to obtain.
     */
    public static Node leastUpperBound(Node[] n) {
	Vector candidates;

	if( n.length == 0 )
	    return null;

	else {
	    candidates = n[0].ancestors();

	    for( int i = 1; i < n.length; i++ ) {
		Vector anc = n[i].ancestors();
		Hashtable tmp = new Hashtable();

		// Collect the ancestors of n[i] into a hashtable ...
		for( Enumeration e = anc.elements(); e.hasMoreElements(); )
		    tmp.put(e.nextElement(), new Integer(1));

		// ... and remove everyone from candidates who isn't in this table.

		// Store all current candidates in an array and iterate through
		// the array, because iterating over an Enumeration doesn't cooperate
		// with removing elements from the vector within the loop body.
		Object[] candidateArr = candidates.toArray();
		for( int j = 0; j < candidateArr.length; j++ ) {
		    Object el = candidateArr[j];

		    if( !tmp.containsKey(el) )
			candidates.remove(el);
		}
	    }
	    
	    return (Node) candidates.lastElement();
	}
    }


    /**
     * Look up a node given an XML element. The Child object ch must really
     * be an object of class electric.xml.Element.
     *
     * @param ch an Element object from whose attributes to look up a node.
     * @param attributeName the name of the attribute that contains the node ID.
     * @return the node by that id.
     */
    protected Node lookupNodeFromElement(Child ch, String attributeName) {
	String id = ((Element) ch).getAttribute(attributeName);

	return nodeForID(id);
    }


    /** 
     * Compute the yield of this tree.
     *
     * The yield is defined as the space-separated concatenation of the leaves
     * below a node, in left-to-right order. This means that in the Negra annotation
     * scheme, the yield of a tree doesn't contain punctuation, because those nodes
     * aren't reachable from the root of the tree.
     * 
     * @return the yield of the tree.
     */
    public String yield() {
	return getRoot().yield();
    }


    /** 
     * Compute the surface form of a tree, as a string of space-separated tokens.
     *
     * Unlike yield(), this method simply concatenates the tokens of the sentence.
     * As a consequence, its output also contains e.g. punctuation symbols, whose
     * nodes typically aren't reachable from the root of the syntax tree.
     * 
     * @return that.
     */
    public String surface() {
	String[] wordkeys = (String[]) words.keySet().toArray(new String[0]);
	String[] wordvals = new String[wordkeys.length];

	Arrays.sort(wordkeys,
		    new Comparator() {
			    public int compare(Object o1, Object o2) {
				String s1 = (String) o1,
				    s2 = (String) o2;

				if( s1.length() < s2.length() )
				    return -1;
				else if( s1.length() > s2.length() )
				    return 1;
				else
				    return s1.compareTo(s2);
			    }
			});

	for( int i = 0; i < wordkeys.length; i++ ) {
	    Word w = words.get(wordkeys[i]);
	    wordvals[i] = w.getWord();
	}

	return Auxiliary.join(wordvals, " ");
    }
}
