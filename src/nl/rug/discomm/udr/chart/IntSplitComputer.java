package nl.rug.discomm.udr.chart;

import java.util.List;
import java.util.Map;

import nl.rug.discomm.udr.graph.Chain;
import de.saar.chorus.domgraph.chart.Split;
import de.saar.chorus.domgraph.chart.SplitComputer;

public class IntSplitComputer extends SplitComputer<IntegerNonterminal> {

	private Map<Integer, List<Integer>> domEdges;
	
	public IntSplitComputer(Chain graph) {
		super(graph);
		domEdges = graph.getAdditionalEdges();
	}
	
	@Override
	public Split<IntegerNonterminal> computeSplit(String root, IntegerNonterminal subgraph) {
		
		int iRoot = Integer.parseInt(root.replaceAll("\\D",""));
		if(isFree(iRoot, subgraph)) {
			Split<IntegerNonterminal> split = new Split<IntegerNonterminal>(root);
			
			int l = subgraph.getLeftBorder();
			int r = subgraph.getRightBorder();
			
			IntegerNonterminal left, right;
			
			if(l == iRoot) {
				left = new IntegerNonterminal(0,0);
			}  else {
				left = new IntegerNonterminal(l,iRoot-1);
			}
			
			if(r == iRoot) {
				right = new IntegerNonterminal(0,0);
			} else {
				right = new IntegerNonterminal(iRoot +1, r);
			}
			
			split.addWcc(root + "l", left);
			split.addWcc(root + "r", right);
			return split;
		} else {
			return null;
		}
	}
	
	private boolean isFree(int root, IntegerNonterminal subgraph) {
		
		int left = subgraph.getLeftBorder();
		int right = subgraph.getRightBorder();
		
		for( int i = left; i <= right; i++) {
			if(domEdges.containsKey(i)) {
				for(Integer tgt : domEdges.get(i)) {
					if( tgt <= right) {
						if( root == tgt) {
							return false;
						}
						if(tgt > i) {
							if(root > i && root < tgt) {
								return false;
							}
							
						} else {
							
							if(root > tgt && root < i) {
								return false;
							}
							
						}
					}
				}
			}
		}
		
		return true;
	}
	
	@Override
	protected IntegerNonterminal createEmptyNonterminal() {
		
		return new IntegerNonterminal(0,0);
	}

}
