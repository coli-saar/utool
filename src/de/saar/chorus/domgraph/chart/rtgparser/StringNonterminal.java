package de.saar.chorus.domgraph.chart.rtgparser;

import java.util.Set;

import de.saar.chorus.domgraph.chart.Nonterminal;

public class StringNonterminal implements Nonterminal {
    private final String string;
    private boolean finalstate;



    public StringNonterminal(String string) {
        super();
        this.string = string;
        finalstate = false;
    }

    public void setFinal(boolean f) {
        finalstate = f;
    }

    @Override
    public void addNode(String node) {
        // TODO Auto-generated method stub

    }

    @Override
    public Set<String> getNodes() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getRootIfSingleton() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isEmpty() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    // what this _really_ means is that it's a final state
    public boolean isSingleton(Set<String> roots) {
        return finalstate;
    }

    @Override
    public String toString(Set<String> roots) {
        return string;
    }

    @Override
    public String toString() {
        return string;
    }

    @Override
    public int hashCode() {
        return string.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if( obj instanceof StringNonterminal ) {
            StringNonterminal new_name = (StringNonterminal) obj;

            return new_name.string.equals(this.string);

        } else {
            return false;
        }
    }
}
