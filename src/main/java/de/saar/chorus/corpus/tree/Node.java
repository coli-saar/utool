/**
 * @file   Node.java
 * @author Alexander Koller
 * @date   Wed May 21 14:29:14 2003
 * 
 * @brief  A tree node.
 * 
 */

package de.saar.chorus.corpus.tree;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import electric.xml.Attribute;
import electric.xml.Attributes;
import electric.xml.Element;


/**
 * A node in a tree.
 *
 * The node is connected to other nodes over labeled edges, and knows
 * to which tree it belongs. It has an ID, a category, and arbitrarily
 * many other attributes.
 *
 * @todo Sensible, symmetrical treatment of addChild and setParent.
 * At the moment, addChild sets myself as the new child's parent, but
 * setParent doesn't remove me from the old parent's children, or add
 * me to the new parent's.
 *
 */
public class Node  {
    /** The unique ID of this node. */
    protected String id;

    /** The category (= node label) of this node. */
    protected String cat;

    /** A hashtable that maps attribute names (Strings) to other Strings. */
    protected Hashtable attributes; 

    /** The edge that comes into this node. In a connected node, parent = null 
     * means that this is the root of the tree. */
    protected Edge parent;

    /** The outgoing edges of this node. The elements of the Vector are all
     * Edge objects. */
    protected Vector children; 



    /** 
     * A dummy constructor that should never be called by the user.
     */
    Node() {
	id = cat = null;
	attributes = new Hashtable();
	parent = null;
	children = new Vector();
    }

    /**
     * Construct node from the information contained in an XML element.
     * This element must contain attributes "id" and "cat" from which we take
     * the node's ID and category. Further attributes of the element are
     * stored in the node's attributes.
     *
     * The new node is unconnected and belongs to the tree t.
     *
     * @param e the element from which to construct the node.
     * @param t the tree to which the node belongs.
     */
    Node(Element e) {

	attributes = new Hashtable();

	id = e.getAttribute("id");
	cat = e.getAttribute("cat");

	for( Attributes attrs = e.getAttributeObjects();
	     attrs.hasMoreElements(); ) {
	    Attribute attr = attrs.next();
	    String name = attr.getName();
	    
	    if( !name.equals("id") && !name.equals("cat") )
		attributes.put(name, attr.getValue());
	}

	parent = null;
	children = new Vector();
	//	tree = t;
    }


    /** 
     * A copy constructor.
     *
     * The new node has fresh copies of the internal data structures, so
     * it can be moved into a different tree, or connected via different edges,
     * without affecting the original node.
     *
     * However, the constructor doesn't copy the objects that these structures 
     * refer to (e.g. the edges that connect this node to others). So a destructive
     * change to such an object will affect all copies of the node.
     * 
     * @param n the node to be copied.
     */
    Node(Node n) {
	id = n.id;
	cat = n.cat;
	parent = n.parent;
	//	tree = n.tree;

	children = new Vector(n.children.size());
	attributes = new Hashtable(n.attributes.size());

	for( Enumeration el = n.children.elements(); el.hasMoreElements() ; )
	    children.add(el.nextElement());

	for( Enumeration keys = n.attributes.keys(); keys.hasMoreElements() ; ) {
	    Object key = keys.nextElement();
	    attributes.put(key, n.attributes.get(key));
	}
    }
	

    /** 
     * Create a new copy of this node, by calling the copy constructor.
     * 
     * @return A new Node that looks just like me.
     */
    public Object clone() {
	return new Node(this);
    }

    /** 
     * Set the incoming edge (and thus, the parent) of the current node.
     * 
     * @param p an edge whose target is this node.
     */
    public void setInEdge(Edge p) {
	parent = p;
    }

    /** 
     * Add an outgoing edge (and thus, a child) to the current node.
     * 
     * @param e an edge whose source is this node.
     */
    public void addOutEdge(Edge e) {
	children.add(e);
	e.getTarget().setInEdge(e);
    }

    /** 
     * Return the edge that goes into this node.
     * 
     * @return The edge that goes into this node.
     */
    public Edge getInEdge() {
	return parent;
    }

    /** 
     * Return the parent of this node (i.e. the source of the incoming edge).
     * 
     * @return The parent of this node.
     */
    public Node getParent() {
	if( parent == null )
	    return null;
	else
	    return parent.getSource();
    }

    /** 
     * Return the outgoing edges of this node.
     * 
     * @return The outgoing edges of this node.
     */
    public Edges getOutEdges() {
	return new Edges(children);
    }

    /** 
     * Return the children of this node (i.e. the targets of the outgoing edges).
     * 
     * @return The children of this node.
     */
    public Nodes getChildren() {
	return new Nodes(getOutEdges());
    }

    /** 
     * Return all attributes that are defined on this node.
     * Note that "cat" and "id" are never node attributes.
     * 
     * @return an array containing all attribute names.
     */
    public String[] getAttributes() {
	return (String[]) attributes.keySet().toArray(new String[0]);
    }

    /** 
     * Return the value for a given attribute, or null if the node
     * doesn't have an attribute by that name.
     * 
     * @param key the name of an attribute of this node.
     * 
     * @return The value of this attribute.
     */
    public String getAttributeValue(String key) {
	return (String) attributes.get(key);
    }

    /** 
     * Return the ID of this node.
     * 
     * @return The ID of this node.
     */
    public String getID() {
	return id;
    }

    /** 
     * Return the category of this node.
     * 
     * @return The category of this node.
     */
    public String getCat() {
	return cat;
    }

    /** 
     * A string representation of the tree.
     * 
     * @return a string representation of the tree.
     */
    public String toString() {
	return "(" + id + ":" + cat + ")";
    }


    /** 
     * Compute the yield of the subtree below this node.
     *
     * The words at the leaves are separated by spaces.
     * 
     * @return the yield of the subtree below this node.
     */
    public String yield() {
	Nodes children = getChildren();
	String[] subyields = new String[children.size()];
	int i = 0;
	
	while(children.hasMoreElements()) {
	    subyields[i++] = children.next().yield();
	}

	return Auxiliary.join(subyields, " ");
    }

    /** 
     * Compute the (reflexive, transitive) ancestors of the node in its tree.
     * 
     * The vector this method returns contains the ancestors in top-down order,
     * i.e. its first entry is the root of the tree, and its last entry is the
     * node itself.
     * 
     * @return the ancestors of this node.
     */    
    public Vector ancestors() {
	Vector ret;

	if( getParent() == null ) {
	    ret = new Vector();
	} else {
	    ret = getParent().ancestors();
	}

	ret.add(this);

	return ret;
    }

    // Position (first pos is 1) where this constituent starts.
    public int startPos() {
	return ((Edge) children.firstElement()).getTarget().startPos();
    }

    // Position (first pos is 1) of the last word in this constituent.
    public int endPos() {
	return ((Edge) children.lastElement()).getTarget().endPos();
    }
}
