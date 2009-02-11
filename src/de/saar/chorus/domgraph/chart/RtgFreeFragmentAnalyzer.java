package de.saar.chorus.domgraph.chart;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

public class RtgFreeFragmentAnalyzer<E extends GraphBasedNonterminal> {
	private RegularTreeGrammar<E> rtg;
	private Multimap<String, E> ntsWhereFree;
	private Map<String, Map<String,Integer>> reachability;
	private Set<String> roots;
	
	public RtgFreeFragmentAnalyzer(RegularTreeGrammar<E> rtg) {
		this.rtg = rtg;
		ntsWhereFree = new ArrayListMultimap<String, E>();
		reachability = new HashMap<String, Map<String,Integer>>();
		roots = new HashSet<String>();
	}
	
	public void analyze() {
		Set<E> nts = rtg.getAllNonterminals();
		
		for( E nt : nts ) {
			for( Split<E> split : rtg.getSplitsFor(nt)) {
				ntsWhereFree.put(split.getRootFragment(), nt);
				roots.add(split.getRootFragment());
			}
		}
		
		for( E nt : nts ) {
			for( Split<E> split : rtg.getSplitsFor(nt) ) {
				List<String> dom = split.getAllDominators();
				
				Map<String,Integer> hereReachability = reachability.get(split.getRootFragment());
				if( hereReachability == null ) {
					hereReachability = new HashMap<String, Integer>();
					reachability.put(split.getRootFragment(), hereReachability);
				}
				
				for( int i = 0; i < dom.size(); i++ ) {
					for( E subgraph : split.getWccs(dom.get(i))) {
						for( String node : subgraph.getNodes() ) {
							if( roots.contains(node) ) {
								hereReachability.put(node, i);
							}
						}
					}
				}
			}
		}
	}
	
	public int getReachability(String u, String v) {
		return reachability.get(u).get(v);
	}
	
	public List<E> getSubgraphsWhereFree(String root) {
		return (List<E>) ntsWhereFree.get(root);
	}
	
	/**
	 * Determines whether the roots u and v are co-free. Two roots are called
	 * co-free if F(u) \cap F(v) \neq \emptyset.
	 * 
	 * @param u
	 * @param v
	 * @return
	 */
	public boolean isCoFree(String u, String v) {
		Set<E> subgraphs = new HashSet<E>(getSubgraphsWhereFree(u));
		subgraphs.retainAll(getSubgraphsWhereFree(v));
		
		return ! subgraphs.isEmpty();
	}
}
