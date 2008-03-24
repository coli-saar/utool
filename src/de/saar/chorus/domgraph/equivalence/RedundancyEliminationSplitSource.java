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

import de.saar.chorus.domgraph.chart.RegularTreeGrammar;
import de.saar.chorus.domgraph.chart.Split;
import de.saar.chorus.domgraph.chart.SplitComputer;
import de.saar.chorus.domgraph.chart.SplitSource;
import de.saar.chorus.domgraph.chart.SubgraphNonterminal;
import de.saar.chorus.domgraph.chart.SubgraphSplitComputer;
import de.saar.chorus.domgraph.graph.DomGraph;

/**
 * An implementation of <code>SplitSource</code> for filling a chart
 * with just the irredundant splits. Redundancy is defined by an object
 * of a subclass of {@link de.saar.chorus.domgraph.equivalence.RedundancyElimination}.
 *
 * @author Alexander Koller
 *
 */
public class RedundancyEliminationSplitSource extends SplitSource<SubgraphNonterminal> {
    private final RedundancyElimination<SubgraphNonterminal> elim;
    protected SplitComputer<SubgraphNonterminal> sc;

    public RedundancyEliminationSplitSource(RedundancyElimination<SubgraphNonterminal> elim, DomGraph graph) {
        super(graph);
        this.elim = elim;
        sc = new SubgraphSplitComputer(graph);
    }

    @Override
    protected Iterator<Split<SubgraphNonterminal>> computeSplits(SubgraphNonterminal subgraph) {

        List<Split<SubgraphNonterminal>> splits = new ArrayList<Split<SubgraphNonterminal>>();
        List<String> potentialFreeRoots = computePotentialFreeRoots(subgraph);

        for( String root : potentialFreeRoots ) {
            Split<SubgraphNonterminal> split = sc.computeSplit(root, subgraph);

            if( split != null ) {
                splits.add(split);
            }
        }

        return elim.getIrredundantSplits(subgraph, splits).iterator();
    }

    @Override
    public SubgraphNonterminal makeToplevelSubgraph(Set<String> graph) {
        return new SubgraphNonterminal(graph);
    }

    @Override
    public void reduceIfNecessary(RegularTreeGrammar<SubgraphNonterminal> chart) {
    }

}
