package nl.rug.discomm.udr.structurecheck;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.EdgeType;
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
	 * Frequency counts for all features for one DomGraph. 
	 * 
	 * @param graph
	 * @param labels
	 */
	public void checkDomGraph(DomGraph graph, NodeLabels labels) {
		for( String root : graph.getAllRoots() ) {
			if(! graph.isLeaf(root)) {
				String relation = labels.getLabel(root);
				
				// Joint and List are not of interest.
				if(! relation.startsWith("List") ||
						relation.startsWith("Joint")) {
					
					Utilities.countUp(overallCount, relation);
					
					if(graph.indeg(root) == 0) {
						countUp(relation,BooleanFeatures.IS_ROOT);
					}
					
					// '0' = nucleus left, '1' = nucleus right.
					int nucleus = Integer.parseInt(relation.
							substring(relation.length()-2,
	 						relation.length() -1));
					boolean rightleaf = false, leftleaf = false;
					List<String> holes = graph.getHoles(root);
					String leftRel = null, rightRel = null;
					
					String lefthole = holes.get(0);
					String leftChild = graph.getChildren(lefthole, EdgeType.DOMINANCE).get(0);
					
					if(graph.isLeaf(leftChild)) {
						countUp(relation, BooleanFeatures.LEFT_CHILD_LEAF);
						leftleaf = true;
						if(nucleus == 0) {
							countUp(relation,BooleanFeatures.NUCLEUS_LEAF);
						} else {
							countUp(relation,BooleanFeatures.SATTELITE_LEAF);
						}
					} else {
						
						leftRel = labels.getLabel(leftChild);
						countUp(relation, leftRel, StringFeatures.LEFT_CHILD);
						
						int childnuc = Integer.parseInt(relation.
								substring(relation.length()-2,
				 						relation.length() -1));
						
						
						if(nucleus ==0) {
							countUp(relation, leftRel, StringFeatures.NUCLEUS);
							if(childnuc == nucleus) {
								countUp(relation, BooleanFeatures.LEFT_CHILD_NUCEQ);
								countUp(relation, BooleanFeatures.NUCLEUS_NUCEQ);
							}
							
						} else {
							countUp(relation, leftRel, StringFeatures.SATTELITE);
							if(childnuc == nucleus) {
								countUp(relation, BooleanFeatures.LEFT_CHILD_NUCEQ);
								countUp(relation, BooleanFeatures.SATTELITE_NUCEQ);
							}
						}
					}
					
					
					String righthole = holes.get(1);
					String rightChild = graph.getChildren(righthole, EdgeType.DOMINANCE).get(0);
					
					if(graph.isLeaf(rightChild)) {
						countUp(relation, BooleanFeatures.RIGHT_CHILD_LEAF);
						rightleaf = true;
						if(leftleaf) {
							countUp(relation, BooleanFeatures.EQUAL_CHILDREN);
						}
						if(nucleus == 1) {
							countUp(relation,BooleanFeatures.NUCLEUS_LEAF);
						} else {
							countUp(relation,BooleanFeatures.SATTELITE_LEAF);
						}
					} else {
						
						rightRel = labels.getLabel(rightChild);
						if(! leftleaf) {
							if( leftRel.equals(rightRel)) {
								countUp(relation,BooleanFeatures.EQUAL_CHILDREN);
							}
						}
						
						countUp(relation, rightRel, StringFeatures.RIGHT_CHILD);
						
						int childnuc = Integer.parseInt(relation.
								substring(relation.length()-2,
				 						relation.length() -1));
						
						
						if(nucleus == 1) {
							countUp(relation, rightRel, StringFeatures.NUCLEUS);
							if(childnuc == nucleus) {
								countUp(relation, BooleanFeatures.RIGHT_CHILD_NUCEQ);
								countUp(relation, BooleanFeatures.NUCLEUS_NUCEQ);
							}
							
						} else {
							countUp(relation, rightRel, StringFeatures.SATTELITE);
							if(childnuc == nucleus) {
								countUp(relation, BooleanFeatures.RIGHT_CHILD_NUCEQ);
								countUp(relation, BooleanFeatures.SATTELITE_NUCEQ);
							}
						}
					}
					
				}
				
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
