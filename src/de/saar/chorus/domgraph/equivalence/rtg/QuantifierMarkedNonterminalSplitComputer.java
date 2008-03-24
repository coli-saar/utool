package de.saar.chorus.domgraph.equivalence.rtg;

import de.saar.chorus.domgraph.chart.SplitComputer;
import de.saar.chorus.domgraph.chart.SubgraphNonterminal;
import de.saar.chorus.domgraph.graph.DomGraph;

public class QuantifierMarkedNonterminalSplitComputer extends SplitComputer<QuantifierMarkedNonterminal> {

    public QuantifierMarkedNonterminalSplitComputer(DomGraph graph) {
        super(graph);
    }

    @Override
    protected QuantifierMarkedNonterminal createEmptyNonterminal() {
        return new QuantifierMarkedNonterminal(new SubgraphNonterminal(), theRoot);
    }

}
