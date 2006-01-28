/*
 * @(#)NodeData.java created 25.01.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.graph;


/**
 * The data that can be stored in the node of a dominance graph -- namely,
 * a name, a label, and a node type.
 * 
 * In addition, objects of this class can serve as popup targets, i.e.
 * they provide a menu item for a popup menu.
 *  
 * @author Alexander Koller
 *
 */
public class NodeData {
    private String name;
    private NodeType type;
    
    /**
     * New node data for a labelled node (with label).
     * 
     * @param name
     * @param label
     * @param type
     */
    public NodeData(NodeType type, String name) {
        this.name = name;
        this.type = type;
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
    
    public String toString() {
        return ((type==NodeType.LABELLED)?"[L:":"[U:") + name + "]";
    }
}
