package nl.rug.discomm.udr.structurecheck;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.saar.chorus.domgraph.graph.DomEdge;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;

public class CheckRFC {

	
	static Set<String> rfctriggers;
	public static String RELATION = "relation";
	
	static {
		rfctriggers = new HashSet<String>();
		
		// mapped to SDRT's RESULT
		rfctriggers.add("cause(1)");
		rfctriggers.add("result(2)");
		rfctriggers.add("explanation-argumentative(2)");
		rfctriggers.add("consequence-s(1)");
		rfctriggers.add("consequence-n(2)");
		rfctriggers.add("Consequence");
		rfctriggers.add("reason(2)");
		
		// mapped to SDRT's CONTRAST
		rfctriggers.add("contrast");
		rfctriggers.add("antithesis");
		rfctriggers.add("concession");
		
	}
	
	public static Map<String,Double> checkRFCViolations(DomGraph graph, NodeLabels labels) {
		Map<String, Double> ret = new HashMap<String, Double>();
		Map<String, Integer> countViolations = new HashMap<String, Integer>();
		Map<String, Integer> countOverall = new HashMap<String, Integer>();
		
		countOverall.put(RELATION, 0);
		countViolations.put(RELATION, 0);
		
		for( DomEdge de : graph.getAllDomEdges() ) {
			String hole = de.getSrc();
			String root = de.getTgt();

			if(! graph.isLeaf(root)) {
				root= labels.getLabel(root);
				Utilities.countUp(countOverall, RELATION);
				Utilities.countUp(countOverall, root);
				
				if(hole.contains("hl")) {
					Utilities.countUp(countViolations, root);
					Utilities.countUp(countViolations, RELATION);
				} else {
					if(! countViolations.containsKey(root) ) {
						countViolations.put(root,0);
					}
				}
			}
		}
		
		for( Map.Entry<String, Integer> countall : countOverall.entrySet() ) {
			double all = countall.getValue();
			double violations = countViolations.get(countall.getKey());
			ret.put(countall.getKey(), violations/all);
			
		}
		
		System.err.println("Overall:  " + ret.get(RELATION));
		
		return ret;
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
