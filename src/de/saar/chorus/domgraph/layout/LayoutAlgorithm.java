package de.saar.chorus.domgraph.layout;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org._3pq.jgrapht.Edge;

import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.EdgeType;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.domgraph.graph.NodeType;
import de.saar.chorus.domgraph.layout.LayoutOptions.LabelType;
import de.saar.chorus.ubench.JDomGraphTab;

public abstract class LayoutAlgorithm {
	
	protected Map<String, String> nodeToLabel;
	
	protected abstract void layout(DomGraph graph, NodeLabels labels, Canvas canvas);
	
	public void layout(DomGraph graph, NodeLabels labels, Canvas canvas,
			LayoutOptions lo) {


		nodeToLabel = new HashMap<String, String>();

		// default
		if(lo == null || lo.getLabeltype() == LabelType.NAME) {
			for(String node : graph.getAllNodes() ) {
				if(graph.getData(node).getType() == NodeType.LABELLED) {
					nodeToLabel.put(node, node);
				} else {
					nodeToLabel.put(node, "(" + node + ")");
				}
			}
		} else {
			LabelType lt = lo.getLabeltype();
			for(String node : graph.getAllNodes()) {
				// options for labelled nodes
				if(graph.getData(node).getType() == NodeType.LABELLED) {
					if(lt == LabelType.LABEL) {
						nodeToLabel.put(node,labels.getLabel(node));
					} else {
						nodeToLabel.put(node, node + " : " + labels.getLabel(node));
					}

				} else {
					// holes
					nodeToLabel.put(node, "(" + node + ")");
				}
			}
		}
		
		
		if(lo != null && lo.isRemoveRdundandEdges() == true) {
			graph = removeRedundandEdges(graph);
		}
		
		
		layout(graph, labels, canvas);
	}
	
	protected DomGraph removeRedundandEdges(DomGraph graph) {
		Set<Edge> visited = new HashSet<Edge>();
		DomGraph toReturn = (DomGraph) graph.clone();
		
		for(Edge edge : graph.getAllEdges()) {
			
			if(graph.getData(edge).getType() == EdgeType.DOMINANCE && 
					(! visited.contains(edge)) ) {
				visited.add(edge);
				String src = (String) edge.getSource();
				String tgt = (String) edge.getTarget();

				
				toReturn.remove(edge); 
				if(! toReturn.reachable(src, tgt)) {
					toReturn.addEdge(src, tgt, graph.getData(edge));
				} 
				
			}
			
		}
		return toReturn;
	}
	
}
