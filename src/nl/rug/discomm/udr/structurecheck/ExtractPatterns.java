package nl.rug.discomm.udr.structurecheck;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;

public class ExtractPatterns {

	
	private Map<String,int[]> boolFeat;
	private Map<String, List<Map<String,Integer>>> stringFeat;
	private Map<String, Integer> overallCount;
	private int boolcount, stringcount;
	
	public ExtractPatterns() {
		boolFeat = new HashMap<String, int[]>();
		stringFeat = new HashMap<String, List<Map<String,Integer>>>();
		boolcount = BooleanFeatures.values().length;
		stringcount = StringFeatures.values().length;
		overallCount = new HashMap<String,Integer>();
	}
	
	
	private void countUp(String key, BooleanFeatures feat) {
		if(! boolFeat.containsKey(key)) {
			boolFeat.put(key, new int[boolcount]);
		}
		boolFeat.get(key)[feat.ordinal()]++;
	}
	
	private void countUp(String key, String value, StringFeatures feat) {
		int ord = feat.ordinal();
		if(! stringFeat.containsKey(key)) {
			Map<String,Integer> val = new HashMap<String,Integer>();
			val.put(value,0);
			List<Map<String,Integer>> list = new ArrayList<Map<String,Integer>>(stringcount);
			list.set(ord, val);
			stringFeat.put(key, list);
		}
		Utilities.countUp(stringFeat.get(key).get(ord), value);
	}
	
	/**
	 * TODO implement me
	 * 
	 * @param graph
	 * @param labels
	 */
	public void checkDomGraph(DomGraph graph, NodeLabels labels) {
		for( String root : graph.getAllRoots() ) {
			if(! graph.isLeaf(root)) {
				// here go lots of if-then-else's.
			}
		}
		
		
	}
	
	/**
	 * TODO implement me
	 * 
	 * @param filename
	 */
	public void saveToFile(String filename) {
		//perhaps a nice toString method would be convenient, too.
	}
	
	
	enum BooleanFeatures {
		
		// EDUs
		RIGHT_CHILD_LEAF, LEFT_CHILD_LEAF, NUCLEUS_LEAF, SATTELITE_LEAF,
		
		// same relations or two leaves
		EQUAL_CHILDREN, 
		
		// compare nuclearity to parent relation (left or right or coord)
		RIGHT_CHILD_NUCEQ, LEFT_CHILD_NUCEQ, NUCLEUS_NUCEQ, SATTELITE_NUCEQ,
		
		// general structure
		IS_ROOT;
	}
	
	enum StringFeatures {
		// relations; only relevant, if the corresponding
		// LEAF-feature is 'false'
		RIGHT_CHILD, LEFT_CHILD, NUCLEUS, SATTELITE;
	}

}
