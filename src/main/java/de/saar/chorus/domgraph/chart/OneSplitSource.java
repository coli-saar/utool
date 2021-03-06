/*
 * @(#)OneSplitSource.java created 13.02.2006
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
 * A split source which only computes the first split for each
 * subgraph. A <code>ChartSolver</code> which uses a split source
 * of this class will not compute the complete set of solved forms.
 * However, it is still guaranteed to detect whether a graph is
 * unsolvable, and will be considerably faster than a solver that
 * uses a <code>CompleteSplitSource</code>.
 *
 * @author Alexander Koller
 *
 */
public class OneSplitSource extends SplitSource<SubgraphNonterminal> {

    public OneSplitSource(DomGraph graph) {
        super(graph);
    }

    @Override
    public Iterator<Split<SubgraphNonterminal>> computeSplits(SubgraphNonterminal subgraph) throws UnsolvableSubgraphException {
        SplitComputer<SubgraphNonterminal> sc = new SubgraphSplitComputer(graph);
        List<Split<SubgraphNonterminal>> ret = new ArrayList<Split<SubgraphNonterminal>>();
        List<String> potentialFreeRoots = computePotentialFreeRoots(subgraph);

        for( String root : potentialFreeRoots ) {
            Split<SubgraphNonterminal> split = sc.computeSplit(root, subgraph);

            if( split != null ) {
                ret.add(split);
                return ret.iterator();
            }
        }

        if( ret.isEmpty() ) {
            throw new UnsolvableSubgraphException();
        }

        return ret.iterator();
    }

    public static boolean isGraphSolvable(DomGraph graph) throws SolverNotApplicableException {
        Chart chart = new Chart(null);

        return ChartSolver.solve(graph, chart, new OneSplitSource(graph));
    }

    @Override
    public SubgraphNonterminal makeToplevelSubgraph(Set<String> graph) {
        return new SubgraphNonterminal(graph);
    }

    @Override
    public void reduceIfNecessary(ConcreteRegularTreeGrammar<SubgraphNonterminal> chart) {
    }
}
