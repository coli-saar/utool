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
 * The solver successively computes the splits corresponding
 * to the free fragments of the subgraphs of the dominance graph, and
 * adds them to the chart.  It assumes that the input dominance graph has
 * only dominance edges into roots or from roots to holes; this can be
 * achieved e.g. by calling the {@link DomGraph#preprocess()} method first.<p>
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
public class ChartSolver<E extends Nonterminal> {
    private final DomGraph graph;
    private final RegularTreeGrammar<E> chart;
    private final Set<String> roots;
    private final SplitSource<E> splitSource;



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
     * only restriction is that the graph must not contain empty fragments.  However,
     * the solver makes certain assumptions about the form of the dominance edges that
     * can be achieved by calling {@link DomGraph#preprocess()} on it first.
     *
     * @param graph an arbitrary dominance graph
     * @param chart a chart which will be filled with the splits of this graph
     * @param splitsource a split source
     * @return true if the graph is solvable, false otherwise
     */
    public static <E extends Nonterminal> boolean solve(DomGraph graph, RegularTreeGrammar<E> chart, SplitSource<E> splitsource) throws SolverNotApplicableException {
    	DomGraph preprocessed = graph;
    	boolean isSolvable;

    	if( obviouslyUnsolvable(preprocessed) ) {
    		return false;
    	}

    	checkApplicability(graph);

    	// Otherwise, solve the preprocessed graph.
        ChartSolver<E> solver = new ChartSolver<E>(preprocessed, chart, splitsource);
        isSolvable = solver.solve();

        if( isSolvable ) {
            splitsource.reduceIfNecessary(chart);
        } else {
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
    private ChartSolver(DomGraph graph, RegularTreeGrammar<E> chart, SplitSource<E> splitSource) {
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
            E sub = splitSource.makeToplevelSubgraph(wcc);
            chart.addToplevelSubgraph(sub);

            if( !solve(sub) ) {
                return false;
            }
        }
        return true;
    }


    private boolean solve(E subgraph) {
        Iterator<Split<E>> splits;
        int numRootsInSubgraph;

        // If fragset is already in chart, nothings needs to be done.
        if( chart.containsSplitFor(subgraph) ) {
            return true;
        }

        // If fs is singleton and its root is free, it is in solved form.
        // The fs will be entered into the chart as part of the parent's split.
        // NB: Even in a compact graph, there may be fragments with >1 node!
        numRootsInSubgraph = 0;
        for( String node : subgraph.getNodes() ) {
            if( roots.contains(node) ) {
                numRootsInSubgraph++;
            }
        }

        if( numRootsInSubgraph == 1 ) {
            return true;
        }

        // get splits for this subgraph
        try {
            splits = splitSource.computeSplits(subgraph);
        } catch( UnsolvableSubgraphException e ) {
            // if the subgraph is unsolvable (because there are no free roots),
            // then the original graph is unsolvable
            return false;
        }

        while( splits.hasNext() ) {
            Split<E> split = splits.next();

            // iterate over wccs
            for( E wcc : split.getAllSubgraphs() ) {
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
