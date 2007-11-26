/*
 * @(#)CompleteSplitSource.java created 03.02.2006
 *
 * Copyright (c) 2006 Alexander Koller
 *
 */

package de.saar.chorus.domgraph.chart;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.saar.chorus.domgraph.graph.DomGraph;

/**
 * A split source which computes the complete list of
 * splits for a subgraph. A <code>ChartSolver</code> which uses
 * an object of this class as its split source will compute
 * the set of all solved forms of the dominance graph.
 *
 * @author Alexander Koller
 *
 */
public class CompleteSplitSource extends SplitSource {
    public CompleteSplitSource(DomGraph graph) {
        super(graph);
    }

    @Override
    protected Iterator<Split<SubgraphNonterminal>> computeSplits(SubgraphNonterminal subgraph) {
        SplitComputer sc = new SplitComputer(graph);
        List<Split<SubgraphNonterminal>> splits = new ArrayList<Split<SubgraphNonterminal>>();
        List<String> potentialFreeRoots = computePotentialFreeRoots(subgraph);

        for( String root : potentialFreeRoots ) {
            Split<SubgraphNonterminal> split = sc.computeSplit(root, subgraph);

            if( split != null ) {
                // if the root was free, then add the split
                splits.add(split);
            }
        }

        return splits.iterator();
    }

}
