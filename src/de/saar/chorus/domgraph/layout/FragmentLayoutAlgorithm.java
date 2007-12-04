package de.saar.chorus.domgraph.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org._3pq.jgrapht.Edge;

import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.EdgeType;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.domgraph.layout.treelayout.DomGraphDrawingCursor;
import de.saar.chorus.domgraph.layout.treelayout.DomGraphLayoutCursor;
import de.saar.chorus.gecode.BoundingBox;
import de.saar.chorus.gecode.PostOrderNodeVisitor;
import de.saar.chorus.gecode.PreOrderNodeVisitor;
import de.saar.chorus.gecode.Shape;

public abstract class FragmentLayoutAlgorithm extends LayoutAlgorithm {
	
//	 the position of a node within its fragment
	protected Map<String, Integer> relXtoParent, relXtoRoot, relYpos;

	protected Map<String, Integer> fragWidth, fragHeight, fragOffset, fragYpos, fragXpos;

	protected Map<String, Shape> nodesToShape;
	
	protected Map<String, List<String>> fragmentToHoles;

	protected Set<String> fragments;
		
	protected DomGraph domgraph;
	protected NodeLabels nodelabels;
	protected Canvas canvas;
	
	
	/**
	 * computes the tree layout for each fragment.
	 * 
	 */
	 void computeFragmentLayouts() {

		 
		// iterating over the fragments
		for (String frag : fragments) {
			
			// the recent root
			String root = domgraph.getRoot(frag);
			// computing the x-positions, dependent on the _direct_
			
			
			DomGraphLayoutCursor layCursor = new DomGraphLayoutCursor(root, canvas, this,
					domgraph, domgraph.getFragment(frag), nodeToLabel);
			
			PostOrderNodeVisitor postVisitor = new PostOrderNodeVisitor(
					layCursor);
			postVisitor.run();

			// another DFS computes the y- and x-positions relative to the
			// _root_
			
			
			DomGraphDrawingCursor drawCursor = new DomGraphDrawingCursor(root, this,
					domgraph, domgraph.getFragment(frag));
			PreOrderNodeVisitor preVisitor = new PreOrderNodeVisitor(drawCursor);
			preVisitor.run();
			
		}

	}

	

	/**
	 * Resolving the holes of a fragment in the right order. This does not the
	 * same job as <code>getFragLeaves</code>, because leaves that are roots
	 * at the same time have to be excluded.
	 * 
	 * @param frag
	 *            the fragment to get the holes from
	 * @return the list of holes; an empty list if there are none.
	 */
	 protected List<String> getFragHoles(String frag) {
		 if(fragmentToHoles.containsKey(frag)) {
			 return fragmentToHoles.get(frag);
		 } else {
			
			 List<String> holes = domgraph.getHoles(frag);
		
			
			 fragmentToHoles.put(frag, holes);
			 return holes;
		 }
	}

	/**
	 * Compute the incoming edges of a fragment.
	 * 
	 * @param frag
	 *            the fragment to compute the in-edges for
	 * @return the sorted list of incoming edges.
	 */
	 protected List<Edge> getFragInEdges(String frag) {
		
		List<Edge> inedges = new ArrayList<Edge>();
		for (String node : domgraph.getFragment(frag)) {
			inedges.addAll(domgraph.getInEdges(node, de.saar.chorus.domgraph.graph.EdgeType.DOMINANCE));
		}

		return inedges;
	}

	/**
	 * Compute the outgoing edges of a fragment.
	 * 
	 * @param frag
	 *            the fragment to compute the out-edges for
	 * @return the sorted list of outgoing edges.
	 */
	 protected List<Edge> getFragOutEdges(String frag) {

		List<Edge> outEdges = new ArrayList<Edge>();

		/*
		 * the outgoing edges are the edges going out from the fragment's holes.
		 * The holes are saved in the right order and
		 * getOutEdges(DefaulGraphCell) returnes a sorted list of edges. So the
		 * list of out-edges is resolved by computing the out-edges of a hole
		 * and appending the out-edges of the next hole(s).
		 */
		
		
		
		for(String node : domgraph.getFragment(frag)) {
				outEdges.addAll(domgraph.getOutEdges(node, EdgeType.DOMINANCE));
		}
		Collections.reverse(outEdges);
		return outEdges;

	}

	/**
	 * Resolving the number of dominance edges adjacent to a fragment.
	 * 
	 * @param frag
	 *            the fragment to compute the degree for
	 * @return the fragment degree (considering the fragment graph)
	 */
	 protected int getFragDegree(String frag) {
		// adding indegree and outdegree.
		return getFragInEdges(frag).size() + getFragOutEdges(frag).size();
	}

	
	/**
	 * computes the dimensions of all fragments using computeFragHeight and
	 * computeFragWidth. Before computing a fragment's height, computeXandDepth
	 * is called.
	 * 
	 */
	private void computeFragDimensions() {
		computeFragmentLayouts();
		
		for (String frag : fragments) {
			fragHeight.put(frag, new Integer(computeFragHeight(frag)));
			fragWidth.put(frag, new Integer(computeFragWidth(frag)));
		}
	}
	
	/**
	 * computes the height of a fragment depndent on its maximal depth. node:
	 * this method is not meaningful before having performed computeXandDepth.
	 * 
	 * @param frag
	 *            the fragment
	 */
	private int computeFragHeight(String frag) {
	
		int toReturn = 0;
		for(String hole: getFragHoles(frag)) {
			toReturn = Math.max(relYpos.get(hole), toReturn);
		}
		return toReturn + 30;
	}

	/**
	 * computes the width of all fragments dependent on the number of their
	 * leaves.
	 */
	private int computeFragWidth(String frag) {

		
		Shape box = nodesToShape.get(frag);
		BoundingBox bb = box.getBoundingBox();
		int extL = bb.left;
		int extR = bb.right;

		int offset = 0 - extL;
		fragOffset.put(frag, offset);

		return extR - extL;
	}
	/**
	 * @return Returns the nodesToShape.
	 */
	public Map<String, Shape> getNodesToShape() {
		return nodesToShape;
	}

	public Integer getRelXtoParent(String node) {
		return relXtoParent.get(node);
	}

	public void addRelXtoParent(String node, Integer x) {
		relXtoParent.put(node, x);
	}

	public void addRelYpos(String node, Integer y) {
		relYpos.put(node, y);
	}

	

	public Shape getNodesToShape(String node) {
		return nodesToShape.get(node);
	}

	public void addRelXtoRoot(String node, Integer x) {
		relXtoRoot.put(node, x);
	}

	
	/**
	 * @return Returns the relXtoRoot.
	 */
	public Map<String, Integer> getRelXtoRoot() {
		return relXtoRoot;
	}


	public void putNodeToShape(String node, Shape shape) {
		nodesToShape.put(node, shape);
	}
	
	public void initialise(DomGraph graph, NodeLabels labels, Canvas canv) throws LayoutException {
		domgraph = graph; 
		fragments = domgraph.getAllRoots();
		canvas = canv;
		nodelabels = labels;
		
		fragWidth = new HashMap<String, Integer>();
		fragHeight = new HashMap<String, Integer>();
		fragOffset = new HashMap<String, Integer>();

		fragXpos = new HashMap<String, Integer>();
		fragYpos = new HashMap<String, Integer>();
		
		relXtoParent = new HashMap<String, Integer>();
		nodesToShape = new HashMap<String, Shape>();
		relXtoRoot = new HashMap<String, Integer>();
		relYpos = new HashMap<String, Integer>();
		
		fragmentToHoles = new HashMap<String, List<String>>();
	}
	
	protected abstract void computeFragmentPositions();
	protected abstract void computeNodePositions();
	protected abstract void placeNodes();
	protected abstract void drawEdges();
	
	protected void layout(DomGraph graph, NodeLabels labels, Canvas canvas) throws LayoutException {
		initialise(graph, labels,canvas);
		computeFragDimensions();
		computeFragmentPositions();
		
		computeNodePositions();
		
		placeNodes();
		
		drawEdges();

	}


}
