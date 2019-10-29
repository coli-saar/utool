package de.saar.chorus.domgraph.chart;

import de.saar.chorus.domgraph.graph.DomGraph;

public class SubgraphSplitComputer extends SplitComputer<SubgraphNonterminal> {
    public SubgraphSplitComputer(DomGraph graph) {
        super(graph);
    }

    @Override
    protected SubgraphNonterminal createEmptyNonterminal() {
        return new SubgraphNonterminal();
    }

}
