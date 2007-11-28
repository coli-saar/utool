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
import de.saar.chorus.domgraph.equivalence.RedundancyElimination;
import de.saar.chorus.domgraph.graph.DomGraph;

public class RtgRedundancyEliminationSplitSource extends SplitSource<QuantifierMarkedNonterminal> {
    private final RedundancyElimination<QuantifierMarkedNonterminal> elim;

    public RtgRedundancyEliminationSplitSource(RedundancyElimination<QuantifierMarkedNonterminal> elim, DomGraph graph) {
        super(graph);
        this.elim = elim;
    }

    @Override
    public QuantifierMarkedNonterminal makeToplevelSubgraph(Set<String> graph) {
        return new QuantifierMarkedNonterminal(new SubgraphNonterminal(graph), null);
    }

    @Override
    protected Iterator<Split<QuantifierMarkedNonterminal>> computeSplits(QuantifierMarkedNonterminal subgraph) {
        SplitComputer<QuantifierMarkedNonterminal> sc = new QuantifierMarkedNonterminalSplitComputer(graph);
        List<Split<QuantifierMarkedNonterminal>> splits = new ArrayList<Split<QuantifierMarkedNonterminal>>();
        List<String> potentialFreeRoots = computePotentialFreeRoots(subgraph);

        for( String root : potentialFreeRoots ) {
            Split<QuantifierMarkedNonterminal> split = sc.computeSplit(root, subgraph);

            if( split != null ) {
                splits.add(split);
            }
        }

        return elim.getIrredundantSplits(subgraph, splits).iterator();
    }

    @Override
    public void reduceIfNecessary(RegularTreeGrammar<QuantifierMarkedNonterminal> chart) {
        chart.reduce(graph.getAllRoots());
    }

}
