package de.saar.chorus.domgraph.chart.modelcheck;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import de.saar.chorus.domgraph.chart.Chart;
import de.saar.chorus.domgraph.chart.ChartSolver;
import de.saar.chorus.domgraph.chart.SolverNotApplicableException;
import de.saar.chorus.domgraph.chart.Split;
import de.saar.chorus.domgraph.chart.SubgraphNonterminal;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;

public class ModelCheck {

	
	/**
	 * Checks whether two <code>Chart</code> objects, based on the given <code>DomGraph</code>s,
	 * are equivalent. Node names are ignored. Variable naming within labels is taken into account
	 * to the degree that the mapping of variable names has to be consistent. (If some variable in
	 * chart1 is called h1 and is mapped to some variable a1 in chart1, all occurrences of h1 have
	 * to be mapped to occurrences of a1.)
	 * 
	 * @param chart1 a chart
	 * @param dg1 the <code>DomGraph</code> chart1 is based on
	 * @param nl1 the labels dg1 is labeled with
	 * @param chart2 a second chart
	 * @param dg2 the <code>DomGraph</code> chart2 is based on
	 * @param nl2 the labels dg2 is labeled with
	 * @return true if chart1 and chart2 are equivalent with respect to the labels, false otherwise.
	 */
	
	public static boolean equals(Chart chart1, DomGraph dg1, NodeLabels nl1,
			Chart chart2, DomGraph dg2, NodeLabels nl2) {
		List<SubgraphNonterminal> top1 = chart1.getToplevelSubgraphs();
		List<SubgraphNonterminal> top2 = chart2.getToplevelSubgraphs();
		
		if(top1.size() != top2.size()) {
			System.err.println("different number of top wccs.");
			return false;
		}
		
		Set<SubgraphNonterminal> checked = new HashSet<SubgraphNonterminal>();
		Map<String, String> variables = new HashMap<String,String>()
;		for(int i = 0; i< top1.size(); i++) {
			if(! matchesSubgraph(top1.get(i), chart1, dg1, nl1, top2.get(i), chart2, dg2, nl2, 
					checked, variables, true)) {
				return false;
			}
		}
		
		return true;
	}
	
	
	/**
	 * Determines whether a <code>Chart</code> object basing on a given <code>DomGraph</code> is 
	 * more general (subsumes) a second one. The first argument has to be the more general one,
	 * the second one the equivalent or more specific chart.
	 * Node names are ignored. Variable naming within labels is taken into account
	 * to the degree that the mapping of variable names has to be consistent. (If some variable in
	 * chart1 is called h1 and is mapped to some variable a1 in chart1, all occurrences of h1 have
	 * to be mapped to occurrences of a1.)
	 * 
	 * TODO I need the DomGraphs to determine the left-to-right order of holes.
	 * Can't we order the subgraphs in the chart according to the graph (so as to make
	 * Split#getAllDominators return the holes in the graph-based order)? 
	 * They are ordered anyway, but not deterministically. 
	 * 
	 * @param chart1 the more general <code>Chart</code>
	 * @param dg1 the <code>DomGraph</code> underlying chart1
	 * @param nl1 the labels dg1 is marked with
	 * @param chart2 the more specific <code>Chart</code>
	 * @param dg2 the <code>DomGraph</code> underlying chart2
	 * @param nl2 the labels dg2 is marked with
	 * @return truw if chart1 subsumes chart2, false otherwise
	 */
	public static boolean subsumes(Chart chart1, DomGraph dg1, NodeLabels nl1,
			Chart chart2, DomGraph dg2, NodeLabels nl2) {

		
		List<SubgraphNonterminal> top1 = chart1.getToplevelSubgraphs();
		List<SubgraphNonterminal> top2 = chart2.getToplevelSubgraphs();
		
		if(top1.size() != top2.size()) {
			System.err.println("different number of top wccs.");
			return false;
		}
		
		Set<SubgraphNonterminal> checked = new HashSet<SubgraphNonterminal>();
		Map<String, String> variables = new HashMap<String,String>()
;		for(int i = 0; i< top1.size(); i++) {
			if(! matchesSubgraph(top1.get(i), chart1, dg1, nl1, top2.get(i), chart2, dg2, nl2, 
					checked, variables,false)) {
				return false;
			}
		}
		
		return true;
	}
	
	private static boolean matchesSplit(Split<SubgraphNonterminal> sp1, Chart c1, 
			DomGraph dg1, NodeLabels nl1,
			Split<SubgraphNonterminal> sp2, Chart c2, DomGraph dg2,  NodeLabels nl2,
			Set<SubgraphNonterminal> checked, Map<String, String> variables, boolean equal) {
		
	//	System.err.println("Checking splits...");
		if(! nl1.getLabel(sp1.getRootFragment()).equals(nl2.getLabel(sp2.getRootFragment())) ) {
		//	System.err.println("root " + c1.getLabelForSplit(sp1) + " doesn't match " + c2.getLabelForSplit(sp2));
			return false;
		}
		
		Matcher match1 = Pattern.compile("[eixuh]\\d+").matcher(c1.getLabelForSplit(sp1));
		Matcher match2 = Pattern.compile("[eixuh]\\d+").matcher(c2.getLabelForSplit(sp2));
		
		while(match1.find() && match2.find()) {
			String var1 = match1.group();
			String var2 = match2.group();
			if(variables.containsKey(var1)) {
				if(! var2.equals(variables.get(var1))) {
					return false;
				}
			} else {
				variables.put(var1, var2);
			}
			
		}
		
		List<String> dominators1 = dg1.getHoles(sp1.getRootFragment());
		List<String> dominators2 = dg2.getHoles(sp2.getRootFragment());
		
		
		if( dominators1.size() != dominators2.size() ) {
			//this should actually never occur in our label style, als labels include the arity.
	//		System.err.println("Wrong arity.");
			return false;
		}
		for(int i = 0; i < dominators1.size(); i++) {
			List<SubgraphNonterminal> wccs1 = sp1.getWccs(dominators1.get(i));
			List<SubgraphNonterminal> wccs2 = sp2.getWccs(dominators2.get(i));
			
			
			if( wccs1.size() != wccs2.size() ) {
				// I'm not sure whether or not this is too strict
	//			System.err.println("Wrong number of targets of hole.");
				return false;
			}
			
			/*
			 * TODO this is not correct, but should work for hnv graphs.
			 * Find a way to match wccs properly.
			 */
			for(int h = 0; h < wccs1.size(); h++) {
		//		System.err.println("Subgraph match failed");
				if(! matchesSubgraph(wccs1.get(h), c1, dg1, nl1, wccs2.get(h), 
						c2, dg2, nl2, checked, variables,equal)) {
					return false;
				}
			}
			
		}
	//	System.err.println("Matched Splits.");
		return true;
	}

	
	private static boolean matchesSubgraph(SubgraphNonterminal st1, Chart c1, DomGraph dg1, NodeLabels nl1,
			SubgraphNonterminal st2, Chart c2, DomGraph dg2, NodeLabels nl2, 
			Set<SubgraphNonterminal> checked, Map<String, String> variables, boolean equal) {
		
		if(checked.contains(st2)) {
			return true;
		}
		
		if( st1.getNodes().size()!= st2.getNodes().size() ) {
			
	//		System.err.println("Wrong number of nodes in SG.");
			return false;
		}
		
	
		Multiset<String> labels1 = new HashMultiset<String>();		
		Multiset<String> labels2 = new HashMultiset<String>();		
		
		for(String node : st1.getNodes()) {
			labels1.add(nl1.getLabel(node));
		}
	
		for(String node : st2.getNodes()) {
			labels2.add(nl2.getLabel(node));
		}
		
		
		if(! labels1.equals(labels2)) {
	//		System.err.println("Label sets don't match.");
			return false;
		}
		
		List<Split<SubgraphNonterminal>> splits1 = c1.getSplitsFor(st1);
		
		List<Split<SubgraphNonterminal>> splits2 = c2.getSplitsFor(st2);
		
		Set<Split<SubgraphNonterminal>> kickoutsplits = new HashSet<Split<SubgraphNonterminal>>(splits2);
		
		// the first argument is meant to be either equal or more general than the second one
		
		
		if(equal) {
			if(splits1.size() != splits2.size()) {
				//		System.err.println("the first SG is more specific than the second");
						return false;
					}
		} else {
			if(splits1.size() < splits2.size()) {
				//		System.err.println("the first SG is more specific than the second");
						return false;
					}
		}
		
		for(Split<SubgraphNonterminal> values : splits2) {
			for(Split<SubgraphNonterminal> key : splits1) {
				if(matchesSplit(key, c1, dg1, nl1, values, c2, dg2, nl2, checked, variables, equal)) {
					kickoutsplits.remove(values);
				}
			}
		}
		
		if(kickoutsplits.isEmpty()) {
			checked.add(st2);
	//		System.err.println("Matched SGs.");
			return true;
		}
//		System.err.println("Could not match all of the more specific splits.");
		return false;
	}
	
	
	/**
	 * Checks whether some tree is a solved form of a <code>DomGraph</code>. 
	 * This works with pure chains only. 
	 * 
	 * @deprecated
	 * @param sf
	 * @param sfl
	 * @param dg
	 * @param dgl
	 * @return
	 */
	public static boolean solves(DomGraph sf, NodeLabels sfl, DomGraph dg, NodeLabels dgl) {
		if(! sf.isSolvedForm() ) {
			System.err.println("The solved form is not a tree!");
			return false;
		} else {
			try {
			return ChartSolver.solve(dg, new Chart(dgl), new ModelSplitSource(dg,dgl,sf,sfl));
			} catch(SolverNotApplicableException e) {
				e.printStackTrace();
				return false;
			}
		}
	}

	
	
}
