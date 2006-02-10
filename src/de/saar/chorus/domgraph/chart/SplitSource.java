/*
 * @(#)SplitSource.java created 03.02.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.chart;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org._3pq.jgrapht.Edge;

import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.EdgeType;

public abstract class SplitSource {
    protected DomGraph graph;
    /*
    protected List<String> potentialFreeRoots;
    protected List<Split> splits;
    protected Iterator<Split> splitIt;
    */
    
    public SplitSource(DomGraph graph) {
        this.graph = graph;
    }
    
    
    
    // IMPLEMENT THIS
    abstract protected Iterator<Split> computeSplits(Set<String> subgraph);

    
    
    protected List<String> computePotentialFreeRoots(Set<String> subgraph) {
        // initialise potentialFreeRoots with all nodes without
        // incoming dom-edges
        List<String> potentialFreeRoots = new ArrayList<String>();
        for( String node : subgraph ) {
            if( graph.indegOfSubgraph(node, null, subgraph) == 0 ) {
                potentialFreeRoots.add(node);
            }
        }
        
        return potentialFreeRoots;
    }


    
    
    /**** the class that actually does the dirty work ****/
    
    protected static class SplitComputer {
        private DomGraph graph;

        private String theRoot;
        private Set<String> rootFragment;
        
        // node in rootfrag -> edge in this node -> wcc
        private Map<String,Map<Edge,Set<String>>> splitmap;

        // node -> dom edge out of root frag by which this node is reached
        private Map<String,Edge> domEdgeForNodeWcc;
        
        
        // for dfs
        private Set<String> visited;

        
        public SplitComputer(DomGraph graph) {
            this.graph = graph;
            rootFragment = new HashSet<String>();
            domEdgeForNodeWcc = new HashMap<String,Edge>();
            splitmap = new HashMap<String,Map<Edge,Set<String>>>();
            
            
            visited = new HashSet<String>();
        }
        
        // ASSUMPTION: this method never visits a node in the root
        // fragment coming from outside the root fragment
        private void dfs(String node, Set<String> subgraph) throws RootNotFreeException {
            //System.err.println("dfs tries to visit " + node);
            
            // ASSUMPTION: this method is only called on unvisited nodes
            assert !visited.contains(node) : "DFS visited node twice";
            
            if( !subgraph.contains(node) ) {
                //System.err.println("not in subgraph!");
                return;
            }
            
            //System.err.println("visiting");
            visited.add(node);
            
            // If node is not in the root fragment, then determine
            // its wcc set and assign it to it.
            if( !rootFragment.contains(node) ) {
                assert domEdgeForNodeWcc.containsKey(node);
                assignNodeToWcc(node);
            }
            
            // otherwise, iterate over all adjacent edges, visiting
            // tree edges first
            List<Edge> edgeList = graph.getAdjacentEdges(node, EdgeType.TREE);
            edgeList.addAll(graph.getAdjacentEdges(node, EdgeType.DOMINANCE));
            
            for( Edge edge : edgeList ) {
                String neighbour = (String) edge.oppositeVertex(node);
                
                if( rootFragment.contains(neighbour) && !rootFragment.contains(node)) {
                    // edge into the root fragment from outside: we never traverse
                    // such an edge, but must check whether the neighbour is
                    // consistent with the wcc assignment

                    // ASSUMPTION: edge is a dom edge from neighbour to node
                    assert node.equals(edge.getTarget());
                    assert neighbour.equals(edge.getSource());
                    assert graph.getData(edge).getType() == EdgeType.DOMINANCE;
                    
                    if( !neighbour.equals(theRoot)
                            && !neighbour.equals(domEdgeForNodeWcc.get(node).getSource()) ) {
                        // dom edge goes into a hole that is not the one we came from
                        throw new RootNotFreeException();
                    }
                } else {
                    // any other edge -- let's explore it
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
            
            /*
            System.err.println("ude: " + neighbour + " from " + node + " via " + edge);
            System.err.println(graph.getData(edge).getType());
            System.err.println(rootFragment.contains(src));
            System.err.println(src == node);
            //System.err.println("src = /" + src);
             * */
            
            if( (graph.getData(edge).getType() == EdgeType.DOMINANCE)
                    && (src.equals(node))
                    && (rootFragment.contains(src)) ) {
                // dom edge out of fragment => initialise dEFNW
                // NB: If two dom edges out of the same hole point into the
                // same wcc, we will only ever use one of them, because the others'
                // target node will have been visited when we consider them.
                domEdgeForNodeWcc.put(tgt,edge);
                //System.err.println("put(" + tgt + "," + edge + ")");
            } else if( !rootFragment.contains(neighbour) ) {
                // otherwise make neighbours inherit my dEFNW
                domEdgeForNodeWcc.put(neighbour, domEdgeForNodeWcc.get(node));
                //System.err.println("inherit(" + neighbour + "," + domEdgeForNodeWcc.get(node));
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


        public Split computeSplit(String root, Set<String> subgraph)
        throws RootNotFreeException
        {
            // initialise root fragment
            rootFragment.clear();
            rootFragment.add(root);
            rootFragment.addAll(graph.getChildren(root, EdgeType.TREE));
            theRoot = root;
            
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

}
