package nl.rug.discomm.udr.chart;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.saar.chorus.domgraph.chart.SolvedFormSpec;
import de.saar.chorus.domgraph.chart.wrtg.Semiring;
import de.saar.chorus.domgraph.graph.DomEdge;


/**
 * TODO 
 * * make an Interface that covers both kinds of conf.-extraction
 * * parametrize the weight type of IntSplits
 * * take the weight out of IntSplits, and take the weight out of the generalized IntegerChart at all
 * * make IntegerChart Implement RegularTreeGrammar
 * * Implement this class using Semirings
 * * check runtime and write a happy E-Mail to Manfred Stede ;))
 * 
 * @author Michaela Regneri
 *
 */
public class IntegerCheapestSolvedFormComputer {
	private Semiring<Double> semiring;
	private IntegerChart chart;
	private Map<IntegerChart.IntSplit, Double> splitToDerivationCost;
	private Map<List<Integer>, Double> subgraphToCost;
	private Map<List<Integer>, Integer> root;
	private Map<List<Integer>, List<DomEdge> > nonterminalToDomEdges;
	
	private SolvedFormSpec form;
	private Double cost;
	
	public IntegerCheapestSolvedFormComputer(IntegerChart chart, Semiring<Double> sr) {
		semiring = sr;
		this.chart = chart;
		splitToDerivationCost = new HashMap<IntegerChart.IntSplit, Double>();
		subgraphToCost = new HashMap<List<Integer>, Double>();
		root = new HashMap<List<Integer>, Integer>();
		nonterminalToDomEdges = new HashMap<List<Integer>, List<DomEdge>>();
	}
	
	private void computeCheapestForm() {
		cost = semiring.one();
		form = new SolvedFormSpec();
		List<Integer> top = chart.getToplevelSubgraph();

		cost = semiring.mult(cost, computeCostForSubgraph(top));

		form.addAllDomEdges(nonterminalToDomEdges.get(top));

	//	System.err.println(form);
	}
	
	public SolvedFormSpec getCheapestSolvedForm() {

		if(form == null) {
			computeCheapestForm();
		}
		return form;
	}

	public Double getCost() {
		if(cost == null) {
			computeCheapestForm();
		}
		return cost;
	}
	
	private Double computeCostforSplit(IntegerChart.IntSplit split, Double limit) {

		if(! splitToDerivationCost.containsKey(split)) {


			Double subgraphProduct = semiring.one();

			List<Integer> rsub = split.rightSub;
			List<Integer> lsub = split.leftSub;

			Double sgcost = computeCostForSubgraph(lsub);
			if(sgcost.compareTo(limit) > 0) {
				return null;
			} else {
				subgraphProduct = semiring.mult(subgraphProduct, sgcost);
			}

			if(subgraphProduct.compareTo(limit) > 0) {
				return null;
			} else {
				sgcost = computeCostForSubgraph(rsub);
				if(sgcost.compareTo(limit) > 0) {
					return null;
				} else {
					subgraphProduct = semiring.mult(subgraphProduct, sgcost);
				}
			}



			Double result = semiring.mult(split.getLikelihood(), 
					subgraphProduct);
			splitToDerivationCost.put(split, result);


		}
		return splitToDerivationCost.get(split);
	}
	
	private Double computeCostForSubgraph(List<Integer> subgraph) {

		if(! subgraphToCost.containsKey(subgraph)) {
			
			List<DomEdge> des = new ArrayList<DomEdge>();
			Double ret = semiring.zero();
			IntegerChart.IntSplit recall = null;
			if(chart.containsSplitFor(subgraph)) {
				for(IntegerChart.IntSplit split : chart.getSplitsFor(subgraph)) {
					
				/*	if(recall != null &&
							ret < split.getLikelihood()) {
						continue;
					} */
					
					Double sw = computeCostforSplit(split,ret);
					
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


				int theRoot = recall.getRoot();
				root.put(subgraph, theRoot);
				List<Integer> left = recall.leftSub;
				List<Integer> right = recall.rightSub;
				
				if(left.get(0) == 0 && left.get(1) == 0) {
					des.add(new DomEdge(theRoot + "xl", (theRoot -1) + "y"));
				} else {
					des.add(new DomEdge(theRoot + "xl", root.get(left) + "x"));
				}
				
				
				if(nonterminalToDomEdges.containsKey(left)) {
					des.addAll(nonterminalToDomEdges.get(left));
				}
				
				if(right.get(0) == 0 && right.get(1) == 0) {
					des.add(new DomEdge(theRoot + "xr", theRoot + "y"));
				} else {
					des.add(new DomEdge(theRoot + "xr", root.get(right) + "x"));
				}
				if(nonterminalToDomEdges.containsKey(right)) {
					des.addAll(nonterminalToDomEdges.get(right));
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
	
}
