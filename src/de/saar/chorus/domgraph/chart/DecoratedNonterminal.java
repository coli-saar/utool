package de.saar.chorus.domgraph.chart;


public class DecoratedNonterminal<E, F> {
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
