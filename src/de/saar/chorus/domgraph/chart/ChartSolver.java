/*
 * @(#)ChartSolver.java created 25.01.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.chart;


import java.lang.reflect.Constructor;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.saar.chorus.domgraph.graph.DomGraph;

public class ChartSolver {
    private DomGraph graph;
    private Chart chart;
    private Set<String> roots;
    private Class splitIteratorClass;
    
    public ChartSolver(DomGraph graph, Chart chart) {
        try {
            init(graph, chart, CompleteSplitSource.class);
        } catch(IllegalSplitSourceException e) {
            // this should never happen
            assert false;
        }
    }

    public ChartSolver(DomGraph graph, Chart chart, Class splitIteratorClass) throws IllegalSplitSourceException {
        init(graph, chart, splitIteratorClass);
    }

    private void init(DomGraph graph, Chart chart, Class splitIteratorClass) throws IllegalSplitSourceException {
        try {
            Set<String> subgraph = graph.getAllNodes();
            Constructor constructor = splitIteratorClass.getConstructor(DomGraph.class, Set.class);
            Iterator<Split> x = (Iterator<Split>) constructor.newInstance(graph, subgraph);
        } catch(Exception e) {
            throw new IllegalSplitSourceException("The class you specified does not implement Iterator<Split>");
        }
        
        this.splitIteratorClass = splitIteratorClass;
        
        this.graph = graph;
        this.chart = chart;
        
        // ASSUMPTION graph is compact and weakly normal
        assert graph.isCompact();
        assert graph.isWeaklyNormal();
        
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
        Iterator<Split> splits;
        int numRootsInSubgraph;
        
        //System.err.println("solve: " + subgraph);
        
        // If fragset is already in chart, nothings needs to be done.
        if( chart.containsSplitFor(subgraph) ) {
            //System.err.println(" - already in chart");
            return true;
        }
        
        /*
        // If the fs has no free roots, then the original graph is unsolvable.
        freeRoots = freerc.getFreeRoots(subgraph);
        if( freeRoots.isEmpty() ) {
            return false;
        }
        */
        
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

        // get splits for this subgraph
        splits = makeSplitIterator(subgraph);
        //System.err.println(" - has " + splits.count() + " splits");
        
        // if there are none (i.e. there are no free roots),
        // then the original graph is unsolvable
        if( !splits.hasNext() ) {
            return false;
        }
        
        else {
            while( splits.hasNext() ) {
                Split split = splits.next();
                
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

    protected Iterator<Split> makeSplitIterator(Set<String> subgraph) {
        try {
            Constructor constructor = splitIteratorClass.getConstructor(DomGraph.class, Set.class);
            return (Iterator<Split>) constructor.newInstance(graph, subgraph);
        } catch(Exception e) {
            // this should never happen -- we checked it before!
            assert false;
            return null;
        }
    }
}
