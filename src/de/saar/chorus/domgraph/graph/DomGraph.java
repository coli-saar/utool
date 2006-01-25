/*
 * @(#)DomGraph.java created 25.01.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.graph;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org._3pq.jgrapht.DirectedGraph;
import org._3pq.jgrapht.Edge;
import org._3pq.jgrapht.event.ConnectedComponentTraversalEvent;
import org._3pq.jgrapht.event.TraversalListenerAdapter;
import org._3pq.jgrapht.event.VertexTraversalEvent;
import org._3pq.jgrapht.graph.AsUndirectedGraph;
import org._3pq.jgrapht.graph.DefaultDirectedGraph;
import org._3pq.jgrapht.graph.DirectedSubgraph;
import org._3pq.jgrapht.traverse.DepthFirstIterator;

public class DomGraph {
    private DirectedGraph graph;
    private Map<String,NodeData> nodeData;
    private Map<Edge,EdgeData> edgeData;
    private boolean isSubgraph;
    
    public DomGraph() {
        isSubgraph = false;
        clear();
    }
    
    public DomGraph(DomGraph g) {
        graph = new DirectedSubgraph(g.graph, null, null);
        nodeData = g.nodeData;
        edgeData = g.edgeData;
        isSubgraph = true;
    }
    
    
    
    public void clear() {
        if( !isSubgraph ) {
            graph = new DefaultDirectedGraph();
            nodeData = new HashMap<String,NodeData>();
            edgeData = new HashMap<Edge,EdgeData>();
        }
    }
    
    public void addNode(String name, NodeData data) {
        if( !isSubgraph ) {
            graph.addVertex(name);
            nodeData.put(name,data);
        }
    }
    
    public void addEdge(String src, String tgt, EdgeData data) {
        if( !isSubgraph ) {
            Edge e = graph.addEdge(src,tgt);
            edgeData.put(e, data);
        }
    }
    
    public Set<String> getAllNodes() {
        return graph.vertexSet();
    }
    
    public Set<Edge> getAllEdges() {
        return graph.edgeSet();
    }
    
    
    

    public void hide(String node) {
        if( isSubgraph )
            graph.removeVertex(node);
    }
    
    public void restore(String node) {
        if( isSubgraph )
            graph.addVertex(node);
    }
    
    public void hide(Edge e) {
        if( isSubgraph )
            graph.removeEdge(e);
    }
    
    public void restore(Edge e) {
        if( isSubgraph )
            graph.addEdge(e);
    }
    
    
    
    
    public NodeData getData(String node) {
        return nodeData.get(node);
    }
    
    public EdgeData getData(Edge edge) {
        return edgeData.get(edge);
    }
    
    
    
    
    public int indeg(String node) {
        return graph.inDegreeOf(node);
    }
    
    public int outdeg(String node) {
        return graph.outDegreeOf(node);
    }
    
    

    
    /**
     * Compute the weakly connected components of the selected subgraph.
     * 
     * @param components the elements of this collection will be the sets of roots in the different wccs
     * @return the number of wccs
     */
    public int wccs(final Collection<Set<String>> components) {
        Set<String> nodes = getAllNodes();
        Set<String> visited = new HashSet<String>();
        DepthFirstIterator it = new DepthFirstIterator(new AsUndirectedGraph(graph), null);
        
        components.clear();

        it.addTraversalListener(new TraversalListenerAdapter() {
            Set<String> thisComponent;
            int componentId = 0;

            public void connectedComponentStarted(ConnectedComponentTraversalEvent e) {
                thisComponent = new HashSet<String>();
            }
            
            public void vertexTraversed(VertexTraversalEvent e) {
                thisComponent.add((String) e.getVertex());
                System.err.println(e.getVertex() + " is in wcc " + componentId);
            }

            public void connectedComponentFinished(ConnectedComponentTraversalEvent e) {
                components.add(thisComponent);
                componentId++;
            }
        });

        // run DFS
        while( it.hasNext() ) {
            it.next();
        }
        
        return components.size();
    }
    
}
    