package de.saar.chorus.domgraph.chart.wrtg;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.saar.chorus.domgraph.chart.GraphBasedNonterminal;
import de.saar.chorus.domgraph.chart.RegularTreeGrammar;
import de.saar.chorus.domgraph.chart.Split;

public class WeightedRegularTreeGrammar<E extends GraphBasedNonterminal,T> extends RegularTreeGrammar<E> {
		private Semiring<T> semiring;
		private Map<Split<E>, T> weights;
		
		public WeightedRegularTreeGrammar(Semiring<T> s) {
			semiring = s;
			weights = new HashMap<Split<E>, T>();
		}
		
		/**
		 * TODO what to do if this split has a weight already?
		 * @param s
		 * @param weight
		 * @return
		 */
		public boolean setWeightForSplit(Split<E> s, T weight) {
			if(! semiring.isInDomain(weight)) {
				return false;
			}
			weights.put(s, weight);
			return true;
		}
		
		public T getWeightForSplit(Split<E> split) {
			if(weights.containsKey(split)) {
				return weights.get(split);
			} else {
				return semiring.getBestCost();
			}
			
		}
		
		public Semiring<T> getSemiring() {
			return semiring;
		}
		
		public void addSplit(E subgraph, Split<E> split, T weight) {
			 super.addSplit(subgraph, split);
			 weights.put(split, weight);
		}
		
		public boolean addWeightedDomEdge(String src, String tgt, T weight) {
			for(E subgraph : getToplevelSubgraphs()) {
				recRestrictSubgraph(subgraph, src, tgt, weight, new HashSet<E>());
			}

			return true;
		}
		
		private void recRestrictSubgraph(E subgraph, String src, String tgt, T weight, Set<E> visited) {
			System.err.println(src + " --> " + tgt + "(" + subgraph + ")");
			if(! visited.contains(subgraph)) {
				visited.add(subgraph);
				if(subgraph.getNodes().contains(src) &&
						subgraph.getNodes().contains(tgt)) {
					for(Split<E> split : chart.get(subgraph)) {
						if(split.getRootFragment().equals(tgt)) {
							System.err.println(split + " " + weight);
							setWeightForSplit(split, weight);
						} else {
							for(E wcc : split.getAllSubgraphs()) {
								if(wcc.getNodes().contains(src) ) {
									if(! wcc.getNodes().contains(tgt)) {
										setWeightForSplit(split, weight);
									} else {
										recRestrictSubgraph(wcc, src, tgt, weight, visited);
									}
								}
							}
						}
					}
				}
			}
		}
}
