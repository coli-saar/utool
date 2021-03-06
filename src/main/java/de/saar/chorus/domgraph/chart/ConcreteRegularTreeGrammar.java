package de.saar.chorus.domgraph.chart;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org._3pq.jgrapht.util.ModifiableInteger;

public class ConcreteRegularTreeGrammar<E> extends RegularTreeGrammar<E> implements Cloneable {
    protected Map<E, List<Split<E>>> chart;
    protected Map<E, ModifiableInteger> refcount;
    protected int size;
    protected List<E> toplevelSubgraphs;
    protected Set<E> finalStates;

    protected Map<E,String> singletons;


    /**
     * The constructor.
     */
    public ConcreteRegularTreeGrammar() {
        chart = new HashMap<E, List<Split<E>>>();
        refcount = new HashMap<E, ModifiableInteger>();
        toplevelSubgraphs = new ArrayList<E>();
        size = 0;
        finalStates = new HashSet<E>();

        singletons = new HashMap<E,String>();
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

        /*//TA -- removed for efficiency, and because I think we'll end up not needing this
        // update reference counts
        for( E subsubgraph : split.getAllSubgraphs() ) {
            incReferenceCount(subsubgraph);
        }
        */

        size++;
    }
    
    public Object clone() {
    	ConcreteRegularTreeGrammar<E> ret = new ConcreteRegularTreeGrammar<E>();

        for( Map.Entry<E, List<Split<E>>> entry : chart.entrySet() ) {
            ret.chart.put(entry.getKey(), new ArrayList<Split<E>>(entry.getValue()));
        }

        for( Map.Entry<E, ModifiableInteger> entry : refcount.entrySet() ) {
            ret.refcount.put(entry.getKey(), new ModifiableInteger(entry.getValue().getValue()));
        }

        for( Map.Entry<E, String> entry : singletons.entrySet() ) {
        	ret.singletons.put(entry.getKey(), new String(entry.getValue()));
        }
        
        ret.size = size;

        ret.toplevelSubgraphs = new ArrayList<E>(toplevelSubgraphs);
        ret.finalStates = new HashSet<E>(finalStates);
        return ret;
    }

    public static <E> ConcreteRegularTreeGrammar<E> makeExplicit(RegularTreeGrammar<E> rtg) {
        ConcreteRegularTreeGrammar<E> ret = new ConcreteRegularTreeGrammar<E>();

        for( E nt : rtg.getAllNonterminals() ) {
            for( Split<E> split : rtg.getSplitsFor(nt)) {
                ret.addSplit(nt, split);
            }
        }

        for( E nt : rtg.getToplevelSubgraphs() ) {
            ret.addToplevelSubgraph(nt);
        }

        ret.recomputeSingletons();
        
        return ret;
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

    @Override
    public Set<E> getAllNonterminals() {
        return chart.keySet();
    }

    /**
     * Recomputes the singleton subgraphs (in RTG language: the preterminal
     * nonterminals) for the RTG.  It is expected that this method is called
     * whenever the RTG changes.
     */
    public void recomputeSingletons() {
        singletons.clear();

        for( E lhs : chart.keySet() ) {
            for( Split<E> split : chart.get(lhs) ) {
                if( split.getAllDominators().isEmpty() ) {
                    singletons.put(lhs, split.getRootFragment());
                }
            }
        }
    }

    public boolean isSingleton(E nt) {
        return singletons.containsKey(nt);
    }

    public String getRootForSingleton(E nt) {
        return singletons.get(nt);
    }



	public void cleanup() {
		recomputeSingletons();
		reduce();
	}


	
    /**
     * Removes all unproductive nonterminals and splits from this chart.
     * A nonterminal is called unproductive if it isn't possible to derive a solved
     * form from it (but it may still be inaccessible from the top-level subgraphs).<p>
     *
     * This method accesses the precomputed preterminals of this RTG, and therefore
     * assumes that recomputeSingletons() has been called since the latest change
     * to the RTG.
     *
     * @param roots the roots of the dominance graph on which this chart is based
     */
    public void reduce() {
        Set<E> usefulNonterminals = new HashSet<E>();
        Map<E,List<Split<E>>> nonterminalUses = new HashMap<E,List<Split<E>>>();
        Map<Split<E>,List<E>> splitToLhs = new HashMap<Split<E>,List<E>>();
        Queue<E> agenda = new LinkedList<E>();

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


            }
        }

        // bottom-up pass: initialize agenda with singletons
        agenda.addAll(singletons.keySet());

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
    
	@Override
	public void getSplitsFor(E subgraph, String label, List<Split<E>> ret) {
		ret.clear();
		
		for( Split<E> split : getSplitsFor(subgraph)) {
			if( split.getRootFragment().equals(label) ) {
				ret.add(split);
			}
		}
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
    @Override
    public int size() {
        return this.size;
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


    





    public void clear() {
        toplevelSubgraphs.clear();
        chart.clear();
        numSolvedForms.clear();
        refcount.clear();
        size = 0;
    }


    public boolean isFinal(E nt) {
    	return finalStates.contains(nt);
    }

    public void setFinal(E nt) {
    	finalStates.add(nt);
    }



}
