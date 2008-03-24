package de.saar.chorus.domgraph.equivalence.rtg;

import java.util.Set;

import de.saar.chorus.domgraph.chart.GraphBasedNonterminal;
import de.saar.chorus.domgraph.chart.SubgraphNonterminal;

public class QuantifierMarkedNonterminal implements GraphBasedNonterminal {
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




    public Set<String> getNodes() {
        return subgraph.getNodes();
    }




    public String toString(Set<String> roots) {
        return previousQuantifier + "/" + subgraph.toString(roots);
    }




    public void addNode(String node) {
        subgraph.add(node);
    }
}
