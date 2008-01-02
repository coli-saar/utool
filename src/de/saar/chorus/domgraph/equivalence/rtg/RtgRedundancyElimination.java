package de.saar.chorus.domgraph.equivalence.rtg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import de.saar.chorus.domgraph.chart.Chart;
import de.saar.chorus.domgraph.chart.RegularTreeGrammar;
import de.saar.chorus.domgraph.chart.Split;
import de.saar.chorus.domgraph.chart.SubgraphNonterminal;
import de.saar.chorus.domgraph.equivalence.EquationSystem;
import de.saar.chorus.domgraph.equivalence.RedundancyElimination;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.EdgeType;
import de.saar.chorus.domgraph.graph.NodeLabels;

public class RtgRedundancyElimination extends RedundancyElimination<QuantifierMarkedNonterminal> {
    private final static boolean DEBUG = false;

    private final Queue<QuantifierMarkedNonterminal> agenda;
    private final Set<String> roots;
    private final Map<String,List<Integer>> wildcardLabeledNodes;

    public RtgRedundancyElimination(DomGraph graph, NodeLabels labels, EquationSystem eqs) {
        super(graph, labels, eqs);

        agenda = new LinkedList<QuantifierMarkedNonterminal>();
        roots = graph.getAllRoots();

        wildcardLabeledNodes = new HashMap<String,List<Integer>>();
        for( String node : roots ) {
            if( eqs.isWildcardLabel(labels.getLabel(node)) ) {
                List<Integer> holeIndices = new ArrayList<Integer>();
                wildcardLabeledNodes.put(node, holeIndices);

                for( int i = 0; i < compact.outdeg(node,EdgeType.TREE); i++ ) {
                    if( eqs.isWildcard(labels.getLabel(node), i) ) {
                        holeIndices.add(i);
                    }
                }
            }
        }
    }


    private void addSplitAndSubgraphs(QuantifierMarkedNonterminal sub, Split<QuantifierMarkedNonterminal> split, RegularTreeGrammar<QuantifierMarkedNonterminal> out) {
        out.addSplit(sub, split);
        if( DEBUG ) {
            System.err.println("add split: " + split + " for " + sub);
        }

        for( QuantifierMarkedNonterminal candidate : split.getAllSubgraphs() ) {
            agenda.add(candidate);
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
                List<Split<SubgraphNonterminal>> splits = c.getSplitsFor(sub.getSubgraph());
                Set<String> roots = new HashSet<String>();

                for( Split<SubgraphNonterminal> split : splits ) {
                    roots.add(split.getRootFragment());
                }

                List<String> permutingWildcards = getPermutingWildcards(sub, splits, roots);

                // if the subgraph contains permuting wildcards, allow only the split
                // with the smallest p.w. at the root
                if( !permutingWildcards.isEmpty() ) {
                    //System.err.println("Subgraph " + sub + " has a permuting wildcard: allow only " + permutingWildcards.get(0));

                    for( Split<SubgraphNonterminal> split : splits ) {
                        if( split.getRootFragment().equals(permutingWildcards.get(0))) {
                            addSplitAndSubgraphs(sub, makeSplit(split), out);
                            break;
                        }
                    }
                } else {
                    for( Split<SubgraphNonterminal> split : splits ) {
                        if( allowedSplit(split, sub.getPreviousQuantifier(), roots) ) {
                            addSplitAndSubgraphs(sub, makeSplit(split), out);
                        } else {
                            if(DEBUG) {
                                System.err.println("Disallowed split: " + split + " (from " + sub.getPreviousQuantifier() + ")");
                            }
                        }
                    }
                }
            }
        }


        /*
        System.err.println("#sfs after phase 1: " + out.countSolvedForms());


        System.err.println("chart after phase 1: ");
        System.err.println(ChartPresenter.chartOnlyRoots(out, graph));
*/

        //System.err.println("Elimination done, new chart size is " + out.size());


        out.reduce();


        /*
        System.err.println("#sfs after phase 2: " + out.countSolvedForms());

        System.err.println(ChartPresenter.chartOnlyRoots(out, graph));
        System.err.println("--------------------------------------------------------------------------------\n\n");
*/

        //System.err.println("Reduction done, new chart size is " + out.size());

    }

    private List<String> getPermutingWildcards(QuantifierMarkedNonterminal subgraph, List<Split<SubgraphNonterminal>> splits, Set<String> roots) {
        List<String> ret = new ArrayList<String>();


        for( String node : roots ) {
            if( isPermutingWildcard(node, subgraph, roots) ) {
                ret.add(node);
            }
        }

        // sort result list in ascending order
        Collections.sort(ret);

        return ret;
    }

    private boolean isPermutingWildcard(String node, QuantifierMarkedNonterminal subgraph, Set<String> roots) {
        // only wildcards can be permuting wildcards
        if( !wildcardLabeledNodes.containsKey(node) ) {
            return false;
        }

        // only free fragments can be permuting wildcards
        if( !roots.contains(node)) {
            return false;
        }


        // only fragments that are connected to all their possible dominators
        // by a wildcard hole can be permuting wildcards
        for( String other : roots ) {
            if( !other.equals(node) && isPossibleDominator(other, node) ) {
                int connectingHole = hypernormalReachability.get(node).get(other);

                if( !wildcardLabeledNodes.get(node).contains(connectingHole) ) {
                    return false;
                }
            }
        }

        return true;
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
    public boolean allowedSplit(Split split, String previousQuantifier, Set<String> freeRoots) {
        //System.err.print("Consider " + split + " below " + previousQuantifier + ": ");

        String root = split.getRootFragment();

        /*
        // wildcard-labeled nodes are forbidden if the wcc of the permuting hole
        // contains another free fragment
        if( wildcardLabeledNodes.containsKey(root)) {
            for( int wildcardHoleIndex : wildcardLabeledNodes.get(root) ) {
                String hole = compact.getChildren(root, EdgeType.TREE).get(wildcardHoleIndex);

                for( Object wcc : split.getWccs(hole)) {
                    for( String other : ((GraphBasedNonterminal) wcc).getNodes() ) {
                        if( freeRoots.contains(other) ) {
                            if(DEBUG) {
                                System.err.print(" [wildcard] ");
                            }
                            return false;
                        }
                    }
                }
            }
        }
*/


        // if there was no previous quantifier, all splits are allowed
        if( previousQuantifier == null ) {
            if(DEBUG) {
                System.err.print("[pq=null -> allowed] ");
            }
            return true;
        }

        // if the previous quantifier was a wildcard, then it doesn't restrict the allowed splits
        if( wildcardLabeledNodes.containsKey(previousQuantifier)) {
            if(DEBUG) {
                System.err.print("[pq=wildcard -> allowed]");
            }
            return true;
        }



        // if the two quantifiers are in the right order (previous < here), then the split is allowed
        if( previousQuantifier.compareTo(split.getRootFragment()) < 0 ) {
            if(DEBUG) {
                System.err.println("[pq smaller -> allowed]");
            }
            return true;
        }

        // if the two quantifiers are permutable, then the split is not allowed
        //System.err.print("[" + previousQuantifier + "," + split.getRootFragment() +
           //     (isPermutable(previousQuantifier, split.getRootFragment()) ? "" : " not") +
              //  " permutable]");
        if(DEBUG) {
            System.err.println("[perm: allowed=" + !isPermutable(previousQuantifier, split.getRootFragment()) + "] ");
        }
        return !isPermutable(previousQuantifier, split.getRootFragment());
    }

    @Override
    public List<Split<QuantifierMarkedNonterminal>> getIrredundantSplits(QuantifierMarkedNonterminal subgraph, List<Split<QuantifierMarkedNonterminal>> allSplits) {
        List<Split<QuantifierMarkedNonterminal>> ret = new ArrayList<Split<QuantifierMarkedNonterminal>>();

        for( Split<QuantifierMarkedNonterminal> candidate : allSplits ) {
            // TODO - fix me
            if( allowedSplit(candidate, subgraph.getPreviousQuantifier(), new HashSet<String>()) ) {
                ret.add(candidate);
            }
        }

        return ret;
    }

}
