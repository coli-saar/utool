/**
 * @file   Edge.java
 * @author Alexander Koller
 * @date   Wed May 21 15:45:03 2003
 * 
 * @brief  A labeled edge.
 * 
 * 
 */



package de.saar.chorus.corpus.tree;

import java.util.Hashtable;

import electric.xml.Attribute;
import electric.xml.Attributes;
import electric.xml.Element;


/**
 * A labeled edge.
 *
 * An edge connects two Nodes and has a label and possibly some additional
 * attributes.
 * 
 */
public class Edge {
    protected Node source, target;
    protected String label;
    protected Hashtable attributes;

    /**
     * Basic constructor for an edge from source to target with label,
     * and no attributes.
     *
     * @param source source node
     * @param target target node
     * @param label edge label
     */
    Edge(Node source, Node target, String label) {
	this.source = source;
	this.target = target;
	this.label = label;
	attributes = new Hashtable();
    }

    /**
     * Basic constructor for an edge from source to target with no label
     * or attributes.
     *
     * @param source source node
     * @param target target node
     */
    Edge(Node source, Node target) {
	this.source = source;
	this.target = target;
	this.label = null;
	attributes = new Hashtable();
    }
	
    /**
     * An edge from source to target. Read label and attributes from e.
     *
     * @param source source node
     * @param target target node
     * @param e the XML element that contains a label attribute and possibly others.
     */
    Edge(Node source, Node target, Element e) {
	this.source = source;
	this.target = target;

	this.label = e.getAttribute("label");

	for( Attributes attrs = e.getAttributeObjects();
	     attrs.hasMoreElements(); ) {
	    Attribute attr = attrs.next();
	    if( !attr.getName().equals("label") && !attr.getName().equals("idref") )
		attributes.put(attr.getName(), attr.getValue());
	}
    }

    /** 
     * Return the source node of the edge.
     * 
     * @return The source node of the edge.
     */
    public Node getSource() {
	return source;
    }

    /** 
     * Return the target node of the edge.
     * 
     * @return The target node of the edge.
     */
    public Node getTarget() {
	return target;
    }

    /** 
     * Return the label of the edge.
     * 
     * @return The label of the edge.
     */
    public String getLabel() {
	return label;
    }

    /** 
     * Return all attributes that are defined on this edge.
     * Note that "label" is never a node attribute.
     * 
     * @return an array containing all attribute names.
     */
    public String[] getAttributes() {
	return (String[]) attributes.keySet().toArray(new String[0]);
    }

    /** 
     * Return the value for a given attribute, or null if the edge
     * doesn't have an attribute by that name.
     * 
     * @param key the name of an attribute of this edge.
     * 
     * @return The value of this attribute.
     */
    public String getAttributeValue(String key) {
	return (String) attributes.get(key);
    }
}
