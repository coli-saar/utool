package de.saar.chorus.domgraph.chart.wrtg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.rug.discomm.udr.chart.IntegerNonterminal;
import nl.rug.discomm.udr.chart.IntegerSplitSource;
import nl.rug.discomm.udr.graph.Chain;
import de.saar.chorus.domgraph.chart.ChartSolver;
import de.saar.chorus.domgraph.chart.GraphBasedNonterminal;
import de.saar.chorus.domgraph.chart.SolvedFormSpec;
import de.saar.chorus.domgraph.chart.Split;
import de.saar.chorus.domgraph.graph.DomEdge;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.ubench.Ubench;

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
		cost = semiring.one();
		form = new SolvedFormSpec();
		for(E top : grammar.getToplevelSubgraphs()) {
			cost = semiring.mult(cost, computeCostForSubgraph(top));
			form.addAllDomEdges(nonterminalToDomEdges.get(top));
		}
	
		
	}
	
	private T computeCostforSplit(Split<E> split) {
		
		if(! splitToDerivationCost.containsKey(split)) {
		T subgraphProduct = semiring.one();
		
		for(E subgraph : split.getAllSubgraphs()) {
			subgraphProduct = semiring.mult(subgraphProduct, 
					computeCostForSubgraph(subgraph));
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
					T sw = computeCostforSplit(split);
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
	
	public static void main(String[] args) {
		
		for(int i = 60; i <= 90; i++) {
			
			long so = 0, ext = 0;
			Chain chain = new Chain(i);
			NodeLabels labels = new NodeLabels();
			for(String node : chain.getAllNodes()) {
				labels.addLabel(node, node.replaceAll("\\D", "f"));
			}
			WeightedRegularTreeGrammar<IntegerNonterminal, Double> grammar =
				new WeightedRegularTreeGrammar<IntegerNonterminal, Double> (new TropicalSemiring());

			try {
				long time = System.currentTimeMillis();
				ChartSolver.solve(chain, grammar, new IntegerSplitSource(chain));
			
				so =  System.currentTimeMillis() - time;

			//	time = System.currentTimeMillis();


				grammar.addWeightedDomEdge("2xr", "3x", 8.0);
				grammar.addWeightedDomEdge("29xl", "4x", 6.0);
				grammar.addWeightedDomEdge("22xr", "25x", 7.0);
				grammar.addWeightedDomEdge("4xr", "" + (i-5) + "x", 3.0);
				//grammar.addWeightedDomEdge("1xr", "3x", 6);

				


				CheapestSolvedFormComputer<IntegerNonterminal, Double> comp = 
					new CheapestSolvedFormComputer<IntegerNonterminal, Double>(grammar,chain);

				//System.err.println(comp.getCheapestSolvedForm());
				
				time = System.currentTimeMillis();
				SolvedFormSpec spec = comp.getCheapestSolvedForm();
				ext = System.currentTimeMillis() - time;
				
				
				
				System.err.println(i+ "\t\t\t\t" + so + "\t\t\t\t" + ext );
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
}
