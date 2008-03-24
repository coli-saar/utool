/*
 * @(#)RedundancyEliminationSplitSource.java created 10.02.2006
 *
 * Copyright (c) 2006 Alexander Koller
 *
 */

package de.saar.chorus.domgraph.equivalence;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.saar.chorus.domgraph.chart.GraphBasedNonterminal;
import de.saar.chorus.domgraph.chart.RegularTreeGrammar;
import de.saar.chorus.domgraph.chart.Split;
import de.saar.chorus.domgraph.chart.SplitComputer;
import de.saar.chorus.domgraph.chart.SplitSource;
import de.saar.chorus.domgraph.graph.DomGraph;

/**
 * An implementation of <code>SplitSource</code> for filling a chart
 * with just the irredundant splits. Redundancy is defined by an object
 * of a subclass of {@link de.saar.chorus.domgraph.equivalence.RedundancyElimination}.
 *
 * @author Alexander Koller
 *
 */
public class RedundancyEliminationSplitSource<E extends GraphBasedNonterminal> extends SplitSource<E> {
    private final RedundancyElimination<E> elim;
    protected SplitComputer<E> sc;

    public RedundancyEliminationSplitSource(RedundancyElimination<E> elim, DomGraph graph) {
        super(graph);
        this.elim = elim;
        sc = elim.provideSplitComputer(graph);
    }

    @Override
    protected Iterator<Split<E>> computeSplits(E subgraph) {
        List<Split<E>> splits = new ArrayList<Split<E>>();
        List<String> potentialFreeRoots = computePotentialFreeRoots(subgraph);

        for( String root : potentialFreeRoots ) {
            Split<E> split = sc.computeSplit(root, subgraph);

            if( split != null ) {
                splits.add(split);
            }
        }

        return elim.getIrredundantSplits(subgraph, splits).iterator();
    }

    @Override
    public E makeToplevelSubgraph(Set<String> graph) {
        return elim.makeToplevelSubgraph(graph);
    }

    @Override
    public void reduceIfNecessary(RegularTreeGrammar<E> chart) {
        if( elim.requiresReduce() ) {
            chart.reduce();
        }
    }

}
