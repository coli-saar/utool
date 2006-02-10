/*
 * @(#)PermutabilityRedundancyElimination.java created 10.02.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.equivalence;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import de.saar.chorus.domgraph.chart.Chart;
import de.saar.chorus.domgraph.chart.Split;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;

public class PermutabilityRedundancyElimination extends RedundancyElimination {

    public PermutabilityRedundancyElimination(DomGraph graph, NodeLabels labels, EquationSystem eqs) {
        super(graph, labels, eqs);
    }


    protected List<Split> getIrredundantSplits(Set<String> subgraph, Chart c) {
        List<Split> splits = c.getSplitsFor(subgraph);
        List<Split> ret = new ArrayList<Split>(1);
        
        for( Split split : splits ) {
            if( isPermutableSplit(split, subgraph)) {
                // i.e. the split with this index is permutable => eliminate all others
                ret.add(split);
                return ret;
            }
        }
        
        return splits;
    }


    private boolean isPermutableSplit(Split s, Set<String> subgraph) {
        String splitRoot = s.getRootFragment();
        
        //System.err.println("\nCheck split " + s + " for permutability.");
        
        for( String root : subgraph ) {
            if( graph.isRoot(root) &&  !root.equals(splitRoot) ) {
                if( isPossibleDominator(root, splitRoot)) {
                    if( !isPermutable(root, splitRoot) ) {
                        //System.err.println("  -- not permutable with " + root);
                        return false;
                    } else {
                        //System.err.println("  -- permutable with " + root);
                    }
                } else {
                    //System.err.println("  -- other root " + root + " is not a p.d.");
                }
            }
        }
        
        //System.err.println("  -- split is permutable!");
        return true;
    }
}
