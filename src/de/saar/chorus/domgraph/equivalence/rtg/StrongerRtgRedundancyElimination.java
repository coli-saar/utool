package de.saar.chorus.domgraph.equivalence.rtg;

import java.util.List;

import de.saar.chorus.domgraph.chart.DecoratedNonterminal;
import de.saar.chorus.domgraph.chart.Nonterminal;
import de.saar.chorus.domgraph.chart.RegularTreeGrammar;
import de.saar.chorus.domgraph.chart.Split;
import de.saar.chorus.domgraph.chart.rtgparser.StringNonterminal;
import de.saar.chorus.domgraph.equivalence.EquationSystem;
import de.saar.chorus.domgraph.equivalence.RedundancyElimination;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;

public class StrongerRtgRedundancyElimination<E extends Nonterminal> extends RedundancyElimination<E> {
	public StrongerRtgRedundancyElimination(DomGraph graph, NodeLabels labels, EquationSystem eqs) {
		super(graph, labels, eqs);
	}
	
	
	public void eliminate(RegularTreeGrammar<E> chart, RegularTreeGrammar<DecoratedNonterminal<E, StringNonterminal>> out) {
		NodeLabels selfLabels = makeSelfLabels(graph);
		
		out.clear();
		
		
	}
	
	

	private NodeLabels makeSelfLabels(DomGraph graph) {
		NodeLabels ret = new NodeLabels();
		
		for( String node : graph.getAllNodes() ) {
			ret.addLabel(node, node);
		}
		
		return ret;
	}


	@Override
	public List<Split<E>> getIrredundantSplits(E subgraph, List<Split<E>> allSplits) {
		// TODO Auto-generated method stub
		return null;
	}

}
