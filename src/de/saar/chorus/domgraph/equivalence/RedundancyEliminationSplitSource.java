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

import de.saar.chorus.domgraph.chart.Split;
import de.saar.chorus.domgraph.chart.SplitComputer;
import de.saar.chorus.domgraph.chart.SplitSource;
import de.saar.chorus.domgraph.chart.SubgraphNonterminal;
import de.saar.chorus.domgraph.graph.DomGraph;

/**
 * An implementation of <code>SplitSource</code> for filling a chart
 * with just the irredundant splits. Redundancy is defined by an object
 * of a subclass of {@link de.saar.chorus.domgraph.equivalence.RedundancyElimination}.
 *
 * @author Alexander Koller
 *
 */
public class RedundancyEliminationSplitSource extends SplitSource {
    private final RedundancyElimination elim;

    public RedundancyEliminationSplitSource(RedundancyElimination elim, DomGraph graph) {
        super(graph);
        this.elim = elim;
    }

    @Override
    protected Iterator<Split<SubgraphNonterminal>> computeSplits(SubgraphNonterminal subgraph) {
        SplitComputer sc = new SplitComputer(graph);
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

}
