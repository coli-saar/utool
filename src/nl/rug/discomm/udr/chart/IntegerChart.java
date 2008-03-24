package nl.rug.discomm.udr.chart;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.saar.chorus.domgraph.chart.Split;

/**
 * This class is a chart for pure chains and possibly some extra dominance edges, whereby
 * no unsolvable graphs are allowed. The chart's splits may also contain likelihoods.
 * A Split's likelihood is the likelihood of a split given that its subgraph was selected.
 * (Put differently: The likelihood of a split is its relative frequency within the solved
 * forms of its parent subgraph.)
 * 
 * This chart assumes
 *  - canonical naming of its nodes (upper fragments have numbers from 1 to n = the chain's length)
 *  - the graph to solve to be a pure chain with binary fragments and a
 *    non-contradictory, possibly empty set of dominance edges  (unsolvable graphs 
 *    contain contradictory dominance edges)
 *  
 * To solve the chart, the graph itself is not considered. The solving process relies on the
 * canonical naming and the dominance edges given.
 * The architecture is based on the {@link de.saar.chorus.domgraph.Chart} for unrestricted dominance graphs. With
 * the assumptions we can make about this pure chains resp. the graphs arising when adding
 * dominance edges to pure chains, the solving process can be up to 100 times faster.
 * 
 * 
 * @see de.saar.chorus.domgraph.chart.Chart
 * @author Michaela Regneri
 *
 */
public class IntegerChart {

	private Map<List<Integer>, List<IntSplit>> chart;
	private Map<List<Integer>, BigInteger> numSolvedForms;
	private Map<Integer, List<Integer>> domEdges;

	private int chainlength;
	private List<Integer> toplevel;
	
	public IntegerChart(int length) {
		chainlength = length;
		chart = new HashMap<List<Integer>,List<IntSplit>>();
		domEdges = new HashMap<Integer, List<Integer>>();
		toplevel = new ArrayList<Integer>();
		if(length > 0) {
			toplevel.add(1);
			toplevel.add(chainlength);
		}

		numSolvedForms = new HashMap<List<Integer>, BigInteger>();
	}
	
	public IntegerChart(int length, Map<Integer, List<Integer>> edges) {
		domEdges = edges;
		
		chainlength = length;
		chart = new HashMap<List<Integer>,List<IntSplit>>();
		toplevel = new ArrayList<Integer>();
		if(length > 0) {
			toplevel.add(1);
			toplevel.add(chainlength);
		}
		numSolvedForms = new HashMap<List<Integer>, BigInteger>();
	}
	
	public List<Integer> getToplevelSubgraph() {
		return toplevel;
	}
	public void solve() {
		for(List<Integer> de : domEdges.values()) {
			Collections.sort(de);
		}
		computeSplitsForSubgraph(toplevel);
	}
	
	
	public void addDominanceEdges(Map<Integer, List<Integer>> edges) {
		for(Map.Entry<Integer, List<Integer>> pair : edges.entrySet()) {
			for(int tgt : pair.getValue()) {
				addDominanceEdge(pair.getKey(), tgt);
			}
		}
	}
	
	public boolean addDominanceEdge(int src, int tgt) {
		//this assumes that the dominance edge goes out of 
		// the respective hole, i.e. src < tgt -> right hole,
		// src > tgt -> left hole
		
		if(domEdges.containsKey(tgt)) {
			
			if(domEdges.get(tgt).contains(src)) {
				return false;
			} 			
		}
		List<Integer> tgts = domEdges.get(src);
		if(tgts == null) {
			tgts = new ArrayList<Integer>();
			domEdges.put(src, tgts);
		}
		tgts.add(tgt);
	
		restrictSubgraph(toplevel, src, tgt, new HashSet<List<Integer>> ());
		deleteNotReferencedSubgraphs();
		numSolvedForms.clear();
		//initialiseProbabilities();
		return true;
	}
	
	/**
	 * TODO find out how to get the same effect more efficiently
	 */
	private void deleteNotReferencedSubgraphs() {
		Set<List<Integer>> referenced = new HashSet<List<Integer>>();
		storeReferencedGraphs(referenced, toplevel);
		Set<List<Integer>> chartkeys = new HashSet<List<Integer>>(chart.keySet());
		chartkeys.removeAll(referenced);
	//	System.err.println(chartkeys);
		for(List<Integer> subgraph : chartkeys) {
			chart.remove(subgraph);
			//System.err.println("Useless: " + subgraph);
		}
	//	System.err.println(chart);
	}
	
	public List<IntSplit> getAllSplits() {
		
		List<IntSplit> ret = new ArrayList<IntSplit>();
		for(List<IntSplit> list : chart.values()) {
			ret.addAll(list);
		}
		return ret;
	}
	
	private void storeReferencedGraphs(Set<List<Integer>> ref, List<Integer> current) {
		if(! ref.contains(current)) {
			ref.add(current);
			if(chart.containsKey(current)) {
			for(IntSplit split : chart.get(current)) {
				storeReferencedGraphs(ref, split.rightSub);
				storeReferencedGraphs(ref, split.leftSub);
			}
			}
		}
	}
	
	
	public boolean addWeightedDomEdge(int src, int tgt, double weight) {
			restrictSubgraph(toplevel, src, tgt, weight, new HashSet<List<Integer>>());
		return true;
	}
	
	private void restrictSubgraph(List<Integer> subgraph, int src, int tgt,
			double weight, Set<List<Integer>> visited) {
		if(! visited.contains(subgraph)) {

			visited.add(subgraph);
			int left = subgraph.get(0), right = subgraph.get(1);

			if(left <= src && src <= right) {
				if(left <= tgt && tgt <= right) {

					//List<IntSplit> toDelete = new ArrayList<IntSplit>();

					for(int i = 0; i < chart.get(subgraph).size(); i++) {
						IntSplit split = chart.get(subgraph).get(i);
						int root = split.root;
						if(root == tgt) {
							split.setLikelihood(weight);
						} else if((src < root && root < tgt) ||
								(tgt < root && root < src) ) {
							split.setLikelihood(weight);

						} else {
							if(chart.containsKey(split.rightSub)) {
								restrictSubgraph(split.rightSub, src, tgt, weight, visited);
							}
							if(chart.containsKey(split.leftSub)) {
								restrictSubgraph(split.leftSub, src, tgt, weight, visited);
							}
						}
					}


				}

			}
		}
	}
	
	private void restrictSubgraph(List<Integer> subgraph, int src, int tgt, 
			Set<List<Integer>> visited) {
		
		if(! visited.contains(subgraph)) {
		//	System.err.println("Edge: " + src + " --> " + tgt + ", subgraph: " + subgraph);
			visited.add(subgraph);
			int left = subgraph.get(0), right = subgraph.get(1);
			
			if(left <= src && src <= right) {
				if(left <= tgt && tgt <= right) {
				//	System.err.println("Edge in subgraph! " );
					List<IntSplit> toDelete = new ArrayList<IntSplit>();
					
					for(int i = 0; i < chart.get(subgraph).size(); i++) {
						IntSplit split = chart.get(subgraph).get(i);
						int root = split.root;
						if(root == tgt) {
							toDelete.add(split);
					//		System.err.println("Split with root " + root + " in trashbin. (root == tgt)");
						} else if((src < root && root < tgt) ||
								(tgt < root && root < src) ) {
								toDelete.add(split);
						//		System.err.println("Split with root " + root + " in trashbin. (tgt < root < src)");
								//TODO deleteSplit
						} else {
							restrictSubgraph(split.rightSub, src, tgt, visited);
							restrictSubgraph(split.leftSub, src, tgt, visited);
						}
					}
					for(IntSplit del : toDelete) {
							deleteSplit(subgraph,del);
						
						if(del.root != tgt) {
							restrictSubgraph(del.rightSub, src, tgt, visited);
							restrictSubgraph(del.leftSub, src, tgt, visited);
						}
					}
					
				}
			}
		}
	}
	
	private void deleteSplit(List<Integer> subgraph, IntSplit split) {
		double prob = split.likelihood;
		List<IntSplit> old =chart.get(subgraph);
		int del = -1;
		for(int i = 0; i < old.size(); i++) {
			IntSplit current = old.get(i);
			if(current.equals(split)) {
				del = i;
			} else {
				
				current.setLikelihood(
						current.likelihood/(1.0-prob));
			}
		}
		if(del == 0 && old.size() == 1) {
			System.err.println("Can't delete last split!");
		} else {
			old.remove(del);
		}
		
	}
	
	private IntSplit deleteSplit(List<Integer> subgraph, int index) {
		IntSplit split = chart.get(subgraph).get(index);
		double prob = split.likelihood;
		List<IntSplit> old =chart.get(subgraph);
		
		for(int i = 0; i < old.size(); i++) {
			IntSplit current = old.get(i);
			if(i != index) {
				current.setLikelihood(
						(1.0-prob)/current.likelihood);
			}
		}
		// TODO clean that up
		if(index == 0 && old.size() == 1) {
			System.err.println("Can't delete last split!");
			return null;
		} else {
		
			return old.remove(index);
		}
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
						if(tgt <= right &&
								tgt >= left) {
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
						} else if(tgt > right) {
							break;
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
	
	public Set<String> convertToSubgraph(List<Integer> subgraph) {
		Set<String> ret = new HashSet<String>();
		int left = subgraph.get(0), right = subgraph.get(1);
		ret.add( (left-1) + "y");
		for(int i = left; i <= right; i++) {
			ret.add(i + "x");
			ret.add(i + "xl");
			ret.add(i + "xr");
			ret.add(i + "y");
		}
		
		return ret;
	}
	
	
	
	public List<IntSplit> getSplitsFor(List<Integer> subgraph) {
		return chart.get(subgraph);
	}
	
	public boolean containsSplitFor(List<Integer> subgraph) {
		return chart.containsKey(subgraph);
	}
	
	public int getChainlength() {
		return chainlength;
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
					countNumberOfSolvedForms(split.leftSub).multiply(
							countNumberOfSolvedForms(split.rightSub));
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
			
			likelihood = 0.0;
		}
		
		public IntSplit(int root, List<Integer> subgraph, double prob) {
			this.root = root;
			leftBoundary = subgraph.get(0);
			rightBoundary = subgraph.get(1);
			likelihood = prob;
		}
		
		public Split toChartSplit() {
			Split ret = new Split(root + "x");
			
		
			ret.addWcc(root + "xl", convertToSubgraph(leftSub));
			ret.addWcc(root + "xr", convertToSubgraph(rightSub));
			
			
			return ret;
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
		
		public String toString() {
			
			return "[" + leftSub.get(0) + "," + leftSub.get(1) + "] <-- " +
					root + " -->  [" + rightSub.get(0) +
					"," + rightSub.get(1) + "]";
		}
		
		
	}
}
