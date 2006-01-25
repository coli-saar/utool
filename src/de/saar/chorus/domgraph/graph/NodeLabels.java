/*
 * @(#)NodeLabels.java created 25.01.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.graph;

import java.util.HashMap;
import java.util.Map;

public class NodeLabels {
    private Map<String,String> labels;
    
    public NodeLabels() {
        labels = new HashMap<String,String>();
    }
    
    public void clear() {
        labels.clear();
    }
    
    public void addLabel(String node, String label) {
        labels.put(node,label);
    }
    
    public String getLabel(String node) {
        return labels.get(node);
    }
}
