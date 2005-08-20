package de.saar.chorus.leonardo;

import org.jgraph.graph.DefaultGraphCell;

import de.saar.chorus.treelayout.NodeCursorInterface;


/**
 * Class to layout fragments implementing the <code>NodeCursorInterface</code>.
 * This class provides the methods needed compute the coordinates
 * of nodes within their fragments.
 * 
 * 
 * The abstract method <code>processCurrentNode</code> has to be
 * implemented for layouting. 
 * 
 * @author Marco Kuhlmann
 * @author Michaela Regneri
 *
 */

abstract public class FragmentNodeCursor implements NodeCursorInterface {
    
    private DefaultGraphCell startNode;	// the node to start with
    private DefaultGraphCell node;		// the current node
	private Fragment nodeFragment;		// the fragment to layout   
	private JDomGraph domGraph;			// the parent graph
	
	
	
	/**
	 * Creates a new <code>FragmentNodeCursor</code>.
	 * 
	 * @param theNode the fragment root
	 * @param frag the fragment to layout
	 * @param theGraph the parent graph
	 */
    public FragmentNodeCursor(DefaultGraphCell theNode, Fragment frag, 
								JDomGraph theGraph) {
        this.startNode = theNode;
        this.node = theNode;
		this.nodeFragment = frag;
		this.domGraph = theGraph;
    }
    
    /**
     * @return the recently processed node
     */
    public DefaultGraphCell getCurrentNode() {
        return node;
    }
    
    /**
     * Checking whether the current node has
     * a parent node.
     * 
     * @return true if the current node is neither a fragment root nor the start node
     */
    public boolean mayMoveUpwards() {
        return ((node != startNode) && 
				(! domGraph.isFragRoot(node)));
    }
    
    /**
     * Moves to the current node's parent node.
     * (assuming that there is one).
     */
    public void moveUpwards() {
        node = domGraph.getParents(node).get(0);
    }
    
    /**
     * Checking whether the current node has at least
     * one child node.
     * 
     * @return true if the current node is no leaf within its fragment
     */
    public boolean mayMoveDownwards() {
        return (! domGraph.isFragLeaf(node));
    }
    
    /**
     * Move to the current node's most left child node
     * (assuming that there is at least one child)
     */
    public void moveDownwards() {
        node = domGraph.getChildren(node).get(0);
    }
    
    /**
     * Checking whether the current node has a right
     * sibling.
     * 
     * @return true if the node's parent has another more right child
     */
    public boolean mayMoveSidewards() {
		DefaultGraphCell sibling = domGraph.getRightSibling(node);
		
		// We have to make sure that the right sibling is contained
		// in the fragment to layout. A root has no sibling within
		// the same fragment.
        return ( (! domGraph.isFragRoot(node)) && (sibling != null) &&
				nodeFragment.getNodes().contains(sibling));
    }
    
    /**
     * Move to the current node's right sibling.
     * (assuming that there is one within the same
     * fragment).
     * 
     * @see <code>JDomGraph.moveSidewards(DefaultGraphDell node)</code>
     */
    public void moveSidewards() {
        node = domGraph.getRightSibling(node);
    }
    
    /**
     * Abstract method that operates on the current node.
     * To be implemented by subclasses.
     */
    abstract public void processCurrentNode(); 
    
}
