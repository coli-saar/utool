package nl.rug.discomm.udr.modelcheck;

import java.util.ArrayList;
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
