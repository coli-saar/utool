package nl.rug.discomm.udr.chart;

import java.util.List;
import java.util.Map;

import nl.rug.discomm.udr.graph.Chain;
import de.saar.chorus.domgraph.chart.Split;
import de.saar.chorus.domgraph.chart.SplitComputer;

public class IntSplitComputer extends SplitComputer<IntegerNonterminal> {


	
	public IntSplitComputer(Chain graph) {
		super(graph);
	}
	
	
	public Split<IntegerNonterminal> computeSplit(int root, IntegerNonterminal subgraph) {
		
		IntegerSplit split = new IntegerSplit(root);
		
		int l = subgraph.getLeftBorder();
		int r = subgraph.getRightBorder();
		
		IntegerNonterminal left, right;
		
		if(l == root) {
			left = new IntegerNonterminal(root-1);
		}  else {
			left = new IntegerNonterminal(l,root-1);
		}
		
		if(r == root) {
			right = new IntegerNonterminal(root);
		} else {
			right = new IntegerNonterminal(root +1, r);
		}
		
		split.setLeftWcc(left);
		split.setRightWcc(right);
		return split;
	
	}
	
	
	/**
	 * This assumes the root to be free !
	 */
	@Override
	public Split<IntegerNonterminal> computeSplit(String root, IntegerNonterminal subgraph) {
		
		int iRoot = Integer.parseInt(root.replaceAll("\\D",""));
		
			Split<IntegerNonterminal> split = new Split<IntegerNonterminal>(root);
			
			int l = subgraph.getLeftBorder();
			int r = subgraph.getRightBorder();
			
			IntegerNonterminal left, right;
			
			if(l == iRoot) {
				left = new IntegerNonterminal(iRoot-1);
			}  else {
				left = new IntegerNonterminal(l,iRoot-1);
			}
			
			if(r == iRoot) {
				right = new IntegerNonterminal(iRoot);
			} else {
				right = new IntegerNonterminal(iRoot +1, r);
			}
			
			split.addWcc(root + "l", left);
			split.addWcc(root + "r", right);
			return split;
		
	}
	

	
	@Override
	protected IntegerNonterminal createEmptyNonterminal() {
		
		return new IntegerNonterminal(0,0);
	}

}
