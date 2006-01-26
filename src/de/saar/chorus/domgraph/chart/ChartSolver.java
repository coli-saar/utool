/*
 * @(#)ChartSolver.java created 25.01.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.chart;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org._3pq.jgrapht.Edge;

import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.EdgeType;

public class ChartSolver {
    private DomGraph completeGraph;
    private Chart chart;
    
    // ASSUMPTION graph is compact and weakly normal
    public ChartSolver(DomGraph graph, Chart chart) {
        completeGraph = graph;
        this.chart = chart;
    }
    
    public boolean solve() {
        // TODO split unconnected graphs into their wccs first
        chart.addCompleteFragset(completeGraph.getAllRoots());
        return solve(new DomGraph(completeGraph));
    }
    
    private boolean solve(DomGraph graph) {
        Set<String> fragset = graph.getAllRoots();
        Set<String> freeRoots;

        Map<Integer,String> wccIndexToHole = new HashMap<Integer,String>();
        Map<String,String> nodeToHole = new HashMap<String,String>();
        
        List<String> hiddenNodesWcc = new ArrayList<String>();
        List<Edge> hiddenEdgesWcc = new ArrayList<Edge>();

        
        
        // If fragset is already in chart, nothings needs to be done.
        if( chart.containsSplitFor(fragset) ) {
            return true;
        }
        
        // If the fs has no free roots, then the original graph is unsolvable.
        freeRoots = getFreeRoots(graph);
        if( freeRoots.isEmpty() ) {
            return false;
        }
        
        // If fs is singleton and its root is free, it is in solved form.
        // The fs will be entered into the chart as part of the parent's split.
        if( fragset.size() == 1 ) {
            return true;
        }
        
        // Otherwise, iterate over all possible free roots
        for( String root : freeRoots ) {
            Split split = new Split(root);
            List<String> holes = graph.getChildren(root, EdgeType.TREE);
            List<Edge> hiddenEdges = graph.getOutEdges(root, EdgeType.TREE);
            
            wccIndexToHole.clear();
            nodeToHole.clear();
            
            // remove root (and its outgoing tree edges) from the graph
            graph.hide(root);
            
            // determine the hole (or root) to which each root in the graph
            // is connected
            Map<String,Integer> wccMap = graph.computeWccMap();
            for( String hole : holes ) {
                wccIndexToHole.put(wccMap.get(hole), hole);
            }
            
            for( String node : graph.getAllRoots() ) {
                Integer wccOfNode = wccMap.get(node);
                if( wccIndexToHole.containsKey(wccOfNode)) {
                    nodeToHole.put(node, wccIndexToHole.get(wccOfNode));
                } else {
                    nodeToHole.put(node, root);
                }
            }
            
            // now remove holes so the graph is split into the smaller wccs
            for( String hole : holes ) {
                hiddenEdges.addAll(graph.getOutEdges(hole, null));
                graph.hide(hole);
            }
            
            for( Set<String> wcc : graph.wccs() ) {
                Set<String> rootsInWcc = graph.pickRootsFrom(wcc);
                String wccDominator = nodeToHole.get(rootsInWcc.iterator().next()); 
                
                split.addWcc(wccDominator, rootsInWcc);
                
                hiddenEdgesWcc.clear();
                hiddenNodesWcc.clear();
                hideAllNodesExcept(graph, wcc, hiddenNodesWcc, hiddenEdgesWcc);

                if( !solve(graph) ) {
                    return false;
                }
                
                restoreAll(graph, hiddenNodesWcc, hiddenEdgesWcc);
            }
            
            for( String hole : holes ) {
                graph.restore(hole);
            }
            
            chart.addSplit(fragset, split);
            
            // put deleted nodes and holes back into the graph
            graph.restore(root);
            for( String hole : holes ) {
                graph.restore(hole);
            }
            for( Edge e : hiddenEdges ) {
                graph.restore(e);
            }
        }
        
        
        return true;
    }

    private void restoreAll(DomGraph graph, List<String> hiddenNodes, List<Edge> hiddenEdges) {
    	for( int i = 0; i < hiddenNodes.size(); i++ ) {
    		String node = hiddenNodes.get(i);
            graph.restore(node);
        }
    	
    	for( int i = 0; i < hiddenEdges.size(); i++ ) {
    		Edge e = hiddenEdges.get(i);
    		graph.restore(e);
    	}
    	
    }

    private void hideAllNodesExcept(DomGraph graph, Set<String> nodes, List<String> hiddenNodes, List<Edge> hiddenEdges) {
        List<String> allnodes = new ArrayList<String>();
        
        allnodes.addAll(graph.getAllNodes());
        
        for( int i = 0; i < allnodes.size(); i++ ) {
        	String node = allnodes.get(i);
            if( !nodes.contains(node) ) {
                hiddenNodes.add(node);
                hiddenEdges.addAll(graph.getAdjacentEdges(node));
                graph.hide(node);
            }
        }
    }

    
    // compute the free roots of a graph
    private Set<String> getFreeRoots(DomGraph graph) {
        Set<String> ret = new HashSet<String>();
        
        for( String node : graph.getAllNodes() ) {
            boolean isFree = true;
            
            if( graph.indeg(node) == 0 ) {
                // roots without incoming dom edges
                List<String> holes = graph.getChildren(node, EdgeType.TREE);
                Set<Integer> seenWccs = new HashSet<Integer>();

                // compute wccs of G - R(F)
                DomGraph subsubgraph = new DomGraph(graph);
                subsubgraph.hide(node);
                Map<String,Integer> wccMap = subsubgraph.computeWccMap();
                
                // check whether all holes are in different wccs
                for( String hole : holes ) {
                    if( seenWccs.contains(wccMap.get(hole))) {
                        isFree = false;
                    } else {
                        seenWccs.add(wccMap.get(hole));
                    }
                }
                
                // TODO what about dom edges out of the root?
                
                if( isFree ) {
                    ret.add(node);
                }
            }
        }
        
        return ret;
    }
    
    public Chart getChart() {
        return chart;
    }
}
