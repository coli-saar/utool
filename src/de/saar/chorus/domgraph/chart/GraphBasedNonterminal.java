package de.saar.chorus.domgraph.chart;

import java.util.Set;

public interface GraphBasedNonterminal {
    public void addNode(String node);
    public Set<String> getNodes();
    public String toString(Set<String> roots);
}
