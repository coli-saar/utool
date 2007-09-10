package de.saar.testingtools;

import de.saar.chorus.domgraph.codec.*;
import de.saar.chorus.domgraph.codec.domcon.*;
import de.saar.chorus.domgraph.graph.*;
import de.saar.chorus.domgraph.chart.*;

import java.io.*;
import java.util.*;

public class TestingTools {
	private static InputCodec dc = new DomconOzInputCodec();
	private static OutputCodec outdc = new DomconOzOutputCodec();
	
	public static void decodeDomcon(String domcon, DomGraph graph, NodeLabels labels) {
		dc.decode(new StringReader(domcon), graph, labels);
	}
	
	public static void assertDomgraphEquals(DomGraph graph, NodeLabels labels, String domcon, String msg) {
		DomGraph goldGraph = new DomGraph();
		NodeLabels goldLabels = new NodeLabels();
		
		decodeDomcon(domcon, goldGraph, goldLabels);
		assert DomGraph.isEqual(graph, labels, goldGraph, goldLabels) : msg + " " + encodeDomcon(graph,labels);
	}
	
	public static String encodeDomcon(DomGraph graph, NodeLabels labels) {
		StringWriter buf = new StringWriter();
		
		outdc.encode(graph, labels, buf);
		return buf.toString();
	}
	
	
	public static void expectException(Class exceptionType, Closure someCode) {
		try {
			someCode();
			assert false;
		} catch(Exception e) {
			assert exceptionType.isInstance(e)
		}
	}
	
	public static List collectIteratorValues(Iterator it) {
		List ret = new ArrayList();
		
		while( it.hasNext() ) {
			ret.add(it.next());
		}
		
		return ret;
	}
	
	
	
	
	public static void checkSolvedForms(DomGraph graph, List goldSolvedForms) {
		Chart chart = new Chart();	
	
		assert ChartSolver.solve(graph, chart) == true;
		
		BigInteger predictedSolvedForms = chart.countSolvedForms();
		assert predictedSolvedForms.equals(new BigInteger(goldSolvedForms.size())) : "predicated " + predictedSolvedForms + " solved forms";
		
		SolvedFormIterator sfi = new SolvedFormIterator(chart, graph);
		List sfs = collectIteratorValues(sfi);
		
		assert solvedFormsEqual(sfs, goldSolvedForms) : "sfs = " + sfs;
	}
	
	public static void checkUnsolvable(DomGraph graph) {
		Chart chart = new Chart();
		assert ChartSolver.solve(graph, chart) == false;
	}
	

	
	// Compare two lists of solved forms. The first (result) is a list of SolvedFormSpecs
	// as returned by a SolvedFormIterator. The second (gold) has the following form:
	//    gold     -> List(sf)
	//    sf       -> [List(domedge), substitution)
	//    domedge  -> [source,target]
	//    substitution -> map(string->string)
	private static boolean solvedFormsEqual(List result, List gold) {
		List goldSolvedFormSpecs = gold.collect { sf ->
			List domEdges = sf.get(0).collect { new DomEdge(it.get(0), it.get(1)) };
			Map substitution = sf.get(1);
			
			new SolvedFormSpec(domEdges,substitution);
		};
		
		if( result.size() != gold.size() ) {
			return false;
		}
		
		for( spec in goldSolvedFormSpecs ) {
			if( result.find { 
				  (new HashSet(it.getDomEdges()).equals(new HashSet(spec.getDomEdges()))) && it.getSubstitution().equals(spec.getSubstitution()) 
				} == null ) {
				return false;
			}
		}
		
		return true;
	}
}