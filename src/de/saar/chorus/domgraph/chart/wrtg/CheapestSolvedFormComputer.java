package de.saar.chorus.domgraph.chart.wrtg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nl.rug.discomm.udr.graph.Chain;
import de.saar.chorus.domgraph.chart.ChartSolver;
import de.saar.chorus.domgraph.chart.CompleteSplitSource;
import de.saar.chorus.domgraph.chart.GraphBasedNonterminal;
import de.saar.chorus.domgraph.chart.SolvedFormSpec;
import de.saar.chorus.domgraph.chart.Split;
import de.saar.chorus.domgraph.chart.SubgraphNonterminal;
import de.saar.chorus.domgraph.graph.DomEdge;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.ubench.Ubench;

public class CheapestSolvedFormComputer<E extends GraphBasedNonterminal, T> {
	
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
	
	
	public CheapestSolvedFormComputer(WeightedRegularTreeGrammar<E,T> grammar, DomGraph graph) {
		this.grammar = grammar;
		this.graph = graph;
		subgraphToCost = new HashMap<E,T>();
		splitToDerivationCost = new HashMap<Split<E>,T>();
		semiring = grammar.getSemiring();
		root = new HashMap<E, String>();
		domedges = new ArrayList<DomEdge>();
		nonterminalToDomEdges = new HashMap<E, List<DomEdge>>();
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
		cost = semiring.getProductIdentityElement();
		form = new SolvedFormSpec();
		for(E top : grammar.getToplevelSubgraphs()) {
			cost = semiring.semiringProduct(cost, computeCostForSubgraph(top));
			form.addAllDomEdges(nonterminalToDomEdges.get(top));
		}
	
		
	}
	
	private T computeCostforSplit(Split<E> split) {
		
		if(! splitToDerivationCost.containsKey(split)) {
		T subgraphProduct = semiring.getProductIdentityElement();
		
		for(E subgraph : split.getAllSubgraphs()) {
			subgraphProduct = semiring.semiringProduct(subgraphProduct, 
					computeCostForSubgraph(subgraph));
		}
		
		T result = semiring.semiringProduct(grammar.getWeightForSplit(split), 
				subgraphProduct);
		splitToDerivationCost.put(split, result);
		
		
		}
		return splitToDerivationCost.get(split);
	}
	
	private T computeCostForSubgraph(E subgraph) {

		if(! subgraphToCost.containsKey(subgraph)) {
			
			List<DomEdge> des = new ArrayList<DomEdge>();
			T ret = semiring.getSumIdentityElement();
			Split<E> recall = null;
			if(grammar.containsSplitFor(subgraph)) {
				for(Split<E> split : grammar.getSplitsFor(subgraph)) {
					T sw = computeCostforSplit(split);
					if( (recall == null) || 
							(semiring.compare(sw, ret) < 0)) {
						ret = sw;
						recall = split;
					}
				}


				root.put(subgraph, recall.getRootFragment());
				
				for(String dom : recall.getAllDominators()) {
					for(E sg : recall.getWccs(dom)) {
						System.err.println(dom + " --> " + root.get(sg));
						des.add(new DomEdge(dom, root.get(sg)));
						if(nonterminalToDomEdges.containsKey(sg)) {
							des.addAll(nonterminalToDomEdges.get(sg));
						}
					}
				}
				
				nonterminalToDomEdges.put(subgraph, des);
			} else {
				// TODO what does it mean if there is no split in the RTG?
				ret = semiring.getBestCost();
				root.put(subgraph, subgraph.getRootIfSingleton());
			}
			subgraphToCost.put(subgraph,ret);
			return ret;
		}
		return subgraphToCost.get(subgraph);
	}
	
	public static void main(String[] args) {
		Chain chain = new Chain(3);
		NodeLabels labels = new NodeLabels();
		for(String node : chain.getAllNodes()) {
			labels.addLabel(node, node.replaceAll("\\D", "f"));
		}
		WeightedRegularTreeGrammar<SubgraphNonterminal, Integer> grammar =
			new WeightedRegularTreeGrammar<SubgraphNonterminal, Integer> (new TropicalSemiring());
	
		try {
		ChartSolver.solve(chain, grammar, new CompleteSplitSource(chain));
		System.err.println(grammar.toString());
			grammar.addWeightedDomEdge("2xr", "3x", 8);
			grammar.addWeightedDomEdge("3xl", "2x", 6);
			grammar.addWeightedDomEdge("1xr", "2x", 2);
			
		
		
		CheapestSolvedFormComputer<SubgraphNonterminal, Integer> comp = 
			new CheapestSolvedFormComputer<SubgraphNonterminal, Integer>(grammar,chain);
		
		//System.err.println(comp.getCheapestSolvedForm());
		Ubench u = Ubench.getInstance();
		u.addJDomGraphTab("cheapest form?", chain.makeSolvedForm(comp.getCheapestSolvedForm()), labels);
		
		
		System.err.println(comp.getCost());
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
