/*
 * @(#)Chart.java created 25.01.2006
 *
 * Copyright (c) 2006 Alexander Koller
 *
 */

package de.saar.chorus.domgraph.chart;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org._3pq.jgrapht.util.ModifiableInteger;

import de.saar.chorus.domgraph.graph.NodeLabels;



/**
 * A chart for storing intermediate results of the graph-chart solver.
 * This data structure assigns to a weakly connected subgraph G of the original
 * dominance graph a list of splits for G. A split records the choice of a fragment F
 * of G as the root fragment of a solved form of G, and how the other
 * fragments of G must be distributed over the holes of F. That is, it
 * splits G into a root fragment F and the weakly connected components
 * that remain after F is removed.<p>
 *
 * This class supports the dynamic addition and removal of splits and
 * subgraphs, and maintains the invariant that all subgraphs and splits
 * in the chart can be used in some solved form -- if they can't, they
 * are removed from the chart. It uses reference counters to keep track
 * of this; to initialise them, the user must specify one or more
 * subgraphs as "top-level" subgraphs, which receive a reference count
 * of 1. One important limitation is that it is not allowed to delete
 * a subgraph (or the last split in the subgraph) if this subgraph
 * is still referenced from elsewhere. The relevant methods throw an
 * UnsupportedOperationException if you attempt this.
 *
 * @author Alexander Koller
 *
 */
public class Chart extends RegularTreeGrammar<SubgraphNonterminal> {
	private NodeLabels labels;
	
	public Chart(NodeLabels labels) {
		super();
		
		this.labels = labels;
	}
	
	
	/**
     * Computes a clone of the chart. Splits and subgraphs can be added and deleted,
     * and toplevel subgraphs changed, on the clone without affecting
     * the original chart object. However, the clone contains referneces to the same
     * individual subgraphs and splits as the original chart, so be sure
     * not to modify the subgraphs and splits themselves. (This would be
     * a bad idea anyway.)
     *
     * @return a <code>Chart</code> object which is a clone of the current
     * chart
     */
    @Override
    public Object clone() {
    	Chart ret = new Chart(labels);

        for( Map.Entry<SubgraphNonterminal, List<Split<SubgraphNonterminal>>> entry : chart.entrySet() ) {
            ret.chart.put(entry.getKey(), new ArrayList<Split<SubgraphNonterminal>>(entry.getValue()));
        }

        for( Map.Entry<SubgraphNonterminal, ModifiableInteger> entry : refcount.entrySet() ) {
            ret.refcount.put(entry.getKey(), new ModifiableInteger(entry.getValue().getValue()));
        }

        ret.size = size;

        ret.toplevelSubgraphs = new ArrayList<SubgraphNonterminal>(toplevelSubgraphs);

        return ret;
    }

	@Override
	public String getLabelForSplit(Split<SubgraphNonterminal> split) {
		return labels.getLabel(split.getRootFragment());
	}
}



/*
 * UNIT TESTS:
 *  - clone is different object than original chart
 *  - maps in clone are equal
 *  - changing stuff in clone doesn't make a difference
 *
 */