package de.saar.chorus.domgraph.equivalence.rtg;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import de.saar.chorus.domgraph.chart.Chart;
import de.saar.chorus.domgraph.chart.RegularTreeGrammar;
import de.saar.chorus.domgraph.chart.Split;
import de.saar.chorus.domgraph.chart.SubgraphNonterminal;
import de.saar.chorus.domgraph.equivalence.EquationSystem;
import de.saar.chorus.domgraph.equivalence.RedundancyElimination;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;

public class RtgRedundancyElimination extends RedundancyElimination<QuantifierMarkedNonterminal> {
    private final Queue<QuantifierMarkedNonterminal> agenda;
    private final Set<String> roots;

    public RtgRedundancyElimination(DomGraph graph, NodeLabels labels, EquationSystem eqs) {
        super(graph, labels, eqs);

        agenda = new LinkedList<QuantifierMarkedNonterminal>();
        roots = graph.getAllRoots();
    }

    public void eliminate(Chart c, RegularTreeGrammar<QuantifierMarkedNonterminal> out) {
        out.clear();
        agenda.clear();

        //System.err.println("Elimination starts...");

        for( SubgraphNonterminal sub : c.getToplevelSubgraphs() ) {
            QuantifierMarkedNonterminal nt = new QuantifierMarkedNonterminal(sub, null);
            agenda.add(nt);
            out.addToplevelSubgraph(nt);
        }

        while( !agenda.isEmpty() ) {
            QuantifierMarkedNonterminal sub = agenda.remove();

            if( !out.containsSplitFor(sub) ) {
                for( Split<SubgraphNonterminal> split : c.getSplitsFor(sub.getSubgraph()) ) {
                    if( allowedSplit(split, sub.getPreviousQuantifier()) ) {
                        Split<QuantifierMarkedNonterminal> outSplit = makeSplit(split);
                        out.addSplit(sub, outSplit);

                        for( QuantifierMarkedNonterminal candidate : outSplit.getAllSubgraphs() ) {
                            if( !candidate.isSingleton(roots) && !out.containsSplitFor(candidate)) {
                                agenda.add(candidate);
                            }
                        }
                    }
                }
            }
        }

        //System.err.println("Elimination done, new chart size is " + out.size());

        out.reduce(roots);

        //System.err.println("Reduction done, new chart size is " + out.size());

    }





    private Split<QuantifierMarkedNonterminal> makeSplit(Split<SubgraphNonterminal> split) {
        String root = split.getRootFragment();
        Split<QuantifierMarkedNonterminal> ret = new Split<QuantifierMarkedNonterminal>(root);

        for( String dominator : split.getAllDominators() ) {
            for( SubgraphNonterminal wcc : split.getWccs(dominator)) {
                ret.addWcc(dominator, new QuantifierMarkedNonterminal(wcc, root));
            }
        }

        return ret;
    }

    @SuppressWarnings("unchecked")
    private boolean allowedSplit(Split split, String previousQuantifier) {
        // if there was no previous quantifier, all splits are allowed
        if( previousQuantifier == null ) {
            return true;
        }

        // if the two quantifiers are in the right order (previous < here), then the split is allowed
        if( previousQuantifier.compareTo(split.getRootFragment()) < 0 ) {
            return true;
        }

        // if the two quantifiers are permutable, then the split is not allowed
        return !isPermutable(previousQuantifier, split.getRootFragment());
    }

    @Override
    public List<Split<QuantifierMarkedNonterminal>> getIrredundantSplits(QuantifierMarkedNonterminal subgraph, List<Split<QuantifierMarkedNonterminal>> allSplits) {
        List<Split<QuantifierMarkedNonterminal>> ret = new ArrayList<Split<QuantifierMarkedNonterminal>>();

        for( Split<QuantifierMarkedNonterminal> candidate : allSplits ) {
            if( allowedSplit(candidate, subgraph.getPreviousQuantifier()) ) {
                ret.add(candidate);
            }
        }

        return ret;
    }

}
