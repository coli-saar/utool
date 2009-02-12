package de.saar.chorus.domgraph.weakest;

import java.util.List;

import de.saar.chorus.domgraph.chart.RewritingRtg;
import de.saar.chorus.domgraph.chart.RtgFreeFragmentAnalyzer;
import de.saar.chorus.domgraph.chart.Split;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.EdgeType;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.domgraph.graph.CompactificationRecord.NodeChildPair;

public class WeakestReadingsRtg extends RewritingRtg<Annotation> {
	private static boolean DEBUG = false;
	
	private RewriteSystem rewriteSystem;
	private Annotator annotator;
	
	public WeakestReadingsRtg(DomGraph graph, NodeLabels labels, RtgFreeFragmentAnalyzer<?> analyzer, RewriteSystem system, Annotator annotator) {
		super(graph, labels, analyzer);
		
		this.rewriteSystem = system;
		this.annotator = annotator;
	}

	@Override
	protected boolean allowedSplit(Annotation previousQuantifier, String currentRoot) {
		String parent = previousQuantifier.getPreviousNode();
		
		if(DEBUG) System.err.print("(check " + currentRoot + " under " + previousQuantifier + ") ");
		
		if( parent == null ) {
			if(DEBUG) System.err.println("[allowed, par=null]");
			return true;
		} else if( ! analyzer.isCoFree(parent, currentRoot) ) {
			if(DEBUG) System.err.println("[allowed, not co-free]");
			return true;
		} else {
			boolean permutable = isPermutable(previousQuantifier, currentRoot);
			if(DEBUG) System.err.println("[allowed=" + !permutable + " perm]");
			return !permutable;
		}
	}

	private boolean isPermutable(Annotation previousQuantifier, String currentRoot) {
		String parent = previousQuantifier.getPreviousNode();
		int parentToCurrent = analyzer.getReachability(parent, currentRoot);
		int currentToParent = analyzer.getReachability(currentRoot, parent);
		
		List<NodeChildPair> pathInParent = compactificationRecord.getRecord(parent, parentToCurrent);
		List<NodeChildPair> pathInCurrent = compactificationRecord.getRecord(currentRoot, currentToParent);
		
		String annotation = previousQuantifier.getAnnotation();
		
		for( NodeChildPair ncpInParent : pathInParent ) {
			for( NodeChildPair ncpInCurrent : pathInCurrent ) {
				if( ! rewriteSystem.hasRule(labels.getLabel(ncpInParent.node), ncpInParent.childIndex,
						labels.getLabel(ncpInCurrent.node), ncpInCurrent.childIndex, annotation) ) {
					return false;
				}	
			}
			
			annotation = annotator.getChildAnnotation(annotation, labels.getLabel(ncpInParent.node), ncpInParent.childIndex);
		}

		return true;
	}

	@Override
	protected Split<Annotation> makeSplit(Annotation previous, String root) {
		Split<Annotation> ret = new Split<Annotation>(root);
		String myAnnotation = (previous.getPreviousNode() == null) ? annotator.getStart() : getPathAnnotation(previous.getPreviousNode(), previous.getAnnotation(), analyzer.getReachability(previous.getPreviousNode(), root));
		Annotation ann = new Annotation(root,myAnnotation);
		List<String> children = compact.getChildren(root, EdgeType.TREE);
		
		for( int i = 0; i < children.size(); i++ ) {
			ret.addWcc(children.get(i), ann);
		}
		
		return ret;
	}
	
	private String getPathAnnotation(String root, String rootAnnotation, int hole) {
		if( root == null ) {
			return annotator.getStart();
		} else {
			List<NodeChildPair> path = compactificationRecord.getRecord(root, hole);
			String ret = rootAnnotation;

			for( NodeChildPair ncp : path ) {
				ret = annotator.getChildAnnotation(ret, labels.getLabel(ncp.node), ncp.childIndex);
			}

			return ret;
		}
	}

	@Override
	protected Annotation makeTopLevelNonterminal() {
		return new Annotation(null, annotator.getStart());
	}
}
