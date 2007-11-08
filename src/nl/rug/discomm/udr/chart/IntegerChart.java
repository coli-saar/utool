package nl.rug.discomm.udr.chart;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class IntegerChart {

	Map<List<Integer>, List<IntSplit>> chart;
	Map<Integer, List<Integer>> domEdges;
	int chainlength;
	
	public IntegerChart(int length) {
		chainlength = length;
		chart = new HashMap<List<Integer>,List<IntSplit>>();
		domEdges = new HashMap<Integer, List<Integer>>();
	}
	
	public IntegerChart(int length, Map<Integer, List<Integer>> edges) {
		domEdges = edges;
		chainlength = length;
		chart = new HashMap<List<Integer>,List<IntSplit>>();
	}
	
	
	public void solve() {
		List<Integer> top = new ArrayList<Integer>();
		top.add(1);
		top.add(chainlength);
		computeSplitsForSubgraph(top);
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
						split.rightSub.get(1) + "] }");
			}
			ret.append(System.getProperty("line.separator") + System.getProperty("line.separator"));
		}
		
		return ret.toString();
	}
	
	public void computeProbabilities() {
		
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
