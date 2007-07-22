package de.saar.chorus.domgraph.layout.treelayout;

import static de.saar.chorus.domgraph.layout.treelayout.GecodeTreeLayoutSettings.nodeYDistance;

import java.util.Set;

import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.layout.FragmentLayoutAlgorithm;


/**
 * A class to determine the coordinates of a node relative to the graph root.
 * This converts the positions (relative to the direct parent) computet by a
 * <code>GNGraphLayoutCursor</code> into positions relative to the
 *  root.
 * The computet coordinates are stored in maps of the given 
 * <code>SolvedFormLayout</code> and processed there.
 * 
 * A subclass of <code>GNGraphNodeCursor</code>.
 * 
 * @author Marco Kuhlmann
 * @author Michaela Regneri
 *
 */
public class GNGraphDrawingCursor extends GNGraphNodeCursor {
	int x;	// the recent x-coordinate
    int y;  // the recent y-coordinate
    
    FragmentLayoutAlgorithm layout; // the layout object to store 
    						 // the coordinates in
    
	
	/**
	 * Creates a new <code>GNGraphDrawingCursor</code>
	 * 
	 * @param theNode the graph root
	 * @param theLayout the layout algorithm to store the coordinates
	 * @param aGraph the graph to layout
	 */
    public GNGraphDrawingCursor(String theNode, 
						FragmentLayoutAlgorithm theLayout, DomGraph aGraph) {
        super(theNode, aGraph);
        this.x = 0;
        this.y = 0;
		layout = theLayout;
    }
    
    /**
	 * Creates a new <code>GNGraphDrawingCursor</code>
	 * 
	 * @param theNode the graph root
	 * @param theLayout the layout algorithm to store the coordinates
	 * @param aGraph the graph to layout
	 * @param theNodes nodes the layout shall arrange 
	 */
    public GNGraphDrawingCursor(String theNode,
						FragmentLayoutAlgorithm theLayout, DomGraph aGraph, 
						 Set<String> theNodes) {
        super(theNode, aGraph, theNodes);
        this.x = 0;
        this.y = 0;
		layout = theLayout;
    }
    
    /**
     * 
     * @return the recently processed node
     */
    private String getVisualNode() {
        return super.getCurrentNode();
    }
    
    /**
     * Move to the current node's parent and compute the new
     * coordinates (for the parent node).
     */
    public void moveUpwards() {
		String currentNode = getVisualNode();
        x = x - layout.getRelXtoParent(currentNode);
        y = y - (nodeYDistance + 30);
        super.moveUpwards();
    }
        
    /**
     * Move to the current node's most left child and compute 
     * the new coordinates (for the child node).
     */
    public void moveDownwards() {
        super.moveDownwards();
        String currentNode = getVisualNode();
		 x = x + layout.getRelXtoParent(currentNode);
	     y = y + (nodeYDistance + 30);
    }
    
    /**
     * Move to the current node's right sibling
     * and compute the new coordinates (for the sibling).
     */
    public void moveSidewards() {
		x = x - layout.getRelXtoParent(getVisualNode());
        super.moveSidewards();
		 x = x + layout.getRelXtoParent(getVisualNode());
    }
    
    
    /** 
     * Computes the positions for the current node and
     * stores it in a map of the given layout algorithm.
     */
    public void processCurrentNode() {
        String currentNode = getVisualNode();
        int parentX = x - layout.getRelXtoParent(currentNode);
        int parentY = y - (nodeYDistance + 30);
		
		layout.addRelXtoRoot(currentNode,x);
		layout.addRelYpos(currentNode, y);
    }
}
