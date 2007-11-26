package de.saar.chorus.domgraph.chart;

import java.util.Set;

public interface Nonterminal {
    public boolean isEmpty();
    public boolean isSingleton(Set<String> roots);
    public String getRootIfSingleton();
}
