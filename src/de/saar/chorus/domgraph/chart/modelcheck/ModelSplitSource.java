package de.saar.chorus.domgraph.chart.modelcheck;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.saar.chorus.domgraph.chart.Chart;
import de.saar.chorus.domgraph.chart.ChartSolver;
import de.saar.chorus.domgraph.chart.RegularTreeGrammar;
import de.saar.chorus.domgraph.chart.SolverNotApplicableException;
import de.saar.chorus.domgraph.chart.Split;
import de.saar.chorus.domgraph.chart.SplitComputer;
import de.saar.chorus.domgraph.chart.SplitSource;
import de.saar.chorus.domgraph.chart.SubgraphNonterminal;
import de.saar.chorus.domgraph.chart.SubgraphSplitComputer;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;

public class ModelSplitSource extends SplitSource<SubgraphNonterminal> {




	@Override
	public SubgraphNonterminal makeToplevelSubgraph(Set graph) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public void reduceIfNecessary(RegularTreeGrammar chart) {
		// do nothing? this should not be applicable for model checking.

	}



	private final NodeLabels graphlabels, solvedFormLabels;
	private final Map<SubgraphNonterminal, SubgraphNonterminal> subgraphToSubtree;
	private final DomGraph solvedForm;
	private final Chart sfchart;


	public ModelSplitSource(DomGraph graph, NodeLabels labels, DomGraph solvedForm,
			NodeLabels sfLabels) {
		super(graph);
		graphlabels = labels;
		solvedFormLabels = sfLabels;
		subgraphToSubtree = new HashMap<SubgraphNonterminal, SubgraphNonterminal>();
		this.solvedForm = solvedForm;
		//TODO scale up for disconnected graphs
		System.err.println(solvedForm.wccs());
		if(! solvedForm.wccs().isEmpty()) {
		subgraphToSubtree.put(
				new SubgraphNonterminal(graph.wccs().get(0)),
				new SubgraphNonterminal(solvedForm.wccs().get(0))); // TA
		}
		sfchart = new Chart(sfLabels);
		try {
		ChartSolver.solve(solvedForm, sfchart);
		} catch( SolverNotApplicableException e ) {
			e.printStackTrace();
		}

	}




	@Override
    protected Iterator<Split<SubgraphNonterminal>> computeSplits(SubgraphNonterminal subgraph) {
		 List<Split<SubgraphNonterminal>> ret = new ArrayList<Split<SubgraphNonterminal>>();
		SplitComputer<SubgraphNonterminal> sc = new SubgraphSplitComputer(graph);
		 List<String> potentialFreeRoots = computePotentialFreeRoots(subgraph);

		if( subgraphToSubtree.containsKey(subgraph) ) {
			SubgraphNonterminal subtree = subgraphToSubtree.get(subgraph);


			Split<SubgraphNonterminal> treesplit = sfchart.getSplitsFor(subtree).get(0); // TA
			String sfroot = treesplit.getRootFragment();
			List<String> holes = solvedForm.getHoles(sfroot);

			SubgraphNonterminal leftSubtree = treesplit.getWccs(holes.get(0)).get(0); // TA / BA
			SubgraphNonterminal rightSubtree = treesplit.getWccs(holes.get(1)).get(0); // TA / BA

			String rootlabel = solvedFormLabels.getLabel(sfroot);



		        for( String root : potentialFreeRoots ) {
		        	if(graphlabels.getLabel(root).equals(rootlabel)) {
		        		Split<SubgraphNonterminal> split = sc.computeSplit(root, subgraph);

		            	if( split != null ) {
		            		List<String> dom = graph.getHoles(root);
		            		SubgraphNonterminal leftSubgraph = split.getWccs(dom.get(0)).get(0); // TA / BA
		            		SubgraphNonterminal rightSubgraph = split.getWccs(dom.get(1)).get(0); // TA / BA

		            		if(leftSubgraph.size() == leftSubtree.size() &&
		            			rightSubgraph.size() == rightSubtree.size() ) {
		            			if(leftSubtree.size() == 1) {
		            				String treeleaf =
		            					leftSubtree.iterator().next();
		            				String dgleaf =
		            					leftSubgraph.iterator().next();
		            					if(! graphlabels.getLabel(dgleaf).equals(
		            							solvedFormLabels.getLabel(treeleaf))) {
		            						continue;
		            					}
		            			}
		            			if(rightSubtree.size() == 1) {
		            				String treeleaf =
		            					rightSubtree.iterator().next();
		            				String dgleaf =
		            					rightSubgraph.iterator().next();
		            					if(! graphlabels.getLabel(dgleaf).equals(
		            							solvedFormLabels.getLabel(treeleaf))) {
		            						continue;
		            					}
		            			}

		            	    	ret.add(split);
		            	    	subgraphToSubtree.put(leftSubgraph, leftSubtree);
		            	    	subgraphToSubtree.put(rightSubgraph, rightSubtree);
		            		}

		            	}
		        	}
		        }


		} else {
			// elsewise I have no Idea.
			System.err.println("Not in a recursive step.");
		}


		return ret.iterator();
	}

}
