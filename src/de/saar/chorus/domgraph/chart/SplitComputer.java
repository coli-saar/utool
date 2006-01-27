/*
 * @(#)SplitComputer.java created 27.01.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.chart;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org._3pq.jgrapht.DirectedGraph;
import org._3pq.jgrapht.Edge;

import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.EdgeType;

class SplitComputer {
    private DomGraph graph;
    private DirectedGraph lowlevel;

    private Set<String> rootFragment;
    
    // node in rootfrag -> edge in this node -> wcc
    private Map<String,Map<Edge,Set<String>>> splitmap;

    // node -> dom edge out of root frag by which this node is reached
    private Map<String,Edge> domEdgeForNodeWcc;
    
    
    // for dfs
    Set<String> visited;

    
    public SplitComputer(DomGraph graph) {
        this.graph = graph;
        lowlevel = graph.getLowlevelGraph();
        rootFragment = new HashSet<String>();
        domEdgeForNodeWcc = new HashMap<String,Edge>();
        splitmap = new HashMap<String,Map<Edge,Set<String>>>();
        
        
        visited = new HashSet<String>();
    }
    
    
    private void dfs(String node, Set<String> subgraph) {
        if( !subgraph.contains(node) )
            return;
        
        if( visited.contains(node) ) {
            // NOP -- so far
        } else {
            visited.add(node);
            
            // If node is not in the root fragment, then determine
            // its wcc set and assign it to it.
            if( !rootFragment.contains(node) ) {
                assignNodeToWcc(node);
            }
            
            // otherwise, iterate over all adjacent edges
            for( Edge edge : (List<Edge>) lowlevel.edgesOf(node) ) {
                String neighbour = (String) edge.oppositeVertex(node);
                
                if( !visited.contains(neighbour)) {
                    updateDomEdge(neighbour, node, edge);
                
                    //System.err.println("Explore edge: " + edge);
                    
                    // recurse into children
                    dfs(neighbour, subgraph);
                }
            }
        }
    }
    
    private void updateDomEdge(String neighbour, String node, Edge edge) {
        String src = (String) edge.getSource(); // not necessarily = node
        String tgt = (String) edge.getTarget();
        
        // BUG -- this code is only correct for graphs in which
        // the ends of the dom edges out of the root fragment are
        // not otherwise connected; i.e. it assumes that different
        // dom edges lead to different wccs. This is true for hnc
        // graphs in particular, but in general it is wrong!!
        if( (graph.getData(edge).getType() == EdgeType.DOMINANCE)
                && (src == node)
                && (rootFragment.contains(src)) ) {
            // dom edge out of fragment => initialise dEFNW
            domEdgeForNodeWcc.put(tgt,edge);
        } else if( !rootFragment.contains(neighbour) ) {
            // otherwise make neighbours inherit my dEFNW
            domEdgeForNodeWcc.put(neighbour, domEdgeForNodeWcc.get(node));
        } else {
          //  System.err.println("Can't inherit domedge: " + edge);
        }
    }


    private void assignNodeToWcc(String node) {
        Edge dominatorEdge = domEdgeForNodeWcc.get(node);
        String dominator = (String) dominatorEdge.getSource();
        
        Map<Edge,Set<String>> thisMap = splitmap.get(dominator);
        if( thisMap == null ) {
            thisMap = new HashMap<Edge,Set<String>>();
            splitmap.put(dominator, thisMap);
        }
        
        Set<String> thisWcc = thisMap.get(dominatorEdge);
        if( thisWcc == null ) {
            thisWcc = new HashSet<String>();
            thisMap.put(dominatorEdge, thisWcc);
        }
        
        thisWcc.add(node);
    }


    public Split computeSplit(String root, Set<String> subgraph) {
        // initialise root fragment
        rootFragment.clear();
        rootFragment.add(root);
        rootFragment.addAll(graph.getChildren(root, EdgeType.TREE));

        // perform DFS
        domEdgeForNodeWcc.clear();
        splitmap.clear();
        visited.clear();
        dfs(root,subgraph);

        // build Split object
        Split ret = new Split(root);
        
        for( String dominator : splitmap.keySet() ) {
            for( Edge key : splitmap.get(dominator).keySet() ) {
                ret.addWcc(dominator, splitmap.get(dominator).get(key));
            }
        }
        
        return ret;
    }
}