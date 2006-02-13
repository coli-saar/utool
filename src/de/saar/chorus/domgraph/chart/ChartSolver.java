/*
 * @(#)ChartSolver.java created 25.01.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.chart;


import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.saar.chorus.domgraph.graph.DomGraph;



/**
 * A solver for weakly normal dominance graphs. This solver computes
 * a {@link Chart} as described in Koller & Thater (2005), "The evolution
 * of dominance constraint solvers", ACL-05 Workshop on Software. It
 * can be seen as an implementation of the Bodirsky et al. 2005 graph
 * solver.<p>
 * 
 * This solver requires that the input dominance graph is weakly normal
 * and compact. It will successively compute the splits corresponding
 * to the free fragments of the subgraphs of the dominance graph, and
 * adds them to the chart.<p>
 * 
 * The solver relies on an object of a subclass of {@link SplitSource} 
 * to provide the splits of a subgraph. By default, it uses
 * an object of the class {@link CompleteSplitSource}, which computes
 * all splits of this subgraph. Alternatively, you can provide
 * a split source which only adds a certain subset of all splits
 * to the chart.<p> 
 * 
 * Notice that the role of this class is only to fill the chart. The actual
 * solved forms can later be extracted from the chart using a
 * {@link SolvedFormIterator} object.
 * 
 * @author Alexander Koller
 *
 */
public class ChartSolver {
    private DomGraph graph;
    private Chart chart;
    private Set<String> roots;
    private SplitSource splitSource;
    
    /**
     * A constructor which uses a {@link CompleteSplitSource} as
     * the split source. 
     * 
     * @param graph
     * @param chart
     */
    public ChartSolver(DomGraph graph, Chart chart) {
        this(graph, chart, new CompleteSplitSource(graph));
    }

    /**
     * A constructor which allows you to specify a customised
     * <code>SplitSource</code>. 
     * 
     * @param graph
     * @param chart
     * @param splitSource
     */
    public ChartSolver(DomGraph graph, Chart chart, SplitSource splitSource) {
        this.splitSource = splitSource;
        this.graph = graph;
        this.chart = chart;
        
        // ASSUMPTION graph is compact and weakly normal
        assert graph.isCompact();
        assert graph.isWeaklyNormal();
        
        roots = graph.getAllRoots();
    }


    /**
     * Solves the graph. This fills the chart which was specified
     * in the constructor call.  
     * 
     * @return true iff the graph is solvable
     */
    public boolean solve() {
        List<Set<String>> wccs = graph.wccs();
      
        if( !graph.isWellFormed() ) {
            return false;
        }

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
        splits = splitSource.computeSplits(subgraph);
        
        // if there are none (i.e. there are no free roots),
        // then the original graph is unsolvable
        if( !splits.hasNext() ) {
            //System.err.println(" - unsolvable (no splits)");
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
}
