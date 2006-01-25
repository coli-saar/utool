/*
 * @(#)NodeData.java created 25.01.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.graph;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private String simplifiedLabel;
    private String label;
    private NodeType type;
    
    /**
     * New node data for a labelled node (with label).
     * 
     * @param name
     * @param label
     * @param type
     */
    public NodeData(NodeType type, String name, String label) {
        this.name = name;
        setLabel(label);
        this.type = type;
    }
    
    /**
     * New node data for an unlabelled node.
     * 
     * @param name
     * @param type
     */
    public NodeData(NodeType type, String name) {
        this.name = name;
        this.type = type;
        this.label = "";
    }
    
    private void setLabel(String label) {
        Pattern p = Pattern.compile("\\s+\\S+:");
        Matcher m = p.matcher(label);
        simplifiedLabel = m.replaceAll(",");
        
        this.label = label;
    }
    
    /**
     * @return Returns the label.
     */
    public String getLabel() {
        return label;
    }
    

    /**
     * @return Returns the simplified label.
     */
    public String getSimplifiedLabel() {
        return simplifiedLabel;
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
        switch(type) {
        case LABELLED: return "<L:" + simplifiedLabel + ">";
        case UNLABELLED: return "<U:" + name + ">";
        default: return null;
        }
    }
}
