/*
 * @(#)NodeData.java created 25.08.2005
 * 
 * Copyright (c) 2005 Alexander Koller
 *  
 */

package de.saar.chorus.tag.viewer;

import de.saar.chorus.jgraph.improvedjgraph.INodeData;

public class NodeData implements INodeData<NodeType> {
    private NodeType type;
    private String name;
    
    public NodeData(NodeType type, String name) {
        this.type = type;
        this.name = name;
    }

    public NodeType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getToolTipText() {
        return "Tooltip: " + name;
    }
    
    public String toString() {
        return name + type.getMarker();
    }

}
