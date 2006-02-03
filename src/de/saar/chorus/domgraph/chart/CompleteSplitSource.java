/*
 * @(#)CompleteSplitSource.java created 03.02.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.chart;

import java.util.ArrayList;
import java.util.Set;

import de.saar.chorus.domgraph.graph.DomGraph;

public class CompleteSplitSource extends SplitSource {
    public CompleteSplitSource(DomGraph graph, Set<String> subgraph) {
        super(graph,subgraph);
    }
    
    protected void computeSplits(DomGraph graph, Set<String> subgraph) {
        SplitComputer sc = new SplitComputer(graph);
        
        splits = new ArrayList<Split>();
        for( String root : potentialFreeRoots ) {
            try {
                Split split = sc.computeSplit(root, subgraph);
                splits.add(split);
            } catch (RootNotFreeException e) {
                // if the root was not free, do nothing
            }
        }
    }

}
