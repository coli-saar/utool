package de.saar.chorus.domgraph.layout.treelayout;

import static de.saar.chorus.domgraph.layout.treelayout.GecodeTreeLayoutSettings.nodeXDistance;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.layout.Canvas;
import de.saar.chorus.domgraph.layout.FragmentLayoutAlgorithm;
import de.saar.chorus.gecode.Extent;
import de.saar.chorus.gecode.Shape;
import de.saar.chorus.gecode.ShapeList;

/**
 * A class to determine the positions of nodes in a graph that 
 * is a tree, relative to their direct parents. These positions are stored 
 * in a given layout algorithm and converted later on by a 
 * <code>GNGraphDrawingCursor</code>.
 * 
 * A subclass of <code>GNGraphNodeCursor</code>.
 * 
 * @author Marco Kuhlmann
 * @author Michaela Regneri
 *
 */
public class GNGraphLayoutCursor extends GNGraphNodeCursor {
		DomGraph graph;
		FragmentLayoutAlgorithm layout;
		Canvas canvas;
		Map<String, String> nodelabels;
		Set<String> nodes;
		
		/**
		 * Creates a new <code>GNGraphLayoutCursor</code>
		 * 
		 * @param theNode the graph root
		 * @param theLayout the layout algorithm to store the coordinates
		 * @param theGraph the graph to layout
		 */
	    public GNGraphLayoutCursor(String theNode,  Canvas canv, 
								FragmentLayoutAlgorithm theLayout, 
								DomGraph theGraph, Map<String, String> nl) {
	        super(theNode, theGraph);
			
	        nodelabels = nl;
			graph=theGraph;
			layout = theLayout;
			canvas = canv;
			nodes = theGraph.getAllNodes();
	    }
	    
	    
	    /**
		 * Creates a new <code>GNGraphLayoutCursor</code>
		 * 
		 * @param theNode the graph root
		 * @param theLayout the layout algorithm to store the coordinates
		 * @param theGraph the graph to layout
		 * @param theNodes nodes the layout shall arrange
		 */
	    public GNGraphLayoutCursor(String theNode,  Canvas canv,
	    		FragmentLayoutAlgorithm theLayout, DomGraph theGraph, 
								Set<String> theNodes, Map<String, String> nl) {
	        super(theNode, theGraph, theNodes);
			
	        nodelabels = nl;
			graph=theGraph;
			layout = theLayout;
			canvas = canv;
			nodes = theNodes;
	    }
	    /**
	     * @return the recently processed node
	     */
	    private String getVisualNode() {
	        return super.getCurrentNode();
	    }
	    
	    /**
	     *  Computes the x- and y-coordinates of the current node,
	     * both relative to the direct parent node.
	     */
	    public void processCurrentNode() {
	        String currentNode = getVisualNode();

	        boolean containsParents = false;
	        
	        
			if( graph.isRelativeRoot(currentNode, nodes) )  {
                layout.addRelXtoParent(currentNode,0);
			}
          
			Extent extent = new Extent(canvas.getNodeWidth(nodelabels.get(currentNode)));
			Shape shape;
			List<String> children = graph.getChildren(currentNode, null);
			children.retainAll(nodes);
			if ( graph.isRelativeLeaf(currentNode, nodes) ) {
				shape = new Shape(extent);
			} else {
				ShapeList childShapes = new ShapeList(nodeXDistance);
              //  List<DefaultGraphCell> children = graph.getChildren(currentNode);
                for( String nextChild : children ) {
                	if(nodes.contains(nextChild)) {
					childShapes.add(layout.getNodesToShape(nextChild));
				
                	}
                }
                
				Shape subtreeShape = childShapes.getMergedShape();
				subtreeShape.extend(- extent.extentL, - extent.extentR);
				shape = new Shape(extent, subtreeShape);
				
                Iterator offsetIterator = childShapes.offsetIterator();
                for( String nextChild : children ) {
					int childOffset = ((Integer) offsetIterator.next()).intValue();
					layout.addRelXtoParent(nextChild,childOffset);
				}
			}
			layout.putNodeToShape(currentNode,shape);
		}
				
}
