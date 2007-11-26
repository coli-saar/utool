/*
 * @(#)Chart.java created 25.01.2006
 *
 * Copyright (c) 2006 Alexander Koller
 *
 */

package de.saar.chorus.domgraph.chart;



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

}



/*
 * UNIT TESTS:
 *  - clone is different object than original chart
 *  - maps in clone are equal
 *  - changing stuff in clone doesn't make a difference
 *
 */