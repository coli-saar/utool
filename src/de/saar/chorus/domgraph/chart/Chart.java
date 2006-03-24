/*
 * @(#)Chart.java created 25.01.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.chart;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org._3pq.jgrapht.util.ModifiableInteger;


/**
 * A chart for storing intermediate results of the graph-chart solver.
 * This data structure assigns to a weakly connected subgraph G of the original 
 * dominance graph a list of splits for G. A split records the choice of a fragment F
 * of G as the root fragment of a solved form of G, and how the other
 * fragments of G must be distributed over the holes of F. That is, it
 * splits G into a root fragment F and the weakly connected components
 * that remain after F is removed.<p>
 * 
 * All subgraphs and splits in a chart object are productive, in the sense
 * that each of them can be used in some solved form. This is guaranteed
 * initially by the {@link ChartSolver}. Then the Chart object keeps track 
 * of how often each subgraph is referenced
 * in the splits. The user designates one or more subgraphs as top-level
 * subgraphs, which will always retain a reference count of at least 1.
 * If the reference count of any other subgraph drops to zero at any point,
 * this subgraph and all of its splits will be deleted from the chart,
 * which may lead to the removal of other subgraphs, and so on. 
 * 
 * @author Alexander Koller
 *
 */
public class Chart {
    private Map<Set<String>, List<Split>> chart;
    private Map<Set<String>, ModifiableInteger> refcount;
    private int size;
    private List<Set<String>> toplevelSubgraphs;
    
    /**
     * The constructor.
     */
    public Chart() {
        chart = new HashMap<Set<String>, List<Split>>();
        refcount = new HashMap<Set<String>, ModifiableInteger>();
        toplevelSubgraphs = new ArrayList<Set<String>>();
        size = 0;
    }
    
    /**
     * Adds a split for the given subgraph.
     * 
     * @param subgraph a subgraph of some dominance graph
     * @param split a split of this subgraph
     */
    public void addSplit(Set<String> subgraph, Split split) {
        List<Split> splitset = chart.get(subgraph);
        if( splitset == null ) {
            splitset = new ArrayList<Split>();
            chart.put(subgraph, splitset);
            
        }
        
        // add split to the chart
        splitset.add(split);
        
        // update reference counts
        for( Set<String> subsubgraph : split.getAllSubgraphs() ) {
            incReferenceCount(subsubgraph);
        }
        
        size++;
    }
    
    /**
     * Increments the reference count for the given subgraph.
     * 
     * @param subgraph a subgraph
     */
    private void incReferenceCount(Set<String> subgraph) {
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
    private void decReferenceCount(Set<String> subgraph) {
        assert(refcount.containsKey(subgraph));
        
        ModifiableInteger x = refcount.get(subgraph);
        x.setValue(x.intValue()-1);
    }
    

    /**
     * Sets the splits for a given subgraph. If the subgraph already
     * had splits, these are deleted first. 
     * 
     * @param subgraph a subgraph
     * @param splits the new splits for this subgraph
     */
    public void setSplitsForSubgraph(Set<String> subgraph, List<Split> splits) {
        Set<Set<String>> subgraphsAllSplits = new HashSet<Set<String>>();
        List<Split> oldSplits = getSplitsFor(subgraph);
        
        // update reference count effects of deleting the old splits
        for( Split oldSplit : oldSplits ) {
            List<Set<String>> subsubgraphs = oldSplit.getAllSubgraphs(); 
            subgraphsAllSplits.addAll(subsubgraphs);
            
            for( Set<String> subsubgraph : subsubgraphs ) {
                decReferenceCount(subsubgraph);
            }
        }
        
        // delete the old splits
        size -= oldSplits.size();
        oldSplits.clear();
        
        
        // add the new split
        for( Split split : splits ) {
            addSplit(subgraph, split);
        }
        
        // remove subgraphs with zero reference count from the chart
        deleteUnproductiveSubgraphs(subgraphsAllSplits);
    }

    
    
    /**
     * Deletes all subgraphs from the given collection whose reference count is zero.
     * 
     * @param subgraphs a collection of subgraphs
     */
    private void deleteUnproductiveSubgraphs(Collection<Set<String>> subgraphs) {
        for( Set<String> subgraph : subgraphs ) {
            if( refcount.get(subgraph).getValue() == 0 ) {
                deleteSubgraph(subgraph);
            }
        }
    }

    /**
     * Deletes a subgraph and all of its splits from the chart.
     * 
     * @param subgraph a subgraph
     */
    private void deleteSubgraph(Set<String> subgraph) {
        List<Split> splits = getSplitsFor(subgraph);
        
        // update reference counts for referred sub-subgraphs and
        // recursively delete those if they drop to zero
        for( Split split : splits ) {
            for( Set<String> subsubgraph : split.getAllSubgraphs() ) {
                decReferenceCount(subsubgraph);
                
                if( refcount.get(subsubgraph).getValue() == 0 ) {
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
    public List<Split> getSplitsFor(Set<String> subgraph) {
        return chart.get(subgraph);
    }
    
    /**
     * Checks whether the chart contains a split for the given subgraph.
     * 
     * @param subgraph a subgraph
     * @return true iff the chart contains any splits for this subgraph.
     */
    public boolean containsSplitFor(Set<String> subgraph) {
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
    public String toString() {
        StringBuilder ret = new StringBuilder();
        
        for( Set<String> fragset : chart.keySet() ) {
            for( Split split : chart.get(fragset) ) {
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
    public List<Set<String>> getToplevelSubgraphs() {
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
    public void addToplevelSubgraph(Set<String> subgraph) {
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
        Map<Set<String>, BigInteger> numSolvedForms = new HashMap<Set<String>,BigInteger>();
        
        for( Set<String> subgraph : getToplevelSubgraphs() ) {
            ret = ret.multiply(countSolvedFormsFor(subgraph, numSolvedForms));
        }
        
        return ret;
    }

    private BigInteger countSolvedFormsFor(Set<String> subgraph, Map<Set<String>,BigInteger> numSolvedForms) {
        BigInteger ret = BigInteger.ZERO;
        
        if( numSolvedForms.containsKey(subgraph) ) {
            return numSolvedForms.get(subgraph);
        } else if( !containsSplitFor(subgraph) ) {
            // no split for subgraph => subgraph contains only one fragment
            return BigInteger.ONE;
        } else {
            for( Split split : getSplitsFor(subgraph) ) {
                BigInteger sfsThisSplit = BigInteger.ONE;
                
                for( Set<String> subsubgraph : split.getAllSubgraphs() ) {
                    sfsThisSplit = sfsThisSplit.multiply(countSolvedFormsFor(subsubgraph, numSolvedForms));
                }
                
                ret = ret.add(sfsThisSplit);
            }
            
            numSolvedForms.put(subgraph, ret);
            return ret;
        }
    }
}
