package de.saar.chorus.domgraph.chart.wrtg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.rug.discomm.udr.chart.IntegerNonterminal;
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

public class CheapestSolvedFormComputer<E extends GraphBasedNonterminal, T> {
	
	private WeightedRegularTreeGrammar<E, T> grammar;
	private DomGraph graph;
	private T cost;
	private SolvedFormSpec form;
	private Map<E,T> subgraphToCost;
	private Map<Split<E>,T> splitToDerivationCost;
	private Semiring<T> semiring;
	private Map<E, String> root;
	private List<DomEdge> domedges;
	
	public CheapestSolvedFormComputer(WeightedRegularTreeGrammar<E,T> grammar, DomGraph graph) {
		this.grammar = grammar;
		this.graph = graph;
		subgraphToCost = new HashMap<E,T>();
		splitToDerivationCost = new HashMap<Split<E>,T>();
		semiring = grammar.getSemiring();
		root = new HashMap<E, String>();
		domedges = new ArrayList<DomEdge>();
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
		for(E top : grammar.getToplevelSubgraphs()) {
			cost = semiring.semiringProduct(cost, computeCostForSubgraph(top));
		}
		form = new SolvedFormSpec();
		form.addAllDomEdges(domedges);
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
						domedges.add(new DomEdge(dom, root.get(sg)));
					}
				}
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
			grammar.addWeightedDomEdge("1xr", "3x", 6);
			
		
		
		CheapestSolvedFormComputer<SubgraphNonterminal, Integer> comp = 
			new CheapestSolvedFormComputer<SubgraphNonterminal, Integer>(grammar,chain);
		
		System.err.println(comp.getCheapestSolvedForm());
		System.err.println(comp.getCost());
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
