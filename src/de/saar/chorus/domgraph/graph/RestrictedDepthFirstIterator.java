/*
 * @(#)RestrictedDepthFirstIterator.java created 26.01.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.graph;

import java.util.Set;

import org._3pq.jgrapht.Graph;
import org._3pq.jgrapht.traverse.DepthFirstIterator;

/**
 * This class implements a <code>JGraphT</code> depth-first-search
 * iterator which never leaves the specified subgraph.
 *  
 * @author Alexander Koller
 *
 */
class RestrictedDepthFirstIterator extends DepthFirstIterator {
    protected Set<String> subgraph;

    public RestrictedDepthFirstIterator(Graph g, Object startVertex, Set<String> subgraph) {
        super(g, startVertex);
        this.subgraph = subgraph;
        
        // make sure that the dfs will never visit any nodes outside of wcc
        for( Object node : g.vertexSet() ) {
            if( !subgraph.contains(node) ) {
                putSeenData(node, null);
            }
        }
    }

    public RestrictedDepthFirstIterator(Graph g, Set<String> wcc) {
        this(g, null, wcc);
    }


    /**
     * @see org._3pq.jgrapht.traverse.CrossComponentIterator#encounterVertex(java.lang.Object,
     *      org._3pq.jgrapht.Edge)
     */
    /*
    protected void encounterVertex( Object vertex, Edge edge ) {
        if( wcc.contains(vertex) ) {
            super.encounterVertex(vertex,edge);
        }
    }
    */
}
