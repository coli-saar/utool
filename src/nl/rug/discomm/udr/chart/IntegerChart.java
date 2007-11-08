package nl.rug.discomm.udr.chart;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nl.rug.discomm.udr.structurecheck.Utilities;

public class IntegerChart {

	Map<List<Integer>, List<IntSplit>> chart;
	Map<List<Integer>, BigInteger> numSolvedForms;
	Map<Integer, List<Integer>> domEdges;
	int chainlength;
	List<Integer> toplevel;
	
	public IntegerChart(int length) {
		chainlength = length;
		chart = new HashMap<List<Integer>,List<IntSplit>>();
		domEdges = new HashMap<Integer, List<Integer>>();
		toplevel = new ArrayList<Integer>();
		toplevel.add(1);
		toplevel.add(chainlength);
		numSolvedForms = new HashMap<List<Integer>, BigInteger>();
	}
	
	public IntegerChart(int length, Map<Integer, List<Integer>> edges) {
		domEdges = edges;
		chainlength = length;
		chart = new HashMap<List<Integer>,List<IntSplit>>();
		toplevel = new ArrayList<Integer>();
		toplevel.add(1);
		toplevel.add(chainlength);
		numSolvedForms = new HashMap<List<Integer>, BigInteger>();
	}
	
	
	public void solve() {
		
		computeSplitsForSubgraph(toplevel);
	}
	
	
	private void computeSplitsForSubgraph(List<Integer> subgraph) {

		if(! chart.containsKey(subgraph)) {
			List<IntSplit> splits = new ArrayList<IntSplit>();
			chart.put(subgraph, splits);

			int left = subgraph.get(0), right = subgraph.get(1);

			Set<Integer> forbiddenRoots = new HashSet<Integer>();
			for( int i = left; i <= right; i++) {
				if(domEdges.containsKey(i)) {
					for(Integer tgt : domEdges.get(i)) {
						forbiddenRoots.add(tgt);
						if(tgt > i) {
							for(int f = i+1; f < tgt; f++) {
								forbiddenRoots.add(f);
							}
						} else {
							for(int f = tgt +1; f < i;f++) {
								forbiddenRoots.add(f);
							}
						}
					}
				}
			}
			
			for(int i = left; i <= right; i++) {
				if(! forbiddenRoots.contains(i)) {
					IntSplit split = new IntSplit(i, subgraph);
					splits.add(split);
					if(left < i ) {
						List<Integer> leftsg = new ArrayList<Integer>();
						leftsg.add(left);
						leftsg.add(i-1);
						computeSplitsForSubgraph(leftsg);
					}

					if( i < right  ) {
						List<Integer> rightsg = new ArrayList<Integer>();
						rightsg.add(i+1);
						rightsg.add(right);
						computeSplitsForSubgraph(rightsg);
					}
				}
			}

		}
	}
	
	public String toString() {
		StringBuffer ret = new StringBuffer();
		
		for(Map.Entry<List<Integer>, List<IntSplit>> pair : chart.entrySet()) {
			ret.append("[" + pair.getKey().get(0) + ", " + 
					pair.getKey().get(1) + "]  --> ");
			ret.append(System.getProperty("line.separator"));
			for( IntSplit split : pair.getValue() ) {
				ret.append("{ " + "[" + split.leftSub.get(0) + ", " + 
					split.leftSub.get(1) + "] <--" + split.root + "-->  [" + split.rightSub.get(0) + ", " + 
						split.rightSub.get(1) + "] } -- " + 
						split.likelihood);
			}
			ret.append(System.getProperty("line.separator") + System.getProperty("line.separator"));
		}
		
		return ret.toString();
	}
	
	
	public BigInteger countNumberOfSolvedForms() {
		return countNumberOfSolvedForms(toplevel);
	}
	
	
	private BigInteger countNumberOfSolvedForms(List<Integer> subgraph) {
		if(numSolvedForms.containsKey(subgraph)) {
			return numSolvedForms.get(subgraph);
		}
		
		int left = subgraph.get(0), right = subgraph.get(1);
		BigInteger ret;
		if(left == right) {
			
			ret =  BigInteger.ONE;
		} else {
			ret = BigInteger.ZERO;
			for( IntSplit split : chart.get(subgraph) ) {
				
				ret = ret.add(countNumberOfSolvedForms(split.rightSub)
						.multiply(countNumberOfSolvedForms(split.leftSub)));
			}
			
		}
		numSolvedForms.put(subgraph, ret);
		return ret;
	}
	
	
	public void initialiseProbabilities() {
		
		for(List<Integer> subgraph: chart.keySet()) {
			BigDecimal all = BigDecimal.ZERO;
			Map<IntSplit, BigInteger> solvedForms = new HashMap<IntSplit, BigInteger>();
			for(IntSplit split : chart.get(subgraph)) {
				BigInteger sfs =
					Utilities.catalanNumber(split.root - split.leftBoundary).multiply(
							Utilities.catalanNumber(split.rightBoundary - split.root));
				solvedForms.put(split,sfs);
				all = all.add(new BigDecimal(sfs));
				
					
			}
			
			for(Map.Entry<IntSplit, BigInteger> splitToForms : solvedForms.entrySet()) {
				splitToForms.getKey().setLikelihood(
						new BigDecimal(splitToForms.getValue()).divide(all, 4, BigDecimal.ROUND_HALF_EVEN).doubleValue());
			}
			
		}
	}
	
	public class IntSplit {
		
		private double likelihood;
		private int root, leftBoundary, rightBoundary;
		List<Integer> leftSub, rightSub;
		
		
		public IntSplit(int root, List<Integer> subgraph) {
			this.root = root;
			leftBoundary = subgraph.get(0);
			rightBoundary = subgraph.get(1);
			
			rightSub = new ArrayList<Integer>();
			if(rightBoundary == root) {
				rightSub.add(0);
				rightSub.add(0);
			} else {
				rightSub.add(root+1);
				rightSub.add(rightBoundary);
			}
			
			leftSub = new ArrayList<Integer>();
			if(leftBoundary == root) {
				leftSub.add(0);
				leftSub.add(0);
			} else {
				leftSub.add(leftBoundary);
				leftSub.add(root-1);
			}
			
			likelihood = 1;
		}
		
		public IntSplit(int root, List<Integer> subgraph, double prob) {
			this.root = root;
			leftBoundary = subgraph.get(0);
			rightBoundary = subgraph.get(1);
			likelihood = prob;
		}
		
		public List<Integer> getLeftSubgraph() {
			return leftSub;
		}
		
		public List<Integer> getRightSubgraph() {
			return rightSub;
		}

		public double getLikelihood() {
			return likelihood;
		}

		public void setLikelihood(double likelihood) {
			this.likelihood = likelihood;
		}

		public int getRoot() {
			return root;
		}

		public void setRoot(int root) {
			this.root = root;
		}
		
		
		
	}
}
