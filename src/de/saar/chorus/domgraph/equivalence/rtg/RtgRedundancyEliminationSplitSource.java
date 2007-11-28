package de.saar.chorus.domgraph.equivalence.rtg;

import java.util.Iterator;
import java.util.Set;

import de.saar.chorus.domgraph.chart.Split;
import de.saar.chorus.domgraph.chart.SplitSource;
import de.saar.chorus.domgraph.chart.SubgraphNonterminal;
import de.saar.chorus.domgraph.graph.DomGraph;

public class RtgRedundancyEliminationSplitSource extends SplitSource<QuantifierMarkedNonterminal> {
    public RtgRedundancyEliminationSplitSource(DomGraph graph) {
        super(graph);
    }

    @Override
    public QuantifierMarkedNonterminal makeToplevelSubgraph(Set<String> graph) {
        return new QuantifierMarkedNonterminal(new SubgraphNonterminal(graph), null);
    }

    @Override
    protected Iterator<Split<QuantifierMarkedNonterminal>> computeSplits(QuantifierMarkedNonterminal subgraph) {
        // TODO Auto-generated method stub
        return null;
    }

}
