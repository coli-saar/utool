/*
 * @(#)CompleteSplitSource.java created 03.02.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.chart;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.saar.chorus.domgraph.graph.DomGraph;

public class CompleteSplitSource implements Iterator<Split> {
    protected List<String> potentialFreeRoots;
    protected List<Split> splits;
    protected Iterator<Split> splitIt;
    
    public CompleteSplitSource(DomGraph graph, Set<String> subgraph) {
        initPotentialFreeRoots(graph, subgraph);
        computeAllSplits(graph,subgraph);
        splitIt = splits.iterator();
    }
    
    
    
    
    
    
    
    
    
    
    
    protected void computeAllSplits(DomGraph graph, Set<String> subgraph) {
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

    protected void initPotentialFreeRoots(DomGraph graph, Set<String> subgraph) {
        // initialise potentialFreeRoots with all nodes without
        // incoming dom-edges
        potentialFreeRoots = new ArrayList<String>();
        for( String node : subgraph ) {
            if( graph.indegOfSubgraph(node, null, subgraph) == 0 ) {
                potentialFreeRoots.add(node);
            }
        }
    }








    public int count() {
        return splits.size();
    }

    public boolean hasNext() {
        return splitIt.hasNext();
    }

    public Split next() {
        return splitIt.next();
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

}
