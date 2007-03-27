package de.saar.chorus.domgraph.chart;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org._3pq.jgrapht.Edge;

import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.EdgeType;

/**
 * A utility class for computing the split of a free root. 
 * 
 * @author Alexander Koller
 *
 */
public class SplitComputer {
	/*
	 * - WCCs are identified by one dominance edge out of the root fragment that
	 *   points into them (namely, the first one that is visited by the DFS). It is
	 *   this dominance edge to which the node is mapped by wccOfNode. 
	 */
	
	
    private DomGraph graph;

    //private String theRoot;
    private Set<String> rootFragment;
    
    // node in rootfrag -> edge in this node -> wcc
    //private Map<String,Map<Edge,Set<String>>> splitmap;
    // (wcc identity, i.e. dom edge into this wcc) -> (nodes in that wcc)
    private Map<Edge,Set<String>> wccs;
    
    // set of nodes already visited by DFS
    // this set is implemented as a field of the class in order
    // to save on object allocations
    private Set<String> visited;
    
    // maps holes to roots with which they must be plugged directly
    // (occurs for non-weakly-normal dominance graphs)
    private Map<String,String> substitution;
    
    
            
    public SplitComputer(DomGraph graph) {
        this.graph = graph;
        rootFragment = new HashSet<String>();
        wccs = new HashMap<Edge, Set<String>>();
        substitution = new HashMap<String, String>();
        
        visited = new HashSet<String>();
    }
    
    

    /**
     * Compute the split induced by the free root of a subgraph.
     * The method assumes that the given root is a node without
     * incoming edges. It does _not_ assume that the root is actually
     * free, but if it isn't, the method will return null.<p>
     * 
     * The runtime of this method is O(m+n) for a subgraph with m edges
     * and n nodes (it performs a single DFS through the graph).
     * 
     * @param root a node without incoming edges
     * @param subgraph a subgraph 
     * @return the split induced by this root, or null if the root
     * is not the root of a free fragment
     */
    public Split computeSplit(String root, Set<String> subgraph)
    {
        // initialise root fragment
        rootFragment = computeRootFragment(root, graph, subgraph);
        
        if( rootFragment == null ) {
        	return null;
        }
        
        // perform DFS
        wccs.clear();
        visited.clear();
        
        
        Set<String> path = new HashSet<String>();
        path.add(root);
        
        if( !dfs(root, null, path, subgraph, visited) ) {
            return null;
        }

        // build Split object
        Split ret = new Split(root);
        
        ret.setSubstitution(substitution);
        for( Edge wccId : wccs.keySet() ) {
        	ret.addWcc((String) wccId.getSource(), wccs.get(wccId));
        }
        
        return ret;
    }
    
    
    
    private Set<String> computeRootFragment(String root, DomGraph graph, Set<String> subgraph) {
    	Set<String> nodes = new HashSet<String>();
    	Set<String> ancestors = new HashSet<String>();
    	
    	if( computeRootFragmentDfs(root, nodes, ancestors, subgraph) ) {
    		return nodes;	
    	} else {
    		return null;
    	}
	}
    
    private boolean computeRootFragmentDfs(String node, Set<String> nodes, Set<String> ancestors, Set<String> subgraph)  {
    	nodes.add(node);
    	
    	List<Edge> outgoingTreeEdges = graph.getOutEdges(node, EdgeType.TREE);
    	
    	// determine the set of dominance parents within this subgraph
    	Set<String> dominanceParents = new HashSet<String>(graph.getParents(node, EdgeType.DOMINANCE));
    	dominanceParents.retainAll(subgraph);
    	dominanceParents.removeAll(ancestors); // ancestors are satisfied transitively and can be ignored
    	
    	if( dominanceParents.size() > 1 ) {
    		//System.err.println(node + " has more than one incoming dom edges");
    		return false;
    	}
    	
    	if( !dominanceParents.isEmpty() ) {
    		// the only incoming dom edges that are allowed are from roots and into holes
    		if( !outgoingTreeEdges.isEmpty() ) { 
    			// System.err.println(node + " is not a hole");
    			return false;
    		}
    		
    		for( String parent : dominanceParents ) {
    			if( !graph.isRoot(parent) ) {
    				// System.err.println(node + " has non-root parent");
    				return false;
    			}
    		}
    		
    	}
    	
    	// dfs over tree children
    	for( Edge edge : outgoingTreeEdges ) {
    		String neighbour = (String) edge.getTarget();
    		
    		if( subgraph.contains(neighbour)) {
    			if( nodes.contains(neighbour) ) {
    				// already visited the neighbour => this neighbour is reachable
    				// by two different paths, i.e. the superfragment is not a tree
    				return false;
    			} else {
    				ancestors.add(node);
    				if( !computeRootFragmentDfs(neighbour, nodes, ancestors, subgraph) ) {
    					return false;
    				}
    				ancestors.remove(node);
    			}
    		}
    	}
    	
    	// dfs over incoming dom edges
    	for( String parent : dominanceParents ) {
    		substitution.put(node, parent);
    		
    		if( nodes.contains(parent) ) {
    			// incoming cross edge from a previously visited non-ancestor
    			// => the source node is disjoint to our current position
    			return false;
    		} else {
    			ancestors.add(node);
    			if( !computeRootFragmentDfs(parent, nodes, ancestors, subgraph) ) {
    				return false;
    			}
    			ancestors.remove(node);
    		}
    	}
    	
    	return true;
	}



	/**
     * Performs a depth-first search through the graph which
     * determines the wccs of the split for the given node
     * and enters them into domEdgeForNodeWcc and splitmap.
     * The method returns true if this was successful, and
     * false if the given node was not the root of a free fragment.
     * <p>
     * INVARIANT: this method never visits a node in the root
     * fragment coming from outside the root fragment
     * <p>
     *  INVARIANT: this method is only called on unvisited nodes
     * 
     * @param node the root of a fragment
     * @param subgraph the subgraph for which we want to compute
     * the split
     * @param visited the set of nodes we already visited during 
     * this DFS
     * @return true iff the root was indeed free
     */
    private boolean dfs(String node, Edge wccId, Set<String> pathInRootFragment, Set<String> subgraph, Set<String> visited) {
        // INVARIANT: this method is only called on unvisited nodes
        assert !visited.contains(node) : "DFS visited node twice";
        
        // INVARIANT: this method is only called on nodes in the subgraph
        assert subgraph.contains(node) : "DFS left subgraph";
        
        visited.add(node);
        
        // If node is not in the root fragment, then determine
        // its wcc set and assign it to it.
        if( !rootFragment.contains(node) ) {
            assert wccId != null;
            assignNodeToWcc(node, wccId);
        }
        
        // otherwise, iterate over all adjacent edges, visiting
        // tree edges first
        List<Edge> edgeList = graph.getAdjacentEdges(node, EdgeType.TREE);
        edgeList.addAll(graph.getInEdges(node, EdgeType.DOMINANCE));
        edgeList.addAll(graph.getOutEdges(node, EdgeType.DOMINANCE));
        
        for( Edge edge : edgeList ) {
            String neighbour = (String) edge.oppositeVertex(node);
            
            if( subgraph.contains(neighbour) ) {
            	if( rootFragment.contains(neighbour) && !rootFragment.contains(node) ) {
            		// The (undirected) DFS steps from a node outside the root fragment
            		// into the root fragment.  Because the root fragment has no incoming
            		// dominance edges, this must be a dominance edge out of the root
            		// fragment.  We don't traverse this edge in the DFS, but we must check
            		// that the edge is consistent with the wcc assignment we have so far.

            		assert node.equals(edge.getTarget());
            		assert neighbour.equals(edge.getSource());
            		assert graph.getData(edge).getType() == EdgeType.DOMINANCE;

            		// Because the DFS visits outgoing tree edges first, we can only return
            		// from a DFS into a WCC to either (a) a disjoint node (in which case
            		// the fragment was not free) or (b) a node that dominates the original
            		// dominator (in which case it's ok).
            		if( ! pathInRootFragment.contains(neighbour)) {
            			return false;
            		}
            	} else if( !visited.contains(neighbour) ) {
            		// any other edge -- let's explore it

            		if( rootFragment.contains(node) ) {
            			// we're inside the root fragment, walking down

            			if( rootFragment.contains(neighbour) ) {
            				// - downward tree edge (upward nodes are all in visited)
            				// - upward dominance cross edge (downward edges all point out of root fragment)
            				pathInRootFragment.add(neighbour);
            				if( !dfs(neighbour, null, pathInRootFragment, subgraph, visited)) {
            					return false;
            				}
            				pathInRootFragment.remove(neighbour);
            			} else {
            				// Downward dominance edge. The WCC at the other end of the edge
            				// hasn't been visited yet, otherwise neighbour would be an element
            				// of the "visited" set.
                			assert node.equals(edge.getSource());

                			if( !dfs(neighbour, edge, pathInRootFragment, subgraph, visited )) {
            					return false;
            				}
            			}
            		} else {
            			// outside the root fragment: just keep collecting nodes for same wcc
            			if( !dfs(neighbour, wccId, pathInRootFragment, subgraph, visited)) {
            				return false;
            			}
            		}
            	}
            }
        }
        
        return true;
    }
    



    private void assignNodeToWcc(String node, Edge wccId) {
    	assert wccId != null;
    	
    	
    	Set<String> thisWcc = wccs.get(wccId);
    	
    	if( thisWcc == null ) {
    		thisWcc = new HashSet<String>();
    		wccs.put(wccId, thisWcc);
    	}
    	
    	thisWcc.add(node);
    }
}




/*
 * unit tests:
 * 
 * - check whether computeSplit computes correct splits
 * - computeSplit returns null if root is not free
 */

