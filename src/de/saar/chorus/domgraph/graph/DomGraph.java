/*
 * @(#)DomGraph.java created 25.01.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
        this(g,null,null);
    }
    
    public DomGraph(DomGraph g, Set<String> nodes, Set<Edge> edges) {
        graph = new DirectedSubgraph(g.graph, nodes, edges);
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
    
    public List<Edge> getInEdges(String node, EdgeType type) {
        List<Edge> ret = new ArrayList<Edge>();
      
        for( Object _edge : graph.incomingEdgesOf(node) ) {
            Edge edge = (Edge) _edge;
            EdgeType myType = getData(edge).getType();
            
            if( (type == null) || (type == myType) ) {
                ret.add(edge);
            }
        }
        
        return ret;
    }
    
    public List<String> getParents(String node, EdgeType type) {
        List<String> parents = new ArrayList<String>();
        
        for( Edge e : getInEdges(node, type)) {
            parents.add((String) e.getSource() );
        }
        
        return parents;
    }

    
    public List<Edge> getOutEdges(String node, EdgeType type) {
        List<Edge> ret = new ArrayList<Edge>();
      
        for( Object _edge : graph.outgoingEdgesOf(node) ) {
            Edge edge = (Edge) _edge;
            EdgeType myType = getData(edge).getType();
            
            if( (type == null) || (type == myType) ) {
                ret.add(edge);
            }
        }
        
        return ret;
    }
    
    public List<Edge> getAdjacentEdges(String node) {
    	return (List<Edge>) graph.edgesOf(node);
    }
    
    public List<String> getChildren(String node, EdgeType type) {
        List<String> children = new ArrayList<String>();
        
        for( Edge e : getOutEdges(node, type)) {
            children.add((String) e.getTarget() );
        }
        
        return children;
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
    
    public int indeg(String node, EdgeType type) {
        return getInEdges(node,type).size();
    }
    
    public int outdeg(String node, EdgeType type) {
        return getOutEdges(node,type).size();
    }
    
    public boolean isRoot(String node) {
        return indeg(node,EdgeType.TREE) == 0;
    }
    
    public Set<String> getAllRoots() {
        Set<String> ret = new HashSet<String>();
        
        for( String node : getAllNodes() ) {
            if( isRoot(node) ) {
                ret.add(node);
            }
        }
        
        return ret;
    }
    

    
    /**
     * Compute the weakly connected components of the selected subgraph.
     * 
     * @param components the elements of this collection will be the sets of nodes in the different wccs
     * @return the number of wccs
     */
    public List<Set<String>> wccs() {
        final List<Set<String>> components = new ArrayList<Set<String>>();
        DepthFirstIterator it = new DepthFirstIterator(new AsUndirectedGraph(graph), null);
        
        it.addTraversalListener(new TraversalListenerAdapter() {
            Set<String> thisComponent;

            public void connectedComponentStarted(ConnectedComponentTraversalEvent e) {
                thisComponent = new HashSet<String>();
            }
            
            public void vertexTraversed(VertexTraversalEvent e) {
                String node = (String) e.getVertex();
                thisComponent.add(node);
            }

            public void connectedComponentFinished(ConnectedComponentTraversalEvent e) {
                components.add(thisComponent);
            }
        });

        // run DFS
        while( it.hasNext() ) {
            it.next();
        }
        
        return components;
    }
    
    public Map<String,Integer> computeWccMap() {
        List<Set<String>> wccs = wccs();
        Map<String,Integer> wccMap = new HashMap<String,Integer>();
        
        for( int i = 0; i < wccs.size(); i++ ) {
            Integer index = new Integer(i);
            for( String node : wccs.get(i) ) {
                wccMap.put(node,index);
            }
        }
        
        return wccMap;
    }
    
    public Set<String> pickRootsFrom(Set<String> nodeset) {
        Set<String> ret = new HashSet<String>();
        
        for( String node : nodeset ) {
            if( isRoot(node))
                ret.add(node);
        }
        
        return ret;
    }
    
    private void removeAllDominanceEdges() {
        List<Edge> allEdges = new ArrayList<Edge>();
        allEdges.addAll(getAllEdges());
        
        for( int i = 0; i < allEdges.size(); i++ ) {
            Edge e = allEdges.get(i);
            if( getData(e).getType() == EdgeType.DOMINANCE ) {
                graph.removeEdge(e);
            }
        }
    }

    public void setDominanceEdges(Collection<DomEdge> domedges) {
        removeAllDominanceEdges();
        
        for( DomEdge e : domedges ) {
            addEdge(e.getSrc(), e.getTgt(), new EdgeData(EdgeType.DOMINANCE, "(domedge)"));
        }
    }
    
    
    
    
    /***** graph classes ******/
    
    public boolean isSimpleSolvedForm() {
        for( String node : getAllNodes() ) {
            // TODO check cyclicity
            
            if( indeg(node) > 1 ) {
                return false;
            }
            
            if( outdeg(node, EdgeType.DOMINANCE) > 1 ) {
                return false;
            }
        }
        
        return true;
    }
    
    public boolean isNormal() {
        // TODO implement me
        return true;
    }
    
}
    