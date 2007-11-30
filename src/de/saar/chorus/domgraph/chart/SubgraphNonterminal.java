package de.saar.chorus.domgraph.chart;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class SubgraphNonterminal extends HashSet<String> implements Nonterminal {
    private String rootForThisFragset;

    public SubgraphNonterminal(Collection<String> init) {
        super(init);
    }

    public SubgraphNonterminal() {
        super();
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
