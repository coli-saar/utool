/*
 * Created on 28.07.2004
 *
 */
package de.saar.chorus.leonardo;



/**
 * The data that can be stored in the node of a dominance graph -- namely,
 * a name, a label, and a node type.
 * 
 * In addition, objects of this class can serve as popup targets, i.e.
 * they provide a menu item for a popup menu.
 *  
 * @author Alexander
 *
 */
public class NodeData extends DomGraphPopupTarget {
	private String name;
	private String label;
	private NodeType type;
	
	/**
	 * New node data for a labelled node (with label).
	 * 
	 * @param name
	 * @param label
	 * @param type
	 */
	public NodeData(NodeType type, String name, String label, JDomGraph parent) {
	    super(parent);
	    
		this.name = name;
		this.label = label;
		this.type = type;
	}
	
	/**
	 * New node data for an unlabelled node.
	 * 
	 * @param name
	 * @param type
	 */
	public NodeData(NodeType type, String name, JDomGraph parent) {
	    super(parent);
	    
		this.name = name;
		this.type = type;
		this.label = "";
	}
	
	public String getMenuLabel() {
	    if( label.equals("") ) {
	        return "Node " + name + " (hole)";
	    } else {
	        return "Node " + name + " (" + label + ")";
	    }
	}
	
	
	
	/**
	 * @return Returns the label.
	 */
	public String getLabel() {
		return label;
	}
	
	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * @return Returns the type.
	 */
	public NodeType getType() {
		return type;
	}
	
	public String getDesc() {
		return "(node " + name + " type=" + type + ", label=" + label + ")";
	}
	
	public String toString() {
		return label;
	}
}
