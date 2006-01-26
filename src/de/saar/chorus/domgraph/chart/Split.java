/*
 * @(#)Split.java created 25.01.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.chart;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Split {
    private String rootFragment;
    private Map<String,Set<Set<String>>> wccs;  // root/hole -> wccs
    
    public Split(String rootFragment) {
        this.rootFragment = rootFragment;
        wccs = new HashMap<String,Set<Set<String>>>();
    }
    
    public void addWcc(String node, Set<String> wcc) {
        Set<Set<String>> wccSet = wccs.get(node);
        
        if( wccSet == null ) {
            wccSet = new HashSet<Set<String>>();
            wccs.put(node, wccSet);
        }
        
        wccSet.add(wcc);
    }
    
    
    public String getRootFragment() {
        return rootFragment;
    }

    public Set<Set<String>> getWccs(String node) {
        return wccs.get(node);
    }
    
    public Set<Set<String>> getAllSubgraphs() {
        Set<Set<String>> ret = new HashSet<Set<String>>();
        
        for( String node : wccs.keySet() ) {
            ret.addAll(wccs.get(node));
        }
        
        return ret;
    }
    
    public String toString() {
        return "<" + rootFragment + " " + wccs + ">";
    }
}
