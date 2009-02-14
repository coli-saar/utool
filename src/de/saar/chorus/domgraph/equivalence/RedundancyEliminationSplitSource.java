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

import de.saar.chorus.domgraph.chart.ConcreteRegularTreeGrammar;
import de.saar.chorus.domgraph.chart.Split;
import de.saar.chorus.domgraph.chart.SplitComputer;
import de.saar.chorus.domgraph.chart.SplitSource;
import de.saar.chorus.domgraph.chart.UnsolvableSubgraphException;
import de.saar.chorus.domgraph.equivalence.rtg.QuantifierMarkedNonterminal;
import de.saar.chorus.domgraph.equivalence.rtg.RtgRedundancyElimination;
import de.saar.chorus.domgraph.graph.DomGraph;

/**
 * An implementation of <code>SplitSource</code> for filling a chart
 * with just the irredundant splits. Redundancy is defined by an object
 * of a subclass of {@link de.saar.chorus.domgraph.equivalence.RedundancyElimination}.
 *
 * @author Alexander Koller
 *
 */
public class RedundancyEliminationSplitSource extends SplitSource<QuantifierMarkedNonterminal> {
    private final RtgRedundancyElimination elim;
    protected SplitComputer<QuantifierMarkedNonterminal> sc;

    public RedundancyEliminationSplitSource(RtgRedundancyElimination elim, DomGraph graph) {
        super(graph);
        this.elim = elim;
        sc = elim.provideSplitComputer(graph);
    }

    @Override
    protected Iterator<Split<QuantifierMarkedNonterminal>> computeSplits(QuantifierMarkedNonterminal subgraph) throws UnsolvableSubgraphException {
        List<Split<QuantifierMarkedNonterminal>> splits = new ArrayList<Split<QuantifierMarkedNonterminal>>();
        List<String> potentialFreeRoots = computePotentialFreeRoots(subgraph);

        for( String root : potentialFreeRoots ) {
            Split<QuantifierMarkedNonterminal> split = sc.computeSplit(root, subgraph);

            if( split != null ) {
                splits.add(split);
            }
        }

        if( splits.isEmpty() ) {
            throw new UnsolvableSubgraphException();
        }

        return elim.getIrredundantSplits(subgraph, splits).iterator();
    }

    @Override
    public QuantifierMarkedNonterminal makeToplevelSubgraph(Set<String> graph) {
        return elim.makeToplevelSubgraph(graph);
    }

    @Override
    public void reduceIfNecessary(ConcreteRegularTreeGrammar<QuantifierMarkedNonterminal> chart) {
        if( elim.requiresReduce() ) {
            chart.reduce();
        }
    }

}
