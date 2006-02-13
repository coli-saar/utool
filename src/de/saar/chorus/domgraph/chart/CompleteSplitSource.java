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

/**
 * A split source which computes the complete list of 
 * splits for a subgraph. A <code>ChartSolver</code> which uses
 * an object of this class as its split source will compute
 * the set of all solved forms of the dominance graph.
 * 
 * @author Alexander Koller
 *
 */
public class CompleteSplitSource extends SplitSource {
    public CompleteSplitSource(DomGraph graph) {
        super(graph);
    }
    
    protected Iterator<Split> computeSplits(Set<String> subgraph) {
        SplitComputer sc = new SplitComputer(graph);
        List<Split> splits = new ArrayList<Split>();
        List<String> potentialFreeRoots = computePotentialFreeRoots(subgraph);
        
        //System.err.println("graph has " + potentialFreeRoots.size() + " free roots");

        for( String root : potentialFreeRoots ) {
          //  System.err.println("potential root: " + root);
            try {
                Split split = sc.computeSplit(root, subgraph);
                splits.add(split);
            } catch (RootNotFreeException e) {
                // if the root was not free, do nothing
            //    System.err.println("   - not free");
            }
        }
        
        return splits.iterator();
    }

}
