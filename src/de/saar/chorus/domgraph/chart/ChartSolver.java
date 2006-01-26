/*
 * @(#)ChartSolver.java created 25.01.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.chart;

import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.EdgeType;

public class ChartSolver {
    private DomGraph graph;
    private Chart chart;
    
    // ASSUMPTION graph is compact and weakly normal
    public ChartSolver(DomGraph graph) {
        this.graph = new DomGraph(graph); // subgraph which we can modify
        chart = new Chart();
    }
    
    public boolean solve() {
        return solve(graph.getAllRoots());
    }
    
    private boolean solve(Set<String> fragset) {
        Set<String> freeRoots;
        
        // If fragset is already in chart, nothings needs to be done.
        if( chart.containsSplitFor(fragset) ) {
            return true;
        }
        
        // If the fs has no free roots, then the original graph is unsolvable.
        freeRoots = getFreeRoots();
        if( freeRoots.isEmpty() ) {
            return false;
        }
        
        // If fs is singleton and its root is free, it is in solved form.
        // The fs will be entered into the chart as part of the parent's split.
        if( fragset.size() == 1 ) {
            return true;
        }
        
        // Otherwise, iterate over all possible free roots
        for( String root : freeRoots ) {
            Split split = new Split(root);
            List<String> holes = graph.getChildren(root, EdgeType.TREE);
            Map<Integer,String> wccIndexToHole = new HashMap<Integer,String>();
            Map<String,String> nodeToHole = new HashMap<String,String>();
            
            graph.hide(root);
            
            // assign nodes to wccs
            Map<String,Integer> wccMap = graph.computeWccMap();
            for( String hole : holes ) {
                wccIndexToHole.put(wccMap.get(hole), hole);
            }
            
            for( String node : graph.getAllNodes() ) {
                if( wccIndexToHole.containsKey(wccMap.get(node))) {
                    nodeToHole.put(node, wccIndexToHole.get(wccMap.get(node)));
                } else {
                    nodeToHole.put(node, root);
                }
            }
            
            // remove holes and recurse
            for( String hole : holes ) {
                graph.hide(hole);
            }
            
            for( Set<String> wcc : graph.wccs() ) {
                DomGraph backup = graph;
                graph = new DomGraph(graph, wcc, null);
                Set<String> rootsInWcc = graph.pickRootsFrom(wcc);
                
                split.addWcc(nodeToHole.get(wcc.iterator().next()), rootsInWcc);

                if( !solve(rootsInWcc) ) {
                    return false;
                }
                
                graph = backup;
            }
            
            for( String hole : holes ) {
                graph.restore(hole);
            }
            
            chart.addSplit(fragset, split);
            graph.restore(root);
        }
        
        
        return true;
    }

    private void restoreAll(Set<String> hidden) {
        for( String node : hidden ) {
            graph.restore(node);
        }
    }

    private Set<String> hideAllNodesExcept(Set<String> nodes) {
        Set<String> hidden = new HashSet<String>();
        
        for( String node : graph.getAllNodes() ) {
            if( !nodes.contains(node) ) {
                hidden.add(node);
                graph.hide(node);
            }
        }
        
        return hidden;
    }

    // compute the free roots of the current graph
    private Set<String> getFreeRoots() {
        Set<String> ret = new HashSet<String>();
        Set<String> allNodes = graph.getAllNodes();
        
        for( String node : allNodes ) {
            boolean isFree = true;
            
            if( graph.indeg(node) == 0 ) {
                // roots without incoming dom edges
                List<String> holes = graph.getChildren(node, EdgeType.TREE);
                Set<Integer> seenWccs = new HashSet<Integer>();

                DomGraph subsubgraph = new DomGraph(graph);
                subsubgraph.hide(node);
                Map<String,Integer> wccMap = subsubgraph.computeWccMap();
                
                for( String hole : holes ) {
                    if( seenWccs.contains(wccMap.get(hole))) {
                        isFree = false;
                    } else {
                        seenWccs.add(wccMap.get(hole));
                    }
                }
                
                // TODO what about dom edges out of the root?
                
                if( isFree ) {
                    ret.add(node);
                }
            }
        }
        
        return ret;
    }
    
    public Chart getChart() {
        return chart;
    }
}
