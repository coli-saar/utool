package de.saar.chorus.layout.chartlayout;

import static de.saar.chorus.layout.domgraphlayout.DomGraphLayoutParameters.fragmentXDistance;
import static de.saar.chorus.layout.domgraphlayout.DomGraphLayoutParameters.fragmentYDistance;
import static de.saar.chorus.layout.domgraphlayout.DomGraphLayoutParameters.nodeYDistance;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgraph.JGraph;
import org.jgraph.graph.AttributeMap;
import org.jgraph.graph.CellView;
import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.GraphConstants;

import de.saar.chorus.domgraph.chart.Chart;
import de.saar.chorus.domgraph.chart.ChartSolver;
import de.saar.chorus.domgraph.chart.Split;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.jgraph.GraphDrawingCursor;
import de.saar.chorus.jgraph.GraphLayoutCursor;
import de.saar.chorus.jgraph.ImprovedJGraphLayout;
import de.saar.chorus.layout.domgraphlayout.DomGraphLayoutParameters;
import de.saar.chorus.layout.treelayout.BoundingBox;
import de.saar.chorus.layout.treelayout.PostOrderNodeVisitor;
import de.saar.chorus.layout.treelayout.PreOrderNodeVisitor;
import de.saar.chorus.layout.treelayout.Shape;
import de.saar.chorus.ubench.EdgeType;
import de.saar.chorus.ubench.Fragment;
import de.saar.chorus.ubench.JDomGraph;
import de.saar.chorus.ubench.NodeType;

/**
 * This is a draft for a new chart-based layout algorithm.
 * 
 * 
 * 
 * @author Alexander Koller
 * @author Michaela Regneri
 */
public class DomGraphChartLayout extends ImprovedJGraphLayout {

	// the dominance graph
	private JDomGraph graph;

	private DomGraph domgraph;

	private Chart chart;

	// the fragments
	private Set<Fragment> fragments;

	// the fragment positions
	private Map<Fragment, Integer> fragXpos;

	private Map<Fragment, Integer> fragYpos;

	private Map<Fragment, Integer> fragWidth;

	private Map<Fragment, Integer> fragHeight;

	private Map<Fragment, String> fragToRoot;

	private Map<String, Fragment> rootToFrag;

	private List<Set<String>> layers;

	private Map<Fragment, DefaultGraphCell> leaflayer; // a leaf mapped to its
														// parent hole

	private Map<Fragment, Integer> fragmentToLayer;

	private Set<Fragment> oneHoleFrags;

	private List<Set<Fragment>> fraglayers;

	// the x-offset of the nodes of a fragment
	private Map<Fragment, Integer> fragOffset;

	// the position of a node within its fragment
	private Map<DefaultGraphCell, Integer> relXtoParent;

	private Map<DefaultGraphCell, Integer> relXtoRoot;

	private Map<DefaultGraphCell, Integer> relYpos;

	private Map<DefaultGraphCell, Shape> nodesToShape;

	// the absolute position of a node in the graph
	private Map<DefaultGraphCell, Integer> xPos;

	private Map<DefaultGraphCell, Integer> yPos;

	private Fragment movedRoot;

	private int yOffset;

	/**
	 * Initializes a new dominance graph layout of a given dominanc graph.
	 * 
	 * @param gr
	 *            the graph to compute the layout for
	 */
	public DomGraphChartLayout(JDomGraph gr, Chart ch, DomGraph origin) {
		/*
		 * initializing the graph and its attributes
		 */

		// checking whether or not the DomGraph is weakly normal
		if (origin.isWeaklyNormal()) {
			// if so, proceed with the "normal" chart
			chart = (Chart) ch.clone();
		} else {
			// if the graph is not wn, compute the chart of the wn backbone.
			DomGraph wnbackbone = origin.makeWeaklyNormalBackbone();
			chart = new Chart();
			ChartSolver.solve(wnbackbone, chart);
		}
		this.graph = gr;

		domgraph = origin;

		fragments = graph.getFragments();

		layers = new ArrayList<Set<String>>();
		fraglayers = new ArrayList<Set<Fragment>>();
		fragmentToLayer = new HashMap<Fragment, Integer>();

		movedRoot = null;
		yOffset = 0;
		// all the other fields are initialized empty

		fragXpos = new HashMap<Fragment, Integer>();
		fragYpos = new HashMap<Fragment, Integer>();

		fragWidth = new HashMap<Fragment, Integer>();
		fragHeight = new HashMap<Fragment, Integer>();

		fragOffset = new HashMap<Fragment, Integer>();

		oneHoleFrags = new HashSet<Fragment>();

		relXtoParent = new HashMap<DefaultGraphCell, Integer>();
		relYpos = new HashMap<DefaultGraphCell, Integer>();

		xPos = new HashMap<DefaultGraphCell, Integer>();
		yPos = new HashMap<DefaultGraphCell, Integer>();

		nodesToShape = new HashMap<DefaultGraphCell, Shape>();
		relXtoRoot = new HashMap<DefaultGraphCell, Integer>();

		fragToRoot = new HashMap<Fragment, String>();
		rootToFrag = new HashMap<String, Fragment>();

		for (Fragment frag : fragments) {
			String root = graph.getNodeData(frag.getRoot()).getName();
			fragToRoot.put(frag, root);
			rootToFrag.put(root, frag);
		}

		leaflayer = new HashMap<Fragment, DefaultGraphCell>();
	}

	private void fillLayers() {

		List<Set<String>> toplevel = new ArrayList<Set<String>>(chart
				.getToplevelSubgraphs());
		Set<String> roots = domgraph.getAllRoots();

		for (Set<String> tls : toplevel) {
			if (chart.containsSplitFor(tls)) {
				fillLayer(0, new ArrayList<Split>(chart.getSplitsFor(tls)),
						new HashSet<String>());
			} else {
				Set<String> leaves = new HashSet<String>(tls);
				addToLayer(leaves, 0);
			}
		}

		for (int i = 0; i < layers.size(); i++) {

			Set<String> layer = new HashSet<String>(layers.get(i));
			layer.retainAll(roots);
			fraglayers.add(i, new HashSet<Fragment>());

			for (String node : layer) {
				Fragment frag = rootToFrag.get(node);

				if (getFragDegree(frag) == 1
						&& getFragOutEdges(frag).size() == 0) {
					DefaultGraphCell parent = graph
							.getSourceNode(getFragInEdges(frag).iterator()
									.next());
					if (oneHoleFrags.contains(graph.findFragment(parent))) {
						leaflayer.put(frag, parent);
					} else {
						fraglayers.get(i).add(frag);
					}
				} else {

					fraglayers.get(i).add(frag);
				}
				fragmentToLayer.put(frag, i);
			}

		}

	}

	private void fillLayer(int layer, List<Split> splits, Set<String> visited) {

		Set<String> recent = new HashSet<String>();
		Set<Set<String>> remainingSubgraphs = new HashSet<Set<String>>();

		int nextLayer = layer + 1;

		for (Split split : splits) {
			String root = split.getRootFragment();
			if (!visited.contains(root)) {
				visited.add((root));
				recent.add(root);
				recent.addAll(new HashSet<String>(split.getAllDominators()));
				remainingSubgraphs.addAll(new ArrayList<Set<String>>(split
						.getAllSubgraphs()));
			}

		}

		addToLayer(recent, layer);

		for (Set<String> subgraph : remainingSubgraphs) {
			Set<String> sgc = new HashSet<String>(subgraph);
			sgc.removeAll(recent);
			for (Set<String> wccs : domgraph.wccs(sgc)) {
				List<Split> newSplits = chart.getSplitsFor(wccs);
				if (newSplits != null) {

					fillLayer(nextLayer, new ArrayList<Split>(newSplits),
							visited);
				} else {
					Set<String> leaffrags = new HashSet<String>(wccs);

					addToLayer(leaffrags, nextLayer);
					visited.add(wccs.iterator().next());
				}

			}
		}

	}

	private void addToLayer(Set<String> recent, int ind) {
		if (layers.size() <= ind) {
			layers.add(ind, recent);

		} else {
			layers.get(ind).addAll(recent);
		}
	}

	/**
	 * Generic method that handles maps from an Object to a list of objects and
	 * ads a new entry to the value list with the specified object key. If the
	 * map does not contain the key yet, it is added.
	 * 
	 * @param <E>
	 *            the key type
	 * @param <T>
	 *            the type of the list elements
	 * @param map
	 *            the map
	 * @param key
	 *            the key to which list the new value shall be added
	 * @param nVal
	 *            the new value
	 */
	public static <E, T> void addToMapList(Map<E, List<T>> map, E key, T nVal) {
		List<T> typedList;
		if (map.containsKey(key)) {
			typedList = map.get(key);
		} else {
			typedList = new ArrayList<T>();
			map.put(key, typedList);
		}
		typedList.add(nVal);
	}

	/**
	 * Perform DFS in a fragment to resolve its leave nodes in the right order.
	 * 
	 * @param frag
	 *            the fragment to get the leaves from
	 * @param root
	 *            the recent visited node
	 * @param leaves
	 *            the leaves resolved yet
	 */
	private void fragLeafDFS(Fragment frag, DefaultGraphCell root,
			List<DefaultGraphCell> leaves) {

		// if we found a leaf, we add it tot the list.
		if (frag.isLeaf(root)) {
			leaves.add(root);
		} else {
			/*
			 * if the node ist no (fragment-) leave, there have to be some
			 * children. DFS is performed for each child contained in the
			 * fragment.
			 */
			for (int i = 0; i < graph.getChildren(root).size(); i++) {
				if (frag.getNodes().contains(graph.getChildren(root).get(i)))
					fragLeafDFS(frag, graph.getChildren(root).get(i), leaves);
			}
		}
	}

	private void computeOneHoleFrags() {
		for (Fragment frag : fragments) {
			int childwidth = isOneHoleFrag(frag);
			if (childwidth > -1) {
				oneHoleFrags.add(frag);
				// 15 is half a hole.
				int newWidth = fragWidth.get(frag) + (childwidth / 2) - 15;
				fragWidth.remove(frag);
				fragWidth.put(frag, newWidth);
			}
		}
	}

	/**
	 * Resolve all leaves of a fragment (in the right order).
	 * 
	 * @param frag
	 *            the fragment to get the leaves for
	 * @return the list of leaves.
	 */
	private List<DefaultGraphCell> getFragLeaves(Fragment frag) {

		// initializing the leave list
		List<DefaultGraphCell> leaves = new ArrayList<DefaultGraphCell>();

		// performing DFS to fill the leave list.
		fragLeafDFS(frag, frag.getRoot(), leaves);

		return leaves;
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
	private List<DefaultGraphCell> getFragHoles(Fragment frag) {

		List<DefaultGraphCell> holes = new ArrayList<DefaultGraphCell>();

		for (DefaultGraphCell leaf : getFragLeaves(frag)) {
			if (graph.getNodeData(leaf).getType().equals(NodeType.unlabelled)) {
				holes.add(leaf);
			}
		}

		return holes;

	}

	/**
	 * Compute the incoming edges of a fragment.
	 * 
	 * @param frag
	 *            the fragment to compute the in-edges for
	 * @return the sorted list of incoming edges.
	 */
	private List<DefaultEdge> getFragInEdges(Fragment frag) {

		List<DefaultEdge> inedges = new ArrayList<DefaultEdge>();
		for (DefaultGraphCell node : frag.getNodes()) {
			for (DefaultEdge edge : graph.getInEdges(node)) {
				if (graph.getEdgeData(edge).getType() == EdgeType.dominance) {
					inedges.add(edge);
				}
			}
		}

		return inedges;
	}

	/**
	 * Compute the outgoing edges of a fragment.
	 * 
	 * @param frag
	 *            the fragment to compute the in-edges for
	 * @return the sorted list of outgoing edges.
	 */
	private List<DefaultEdge> getFragOutEdges(Fragment frag) {

		List<DefaultEdge> outEdges = new ArrayList<DefaultEdge>();

		/*
		 * the outgoing edges are the edges going out from the fragment's holes.
		 * The holes are saved in the right order and
		 * getOutEdges(DefaulGraphCell) returnes a sorted list of edges. So the
		 * list of out-edges is resolved by computing the out-edges of a hole
		 * and appending the out-edges of the next hole(s).
		 */
		for (DefaultGraphCell hole : getFragHoles(frag)) {
			outEdges.addAll(graph.getOutEdges(hole));
		}

		for (DefaultGraphCell node : frag.getNodes()) {

			for (DefaultEdge edge : graph.getOutEdges(node)) {
				if (graph.getEdgeData(edge).getType() == EdgeType.dominance
						&& (!outEdges.contains(edge))) {
					outEdges.add(edge);
				}
			}
		}

		return outEdges;

	}

	/**
	 * Resolving the number of dominance edges adjacent to a fragment.
	 * 
	 * @param frag
	 *            the fragment to compute the degree for
	 * @return the fragment degree (considering the fragment graph)
	 */
	int getFragDegree(Fragment frag) {

		// adding indegree and outdegree.
		return getFragInEdges(frag).size() + getFragOutEdges(frag).size();
	}

	/**
	 * computes the tree layout for each fragment.
	 * 
	 */
	private void computeFragmentLayouts() {

		// iterating over the fragments
		for (Fragment frag : fragments) {

			// the recent root
			DefaultGraphCell root = frag.getRoot();
			// computing the x-positions, dependent on the _direct_
			// parent

			GraphLayoutCursor layCursor = new GraphLayoutCursor(root, this,
					graph, frag.getNodes());
			PostOrderNodeVisitor postVisitor = new PostOrderNodeVisitor(
					layCursor);
			postVisitor.run();

			// another DFS computes the y- and x-positions relativ to the
			// _root_
			GraphDrawingCursor drawCursor = new GraphDrawingCursor(root, this,
					graph, frag.getNodes());
			PreOrderNodeVisitor preVisitor = new PreOrderNodeVisitor(drawCursor);
			preVisitor.run();

		}

	}

	/**
	 * computes the height of a fragment depndent on its maximal depth. node:
	 * this method is not meaningful before having performed computeXandDepth.
	 * 
	 * @param frag
	 *            the fragment
	 */
	private int computeFragHeight(Fragment frag) {

		Shape box = nodesToShape.get(frag.getRoot());

		// the height is the maximal depth* (node + distance) -
		// the height of the last node (depth starts at 1 at
		// shape)
		return box.depth() * (30 + nodeYDistance) - nodeYDistance;
	}

	/**
	 * computes the width of all fragments dependent on the number of their
	 * leaves.
	 */
	private int computeFragWidth(Fragment frag) {

		DefaultGraphCell fragRoot = frag.getRoot();
		Shape box = nodesToShape.get(fragRoot);
		BoundingBox bb = box.getBoundingBox();
		int extL = bb.left;
		int extR = bb.right;

		int offset = 0 - extL;
		fragOffset.put(frag, offset);

		return extR - extL;
	}

	public void putNodeToShape(DefaultGraphCell node, Shape shape) {
		nodesToShape.put(node, shape);
	}

	/**
	 * computes the dimensions of all fragments using computeFragHeight and
	 * computeFragWidth. Before computing a fragment's height, computeXandDepth
	 * is called.
	 * 
	 */
	private void computeFragDimensions() {
		computeFragmentLayouts();
		for (Fragment frag : graph.getFragments()) {

			fragHeight.put(frag, new Integer(computeFragHeight(frag)));
			fragWidth.put(frag, new Integer(computeFragWidth(frag)));
		}
	}

	
	/**
	 * This method determines "one-hole fragments" which are treated in a 
	 * in a special way during layout. Fragments of this kind are fragments which have 
	 * exactly one hole with at most one outgoing edge and fragments with exactly two holes,
	 * whereby the left hole's child is a leaf.
	 * 
	 * Returned is (for layout purposes) the width of the fragment's child, if the fragment 
	 * is a one-hole fragment and has a child. If there is no child, the width of the hole is returned.
	 * If the fragment is not a one-hole fragment, -1 is returned.
	 * 
	 * @param frag the fragment to check
	 * @return the width of the fragment's child if the frag is a one-hole fragment, -1 otherwise
	 */
	private int isOneHoleFrag(Fragment frag) {
		List<DefaultGraphCell> holes = getFragHoles(frag);
		
		/*
		 * Fragments with one hole fulfill the conditions, if
		 * they have at most one outgoing edge.
		 */
		if (holes.size() == 1) {
			DefaultGraphCell hole = holes.iterator().next();
			List<DefaultEdge> outedges = graph.getOutEdges(hole);
			
			// one outg. edge?
			if (outedges.size() == 1) {
				Fragment child = graph.getTargetFragment(outedges.iterator()
						.next());
				
				// child of the hole is a leaf?
				if (getFragDegree(child) == 1) {
					return fragWidth.get(child);
				} else {
					return graph.computeNodeWidth(hole);
				}
			}
		} else if (holes.size() == 2) {
			// 2 fragment holes
			
			
			boolean first = true;
			for (DefaultGraphCell hole : holes) {
				// the child of the left hole has to be a leaf.
				if (first) {
					first = false;
					List<DefaultEdge> outedges = graph.getOutEdges(hole);
					
					if (outedges.size() == 1) {
						// one edge out of the left hole
						
						Fragment child = graph.getTargetFragment(outedges
								.iterator().next());
						if (getFragDegree(child) == 1) {
							// the only child is a leaf
							return fragWidth.get(child);
						} else {
							// no leaf; fail.
							return -1;
						}
					}
				}
			}
		}

		// fail if there are more than two holes.
		return -1;
	}

	/**
	 * computes the whole fragment graph. computes the fragment's x-position
	 * with undirected DFS, the fragment's later y-position performing directed
	 * DFS (for each root).
	 */
	private void computeFragmentPositions() {

		int yFragPos = 0;
		int yPosToAdd = 0;
		
		
		Set<Fragment> visited = new HashSet<Fragment>();

		// compute y-positions by using the layers.
		
		// for all (main) layers...
		for (int i = 0; i < fraglayers.size(); i++) {
			Set<Fragment> layer = fraglayers.get(i);
			
			// iterate over the layer's fragments...
			for (Fragment frag : layer) {
				if (!visited.contains(frag)) {
					visited.add(frag);
					
					// and place them according to their depth.
					fragYpos.put(frag, yFragPos);
					yPosToAdd = Math.max(fragHeight.get(frag), yPosToAdd);
				}
			}
			yFragPos += yPosToAdd + DomGraphLayoutParameters.fragmentYDistance;
		}
		
		// for all leave fragments (entries in leaf layers)
		for(Map.Entry<Fragment, DefaultGraphCell> leafWithParent : leaflayer.entrySet()){
			
			Fragment leaf = leafWithParent.getKey();
			DefaultGraphCell sourceHole = leafWithParent.getValue();
			Fragment parent = graph.findFragment(sourceHole);

			// place the leaf fragment relative to its parent hole.
			int y = fragYpos.get(parent) + fragHeight.get(parent)
					+ (fragmentYDistance / 3);
			
			fragYpos.put(leaf, y);
		}
		

		int rightBorder = 0; 	// the right border of the recent fragment box
		int xoffset = 0;		// the x-position, where the next fragment box may start
		Fragment topFragment = null; // a variable for a top fragment, if there is one.
		
		// determining the top fragment
		if (fraglayers.get(0).size() == 1) {
			topFragment = fraglayers.get(0).iterator().next();
		}
		
		// making the top level fragment boxes
		for (Set<String> toplevel : chart.getToplevelSubgraphs()) {

			Set<String> free = new HashSet<String>();
			
			// free fragments of the toplevel subgraph
			if (chart.containsSplitFor(toplevel)) {
				for (Split split : chart.getSplitsFor(toplevel)) {
					free.addAll(domgraph.getFragment(split.getRootFragment()));
				}
			} else {
				
				// if there are no splits in the chart, 
				// we consider all fragments as free fragments.
				free.addAll(toplevel);
			}
			
			// toplevel fragment box
			FragmentBox box = makeFragmentBox(toplevel, free);

			// retrieving the relative x-positions in the fragment box
			// and placing the fragments
			for (Fragment boxfrag : box.frags) {
				int x = box.getBoxXPos(boxfrag) + xoffset;
				fragXpos.put(boxfrag, x);
				rightBorder = Math.max(rightBorder, x + fragWidth.get(boxfrag));
			}
			
			// the next toplevel box is placed to the right of the last one.
			xoffset = rightBorder + fragmentXDistance;
		}

		// placing the top fragment in the middle of the graph.
		if (topFragment != null) {
			fragXpos.remove(topFragment);
			fragXpos.put(topFragment, rightBorder / 2
					- fragWidth.get(topFragment) / 2);
			for (DefaultEdge edge : getFragOutEdges(topFragment)) {
				GraphConstants.setLineColor(graph.getModel()
						.getAttributes(edge), new Color(255, 204, 230));
			}
		}

	}

	/**
	 * Helper method that moves  all fragments in x direction by a given value.
	 * 
	 * @param x value indicating the graph movement.
	 */
	private void moveGraph(int x) {
		Set<Fragment> seen = new HashSet<Fragment>();
		for (DefaultGraphCell node : graph.getNodes()) {

			if (xPos.containsKey(node)) {
				int old = xPos.get(node);
				xPos.remove(node);
				xPos.put(node, old + x);
			} else {
				Fragment frag = graph.findFragment(node);
				if (!seen.contains(frag)) {
					seen.add(frag);
					int old = fragOffset.get(frag);
					fragOffset.remove(frag);
					fragOffset.put(frag, old + x);
				}
			}
		}
	}

	/**
	 * computes the position of all nodes considering their relative poitions
	 * within a fragment and the position of their fragment (cp. its fragment
	 * node).
	 */
	private void computeNodePositions() {

		int movegraph = 0;
		for (DefaultGraphCell node : graph.getNodes()) {

			Fragment nodeFrag = graph.findFragment(node);

			int x = relXtoRoot.get(node);
			int offset = fragOffset.get(nodeFrag)
					- graph.computeNodeWidth(node) / 2;
			int xMovement;

			if (leaflayer.containsKey(nodeFrag)) {
				DefaultGraphCell sourceHole = leaflayer.get(nodeFrag);
				Fragment parent = graph.findFragment(sourceHole);

				xMovement = relXtoParent.get(sourceHole)
						- graph.computeNodeWidth(node) / 2
						+ fragOffset.get(parent) + fragXpos.get(parent);

			} else {
				xMovement = fragXpos.get(nodeFrag) + offset;
			}
			/*
			 * the absolute x- position is the relative position added to the
			 * fragment's x-position
			 */
			x += xMovement;

			xPos.put(node, x);

			movegraph = Math.min(movegraph, x);

			/*
			 * the absolute y- position is the relative position added to the
			 * fragment's y-position and decreased by the minimal y-position
			 * (that moves the graph down to let it start by 0).
			 */

			int y = relYpos.get(node);

			y += fragYpos.get(nodeFrag);

			if (yOffset > 0 && (!nodeFrag.equals(movedRoot)))
				y += yOffset;

			yPos.put(node, y);

		}
		moveGraph(movegraph * -1);
	}


	

	/**
	 * places the nodes in the graph model. Not meaningful without having
	 * computed the fragment graph as well as the relative x- and y-positions.
	 */
	private void placeNodes() {
		// the view map to save all the node's positions.
		Map<DefaultGraphCell, AttributeMap> viewMap = new HashMap<DefaultGraphCell, AttributeMap>();

		// place every node on its position
		// and remembering that in the viewMap.
		for (DefaultGraphCell node : graph.getNodes()) {
			int x = xPos.get(node).intValue();
			int y = yPos.get(node).intValue();

			placeNodeAt(node, x, y, viewMap);
		}

		// updating the graph.
		graph.getGraphLayoutCache().edit(viewMap, null, null, null);
	}

	/**
	 * places a node at a given position and remembers the information in a
	 * given Attribute Map.
	 * 
	 * @param node,
	 *            the node to place
	 * @param x
	 *            the x-value of the upper left corner
	 * @param y
	 *            the y-value of the upper left corner
	 * @param viewMap
	 *            hte viewMap to save the position in
	 */
	private void placeNodeAt(DefaultGraphCell node, int x, int y,
			Map<DefaultGraphCell, AttributeMap> viewMap) {

		CellView view = graph.getGraphLayoutCache().getMapping(node, false);
		Rectangle2D rect = (Rectangle2D) view.getBounds().clone();
		Rectangle bounds = new Rectangle((int) rect.getX(), (int) rect.getY(),
				(int) rect.getWidth(), (int) rect.getHeight());

		bounds.x = x;
		bounds.y = y;

		AttributeMap map = graph.getModel().createAttributes();
		GraphConstants.setBounds(map, (Rectangle2D) bounds.clone());

		viewMap.put(node, map);
	}

	/**
	 * Starts the layout algorithm.
	 */
	public void run(JGraph gr, Object[] cells, int arg2) {

		computeFragDimensions();
		computeOneHoleFrags();
		fillLayers();
		computeFragmentPositions();
		computeNodePositions();
		placeNodes();

	}

	
	/****** Getters and Setters ********/
	
	
	/**
	 * @return Returns the nodesToShape.
	 */
	public Map<DefaultGraphCell, Shape> getNodesToShape() {
		return nodesToShape;
	}

	public Integer getRelXtoParent(DefaultGraphCell node) {
		return relXtoParent.get(node);
	}

	public void addRelXtoParent(DefaultGraphCell node, Integer x) {
		relXtoParent.put(node, x);
	}

	public void addRelYpos(DefaultGraphCell node, Integer y) {
		relYpos.put(node, y);
	}

	/**
	 * 
	 * @param node
	 * @return the width of the node
	 */
	public int getNodeWidth(DefaultGraphCell node) {
		return graph.computeNodeWidth(node);
	}

	public Shape getNodesToShape(DefaultGraphCell node) {
		return nodesToShape.get(node);
	}

	public void addRelXtoRoot(DefaultGraphCell node, Integer x) {
		relXtoRoot.put(node, x);
	}

	
	/**
	 * @return Returns the relXtoRoot.
	 */
	public Map<DefaultGraphCell, Integer> getRelXtoRoot() {
		return relXtoRoot;
	}

	
	
	/**** some helper methods ****/
	
	/**
	 * Helper method which takes a set of nodes represented as a string
	 * and returns a set of all fragments of which at least one node 
	 * was given.
	 * @param nodes a set of nodes as string
	 * @return the corresponding set of fragments
	 */
	Set<Fragment> convertStringsToFragments(Collection<String> nodes) {
		Set<Fragment> ret = new HashSet<Fragment>();
		Set<String> roots = new HashSet<String>(nodes);
		roots.retainAll(domgraph.getAllRoots());

		for (String root : roots) {
			ret.add(rootToFrag.get(root));
		}
		return ret;
	}

	/**
	 * Comparator sorting a collection of fragments ascending
	 * according to their number of outgoing edges. One-Hole fragments count
	 * as having fewer outgoing edges when if the actual number is equal.
	 * 
	 * @author Michaela Regneri
	 *
	 */
	public class FragmentOutDegreeComparator implements Comparator<Fragment> {

		/**
		 * @return 1 if the first fragment has more outgoing edges, 
		 * 	       0 if the number of outgoing edges is equal, -1 otherwise
		 */
		public int compare(Fragment arg0, Fragment arg1) {
			if (oneHoleFrags.contains(arg0) && oneHoleFrags.contains(arg1)) {
				return getFragOutEdges(arg0).size()
						- getFragOutEdges(arg1).size();
			} else if (oneHoleFrags.contains(arg0)) {
				return -1;
			} else if (oneHoleFrags.contains(arg1)) {
				return 1;
			} else
				return getFragOutEdges(arg0).size()
						- getFragOutEdges(arg1).size();
		}

	}

	/**
	 * Comparator sorting a Collection of Fragments according to their 
	 * number of incombing edges.
	 * @author Michaela Regneri
	 *
	 */
	public class FragmentInDegreeComparator implements Comparator<Fragment> {

		public int compare(Fragment arg0, Fragment arg1) {
			return getFragInEdges(arg0).size() - getFragInEdges(arg1).size();
		}

	}

	
	/***** FragmentBox handling *******/
	
	
	/**
	 * This computes a <code>FragmentBox</code> and recursively its children.
	 * It is responsible to initiate the layout as well. 
	 * Child-Boxes are boxes consisting of the wccs which emerge from removing the
	 * free fragments. 
	 * 
	 * 
	 * @param wcc the nodes of this box
	 * @param freefrags the roots of the free fragments in this box
	 * @return the resulting <code>FragmentBox</code>
	 */
	FragmentBox makeFragmentBox(Set<String> wcc, Set<String> freefrags) {
		
		
		// initialising the new box with the set of fragments it contains.
		Set<String> fragmentsString = new HashSet<String>(wcc);
		FragmentBox current = new FragmentBox(convertStringsToFragments(fragmentsString));
		

		/*
		 * Computing the complete current subgraph in order to check whether it 
		 * is a tree. If it is a tree, a different layout style is chosen, and 
		 * the child boxes are not computed.
		 */
		Set<DefaultGraphCell> mySubgraph = new HashSet<DefaultGraphCell>();
		for (Fragment frag : current.getFrags()) {
			mySubgraph.addAll(frag.getNodes());
		}
		
		if (graph.isForest(mySubgraph)) {
			// a tree --> tree layout
			if (!fragBoxTreeLayout(current)) {
				System.err.println(":'(");
			}
		} else {
			// no tree. compute the child boxes.
			
			Set<String> freefragments = new HashSet<String>(freefrags);
			Set<String> childnodes = new HashSet<String>(wcc);
			
			// remove the free fragments.
			childnodes.removeAll(freefragments);
			
			
			// asking the graph for the wccs I get for my fragments without
			// the free fragments.
			for (Set<String> childwcc : domgraph.wccs(childnodes)) {

				// this is to store the free fragments of the new wcc.
				Set<String> freeChildFrag = new HashSet<String>();

				// either the wcc is another subgraph in the chart...
				if (chart.containsSplitFor(childwcc)) {

					// then I record all the free fragments in there and
					// pass them on
					for (Split chspl : chart.getSplitsFor(childwcc)) {
						freeChildFrag.addAll(domgraph.getFragment(chspl
								.getRootFragment()));
					}
				} else {
					// if the wcc is not in the chart, it is a leaf.
					// the free fragments of my child are all fragments in the
					// child subgraph.
					freeChildFrag.addAll(childwcc);
				}

				// the new fragment box will be mapped to its free fragments 
				Set<Fragment> childroots = convertStringsToFragments(freeChildFrag);

				// a new childbox
				current.putChild(childroots, makeFragmentBox(childwcc,
						freeChildFrag));
			}

			
			/*
			 * Layout: Determine the best root and then execute the final DFS.
			 */
			
			if (current.getFrags().size() > 1) {
				
				// compute the "allowed" roots which we want to be on the left-hand side.
				Set<Fragment> possibleRoots = getPossibleRoots(current,
						convertStringsToFragments(freefragments));
				if (possibleRoots.isEmpty()) {
					// if there are no "good" roots, consider all fragments as possible roots.
					possibleRoots.addAll(current.getFrags());
				}

				// initialising the best root and the number of crossings.
				Fragment bestRoot = possibleRoots.iterator().next();
				int bestCost = -1;

				// try out all the roots and store the one leading to a layout 
				// with as few crossings as possible.
				for (Fragment frag : possibleRoots) {
					if (bestCost == -1) {
						// first loop
						bestCost = fragBoxDFS(current, 0,
								new HashSet<Fragment>(), frag, 0, null);
						bestRoot = frag;

					} else {
						// not the first loop.
						int nextCrossCount = fragBoxDFS(current, 0,
								new HashSet<Fragment>(), frag, 0, null);
						if (nextCrossCount < bestCost) {
							bestCost = nextCrossCount;
							bestRoot = frag;
						}
					}
					
					// reset the storages in the FragmentBox
					current.clear();
				}
				
				// final DFS
				fragBoxDFS(current, 0, new HashSet<Fragment>(), bestRoot, 0,
						null);

			} else {
				// There is at most one fragment in my box.
				// If there is one, place it at x = 0.
				if (!current.getFrags().isEmpty())
					current.setBoxXPos(current.getFrags().iterator().next(), 0);
			}

		}

		return current;
	}

	
	
	/**
	 * This computes recursively the x-positions of all fragments within a given fragment box. 
	 * The positions are relative to the left border of the box. It places the current 
	 * fragment, its children, its children's boxes, its parents, its parent's boxes and the
	 * parents of its children's boxes.
	 * 
	 * 
	 * @param box the fragment box
	 * @param x the x position for the next fragment
	 * @param visited the fragments already seen
	 * @param current the current fragment
	 * @param crossings the number of crossings counted so far.
	 * @param lastroot the root of the fragment visited right before this loop
	 * @return the number of crossings after this loop
	 */
	int fragBoxDFS(FragmentBox box, int x, Set<Fragment> visited,
			Fragment current, int crossings, DefaultGraphCell lastroot) {

		// initialising
		Set<Fragment> frags = box.getFrags(); // the fragments to consider
		int cross = crossings; // counter for crossings
		int nextX = x;		   // the x for the next DFS loop
		int myX = x;		   // x for the current fragment
		int rightX = x; 	   // x to the right of the current fragment
		
		// root of the current fragment
		DefaultGraphCell currentRoot = current.getRoot();

		// storage for the parent fragments of the current fragment
		List<Fragment> parents = new ArrayList<Fragment>();
		

		// Compute the hole by which we entered; if it's not the left one,
		// assume an edge crossing

		if (lastroot != null) {
			boolean lefthole = true; // the first hole is the left hole.

			for (DefaultGraphCell hole : getFragHoles(current)) {
				for (DefaultEdge outedge : graph.getOutEdges(hole)) {
					if (lastroot.equals(graph.getTargetNode(outedge))) {
						if (!lefthole) {
							cross++; // if the last fragment was not the
										// child of the left hole...
						}
					}
				}
				if (lefthole) {
					lefthole = false;
				}
			}
		}

		if (!visited.contains(current)) {
			/*
			 * Placing the recent fragment. It can be the case that I belong
			 * actually to a childbox of this box.
			 */
			FragmentBox myBox = box.getBoxForFrag(current);
			
			
			if (myBox == null) {
				// there is no childbox for myself.
				visited.add(current);
				
				// if I am _no_ "one-hole fragment"
				if (!oneHoleFrags.contains(current)) {
					// place me at my designated x-position if it is not
					// smaller than the next possible x position in my layer
					myX = Math.max(x, box.getNextPossibleX()[fragmentToLayer
							.get(current)]);
				} else {
					// if I am a one-hole fragment, just fill up the next space
					// in the layer.
					myX = box.getNextPossibleX()[fragmentToLayer.get(current)];
				}
				
				// place myself
				box.setBoxXPos(current, myX);
				
				// update the next possible x-position in my layer
				box.getNextPossibleX()[fragmentToLayer.get(current)] = myX
						+ fragWidth.get(current) + fragmentXDistance;

				
				// compute my parents
				for (DefaultEdge edge : getFragInEdges(current)) {
					Fragment par = graph.getSourceFragment(edge);
					if (frags.contains(par) && (!visited.contains(par))) {
						parents.add(par);
					}
				}

			} else {
				// the current fragment belongs to a box, so I have to place
				// this fragment and the other fragments of the box.
				
				// the first fragment is assumed to be the rightmost one, so
				// we treat it separately.
				boolean first = true;

				// check the box fragments from the left to the right
				for (Fragment frag : myBox.getSortedFragments()) {
					if (first) {
						// the first fragment decides about
						// the left border of the box.
						// 'myX' is now the x of my box.
						first = false;
						
						// one-hole fragment: next possible position in layer
						if (oneHoleFrags.contains(frag)) {
							myX = box.getNextPossibleX()[fragmentToLayer
									.get(frag)];
						} else {
							// not a one-hole fragment: the next x-position.
							myX = myBox.getBoxXPos(frag) + myX;
						}
					} 
					

					if (!visited.contains(frag)) {
						visited.add(frag);
						
						// the x value of a box fragment is its
						// relative x-value + the left border of the box.
						int xVal = myBox.getBoxXPos(frag) + myX;
						box.setBoxXPos(frag, xVal);
						
						// updating the next possible x of the box fragment's layer
						box.getNextPossibleX()[fragmentToLayer.get(frag)] = xVal
								+ fragWidth.get(frag) + fragmentXDistance;
					}
					
					// my parents are extended to all parents of my box fragments.
					for (DefaultEdge edge : getFragInEdges(frag)) {
						Fragment par = graph.getSourceFragment(edge);
						if (frags.contains(par) && (!visited.contains(par))
								&& (!myBox.frags.contains(par))) {
							parents.add(par);
						}
					}
				}

			}
			
			
			/* placing my parents */
			
			// the x to start my parent DFS with.
			nextX = box.getBoxXPos(current) + fragmentXDistance
					+ fragWidth.get(current);

			if (getFragHoles(current).size() == 1) {
				// a one-hole-fragment places its parent
				// _always_ flush with itself
				rightX = box.getBoxXPos(current);
			} else {
				// every other fragment places its parent
				// to its right.
				rightX = nextX;
			}

			
			if (parents.size() > 1) {
				// this is to make sure that I start placing my parents which
				// only dominate myself right above me.#
				
				// place the parents first which have fewer outedges.
				Collections.sort(parents, new FragmentOutDegreeComparator());
				for (Fragment par : parents) {
					cross += fragBoxDFS(box, nextX, visited, par, 0,
							currentRoot);
					nextX = box.getBoxXPos(par) + fragWidth.get(par)
							+ fragmentXDistance;
				}

			} else {
				// if I have only one parent, I don't need any sorting.
				for (Fragment par : parents) {
					cross += fragBoxDFS(box, nextX, visited, par, 0,
							currentRoot);
					nextX = box.getBoxXPos(par) + fragWidth.get(par)
							+ fragmentXDistance;
				}
			}

			

			/* place the children and their boxes. */
			
			// the children are placed at the next position to the right
			// of the current fragment.
			nextX = rightX;
			
			// collect the unseen children
			Set<Fragment> childfrags = new HashSet<Fragment>();
			for (DefaultEdge edge : getFragOutEdges(current)) {
				Fragment child = graph.getTargetFragment(edge);
				if (!visited.contains(child)) {
					childfrags.add(child);
				}
			}

			// iterate over the children
			for (Fragment child : childfrags) {
				
				if (!visited.contains(child)) {
					
					// store the parents of the children's boxes, if there are some.
					List<Fragment> childboxparents = new ArrayList<Fragment>();
					
					// checking whether the child is part of a box.
					FragmentBox childbox = box.getBoxForFrag(child);

					if (childbox != null) {

						// the child is in a box;
						// merge the box into my own.
						
						boolean first = true;
						for (Fragment cbf : childbox.getSortedFragments()) {
							
							// determining the left border of the box
							if (first) {
								// the first box fragment marks the left border
								// of the box, it is treated separately.
								first = false;
								
								// one-hole fragments are placed at the next possible position.
								if (oneHoleFrags.contains(cbf)
										&& (!visited.contains(cbf))) {
									nextX = box.getNextPossibleX()[fragmentToLayer
											.get(cbf)];
								}
							}
							
							// placing the fragment
							if (!visited.contains(cbf)) {
								visited.add(cbf);
								int xVal;
								
								// place the fragment at its relative x-position
								// + the left box border -- if possible.
								// place it at the next possible position otherwise.
								xVal = Math.max(childbox.getBoxXPos(cbf)
										+ nextX,
										box.getNextPossibleX()[fragmentToLayer
												.get(cbf)]);
								box.setBoxXPos(cbf, xVal);
								
								// update the next possible x.
								box.getNextPossibleX()[fragmentToLayer.get(cbf)] = xVal
										+ fragWidth.get(cbf)
										+ fragmentXDistance;

							}
							
							// collect the parents of the childbox fragment
							// which are not contained in the childbox
							for (DefaultEdge edge : getFragInEdges(cbf)) {
								Fragment parent = graph.getSourceFragment(edge);
								if (!visited.contains(parent)
										&& frags.contains(parent)
										&& (!childbox.frags.contains(parent))) {
									childboxparents.add(parent);
								}
							}
						}
						nextX = box.getBoxXPos(child) + fragWidth.get(child)
								+ fragmentXDistance;

						// sort the parents according to their number of outedges.
						Collections.sort(childboxparents,
								new FragmentOutDegreeComparator());
						
						// place the parents of the childbox.
						
						for(Fragment boxpar : childboxparents) {
							cross += fragBoxDFS(box, nextX, visited, boxpar, 0,
								currentRoot);
							nextX = box.getNextPossibleX()[fragmentToLayer.get(boxpar)];
						}

					} else {
						// no childbox. this should never happen. However, if it
						// does - just go on with DFS.
						cross += fragBoxDFS(box, nextX, visited, child, 0,
								currentRoot);
						nextX = box.getBoxXPos(child) + fragWidth.get(child)
								+ fragmentXDistance;
					}
				}
			}

			// the width of the box is the position for the next posible box - 
			// the fragment distance.
			box.setWidth(nextX - fragmentXDistance);

		} 
		return cross;
	}

	
	/**
	 * A simple tree layout for a set of fragments.
	 * Every fragment's x is placed in the middle
	 * between the right and the left border of its
	 * children.
	 * The nodes of the box have to form a forrest, otherwise this will fail.
	 * 
	 * @return true if the layout was successfully computed, false otherwise
	 */
	boolean fragBoxTreeLayout(FragmentBox box) {
		Fragment root = getRoot(box);
		if (root != null) {
			fragBoxTreeLayoutDFS(box, root, new HashSet<Fragment>(), 0);
		} else {
			return false;
		}
		return true;
	}

	/**
	 * Determine the root for a fragment box whose nodes form a forrest.
	 * @return the root, null if the nodes are not a forrest.
	 */
	Fragment getRoot(FragmentBox box) {

		Set<Fragment> frags = box.getFrags();
		for (Fragment frag : frags) {
			if (getFragInEdges(frag).size() == 0)
				return frag;

			boolean parent = false;
			for (DefaultEdge edge : getFragInEdges(frag)) {
				Fragment par = graph.getSourceFragment(edge);
				if (frags.contains(par)) {
					parent = true;
					break;
				}
			}
			if (!parent) {
				return frag;
			}
		}
		return null;
	}

	
	/**
	 * Recursive method to compute the tree layout of a FragmentBox.
	 * 
	 * @param box the fragment box to layout
	 * @param current the current fragment
	 * @param visited the visited fragments
	 * @param xStart the current left-hand x
	 * @return the right border if the current fragment.
	 */
	int fragBoxTreeLayoutDFS(FragmentBox box, Fragment current,
			Set<Fragment> visited, int xStart) {

		if (!visited.contains(current)) {
			visited.add(current);
			
			// determine my children
			List<Fragment> childfrags = new ArrayList<Fragment>();
			for (DefaultEdge edge : getFragOutEdges(current)) {
				Fragment child = graph.getTargetFragment(edge);
				if (!visited.contains(child)) {
					childfrags.add(child);
				}
			}

			// the right border of my children is at least
			// as far to the right as my own right border.
			int rightborder = xStart + fragWidth.get(current);
			
			int myX = 0; // my x position
			int leftborder = xStart; // the left border of my children.

			// iterate over my children
			for (Fragment child : childfrags) {
				
				// place them and store their right border
				rightborder = fragBoxTreeLayoutDFS(box, child, visited,
						leftborder);
				
				// the next child is placed to the right hand side of the
				// previous child.
				leftborder = rightborder + fragmentXDistance;
			}
			
			// I'm in the middle of my children
			myX = (xStart + rightborder) / 2 - fragWidth.get(current) / 2;
			
			// if my position is not allowed in my layer, I have to be placed
			// more to the right.
			myX = Math.max(myX, box.getNextPossibleX()[fragmentToLayer.get(current)]);
			
			int rightBorder = myX + fragWidth.get(current);
			box.getNextPossibleX()[fragmentToLayer.get(current)] =
				rightBorder;
			box.setBoxXPos(current, myX);
			return rightBorder;
		}
		
		// if i have been visited before, I simulated a "null" width.
		return xStart;
	}

	/**
	 * Find the fragments which are allowed to be the leftmost fragments in a
	 * box.
	 * 
	 * @return
	 */
	Set<Fragment> getPossibleRoots(FragmentBox box, Set<Fragment> freefragments) {
		Set<Fragment> theroots = new HashSet<Fragment>();

		for (Fragment frag : box.getFrags()) {
			if ( // I have one and only one parent, or my parents are not in
					// the box.
			(getFragInEdges(frag).size() == 1 || freefragments.contains(frag))
					&&
					// AND I have no children
					(getFragOutEdges(frag).size() == 0 || oneHoleFrags
							.contains(frag))) {
				theroots.add(frag);
			}
		}

		return theroots;
	}

	/**
	 * A helper class collecting wccs of fragments and the 
	 * x-positions of the fragments relative to the left border
	 * of the box
	 * 
	 * @author Michaela Regneri
	 * 
	 */
	private class FragmentBox {

		private Map<Fragment, Integer> fragToXPos; // positions within the box
		
		private Map<Set<Fragment>, FragmentBox> children; // boxes contained
															// in this box

		private Set<Fragment> frags; // my fragments

		private int[] nextPossibleX; // the leftmost free position for a
										// fragment in a slot.

		private int width; // my width (after layout)
	
		
		 Set<Fragment> getFrags() {
			return frags;
		}

		 void setFrags(Set<Fragment> frags) {
			this.frags = frags;
		}

		  int[] getNextPossibleX() {
			return nextPossibleX;
		}

		  void setNextPossibleX(int[] nextPossibleX) {
			this.nextPossibleX = nextPossibleX;
		}

		  void setWidth(int width) {
			this.width = width;
		}

		  void putChild(Set<Fragment> parents, FragmentBox chbox) {
			children.put(parents, chbox);
		}

		  void clear() {
			nextPossibleX = new int[fraglayers.size()];
			fragToXPos.clear();
		}

		/**
		 * A fragment box for a wcc of a graph
		 */
		FragmentBox(Set<Fragment> subgraph) {

			fragToXPos = new HashMap<Fragment, Integer>();
			nextPossibleX = new int[fraglayers.size()];
			frags = subgraph;
			children = new HashMap<Set<Fragment>, FragmentBox>();
		}

		/**
		 * Returns the FragmentBox a child of mine belongs to. Returns null if
		 * the fragment is no child of mine.
		 * 
		 * @param frag
		 * @return
		 */
		FragmentBox getBoxForFrag(Fragment frag) {

			for (Set<Fragment> rootSet : children.keySet()) {
				if (rootSet.contains(frag))
					return children.get(rootSet);
			}
			return null;
		}

		/**
		 * 
		 * @return
		 */
		List<Fragment> getSortedFragments() {
			List<Fragment> fraglist = new ArrayList<Fragment>(frags);
			Collections.sort(fraglist, new FragmentXComparator());
			return fraglist;
		}

		/**
		 * 
		 * @author Michaela Regneri
		 * 
		 */
		private class FragmentXComparator implements Comparator<Fragment> {

			/**
			 * 
			 */
			 public int compare(Fragment arg0, Fragment arg1) {
				return fragToXPos.get(arg0) - fragToXPos.get(arg1);
			}

		}

		/**
		 * 
		 * @return
		 */
		int getWidth() {
			return width;
		}

		/**
		 * 
		 * @param frag
		 * @return
		 */
		int getBoxXPos(Fragment frag) {
			return fragToXPos.get(frag);
		}

		void setBoxXPos(Fragment frag, int x) {
			fragToXPos.put(frag, x);
		}

	}

}
