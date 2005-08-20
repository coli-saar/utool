package de.saar.chorus.jgraph;

import static de.saar.chorus.jgraph.GecodeTreeLayoutSettings.nodeXDistance;

import java.util.Iterator;
import java.util.List;

import org.jgraph.graph.DefaultGraphCell;

import de.saar.chorus.treelayout.Extent;
import de.saar.chorus.treelayout.Shape;
import de.saar.chorus.treelayout.ShapeList;

/**
 * A class to determine the positions of nodes in a graph that 
 * is a tree, relative to their direct parents. These positions are stored 
 * in a given layout algorithm and converted later on by a 
 * <code>GraphDrawingCursor</code>.
 * 
 * A subclass of <code>GraphNodeCursor</code>.
 * 
 * @author Marco Kuhlmann
 * @author Michaela Regneri
 *
 */
class GraphLayoutCursor extends GraphNodeCursor {
		ImprovedJGraph graph;
		GecodeTreeLayout layout;
		
		/**
		 * Creates a new <code>GraphLayoutCursor</code>
		 * 
		 * @param theNode the graph root
		 * @param theLayout the layout algorithm to store the coordinates
		 * @param theGraph the graph to layout
		 */
	    public GraphLayoutCursor(DefaultGraphCell theNode,  
								GecodeTreeLayout theLayout, ImprovedJGraph theGraph) {
	        super(theNode, theGraph);
			
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
	     *  Computes the x- and y-coordinates of the current node,
	     * both relative to the direct parent node.
	     */
	    public void processCurrentNode() {
	        DefaultGraphCell currentNode = getVisualNode();
			if( graph.isRoot(currentNode) ) {
                layout.addRelXtoParent(currentNode,0);
			}
            
			Extent extent = new Extent(layout.getNodeWidth(currentNode));
			Shape shape;
			if ( graph.isLeaf(currentNode) ) {
				shape = new Shape(extent);
			} else {
				ShapeList childShapes = new ShapeList(nodeXDistance);
                List<DefaultGraphCell> children = graph.getChildren(currentNode);
                for( DefaultGraphCell nextChild : children ) {
					childShapes.add(layout.getNodesToShape(nextChild));
				}
                
				Shape subtreeShape = childShapes.getMergedShape();
				subtreeShape.extend(- extent.extentL, - extent.extentR);
				shape = new Shape(extent, subtreeShape);
				
                Iterator offsetIterator = childShapes.offsetIterator();
                for( DefaultGraphCell nextChild : children ) {
					int childOffset = ((Integer) offsetIterator.next()).intValue();
					layout.addRelXtoParent(nextChild,childOffset);
				}
			}
			layout.putNodeToShape(currentNode,shape);
		}
				
}
