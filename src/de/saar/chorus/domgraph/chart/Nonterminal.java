package de.saar.chorus.domgraph.chart;

public interface Nonterminal {
}


/*
 * Plan for better nonterminals:
 * - don't distinguish between singletons and non-singletons
 * - RTG stores terminal productions (and can thus decide whether something is a "singleton")
 * - generic class Nonterminal no longer needed -- RTG can take arbitrary type parameter for nonterminal
 * - chart solver etc. has type constraint for SubgraphBasedNonterminal
 * - trivial implementations for DecoratedNonterminal (= Pair) and DecoratedSubgraphBasedNonterminal
 * - Normally, rules in RTGs refer directly to terminal symbols. However, in a chart we refer to node
 *   names. This means that the RTG class must provide a method for retrieving the terminal symbol of
 *   a split. This method is overwritten for the Chart class to look it up in the NodeLabels. That is,
 *   a Chart constructor must accept and store a NodeLabels argument.
 */