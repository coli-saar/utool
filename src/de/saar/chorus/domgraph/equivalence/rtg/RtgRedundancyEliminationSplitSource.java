package de.saar.chorus.domgraph.equivalence.rtg;

import java.util.ArrayList;
import java.util.HashMap;
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
    private final Map<SubgraphNonterminal,List<Split<QuantifierMarkedNonterminal>>> precomputedSplits;

    public RtgRedundancyEliminationSplitSource(RtgRedundancyElimination elim, DomGraph graph) {
        super(graph);

        this.elim = elim;
        precomputedSplits = new HashMap<SubgraphNonterminal,List<Split<QuantifierMarkedNonterminal>>>();
    }

    @Override
    public QuantifierMarkedNonterminal makeToplevelSubgraph(Set<String> graph) {
        return new QuantifierMarkedNonterminal(new SubgraphNonterminal(graph), null);
    }

    @Override
    protected Iterator<Split<QuantifierMarkedNonterminal>> computeSplits(QuantifierMarkedNonterminal subgraph) throws UnsolvableSubgraphException {
        if( precomputedSplits.containsKey(subgraph.getSubgraph())) {
            List<Split<QuantifierMarkedNonterminal>> ret = new ArrayList<Split<QuantifierMarkedNonterminal>>();

            /*
            System.err.println("\nSubsequent visit to " + subgraph.getSubgraph() + " (from " + subgraph.getPreviousQuantifier() + ")");
            System.err.println("Stored splits: " + precomputedSplits.get(subgraph.getSubgraph()));
            */

            // TODO: Further optimization: While all allowed splits must be added to the chart,
            // only allowed splits that are allowed for the first time must be further pursued by the solver.
            for( Split<QuantifierMarkedNonterminal> split : precomputedSplits.get(subgraph.getSubgraph())) {
                if( elim.allowedSplit(split, subgraph.getPreviousQuantifier())) {
                    //System.err.println("   -> " + split.toString() + " is allowed");
                    ret.add(split);
                } else {
                    //System.err.println("   -> " + split.toString() + " is not allowed");
                }
            }

            return ret.iterator();
        } else {
            SplitComputer<QuantifierMarkedNonterminal> sc = new QuantifierMarkedNonterminalSplitComputer(graph);
            List<Split<QuantifierMarkedNonterminal>> splits = new ArrayList<Split<QuantifierMarkedNonterminal>>();
            List<String> potentialFreeRoots = computePotentialFreeRoots(subgraph);
            boolean subgraphIsSolvable = false;

            List<Split<QuantifierMarkedNonterminal>> allSplits = new ArrayList<Split<QuantifierMarkedNonterminal>>();
            precomputedSplits.put(subgraph.getSubgraph(), allSplits);

            //System.err.println("\nFirst visit to " + subgraph.getSubgraph() + " (from " + subgraph.getPreviousQuantifier() + ")");

            for( String root : potentialFreeRoots ) {
                Split<QuantifierMarkedNonterminal> split = sc.computeSplit(root, subgraph);

                if( split != null ) {
                    subgraphIsSolvable = true;

                    allSplits.add(split);

                    if( elim.allowedSplit(split, subgraph.getPreviousQuantifier()) ) {
                        //System.err.println("   -> " + split.toString() + " is allowed");
                        splits.add(split);
                    } else {
                        //System.err.println("   -> " + split.toString() + " is not allowed");
                    }
                }
            }

            if( !subgraphIsSolvable ) {
                throw new UnsolvableSubgraphException();
            }

            return splits.iterator();
        }
    }

    @Override
    public void reduceIfNecessary(RegularTreeGrammar<QuantifierMarkedNonterminal> chart) {
        //System.err.println("Chart before reduction:");
        //System.err.println(ChartPresenter.chartOnlyRoots(chart, graph));

        chart.reduce(graph.getAllRoots());
    }

}
