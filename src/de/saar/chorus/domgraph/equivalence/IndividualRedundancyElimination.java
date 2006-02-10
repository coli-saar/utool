/*
 * @(#)IndividualRedundancyElimination.java created 10.02.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.equivalence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.saar.chorus.domgraph.chart.Chart;
import de.saar.chorus.domgraph.chart.Split;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;

public class IndividualRedundancyElimination extends RedundancyElimination {
    public IndividualRedundancyElimination(DomGraph graph, NodeLabels labels,
            EquationSystem eqs) {
        super(graph, labels, eqs);
    }

    public List<Split> getIrredundantSplits(Set<String> subgraph, List<Split> allSplits) {
        List<Split> splits = new ArrayList<Split>(allSplits);
        int i = 0;
        
        while( i < splits.size() ) {
            Split split = splits.get(i);
            
            if( isEliminableSplit(split, splits)) {
                splits.remove(split);
            } else {
                i++;
            }
        }
        
        return splits;
    }

    private boolean isEliminableSplit(Split split, List<Split> splitsForSubgraph) {
        Map<String,Set<String>> rootsToWccs = new HashMap<String,Set<String>>();
        String splitRoot = split.getRootFragment();
        Set<String> allRoots = graph.getAllRoots();
        
        // compute rootsToWccs
        for( Set<String> wcc : split.getAllSubgraphs() ) {
            for( String root : wcc ) {
                rootsToWccs.put(root, wcc);
            }
        }
        
        splitloop:
        for( Split otherSplit : splitsForSubgraph ) {
            if( !split.equals(otherSplit)) {
                String root = otherSplit.getRootFragment();
                Set<String> wcc = rootsToWccs.get(root);
                
                // check: is every other root in the same wcc, including splitRoot,
                // permutable with root?
                if( !isPermutable(root, splitRoot) ) {
                    continue;
                }
                
                for( String node : wcc ) {
                    if( allRoots.contains(node) && !root.equals(node) ) {
                        if( isPossibleDominator(node, root)) {
                            if( !isPermutable(root, node)) {
                                // if not, then continue
                                continue splitloop;
                            }
                        }
                    }
                }
                
                return true;
            }
        }
        
        return false;
    }
}
