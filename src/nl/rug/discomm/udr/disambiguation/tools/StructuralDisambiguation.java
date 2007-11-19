package nl.rug.discomm.udr.disambiguation.tools;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.rug.discomm.udr.chart.IntegerChart;
import nl.rug.discomm.udr.graph.Chain;
import nl.rug.discomm.udr.structurecheck.Utilities;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.EdgeData;
import de.saar.chorus.domgraph.graph.EdgeType;
import de.saar.chorus.domgraph.graph.NodeLabels;

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
	
	public static boolean markClauseRelation(List<Integer> src, List<Integer> tgt, 
			NodeLabels labels, String relation) {
		int middlefragment;
		
		if(src.get(0) > tgt.get(0)) {
			// edge from right to left
			middlefragment = tgt.get(1);
		
			if(tgt.get(1) == src.get(1)) {
				// something went wrong (sentence boundary != segment boundary e.g.)
				return false;
			}
			
		} else {
			// edge from left to right
			middlefragment = tgt.get(0);
			
			
			if(src.get(1) == middlefragment) {
				return false;
			}
		}
		
		labels.addLabel(middlefragment + "x", relation);
		return true;
	}
	
	public static Map<Integer, List<Integer>> addSentenceDominance(List<Integer> src, List<Integer> tgt, Chain graph) {
		
		int middlefragment, left, right;
		
		if(src.get(0) > tgt.get(0)) {
			// edge from right to left
			middlefragment = tgt.get(1);
			left = tgt.get(0);
			right = src.get(1);
			
			if(tgt.get(1) == right) {
				// something went wrong (sentence boundary != segment boundary e.g.)
				return null;
			}
			
		} else {
			// edge from left to right
			middlefragment = tgt.get(0);
			left = src.get(0);
			right = tgt.get(1);
			
			if(src.get(1) == middlefragment) {
				return null;
			}
		}
		
		Map<Integer, List<Integer>> edges = new HashMap<Integer, List<Integer>>();
		
		for(int l = left + 1; l < middlefragment; l++) {
			 if(graph.addDominanceEdge(middlefragment + "xl", l + "x")) {
				 Utilities.addToMapList(edges, middlefragment, l);
			 }
		}
		
		for(int r = middlefragment + 1; r <= right; r++) {
			if(graph.addDominanceEdge(middlefragment + "xr", r + "x") ) {
				 Utilities.addToMapList(edges, middlefragment, r);
			}
		}
		
		return edges;
	}
}
