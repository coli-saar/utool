package de.saar.chorus.domgraph.equivalence.rtg;

import java.util.Set;

import de.saar.chorus.domgraph.chart.Nonterminal;
import de.saar.chorus.domgraph.chart.SubgraphNonterminal;

public class QuantifierMarkedNonterminal implements Nonterminal {
    private final SubgraphNonterminal subgraph;
    private final String previousQuantifier;

    private String rootForThisFragset;

    public QuantifierMarkedNonterminal(SubgraphNonterminal subgraph, String previousQuantifier) {
        super();
        this.subgraph = subgraph;
        this.previousQuantifier = previousQuantifier;
    }



    public SubgraphNonterminal getSubgraph() {
        return subgraph;
    }



    public String getPreviousQuantifier() {
        return previousQuantifier;
    }



    @Override
    public boolean isEmpty() {
        return subgraph.isEmpty();
    }

    @Override
    public String getRootIfSingleton() {
        return rootForThisFragset;
    }

    @Override
    public boolean isSingleton(Set<String> roots) {
        int numRoots = 0;


        for( String node : subgraph ) {
            if( roots.contains(node) ) {
                numRoots++;

                if( numRoots > 1 ) {
                    return false;
                }

                rootForThisFragset = node;
            }
        }

        return numRoots == 1;
    }

    @Override
    public String toString() {
        return previousQuantifier + "/" + subgraph;
    }
}
