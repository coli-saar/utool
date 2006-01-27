/*
 * @(#)FreeRootsComputer.java created 27.01.2006
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

import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.EdgeType;

class FreeRootsComputer {
    private DomGraph graph;
    private DirectedGraph lowlevel;
    
    private TObjectIntHashMap bccIndex;
    
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
        
        seenEdgeIndices = new TIntHashSet();
        
        bccIndex = new TObjectIntHashMap();
        nextBccIndex = 0;
        
        bccs(graph.getAllNodes().iterator().next());
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
    }
}