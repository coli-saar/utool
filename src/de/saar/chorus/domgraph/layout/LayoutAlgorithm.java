package de.saar.chorus.domgraph.layout;

import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;

public interface LayoutAlgorithm {
	public void layout(DomGraph graph, NodeLabels labels, Canvas canvas);
}
