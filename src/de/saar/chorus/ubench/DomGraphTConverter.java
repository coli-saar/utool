package de.saar.chorus.ubench;

import org._3pq.jgrapht.Edge;
import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphCell;

import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.domgraph.graph.NodeType;

public class DomGraphTConverter {
	
	DomGraph domGraph;
	JDomGraph jDomGraph;
	
	public DomGraphTConverter(JDomGraph graph) {
		jDomGraph = graph;
		domGraph = new DomGraph();
		
		for(DefaultGraphCell node : jDomGraph.getNodes() ) {
			
			de.saar.chorus.domgraph.graph.NodeData data;
			
			if( jDomGraph.getNodeData(node).getType().equals(NodeType.LABELLED)){
					data = new de.saar.chorus.domgraph.graph.NodeData(de.saar.chorus.domgraph.graph.NodeType.LABELLED);
			} else {
					data = new de.saar.chorus.domgraph.graph.NodeData(de.saar.chorus.domgraph.graph.NodeType.UNLABELLED); break;
			}
			
			domGraph.addNode(jDomGraph.getNodeData(node).getName(), data);

		}
		
		for( DefaultEdge edge : jDomGraph.getEdges() ) {
			de.saar.chorus.domgraph.graph.EdgeData data;
			
			if( jDomGraph.getEdgeData(edge).getType().equals(EdgeType.dominance )) {
				data = new de.saar.chorus.domgraph.graph.EdgeData(de.saar.chorus.domgraph.graph.EdgeType.DOMINANCE);
			} else {
				data = new de.saar.chorus.domgraph.graph.EdgeData(de.saar.chorus.domgraph.graph.EdgeType.TREE);
			}
			
			domGraph.addEdge(jDomGraph.getNodeData(jDomGraph.getSourceNode(edge)).getName(),
					jDomGraph.getNodeData(jDomGraph.getTargetNode(edge)).getName(),
					data);
		}
	}
	
	public DomGraphTConverter(DomGraph graph, NodeLabels labels) {
		domGraph = graph;
		jDomGraph = new JDomGraph();
		
		for(String node : domGraph.getAllNodes() ) {
			NodeData cloneData;
			if( domGraph.getData(node).getType().equals(NodeType.LABELLED) ) {
				cloneData = new NodeData(de.saar.chorus.ubench.NodeType.labelled, 
						node, labels.getLabel(node), jDomGraph);
			} else {
				cloneData = new NodeData(de.saar.chorus.ubench.NodeType.unlabelled, 
						node, jDomGraph);
			}
			
			cloneData.addMenuItem(node, node);
			jDomGraph.addNode(cloneData);
		}
		
		
		for(Edge edge :  domGraph.getAllEdges() ) {
			EdgeData cloneData;
			
			if( domGraph.getData(edge).getType().equals(de.saar.chorus.domgraph.graph.EdgeType.TREE)) {
				cloneData = new EdgeData(EdgeType.solid, edge.toString(), jDomGraph );
			} else {
				cloneData = new EdgeData(EdgeType.dominance, edge.toString(), jDomGraph );
			}
			
			cloneData.addMenuItem(edge.toString(), edge.toString());
			jDomGraph.addEdge(cloneData, jDomGraph.getNodeForName((String) edge.getSource()), 
					jDomGraph.getNodeForName((String) edge.getTarget()));
		}
	}
	
	public JDomGraph getJDomGraph() {
		return jDomGraph;
	}
	
	public DomGraph getDomGraph() {
		return domGraph;
	}
}
