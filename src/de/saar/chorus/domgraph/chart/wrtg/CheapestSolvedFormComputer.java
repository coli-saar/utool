package de.saar.chorus.domgraph.chart.wrtg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import de.saar.chorus.domgraph.chart.GraphBasedNonterminal;
import de.saar.chorus.domgraph.chart.SolvedFormSpec;
import de.saar.chorus.domgraph.chart.Split;
import de.saar.chorus.domgraph.graph.DomEdge;
import de.saar.chorus.domgraph.graph.DomGraph;

public class CheapestSolvedFormComputer<E extends GraphBasedNonterminal, T extends Comparable<T>> {
	
	private WeightedRegularTreeGrammar<E, T> grammar;
	private DomGraph graph;
	private T cost;
	private SolvedFormSpec form;
	private Map<E,T> subgraphToCost;
	private Map<Split<E>,T> splitToDerivationCost;
	private Semiring<T> semiring;
	private Map<E, String> root;
	private Map<E, List<DomEdge> > nonterminalToDomEdges;
	private List<DomEdge> domedges;
	private PriorityQueue<SubgraphHeapMember<E,T>> queue;
	private SubgraphHeapMember<E,T> sink;
	private Map<E, SubgraphHeapMember<E,T>> ntToSHM;
	
	private Map<Split<E>,Integer> splitToCounter;
	
	public CheapestSolvedFormComputer(WeightedRegularTreeGrammar<E,T> grammar, DomGraph graph) {
		this.grammar = grammar;
		this.graph = graph;
		subgraphToCost = new HashMap<E,T>();
		splitToDerivationCost = new HashMap<Split<E>,T>();
		semiring = grammar.getSemiring();
		root = new HashMap<E, String>();
		domedges = new ArrayList<DomEdge>();
		nonterminalToDomEdges = new HashMap<E, List<DomEdge>>();
		queue = new PriorityQueue<SubgraphHeapMember<E,T>>();
		sink = new SubgraphHeapMember<E, T>(null, semiring.one());
		ntToSHM = new HashMap<E,SubgraphHeapMember<E,T>>();
		splitToCounter = new HashMap<Split<E>, Integer>();
		queue.add(sink);
	}
	
	
	
	
	
	public SolvedFormSpec getCheapestSolvedForm() {
		
		
		
		if(form == null) {
			computeCheapestForm();
		}
		return form;
	}
	
	public T getCost() {
		if(cost == null) {
			computeCheapestForm();
		}
		return cost;
	}
	
	private void computeCheapestForm() {
		
		long time = System.currentTimeMillis();
		
		for(E nt : grammar.getAllNonterminals()) {
			
			SubgraphHeapMember<E,T> shm = 
				new SubgraphHeapMember<E,T>(nt,semiring.maxElement());
			ntToSHM.put(nt, shm);
		}
		
		//System.err.println(ntToSHM.size());
		//System.err.println(System.currentTimeMillis() - time);
		time = System.currentTimeMillis();
		for(E nt: grammar.getAllNonterminals()) {
			SubgraphHeapMember<E,T> shm = ntToSHM.get(nt);
			
			
			if(grammar.isFinal(nt)) {
				
				Split<E> singleton = grammar.getSplitsFor(nt).get(0);
				splitToCounter.put(singleton, 0);
				sink.rules.put(ntToSHM.get(nt), singleton);
				splitToDerivationCost.put(singleton, grammar.getWeightForSplit(singleton));
			} else {
				for(Split<E> split : grammar.getSplitsFor(nt)) {
					splitToDerivationCost.put(split, grammar.getWeightForSplit(split));
					List<E> children = split.getAllSubgraphs();
					
					splitToCounter.put(split, children.size());
					for(E sg : children) {
						ntToSHM.get(sg).rules.put(shm, split);
						
					}
				}
			}
		}
		//System.err.println(System.currentTimeMillis() - time);
		time = System.currentTimeMillis();
		long count = 0;
		
		
		
		while(! queue.isEmpty() ) {
			count++;
			SubgraphHeapMember<E, T> shm = 
				queue.remove();
			
			
			for(Map.Entry<SubgraphHeapMember<E,T>, Split<E>> rule : shm.rules.entrySet()) {
				Split<E> parent = rule.getValue();
				T scost = splitToDerivationCost.get(parent);
				T ntcost = rule.getKey().key;
			
				if(scost.compareTo(ntcost) < 0) {
					splitToDerivationCost.remove(parent);
					T ncost = semiring.mult(scost, shm.key);
					splitToDerivationCost.put(parent, ncost);
					int old = splitToCounter.get(parent);
					old--;
					splitToCounter.remove(parent);
					splitToCounter.put(parent, old);
					if(old <= 0) {
				
						if(ncost.compareTo(ntcost) < 0) {
							
							if(ntcost.compareTo(semiring.maxElement()) == 0) {
								
								rule.getKey().key = ncost;
								queue.add(rule.getKey());
								rule.getKey().root = parent.getRootFragment();
								List<DomEdge> edgestore = rule.getKey().domedges;
								for(String hole: parent.getAllDominators()) {
									for(E wcc : parent.getWccs(hole)) {
										edgestore.addAll(ntToSHM.get(wcc).domedges);
										edgestore.add(new DomEdge(hole,ntToSHM.get(wcc).root ));
									}
								}
							} else {
								queue.remove(rule.getKey());
								rule.getKey().key = ncost;
								queue.add(rule.getKey());
								rule.getKey().root = parent.getRootFragment();
								//TODO store domedges
								List<DomEdge> edgestore = rule.getKey().domedges;
								for(String hole: parent.getAllDominators()) {
									for(E wcc : parent.getWccs(hole)) {
										edgestore.addAll(ntToSHM.get(wcc).domedges);
										edgestore.add(new DomEdge(hole,ntToSHM.get(wcc).root ));
									}
								}
							}
						}
					}
				}
			}
		}
		int useless = 0;
		
		for( SubgraphHeapMember<E,T> shm : ntToSHM.values()) {
			if(shm.key.compareTo(semiring.maxElement()) == 0) {
				useless++;
			}
		}
		
	//	System.err.println(useless);
	//	System.err.println((System.currentTimeMillis() - time) + "ms\t\t" + count);
		
		cost = semiring.one();
		form = new SolvedFormSpec();
		List<DomEdge> domedges = new ArrayList<DomEdge>();
		for(E top : grammar.getToplevelSubgraphs()) {
			SubgraphHeapMember<E,T> shm = ntToSHM.get(top);
			cost = semiring.mult(shm.key,cost);
			
			//extractDomEdges(shm, domedges);
			form.addAllDomEdges(shm.domedges);
			
		}
		
	//	System.err.println(form); 
		
		/*cost = semiring.one();
		form = new SolvedFormSpec();
		for(E top : grammar.getToplevelSubgraphs()) {
			
			cost = semiring.mult(cost, computeCostForSubgraph(top));
			
			form.addAllDomEdges(nonterminalToDomEdges.get(top));
		}
		System.err.println(form);*/
	}
	
	private void extractDomEdges(SubgraphHeapMember<E,T> shm, List<DomEdge> edges) {
		String root = shm.root;
		
		for(Split<E> split : grammar.getSplitsFor(shm.nonterminal)) {
			if(split.getRootFragment().equals(root)) {
				for(String dom : split.getAllDominators()) {
					for(E sg : split.getWccs(dom)) {
						SubgraphHeapMember<E,T> child = ntToSHM.get(sg);
						edges.add(new DomEdge(dom, child.root));
						extractDomEdges(child, edges);
					}
				}
				break;
			}
		}
	}
	
	private T computeCostforSplit(Split<E> split, T limit) {
		
		if(! splitToDerivationCost.containsKey(split)) {
			
			
		T subgraphProduct = semiring.one();
		
		for(E subgraph : split.getAllSubgraphs()) {
			
			T sgcost = computeCostForSubgraph(subgraph);
			if(sgcost.compareTo(limit) > 0) {
				return null;
			}
			
			subgraphProduct = semiring.mult(subgraphProduct, 
					sgcost);
			if(subgraphProduct.compareTo(limit) > 0) {
				return null;
			}
		}
		
		T result = semiring.mult(grammar.getWeightForSplit(split), 
				subgraphProduct);
		splitToDerivationCost.put(split, result);
		
		
		}
		return splitToDerivationCost.get(split);
	}
	
	private T computeCostForSubgraph(E subgraph) {

		if(! subgraphToCost.containsKey(subgraph)) {
			
			List<DomEdge> des = new ArrayList<DomEdge>();
			T ret = semiring.zero();
			Split<E> recall = null;
			if(grammar.containsSplitFor(subgraph)) {
				for(Split<E> split : grammar.getSplitsFor(subgraph)) {
					
					T sw = computeCostforSplit(split,ret);
					
					if(sw == null) {
						if(recall == null) {
							recall = split;
						}
						continue;
					}
					
					if( (recall == null) || 
							(sw.compareTo(ret)  < 0)) {
						ret = sw;
						recall = split;
					}
					
					
				}


				root.put(subgraph, recall.getRootFragment());
				
				for(String dom : recall.getAllDominators()) {
					for(E sg : recall.getWccs(dom)) {
					
						des.add(new DomEdge(dom, root.get(sg)));
						if(nonterminalToDomEdges.containsKey(sg)) {
							des.addAll(nonterminalToDomEdges.get(sg));
						}
					}
				}
				
				nonterminalToDomEdges.put(subgraph, des);
			} else {
				// TODO what does it mean if there is no split in the RTG?
				ret = semiring.one();
				
				
			}
			subgraphToCost.put(subgraph,ret);
			return ret;
		}
		return subgraphToCost.get(subgraph);
	}
	
	
	
	
	private class SubgraphHeapMember<E extends GraphBasedNonterminal, T extends Comparable<T>> 
			implements Comparable<SubgraphHeapMember<E,T>> {
		private E nonterminal;
		private T key;
		private Map<SubgraphHeapMember<E,T>, Split<E>> rules;
		private Split<E> preferred;
		
		private List<DomEdge> domedges;
		private String root;
		
		
		SubgraphHeapMember(E nt, T k) {
			super();
			nonterminal = nt;
			key =k;
			rules = new HashMap<SubgraphHeapMember<E,T>, Split<E>>();
			preferred = null;
			domedges = new ArrayList<DomEdge>();
			root = "";
		}
		
		
		
		public Split<E> getBestSplit() {
			return preferred;
		}
		
		public void setBestSplit(Split<E> split) {
			preferred = split;
		}
		
		public void setKey(T nk) {
			key = nk;
		}
		
		public E getNonterminal() {
			return nonterminal;
		}


		public int compareTo(SubgraphHeapMember<E, T> o) {
			
			return this.key.compareTo(o.key);
		}


		@Override
		public boolean equals(Object obj) {
			if(! (obj instanceof SubgraphHeapMember) ) {
				return false;
			}
			
			SubgraphHeapMember<E,T> co = (SubgraphHeapMember<E,T>)obj;
			
			
			return co.compareTo(this) == 0 && co.nonterminal.equals(this.nonterminal);
		}


		public int hashCode() {
			
			return nonterminal.hashCode() * key.hashCode();
		}
		
		
		
	}

}
