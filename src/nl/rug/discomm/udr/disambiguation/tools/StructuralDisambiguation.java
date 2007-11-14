package nl.rug.discomm.udr.disambiguation.tools;

import java.util.Map;

import nl.rug.discomm.udr.chart.IntegerChart;
import nl.rug.discomm.udr.chart.ModifiableChart;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.EdgeData;
import de.saar.chorus.domgraph.graph.EdgeType;

public class StructuralDisambiguation {
	
	public static void addDominance(String src, String tgt, DomGraph graph) {
		graph.addEdge(src,tgt, new EdgeData(EdgeType.DOMINANCE));
	}
	
	
	public static void addDominance(String src, String tgt, DomGraph graph, IntegerChart chart) {
		graph.addEdge(src,tgt, new EdgeData(EdgeType.DOMINANCE));
		
		int s= Integer.parseInt(src.substring(0, src.length() -2));
		int t = Integer.parseInt(tgt.substring(0,tgt.length() -1));
		chart.addDominanceEdge(s,t);
	}
	
	
	public static void addAllDominances(Map<String,String> domedges, DomGraph graph, IntegerChart chart) {
		for(Map.Entry<String, String> edge : domedges.entrySet()) {	
			long time = System.currentTimeMillis();
			addDominance(edge.getKey(),edge.getValue(),graph,chart);
			System.err.println("Dominance " + edge.getKey() + " --> " + edge.getValue() + " :" +
					(System.currentTimeMillis() - time) + "ms");
		}
	}
}
