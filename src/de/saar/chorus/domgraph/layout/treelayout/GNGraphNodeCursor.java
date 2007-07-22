package de.saar.chorus.domgraph.layout.treelayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.gecode.NodeCursorInterface;

/**
 * Class to layout solved forms implementing the <code>NodeCursorInterface</code>.
 * This class provides the methods needed to compute the 
 * node coordinates of solved forms, resp.
 * of a whole <code>JDomGraph</code> that is a forest.
 * 
 * The abstract method <code>processCurrentNode</code> has to be
 * implemented by subclasses.
 * 
 * @author Marco Kuhlmann
 * @author Michaela Regneri
 *
 */
public abstract  class GNGraphNodeCursor implements NodeCursorInterface {

	
	private String startNode;	// the graph root
    private String node;		// the recently processed node
    private DomGraph graph;			// the graph to layout
    private Set<String> nodesToLayout;
	
    /**
     * A new instance of <code>GNGraphNodeCursor<code>
     * 
     * @param theNode the graph root
     * @param theGraph the graph to layout
     */
    public GNGraphNodeCursor(String theNode, 
								DomGraph theGraph) {
        this.startNode = theNode;
        this.node = theNode;
		this.graph = theGraph;
		nodesToLayout = theGraph.getAllNodes();
    }
    
    /**
     * A new instance of <code>GNGraphNodeCursor<code>
     * 
     * @param theNode the graph root
     * @param theGraph the graph to layout
     * @param allowedNodes nodes the layout shall arrange, all are taken if not specified
     */
    public GNGraphNodeCursor(String theNode, 
								DomGraph theGraph,
								Set<String> allowedNodes) {
        this.startNode = theNode;
        this.node = theNode;
		this.graph = theGraph;
		nodesToLayout = allowedNodes;
    }
	
    /**
     * Returns the recently processed node.
     */
    public String getCurrentNode() {
        return node;
    }
    
    /**
     * Checking whether the current node has a direct 
     * parent node.
     * 
     * @return true if there is a parent node
     */
    public boolean mayMoveUpwards() {
    	
    	/*
    	 * The node must not be the start node, neither the
    	 * graph root.
    	 */
    	boolean parentfound = false;
    	for(Object par : graph.getParents(node,null)) {
    		if(nodesToLayout.contains(par)) {
    			parentfound = true;
    			break;
    		}
    	}
    	
        return ((node != startNode) &&  parentfound);
    }
    
    /**
     * Moving to the current node's parent node 
     * (assuming that there is one).
     */
    public void moveUpwards() {
    	List<String> parents = 
    		new ArrayList<String>(graph.getParents(node,null));
    	parents.retainAll(nodesToLayout);
    	
        node =  parents.get(0);
    }
    
    /**
     * Checking whether the current node has at least
     * one child.
     * 
     * @return true if there are one ore more children
     */
    public boolean mayMoveDownwards() {
    	boolean childfound = false;
    	for(Object par : graph.getChildren(node, null)) {
    		if(nodesToLayout.contains(par)) {
    			childfound = true;
    			break;
    		}
    	}
    	
        return ( childfound );
    }
    
    /**
     * Moving to the current node's most left child
     * (assuming that there is one).
     */
    public void moveDownwards() {
    	List<String> children = 
    		new ArrayList<String>(graph.getChildren(node, null));
    	children.retainAll(nodesToLayout);
    	
        node = children.get(0);
    }
    
    /**
     * Checking whether the current node has a sibling on
     * the right.
     * 
     * @return true if there is a right sibling
     */
    public boolean mayMoveSidewards() {
		String sibling = graph.getRelativeRightSibling(node, nodesToLayout);
		
        return sibling != null;
    }
    
    
    
    /**
     * Moving to the current node's right sibling
     * (assuming that there is one).
     * 
     * @see <code>JDomGraph.getRightSibling(DefaultGraphCell node)</code>
     */
    public void moveSidewards() {
    	
        node = graph.getRelativeRightSibling(node, nodesToLayout);
    }
    
    abstract public void processCurrentNode(); 

}
