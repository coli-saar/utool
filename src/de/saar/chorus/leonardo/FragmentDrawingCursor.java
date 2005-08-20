package de.saar.chorus.leonardo;


import static de.saar.chorus.leonardo.DomGraphLayoutParameters.nodeYDistance;

import org.jgraph.graph.DefaultGraphCell;


/**
 * A class to determine the positions of a node within its fragment.
 * This converts the positions (relative to the direct parent) computet by a
 * <code>FragmentLayoutCursor</code> into positions relative to the
 * fragment root.
 * The computet coordinates are stored in maps of the given 
 * <code>DomGraphLayout</code> and processed there.
 * 
 * A subclass of <code>FragmentNodeCursor</code>.
 * 
 * @author Marco Kuhlmann
 * @author Michaela Regneri
 *
 */
public class FragmentDrawingCursor extends FragmentNodeCursor {
    
    int x;	// recent x-coordinate
    int y;  // recent y-coordinate.
    
    // the layout to forward the results to
	DomGraphLayout layout;
	
	/**
	 * Setting up a new <code>FragmentDrawingCursor</code>
	 * 
	 * @param theNode the node to start with
	 * @param frag the fragment to layout
	 * @param theLayout the layout of the parent graph
	 * @param aGraph the parent graph
	 */
    public FragmentDrawingCursor(DefaultGraphCell theNode, Fragment frag, 
						DomGraphLayout theLayout, JDomGraph aGraph) {
        super(theNode, frag, aGraph);
        this.x = 0;
        this.y = 0;
		layout = theLayout;
    }
    
    /**
     * @return the recently processed node
     */
    private DefaultGraphCell getVisualNode() {
        return super.getCurrentNode();
    }
    
    /**
     * Move to the current node's parent and compute the new
     * coordinates (for the parent node).
     */
    public void moveUpwards() {
		DefaultGraphCell currentNode = getVisualNode();
		
		// moving to the parent's x-position...
        x = x - layout.getRelXtoParent(currentNode);
        
        // ...and upwards by one node height + y-distance
        y = y - (nodeYDistance + 30);
        super.moveUpwards();
    }
        
    /**
     * Move to the current node's most left child and compute 
     * the new coordinates (for the child node).
     */
    public void moveDownwards() {
        super.moveDownwards();
        DefaultGraphCell currentNode = getVisualNode();
        
        // moving to the next x-coordinate...
		 x = x + layout.getRelXtoParent(currentNode);
	    
		 // ...and down by the node height + the node distance
		 y = y + (nodeYDistance + 30);
    }
    
    /**
     * Move to the current node's right sibling
     * and compute the new coordinates (for the sibling).
     */
    public void moveSidewards() {
		
    	// move to the parent first...
		x = x - layout.getRelXtoParent(getVisualNode());
		
		// change the current node...
        super.moveSidewards();
        
        // and move right again to the next child
		 x = x + layout.getRelXtoParent(getVisualNode());
    }
    
    
    /**
     * Computes the positions for the current node and
     * stores it in a map of the given layout algorithm.
     */
    public void processCurrentNode() {
        DefaultGraphCell currentNode = getVisualNode();
        int parentX = x - layout.getRelXtoParent(currentNode);
        int parentY = y - (nodeYDistance + 30);
		
		layout.getRelXtoRoot().put(currentNode,x);
        layout.addRelYpos(currentNode, y);
    }
    
}
