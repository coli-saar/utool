/*
 * @(#)ChartSolver.java created 25.01.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.chart;

import gnu.trove.TIntHashSet;
import gnu.trove.TObjectIntHashMap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org._3pq.jgrapht.DirectedGraph;
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
    private FreeRootsComputer freerc;
    
    public long totalSplitTime = 0;
    
    // ASSUMPTION graph is compact and weakly normal
    public ChartSolver(DomGraph graph, Chart chart) {
        this.graph = graph;
        this.chart = chart;
        
        splitc = new SplitComputer(graph);
        freerc = new FreeRootsComputer(graph);
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
        //List<Set<String>> wccsThisNode = new ArrayList<Set<String>>();
        
        //System.err.println("solve " + subgraph);
        
        // If fragset is already in chart, nothings needs to be done.
        if( chart.containsSplitFor(subgraph) ) {
            return true;
        }
        
        // If the fs has no free roots, then the original graph is unsolvable.
        freeRoots = freerc.getFreeRoots(subgraph);
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
            // make new Split
            Split split = splitc.computeSplit(root, subgraph);
            
            // iterate over wccs
            for( Set<String> wcc : split.getAllSubgraphs() ) {
                if( !solve(wcc) ) {
                    return false;
                }
            }
            
            // add split to chart
            chart.addSplit(subgraph, split);
        }
        
        //System.err.println("/solve " + subgraph);
        return true;
    }
    
    public Chart getChart() {
        return chart;
    }

    
    
    
    private static class FreeRootsComputer {
        private DomGraph graph;
        private DirectedGraph lowlevel;
        
        private TObjectIntHashMap bccIndex;
        
        //private Map<Edge,Integer> bccIndex;
        
        // for bcc algorithm
        private Set<String> visited;
        private int count;
        private Stack<Edge> stack;
        private Set<Edge> stackAsSet;
        private Map<String,Integer> dfsnum;
        private Map<String,Integer> LOW;
        private Map<String,String> parent;
        private int nextBccIndex;
        
        // for getFreeRoots
        //private Set<Integer> seenEdgeIndices;
        private TIntHashSet seenEdgeIndices;
        
        
        // ASSUMPTION subgraph is connected
        public FreeRootsComputer(DomGraph graph) {
            this.graph = graph;
            lowlevel = graph.getLowlevelGraph();
            
            visited = new HashSet<String>();
            count = 1;
            stack = new Stack<Edge>();
            stackAsSet = new HashSet<Edge>();
            dfsnum = new HashMap<String,Integer>();
            LOW = new HashMap<String,Integer>();
            parent = new HashMap<String,String>();
            
            //seenEdgeIndices = new HashSet<Integer>();
            seenEdgeIndices = new TIntHashSet();
            
            //bccIndex = new HashMap<Edge,Integer>();
            bccIndex = new TObjectIntHashMap();
            nextBccIndex = 0;
            
            long start = System.currentTimeMillis();
            bccs(graph.getAllNodes().iterator().next());
            long end = System.currentTimeMillis();
            System.err.println("bcc computation: " + (end-start) + "ms");
        }
        
        
        public Set<String> getFreeRoots(Set<String> subgraph) {
            Set<String> ret = new HashSet<String>();
            
            for( String node : subgraph ) {
                if( graph.indegOfSubgraph(node, null, subgraph) == 0 ) {
                    // roots without incoming edges
                    
                    seenEdgeIndices.clear();
                    for( Edge treeEdge : graph.getOutEdges(node, EdgeType.TREE)) {
                        int idx = bccIndex.get(treeEdge);
                        if( seenEdgeIndices.contains(idx) ) {
                            continue;
                        } else {
                            seenEdgeIndices.add(idx);
                        }
                    }
                    
                    ret.add(node);
                }
            }
            
            return ret;
        }
                    


        // bcc algorithm from http://www.ececs.uc.edu/~gpurdy/lec24.html
        private void bccs(String v) {
            //System.err.println("bccs: " + v);
            
            // mark v "visited"
            visited.add(v);
            
            // dfsnum[v] = Count
            // ++Count
            dfsnum.put(v, count);
            LOW.put(v, count++);
            
            // for each vertex w in adjlist(v) do
            for( Edge e : (List<Edge>) lowlevel.edgesOf(v) ) {
                String w = (String) e.oppositeVertex(v);
                
                // if (v,w) is not on the STACK, push (v,w)
                if( !stackAsSet.contains(e)) {
                    stack.push(e);
                    stackAsSet.add(e);
                }
                
                // if w is "unvisited" then
                if( !visited.contains(w)) {
                    // set parent(w) = v
                    parent.put(w, v);
                    
                    // BICONSEARCH(w)
                    bccs(w);
                    
                    // if LOW[w] >= dfsnum[v] then a biconnected component
                    //   has been found;
                    if( LOW.get(w) >= dfsnum.get(v) ) {
                        // pop the edges up to and including
                        // (v,w); these are the component
                        do {
                            bccIndex.put(stack.peek(), nextBccIndex);
                        } while( stack.pop() != e );
                        
                        nextBccIndex++;
                    }
                    
                    // LOW[v] = min (LOW[v],LOw[w])
                    LOW.put(v, Math.min(LOW.get(v), LOW.get(w)));
                }
                
                // else if w is not parent[v] then
                else if( parent.get(v) != w ) {
                    //  LOW[v] = min(LOW[v], dfsnum[w])
                    LOW.put(v, Math.min(LOW.get(v), dfsnum.get(w)));
                }
            }
            
            //System.err.println("/bcc: " + v);
        }

    }

/*
 * performance ideas:
 * - pull management of splitmap entries out of the inner loop
 * - Trove
 */
    private static class SplitComputer extends TraversalListenerAdapter {
        // global information (once per object)
        private DomGraph graph;
        private DirectedGraph lowlevel;
        // node in rootfrag -> edge in this node -> wcc
        private Map<String,Map<Edge,Set<String>>> splitmap;

        // local information
        private Set<String> rootFragment;
        
        // node -> dom edge out of root frag by which this node is reached
        private Map<String,Edge> domEdgeForNodeWcc;
        
        
        // for dfs
        Set<String> visited;

        
        public SplitComputer(DomGraph graph) {
            this.graph = graph;
            //lowlevelGraph = new AsUndirectedGraph(graph.getLowlevelGraph());
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
                //System.err.println("Visit: " + node);
              
                
                
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
            rootFragment.clear();
            rootFragment.add(root);
            rootFragment.addAll(graph.getChildren(root, EdgeType.TREE));

            domEdgeForNodeWcc.clear();
            splitmap.clear();
            
            visited.clear();
            
            //System.err.println("rootfrag = " + rootFragment);
            dfs(root,subgraph);
            
            /*
            
            RestrictedDepthFirstIterator it = new RestrictedDepthFirstIterator(lowlevelGraph, root, subgraph);
            it.addTraversalListener(this);
            it.setReuseEvents(true);
            
            while( it.hasNext() ) {
                it.next();
            }
            */
            
            

            Split ret = new Split(root);
            
            for( String dominator : splitmap.keySet() ) {
                for( Edge key : splitmap.get(dominator).keySet() ) {
                    ret.addWcc(dominator, splitmap.get(dominator).get(key));
                }
            }
            
            return ret;
        }
        
        
        /*
        public void edgeTraversed(EdgeTraversalEvent e) {
            Edge edge = e.getEdge();
            String src = (String) edge.getSource();
            String tgt = (String) edge.getTarget();
            
            //System.err.println("et: " + edge);
            
            // BUG -- this code is only correct for graphs in which
            // the ends of the dom edges out of the root fragment are
            // not otherwise connected; i.e. it assumes that different
            // dom edges lead to different wccs. This is true for hnc
            // graphs in particular, but in general it is wrong!!
            
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
                    
                    // (perhaps we can recognise here that we are revisiting
                    // a hole, and mark the edge to remember it leads into
                    // the same wcc)
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
        
        public Split computeSplit(String root, Set<String> subgraph) {
            rootFragment.clear();
            rootFragment.add(root);
            rootFragment.addAll(graph.getChildren(root, EdgeType.TREE));

            domEdgeForNodeWcc.clear();
            splitmap.clear();
            
            RestrictedDepthFirstIterator it = new RestrictedDepthFirstIterator(lowlevelGraph, root, subgraph);
            it.addTraversalListener(this);
            it.setReuseEvents(true);
            
            while( it.hasNext() ) {
                it.next();
            }
            
            

            Split ret = new Split(root);
            
            for( String dominator : splitmap.keySet() ) {
                for( Edge key : splitmap.get(dominator).keySet() ) {
                    ret.addWcc(dominator, splitmap.get(dominator).get(key));
                }
            }
            
            return ret;
        }
        */
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
