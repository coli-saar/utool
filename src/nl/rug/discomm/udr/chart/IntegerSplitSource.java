package nl.rug.discomm.udr.chart;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nl.rug.discomm.udr.graph.Chain;
import de.saar.chorus.domgraph.chart.RegularTreeGrammar;
import de.saar.chorus.domgraph.chart.Split;
import de.saar.chorus.domgraph.chart.SplitSource;
import de.saar.chorus.domgraph.chart.UnsolvableSubgraphException;

public class IntegerSplitSource extends SplitSource<IntegerNonterminal> {
	Chain chain;
	
	public IntegerSplitSource(Chain chain) {
		super(chain); 
		this.chain = chain; 
	}
	
	@Override
	protected Iterator<Split<IntegerNonterminal>> computeSplits(IntegerNonterminal subgraph)
			throws UnsolvableSubgraphException {
		
		List<Split<IntegerNonterminal>> splits = new ArrayList<Split<IntegerNonterminal>>();
		IntSplitComputer sc = new IntSplitComputer(chain);
		
		for(int root : computePotentialFreeRootsInt(subgraph)) {
			splits.add(sc.computeSplit(root, subgraph));
		}
		
		if( splits.isEmpty() ) {
            throw new UnsolvableSubgraphException();
        }
		
		return splits.iterator();
	}

	
	public IntegerNonterminal makeToplevelSubgraph(Chain graph) {
		return new IntegerNonterminal(1, graph.getLength());
	}
	
	/**
	 * This is only implemented for compatibility reasons
	 */
	@Override
	public IntegerNonterminal makeToplevelSubgraph(Set<String> graph) {
		// TODO doing this properly will eat up lots of time...
	/*	int left = -1, right = -1;
		for(String node : graph) {
			int value = Integer.parseInt(node.replaceAll("\\D", ""));
			if(left == -1) {
				left = value;
			}
			right = Math.max(right, value);
		}
		return new IntegerNonterminal(left,right);*/
		return makeToplevelSubgraph(chain);
	}

	@Override
	public void reduceIfNecessary(RegularTreeGrammar<IntegerNonterminal> chart) {
		// TODO ?

	}
	
	private List<Integer> computePotentialFreeRootsInt(IntegerNonterminal subgraph) {
		List<Integer> ret = new ArrayList<Integer>();
		int left = subgraph.getLeftBorder();
		int right = subgraph.getRightBorder();
		Map<Integer, List<Integer>> domEdges = chain.getAdditionalEdges();
	
		Set<Integer> forbiddenRoots = new HashSet<Integer>();
		for( int i = left; i <= right; i++) {
			if(domEdges.containsKey(i)) {
				for(Integer tgt : domEdges.get(i)) {
					if(tgt <= right &&
							tgt >= left) {
						forbiddenRoots.add(tgt);
						if(tgt > i) {
							// edge pointing "to the right"
							for(int f = i+1; f < tgt; f++) {
								forbiddenRoots.add(f);
							}
						} else {
							// edge pointing "to the left"
							for(int f = tgt +1; f < i;f++) {
								forbiddenRoots.add(f);
							}
						}
					}
				}
			}
		}
		
		for(int i = left; i <= right; i++) {
			if(! forbiddenRoots.contains(i)) {
				ret.add(i);
			}
		}
		
		
		return ret;
	}

	@Override
	protected List<String> computePotentialFreeRoots(IntegerNonterminal subgraph) {
		
		List<String> ret = new ArrayList<String>();
		int left = subgraph.getLeftBorder();
		int right = subgraph.getRightBorder();
		Map<Integer, List<Integer>> domEdges = chain.getAdditionalEdges();
		
		Set<Integer> forbiddenRoots = new HashSet<Integer>();
		for( int i = left; i <= right; i++) {
			if(domEdges.containsKey(i)) {
				for(Integer tgt : domEdges.get(i)) {
					if(tgt <= right &&
							tgt >= left) {
						forbiddenRoots.add(tgt);
						if(tgt > i) {
							// edge pointing "to the right"
							for(int f = i+1; f < tgt; f++) {
								forbiddenRoots.add(f);
							}
						} else {
							// edge pointing "to the left"
							for(int f = tgt +1; f < i;f++) {
								forbiddenRoots.add(f);
							}
						}
					}
				}
			}
		}
		
		for(int i = left; i <= right; i++) {
			if(! forbiddenRoots.contains(i)) {
				ret.add(i + "x");
			}
		}
		
		return ret;
	}
	
	

}