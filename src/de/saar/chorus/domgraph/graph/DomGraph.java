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
import java.util.Iterator;
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

import de.saar.chorus.domgraph.chart.OneSplitSource;

/**
 * A dominance graph. Dominance graphs are directed
 * graphs. Nodes are either labelled or unlabelled; edges are either
 * dominance or tree edges. We say that a node is a root if it has no
 * incoming tree edges; a leaf if it has no outgoing tree edges; and
 * a hole if it is unlabelled.<p>
 * 
 * Graph objects provide several basic methods for accessing nodes, edges,
 * and node and edge data. In addition, they provide a number of methods
 * for checking whether the graph belongs to one of the important graph classes,
 * such as (weakly) normal and hypernormally connected graphs.<p> 
 * 
 * Several methods can take a subgraph of this graph as an argument.
 * In this context, a subgraph is always a set of nodes.<p>
 * 
 * While nodes are marked as labelled or unlabelled here, the actual node
 * labels are not stored here, but in objects of the class NodeLabels.
 * 
 * @author Alexander Koller
 *
 */

public class DomGraph implements Cloneable {
    private DirectedGraph graph;
    private Map<String,NodeData> nodeData;
    private Map<Edge,EdgeData> edgeData;
    
    public DomGraph() {
        clear();
    }
    
    
    /**
     * Computes the root of the fragment of the given node.
     * 
     * @param node a node in the graph
     * @return the root of this node's fragment.
     */
    public String getRoot(String node) {
        for (String parent : getParents(node, EdgeType.TREE)) {
            return getRoot(parent);
        }
        return node;
    }
    
    /**
     * Computes the holes below the given node.
     * 
     * @param node a node in the graph
     * @return the holes of this node's fragment.
     */
    public List<String> getHoles(String node) {
        return getHoles(getFragment(node));
    }
    
    /**
     * Computes the holes out of a given collection of nodes.
     * 
     * @param fragment a collection of nodes in this graph
     * @return those nodes out of "fragment" which are holes.
     */
    public List<String> getHoles(Collection<String> fragment) {
        List<String> holes = new ArrayList<String>();
        
        for (String node : fragment) {
            if (getData(node).getType() == NodeType.UNLABELLED) {
                holes.add(node);
            }
        }
        
        return holes;
    }
    
    /**
     * Computes the open holes below a given node. Open holes are
     * holes without outgoing dominance edges.
     * 
     * @param node a node in this graph
     * @return the list of open holes below "node"
     */
    public List<String> getOpenHoles(String node) {
        List<String> holes = new ArrayList<String>();
        
        for (String hole : getHoles(node)) {
            if (outdeg(hole) == 0) {
                holes.add(hole);
            }
        }
        return holes;
    }
    
    /**
     * Computes the fragment of a given node. A fragment is a
     * maximal set of nodes that are connected via tree edges.
     * 
     * @param node a node of this graph
     * @return the fragment of this node
     */
    public Set<String> getFragment(String node) {
        Set<String> nodes = new HashSet<String>();
        getFragmentDfs(node, nodes);
        return nodes;
    }
    
    /**
     * Performs a depth-first search for exploring the fragment of
     * "node". The visited nodes are collected in "nodes".
     * 
     * @param node a node in this graph
     * @param nodes the nodes which were visited so far in this DFS.
     */
    private void getFragmentDfs(String node, Set<String> nodes) {
        if (nodes.contains(node))
            return;
        
        nodes.add(node);
        
        for( Edge edge : getAdjacentEdges(node, EdgeType.TREE)) {
            getFragmentDfs((String) edge.oppositeVertex(node), nodes);
        }
    }
    
    
    /**
     * Checks whether there is a directed path from "upper" to
     * "lower" in the graph.<p>
     * 
     * TODO: this is an ad hoc implementation which will not work
     * for graphs with cycles.
     * 
     * @param upper a node in the graph
     * @param lower a node in the graph
     * @return true iff there is a directed path from upper to lower
     */
    public boolean reachable (String upper, String lower) {
        if (upper.equals(lower))
            return true;
        
        for (String node : getParents(lower, null)) {
            if (reachable(upper, node)) return true;
        }
        return false;
    }
    
    /**
     * Removes all nodes and edges from this graph.
     */
    public void clear() {
        graph = new DefaultDirectedGraph();
        nodeData = new HashMap<String,NodeData>();
        edgeData = new HashMap<Edge,EdgeData>();
    }
    
    /**
     * Adds a node with the given node name and node data to
     * the graph.
     * 
     * @param name the name of the new node
     * @param data the data for the new node
     */
    public void addNode(String name, NodeData data) {
        graph.addVertex(name);
        nodeData.put(name,data);
    }
    
    /**
     * Adds an edge from "src" to "tgt" with the given edge
     * data to the graph.
     * 
     * @param src an existing node in the graph
     * @param tgt an existing node in the graph
     * @param data the data for the new edge
     */
    public void addEdge(String src, String tgt, EdgeData data) {
        Edge e = graph.addEdge(src,tgt);
        edgeData.put(e, data);
    }
    
    /**
     * Removes a given node and all adjacent edges from the graph.
     * 
     * @param node a node in this graph
     */
    public void remove(String node) {
        graph.removeVertex(node);
    }
    
    /**
     * Removes a given edge from the graph.
     * 
     * @param edge an edge in this graph
     */
    public void remove(Edge edge) {
        graph.removeEdge(edge);
    }
    
    /**
     * Computes the set of all nodes in this graph.
     * 
     * @return the set of all nodes
     */
    @SuppressWarnings("unchecked")
    public Set<String> getAllNodes() {
        return graph.vertexSet();
    }
    
    /**
     * Checks whether the graph has a node with the given name.
     * 
     * @param name a node name
     * @return true iff "name" is a node of this graph. 
     */
    public boolean hasNode(String name) {
        return nodeData.containsKey(name);
    }
    
    /**
     * Computes the set of all edges in this graph.
     * 
     * @return the set of all edges
     */
    @SuppressWarnings("unchecked")
    public Set<Edge> getAllEdges() {
        return graph.edgeSet();
    }
    
    /**
     * Computes the set of all incoming edges of a node with a given
     * type. You can select edges of all types by passing null
     * as the "type" argument. 
     * 
     * @param node a node in the graph
     * @param type an edge type, or null for edges of all types
     * @return the list of incoming edges of this type
     */
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
    
    /**
     * Computes the set of parents of a node via edges of
     * a given type. You can select parents via edges of
     * all types by passing null as the "type" argument.
     * 
     * @param node a node in the graph
     * @param type an edge type, or null for edges of all types
     * @return the list of parents via edges of this type
     */
    public List<String> getParents(String node, EdgeType type) {
        List<String> parents = new ArrayList<String>();
        
        for( Edge e : getInEdges(node, type)) {
            parents.add((String) e.getSource() );
        }
        
        return parents;
    }
    
    
    /**
     * Computes the set of all outgoing edges of a node with a given
     * type. You can select edges of all types by passing null
     * as the "type" argument. 
     * 
     * @param node a node in the graph
     * @param type an edge type, or null for edges of all types
     * @return the list of outgoing edges of this type
     */
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
    
    /**
     * Computes the set of all adjacent edges of a node with a given
     * type. You can select edges of all types by passing null
     * as the "type" argument. 
     * 
     * @param node a node in the graph
     * @param type an edge type, or null for edges of all types
     * @return the list of adjacent edges of this type
     */
    public List<Edge> getAdjacentEdges(String node, EdgeType type) {
        List<Edge> ret = getInEdges(node,type);
        ret.addAll(getOutEdges(node,type));
        return ret;
    }
    
    /**
     * Computes the set of all adjacent edges of a node. This
     * is equivalent to getAdjacentEdges(node,null).
     * 
     * @param node a node in the graph
     * @return the list of adjacent edges
     */
    @SuppressWarnings("unchecked")
    public List<Edge> getAdjacentEdges(String node) {
        return (List<Edge>) graph.edgesOf(node);
    }
    
    /**
     * Computes the set of children of a node via edges of
     * a given type. You can select children via edges of
     * all types by passing null as the "type" argument.
     * 
     * @param node a node in this graph
     * @param type an edge type, or null for edges of all types
     * @return the list of children via edges of this type

     */
    public List<String> getChildren(String node, EdgeType type) {
        List<String> children = new ArrayList<String>();
        
        for( Edge e : getOutEdges(node, type)) {
            children.add((String) e.getTarget() );
        }
        
        return children;
    }
    
    
    
    
    /**
     * Gets the data associated with the given node.
     * 
     * @param node
     * @return
     */
    public NodeData getData(String node) {
        return nodeData.get(node);
    }
    
    /**
     * Gets the data associated with the given edge.
     * 
     * @param edge
     * @return
     */
    public EdgeData getData(Edge edge) {
        return edgeData.get(edge);
    }
    
    
    
    
    /**
     * Computes the number of incoming edges of a given node. This is
     * equivalent to indeg(node,null).
     * 
     * @param node
     * @return
     */
    public int indeg(String node) {
        return graph.inDegreeOf(node);
    }
    
    /**
     * Computes the number of outgoing edges of a given node. This is
     * equivalent to outdeg(node,null).
     * 
     * @param node
     * @return
     */
    public int outdeg(String node) {
        return graph.outDegreeOf(node);
    }
    
    /**
     * Computes the number of incoming edges of a given node
     * with a given type. You can specify that you want to count
     * edges of any type by passing null in the "type" argument.
     * 
     * @param node a node in the graph
     * @param type the type of the in-edges you want to count, or null for edges of any type
     * @return the number of in-edges of this type
     */
    public int indeg(String node, EdgeType type) {
        return getInEdges(node,type).size();
    }
    
    /**
     * Computes the number of incoming edges of a given node,
     * with a given edge type, and whose source nodes are in the
     * given subgraph. 
     * 
     * @param node a node in the graph
     * @param type an edge type, or null for edges of any type
     * @param subgraph the subgraph (i.e. set of nodes) in which the parents must be
     * @return the number of such in-edges
     */
    public int indegOfSubgraph(String node, EdgeType type, Set<String> subgraph) {
        List<String> parents = getParents(node, type);
        parents.retainAll(subgraph);
        return parents.size();
    }
    
    /**
     * Computes the number of outgoing edges of a given node
     * with a given type. You can specify that you want to count
     * edges of any type by passing null in the "type" argument.
     * 
     * @param node a node in the graph
     * @param type the type of the out-edges you want to count, or null for edges of any type
     * @return the number of out-edges of this type
     */
    public int outdeg(String node, EdgeType type) {
        return getOutEdges(node,type).size();
    }
    
    /**
     * Checks whether a node is a root. Roots are nodes with no
     * incoming tree edges.
     * 
     * @param node a node
     * @return true iff the node has no incoming tree edges
     */
    public boolean isRoot(String node) {
        return indeg(node,EdgeType.TREE) == 0;
    }
    
    /**
     * Checks whether a node is a leaf. Leaves are nodes with no
     * outgoing tree edges. In particular, all holes are leaves
     * by definition (but not vice versa).
     * 
     * @param node a node in the graph
     * @return true iff the node has no outgoing tree edges
     */
    public boolean isLeaf(String node) {
        return outdeg(node, EdgeType.TREE) == 0;
    }
    
    /**
     * Collects all nodes in a given subgraph which are roots.
     * 
     * @param nodes a collection of nodes (defining a subgraph)
     * @return the set of all nodes among "nodes" which are roots
     */
    public Set<String> getAllRoots(Collection<String> nodes) {
        Set<String> roots = new HashSet<String>();
        
        for( String node : nodes ) {
            if( isRoot(node) ) {
                roots.add(node);
            }
        }
        
        return roots;
        
    }
    
    /**
     * Collects all roots in the graph. This is equivalent to
     * getAllRoots(getAllNodes()).
     * 
     * @return all roots in this graph
     */
    public Set<String> getAllRoots() {
        Set<String> ret = new HashSet<String>();
        
        for( String node : getAllNodes() ) {
            if( isRoot(node) ) {
                ret.add(node);
            }
        }
        
        return ret;
    }
    
    
    
    
    /******************* some elementary graph algorithms *******************/

    /**
     * Determines whether a subgraph has a directed cycle. You can specify
     * the subgraph whose nodes can be used for the cycle (or pass 
     * <code>null</code> for the complete graph) and the edge type which
     * can be used for the cycle (or pass <code>null</code> for edges
     * of any type). The subgraph need not be (strongly) connected; the
     * method will restart the DFS at unvisited nodes of the
     * <code>subgraph</code> while any exist.
     * 
     * @param subgraph the nodes which the DFS may visit, or null for the whole graph
     * @param type the edge types which the DFS may use, or null for any type
     * @return true iff a cycle was found given these constraints
     */
    public boolean hasCycle(Set<String> subgraph, EdgeType type) {
        Set<String> visited = new HashSet<String>();
        Set<String> visitedThisScc = new HashSet<String>();
        Iterator<String> nodeIt;
        
        if( subgraph == null ) {
            subgraph = getAllNodes();
        }
        
        nodeIt = subgraph.iterator();
        
        while( (visited.size() < subgraph.size()) && nodeIt.hasNext() ) { 
            String node = nodeIt.next();
             
            visitedThisScc.clear();
            if( hasCycle(node, subgraph, type, visitedThisScc, visited)) {
                return true;
            }
        }

        return false;        
    }
    
    private boolean hasCycle(String node, Set<String> subgraph, EdgeType type, 
            Set<String> visitedThisScc, Set<String> visited) {
        if( visited.contains(node)) {
            if( visitedThisScc.contains(node)) {
                return true;
            } else {
                return false;
            }
        } else {
            visited.add(node);
            visitedThisScc.add(node);
            
            for( Edge edge : getOutEdges(node, type)) {
                if( hasCycle((String) edge.getTarget(), subgraph, type, visitedThisScc, visited) ) {
                    return true;
                }
            }

            return false;
        }
    }
    
    
    
    /**
     * Computes the weakly connected components of the graph.
     * A weakly connected component is a maximal subgraph which is connected
     * via edges or inverse edges of any type. This is equivalent
     * to wccs(getAllNodes()).
     * 
     * @return the list of wccs; each wcc is a set of nodes.
     */
    public List<Set<String>> wccs() {
        return wccs(getAllNodes());
    }
    
    /**
     * Computes the weakly connected components of a subgraph.
     * A weakly connected component is a maximal subgraph which is connected
     * via edges or inverse edges of any type. 
     * 
     * @return the list of wccs; each wcc is a set of nodes.
     * @param nodes
     * @return
     */
    public List<Set<String>> wccs(Set<String> nodes) {
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
    
    /**
     * Computes a mapping of nodes to wcc indices from a list of
     * wccs. Such a mapping assigns to each node in any of the wccs
     * the index between 0 and wccs.size()-1 which contains this node.
     * 
     * @param wccs a list of WCCs, as computed by the wccs methods.
     * @return the mapping described above.
     */
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
    
    
    /**
     * Removes all dominance edges from the dominance graph.
     */
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
    
    /**
     * Sets the dominance edges of the graph to the given collection,
     * possibly removing any existing dominance edges first.
     * Dominance edges are specified as {@link DomEdge} objects.
     * 
     * @param domedges the new dominance edges.
     */
    public void setDominanceEdges(Collection<DomEdge> domedges) {
        removeAllDominanceEdges();
        
        for( DomEdge e : domedges ) {
            addEdge(e.getSrc(), e.getTgt(), new EdgeData(EdgeType.DOMINANCE));
        }
    }
    
    
    
    
    /***** graph classes ******/
    
    /**
     * Checks whether this graph is weakly normal. A graph is weakly
     * normal under the following conditions:
     * <ul>
     * <li> all holes are leaves;
     * <li> fragments are trees (NB: acyclicity not yet implemented);
     * <li> all dominance edges go into roots.
     * </ul>
     *  
     * @return true iff the graph is weakly normal
     */
    public boolean isWeaklyNormal() {
        for( String node : getAllNodes() ) {
            if( getData(node).getType() == NodeType.UNLABELLED ) {
                // unlabelled nodes must be leaves
                if( !isLeaf(node) ) {
                    //System.err.println(node + " is unlabelled but no leaf!");
                    return false;
                }
                
                /*
                 // no empty fragments: all unlabelled nodes must have incoming tree edges
                  if( indeg(node, EdgeType.TREE) == 0 ) {
                  System.err.println(node + " is unlabelled but has no in-tree-edges!");
                  return false;
                  }
                  */
            }
            
            // no two incoming tree edges
            if( indeg(node, EdgeType.TREE) > 1 ) {
//              System.err.println(node + " has two in-tree-edges!");
                return false;
            }
            
            // no cycles via tree edges
            if( hasCycle(null, EdgeType.TREE)) {
                return false;
            }
        }
        
        for( Edge edge : getAllEdges() ) {
            if( getData(edge).getType() == EdgeType.DOMINANCE ) {
                // dominance edges go into roots
                if( !isRoot((String) edge.getTarget()) ) {
                    //                  System.err.println(edge + " is a dom-edge into a non-root!");
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * Checks whether this graph is normal. A graph is normal under the
     * following conditions:
     * <ul>
     * <li> the graph is weakly normal;
     * <li> dominance edges go from holes to roots.
     * </ul>
     * 
     * @return true iff the graph is normal.
     */
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
    
    /**
     * Checks whether this graph is compact. A graph is compact iff
     * it is weakly normal and only holes have incoming tree edges,
     * i.e. every node is either a root or a hole (or both).
     * 
     * @return true iff the graph is compact.
     */
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
    
    /**
     * Checks whether this graph can be compactified. A graph can
     * be compactified iff it is weakly normal, and all dominance
     * edges go either out of holes or out of roots. 
     * 
     * @return true iff the graph can be compactified
     */
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
    
    /**
     * Checks whether the graph is leaf-labelled. A graph is leaf-labelled
     * iff it is weakly normal and all nodes either have a label or an
     * outgoing dominance edge. 
     * 
     * @return true iff the graph is leaf-labelled
     */
    public boolean isLeafLabelled() {
        if( !isWeaklyNormal() ) {
            return false;
        }
        
        for( String node : getAllNodes() ) {
            // unlabelled nodes must have outgoing dom edges
            if( (getData(node).getType() == NodeType.UNLABELLED)
                    && (outdeg(node, EdgeType.DOMINANCE) == 0) ) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Checks whether the graph is hypernormally connected. A graph
     * is hypernormally connected iff it is normal and each pair of
     * nodes is connected by a hypernormal path.<p>
     * 
     * This method checks whether the graph is solvable, and then
     * calls <code>isHypernormallyConnectedFast</code> (if it is)
     * or <code>isHypernormallyConnectedSlow</code> (if it isn't).
     * Its overall runtime is O(n(n+m)), i.e. it is marginally
     * slower in practice than <code>isHypernormallyConnectedFast</code>.
     * 
     * @return true iff the graph is hnc
     */
    public boolean isHypernormallyConnected() {
        if( OneSplitSource.isGraphSolvable(this) ) {
            return isHypernormallyConnectedFast();
        } else {
            return isHypernormallyConnectedSlow();
        }
    }
    
    /**
     * Checks whether the graph is hypernormally connected. A graph
     * is hypernormally connected iff it is normal and each pair of
     * nodes is connected by a hypernormal path.<p>
     *
     * This method performs a depth-first search through the dominance
     * graph for each pair of nodes, and thus runs in time O((m+n) n^2).
     * This is ridiculously slow, although still efficient enough for many
     * practical purposes. However, unlike <code>isHypernormallyConnectedFast</code>,
     * this method is also correct for unsolvable dominance graphs.
     * 
     * @return true iff the graph is hnc
     */
    public boolean isHypernormallyConnectedSlow() {
        for( String u : getAllNodes() ) {
            for( String v : getAllNodes() ) {
                if( !u.equals(v) ) {
                    if( !isHypernormallyReachable(u,v) ) {
                        return false;
                    }
                }
            }
        }
        
        return true;
    }
    
    /**
     * Checks whether the graph is hypernormally connected. A graph
     * is hypernormally connected iff it is normal and each pair of
     * nodes is connected by a hypernormal path.<p>
     * 
     * This method performs a single depth-first search through the
     * dominance graph, and thus runs in time O(m+n). However, <strong>it is
     * only correct if the graph is solvable</strong>; if the graph is unsolvable,
     * the method may claim that the graph is not hypernormally
     * connect although it is.
     * 
     * @return true iff the graph is hnc
     */
    public boolean isHypernormallyConnectedFast() {
        Set<String> visited = new HashSet<String>();
        
        if( !isNormal() ) {
            return false;
        }
        
        hncDfs(getAllNodes().iterator().next(), visited);
        return visited.equals(getAllNodes());
    }
    
    
    /**
     * An auxiliary function for <code>isHypernormallyConnected</code>,
     * which performs a DFS over the entire graph which only uses the
     * first dom-edge out of each node.
     * 
     * @param node
     * @param visited
     */
    private void hncDfs(String node, Set<String> visited) {
        boolean haveUsedOutgoingDomEdge = false;
        
        visited.add(node);
        
        for( Edge edge : getAdjacentEdges(node) ) {
            String neighbour = (String) edge.oppositeVertex(node);
            
            if( (getData(edge).getType() == EdgeType.DOMINANCE)
                    && edge.getSource().equals(node) ) {
                if( haveUsedOutgoingDomEdge ) {
                    continue;
                } else {
                    haveUsedOutgoingDomEdge = true;
                }
            }
            
            if( !visited.contains(neighbour)) {
                hncDfs(neighbour, visited);
            }
        }
    }
    
    
    
    /**
     * Checks whether the graph is a simple solved form, i.e.
     * if it is a tree and every node has at most one outgoing
     * dominance edge.
     * 
     * @return true iff the graph is a simple solved form.
     */
    public boolean isSimpleSolvedForm() {
        for( String node : getAllNodes() ) {
            // no cycles
            if( hasCycle(null, null)) {
                return false;
            }
            
            // no node with indeg > 1
            if( indeg(node) > 1 ) {
                return false;
            }
            
            // no node with more than one outgoing dominance edge
            if( outdeg(node, EdgeType.DOMINANCE) > 1 ) {
                return false;
            }
        }
        
        return true;
    }
    

    /**
     * Check whether the weakly normal graph is "well-formed" in the sense of 
     * Bodirsky et al. 04. This means that every root of a dominance edge
     * dominates a hole of its fragment.<p>
     * 
     * This method assumes that the graph is weakly normal and compact.
     *  
     * @return true iff the graph is well-formed.
     */
    public boolean isWellFormed() {
        assert isWeaklyNormal();
        assert isCompact();
        
        for( Edge edge : getAllEdges()) {
            String src = (String) edge.getSource();
            
            // this check assumes that the graph is compact 
            if( (getData(src).getType() == NodeType.LABELLED)
                    && isLeaf(src) ) {
                return false;
            }
        }
        
        return true;
    }
    
   
    /**** hypernormal paths ****/
    
    
    /**
     * Checks whether there is a hypernormal path between two nodes.
     * This method performs a modified depth-first search through the
     * dominance graph, and thus takes time O(m+n).
     * 
     * @param source one node in this graph
     * @param target another node in this graph
     * @return true iff there is a hypernormal path connecting the two
     */
    public boolean isHypernormallyReachable(String source, String target) {
        return isHnReachable(source, target, new HashSet<String>(), false);
    }
    
    /**
     * Checks whether there is a hypernormal path between source and
     * target which doesn't visit any of the nodes in <code>avoidThese</code>.
     * This method performs a modified depth-first search through the
     * dominance graph, and thus takes time O(m+n).<p>
     * 
     * The method will modify the contents of <code>avoidThese</code>; if
     * you don't want this, you should pass <code>new HashSet<String>(...)</code>
     * as third argument. 
     * 
     * @param source one node in this graph
     * @param target another node in this graph
     * @param avoidThese nodes that must not be on a connecting hn path
     * @return true iff there is a hn path connecting source and target which
     * doesn't visit avoidThese.
     */
    public boolean isHypernormallyReachable(String source, String target, Set<String> avoidThese) {
        return isHnReachable(source, target, avoidThese, false);
    }

    // Is there a hn path from src to target that doesn't use nodes in visited
    // -- assuming that we came via an upwards dom edge iff the fourth argument
    // is true?
    private boolean isHnReachable(String node, String goal, Set<String> visited, boolean previousEdgeWasUpDom) {
        if( visited.contains(node) ) {
            return false;
        } else if( node.equals(goal) ) {
            return true;
        } else {
            visited.add(node);
            
            for( Edge edge : getAdjacentEdges(node) ) {
                String neighbour = (String) edge.oppositeVertex(node);
                boolean isDomEdge = (getData(edge).getType() == EdgeType.DOMINANCE);
                boolean isOutEdge = node.equals(edge.getSource());
                
                // skip outgoing dom edges if we came through an up dom edge
                if( isDomEdge && isOutEdge && previousEdgeWasUpDom ) {
                    continue;
                }
                
                if( isHnReachable(neighbour, goal, visited, isDomEdge && !isOutEdge) ) {
                    //System.err.println("hnr: " + node + " -> " + neighbour + " ->* " + goal);
                    return true;
                }
            }
            
            return false;
        }
    }
    
    
    
    /**** compactification ****/
    
    /**
     * Computes a compact version of this graph. If the graph is
     * already compact, the graph itself is returned. Otherwise,
     * the compactified graph will consist of the roots and holes
     * of the original graph; roots and holes of the same fragment
     * are connected by new tree edges, and holes and roots of different
     * fragments are connected by dominance edges.<p>
     * 
     * The compact graph and the original graph have corresponding
     * solved forms.<p>
     * 
     * The result is only guaranteed to be compact if the graph
     * is compactifiable according to the method <code>isCompactifiable</code>. 
     * 
     * @return a compact version of this graph.
     */
    public DomGraph compactify() {
        if( isCompact() ) {
            return this;
        } else {
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
    }
    
    /**
     * An auxiliary method in the compactification, which copies a fragment
     * to the compact graph.
     * 
     * @param node
     * @param root
     * @param ret
     */
    private void copyFragment(String node, String root, DomGraph ret) {
        if( getData(node).getType() == NodeType.UNLABELLED ) {
            if( !node.equals(root) ) { // i.e. not an empty fragment with root = hole
                ret.addNode(node, getData(node));
                ret.addEdge(root, node, new EdgeData(EdgeType.TREE));
            }
            //System.err.print("cpt edge from " + root + " to " + node);
        } else {
            for( String child : getChildren(node, EdgeType.TREE) ) {
                copyFragment(child, root, ret);
            }
        }
    }
    
    
    
    
    
    
    /* 
     * Computes a string representation of this graph.
     */
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
    
    
    
    @SuppressWarnings("unchecked")
    public Object clone() {
        try {
            DomGraph c = (DomGraph) super.clone();
        
            c.graph = (DirectedGraph) ((DefaultDirectedGraph) graph).clone();
            c.edgeData = (Map<Edge,EdgeData>) ((HashMap<Edge,EdgeData>) edgeData).clone();
            c.nodeData = (Map<String,NodeData>) ((HashMap<String,NodeData>) nodeData).clone();
        
            return c;
        } catch(CloneNotSupportedException e) {
            return null;
        }
    }

  
}





/*
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
*/



/*
public DirectedGraph getLowlevelGraph() {
    return graph;
}
*/
