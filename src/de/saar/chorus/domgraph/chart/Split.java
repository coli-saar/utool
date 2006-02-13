/*
 * @(#)Split.java created 25.01.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.chart;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Split {
    private String rootFragment;
    private Map<String,List<Set<String>>> wccs;  // root/hole -> wccs
    
    public Split(String rootFragment) {
        this.rootFragment = rootFragment;
        wccs = new HashMap<String,List<Set<String>>>();
    }
    
    public void addWcc(String node, Set<String> wcc) {
        List<Set<String>> wccSet = wccs.get(node);
        
        if( wccSet == null ) {
            wccSet = new ArrayList<Set<String>>();
            wccs.put(node, wccSet);
        }
        
        wccSet.add(wcc);
    }
    
    
    public String getRootFragment() {
        return rootFragment;
    }

    public List<Set<String>> getWccs(String node) {
        return wccs.get(node);
    }
    
    public Set<String> getAllDominators() {
        return wccs.keySet();
    }
    
    
    public List<Set<String>> getAllSubgraphs() {
        List<Set<String>> ret = new ArrayList<Set<String>>();
        
        for( String node : wccs.keySet() ) {
            ret.addAll(wccs.get(node));
        }
        
        return ret;
    }
    
    public String toString() {
        return "<" + rootFragment + " " + wccs + ">";
    }
}
