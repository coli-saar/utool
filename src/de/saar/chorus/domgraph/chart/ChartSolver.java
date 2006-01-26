/*
 * @(#)ChartSolver.java created 25.01.2006
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

import org._3pq.jgrapht.Edge;
import org._3pq.jgrapht.event.EdgeTraversalEvent;
import org._3pq.jgrapht.event.TraversalListenerAdapter;
import org._3pq.jgrapht.event.VertexTraversalEvent;
import org._3pq.jgrapht.graph.AsUndirectedGraph;

import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.EdgeType;
import de.saar.chorus.domgraph.graph.RestrictedDepthFirstIterator;

public class ChartSolver {
    private DomGraph graph;
    private Chart chart;
    private SplitComputer splitc;
    
    // ASSUMPTION graph is compact and weakly normal
    public ChartSolver(DomGraph graph, Chart chart) {
        this.graph = graph;
        this.chart = chart;
        splitc = new SplitComputer(graph);
    }
    
    public boolean solve() {
        List<Set<String>> wccs = graph.wccs();

        for( Set<String> wcc : wccs ) {
            chart.addCompleteFragset(wcc);
            if( !solve(wcc) ) {
                return false;
            }
        }
        
        return true;
    }
    
    
    private boolean solve(Set<String> subgraph) {
        Set<String> freeRoots;
        int numRootsInSubgraph;
        
        //System.err.println("solve " + subgraph);
        
        // TODO these could be defined once and for all in the object
        Map<String,Map<Edge,Set<String>>> splitmap = new HashMap<String,Map<Edge,Set<String>>>();
        //List<String> hiddenNodesWcc = new ArrayList<String>();

        
        
        // If fragset is already in chart, nothings needs to be done.
        if( chart.containsSplitFor(subgraph) ) {
            return true;
        }
        
        // If the fs has no free roots, then the original graph is unsolvable.
        freeRoots = getFreeRoots(subgraph);
        if( freeRoots.isEmpty() ) {
            return false;
        }
        
        // If fs is singleton and its root is free, it is in solved form.
        // The fs will be entered into the chart as part of the parent's split.
        numRootsInSubgraph = 0;
        for( String node : subgraph ) {
            if( graph.indegOfSubgraph(node, EdgeType.TREE, subgraph) == 0 ) {
                numRootsInSubgraph++;
            }
        }
        
        if( numRootsInSubgraph == 1 ) {
            return true;
        }
        
        // Otherwise, iterate over all possible free roots
        for( String root : freeRoots ) {
            Split split = new Split(root);
            
            splitmap.clear();
            splitc.computeSplit(root, subgraph, splitmap);
            for( String dominator : splitmap.keySet() ) {
                for( Edge domedge : splitmap.get(dominator).keySet() ) {
                    Set<String> wcc = splitmap.get(dominator).get(domedge);

                    split.addWcc(dominator, wcc);
                    if( !solve(wcc) ) {
                        return false;
                    }
                }
            }
            
            chart.addSplit(subgraph, split);
        }
        
        return true;
    }

    /*
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
*/
    
    // compute the free roots of a subgraph
    private Set<String> getFreeRoots(Set<String> subgraph) {
        Set<String> ret = new HashSet<String>();
        Set<Integer> seenWccs = new HashSet<Integer>();
        Set<String> subsubgraph = new HashSet<String>();
        
        subsubgraph.addAll(subgraph);
        
        for( String node : subgraph ) {
            boolean isFree = true;

            if( graph.indegOfSubgraph(node, null, subgraph) == 0 ) {
                // roots without incoming edges
                
                // INVARIANT tree children of a root are always in the same subgraph
                List<String> holes = graph.getChildren(node, EdgeType.TREE);
                
                // compute wccs of G - R(F)
                subsubgraph.remove(node);
                Map<String,Integer> wccMap = 
                    graph.computeWccMap(graph.wccsOfSubgraph(subsubgraph));
                subsubgraph.add(node);
                
                seenWccs.clear();
                
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



    private static class SplitComputer extends TraversalListenerAdapter {
        // global information (once per object)
        private DomGraph graph;
        private AsUndirectedGraph lowlevelGraph;

        // local information
        private Set<String> subgraph;
        private Set<String> rootFragment;
        
        // node -> dom edge out of root frag by which this node is reached
        private Map<String,Edge> domEdgeForNodeWcc;
        
        // node in rootfrag -> edge in this node -> wcc
        private Map<String,Map<Edge,Set<String>>> splitmap;
        

        
        public SplitComputer(DomGraph graph) {
            this.graph = graph;
            lowlevelGraph = new AsUndirectedGraph(graph.getLowlevelGraph());
            rootFragment = new HashSet<String>();
            domEdgeForNodeWcc = new HashMap<String,Edge>();
        }
        
        public void edgeTraversed(EdgeTraversalEvent e) {
            Edge edge = e.getEdge();
            String src = (String) edge.getSource();
            String tgt = (String) edge.getTarget();
            
            //System.err.println("et: " + edge);
            
            if( (graph.getData(edge).getType() == EdgeType.DOMINANCE)
                    && (rootFragment.contains(src)) ) {
                domEdgeForNodeWcc.put(tgt,edge);
            } else if( !rootFragment.contains(src) ) {
                if( domEdgeForNodeWcc.containsKey(src)) {
                    // traverse edge downwards
                    domEdgeForNodeWcc.put(tgt, domEdgeForNodeWcc.get(src));
                } else if( domEdgeForNodeWcc.containsKey(tgt) ) {
                    // traverse edge upwards
                    domEdgeForNodeWcc.put(src, domEdgeForNodeWcc.get(tgt));
                } else {
                    System.err.println("Can't inherit domedge: " + edge);
                }
            }
        }
        
        public void vertexTraversed(VertexTraversalEvent e) {
            String node = (String) e.getVertex();
            
            //System.err.println("vt: " + node);
            
            if( !rootFragment.contains(node) ) {
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
        }
        
        public void computeSplit(String root, Set<String> subgraph, Map<String,Map<Edge,Set<String>>> splitmap) {
            rootFragment.clear();
            rootFragment.add(root);
            rootFragment.addAll(graph.getChildren(root, EdgeType.TREE));

            this.splitmap = splitmap;
            this.subgraph = subgraph;
            domEdgeForNodeWcc.clear();
            
            splitmap.clear();
            
            RestrictedDepthFirstIterator it = new RestrictedDepthFirstIterator(lowlevelGraph, root, subgraph);
            it.addTraversalListener(this);
            it.setReuseEvents(true);
            
            while( it.hasNext() ) {
                it.next();
            }
            
            //System.err.println("after dfs: splitmap = " + splitmap);
        }
    }

    
    /*      
            // found dom edge out of the root fragment
            List<Set<String>> splitmapThisDominator = splitmap.get(src);
            if( splitmapThisDominator == null ) {
                splitmapThisDominator = new ArrayList<Set<String>>();
                splitmap.put(src, splitmapThisDominator);
            }
            
            currentWcc = new HashSet<String>();
            splitmapThisDominator.add(currentWcc);
            
            System.err.println("new dominator: " + src);
            System.err.println("edge event: " + e);
        }
    }
*/
    

}
