/*
 * @(#)SplitSource.java created 03.02.2006
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
 * An abstract superclass for classes that compute splits. A {@link ChartSolver}
 * relies on an object of a subclass of this class to provide the splits
 * for a subgraph. You can provide your own subclass by implementing
 * the <code>computeSplits</code> method of this class.
 *
 * @author Alexander Koller
 *
 */
public abstract class SplitSource<E extends GraphBasedNonterminal> {
    protected DomGraph graph;

    public SplitSource(DomGraph graph) {
        this.graph = graph;
    }

    abstract public E makeToplevelSubgraph(Set<String> graph);

    /**
     * Implement this abstract method when you write your own
     * subclass of <code>SplitSource</code>. The method gets a subgraph
     * as its argument, and has the task of computing an iterator
     * over the splits of this subgraph.<p>
     *
     * @param subgraph a subgraph
     * @return an iterator over some or all splits of this subgraph
     */
    abstract protected Iterator<Split<E>> computeSplits(E subgraph) throws UnsolvableSubgraphException;

    /**
     * Reduces the computed RTG. If your split source is such that the RTG it computes
     * is not automatically reduced, you should implement this method to call reduce()
     * on the output RTG.  This method is called as the last operation by the chart solver.
     *
     * @param chart the output RTG computed by the chart solver
     */
    abstract public void reduceIfNecessary(RegularTreeGrammar<E> chart);

    /**
     * Computes the list of all nodes in the subgraphs which have no
     * incoming edges. These nodes are candidates for being free roots;
     * however, you still need to check that the holes are in different
     * biconnected components.
     *
     * @param subgraph a subgraph
     * @return the list of nodes without in-edges in the subgraph
     */
    protected List<String> computePotentialFreeRoots(E subgraph) {
        // initialise potentialFreeRoots with all nodes without
        // incoming dom-edges
        List<String> potentialFreeRoots = new ArrayList<String>();
        for( String node : subgraph.getNodes() ) {
            if( graph.indegOfSubgraph(node, null, subgraph.getNodes()) == 0 ) {
                potentialFreeRoots.add(node);
            }
        }

        return potentialFreeRoots;
    }
}
