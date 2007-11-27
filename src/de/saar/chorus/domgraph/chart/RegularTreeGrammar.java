package de.saar.chorus.domgraph.chart;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org._3pq.jgrapht.util.ModifiableInteger;

public class RegularTreeGrammar<E extends Nonterminal> implements Cloneable {
    private final Map<E, List<Split<E>>> chart;
    private final Map<E, ModifiableInteger> refcount;
    private final Map<E, BigInteger> numSolvedForms;
    private int size;
    private List<E> toplevelSubgraphs;


    /**
     * The constructor.
     */
    public RegularTreeGrammar() {
        chart = new HashMap<E, List<Split<E>>>();
        refcount = new HashMap<E, ModifiableInteger>();
        numSolvedForms = new HashMap<E, BigInteger>();
        toplevelSubgraphs = new ArrayList<E>();
        size = 0;
    }

    /**
     * Adds a split for the given subgraph.
     *
     * @param subgraph a subgraph of some dominance graph
     * @param split a split of this subgraph
     */
    public void addSplit(E subgraph, Split<E> split) {
        assert split != null;
        assert split.getSubstitution() != null;

        List<Split<E>> splitset = chart.get(subgraph);
        if( splitset == null ) {
            splitset = new ArrayList<Split<E>>();
            chart.put(subgraph, splitset);

        }

        // add split to the chart
        splitset.add(split);

        // update reference counts
        for( E subsubgraph : split.getAllSubgraphs() ) {
            incReferenceCount(subsubgraph);
        }

        size++;
    }

    /**
     * Increments the reference count for the given subgraph.
     *
     * @param subgraph a subgraph
     */
    private void incReferenceCount(E subgraph) {
        if( refcount.containsKey(subgraph)) {
            ModifiableInteger x = refcount.get(subgraph);
            x.setValue(x.intValue()+1);
        } else {
            refcount.put(subgraph, new ModifiableInteger(1));
        }
    }

    /**
     * Decrements the reference count for the given subgraph.
     *
     * @param subgraph a subgraph
     */
    private void decReferenceCount(E subgraph) {
        assert(refcount.containsKey(subgraph));

        ModifiableInteger x = refcount.get(subgraph);
        x.setValue(x.intValue()-1);
    }

    private int getReferenceCount(E subgraph) {
        return refcount.get(subgraph).getValue();
    }

    public int countSubgraphs() {
        return chart.size();
    }



    /**
     * Removes all unproductive nonterminals and splits from this chart.
     * A nonterminal is called unproductive if it isn't possible to derive a solved
     * form from it (but it may still be inaccessible from the top-level subgraphs).
     *
     * @param roots the roots of the dominance graph on which this chart is based
     */
    public void reduce(Set<String> roots) {
        Set<E> usefulNonterminals = new HashSet<E>();
        Map<E,List<Split<E>>> nonterminalUses = new HashMap<E,List<Split<E>>>();
        Map<Split<E>,E> splitToLhs = new HashMap<Split<E>,E>();
        Queue<E> agenda = new LinkedList<E>();
        Set<E> singletons = new HashSet<E>();

        // compute mappings of nonterminals to the splits that use them,
        // and of splits to their LHSs
        for( E lhs : chart.keySet() ) {
            for( Split<E> split : chart.get(lhs) ) {
                splitToLhs.put(split, lhs);

                for( E subgraph : split.getAllSubgraphs() ) {
                    List<Split<E>> uses = nonterminalUses.get(subgraph);

                    if( uses == null ) {
                        uses = new  ArrayList<Split<E>>();
                        nonterminalUses.put(subgraph, uses);
                    }

                    uses.add(split);

                    if( subgraph.isSingleton(roots) ) {
                        singletons.add(subgraph);
                    }
                }
            }
        }

        // bottom-up pass: initialize agenda with singletons
        agenda.addAll(singletons);

        // then propagate usefulness up from RHSs to LHSs of productions
        while( !agenda.isEmpty() ) {
            E nt = agenda.remove();

            if( !usefulNonterminals.contains(nt)) {
                usefulNonterminals.add(nt);

                if( nonterminalUses.containsKey(nt)) {
                    for( Split<E> split : nonterminalUses.get(nt)) {
                        boolean allRhsUseful = true;

                        for( E rhs : split.getAllSubgraphs() ) {
                            if( !usefulNonterminals.contains(rhs)) {
                                allRhsUseful = false;
                            }
                        }

                        if( allRhsUseful ) {
                            E lhs = splitToLhs.get(split);
                            agenda.add(lhs);
                        }
                    }
                }
            }
        }

        // at this point, we can delete everything that is not productive
        Set<E> uselessNonterminals = new HashSet<E>(nonterminalUses.keySet());
        uselessNonterminals.addAll(getToplevelSubgraphs());
        uselessNonterminals.removeAll(usefulNonterminals);

        Set<Split<E>> uselessSplits = new HashSet<Split<E>>();

        for( E useless : uselessNonterminals ) {
            // remove NT's own entry in the chart
            if( chart.containsKey(useless)) {
                chart.remove(useless);
            }

            // mark all splits which use it for deletion
            if( nonterminalUses.containsKey(useless)) {
                uselessSplits.addAll(nonterminalUses.get(useless));
            }
        }

        // delete all splits that use useless nonterminals
        for( Split<E> split : uselessSplits ) {
            if( chart.containsKey(splitToLhs.get(split))) {
                chart.get(splitToLhs.get(split)).remove(split);
            }
        }
    }



    /**
     * Sets the splits for a given subgraph. If the subgraph already
     * had splits, these are deleted first.
     *
     * @param subgraph a subgraph
     * @param splits the new splits for this subgraph
     * @throws UnsupportedOperationException - if you try to delete all splits
     * of a subgraph that is still referenced from some other split. If this
     * happens, the chart remains unchanged.
     */
    public void setSplitsForSubgraph(E subgraph, List<Split<E>> splits) {
        Set<E> subgraphsAllSplits = new HashSet<E>();
        List<Split<E>> oldSplits = getSplitsFor(subgraph);

        numSolvedForms.clear();

        if( splits.isEmpty() && (getReferenceCount(subgraph) > 0)) {
            throw new UnsupportedOperationException("The subgraph is still referenced "
                    + getReferenceCount(subgraph) + " times. You may not remove its last split.");
        }

        // update reference count effects of deleting the old splits
        for( Split<E> oldSplit : oldSplits ) {
            List<E> subsubgraphs = oldSplit.getAllSubgraphs();
            subgraphsAllSplits.addAll(subsubgraphs);

            for( E subsubgraph : subsubgraphs ) {
                decReferenceCount(subsubgraph);
            }
        }

        // delete the old splits
        size -= oldSplits.size();
        oldSplits.clear();


        // add the new split
        for( Split<E> split : splits ) {
            addSplit(subgraph, split);
        }

        // remove subgraphs with zero reference count from the chart
        for( E s : subgraphsAllSplits ) {
            if( getReferenceCount(s) == 0 ) {
                deleteSubgraph(s);
            }
        }
    }



    /**
     * Deletes a subgraph and all of its splits from the chart. This method
     * updates the reference counts, and recursively deletes all other subgraphs that
     * become unreachable.
     *
     * @param subgraph a subgraph
     * @throws UnsupportedOperationException - if you try to delete a subgraph
     * that is still referenced from some split. If this happens, the chart
     * remains unchanged.
     *
     */
    public void deleteSubgraph(E subgraph) {
        List<Split<E>> splits = getSplitsFor(subgraph);

        if( getReferenceCount(subgraph) > 0 ) {
            throw new UnsupportedOperationException("The subgraph is still referenced "
                    + getReferenceCount(subgraph) + " times. You may not delete it.");
        }

        // update reference counts for referred sub-subgraphs and
        // recursively delete those if they drop to zero
        for( Split<E> split : splits ) {
            for( E subsubgraph : split.getAllSubgraphs() ) {
                decReferenceCount(subsubgraph);

                if( getReferenceCount(subsubgraph) == 0 ) {
                    deleteSubgraph(subsubgraph);
                }
            }
        }

        size -= splits.size();
        splits.clear(); // TODO - or perhaps delete the subgraph altogether?
    }


    /**
     * Returns the list of all splits for the given subgraph.
     *
     * @param subgraph a subgraph
     * @return the list of splits for this subgraph.
     */
    public List<Split<E>> getSplitsFor(E subgraph) {
        return chart.get(subgraph);
    }

    /**
     * Checks whether the chart contains a split for the given subgraph.
     *
     * @param subgraph a subgraph
     * @return true iff the chart contains any splits for this subgraph.
     */
    public boolean containsSplitFor(E subgraph) {
        return chart.containsKey(subgraph);
    }


    /**
     * Returns the number of splits in the entire chart.
     *
     * @return the number of splits
     */
    public int size() {
        return this.size;
    }

    /**
     * Returns a string representation of the chart.
     */
    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();

        ret.append("Top-level subgraphs: " + toplevelSubgraphs + "\n");

        for( E fragset : chart.keySet() ) {
            for( Split<E> split : chart.get(fragset) ) {
                ret.append(fragset.toString() + " -> " + split + "\n");
            }
        }

        return ret.toString();
    }


    /**
     * Returns the list of all top-level subgraphs.
     *
     * @return the top-level subgraphs.
     */
    public List<E> getToplevelSubgraphs() {
        return toplevelSubgraphs;
    }

    /**
     * Adds a top-level subgraph. This should be one of the maximal
     * weakly connected subgraphs of the dominance graph; if the entire
     * graph is connected, then this should be the set of all nodes
     * of the dominance graph.
     *
     * @param subgraph a weakly connected subgraph.
     */
    public void addToplevelSubgraph(E subgraph) {
        this.toplevelSubgraphs.add(subgraph);
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
        RegularTreeGrammar<E> ret = new RegularTreeGrammar<E>();

        for( Map.Entry<E, List<Split<E>>> entry : chart.entrySet() ) {
            ret.chart.put(entry.getKey(), new ArrayList<Split<E>>(entry.getValue()));
        }

        for( Map.Entry<E, ModifiableInteger> entry : refcount.entrySet() ) {
            ret.refcount.put(entry.getKey(), new ModifiableInteger(entry.getValue().getValue()));
        }

        ret.size = size;

        ret.toplevelSubgraphs = new ArrayList<E>(toplevelSubgraphs);

        return ret;
    }



    public void clear() {
        toplevelSubgraphs.clear();
        chart.clear();
        numSolvedForms.clear();
        refcount.clear();
        size = 0;
    }


}
