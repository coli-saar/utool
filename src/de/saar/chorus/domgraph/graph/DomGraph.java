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

import com.sun.corba.se.spi.legacy.connection.GetEndPointInfoAgainException;

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
    
    public List<Edge> getAdjacentEdges(String node, EdgeType type) {
        List<Edge> ret = getInEdges(node,type);
        ret.addAll(getOutEdges(node,type));
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
    
    public int indegOfSubgraph(String node, EdgeType type, Set<String> subgraph) {
        List<String> parents = getParents(node, type);
        parents.retainAll(subgraph);
        return parents.size();
    }
    
    public int outdeg(String node, EdgeType type) {
        return getOutEdges(node,type).size();
    }
    
    public boolean isRoot(String node) {
        return indeg(node,EdgeType.TREE) == 0;
    }
    
    public boolean isLeaf(String node) {
        return outdeg(node, EdgeType.TREE) == 0;
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
        return wccsOfSubgraph(getAllNodes());
    }
    
    public List<Set<String>> wccsOfSubgraph(Set<String> nodes) {
        final List<Set<String>> components = new ArrayList<Set<String>>();
        RestrictedDepthFirstIterator it = 
            new RestrictedDepthFirstIterator(new AsUndirectedGraph(graph), null, nodes);
        
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
    
    public Map<String,Integer> computeWccMap(List<Set<String>> wccs) {
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
    
    public boolean isWeaklyNormal() {
        for( String node : getAllNodes() ) {
            if( getData(node).getType() == NodeType.UNLABELLED ) {
                // unlabelled nodes must be leaves
                if( !isLeaf(node) ) {
                    return false;
                }
            
                // no empty fragments: all unlabelled nodes must have incoming tree edges
                if( indeg(node, EdgeType.TREE) == 0 ) {
                    return false;
                }
            }
            
            // no two incoming tree edges
            if( indeg(node, EdgeType.TREE) > 1 ) {
                return false;
            }
            
            // TODO acyclic fragments
        }
        
        for( Edge edge : getAllEdges() ) {
            if( getData(edge).getType() == EdgeType.DOMINANCE ) {
                // dominance edges go into roots
                if( !isRoot((String) edge.getTarget()) ) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    public boolean isNormal() {
        if( !isWeaklyNormal() ) {
            return false;
        }
        
        for( Edge edge : getAllEdges() ) {
            if( getData(edge).getType() == EdgeType.DOMINANCE ) {
                // dominance edges go out of holes
                if( getData((String) edge.getSource()).getType() != NodeType.UNLABELLED ) {
                    return false;
                }
            }
        }
            
        return true;
    }
    
    public boolean isCompact() {
        if( !isWeaklyNormal() ) {
            return false;
        }
        
        for( String node : getAllNodes() ) {
            // no labelled nodes with incoming tree edges
            if( (getData(node).getType() == NodeType.LABELLED) && (indeg(node, EdgeType.TREE) > 0)) {
                return false;
            }
        }
        
        return true;
    }
    
    public boolean isCompactifiable() {
        if( !isWeaklyNormal() ) {
            return false;
        }
        
        for( Edge edge : getAllEdges() ) {
            if( getData(edge).getType() == EdgeType.DOMINANCE ) {
                // dominance edges go out of holes or roots
                String src = (String) edge.getSource();
                if( (getData(src).getType() != NodeType.UNLABELLED)
                        && !isRoot(src) ) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    public DomGraph compactify() {
        DomGraph ret = new DomGraph();
        
        // build fragments
        for( String root : getAllRoots() ) {
            ret.addNode(root, getData(root));
            copyFragment(root, root, ret);
        }
        
        // copy dominance edges
        for( Edge edge : getAllEdges() ) {
            if( getData(edge).getType() == EdgeType.DOMINANCE ) {
                ret.addEdge((String) edge.getSource(), (String) edge.getTarget(),
                        getData(edge));
            }
        }
        
        return ret;
    }
    
    private void copyFragment(String node, String root, DomGraph ret) {
        if( getData(node).getType() == NodeType.UNLABELLED ) {
            ret.addNode(node, getData(node));
            ret.addEdge(root, node, new EdgeData(EdgeType.TREE, "(cpt dom edge)"));
            //System.err.print("cpt edge from " + root + " to " + node);
        } else {
            for( String child : getChildren(node, EdgeType.TREE) ) {
                copyFragment(child, root, ret);
            }
        }
    }

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
    
    
    public DirectedGraph getLowlevelGraph() {
        return graph;
    }
    
    
    public String toString() {
        StringBuilder ret = new StringBuilder();
        
        for( String node : getAllNodes() ) {
            ret.append("node: " + node + " (" + getData(node) + ")\n");
            for( Edge edge : getOutEdges(node, null)) {
                ret.append("    " + edge + " (" + getData(edge) + ")\n");
            }
        }
        
        return ret.toString();
    }
    
    
    public Map<Set<String>, String> getFragments() {
        Set<String> visited = new HashSet<String>();
        Map<Set<String>,String> ret = new HashMap<Set<String>, String>();
        
        for( Set<String> wcc : wccs() ) {
            computeFragmentTableDfs(wcc.iterator().next(), null, ret, visited);
        }
        
        return ret;
    }

    private void computeFragmentTableDfs(String node, Set<String> currentFragment, Map<Set<String>, String> fragmentTable, Set<String> visited) {
        visited.add(node);
        
        if( currentFragment == null ) {
            currentFragment = new HashSet<String>();
        }
        
        currentFragment.add(node);
        
        if( isRoot(node) ) {
            fragmentTable.put(currentFragment, node);
        }
        
        // visit the other nodes in my fragment
        List<Edge> adjacentEdges = getAdjacentEdges(node);
        for( Edge edge : adjacentEdges ) {
            if( getData(edge).getType() == EdgeType.TREE ) {
                String neighbour = (String) edge.oppositeVertex(node);
                if( !visited.contains(neighbour) ) {
                    computeFragmentTableDfs(neighbour, currentFragment, fragmentTable, visited);
                }
            }
        }
        
        // visit nodes in other fragments
        for( Edge edge : adjacentEdges ) {
            if( getData(edge).getType() == EdgeType.DOMINANCE ) {
                String neighbour = (String) edge.oppositeVertex(node);
                if( !visited.contains(neighbour) ) {
                    computeFragmentTableDfs(neighbour, null, fragmentTable, visited);
                }
            }
        }
    }
    
}
    