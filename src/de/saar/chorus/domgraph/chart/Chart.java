/*
 * @(#)Chart.java created 25.01.2006
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

import org._3pq.jgrapht.util.ModifiableInteger;

public class Chart {
    private Map<Set<String>, List<Split>> chart;
    private Map<Set<String>, ModifiableInteger> refcount;
    private int size;
    private List<Set<String>> completeFragset;
    
    public Chart() {
        chart = new HashMap<Set<String>, List<Split>>();
        refcount = new HashMap<Set<String>, ModifiableInteger>();
        completeFragset = new ArrayList<Set<String>>();
        size = 0;
    }
    
    public void addSplit(Set<String> fragset, Split split) {
        List<Split> splitset = chart.get(fragset);
        
        if( splitset == null ) {
            splitset = new ArrayList<Split>();
            chart.put(fragset, splitset);
        }
        
        splitset.add(split);
        
        
        for( Set<String> subgraph : split.getAllSubgraphs() ) {
            if( refcount.containsKey(subgraph)) {
                ModifiableInteger x = refcount.get(subgraph);
                x.setValue(x.intValue()+1);
            } else {
                refcount.put(subgraph, new ModifiableInteger(1));
            }
        }
        
        size++;
    }
    
    public List<Split> getSplitsFor(Set<String> subgraph) {
        return chart.get(subgraph);
    }
    
    public boolean containsSplitFor(Set<String> subgraph) {
        return chart.containsKey(subgraph);
    }
    

    public int size() {
        return this.size;
    }
    
    public String toString() {
        StringBuilder ret = new StringBuilder();
        
        for( Set<String> fragset : chart.keySet() ) {
            for( Split split : chart.get(fragset) ) {
                ret.append(fragset.toString() + " -> " + split + "\n");
            }
        }
        
        return ret.toString();
    }

    public List<Set<String>> getCompleteFragsets() {
        return completeFragset;
    }

    public void addCompleteFragset(Set<String> completeFragset) {
        this.completeFragset.add(completeFragset);
    }
}
