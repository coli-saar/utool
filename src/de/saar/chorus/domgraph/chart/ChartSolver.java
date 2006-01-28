/*
 * @(#)ChartSolver.java created 25.01.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.chart;


import java.util.List;
import java.util.Set;

import de.saar.chorus.domgraph.graph.DomGraph;

public class ChartSolver {
    private DomGraph graph;
    private Chart chart;
    private SplitComputer splitc;
    private FreeRootsComputer freerc;
    //private Map<Set<String>,String> fragmentMap;
    private Set<String> roots;
    
    // ASSUMPTION graph is compact and weakly normal
    public ChartSolver(DomGraph graph, Chart chart) {
        this.graph = graph;
        this.chart = chart;
        
        splitc = new SplitComputer(graph);
        freerc = new FreeRootsComputer(graph);
        roots = graph.getAllRoots();
    }
    
    public boolean solve() {
        List<Set<String>> wccs = graph.wccs();

        for( Set<String> wcc : wccs ) {
            chart.addToplevelSubgraph(wcc);
            if( !solve(wcc) ) {
                return false;
            }
        }
        return true;
    }
    
    
    private boolean solve(Set<String> subgraph) {
        Set<String> freeRoots;
        int numRootsInSubgraph;
        
        //System.err.println("solve: " + subgraph);
        
        // If fragset is already in chart, nothings needs to be done.
        if( chart.containsSplitFor(subgraph) ) {
            return true;
        }
        
        // If the fs has no free roots, then the original graph is unsolvable.
        freeRoots = freerc.getFreeRoots(subgraph);
        if( freeRoots.isEmpty() ) {
            return false;
        }
        
        // If fs is singleton and its root is free, it is in solved form.
        // The fs will be entered into the chart as part of the parent's split.
        // NB: Even in a compact graph, there may be fragments with >1 node!
        numRootsInSubgraph = 0;
        for( String node : subgraph ) {
            if( roots.contains(node) ) {
                numRootsInSubgraph++;
            }
        }
        
        if( numRootsInSubgraph == 1 ) {
            return true;
        }
        
        // Otherwise, iterate over all possible free roots
        for( String root : freeRoots ) {
            // make new Split
            Split split = splitc.computeSplit(root, subgraph);
            
            //System.err.println("split: " + split);
            //System.err.println("subgraphs: " + split.getAllSubgraphs());
            
            // iterate over wccs
            for( Set<String> wcc : split.getAllSubgraphs() ) {
                if( !solve(wcc) ) {
                    return false;
                }
            }
            
            // add split to chart
            chart.addSplit(subgraph, split);
        }

        return true;
    }
}
