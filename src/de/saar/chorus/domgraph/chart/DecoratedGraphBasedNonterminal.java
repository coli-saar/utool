package de.saar.chorus.domgraph.chart;

import java.util.Set;

public class DecoratedGraphBasedNonterminal<E extends GraphBasedNonterminal, F> extends DecoratedNonterminal<E,F>  implements GraphBasedNonterminal {
	public E nonterminal;
    public F decoration;

    public DecoratedGraphBasedNonterminal(E nonterminal, F decoration) {
        super(nonterminal,decoration);
    }


    public void addNode(String node) {
        nonterminal.addNode(node);
    }


    public Set<String> getNodes() {
        return nonterminal.getNodes();
    }

    /*
    public String getRootIfSingleton() {
        return nonterminal.getRootIfSingleton();
    }
*/

    public boolean isEmpty() {
        return nonterminal.isEmpty();
    }

    /*
    public boolean isSingleton(Set<String> roots) {
        return nonterminal.isSingleton(roots);
    }
    */


    public String toString(Set<String> roots) {
        return nonterminal.toString(roots) + "/" + decoration.toString() ;
    }

}
