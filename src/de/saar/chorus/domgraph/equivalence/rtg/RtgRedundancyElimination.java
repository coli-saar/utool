package de.saar.chorus.domgraph.equivalence.rtg;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import de.saar.chorus.domgraph.chart.Chart;
import de.saar.chorus.domgraph.chart.GraphBasedNonterminal;
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
    private final Set<String> wildcardLabeledNodes;

    public RtgRedundancyElimination(DomGraph graph, NodeLabels labels, EquationSystem eqs) {
        super(graph, labels, eqs);

        agenda = new LinkedList<QuantifierMarkedNonterminal>();
        roots = graph.getAllRoots();

        wildcardLabeledNodes = new HashSet<String>();
        for( String node : roots ) {
            if( eqs.isWildcardLabel(labels.getLabel(node)) ) {
                wildcardLabeledNodes.add(node);
            }
        }
    }

    public void eliminate(Chart c, RegularTreeGrammar<QuantifierMarkedNonterminal> out) {
        out.clear();
        agenda.clear();

        //possibleDominators = c.computePossibleDominators();

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
                        	agenda.add(candidate);
                        }
                    }
                }
            }
        }

        /*
        System.err.println("#sfs after phase 1: " + out.countSolvedForms());
        System.err.println("chart after phase 1: ");
        System.err.println(ChartPresenter.chartOnlyRoots(out, graph));


        //System.err.println("Elimination done, new chart size is " + out.size());

         */
        out.reduce();

        /*
        System.err.println("#sfs after phase 2: " + out.countSolvedForms());
        System.err.println(ChartPresenter.chartOnlyRoots(out, graph));
        System.err.println("--------------------------------------------------------------------------------\n\n");
        */

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
    public boolean allowedSplit(Split split, String previousQuantifier) {
        //System.err.println("Consider " + split + " below " + previousQuantifier + ": ");


        // if there was no previous quantifier, all splits are allowed
        if( previousQuantifier == null ) {
            //System.err.print("[pq=null]");
            return true;
        }

        // if the remaining split contains a wildcard that is larger than pQ, then the split is not allowed
        for( Object o : split.getAllSubgraphs() ) {
            GraphBasedNonterminal subgraph = (GraphBasedNonterminal) o;

            for( String node : subgraph.getNodes() ) {
                if( wildcardLabeledNodes.contains(node) && node.compareTo(previousQuantifier) < 0 ) {
                    //System.err.println("Not allowed (larger wildcard)");
                    return false;
                }
            }
        }

        // if the two quantifiers are in the right order (previous < here), then the split is allowed
        if( previousQuantifier.compareTo(split.getRootFragment()) < 0 ) {
            //System.err.print("[pq smaller]");
            return true;
        }

        // if the two quantifiers are permutable, then the split is not allowed
        //System.err.print("[" + previousQuantifier + "," + split.getRootFragment() +
           //     (isPermutable(previousQuantifier, split.getRootFragment()) ? "" : " not") +
              //  " permutable]");
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
