package de.saar.chorus.domgraph.chart;

import java.util.Set;

public class DecoratedNonterminal<E extends Nonterminal, F> implements Nonterminal {
    public E nonterminal;
    public F decoration;

    public DecoratedNonterminal(E nonterminal, F decoration) {
        super();
        this.nonterminal = nonterminal;
        this.decoration = decoration;
    }


    @Override
    public String toString() {
        return nonterminal.toString() + "/" + decoration.toString() ;
    }

    @Override
    public int hashCode() {
        return nonterminal.hashCode() + 29*decoration.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if( obj instanceof DecoratedNonterminal ) {
            DecoratedNonterminal other = (DecoratedNonterminal) obj;

            return nonterminal.equals(other.nonterminal) && decoration.equals(other.decoration);
        } else {
            return false;
        }
    }

}
