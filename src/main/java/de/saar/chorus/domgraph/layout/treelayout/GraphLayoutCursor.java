package de.saar.chorus.domgraph.layout.treelayout;

import static de.saar.chorus.domgraph.layout.treelayout.GecodeTreeLayoutSettings.nodeXDistance;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jgraph.graph.DefaultGraphCell;

import de.saar.chorus.gecode.Extent;
import de.saar.chorus.gecode.Shape;
import de.saar.chorus.gecode.ShapeList;
import de.saar.chorus.jgraph.improvedjgraph.ImprovedJGraph;
import de.saar.chorus.jgraph.improvedjgraph.layout.ImprovedJGraphLayout;

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
public class GraphLayoutCursor extends GraphNodeCursor {
		ImprovedJGraph graph;
		ImprovedJGraphLayout layout;
		Set<DefaultGraphCell> nodes;
		
		/**
		 * Creates a new <code>GraphLayoutCursor</code>
		 * 
		 * @param theNode the graph root
		 * @param theLayout the layout algorithm to store the coordinates
		 * @param theGraph the graph to layout
		 */
	    public GraphLayoutCursor(DefaultGraphCell theNode,  
								ImprovedJGraphLayout theLayout, ImprovedJGraph theGraph) {
	        super(theNode, theGraph);
			
			graph=theGraph;
			layout = theLayout;
			nodes = theGraph.getNodes();
	    }
	    
	    
	    /**
		 * Creates a new <code>GraphLayoutCursor</code>
		 * 
		 * @param theNode the graph root
		 * @param theLayout the layout algorithm to store the coordinates
		 * @param theGraph the graph to layout
		 * @param theNodes nodes the layout shall arrange
		 */
	    public GraphLayoutCursor(DefaultGraphCell theNode,  
								ImprovedJGraphLayout theLayout, ImprovedJGraph theGraph, 
								Set<DefaultGraphCell> theNodes) {
	        super(theNode, theGraph, theNodes);
			
			graph=theGraph;
			layout = theLayout;
			nodes = theNodes;
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

	        boolean containsParents = false;
	        
	        
			if( graph.isRelativeRoot(currentNode, nodes) )  {
                layout.addRelXtoParent(currentNode,0);
			}
            
			Extent extent = new Extent(layout.getNodeWidth(currentNode));
			Shape shape;
			List<DefaultGraphCell> children = graph.getChildren(currentNode);
			children.retainAll(nodes);
			if ( graph.isRelativeLeaf(currentNode, nodes)) {
				shape = new Shape(extent);
			} else {
				ShapeList childShapes = new ShapeList(nodeXDistance);
              //  List<DefaultGraphCell> children = graph.getChildren(currentNode);
                for( DefaultGraphCell nextChild : children ) {
                	if(nodes.contains(nextChild)) {
					childShapes.add(layout.getNodesToShape(nextChild));
				
                	}
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
