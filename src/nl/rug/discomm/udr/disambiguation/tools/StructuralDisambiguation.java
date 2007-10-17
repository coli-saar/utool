package nl.rug.discomm.udr.disambiguation.tools;

import java.util.Map;

import nl.rug.discomm.udr.chart.ModifiableChart;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.EdgeData;
import de.saar.chorus.domgraph.graph.EdgeType;

public class StructuralDisambiguation {
	
	public static void addDominance(String src, String tgt, DomGraph graph) {
		graph.addEdge(src,tgt, new EdgeData(EdgeType.DOMINANCE));
	}
	
	
	public static void addDominance(String src, String tgt, DomGraph graph, ModifiableChart chart) {
		graph.addEdge(src,tgt, new EdgeData(EdgeType.DOMINANCE));
		chart.addDominance(src,tgt);
	}
	
	
	public static void addAllDominances(Map<String,String> domedges, DomGraph graph, ModifiableChart chart) {
		for(Map.Entry<String, String> edge : domedges.entrySet()) {	
			addDominance(edge.getKey(),edge.getValue(),graph,chart);
		}
	}
}
