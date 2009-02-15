package de.saar.chorus.domgraph.equivalence;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.saar.chorus.domgraph.chart.ConcreteRegularTreeGrammar;
import de.saar.chorus.domgraph.chart.DecoratedNonterminal;
import de.saar.chorus.domgraph.chart.GraphBasedNonterminal;
import de.saar.chorus.domgraph.chart.RewritingRtg;
import de.saar.chorus.domgraph.chart.Split;
import de.saar.chorus.domgraph.chart.SplitSource;
import de.saar.chorus.domgraph.chart.UnsolvableSubgraphException;

public class RewritingSplitSource<E extends GraphBasedNonterminal,F> extends SplitSource<DecoratedNonterminal<E, F>> {
	private RewritingRtg<F> filter;
	private SplitSource<E> embeddedSplitSource;
	
	public RewritingSplitSource(RewritingRtg<F> filter, SplitSource<E> embeddedSplitSource) {
		super(embeddedSplitSource.getGraph());
		this.filter = filter;
		this.embeddedSplitSource = embeddedSplitSource;
	}

	@Override
	public Iterator<Split<DecoratedNonterminal<E, F>>> computeSplits(DecoratedNonterminal<E, F> subgraph)
			throws UnsolvableSubgraphException {
		List<Split<DecoratedNonterminal<E, F>>> splits = new ArrayList<Split<DecoratedNonterminal<E,F>>>();
		Iterator<Split<E>> baseSplits = embeddedSplitSource.computeSplits(subgraph.getBase());
		List<Split<F>> filterSplits = new ArrayList<Split<F>>();
		
		while(baseSplits.hasNext()) {
			Split<E> baseSplit = baseSplits.next();
			filter.getSplitsFor(subgraph.getDecoration(), baseSplit.getRootFragment(), filterSplits);
			
			for( Split<F> filterSplit : filterSplits ) {
				splits.add(makeSplit(baseSplit, filterSplit));
			}
		}
		
		return splits.iterator();
	}

	private  Split<DecoratedNonterminal<E, F>> makeSplit(Split<E> split, Split<F> otherSplit) {
		String root = split.getRootFragment();
		Split<DecoratedNonterminal<E, F>> ret = new Split<DecoratedNonterminal<E, F>>(root);

		for( String dominator : split.getAllDominators() ) {
			if( (split.getWccs(dominator).size() != 1) || (otherSplit.getWccs(dominator).size() != 1) ) {
				throw new UnsupportedOperationException("Can't intersect these grammars! Offending splits: " + split + ", " + otherSplit);
			} else {
				ret.addWcc(dominator, new DecoratedNonterminal<E, F>(split.getWccs(dominator).get(0), otherSplit.getWccs(dominator).get(0)));
			}
		}

		return ret;
	}
	
	@Override
	public DecoratedNonterminal<E, F> makeToplevelSubgraph(Set<String> graph) {
		return new DecoratedNonterminal<E, F>(embeddedSplitSource.makeToplevelSubgraph(graph), filter.getToplevelSubgraphs().get(0));
	}

	@Override
	public void reduceIfNecessary(ConcreteRegularTreeGrammar<DecoratedNonterminal<E, F>> chart) {
		
	}

}
