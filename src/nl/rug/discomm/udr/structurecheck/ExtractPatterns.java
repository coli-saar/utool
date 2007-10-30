package nl.rug.discomm.udr.structurecheck;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
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
	private List<Map<String,Integer>> standardNumbers;
	
	public ExtractPatterns() {
		boolFeat = new HashMap<String, int[]>();
		stringFeat = new HashMap<String, List<Map<String,Integer>>>();
		boolcount = BooleanFeatures.values().length;
		stringcount = StringFeatures.values().length;
		overallCount = new HashMap<String,Integer>();
		standardNumbers = new ArrayList<Map<String,Integer>>();
		for(int i = 0; i < stringcount; i++) {
			standardNumbers.add(null);
		}
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
			
			
			List<Map<String,Integer>> list = new ArrayList<Map<String,Integer>>();
			for(int i = 0; i< stringcount; i++) {
				list.add(new HashMap<String,Integer>());
			}
			list.get(ord).put(value,0);
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
				if(! (relation.startsWith("List") ||
						relation.startsWith("Joint"))) {
					
					Utilities.countUp(overallCount, relation);
					
					if(graph.indeg(root) == 0) {
						countUp(relation,BooleanFeatures.IS_ROOT);
					}
					
					boolean multi = false;
					// '0' = nucleus left, '1' = nucleus right.
					if(relation.contains("(1)(2)") ){
						multi = true;
					}
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
						if(! multi) {
							if(nucleus == 1) {
								countUp(relation,BooleanFeatures.NUCLEUS_LEAF);
							} else {
								countUp(relation,BooleanFeatures.SATELLITE_LEAF);
							}
						}
					} else {
						
						leftRel = labels.getLabel(leftChild);
						countUp(relation, leftRel, StringFeatures.LEFT_CHILD);
						
						
						boolean childmulti = 
							leftRel.contains("(1)(2)");
						int childnuc = Integer.parseInt(relation.
								substring(relation.length()-2,
				 						relation.length() -1));

						if(childmulti && multi) {
							// both coord
							countUp(relation, BooleanFeatures.LEFT_CHILD_NUCEQ);
						} else {
							if(! multi) {
								if(nucleus == 1) {
									// current rel. child is the nucleus
									countUp(relation, leftRel, StringFeatures.NUCLEUS);
									if((! childmulti) && (childnuc == nucleus)) {
										// both left-nuclear
										countUp(relation, BooleanFeatures.LEFT_CHILD_NUCEQ);
										countUp(relation, BooleanFeatures.NUCLEUS_NUCEQ);
									}

								} else {
									// current rel. child is the satellite
									countUp(relation, leftRel, StringFeatures.SATELLITE);
									if((! childmulti) && (childnuc == nucleus)) {
										// both right-nuclear
										countUp(relation, BooleanFeatures.LEFT_CHILD_NUCEQ);
										countUp(relation, BooleanFeatures.SATELLITE_NUCEQ);
									}
								}
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
						if(! multi) {
							if(nucleus == 2) {
								countUp(relation,BooleanFeatures.NUCLEUS_LEAF);
							} else {
								countUp(relation,BooleanFeatures.SATELLITE_LEAF);
							}
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
						boolean childmulti = 
							rightRel.contains("(1)(2)");
						if(childmulti && multi) {
							countUp(relation, BooleanFeatures.RIGHT_CHILD_NUCEQ);
						} else {
							if(! multi) {
								if(nucleus == 2) {
									countUp(relation, rightRel, StringFeatures.NUCLEUS);
									if((! childmulti) && (childnuc == nucleus)) {
										countUp(relation, BooleanFeatures.RIGHT_CHILD_NUCEQ);
										countUp(relation, BooleanFeatures.NUCLEUS_NUCEQ);
									}

								} else {
									countUp(relation, rightRel, StringFeatures.SATELLITE);
									if((! childmulti) && (childnuc == nucleus)) {
										countUp(relation, BooleanFeatures.RIGHT_CHILD_NUCEQ);
										countUp(relation, BooleanFeatures.SATELLITE_NUCEQ);
									}
								}
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
	public void saveToFile(String filename) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
		String tabs = "\t\t\t\t", n = System.getProperty("line.separator");
		DecimalFormat df = new DecimalFormat("0.0000");
		List<String> rels = new ArrayList<String>(overallCount.keySet());
		Collections.sort(rels);
		
		for(String rel : rels) {
			if((! rel.startsWith("List")) && (! rel.startsWith("Joint"))) {
			writer.append(rel + tabs + "Overall counts: " + overallCount.get(rel) + n);
			
			int[] counts = boolFeat.get(rel);
			BooleanFeatures[] bool = BooleanFeatures.values();
			double all = (double) overallCount.get(rel);
			for(int i = 0; i < bool.length; i++) {
				 
				 String fraction = df.format((double)counts[i]/all);
				writer.append(tabs + bool[i] + "" + tabs + "" + 
						counts[i] + "" + tabs + fraction+ "" + n);
			}
			writer.append(n);
			writer.flush();
			StringFeatures[] strings = StringFeatures.values();
			for(int i = 0; i < strings.length; i++) {

				if(stringFeat.containsKey(rel)) {
					writer.append(tabs + strings[i] + n);
					Map<String,Integer> ct = 
						stringFeat.get(rel).get(i);
					List<String> ttmp = new ArrayList<String>(ct.keySet());
					Collections.sort(ttmp);
					
					for(String val : ttmp) {
						if(! (val.startsWith("List") ||
								val.startsWith("Joint")) ) {
							String fraction = df.format((double) ct.get(val)/all);
							writer.append(tabs + "\t\t"+ val + tabs + ct.get(val) + tabs +
									fraction + n);
						}
					}
				}
			}

			writer.flush();
			}
			
		}
		writer.close();
	}
	
	
	enum BooleanFeatures {
		
		// EDUs
		RIGHT_CHILD_LEAF, LEFT_CHILD_LEAF, NUCLEUS_LEAF, SATELLITE_LEAF,
		
		// same relations or two leaves
		EQUAL_CHILDREN, 
		
		// compare nuclearity to parent relation (left or right or coord)
		RIGHT_CHILD_NUCEQ, LEFT_CHILD_NUCEQ, NUCLEUS_NUCEQ, SATELLITE_NUCEQ,
		
		// general structure
		IS_ROOT;
	}
	
	enum StringFeatures {
		// relations; only relevant, if the corresponding
		// LEAF-feature is 'false'
		RIGHT_CHILD, LEFT_CHILD, NUCLEUS, SATELLITE;
	}

}
