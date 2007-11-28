package de.saar.chorus.domgraph.chart;

import java.util.Set;

public interface Nonterminal {
    public boolean isEmpty();

    public boolean isSingleton(Set<String> roots);
    public String getRootIfSingleton();

    public void addNode(String node);
    public Set<String> getNodes();
    public String toString(Set<String> roots);
}
