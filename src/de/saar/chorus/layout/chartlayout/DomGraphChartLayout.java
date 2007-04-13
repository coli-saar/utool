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
	private Map<Fragment,Integer> fragXpos;
	private Map<Fragment,Integer> fragYpos;

	private Map<Fragment,Integer> fragWidth;
	private Map<Fragment,Integer> fragHeight;

	private Map<Fragment, String> fragToRoot;
	private Map<String, Fragment> rootToFrag;
	
	
	private List<Set<String>> layers;
	private Map<Fragment, DefaultGraphCell> leaflayer; // a leaf mapped to its parent hole
	private Map<Fragment, Integer> fragmentToLayer;
	private Set<Fragment> oneHoleFrags;
	
	private List<Set<Fragment>> fraglayers;

	//the x-offset of the nodes of a fragment
	private Map<Fragment,Integer> fragOffset;

	
	

	// the position of a node within its fragment
	private Map<DefaultGraphCell,Integer> relXtoParent;
	private Map<DefaultGraphCell,Integer> relXtoRoot;
	private Map<DefaultGraphCell,Integer> relYpos;

	
	private Map<DefaultGraphCell, Shape> nodesToShape;

	// the absolute position of a node in the graph
	private Map<DefaultGraphCell,Integer> xPos;
	private Map<DefaultGraphCell,Integer> yPos;

	private Fragment movedRoot;
	private int yOffset;



	/**
	 * Initializes a new dominance graph layout
	 * of a given dominanc graph.
	 * 
	 * @param gr the graph to compute the layout for
	 */
	public DomGraphChartLayout(JDomGraph gr, Chart chart, DomGraph origin) {
		/*
		 * initializing the graph and its attributes
		 * by getting them from the graph
		 */
		this.graph = gr;
		this.chart = (Chart) chart.clone();
		domgraph = origin;

		fragments = graph.getFragments();

		layers = new ArrayList<Set<String>>();
		fraglayers = new ArrayList<Set<Fragment>>();
		fragmentToLayer = new HashMap<Fragment, Integer>();

		
		movedRoot = null;
		yOffset = 0;
		//all the other fields are initialized empty

		fragXpos = new HashMap<Fragment, Integer>();
		fragYpos = new HashMap<Fragment, Integer>();

		fragWidth = new HashMap<Fragment, Integer>();
		fragHeight = new HashMap<Fragment, Integer>();

		fragOffset = new HashMap<Fragment, Integer>();


		oneHoleFrags = new HashSet<Fragment>();
		
		relXtoParent = new HashMap<DefaultGraphCell,Integer>();
		relYpos = new HashMap<DefaultGraphCell,Integer>();

		xPos = new HashMap<DefaultGraphCell,Integer>();
		yPos = new HashMap<DefaultGraphCell,Integer>();

		nodesToShape = new HashMap<DefaultGraphCell, Shape>();
		relXtoRoot = new HashMap<DefaultGraphCell, Integer>();
		
		fragToRoot = new HashMap<Fragment, String>();
		rootToFrag = new HashMap<String, Fragment>();
		
		for(Fragment frag : fragments) {
			String root =
				graph.getNodeData(getFragRoot(frag)).getName();
			fragToRoot.put(frag,root);
			rootToFrag.put(root, frag);
		}
		
		leaflayer = new HashMap<Fragment, DefaultGraphCell>();
	}





	private void fillLayers() {

		List<Set<String>> toplevel = new ArrayList<Set<String>>(chart.getToplevelSubgraphs());
		Set<String> roots = domgraph.getAllRoots();
		
		for(Set<String> tls : toplevel ) {
			if(chart.containsSplitFor(tls)) {
				fillLayer(0, new ArrayList<Split>(chart.getSplitsFor(tls)), new HashSet<String>());
			} else {
				Set<String> leaves = new HashSet<String>(tls);
				addToLayer(leaves, 0);
			}
		}
		
		
		for(int i = 0; i < layers.size(); i++ ) {

		
			Set<String> layer = new HashSet<String>(layers.get(i));
			layer.retainAll(roots);
			fraglayers.add(i, new HashSet<Fragment>());

			for(String node : layer) {
				Fragment frag = rootToFrag.get(node);
				
				if(getFragDegree(frag) == 1 && getFragOutEdges(frag).size() == 0) {
					DefaultGraphCell parent = graph.getSourceNode(
							getFragInEdges(frag).iterator().next());
					if(oneHoleFrags.contains(graph.findFragment(parent))) {
						leaflayer.put(frag,parent);
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

		for(Split split : splits) {
			String root = split.getRootFragment();
			if(! visited.contains(root)) {
				visited.add((root));
				recent.add(root);
				recent.addAll(new HashSet<String>(split.getAllDominators()));
				remainingSubgraphs.addAll( new ArrayList<Set<String>>(split.getAllSubgraphs()) ) ;
			}

		}

		addToLayer(recent, layer);




		for(Set<String> subgraph : remainingSubgraphs) {
			Set<String> sgc = new HashSet<String>(subgraph);
			sgc.removeAll(recent);	
			for(Set<String> wccs : domgraph.wccs(sgc)) {
				List<Split> newSplits = chart.getSplitsFor(wccs);
				if(newSplits != null) {

					fillLayer(nextLayer, new ArrayList<Split>(newSplits), visited);
				} else {
					Set<String> leaffrags = new HashSet<String>(wccs);
					
					addToLayer(leaffrags,nextLayer);
					visited.add(wccs.iterator().next());
				}

			}
		}


	}

	private void addToLayer(Set<String> recent, int ind) {
		if(layers.size() <= ind) {
			layers.add(ind, recent);

		} else {
			layers.get(ind).addAll(recent);
		}
	}




	/**
	 * Generic method that handles maps from an Object to 
	 * a list of objects and ads a new entry to the value list with
	 * the specified object key. If the map does not contain the
	 * key yet, it is added.
	 * @param <E> the key type
	 * @param <T> the type of the list elements
	 * @param map the map
	 * @param key the key to which list the new value shall be added
	 * @param nVal the new value
	 */
	public static <E,T> void addToMapList(Map<E,List<T>> map, E key, T nVal) {
		List<T> typedList;
		if(map.containsKey(key)) {
			typedList = map.get(key);
		} else {
			typedList = new ArrayList<T>();
			map.put(key,typedList);
		}
		typedList.add(nVal);
	}


	/**
	 * Perform DFS in a fragment to resolve its leave nodes in
	 * the right order.
	 * 
	 * @param frag the fragment to get the leaves from
	 * @param root the recent visited node
	 * @param leaves the leaves resolved yet
	 */
	private void fragLeafDFS(Fragment frag, DefaultGraphCell root,
			List<DefaultGraphCell> leaves) {

		//if we found a leaf, we add it tot the list.
		if(frag.isLeaf(root)) {
			leaves.add(root);
		} else {
			/*
			 * if the node ist no (fragment-) leave, there have
			 * to be some children.
			 * DFS is performed for each child contained in the
			 * fragment.
			 */
			for(int i = 0; i< graph.getChildren(root).size(); i++) {
				if(frag.getNodes().contains(graph.getChildren(root).get(i)))
					fragLeafDFS(frag, graph.getChildren(root).get(i), leaves);
			}
		}
	}

	private void computeOneHoleFrags() {
		for(Fragment frag : fragments) {
			int childwidth = isOneHoleFrag(frag);
			if(childwidth > -1) {
				oneHoleFrags.add(frag);
				// 15 is half a hole.
				int newWidth = fragWidth.get(frag) + (childwidth/2) - 15;
				fragWidth.remove(frag);
				fragWidth.put(frag, newWidth);
			}
		}
	}
	
	/**
	 * Resolve all leaves of a fragment (in the
	 * right order).
	 *  
	 * @param frag the fragment to get the leaves for
	 * @return the list of leaves.
	 */
	private List<DefaultGraphCell> getFragLeaves(Fragment frag) {

		//initializing the leave list
		List<DefaultGraphCell> leaves = new ArrayList<DefaultGraphCell>();

		//performing DFS to fill the leave list.
		fragLeafDFS(frag, getFragRoot(frag), leaves);

		return leaves;
	}

	/**
	 * Resolving the holes of a fragment in the right order.
	 * This does not the same job as <code>getFragLeaves</code>,
	 * because leaves that are roots at the same time have to be
	 * excluded.
	 * 
	 * @param frag the fragment to get the holes from
	 * @return the list of holes; an empty list if there are none.
	 */
	private List<DefaultGraphCell> getFragHoles(Fragment frag) {

		List<DefaultGraphCell> holes = new ArrayList<DefaultGraphCell>();

		for( DefaultGraphCell leaf : getFragLeaves(frag) ) {
			if(graph.getNodeData(leaf).getType().equals(NodeType.unlabelled)) {
				holes.add(leaf);
			}
		}

		return holes;

	}

	/**
	 * Compute the incoming edges of a fragment.
	 * 
	 * @param frag the fragment to compute the in-edges for
	 * @return the sorted list of incoming edges.
	 */
	private List<DefaultEdge> getFragInEdges(Fragment frag) {
		
		
		
		List<DefaultEdge> inedges = new ArrayList<DefaultEdge>();
		for(DefaultGraphCell node : frag.getNodes()) {
			for(DefaultEdge edge :  graph.getInEdges(node)) {
				if(graph.getEdgeData(edge).getType() == EdgeType.dominance) {
					inedges.add(edge);
				}
			}
		}
		
		
		return inedges;
	}
	
	/**
	 * Compute the outgoing edges of a fragment.
	 *  
	 * @param frag the fragment to compute the in-edges for
	 * @return the sorted list of outgoing edges.
	 */
	private List<DefaultEdge> getFragOutEdges(Fragment frag) {
		
		List<DefaultEdge> outEdges = new ArrayList<DefaultEdge>();
		
		
		/*
		 * the outgoing edges are the edges going out
		 * from the fragment's holes.
		 * The holes are saved in the right order and
		 * getOutEdges(DefaulGraphCell) returnes a 
		 * sorted list of edges.
		 * So the list of out-edges is resolved by
		 * computing the out-edges of a hole and appending
		 * the out-edges of the next hole(s).
		 */
		for(DefaultGraphCell hole : getFragHoles(frag))  {
			outEdges.addAll(graph.getOutEdges(hole));
		}
		
		for(DefaultGraphCell node : frag.getNodes()) {
			
			for(DefaultEdge edge :  graph.getOutEdges(node)) {
				if(graph.getEdgeData(edge).getType() == EdgeType.dominance
						&& (! outEdges.contains(edge))) {
					outEdges.add(edge);
				}
			}
		}
		
		return outEdges;
		
	}

	

	/**
	 * Resolving the number of dominance edges adjacent
	 * to a fragment.
	 * 
	 * @param frag the fragment to compute the degree for
	 * @return the fragment degree (considering the fragment graph)
	 */
	int getFragDegree(Fragment frag) {

		//adding indegree and outdegree.
		return getFragInEdges(frag).size() + 
		getFragOutEdges(frag).size();
	}


	/**
	 * computes the tree layout for each fragment.
	 *
	 */
	private void computeFragmentLayouts() {

		// iterating over the fragments
		for( Fragment frag : fragments ) {

			// the recent root
			DefaultGraphCell root = getFragRoot(frag);
			// computing the x-positions, dependent on the _direct_
			// parent

			
			GraphLayoutCursor layCursor = new GraphLayoutCursor(root, this, graph, frag.getNodes());
			PostOrderNodeVisitor postVisitor = new PostOrderNodeVisitor(layCursor);
			postVisitor.run();

			// another DFS computes the y- and x-positions relativ to the
			// _root_
			GraphDrawingCursor drawCursor = new GraphDrawingCursor(root, this, graph, frag.getNodes());
			PreOrderNodeVisitor preVisitor = new PreOrderNodeVisitor(drawCursor);
			preVisitor.run();


		}

	}

	/**
	 * computes the height of a fragment depndent
	 * on its maximal depth.
	 * node: this method is not meaningful before
	 * having performed computeXandDepth.
	 * 
	 * @param frag the fragment
	 */
	private int computeFragHeight(Fragment frag) {

		Shape box = nodesToShape.get(getFragRoot(frag));

		//the height is the maximal depth* (node + distance) -
		//the height of the last node (depth starts at 1 at
		//shape)
		return box.depth()*(30+ nodeYDistance) - nodeYDistance;
	}

	/**
	 * computes the width of all fragments dependent 
	 * on the number of their leaves.
	 */
	private int computeFragWidth(Fragment frag) {

		DefaultGraphCell fragRoot = getFragRoot(frag);
		Shape box = nodesToShape.get(fragRoot);
		BoundingBox bb = box.getBoundingBox();
		int extL = bb.left;
		int extR = bb.right;



		int offset = 0 - extL;
		fragOffset.put(frag, offset);

		return extR - extL;
	}

	public void putNodeToShape(DefaultGraphCell node, Shape shape) {
		nodesToShape.put(node,shape);
	}

	/**
	 * computes the dimensions of all fragments using
	 * computeFragHeight and computeFragWidth.
	 * Before computing a fragment's height, 
	 * computeXandDepth is called.
	 *
	 */
	private void computeFragDimensions() {
		computeFragmentLayouts();
		for(Fragment frag : graph.getFragments() ) {

			fragHeight.put(frag, new Integer(computeFragHeight(frag)));
			fragWidth.put(frag, new Integer(computeFragWidth(frag)));
		}
	}


	private  int isOneHoleFrag(Fragment frag) {
		List<DefaultGraphCell> holes = getFragHoles(frag);
		if(holes.size() == 1) {
			DefaultGraphCell hole = holes.iterator().next();
			List<DefaultEdge> outedges = graph.getOutEdges(hole);
			if(outedges.size() == 1) {
				Fragment child = graph.getTargetFragment(outedges.iterator().next());
				if(getFragDegree(child) == 1) {
					return fragWidth.get(child);
				} else {
					return graph.computeNodeWidth(hole);
				}
		}} else if(holes.size() == 2){
			boolean first = true;
			for(DefaultGraphCell hole : holes) {
				if(first) {
					first = false;
					List<DefaultEdge> outedges = graph.getOutEdges(hole);
					if(outedges.size() == 1) {
						Fragment child = graph.getTargetFragment(outedges.iterator().next());
						if(getFragDegree(child) == 1) {
							return fragWidth.get(child);
						} else {
							return -1;
						}
					}
				} 
			}
		}
		
		return -1;
	}



	
	/**
	 * computes the whole fragment graph.
	 * computes the fragment's x-position with undirected DFS,
	 * the fragment's later y-position performing directed DFS 
	 * (for each root).
	 */
	private void computeFragmentPositions() {

		int yFragPos = 0;
		int yPosToAdd = 0;
		int noOfBiggestLayer = 0;
		int widthOfBiggestLayer = 0;
		Set<Fragment> visited = new HashSet<Fragment>();
		List<Set<Fragment>> roots = new ArrayList<Set<Fragment>>();

		for(int i = 0; i < fraglayers.size(); i++ ) {

			Set<Fragment> layer = fraglayers.get(i);

			int layerWidth = 0;
			roots.add(i, new HashSet<Fragment>()); 

			for(Fragment frag : layer) {
				if(!visited.contains(frag)) {
					visited.add(frag);
					if(getFragInEdges(frag).size() < 2 && getFragOutEdges(frag).size() < 2) {
						roots.get(i).add(frag);
					}

					layerWidth += fragWidth.get(frag) + DomGraphLayoutParameters.fragmentXDistance;
				}
			}
			layerWidth -= DomGraphLayoutParameters.fragmentXDistance;
			widthOfBiggestLayer = Math.max(layerWidth, widthOfBiggestLayer);
			noOfBiggestLayer = Math.max(fraglayers.get(i).size(), noOfBiggestLayer);
		}


		visited.clear();
		
		
		for(int i = 0; i < fraglayers.size(); i++) {
			Set<Fragment> layer = fraglayers.get(i);
	//		int x0;

			
			for(Fragment frag : layer) {



				if(! visited.contains(frag)) {
					visited.add(frag);
					fragYpos.put(frag, yFragPos);
					
				
					yPosToAdd = Math.max(fragHeight.get(frag), yPosToAdd);

				/*	if(x0 == 0) {
						fragXpos.put(frag,x0);
					} else {
						fragXpos.put(frag, x0 - (fragWidth.get(frag)/2));
					}
					x0 += deltax + (fragWidth.get(frag)/2);*/

				}	
			}
			yFragPos += yPosToAdd + DomGraphLayoutParameters.fragmentYDistance;
		}

		
		int leftBorder = 0;
		int xoffset = 0;
		
		Fragment topFragment = null;
		if(fraglayers.get(0).size() == 1) {
			topFragment = fraglayers.get(0).iterator().next();
		}
		for(Set<String> toplevel : chart.getToplevelSubgraphs()) {
			
			Set<String> free = new HashSet<String>();
			if(chart.containsSplitFor(toplevel)) {
			for(Split split : chart.getSplitsFor(toplevel)) {
				free.addAll(domgraph.getFragment(split.getRootFragment()));
			}
			} else {
				free.addAll(toplevel);
				free.retainAll(roots);
			}
			FragmentBox box = new FragmentBox(toplevel, free);
			
			for(Fragment boxfrag : box.frags) {
				int x = box.getBoxXPos(boxfrag) + xoffset;
				fragXpos.put(boxfrag, x);
				leftBorder = Math.max(leftBorder, x + fragWidth.get(boxfrag));
			}
			xoffset = leftBorder + fragmentXDistance;
		}
		
		if(topFragment != null) {
			fragXpos.remove(topFragment);
			fragXpos.put(topFragment, leftBorder/2 - fragWidth.get(topFragment)/2);
			for (DefaultEdge edge : getFragOutEdges(topFragment)) {
				GraphConstants.setLineColor(graph.getModel().getAttributes(edge), new Color(255,204,230));
			}
		}
		
	}

		private void moveGraph(int x) {
			Set<Fragment> seen = new HashSet<Fragment>();
			for(DefaultGraphCell node : graph.getNodes()) {

				
				if(xPos.containsKey(node)) {
					int old = xPos.get(node);
					xPos.remove(node);
					xPos.put(node, old +x);
				} else {
					Fragment frag = graph.findFragment(node);
					if(! seen.contains(frag) ) {
						seen.add(frag);
						int old = fragOffset.get(frag);
						fragOffset.remove(frag);
						fragOffset.put(frag, old + x);
					}
				}
			}
		}
	
	/**
	 * computes the position of all nodes considering
	 * their relative poitions within a fragment and the
	 * position of their fragment (cp. its fragment node).
	 */
	private void computeNodePositions() {
		
		int movegraph = 0;
		for(DefaultGraphCell node : graph.getNodes() ) {

			Fragment nodeFrag = graph.findFragment(node);

			int x = relXtoRoot.get(node); 
			int offset = fragOffset.get(nodeFrag) - graph.computeNodeWidth(node)/2;
			int xMovement;
			
			if(leaflayer.containsKey(nodeFrag)) {
				DefaultGraphCell sourceHole = leaflayer.get(nodeFrag);
				Fragment parent = graph.findFragment(sourceHole);
				
					xMovement = relXtoParent.get(sourceHole)
					- graph.computeNodeWidth(node)/2
					+ fragOffset.get(parent)
					+ fragXpos.get(parent);

			} else {
				xMovement= fragXpos.get(nodeFrag) + offset;
			}	
			/*
			 * the absolute x- position is the relative
			 * position added to the fragment's x-position
			 */
			x += xMovement ;

			xPos.put(node, x);

			movegraph = Math.min(movegraph, x);

			/*
			 * the absolute y- position is the relative
			 * position added to the fragment's y-position
			 * and decreased by the minimal y-position (that
			 * moves the graph down to let it start by 0). 
			 */


			int y = relYpos.get(node);
			
			int yMovement;
			
			
			if(leaflayer.containsKey(nodeFrag)) {
				DefaultGraphCell sourceHole = leaflayer.get(nodeFrag);
				Fragment parent = graph.findFragment(sourceHole);
				
				yMovement = fragYpos.get(parent) +
							fragHeight.get(parent) +
							(fragmentYDistance/3);
			} else {
				yMovement = fragYpos.get(nodeFrag);
			}
			
			
			y += yMovement;

			if(yOffset> 0 && (! nodeFrag.equals(movedRoot)))
				y += yOffset;

			yPos.put(node,y);


		}
		moveGraph(movegraph * -1);
	}

	/**
	 * computes the root of a fragment
	 * @param frag the fragment
	 * @return the root
	 */
	private DefaultGraphCell getFragRoot(Fragment frag) {

		//starting the search with a node contained
		//in the fragment
		DefaultGraphCell root = (DefaultGraphCell) 
		frag.getNodes().toArray()[0];

		/*
		 * (Fragment).getParent(DefaultGraphCell)
		 * returns null, if the node to check is the
		 * root (or not contained in the fragment, 
		 * we avoided that before).
		 * Otherwise we get a new (parent-)node to check.
		 */
		while(! (frag.getParent(root) == null)) {
			root = frag.getParent(root);
		}

		return root;
	}

	/**
	 * places the nodes in the graph model.
	 * Not meaningful without having computed
	 * the fragment graph as well as the relative
	 * x- and y-positions.
	 */
	private void placeNodes() {
		//the view map to save all the node's positions.
		Map<DefaultGraphCell,AttributeMap> viewMap = new 
		HashMap<DefaultGraphCell,AttributeMap>();


		//place every node on its position
		//and remembering that in the viewMap.
		for(DefaultGraphCell node : graph.getNodes() ) {
			int x = xPos.get(node).intValue();
			int y = yPos.get(node).intValue();

			placeNodeAt(node, x, y, viewMap);
		}


		//updating the graph.
		graph.getGraphLayoutCache().edit(viewMap, null, null, null);
	}


	/**
	 * places a node at a given position and remembers
	 * the information in a given Attribute Map.
	 * @param node, the node to place
	 * @param x the x-value of the upper left corner
	 * @param y the y-value of the upper left corner
	 * @param viewMap hte viewMap to save the position in
	 */
	private void placeNodeAt(DefaultGraphCell node, int x, int y, 
			Map<DefaultGraphCell,AttributeMap> viewMap) {

		CellView view = graph.getGraphLayoutCache().getMapping(node, false);
		Rectangle2D rect = (Rectangle2D) view.getBounds().clone();
		Rectangle bounds =  new Rectangle((int) rect.getX(),
				(int) rect.getY(), (int) rect.getWidth(), (int) rect.getHeight());

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
		relXtoParent.put(node,x);
	}

	public void addRelYpos(DefaultGraphCell node, Integer y) {
		relYpos.put(node,y);
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
		relXtoRoot.put(node,x);
	}

	Set<Fragment> convertStringsToFragments(Collection<String> nodes) {
		Set<Fragment> ret = new HashSet<Fragment>();
		Set<String> roots = new HashSet<String>(nodes);
		roots.retainAll(domgraph.getAllRoots());
		
		for(String root : roots) {
			ret.add(rootToFrag.get(root));
		}
		return ret;
	}
	
	
	/**
	 * @return Returns the relXtoRoot.
	 */
	public Map<DefaultGraphCell, Integer> getRelXtoRoot() {
		return relXtoRoot;
	}

	
	public class FragmentOutDegreeComparator implements Comparator<Fragment> {

		public int compare(Fragment arg0, Fragment arg1) {
			if(oneHoleFrags.contains(arg0) && oneHoleFrags.contains(arg1)) {
				return getFragOutEdges(arg0).size() - getFragOutEdges(arg1).size();
			} else if(oneHoleFrags.contains(arg0)) {
				return -1;
			} else if(oneHoleFrags.contains(arg1)) {
				return 1;
			} else return getFragOutEdges(arg0).size() - getFragOutEdges(arg1).size();
		}
		
	}
	
	public class FragmentInDegreeComparator implements Comparator<Fragment> {
		
		public int compare(Fragment arg0, Fragment arg1) {
			return getFragInEdges(arg0).size() - getFragInEdges(arg1).size();
		}
		
	}
	
	
	
	/**
	 * relative to their box.
	 * 
	 * @author Michaela Regneri
	 *
	 */
	private class FragmentBox {

		private Map<Fragment, Integer> fragToXPos; //positions within the box
		private Map<Set<Fragment>, FragmentBox> children; // boxes contained in this box

		private Set<String> fragmentsString; // string representation of my fragment's nodes
		private Set<Fragment> frags; // my fragments
		private Set<String> freefragments; // the free fragments contained in the wcc I consist of 
		private int[] nextPossibleX; // the leftmost free position for a fragment in a slot.
		
		private int width; // my width (after layout)

		/**
		 * A fragment box for a wcc of a graph
		 * @param wcc
		 * @param freefrags
		 */
		FragmentBox( Set<String> wcc, Set<String> freefrags ) {

			fragmentsString = new HashSet<String>(wcc);	
			frags = convertStringsToFragments(fragmentsString);
			freefragments = freefrags;
			fragToXPos = new HashMap<Fragment, Integer>();
			nextPossibleX = new int[fraglayers.size()];

			children = new HashMap<Set<Fragment>, FragmentBox>();

			Set<String> childnodes = new HashSet<String>(wcc);
			childnodes.removeAll(freefragments);
			
			Set<DefaultGraphCell> mySubgraph = new HashSet<DefaultGraphCell>();
			for(Fragment frag : frags) {
				mySubgraph.addAll(frag.getNodes());
			}
			
			if(graph.isForest(mySubgraph)) {
				if(! fragBoxTreeLayout()) {
					System.err.println(":'(");
				}
			} else {

			
			// asking the graph for the wccs I get for my fragments without
			// the free fragments.
			for(Set<String> childwcc : domgraph.wccs(childnodes)) {

				Set<String> freeChildFrag = new HashSet<String>();
			
				// either the wcc is another subgraph in the chart...
				if(chart.containsSplitFor(childwcc)) {

					// then I record all the free fragments in there and
					// pass them on
					for(Split chspl : chart.getSplitsFor(childwcc)) {
						freeChildFrag.addAll(domgraph.getFragment(chspl.getRootFragment()));
					}
				} else {
					// if the wcc is not in the chart, it is a leaf.
					// the free fragments of my child are all fragments in the child subgraph.
				//	System.out.println("||| >>>>>  No split found.");
					freeChildFrag.addAll(childwcc);
				}

				Set<Fragment> childroots = convertStringsToFragments(freeChildFrag);

				// a new childbox
				children.put(childroots, new FragmentBox(childwcc, freeChildFrag));
			}

			
			//	frags.removeAll(leaflayer.keySet());

			
			//System.out.println("||| >>> children: " + children);
			// layout

			if(frags.size() > 1) {
				// all the possible roots computed

				
					Set<Fragment> possibleRoots = getPossibleRoots();
					if(possibleRoots.isEmpty()) {
						possibleRoots.addAll(frags);
					}

					// initialising the best root and the number of crossings.
					Fragment bestRoot = possibleRoots.iterator().next();
					int bestCost = -1;
					

					for(Fragment frag : possibleRoots ) {
						if(bestCost == -1 ) {
							// first loop
							bestCost = fragBoxDFS(0, new HashSet<Fragment>(), frag, 0, null, new ArrayList<Fragment>());
							bestRoot = frag;

						} else {
							int nextCrossCount = fragBoxDFS(0, new HashSet<Fragment>(), frag, 0, null, new ArrayList<Fragment>());
							if(nextCrossCount < bestCost) {
								bestCost = nextCrossCount;
								bestRoot = frag;
							}
						}

						nextPossibleX = new int[fraglayers.size()];
						fragToXPos.clear();
					}
					fragBoxDFS(0, new HashSet<Fragment>(), bestRoot, 0, null, new ArrayList<Fragment>());
				
			} else {
				if(! frags.isEmpty())
					fragToXPos.put(frags.iterator().next(), 0);
			}

			
			}

		}
		
		/**
		 * Returns the FragmentBox a child of mine belongs to.
		 * Returns null if the fragment is no child of mine.
		 * @param frag
		 * @return
		 */
		FragmentBox getBoxForFrag(Fragment frag) {
			
			for(Set<Fragment> rootSet : children.keySet()) {
				if (rootSet.contains(frag))
					return children.get(rootSet);
			}
			return null;
		}
		
		/**
		 * 
		 * @return
		 */
		boolean fragBoxTreeLayout() {
			Fragment root = getRoot();
			if(root != null) {
				fragBoxTreeLayoutDFS(root, new HashSet<Fragment>(), 0);
			} else {
				return false;
			}
			return true;
		}
		
		/**
		 * 
		 * @return
		 */
		Fragment getRoot() {
			
			
			for(Fragment frag : frags) {
				if(getFragInEdges(frag).size() == 0)
					return frag;
				
				boolean parent = false;
				for(DefaultEdge edge : getFragInEdges(frag)) {
					Fragment par = graph.getSourceFragment(edge);
					if(frags.contains(par) ) {
						parent = true;
						break;
					}
				}
				if(! parent) {
					return frag;
				}
			}
			return null;
		}
		
		/**
		 * 
		 * @param current
		 * @param visited
		 * @param xStart
		 * @return
		 */
		int fragBoxTreeLayoutDFS(Fragment current, Set<Fragment> visited, int xStart) {

			if(!visited.contains(current)) {
				visited.add(current);
				List<Fragment> childfrags = new ArrayList<Fragment>();
				for(DefaultEdge edge : getFragOutEdges(current)) {
					Fragment child = graph.getTargetFragment(edge);
					if(! visited.contains(child)) {
						childfrags.add(child);
					}
				}
				
				

				int rightborder = xStart + fragWidth.get(current); 
				int myX = 0;
				int leftborder = xStart;

				for(Fragment child : childfrags) {
					rightborder = fragBoxTreeLayoutDFS(child, visited, leftborder);
					leftborder = rightborder + fragmentXDistance;
				}
				myX = (xStart + rightborder)/2 - fragWidth.get(current)/2;
				fragToXPos.put(current, myX);
				return myX + fragWidth.get(current) ;
			}	
			return xStart;
		}
		
		/**
		 * The draft for the method computing the final layout.
		 * It's meant to return the number of crossings counted. And to fill the
		 * map storing the x positions of the fragments.
		 * It doesn't do anything of these things yet. 
		 * @param x
		 * @param visited
		 * @param current
		 * @param width
		 * @return the number of crossings
		 */
		int fragBoxDFS(int x, Set<Fragment> visited, Fragment current, int crossings,
				DefaultGraphCell lastroot, List<Fragment> parentsToCover) {

			int cross = crossings;
			int nextX = x;
			int myX = x;
			int rightX = x;
			DefaultGraphCell currentRoot = getFragRoot(current);

			List<Fragment> parents = new ArrayList<Fragment>();

			

			
			// 0. Compute the hole by which we entered; if it's not the left one,
			//    assume an edge crossing

			if( lastroot != null ) {
				boolean lefthole = true; // the first hole is the left hole.
				
				for(DefaultGraphCell hole : getFragHoles(current)) {
					for( DefaultEdge outedge : graph.getOutEdges(hole) ) {
						if( lastroot.equals(graph.getTargetNode(outedge)) ) {
							if(! lefthole) {
								cross++; // if the last fragment was not the child of the left hole...
							}
						}
					}
					if(lefthole) {
						lefthole = false;
					}
				}
			}
			
			
			if(! visited.contains(current)) {
//				System.err.print("Unseen frag: " + current);
			/*
			 * Placing the recent fragment.
			 * It can be the case that I belong actually to a childbox of this box.
			 */
			FragmentBox myBox = getBoxForFrag(current);
				// this is probably the case iff 
				// I am a 'single'
			if(myBox == null) {
				visited.add(current);
				if(! oneHoleFrags.contains(current)) {
				myX = Math.max(x, nextPossibleX[fragmentToLayer.get(current)] );
				} else {
					myX = nextPossibleX[fragmentToLayer.get(current)] ;
				}
				fragToXPos.put(current, myX);
				nextPossibleX[fragmentToLayer.get(current)]  = myX + fragWidth.get(current) + fragmentXDistance;
				
			//	System.err.println("  -> no box, putting myself at " + myX + ", start was " + x);
				for(DefaultEdge edge : getFragInEdges(current)) {
					Fragment par = graph.getSourceFragment(edge);
					if(frags.contains(par) && (! visited.contains(par))) {
						parents.add(par);
					}
				}
				

			} else {
	//			System.err.print("Box! Starting at " + x);
				boolean first = true;
				
				for(Fragment frag : myBox.getSortedFragments()) {
					if(first) {
						first = false;
						if(oneHoleFrags.contains(frag)) {
							myX = nextPossibleX[fragmentToLayer.get(frag)];
						} else {
							myX = myBox.getBoxXPos(frag) + myX;
						} 
					} else {
						
					}
					
					
					if(!visited.contains(frag)) {
					visited.add(frag);
	//				System.err.print("    boxfrag: " + frag);
					int xVal;
				//	if(oneHoleFrags.contains(frag)) {
					//	xVal = nextPossibleX[fragmentToLayer.get(frag)];
				//	} else {
						xVal = myBox.getBoxXPos(frag) + myX;
				//	} 
					fragToXPos.put(frag, xVal);
	//				System.err.println("  put at " + xVal + "next possible was " + nextPossibleX[fragmentToLayer.get(frag)]);
					nextPossibleX[fragmentToLayer.get(frag)] = xVal + fragWidth.get(frag) + fragmentXDistance;
					}
					for(DefaultEdge edge : getFragInEdges(frag)) {
						Fragment par = graph.getSourceFragment(edge);
						if(frags.contains(par) && (! visited.contains(par)) && (! myBox.frags.contains(par))) {
							parents.add(par);
						}
					}
				}
				
			}
			nextX = fragToXPos.get(current) + fragmentXDistance + fragWidth.get(current);
			
			if(getFragHoles(current).size() == 1) {
				rightX = fragToXPos.get(current);
			} else {
				rightX = nextX;
			}
			
			
			if(parents.size() > 1) {
				// this is to make sure that I start placing my parents which only 
				// dominate myself right above me.
				Collections.sort(parents, new FragmentOutDegreeComparator());
				for(Fragment par : parents) {
	//				System.err.println("parent: " + par);
					cross += fragBoxDFS(nextX, visited, par, 0, currentRoot, new ArrayList<Fragment>());
					nextX = fragToXPos.get(par) +  fragWidth.get(par) + fragmentXDistance;
				}
				
			} else {
				for(Fragment par : parents) {
					cross += fragBoxDFS(nextX, visited, par, 0, currentRoot, new ArrayList<Fragment>());
					nextX =  fragToXPos.get(par) + fragWidth.get(par) + fragmentXDistance;
				}
			}
			
			nextX = rightX;
			

			// 2. compute my unseen children.
			Set<Fragment> childfrags = new HashSet<Fragment>();
			for(DefaultEdge edge : getFragOutEdges(current)) {
				Fragment child = graph.getTargetFragment(edge);
				if(! visited.contains(child)) {
					childfrags.add(child);
				}
			}

			

	//		System.err.print("Children! starting at rightX = " + rightX);
			
			for(Fragment child : childfrags) {
	//			System.err.println("   child: " + child);
				if(!visited.contains(child)) {
				List<Fragment> childboxparents = new ArrayList<Fragment>();
				FragmentBox childbox = getBoxForFrag(child);
				
				
				// merging the box of my child, if there is one.
				if(childbox != null) {
				
					boolean first = true;
					for(Fragment cbf : childbox.getSortedFragments()) {
						if(first) {
							if(!visited.contains(cbf)) {
							}
							first = false;
								if(oneHoleFrags.contains(cbf) && (! visited.contains(cbf))) {
									nextX = nextPossibleX[fragmentToLayer.get(cbf)];
								}
						}
						if(! visited.contains(cbf)) {
						visited.add(cbf);
	//					System.err.println("    boxfrag: " + cbf);
						int xVal;
				//		if(oneHoleFrags.contains(cbf)) {
					//		xVal = nextPossibleX[fragmentToLayer.get(cbf)];
						//} else {
							xVal = Math.max(childbox.getBoxXPos(cbf) + nextX, nextPossibleX[fragmentToLayer.get(cbf)]);
						//}
						fragToXPos.put(cbf, xVal);
	//					System.err.println("putting at " + xVal + ", nextX + chboxX was " + (childbox.getBoxXPos(cbf) + nextX));
						nextPossibleX[fragmentToLayer.get(cbf)] = xVal + fragWidth.get(cbf) + fragmentXDistance;
						
						}
						for(DefaultEdge edge : getFragInEdges(cbf)) {
							Fragment parent = graph.getSourceFragment(edge);
							if(! visited.contains(parent) && frags.contains(parent) && 
									(! childbox.frags.contains(parent))) {
								childboxparents.add(parent);
							}
						}
					}
					nextX = fragToXPos.get(child) + fragWidth.get(child) + fragmentXDistance;
			
					
					Collections.sort(childboxparents, new FragmentOutDegreeComparator());
					cross += fragBoxDFS(nextX, visited, child, 0, currentRoot, childboxparents);
					
					
				} else {
					// no childbox. this should never happen. However, if it does - 
					// just go on.
					cross += fragBoxDFS(nextX, visited, child, 0, currentRoot, new ArrayList<Fragment>());
					nextX = fragToXPos.get(child) + fragWidth.get(child) + fragmentXDistance;
				}
				}
			}
			
		



				width = nextX - fragmentXDistance;

			} else {
				for(Fragment frag : parentsToCover) {
					cross += fragBoxDFS(nextX, visited, frag, 0, currentRoot, new ArrayList<Fragment>());
					nextX = nextPossibleX[fragmentToLayer.get(frag)];
				}
			}

			return cross;
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
		 * Find the fragments which are allowed to be the leftmost fragments
		 * in a box. 
		 * 
		 * @return
		 */
		Set<Fragment> getPossibleRoots() {
			Set<Fragment> theroots = new HashSet<Fragment>();

			for(Fragment frag : frags) {
				if( // I have one and only one parent, or my parents are not in the box.
						(getFragInEdges(frag).size() == 1 || convertStringsToFragments(freefragments).contains(frag)) &&
						// AND I have no children
						(getFragOutEdges(frag).size() == 0 || oneHoleFrags.contains(frag)) ) {
					theroots.add(frag);
				}
			}

			return theroots;
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

	}


}



