package de.saar.chorus.leonardo;

import static de.saar.chorus.leonardo.DomGraphLayoutParameters.nodeXDistance;

import java.util.Iterator;
import java.util.List;

import org.jgraph.graph.DefaultGraphCell;

import de.saar.chorus.treelayout.Extent;
import de.saar.chorus.treelayout.Shape;
import de.saar.chorus.treelayout.ShapeList;

/**
 * A class to determine the positions of nodes in a fragment, 
 * relative to their direct parents. These positions are stored 
 * in a given layout algorithm and converted later on by a 
 * <code>FragmentDrawingCursor</code>.
 * 
 *  A subclass of <code>FragmentNodeCursor</code>.
 * 
 * @author Marco Kuhlmann
 * @author Michaela Regneri
 *
 */
public class FragmentLayoutCursor extends FragmentNodeCursor {
	
	JDomGraph graph;		// the parent graph
	Fragment fragment;		// the fragment to layout
	DomGraphLayout layout;  // the layout algorithm (for storing values)
	
	/**
	 * Creates a new <code>FragmentLayoutCursor</code>
	 * 
	 * @param theNode the root of the fragment
	 * @param frag	the fragment to layout
	 * @param theLayout the layout algorithm to store the coordinates
	 * @param theGraph the parent graph
	 */
    public FragmentLayoutCursor(DefaultGraphCell theNode, Fragment frag, 
							DomGraphLayout theLayout, JDomGraph theGraph) {
        super(theNode, frag, theGraph);
		fragment = frag;
		graph=theGraph;
		layout = theLayout;
    }
    
    /**
     * @return the recently processed node
     */ 
    private DefaultGraphCell getVisualNode() {
        return super.getCurrentNode();
    }
    
    /**
     * Computes the x- and y-coordinates of the current node,
     * both relative to the direct parent node.
     */
    public void processCurrentNode() {
    	DefaultGraphCell currentNode = getVisualNode();
        
    	// the root has no parent, so its relative position is
    	// 0
		if(graph.isRelativeRoot(currentNode, fragment.getNodes()) ) {
			layout.addRelXtoParent(currentNode,0);
		}
		
		// extent of the curent node
		Extent extent = new Extent(layout.getNodeWidth(currentNode));
		Shape shape;
		
		// a leaf has no further extent (no subtree)
		if (graph.isRelativeLeaf(currentNode, fragment.getNodes())) {
			shape = new Shape(extent);
		} else {
			
			/*
			 * Resolving the current nodes children and their
			 * extent.
			 */
			ShapeList childShapes = new ShapeList(nodeXDistance);
            List<DefaultGraphCell > children = graph.getChildren(currentNode);
            for( DefaultGraphCell child : children ) {
                childShapes.add(layout.getNodesToShape().get(child));
			}
            
			Shape subtreeShape = childShapes.getMergedShape();
			subtreeShape.extend(- extent.extentL, - extent.extentR);
			shape = new Shape(extent, subtreeShape);
            
			Iterator offsetIterator = childShapes.offsetIterator();
            for( DefaultGraphCell nextChild : graph.getChildren(currentNode) ) {
				int childOffset = ((Integer) offsetIterator.next()).intValue();
				layout.addRelXtoParent(nextChild,childOffset);
			}
		}
		layout.getNodesToShape().put(currentNode,shape);
	}
			
			
}
