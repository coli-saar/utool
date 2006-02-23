package de.saar.chorus.ubench.utool;

import org._3pq.jgrapht.Edge;
import org._3pq.jgrapht.ext.JGraphModelAdapter;

import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeType;
import de.saar.chorus.ubench.EdgeData;
import de.saar.chorus.ubench.EdgeType;
import de.saar.chorus.ubench.JDomGraph;
import de.saar.chorus.ubench.NodeData;

public class DomGraphTConverter {
	
	DomGraph domGraph;
	JDomGraph jDomGraph;
	
	
	public DomGraphTConverter(DomGraph graph) {
		domGraph = graph;
		jDomGraph = new JDomGraph();
		
		for(String node : domGraph.getAllNodes() ) {
			NodeData cloneData;
			if( domGraph.getData(node).getType().equals(NodeType.LABELLED) ) {
				cloneData = new NodeData(de.saar.chorus.ubench.NodeType.labelled, 
						node, jDomGraph);
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
}
