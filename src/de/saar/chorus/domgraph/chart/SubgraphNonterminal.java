package de.saar.chorus.domgraph.chart;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class SubgraphNonterminal implements GraphBasedNonterminal {
    private String rootForThisFragset;
    private boolean changed;
    private int previousHashcode;

    private final Set<String> nodes;

    public SubgraphNonterminal(Collection<String> init) {
        nodes = new HashSet<String>(init);
        changed = true;
    }

    public SubgraphNonterminal() {
        nodes = new HashSet<String>();
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

    private boolean add(String e) {
        // TODO Auto-generated method stub
        changed = true;
        return nodes.add(e);
    }

    private boolean remove(Object o) {
        // TODO Auto-generated method stub
        changed = true;
        return nodes.remove(o);
    }


    private boolean removeAll(Collection<?> arg0) {
        changed = true;
        // TODO Auto-generated method stub
        return nodes.removeAll(arg0);
    }

    /*
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
    */

    private static final long serialVersionUID = 1533989291501267385L;

    // override
    public Set<String> getNodes() {
        return nodes;
    }


    public String toString(Set<String> roots) {
        Set<String> s = new HashSet<String>(nodes);
        s.retainAll(roots);
        return s.toString();
    }


    public void addNode(String node) {
        add(node);
    }

    @Deprecated
    public Iterator<String> iterator() {
        return nodes.iterator();
    }

    @Deprecated
    public int size() {
        return nodes.size();
    }



}
