package de.saar.chorus.domgraph.chart;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

abstract public class RegularTreeGrammar<E> {
	private static final boolean DEBUG = false;

	protected final Map<E, BigInteger> numSolvedForms;

	abstract public Set<E> getAllNonterminals();
	abstract public boolean isSingleton(E nt);
	abstract public String getRootForSingleton(E nt);
	abstract public List<Split<E>> getSplitsFor(E subgraph);
	abstract public void getSplitsFor(E subgraph, String label, List<Split<E>> splits);
	abstract public boolean containsSplitFor(E subgraph);
	abstract public List<E> getToplevelSubgraphs();

	/*
	abstract public <F extends GraphBasedNonterminal> void prepareForIntersection(RegularTreeGrammar<F> other);
	 */

	public RegularTreeGrammar() {
		numSolvedForms = new HashMap<E, BigInteger>();
	}

	/**
	 * Call this method on the filtering grammar and pass the grammar that is to be filtered as the argument.
	 * 
	 * @param <F>
	 * @param other
	 * @return
	 */
	public <F extends GraphBasedNonterminal> void intersect(RegularTreeGrammar<F> other, ConcreteRegularTreeGrammar<DecoratedNonterminal<F,E>>  out) {
		Queue<DecoratedNonterminal<F,E>> agenda = new LinkedList<DecoratedNonterminal<F,E>>();
		List<Split<E>> otherSplits = new ArrayList<Split<E>>();
		
		out.clear();

		if( (getToplevelSubgraphs().size() != 1) || (other.getToplevelSubgraphs().size() != 1) ) {
			throw new UnsupportedOperationException("Can't intersect these automata! Toplevel subgraphs: " + getToplevelSubgraphs() + ", " + other.getToplevelSubgraphs());
		}

		DecoratedNonterminal<F,E> nt = new DecoratedNonterminal<F,E>(other.getToplevelSubgraphs().get(0), getToplevelSubgraphs().get(0));
		agenda.add(nt);
		out.addToplevelSubgraph(nt);

		while( !agenda.isEmpty() ) {
			DecoratedNonterminal<F,E> sub = agenda.remove();

			if( !out.containsSplitFor(sub) ) {
				List<Split<F>> splits = other.getSplitsFor(sub.getBase());

				if( splits != null ) {
					for( Split<F> split : splits ) {
						getSplitsFor(sub.getDecoration(), split.getRootFragment(), otherSplits );

						for( Split<E> otherSplit : otherSplits ) {
							Split<DecoratedNonterminal<F, E>> newSplit = makeSplit(split, otherSplit);
							out.addSplit(sub, newSplit);

							if( DEBUG ) {
								System.err.println("add split: " + newSplit + " for " + sub);
							}

							for( DecoratedNonterminal<F, E> candidate : newSplit.getAllSubgraphs() ) {
								agenda.add(candidate);
							}
						}
					}
				}
			}
		}
	}

	private <F extends GraphBasedNonterminal> Split<DecoratedNonterminal<F, E>> makeSplit(Split<F> otherSplit, Split<E> split) {
		String root = split.getRootFragment();
		Split<DecoratedNonterminal<F, E>> ret = new Split<DecoratedNonterminal<F, E>>(root);

		for( String dominator : split.getAllDominators() ) {
			if( (split.getWccs(dominator).size() != 1) || (otherSplit.getWccs(dominator).size() != 1) ) {
				throw new UnsupportedOperationException("Can't intersect these grammars! Offending splits: " + split + ", " + otherSplit);
			} else {
				ret.addWcc(dominator, new DecoratedNonterminal<F, E>(otherSplit.getWccs(dominator).get(0), split.getWccs(dominator).get(0)));
			}
		}

		return ret;
	}

	/**
	 * Returns a string representation of the chart.
	 */
	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder();

		ret.append("Top-level subgraphs: " + getToplevelSubgraphs() + "\n");

		for( E fragset : getAllNonterminals() ) {
			for( Split<E> split : getSplitsFor(fragset) ) {
				ret.append(fragset.toString() + " -> " + split + "\n");
			}
		}

		return ret.toString();
	}

	/**
	 * Returns the number of solved forms represented by this chart.
	 * This method doesn't compute the solved forms themselves (and
	 * is much faster than that), but it can take a few hundred
	 * milliseconds for a large chart.<p>
	 *
	 * The method assumes that the chart belongs to a solvable dominance
	 * graph, i.e. that it represents any solved forms in the first place.
	 * You can assume this for all charts that were generated by
	 * ChartSolver#solve with a return value of <code>true</code>.
	 *
	 * @return the number of solved forms
	 */
	public BigInteger countSolvedForms() {
		BigInteger ret = BigInteger.ONE;

		numSolvedForms.clear();

		for( E subgraph : getToplevelSubgraphs() ) {
			ret = ret.multiply(countSolvedFormsFor(subgraph, numSolvedForms));
		}

		return ret;
	}

	public BigInteger countSolvedFormsFor(E subgraph) {
		return countSolvedFormsFor(subgraph, numSolvedForms);
	}

	private BigInteger countSolvedFormsFor(E subgraph, Map<E,BigInteger> numSolvedForms) {
		BigInteger ret = BigInteger.ZERO;

		if( numSolvedForms.containsKey(subgraph) ) {
			return numSolvedForms.get(subgraph);
		} else if( !containsSplitFor(subgraph) ) {
			// subgraph contains only one fragment => 1 solved form
			return BigInteger.ONE;
		} else {
			for( Split<E> split : getSplitsFor(subgraph) ) {
				BigInteger sfsThisSplit = BigInteger.ONE;

				for( E subsubgraph : split.getAllSubgraphs() ) {
					sfsThisSplit = sfsThisSplit.multiply(countSolvedFormsFor(subsubgraph, numSolvedForms));
				}

				ret = ret.add(sfsThisSplit);
			}

			numSolvedForms.put(subgraph, ret);
			return ret;
		}
	}
}