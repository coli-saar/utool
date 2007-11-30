package de.saar.chorus.domgraph.equivalence.rtg;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.saar.chorus.domgraph.chart.RegularTreeGrammar;
import de.saar.chorus.domgraph.chart.Split;
import de.saar.chorus.domgraph.chart.SplitComputer;
import de.saar.chorus.domgraph.chart.SplitSource;
import de.saar.chorus.domgraph.chart.SubgraphNonterminal;
import de.saar.chorus.domgraph.chart.UnsolvableSubgraphException;
import de.saar.chorus.domgraph.graph.DomGraph;

public class RtgRedundancyEliminationSplitSource extends SplitSource<QuantifierMarkedNonterminal> {
    private final RtgRedundancyElimination elim;

    public RtgRedundancyEliminationSplitSource(RtgRedundancyElimination elim, DomGraph graph) {
        super(graph);
        this.elim = elim;
    }

    @Override
    public QuantifierMarkedNonterminal makeToplevelSubgraph(Set<String> graph) {
        return new QuantifierMarkedNonterminal(new SubgraphNonterminal(graph), null);
    }

    @Override
    protected Iterator<Split<QuantifierMarkedNonterminal>> computeSplits(QuantifierMarkedNonterminal subgraph) throws UnsolvableSubgraphException {
        SplitComputer<QuantifierMarkedNonterminal> sc = new QuantifierMarkedNonterminalSplitComputer(graph);
        List<Split<QuantifierMarkedNonterminal>> splits = new ArrayList<Split<QuantifierMarkedNonterminal>>();
        List<String> potentialFreeRoots = computePotentialFreeRoots(subgraph);
        boolean subgraphIsSolvable = false;

        for( String root : potentialFreeRoots ) {
            Split<QuantifierMarkedNonterminal> split = sc.computeSplit(root, subgraph);

            if( split != null ) {
                subgraphIsSolvable = true;

                if( elim.allowedSplit(split, subgraph.getPreviousQuantifier()) ) {
                    splits.add(split);
                }
            }
        }

        if( !subgraphIsSolvable ) {
            throw new UnsolvableSubgraphException();
        }

        return splits.iterator();
    }

    @Override
    public void reduceIfNecessary(RegularTreeGrammar<QuantifierMarkedNonterminal> chart) {
        long start = System.currentTimeMillis();
        chart.reduce(graph.getAllRoots());
        long end = System.currentTimeMillis();
    }

}
