package de.saar.chorus.jgraph.improvedjgraph.layout.treelayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jgraph.graph.DefaultGraphCell;

import de.saar.chorus.gecode.NodeCursorInterface;
import de.saar.chorus.jgraph.improvedjgraph.ImprovedJGraph;

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
abstract  class GraphNodeCursor implements NodeCursorInterface {

	
	private DefaultGraphCell startNode;	// the graph root
    private DefaultGraphCell node;		// the recently processed node
    private ImprovedJGraph graph;			// the graph to layout
    private Set<DefaultGraphCell> nodesToLayout;
	
    /**
     * A new instance of <code>GraphNodeCursor<code>
     * 
     * @param theNode the graph root
     * @param theGraph the graph to layout
     */
    public GraphNodeCursor(DefaultGraphCell theNode, 
								ImprovedJGraph theGraph) {
        this.startNode = theNode;
        this.node = theNode;
		this.graph = theGraph;
		nodesToLayout = theGraph.getNodes();
    }
    
    /**
     * A new instance of <code>GraphNodeCursor<code>
     * 
     * @param theNode the graph root
     * @param theGraph the graph to layout
     * @param allowedNodes nodes the layout shall arrange, all are taken if not specified
     */
    public GraphNodeCursor(DefaultGraphCell theNode, 
								ImprovedJGraph theGraph,
								Set<DefaultGraphCell> allowedNodes) {
        this.startNode = theNode;
        this.node = theNode;
		this.graph = theGraph;
		nodesToLayout = allowedNodes;
    }
	
    /**
     * Returns the recently processed node.
     */
    public DefaultGraphCell getCurrentNode() {
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
    	for(Object par : graph.getParents(node)) {
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
    	List<DefaultGraphCell> parents = 
    		new ArrayList<DefaultGraphCell>(graph.getParents(node));
    	parents.retainAll(nodesToLayout);
    	
        node = (DefaultGraphCell) parents.get(0);
    }
    
    /**
     * Checking whether the current node has at least
     * one child.
     * 
     * @return true if there are one ore more children
     */
    public boolean mayMoveDownwards() {
    	boolean childfound = false;
    	for(Object par : graph.getChildren(node)) {
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
    	List<DefaultGraphCell> children = 
    		new ArrayList<DefaultGraphCell>(graph.getChildren(node));
    	children.retainAll(nodesToLayout);
    	
        node = (DefaultGraphCell) children.get(0);
    }
    
    /**
     * Checking whether the current node has a sibling on
     * the right.
     * 
     * @return true if there is a right sibling
     */
    public boolean mayMoveSidewards() {
		DefaultGraphCell sibling = graph.getRelativeRightSibling(node, nodesToLayout);
		
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
