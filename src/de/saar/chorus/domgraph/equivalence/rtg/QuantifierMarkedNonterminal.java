package de.saar.chorus.domgraph.equivalence.rtg;

import java.util.Set;

import de.saar.chorus.domgraph.chart.NonterminalA;
import de.saar.chorus.domgraph.chart.SubgraphNonterminal;

public class QuantifierMarkedNonterminal implements NonterminalA {
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

    @Override
    public boolean equals(Object obj) {
        if( obj instanceof QuantifierMarkedNonterminal ) {
            QuantifierMarkedNonterminal other = (QuantifierMarkedNonterminal) obj;

            if( previousQuantifier == null && other.previousQuantifier != null ) {
                return false;
            }

            if( previousQuantifier != null && other.previousQuantifier == null ) {
                return false;
            }

            if( previousQuantifier == null && other.previousQuantifier == null ) {
                return subgraph.equals(other.subgraph);
            }

            return previousQuantifier.equals(other.previousQuantifier) && subgraph.equals(other.subgraph);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return subgraph.hashCode() + ((previousQuantifier == null)?17:29*previousQuantifier.hashCode());
    }



    @Override
    public Set<String> getNodes() {
        return subgraph.getNodes();
    }



    @Override
    public String toString(Set<String> roots) {
        // TODO Auto-generated method stub
        return null;
    }



    @Override
    public void addNode(String node) {
        subgraph.add(node);
    }
}
