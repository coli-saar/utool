package de.saar.chorus.domgraph.layout.solvedformlayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org._3pq.jgrapht.Edge;
import org.jgraph.JGraph;

import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.domgraph.layout.Canvas;
import de.saar.chorus.domgraph.layout.LayoutAlgorithm;
import de.saar.chorus.gecode.BoundingBox;
import de.saar.chorus.gecode.PostOrderNodeVisitor;
import de.saar.chorus.gecode.PreOrderNodeVisitor;
import de.saar.chorus.gecode.Shape;
import de.saar.chorus.ubench.JDomGraphTab;


/**
 * A layout algorithm for a solved form of a dominance graph
 * represented by a <code>JDomGraph</code>. This tree layout
 * uses the <code>GECODE</code> tree layout classes.
 * 
 * @author Michaela Regneri
 *
 */
public class SFGecodeTreeLayout extends LayoutAlgorithm {
	
	// the solved form to layout
	private DomGraph graph;
	private NodeLabels nodelabels;
	private Canvas canvas;
	
	private int globalXOffset;
	// the relative positions...
	
	//...to the parent node
	private Map<String,Integer> relXtoParent;
	
	//to the graph root
	private Map<String,Integer> relXtoRoot;
	private Map<String,Integer> relYpos;
	
	// the absolute position of a node in the graph
	private Map<String,Integer> xPos;
	private Map<String,Integer> yPos;
	
	// maps the root of every subtree to the subtree shape
	private Map<String, Shape> nodesToShape;

	
	public SFGecodeTreeLayout() {
		globalXOffset = 0;
	}
	
	/**
	 * Initializes a new <code>SolvedFormLayout</code> with the
	 * given <code>JDomGraph</code>.
	 * 
	 * @param gr the solved form as <code>JDomGraph</code> to layout
	 */
	public SFGecodeTreeLayout(int offset) {
		globalXOffset = offset;
	}
	
	private void initialise(DomGraph gr, NodeLabels labels, Canvas canv) {
		graph = gr;
		nodelabels = labels;
		canvas = canv;
		relXtoParent = new HashMap<String,Integer>();
		relXtoRoot = new HashMap<String, Integer>();
		relYpos = new HashMap<String,Integer>();
		
		xPos = new HashMap<String,Integer>();
		yPos = new HashMap<String,Integer>();
		
		nodesToShape = new HashMap<String, Shape>();
	}
  
   
	/**
	 * Computes the root of a <code>JDomGraph</code>, assuming
	 * that it is a forest.
	 * (Otherwise it will return the first node without
	 *  incoming edges.)
	 * 
	 * @param theGraph, the <code>JDomGraph</code> to compute the root for
	 * @return null if there is no node, the root otherwise
	 */
	private List<String> computeGraphRoots() {
		List<String> roots =new ArrayList<String>();
		
		for( String node : (Set<String>) graph.getAllNodes() ) {
			if( graph.getParents(node,null).size() == 0 ) {
				roots.add(node);
			}
		}
		
		return roots;
	}
	
	
	/**
	 * 
	 * Computes the x- and y-positions of every node and
	 * stores it in the xPos resp. yPos map.
	 * 
	 */
	private void computeNodePositions() {
		
		// the root is the node to start DFS with.
		List<String> roots = computeGraphRoots();
		 
		int totheright = 0;
		
		for(String root : roots) {
		
		 // computing the x-positions, dependent on the _direct_
		 // parent
		 SFGraphLayoutCursor layCursor = new SFGraphLayoutCursor(root, canvas, this, graph, nodeToLabel);
	     PostOrderNodeVisitor postVisitor = new PostOrderNodeVisitor(layCursor);
	     postVisitor.run();
		 
		 // another DFS computes the y- and x-positions relativ to the
		 // _root_
	     SFGraphDrawingCursor drawCursor = new SFGraphDrawingCursor(root,  this, graph);
		 PreOrderNodeVisitor preVisitor = new PreOrderNodeVisitor(drawCursor);
	     preVisitor.run();
	     
	     
	     // the whole tree shape (subtree with the graph root as root)
	     Shape box = nodesToShape.get(root);
	     
	     // the box containing the tree
	     BoundingBox bb = box.getBoundingBox();
	     
	     // the extrend to the left (starting from the root)
	     int extL = bb.left;
	     int width = bb.right - extL;
	     // the offset to move the nodes in order to
	     // start at 0 and get positive x-coordinates.
	     int offset = 0 - extL;
	     
	     // computing the absolute coordinates for every node
	     // resp. their left upper corner.
	     for( String node : relXtoRoot.keySet() ) {

	    	 if(! xPos.containsKey(node)) {

	    		 /*
	    		  * The stored value represents the x-coordinate of the 
	    		  * middle axis relative to the graph root middle axis.
	    		  * So we move everithing to the right (to start at x=0)
	    		  * and additionally reduce by the half width of the node.
	    		  */

	    		 int x = relXtoRoot.get(node) + offset - canvas.getNodeWidth(nodeToLabel.get(node))/2;
	    		 xPos.put(node, x + globalXOffset + totheright);

	    		 // the root y-position is zero, that's why the relative
	    		 // y-positions are equivalent to the absolute ones.
	    		 int y = relYpos.get(node);
	    		 yPos.put(node,y);
	    	 }
	     }
	     totheright += width;
		}
	}
	
	
	/**
	 * places the nodes in the graph model.
	 * Not meaningful without having computed
	 * the fragment graph as well as the relative
	 * x- and y-positions.
	 */
	private void placeNodes() {
		
		
		//place every node on its position
		//and remembering that in the viewMap.
		for(String node :  graph.getAllNodes() ) {
			int x = xPos.get(node).intValue();
			int y = yPos.get(node).intValue();
			
			canvas.drawNodeAt(x, y, node, nodelabels.getLabel(node), 
					graph.getData(node), nodeToLabel.get(node));
		}
		
		
	}
	
	
	
	/**
	 * Starts the layout algorithm.
	 */
	public void run(JGraph gr, Object[] cells, int arg2) {
		computeNodePositions();
		placeNodes();
	}
	
	protected void layout(DomGraph graph, NodeLabels labels, Canvas canvas) {
		initialise(graph, labels, canvas);
		computeNodePositions();
		placeNodes();
		drawEdges();
	}

    public Integer getRelXtoParent(String node) {
        return relXtoParent.get(node);
    }

    
    public void addRelXtoParent(String node, Integer x) {
        relXtoParent.put(node,x);
    }

    public void addRelXtoRoot(String node, Integer x) {
        relXtoRoot.put(node,x);
    }

    public void addRelYpos(String node, Integer y) {
        relYpos.put(node,y);
    }

	
	
	public Shape getNodesToShape(String node) {
		return nodesToShape.get(node);
	}
    
    public void putNodeToShape(String node, Shape shape) {
        nodesToShape.put(node,shape);
    }
	
    private void drawEdges() {
		for(Edge edge: graph.getAllEdges()) {
			String src = (String) edge.getSource();
			String tgt = (String) edge.getTarget();
			if( graph.getData(edge).getType().equals(de.saar.chorus.domgraph.graph.EdgeType.TREE)) {
				canvas.drawTreeEdge(src, tgt);
			} else {
				canvas.drawDominanceEdge(src, tgt);
			}
		}
	}
}
