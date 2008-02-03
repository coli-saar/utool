package nl.rug.discomm.udr.modelcheck;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nl.rug.discomm.udr.chart.IntegerChart;
import nl.rug.discomm.udr.graph.Chain;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.EdgeType;
import de.saar.chorus.domgraph.graph.NodeLabels;

public class IntModelCheck {

	private static DomGraph tree;
	private static NodeLabels treeLabels;
	private static Chain chain;
	private static NodeLabels chainlabels;
	
	private static IntegerChart iChart;
	
	private static int leafIndex;
	
	
	public static boolean subsumes(IntegerChart chart1, NodeLabels labels1, 
			IntegerChart chart2, NodeLabels labels2) {
		
		if(chart1.getChainlength() != chart2.getChainlength()) {
	//		System.err.println( "!= chainlength");
			return false;
		} else {
			return matches(chart1.getToplevelSubgraph(), labels1, chart1, -1, 
					chart2.getToplevelSubgraph(), labels2, chart2, -1, new HashSet<List<Integer>>());
		}
		
		
	}
	
	/**
	 * The second argument is the more specific one.
	 * TODO comment me properly.
	 * @param s1
	 * @param l1
	 * @param c1
	 * @param s2
	 * @param l2
	 * @param c2
	 * @return
	 */
	private static boolean matches(IntegerChart.IntSplit s1, NodeLabels l1, IntegerChart c1,
			 IntegerChart.IntSplit s2, NodeLabels l2, IntegerChart c2, Set<List<Integer>> checked) {
		
	//	System.err.println(s1 + " vs" + s2);
		if(! l1.getLabel(s1.getRoot() + "x").equals(l2.getLabel(s2.getRoot() + "x")) ) {
	//		System.err.println("wrong root labels");
			return false;
	
		} else {
	
			
		
			
			if(! matches(s1.getLeftSubgraph(), l1, c1, s1.getRoot() -1, 
					s2.getLeftSubgraph(), l2, c2, s2.getRoot() -1, checked)) {
//				System.err.println("err left");
				return false;
			}
			
			if(! matches(s1.getRightSubgraph(), l1, c1,s1.getRoot(),
					s2.getRightSubgraph(), l2, c2, s2.getRoot(), checked)) {
	//			System.err.println("err right");
				return false;
			}
			
		
		}
		return true;
	}
	
	/**
	 * sg2 is the more special one.
	 * @param sg1
	 * @param l1
	 * @param c1
	 * @param sg2
	 * @param l2
	 * @param c2
	 * @return
	 */
	private static boolean matches(List<Integer> sg1, NodeLabels l1, IntegerChart c1, int i1,
			List<Integer> sg2, NodeLabels l2, IntegerChart c2, int i2, Set<List<Integer>> checked) {
		
		if(checked.contains(sg2)) {
			return true;
		}
	//	System.err.println(sg1 + " vs. " + sg2);
		
		if(sg1.get(0) == 0 && sg2.get(0) == 0) {
	//		System.err.println("Leaf check: ");
			return l1.getLabel(i1 + "y").equals(l2.getLabel(i2 + "y"));
		}
		
		List<IntegerChart.IntSplit> splits2 = c2.getSplitsFor(sg2);
		
		 
		if(splits2 != null) {
			for(IntegerChart.IntSplit split : splits2) {
				IntegerChart.IntSplit match = c1.getSplitWithRoot(sg1, split.getRoot());
				if(match == null) {
			//.err.println("Split missing");
					return false;
				} else {
					if(! matches(split, l1, c1, match, l2, c2, checked)) {
			//			System.err.println("Child split doesn't match");
						return false;
					}
				}
			}

		}
		checked.add(sg2);
		return true;
	}
	
	
	
	public static boolean solves(DomGraph sf, NodeLabels sfl, Chain dg, NodeLabels dgl) {
		
		
		if(! sf.isSolvedForm() ) {
			System.err.println("The solved form is not a tree!");
			return false;
		} else {
			tree = sf;
			treeLabels = sfl;
			chain = dg;
			chainlabels = dgl;
			
			iChart = new IntegerChart(chain.getLength());
			iChart.addDominanceEdges(chain.getAdditionalEdges());
			long t = System.currentTimeMillis();
			iChart.solve();
			System.err.println("For solving: " + (System.currentTimeMillis() - t) );
			
			leafIndex = -1;
			
			String root = "";
			for(String node : tree.getAllRoots()) {
				if(tree.indeg(node) == 0) {
					root = node;
					break;
				}
			}
			return getSubgraphByRoot(root) != null;
		}
		
	}
	
	
	private static List<Integer> getSubgraphByRoot(String root) {
		//System.err.println("next root: " + root);
		List<Integer> ret = new ArrayList<Integer>();
		List<String> holes = tree.getHoles(root);

		int myNumber = 0;
		
		if(holes.isEmpty()) {
			leafIndex++;
			ret.add(0);
			ret.add(0);
	//		System.err.println("leaf!");
			if(chainlabels.getLabel(leafIndex + "y").equals(treeLabels.getLabel(root))) {
				return ret;
			} else {
				return null;
			}

		} else {

			for(int i = 0; i< holes.size(); i++) {
				String hole = holes.get(i);
				String domChild = tree.getChildren(hole, EdgeType.DOMINANCE).get(0);
				if(i == 0) {
					// left Subgraph
					List<Integer> left = 
						getSubgraphByRoot(domChild);
			//		System.err.println("left sg: " + left);
					if(left == null) {
						return null;
					}
					
					if(left.get(0) == 0 &&
							left.get(1) == 0) {
						ret.add(leafIndex +1);
					} else {
						ret.add(left.get(0));
					}
				} else {
					// right Subgraph
					List<Integer> right = 
						getSubgraphByRoot(domChild);
			//		System.err.println("Right sg: " + right);
					if(right == null) {
						return null;
					}
					
					if(right.get(0) == 0 &&
							right.get(1) == 0) {
						myNumber = leafIndex;
						ret.add(leafIndex);
					} else {
						myNumber = right.get(0) - 1;
						ret.add(right.get(1));
					}
				}

			}
			
	//		System.err.println("Checking: " + ret + "(" + myNumber + ")");
			if(iChart.containsSplitWithRoot(ret, myNumber)) {
				
		//		System.err.println("Split there!");
				if(chainlabels.getLabel(myNumber + "x").equals(treeLabels.getLabel(root))) {
			//		System.err.println("label there!");
					return ret;
				}
			}
		}
		
		return null;
		
		
	}
	
}
