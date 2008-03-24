package de.saar.chorus.domgraph.chart;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class SubgraphNonterminal extends HashSet<String> implements GraphBasedNonterminal {
    private String rootForThisFragset;
    private boolean changed;
    private int previousHashcode;


    public SubgraphNonterminal(Collection<String> init) {
        super(init);
        changed = true;
    }

    public SubgraphNonterminal() {
        super();
        changed = true;
    }








    @Override
    public int hashCode() {
        if( !changed ) {
            return previousHashcode;
        } else {
            previousHashcode = super.hashCode();
            changed = false;
            return previousHashcode;
        }
    }

    @Override
    public boolean add(String e) {
        // TODO Auto-generated method stub
        changed = true;
        return super.add(e);
    }

    @Override
    public boolean remove(Object o) {
        // TODO Auto-generated method stub
        changed = true;
        return super.remove(o);
    }


    @Override
    public boolean removeAll(Collection<?> arg0) {
        changed = true;
        // TODO Auto-generated method stub
        return super.removeAll(arg0);
    }

    public String getRootIfSingleton() {
        return rootForThisFragset;
    }


    public boolean isSingleton(Set<String> roots) {
        int numRoots = 0;


        for( String node : this ) {
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

    private static final long serialVersionUID = 1533989291501267385L;


    public Set<String> getNodes() {
        return this;
    }


    public String toString(Set<String> roots) {
        Set<String> s = new HashSet<String>(this);
        s.retainAll(roots);
        return s.toString();
    }


    public void addNode(String node) {
        add(node);
    }

}
