/*
 * Created on 31.07.2004
 *
 */
package de.saar.coli.chorus.leonardo;

import java.util.HashSet;
import java.util.Set;

import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.util.*;

/**
 * A fragment in a dominance graph. This fragment contains nodes (labelled
 * and unlabelled) and solid edges, such that the contained nodes are all
 * connected by the contained edges.
 * 
 * Fragments of a dominance graph are actually computed by the computeFragments
 * method in JDomGraph.
 * 
 * Objects of this class can serve as popup targets, i.e.
 * they provide a menu item for a popup menu.
 * 
 * @author Alexander
 *
 */
class Fragment extends DomGraphPopupTarget {
    private Set<DefaultGraphCell> nodes;
    private Set<DefaultEdge> edges;
    private DefaultGraphCell groupObject;
    private StringBuilder fragmentName;
   
    
    public Fragment(JDomGraph parent) {
        super(parent);
        
        nodes = new HashSet<DefaultGraphCell>();
        edges = new HashSet<DefaultEdge>();
        fragmentName = new StringBuilder("fragment: ");
        groupObject = new DefaultGraphCell(fragmentName);
    }
    
    /**
     * Add a node to this fragment.
     * 
     * @param node
     */
    public void add(DefaultGraphCell node) {
        nodes.add(node);
        groupObject.add(node);
        fragmentName.append(((NodeData)node.getUserObject()).getName() + " ");
    }
    
    /**
     * Add a solid edge to this fragment.
     * 
     * @param edge
     */
    public void add(DefaultEdge edge) {
        edges.add(edge);
        groupObject.add(edge);
    }
    
    /**
     * Add all nodes and edges of some other fragment to this one.
     * This is useful when two fragments are merged when computing
     * the maximal fragments of a dominance graph.
     * 
     * @param frag
     */
    public void addAll(Fragment frag) {
        for( DefaultGraphCell node : frag.getNodes() ) {
            add(node);
        }
        
        for( DefaultEdge edge : frag.getEdges() ) {
            add(edge);
        }
    }
    
    /**
     * Get all nodes in this fragment.
     * 
     * @return the set of all nodes.
     */
    public Set<DefaultGraphCell> getNodes() {
        return nodes;
    }
    
    /**
     * Get all cells in this fragment. This includes all nodes, all edges,
     * and the primary ports of the nodes which were used to connect the
     * edges.
     * 
     * @return all cells in this fragment.
     */
    public Set<Object> getAllCells() {
        Set<Object> ret = new HashSet<Object>();
        ret.addAll(getNodes());
        ret.addAll(getEdges());
        
        // ports
        for( DefaultGraphCell node : getNodes() ) {
            ret.add(node.getChildAt(0));
        }
        
        return ret;
    }
    
    /**
     * Get all edges in this fragment.
     * 
     * @return the set of all edges.
     */
    public Set<DefaultEdge> getEdges() {
        return edges;
    }

    
    
    /**
     * Get the number of nodes in this fragment.
     * 
     * @return the number of nodes.
     */
    public int size() {
        return nodes.size();
    }

    /**
     * Get the graph cell corresponding to the entire fragment.
     * This will be a group cell to which all the nodes, primary ports,
     * and edges (as given by getAllCells) belong.
     * 
     * @return the cell.
     */
    public DefaultGraphCell getGroupObject() {
        return groupObject;
    }
    
    /**
     * Returns the children of a given node contained in
     * this Fragment. Returns null if the given source 
     * node is not contained in the fragment.
     * 
     * @param node, the source node
     * @return a Set of the node's children.
     */
    public Set<DefaultGraphCell> getChildren(DefaultGraphCell node){
    	
    	Set<DefaultGraphCell> fragChildren = new HashSet<DefaultGraphCell>();
    	
    	//if there is no node, there are no children.
    	if(! this.getNodes().contains(node))
    		return null;
    	else {
    		//iterating over the edges.
    		for(DefaultEdge edg : this.getEdges()){
    			
    			//if we find one going out from our source...
    			if(JGraphUtilities.getSourceVertex(this.getParent(),edg).equals(node)){
    				DefaultGraphCell potChild = 
    					(DefaultGraphCell) JGraphUtilities.getTargetVertex(this.getParent(), edg);
    				
    				//we add it, in case it is part of the fragment.
    				if(this.getNodes().contains(potChild))
    					fragChildren.add(potChild);
    			}
    		}
    		
    	}
    	return fragChildren;
    }
    
    /**
     * Indicates if a node is a Leaf - considering
     * just the nodes this fragment contains.
     * Throws NullPointerException if the not ist not part
     * of the fragment, I'll fix that (promised).
     * 
     * @param node, the node to check
     * @return true if the node is a leaf;
     */
    public boolean isLeaf(DefaultGraphCell node)
    {
    	return getChildren(node).isEmpty();
    }
    
    /**
     * Returns the next leave going out from a given node.
     * Returns null if the node is not part of the fragment
     * or there are no following leafs.
     * @param prec, the node to go out from
     * @return the next leaf or null if it cannot be resolved
     */
    public DefaultGraphCell getNextLeaf(DefaultGraphCell prec)
    {
    	if(! this.getNodes().contains(prec))
    		return null;
    	else {
    		boolean seen = false;
        	for(DefaultGraphCell nd : this.getNodes()) {
        		if(nd.equals(prec))
        			seen = true;
        		if(seen && this.isLeaf(nd))
        			return nd;
        	}
    	}
    	return null;
    }
    
    
    
    public String toString() {
        return "<" + fragmentName.toString() + ">";
    }

    public String getMenuLabel() {
	    return fragmentName.toString();
	}
	
	
}
