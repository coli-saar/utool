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
import de.saar.chorus.domgraph.graph.EdgeType;



/**
 * A solver for compact weakly normal dominance graphs. This solver computes
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
     * Solves the given dominance graph using a specific split source. 
     * This method will determine whether the given dominance graph is
     * solvable or not. It will also fill the given chart with the splits
     * that are necessary to later enumerate solved forms of the
     * dominance graph. It will use the given split source in order to
     * compute the splits for each subgraph. <p>
     * 
     * The solver throws an <code>SolverNotApplicableException</code> if the dominance
     * graph doesn't belong to a fragment that the solver understands.  Currently the
     * only restriction is that the graph must not contain empty fragments.
     * 
     * @param graph an arbitrary dominance graph
     * @param chart a chart which will be filled with the splits of this graph
     * @param splitsource a split source
     * @return true if the graph is solvable, false otherwise
     */
    public static boolean solve(DomGraph graph, Chart chart, SplitSource splitsource) throws SolverNotApplicableException {
    	DomGraph preprocessed = null;
    	boolean isSolvable;
    	
    	// Try to preprocess the graph. If this fails (i.e. preprocess() throws
    	// an exception), the graph is trivially unsolvable and we can return immediately.
    	try {
    		preprocessed = graph.preprocess();
    	} catch(Exception e) {
    		return false;
    	}
    	
    	if( obviouslyUnsolvable(preprocessed) ) {
    		return false;
    	}
    	
    	checkApplicability(graph);
    	
    	// Otherwise, solve the preprocessed graph.
        ChartSolver solver = new ChartSolver(preprocessed, chart, splitsource);
        isSolvable = solver.solve();
        
        if( ! isSolvable ) {
        	chart.clear();
        }
        
        return isSolvable;
    }
    
    private static void checkApplicability(DomGraph graph) throws SolverNotApplicableException {
    	if( graph.hasEmptyFragments() ) {
    		throw new SolverNotApplicableException("The graph has empty fragments.");
    	}
	}
    

	private static boolean obviouslyUnsolvable(DomGraph graph) {
    	for( String node : graph.getAllNodes() ) {
    		if( graph.indeg(node, EdgeType.TREE) > 1 ) {
    			return true;
    		}
    	}
    	
    	if( ! graph.isWellFormed() ) {
    		return true;
    	}
    	
    	return false;
    }
    

    /**
     * Solves the given dominance graph using a 
     * {@link de.saar.chorus.domgraph.chart.CompleteSplitSource}.
     * This method will create a new <code>CompleteSplitSource</code> object for
     * the graph and then call {@link #solve(DomGraph, Chart, SplitSource)}.
     * @throws SolverNotApplicableException 
     * 
     * @see #solve(DomGraph, Chart, SplitSource)
     */
    public static boolean solve(DomGraph graph, Chart chart) throws SolverNotApplicableException {
        return solve(graph, chart, new CompleteSplitSource(graph));
    }
    


    /**
     * A constructor which allows you to specify a customised
     * <code>SplitSource</code>. 
     * 
     * @param graph
     * @param chart
     * @param splitSource
     */
    private ChartSolver(DomGraph graph, Chart chart, SplitSource splitSource) {
        this.splitSource = splitSource;
        this.graph = graph;
        this.chart = chart;
        
        // deleted the assumption that graph is weakly normal and compact
        
        roots = graph.getAllRoots();
    }


    /**
     * Solves the graph. This fills the chart which was specified
     * in the constructor call.  
     * 
     * @return true iff the graph is solvable
     */
    private boolean solve() {
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
    
    

   
        /*
         * UNIT TESTS:
         * 
         * Test chart size and correct readings for:
         * - chain of length 3
         * - thatwould.clls (dauert einige Sekunden)
         * - not-hnc.clls
         * - something unsolvable
         */
}
