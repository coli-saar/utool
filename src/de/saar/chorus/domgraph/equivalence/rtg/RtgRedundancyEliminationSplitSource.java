package de.saar.chorus.domgraph.equivalence.rtg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
    private final Map<SubgraphNonterminal,Set<Split<QuantifierMarkedNonterminal>>> unrealizedSplits;

    public RtgRedundancyEliminationSplitSource(RtgRedundancyElimination elim, DomGraph graph) {
        super(graph);

        this.elim = elim;
        unrealizedSplits = new HashMap<SubgraphNonterminal,Set<Split<QuantifierMarkedNonterminal>>>();

    }

    @Override
    public QuantifierMarkedNonterminal makeToplevelSubgraph(Set<String> graph) {
        return new QuantifierMarkedNonterminal(new SubgraphNonterminal(graph), null);
    }

    @Override
    protected Iterator<Split<QuantifierMarkedNonterminal>> computeSplits(QuantifierMarkedNonterminal subgraph) throws UnsolvableSubgraphException {
        /*
        if( unrealizedSplits.containsKey(subgraph.getSubgraph())) {
            List<Split<QuantifierMarkedNonterminal>> ret = new ArrayList<Split<QuantifierMarkedNonterminal>>(unrealizedSplits.get(subgraph.getSubgraph()));

            // compute allowed unrealized splits
            for( int i = 0; i < ret.size(); ) {
                if( elim.allowedSplit(ret.get(i), subgraph.getPreviousQuantifier()) ) {
                    i++;
                } else {
                    ret.remove(i);
                }
            }

            // record allowed splits as now realized
            unrealizedSplits.get(subgraph.getSubgraph()).removeAll(ret);

            return ret.iterator();
        } else {
       */
            SplitComputer<QuantifierMarkedNonterminal> sc = new QuantifierMarkedNonterminalSplitComputer(graph);
            List<Split<QuantifierMarkedNonterminal>> splits = new ArrayList<Split<QuantifierMarkedNonterminal>>();
            List<String> potentialFreeRoots = computePotentialFreeRoots(subgraph);
            boolean subgraphIsSolvable = false;
            Set<Split<QuantifierMarkedNonterminal>> hereUnrealizedSplits = new HashSet<Split<QuantifierMarkedNonterminal>>();

            unrealizedSplits.put(subgraph.getSubgraph(), hereUnrealizedSplits);

            for( String root : potentialFreeRoots ) {
                Split<QuantifierMarkedNonterminal> split = sc.computeSplit(root, subgraph);

                if( split != null ) {
                    subgraphIsSolvable = true;

                    if( elim.allowedSplit(split, subgraph.getPreviousQuantifier()) ) {
                        splits.add(split);
                    } else {
                        hereUnrealizedSplits.add(split);
                    }
                }
            }

            if( !subgraphIsSolvable ) {
                throw new UnsolvableSubgraphException();
            }

            return splits.iterator();
       // }
    }

    @Override
    public void reduceIfNecessary(RegularTreeGrammar<QuantifierMarkedNonterminal> chart) {
        chart.reduce(graph.getAllRoots());
    }

}
