package de.saar.chorus.domgraph.weakest;

import java.util.List;

import de.saar.chorus.domgraph.chart.RewritingRtg;
import de.saar.chorus.domgraph.chart.Split;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;

public class WeakestReadingsRtg extends RewritingRtg<Annotation> {
	public WeakestReadingsRtg(DomGraph graph, NodeLabels labels) {
		super(graph, labels);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected boolean allowedSplit(Annotation previousQuantifier,
			String currentRoot) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected Split<Annotation> makeSplit(Annotation previous, String root) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Annotation> getToplevelSubgraphs() {
		// TODO Auto-generated method stub
		return null;
	}
}
