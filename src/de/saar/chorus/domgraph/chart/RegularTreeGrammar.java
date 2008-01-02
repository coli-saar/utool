package de.saar.chorus.domgraph.chart;

import java.io.File;
import java.io.FileReader;
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

import de.saar.chorus.domgraph.chart.rtgparser.RtgParser;
import de.saar.chorus.domgraph.chart.rtgparser.StringNonterminal;
import de.saar.chorus.domgraph.codec.domcon.DomconOzInputCodec;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;

public class RegularTreeGrammar<E extends Nonterminal> implements Cloneable {
    protected Map<E, List<Split<E>>> chart;
    protected Map<E, ModifiableInteger> refcount;
    protected final Map<E, BigInteger> numSolvedForms;
    protected int size;
    protected List<E> toplevelSubgraphs;
    protected Set<E> finalStates;


    /**
     * The constructor.
     */
    public RegularTreeGrammar() {
        chart = new HashMap<E, List<Split<E>>>();
        refcount = new HashMap<E, ModifiableInteger>();
        numSolvedForms = new HashMap<E, BigInteger>();
        toplevelSubgraphs = new ArrayList<E>();
        size = 0;
        finalStates = new HashSet<E>();
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

    public String getLabelForSplit(Split<E> split) {
		return split.getRootFragment();
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

    public Set<E> getAllNonterminals() {
        return chart.keySet();
    }



    /**
     * Removes all unproductive nonterminals and splits from this chart.
     * A nonterminal is called unproductive if it isn't possible to derive a solved
     * form from it (but it may still be inaccessible from the top-level subgraphs).
     *
     * @param roots the roots of the dominance graph on which this chart is based
     */
    public void reduce() {
        Set<E> usefulNonterminals = new HashSet<E>();
        Map<E,List<Split<E>>> nonterminalUses = new HashMap<E,List<Split<E>>>();
        Map<Split<E>,List<E>> splitToLhs = new HashMap<Split<E>,List<E>>();
        Queue<E> agenda = new LinkedList<E>();
        Set<E> singletons = new HashSet<E>();

        // compute mappings of nonterminals to the splits that use them,
        // and of splits to their LHSs
        for( E lhs : chart.keySet() ) {
        	for( Split<E> split : chart.get(lhs) ) {
                List<E> LHSs = splitToLhs.get(split);

                if( LHSs == null ) {
                    LHSs = new ArrayList<E>();
                    splitToLhs.put(split,LHSs);
                }

                LHSs.add(lhs);

                for( E subgraph : split.getAllSubgraphs() ) {
                    List<Split<E>> uses = nonterminalUses.get(subgraph);

                    if( uses == null ) {
                        uses = new  ArrayList<Split<E>>();
                        nonterminalUses.put(subgraph, uses);
                    }

                    uses.add(split);
                }


                if( split.getAllDominators().isEmpty() ) {
                    singletons.add(lhs);
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
                            for( E lhs : splitToLhs.get(split)) {
                                agenda.add(lhs);
                            }
                        }
                    }
                }
            }
        }

        //System.err.println("useful: " + usefulNonterminals);

        // at this point, we can delete everything that is not productive
        Set<E> uselessNonterminals = new HashSet<E>(nonterminalUses.keySet());
        uselessNonterminals.addAll(getToplevelSubgraphs());
        uselessNonterminals.removeAll(usefulNonterminals);

        Set<Split<E>> uselessSplits = new HashSet<Split<E>>();

        for( E useless : uselessNonterminals ) {
            //System.err.println("Consider useless NT: " + useless);

            // remove NT's own entry in the chart
            if( chart.containsKey(useless)) {
                //System.err.println("Remove useless NT: " + useless);
                chart.remove(useless);
            }

            // if NT is toplevel subgraph, remove that
            if( toplevelSubgraphs.contains(useless)) {
                //System.err.println("Remove useless tl NT: " + useless);
            	toplevelSubgraphs.remove(useless);
            }

            // mark all splits which use it for deletion
            if( nonterminalUses.containsKey(useless) ) {
                //System.err.println("Schedule splits for deletion: " + nonterminalUses.get(useless));
                uselessSplits.addAll(nonterminalUses.get(useless));
            }
        }

        // delete all splits that use useless nonterminals
        for( Split<E> split : uselessSplits ) {
            for( E lhs : splitToLhs.get(split)) {
                if( chart.containsKey(lhs) ) {
                    //System.err.println("Remove split " + split + " (for " + lhs  + ")");
                    chart.get(lhs).remove(split);
                }
            }
        }

        // TODO update size
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

    /*
    public Map<String,Set<String>> computePossibleDominators() {
        Map<String,Set<String>> ret = new HashMap<String,Set<String>>();

        for( E key : getAllNonterminals() ) {
            for( Split<E> split : getSplitsFor(key)) {
                for( String hole : split.getAllDominators() ) {
                    for( E subgraph : split.getWccs(hole)) {
                        for( String node : subgraph.getNodes() ) {
                            addDominee(ret, split.getRootFragment(), node);
                            addDominee(ret, hole, node);
                        }
                    }
                }

            }
        }

        return ret;
    }


    private void addDominee(Map<String,Set<String>> possibleDominators, String dominator, String dominee) {
        Set<String> dominees = possibleDominators.get(dominator);

        if( dominees == null ) {
            dominees = new HashSet<String>();
            possibleDominators.put(dominator, dominees);
        }

        dominees.add(dominee);
    }

*/




    public void clear() {
        toplevelSubgraphs.clear();
        chart.clear();
        numSolvedForms.clear();
        refcount.clear();
        size = 0;
    }


    public <F extends Nonterminal> RegularTreeGrammar<DecoratedNonterminal<E,F>> intersect(RegularTreeGrammar<F> other) {
        RegularTreeGrammar<DecoratedNonterminal<E,F>> ret = new RegularTreeGrammar<DecoratedNonterminal<E,F>>();

        /*
        if( getToplevelSubgraphs().size() != 1 || other.getToplevelSubgraphs().size() != 1 ) {
            throw new UnsupportedOperationException("Can't intersect RTGs with multiple top-level subgraphs.");
        }
        */

        // TODO - this is a hack, because top-level subgraphs were _not_ meant
        // as alternative start states, but for disconnected dominance graphs.
        // For now it'll do.
        for( E top1 : getToplevelSubgraphs() ) {
        	for( F top2 : other.getToplevelSubgraphs() ) {
                DecoratedNonterminal<E,F> newToplevelSubgraph = new DecoratedNonterminal<E,F>(top1, top2);
                ret.addToplevelSubgraph(newToplevelSubgraph);

                intersectPopulate(newToplevelSubgraph, ret, this, other);
        	}
        }

        //System.err.println("before: " + ChartPresenter.chartOnlyRoots(ret, graph));

        ret.reduce();

        return ret;
    }

    private static <E extends Nonterminal, F extends Nonterminal> void intersectPopulate(DecoratedNonterminal<E,F> nt, RegularTreeGrammar<DecoratedNonterminal<E,F>> out, RegularTreeGrammar<E> in1, RegularTreeGrammar<F> in2) {
        E nt1 = nt.nonterminal;
        F nt2 = nt.decoration;

        for( Split<E> split1 : in1.getSplitsFor(nt1)) {
            for( Split<F> split2 : in2.getSplitsFor(nt2)) {
                if( split1.getAllDominators().size() == split2.getAllDominators().size() ) {
                    if( in2.getLabelForSplit(split2).equals(in1.getLabelForSplit(split1)) ) {
                        Split<DecoratedNonterminal<E,F>> newSplit = new Split<DecoratedNonterminal<E,F>>(split1.getRootFragment());
                        List<DecoratedNonterminal<E,F>> newNts = new ArrayList<DecoratedNonterminal<E,F>>();
                        boolean goodSplit = true;

                        /*
                        System.err.println("Intersect splits for " + nt1 + "," + nt2);
                        System.err.println("Split1: " + split1 + " (dominators: " + split1.getAllDominators() + ")");
                        System.err.println("Split2: " + split2 + " (dominators: " + split2.getAllDominators() + ")");
                        */

                        for( int i = 0; i < split1.getAllDominators().size(); i++ ) {
                            String hole1 = split1.getAllDominators().get(i);
                            String hole2 = split2.getAllDominators().get(i);

                            if( split1.getWccs(hole1).size() != 1 || split2.getWccs(hole2).size() != 1 ) {
                                throw new UnsupportedOperationException("Can't intersect RTGs with multiple wccs per dominator.");
                            }

                            E sub1 = split1.getWccs(hole1).get(0);
                            F sub2 = split2.getWccs(hole2).get(0);

                            DecoratedNonterminal<E,F> newNt = new DecoratedNonterminal<E,F>(split1.getWccs(hole1).get(0), split2.getWccs(hole2).get(0));
                            if( ! out.containsSplitFor(newNt)  ) {
                                newNts.add(newNt);
                            }

                            newSplit.addWcc(hole1, newNt);
                        }

                        if( split1.getAllDominators().isEmpty() ) {
                        	if( in1.isFinal(nt1) && in2.isFinal(nt2) ) {
                        		out.addSplit(nt, newSplit);
                        		out.setFinal(nt);
                        	}
                        } else {
                            out.addSplit(nt, newSplit);
                        }

                        for( DecoratedNonterminal<E,F> newNt : newNts ) {
                            intersectPopulate(newNt, out, in1, in2);
                        }
                    }
                }
            }
        }
    }

    public boolean isFinal(E nt) {
    	return finalStates.contains(nt);
    }

    public void setFinal(E nt) {
    	finalStates.add(nt);
    }

    public static void main(String[] args) throws Exception {
        RtgParser parser = new RtgParser();
        DomconOzInputCodec codec = new DomconOzInputCodec();
        DomGraph graph = new DomGraph();
        NodeLabels labels = new NodeLabels();
        Chart chart = new Chart(labels);

        codec.decode(new FileReader(new File(args[0])), graph, labels);
        ChartSolver.solve(graph, chart);

        RegularTreeGrammar<StringNonterminal> g2 = parser.read(new FileReader(new File(args[1])));
        RegularTreeGrammar<DecoratedNonterminal<SubgraphNonterminal,StringNonterminal>> inter = chart.intersect(g2);


        System.err.println(inter.countSolvedForms());

        System.err.println("after:");
        //System.err.println(ChartPresenter.chartOnlyRoots(inter, graph));
        System.err.println(inter);
    }
}
