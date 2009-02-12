package de.saar.chorus.domgraph.weakest;

import java.util.List;

import de.saar.chorus.domgraph.chart.RewritingRtg;
import de.saar.chorus.domgraph.chart.Split;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.EdgeType;
import de.saar.chorus.domgraph.graph.NodeLabels;

public class WeakestReadingsRtg extends RewritingRtg<Annotation> {
	private RewriteSystem rewriteSystem;
	private Annotator annotator;
	
	public WeakestReadingsRtg(DomGraph graph, NodeLabels labels, RewriteSystem system, Annotator annotator) {
		super(graph, labels);
		
		this.rewriteSystem = system;
		this.annotator = annotator;
	}

	@Override
	protected boolean allowedSplit(Annotation previousQuantifier, String currentRoot) {
		String parent = previousQuantifier.getPreviousNode(); 
		if( parent == null ) {
			return true;
		} else if( ! analyzer.isCoFree(parent, currentRoot) ) {
			return true;
		} else {
			return ! rewriteSystem.hasRule(labels.getLabel(parent), analyzer.getReachability(parent, currentRoot), 
					labels.getLabel(currentRoot), analyzer.getReachability(currentRoot, parent),
					previousQuantifier.getAnnotation());
		}
	}

	@Override
	protected Split<Annotation> makeSplit(Annotation previous, String root) {
		Split<Annotation> ret = new Split<Annotation>(root);
		List<String> children = compact.getChildren(root, EdgeType.TREE);
		
		for( int i = 0; i < children.size(); i++ ) {
			ret.addWcc(children.get(i), new Annotation(root, annotator.getChildAnnotation(previous.getAnnotation(), labels.getLabel(root), i)));
		}
		
		return ret;
	}

	@Override
	protected Annotation makeTopLevelNonterminal() {
		return new Annotation(null, annotator.getStart());
	}
}
