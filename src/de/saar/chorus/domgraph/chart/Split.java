/*
 * @(#)Split.java created 25.01.2006
 *
 * Copyright (c) 2006 Alexander Koller
 *
 */

package de.saar.chorus.domgraph.chart;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A split in a dominance chart. A split of a subgraph is
 * induced by the choice of a certain free fragment as the root
 * of a solved form of this subgraph. It also records the
 * weakly connected components into which the subgraph is split
 * by removing the free fragment, and to which hole of the
 * fragment the nodes of each WCC are connected.
 *
 * @author Alexander Koller
 *
 */
public class Split<E> {
    private final String rootFragment;
    private final Map<String,List<E>> wccs;  // node -> wccs
    private Map<String,String> substitution; // hole -> root
    private final List<String> dominators;

    /**
     * Creates a split with a given root fragment.
     *
     * @param rootFragment the root fragment of this split
     */
    public Split(String rootFragment) {
        this.rootFragment = rootFragment;
        wccs = new HashMap<String,List<E>>();
        substitution = new HashMap<String, String>();
        dominators = new ArrayList<String>();
    }

    /**
     * Adds a weakly connected component to a split representation.
     *
     * @param hole the hole of the free fragment to which the wcc is connected
     * @param wcc a weakly connected component of the subgraph
     */
    public void addWcc(String hole, E wcc) {
        List<E> wccSet = wccs.get(hole);

        if( wccSet == null ) {
            wccSet = new ArrayList<E>();
            wccs.put(hole, wccSet);
            dominators.add(hole);
        }

        wccSet.add(wcc);
    }


    /**
     * Returns the root fragment of this split.
     *
     * @return the root fragment
     */
    public String getRootFragment() {
        return rootFragment;
    }

    /**
     * Returns the set of weakly connected components which
     * are connected to the specified node.
     *
     * @param node a node of the root fragment
     * @return the list of wccs connected to this node, or
     * <code>null</code> if no wccs
     * are connected to it.
     */
    public List<E> getWccs(String node) {
        return wccs.get(node);
    }

    /**
     * Returns the set of holes of the root fragment
     * which are connected to any wcc.
     *
     * @return the set of holes
     */
    public List<String> getAllDominators() {
        return dominators;
    }


    /**
     * Returns the set of WCCs into which the subgraph
     * is split by removing the root fragment.
     *
     * @return the set of wccs
     */
    public List<E> getAllSubgraphs() {
        List<E> ret = new ArrayList<E>();

        for( String node : wccs.keySet() ) {
            ret.addAll(wccs.get(node));
        }

        return ret;
    }

    @Override
    public String toString() {
        return "<" + rootFragment + " " + wccs + ", subst =" + substitution + ">";
    }

	public Map<String, String> getSubstitution() {
		return substitution;
	}

	public void setSubstitution(Map<String,String> subst) {
		substitution = subst;
	}
}
