package de.saar.chorus.ubench;



import static de.saar.chorus.ubench.DomGraphLayoutParameters.fragmentXDistance;
import static de.saar.chorus.ubench.DomGraphLayoutParameters.fragmentYDistance;
import static de.saar.chorus.ubench.DomGraphLayoutParameters.nodeYDistance;
import static de.saar.chorus.ubench.DomGraphLayoutParameters.towerXDistance;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.math.BigDecimal;
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
import org.jgraph.util.JGraphUtilities;

import de.saar.chorus.domgraph.chart.Chart;
import de.saar.chorus.domgraph.chart.Split;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.jgraph.GraphDrawingCursor;
import de.saar.chorus.jgraph.GraphLayoutCursor;
import de.saar.chorus.jgraph.ImprovedJGraphLayout;
import de.saar.chorus.treelayout.BoundingBox;
import de.saar.chorus.treelayout.PostOrderNodeVisitor;
import de.saar.chorus.treelayout.PreOrderNodeVisitor;
import de.saar.chorus.treelayout.Shape;



/**
 * This is a draft for a new chart-based layout algorithm.
 * 
 * TODO
 *  - implement FragmentBox class
 *  - do DFS for determining the best root (on the left) within a FragmentBox
 *  - merge boxes by adding the fragments of the child-box and move them to the place 
 *    computed for the box (with DFS)
 *  - FBs probably know their parent frag and whether they are a left or a right child
 *  - spechial treatment of leaves! 
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
	
	
	private Map<Fragment,Integer> fragmentToDepth;
	private List<Set<String>> layers;
	private Map<Fragment, DefaultGraphCell> leaflayer; // a leaf mapped to its parent hole
	private List<Set<Fragment>> fraglayers;

	//the x-offset of the nodes of a fragment
	private Map<Fragment,Integer> fragOffset;

	// dominance edges that we want to ignore so as to avoid
	// walking hypernormal paths
	private Set<DefaultEdge> deactivatedEdges;

	// map each fragment to its towers
	private Map<Fragment, List<FragmentTower>> fragmentToTowers;

	// the position of a node within its fragment
	private Map<DefaultGraphCell,Integer> relXtoParent;
	private Map<DefaultGraphCell,Integer> relXtoRoot;
	private Map<DefaultGraphCell,Integer> relYpos;

	// the depth of a node in its fragment - AK ???
	private Map<DefaultGraphCell,Integer> nodesToDepth;
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

		movedRoot = null;
		yOffset = 0;
		//all the other fields are initialized empty
		deactivatedEdges = new HashSet<DefaultEdge>();

		fragXpos = new HashMap<Fragment, Integer>();
		fragYpos = new HashMap<Fragment, Integer>();

		fragWidth = new HashMap<Fragment, Integer>();
		fragHeight = new HashMap<Fragment, Integer>();

		fragOffset = new HashMap<Fragment, Integer>();

		fragmentToDepth = new HashMap<Fragment, Integer>();


		
		relXtoParent = new HashMap<DefaultGraphCell,Integer>();
		relYpos = new HashMap<DefaultGraphCell,Integer>();

		xPos = new HashMap<DefaultGraphCell,Integer>();
		yPos = new HashMap<DefaultGraphCell,Integer>();

		nodesToShape = new HashMap<DefaultGraphCell, Shape>();
		nodesToDepth = new HashMap<DefaultGraphCell,Integer>();
		fragmentToTowers = new HashMap<Fragment, List<FragmentTower>>();
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



	/**
	 * <code>Cost</code>
	 * Class to represent assessment criteria for 
	 * layouts.
	 *
	 */
	private static class Cost implements Comparable<Cost> {

		private int degree, 						// maximal amount of "box tiers"
		crossings, 						// amount of crossings
		towers,  						// amount of towers
		averageTowerHeight, 			// averaged height of towers
		completeTowerHeight, 			// summed up height of towers
		maxBoxHeight, maxBoxWidth,  	// dimensions of the largest box
		maxEdgeLength, minEdgeLength, 	// extreme values of edge length
		boxes, 							// amount of boxes
		edges; 							// amount of edges

		private float boxRatio; 					// ratio maxBoxHeight/maxBoxWidth 
		private double maxEdgeRange; 				// diference of longest and shortest edge 

		public static Cost maxCost = new Cost(Integer.MAX_VALUE);

		/**
		 * 
		 * @return maximal edge range
		 */
		public double getMaxEdgeRange() {
			return maxEdgeRange;
		}

		/**
		 * 
		 * @param maxEdgeRange
		 */
		public void setMaxEdgeRange(double maxEdgeRange) {
			this.maxEdgeRange = maxEdgeRange;
		}

		/**
		 * initializes all fields, most of them with zero,
		 * the values to set "minimal" later on with 
		 * <code>Integer.MAX_VALUE</code>
		 *
		 */
		public Cost() {
			degree = 0;
			crossings = 0;
			towers = 0;
			averageTowerHeight = 0;
			completeTowerHeight = 0;
			maxBoxHeight = 0;
			maxBoxWidth = 0;
			boxRatio = 0;
			boxes = 0;
			maxEdgeLength = 0;
			minEdgeLength = Integer.MAX_VALUE;
			maxEdgeRange = Integer.MAX_VALUE;
		}

		/**
		 * This constructor sets 
		 * all fields in the cost to the value of the
		 * parameter x. It can be used to create Cost objects that are
		 * maximally expensive. (Use it as new Cost(Integer.MAX_VALUE).)
		 */
		private Cost(int x) {
			degree = x;
			crossings = x;
			towers = x;
			averageTowerHeight = x;
			completeTowerHeight = x;
			maxBoxHeight = x;
			maxBoxWidth = x;
			boxRatio = x;
			boxes = x;
			maxEdgeLength = x;
			minEdgeLength = 0;
			maxEdgeRange = x;
		}

		/**
		 * adds up another Cost object to this one.
		 * (deprecated?)
		 * @param c
		 */
		public void add(Cost c) {
			degree += c.degree;
			crossings += c.crossings;
			towers += c.towers;
			averageTowerHeight += c.averageTowerHeight;
			completeTowerHeight += c.completeTowerHeight;
			maxBoxHeight = Math.max(maxBoxHeight,c.maxBoxHeight);
			maxBoxWidth = Math.max(maxBoxWidth, c.maxBoxWidth);
			boxRatio = (boxRatio + c.boxRatio)/2;
			boxes += c.boxes;
		}

		/**
		 * returns the most relevant values of this <code>Cost</code> 
		 * object as <code>String</code>
		 * @return the String reperesentation 
		 */
		public String toString() {
			StringBuffer content = new StringBuffer();

			content.append("<Cost: crossings= " + crossings + ">");
			content.append(System.getProperty("line.separator"));
			content.append("<Cost: box ratio = " + boxRatio + ">");
			content.append(System.getProperty("line.separator"));
			content.append("<Cost: maximal edge range = " + maxEdgeRange + ">");
			content.append(System.getProperty("line.separator"));
			content.append("<Cost: towers = " + towers + ">");
			content.append(System.getProperty("line.separator"));
			content.append("<Cost: average tower height = " + averageTowerHeight + ">");
			content.append(System.getProperty("line.separator"));
			content.append("<Cost: max. superposed boxes = " + degree + ">");
			content.append(System.getProperty("line.separator"));

			return content.toString();
		}

		/**
		 * Updates the saved difference of the minimal and 
		 * the maximal edge length in this <code>Cost</code> 
		 * object.
		 * @param edgeLength the edge length to consider
		 */
		public void updateEdgeCost(double edgeLength) {
			edges++;

			if( edgeLength < minEdgeLength ) {
				minEdgeLength = (int) edgeLength;
				if(edges == 1) {
					maxEdgeLength = minEdgeLength;
				}
			} else if ( edgeLength > maxEdgeLength ) {
				maxEdgeLength = (int) edgeLength;
			}

			maxEdgeRange = maxEdgeLength - minEdgeLength;
		}



		/**
		 * @return Returns the crossings.
		 */
		int getCrossings() {
			return crossings;
		}
		/**
		 * @param crossings The crossings to set.
		 */
		void setCrossings(int crossings) {
			this.crossings = crossings;
		}



		/**
		 * @return Returns the maxBoxHeight.
		 */
		int getMaxBoxHeight() {
			return maxBoxHeight;
		}

		/**
		 * @param maxBoxHeight The maxBoxHeight to set.
		 */
		void setMaxBoxHeight(int height) {
			if(height> maxBoxHeight) {
				maxBoxHeight = height;
			}
		}

		void raiseCrossings(){
			crossings++;
		}
		/**
		 * @return Returns the degree.
		 */
		int getDegree() {
			return degree;
		}
		/**
		 * @param degree The degree to set.
		 */
		void setDegree(int degree) {
			this.degree = degree;
		}

		/**
		 * Changes degree if the parameter is
		 * a higher degree.
		 * @param deg the degree to compare with
		 */
		void setDegreeIfGreater(int deg) {
			if(deg> degree){
				degree = deg;
			}
		}

		/**
		 * @return Returns the towers.
		 */
		int getTowers() {
			return towers;
		}
		/**
		 * @param towers The towers to set.
		 */
		void setTowers(int towers) {
			this.towers = towers;
		}

		/**
		 * raises the amount of towers by one and
		 * computes (approximately) the average
		 * tower height. 
		 * @param towHeight
		 */
		void raiseTowers(int towHeight) {
			towers++;
			completeTowerHeight += towHeight;
			averageTowerHeight = completeTowerHeight/towers;
		}

		/**
		 * raises the amount of towers by one.
		 *
		 */
		void raiseTowers() {
			towers++;
		}

		/**
		 * Compares this <code>Cost</code> object to
		 * another one.
		 * @param anotherCost they other cost object
		 * @return 0 if the two objects are equal,
		 *  	a negative int value if the other Cost object
		 *  	is greater, otherwise a positive int value.
		 */
		public int compareTo(Cost anotherCost) {
			return compare(this, anotherCost);
		}





		/**
		 * Compares the calling <code>Cost</cost> object to
		 * another one.
		 * @param obj, the second cost object
		 * @return true if the two objects represent
		 * 			the same cost values
		 */
		public boolean equals(Object obj) {	
			return (compare(this, (Cost) obj) == 0);
		}


		/**
		 * Compares two <code>Cost</code> objects.
		 * @param costX, the first cost object
		 * @param costY, the second cost object
		 * @return 0 if the two objects are equal,
		 *  	a negative int value if the second Cost object
		 *  	is greater, otherwise a positive int value.
		 */
		private int compare(Cost costX, Cost costY) {

			if (costX.getCrossings() == costY.getCrossings()) {

				if (costX.getMaxEdgeRange() == costY.getMaxEdgeRange()) {

					if (costX.getBoxRatio() == costY.getBoxRatio()) {

						if (costX.getDegree() == costY.getDegree()) {

							if (costX.getMaxBoxHeight() == costY
									.getMaxBoxHeight()) {

								if (costX.getTowers() == costY.getTowers()) {

									if (costY.getAverageTowerHeight() == costX
											.getAverageTowerHeight()) {
										return 0;
									} else {
										return costX.getAverageTowerHeight()
										- costY.getAverageTowerHeight();
									}
								} else {
									return costY.getTowers()
									- costX.getTowers();
								}
							} else {
								return costX.getMaxBoxHeight()
								- costY.getMaxBoxHeight();
							}
						} else {
							return costX.getDegree() - costY.getDegree();
						}

					} else {
						// TODO probably this could be simplified?!
						BigDecimal ratBigInt = new BigDecimal((double) (
								Math.abs(1 - costX.getBoxRatio()) 
								- Math.abs(1 - costY.getBoxRatio())));
						return ratBigInt.signum();
					}
				} else {
					BigDecimal edgLgth = new BigDecimal(costX.getMaxEdgeRange()
							- costY.getMaxEdgeRange());
					return edgLgth.signum();
				}
			} else {
				return costX.getCrossings() - costY.getCrossings();
			} 
		}



		/**
		 * @return Returns the averageTowerHeight.
		 */
		private int getAverageTowerHeight() {
			return averageTowerHeight;
		}

		/**
		 * 
		 * @return the number of boxes
		 */
		public int getBoxes() {
			return boxes;
		}

		/**
		 * @param boxes
		 */
		public void setBoxes(int boxes) {
			this.boxes = boxes;
		}

		/**
		 * 
		 * @return the box ratio
		 */
		public float getBoxRatio() {
			return boxRatio;
		}

		/**
		 * 
		 * @param boxRatio
		 */
		public void setBoxRatio(float boxRatio) {
			this.boxRatio = boxRatio;
		}

		/**
		 * 
		 * @return the maximal box width
		 */
		public int getMaxBoxWidth() {
			return maxBoxWidth;
		}

		/**
		 * Sets the maximal box width.
		 * @param maxBoxWidth 
		 */
		public void setMaxBoxWidth(int maxBoxWidth) {
			this.maxBoxWidth = maxBoxWidth;
		}

		/**
		 * updates this <code>Cost</code> object's boxRatio.
		 * Notes if a parameter is a new extreme value for box
		 * height or box width.
		 * Counts up the boxes and calculates the new ratio of
		 * the maximal box width and the maximal box height.
		 * @param boxHeight the new box height to check
		 * @param boxWidth the new box width
		 */
		public void updateBoxParameter(int boxHeight, int boxWidth) {
			boxes++;
			if( boxHeight > maxBoxHeight ) {
				maxBoxHeight = boxHeight;
			}

			if( boxWidth > maxBoxWidth) {
				maxBoxWidth = boxWidth;
			}

			boxRatio = (float) maxBoxHeight / maxBoxWidth;
		}

		public int hashCode() {
			return maxBoxWidth * maxBoxHeight * boxes * averageTowerHeight;
		}
	}


	private void fillLayers() {

		List<Set<String>> toplevel = new ArrayList<Set<String>>(chart.getToplevelSubgraphs());
		Set<String> roots = domgraph.getAllRoots();
		
		for(Set<String> tls : toplevel ) {
			fillLayer(0, new ArrayList<Split>(chart.getSplitsFor(tls)), new HashSet<String>());
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
					leaflayer.put(frag,parent);
				} else {
				
				fraglayers.get(i).add(frag);
				}
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

		//the in-edges of a fragment are the equivalent of
		//the in-edges of the fragment's root.
		return graph.getInEdges(getFragRoot(frag));
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
		return outEdges;

	}

	/**
	 * Computes (approximately) the length of the Edge between two 
	 * Fragments. 
	 * 
	 * @param from
	 * @param to
	 * @return the distance
	 */
	private double getFragmentDistance(Fragment from, Fragment to,
			Map<Fragment,Integer> xCoordinates, Map<Fragment, Integer> yCoordinates) {
		int fromX = xCoordinates.get(from) + (fragWidth.get(from)/2);
		int fromY = yCoordinates.get(from) + fragHeight.get(from);
		int toX = xCoordinates.get(to) + (fragWidth.get(to)/2);
		int toY = yCoordinates.get(to);

		double roughDistance =
			Math.sqrt(
					Math.pow(Math.abs(fromX - toX),2) 
					+ Math.pow(Math.abs(fromY-toY),2)
			);

		return roughDistance;
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


	



	
	/**
	 * computes the whole fragment graph.
	 * computes the fragment's x-position with undirected DFS,
	 * the fragment's later y-position performing directed DFS 
	 * (for each root).
	 */
	private void computeFragmentPositions() {

		int yFragPos = 0;
		int yPosToAdd = 0;
		int xFragPos = 0;
		int noOfBiggestLayer = 0;
		int widthOfBiggestLayer = 0;
		Set<Fragment> visited = new HashSet<Fragment>();
		List<Set<Fragment>> roots = new ArrayList<Set<Fragment>>();

		for(int i = 0; i < fraglayers.size(); i++ ) {

			Set<Fragment> layer = fraglayers.get(i);
			int fragCount = 0;

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
		
		
		for(Set<Fragment> layer : fraglayers) {
	//		int x0;
			double deltax; 

			if(layer.size() == noOfBiggestLayer) {
				System.err.println(layer);
		//		x0 = 0;
				deltax = widthOfBiggestLayer/(layer.size() -1);
			} else {
				deltax = widthOfBiggestLayer/(layer.size() + 1);
			//	x0 = (int) deltax;
			}
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

		int offset = 0;
		for(Set<String> toplevel : chart.getToplevelSubgraphs()) {
			
			Set<String> free = new HashSet<String>();
			for(Split split : chart.getSplitsFor(toplevel)) {
				free.add(split.getRootFragment());
			}
			FragmentBox box = new FragmentBox(toplevel, free);
			System.out.println(box.fragToXPos);
			
			for(Fragment boxfrag : box.frags) {
				System.out.println(boxfrag);
				fragXpos.put(boxfrag, box.getBoxXPos(boxfrag) + offset);
				offset +=box.width;
			}
		}
		
		
	





	}

	


	/**
	 * Computes the roots for this fragment graph (roots are all 
	 * the fragments with no incoming edges).
	 * @return the roots
	 */
	private List<Fragment> getFragmentGraphRoots() {
		List<Fragment> roots = new ArrayList<Fragment>();

		for(Fragment frag : fragments) {
			if(getFragInEdges(frag).size() == 0) {

				roots.add(frag);

			}	

		}
		return roots;
	}


	private void computeFragmentGraphDepth() {
		Set<Fragment> visited = new HashSet<Fragment>();
		for(Fragment frag :  getFragmentGraphRoots()) {
			putFragmentDepth(visited, frag, 0);
		}

	}

	private void putFragmentDepth(Set<Fragment> visited, Fragment frag, int depth) {
		if(! visited.contains(frag)) {
			visited.add(frag);
			fragmentToDepth.put(frag,depth);
			int ndepth = depth+ 1;
			List<DefaultEdge> outedges = getFragOutEdges(frag);
			for(DefaultEdge outedge : outedges) {
				Fragment child = graph.getTargetFragment(outedge);
				putFragmentDepth(visited, child, ndepth);
			}

		}

	}


	/**
	 * computes the position of all nodes considering
	 * their relative poitions within a fragment and the
	 * position of their fragment (cp. its fragment node).
	 */
	private void computeNodePositions() {


		for(DefaultGraphCell node : graph.getNodes() ) {

			Fragment nodeFrag = graph.findFragment(node);

			int x = relXtoRoot.get(node); 
			int offset = fragOffset.get(nodeFrag) - graph.computeNodeWidth(node)/2;
			int xMovement;
			
			if(leaflayer.containsKey(nodeFrag)) {
				DefaultGraphCell sourceHole = leaflayer.get(nodeFrag);
				Fragment parent = graph.findFragment(sourceHole);
				
				xMovement = relXtoParent.get(sourceHole)
				+ graph.computeNodeWidth(sourceHole)/2
				- graph.computeNodeWidth(node)/2
				+ fragOffset.get(nodeFrag)
				+ fragXpos.get(parent);
			} else {
				xMovement= fragXpos.get(nodeFrag)  + offset;
			}	
			/*
			 * the absolute x- position is the relative
			 * position added to the fragment's x-position
			 */
			x += xMovement ;

			xPos.put(node, x);

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
							(fragmentYDistance/2);
			} else {
				yMovement = fragYpos.get(nodeFrag);
			}
			
			
			y += yMovement;

			if(yOffset> 0 && (! nodeFrag.equals(movedRoot)))
				y += yOffset;

			yPos.put(node,y);


		}
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

		fillLayers();
		computeFragDimensions();
		computeFragmentPositions();
		computeNodePositions();
		placeNodes();

	}



	/**
	 * @return Returns the nodesToDepth.
	 */
	private Map<DefaultGraphCell, Integer> getNodesToDepth() {
		return nodesToDepth;
	}


	/**
	 * @param nodesToDepth The nodesToDepth to set.
	 */
	private void setNodesToDepth(Map<DefaultGraphCell, Integer> nodesToDepth) {
		this.nodesToDepth = nodesToDepth;
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
		
			return getFragOutEdges(arg0).size() - getFragOutEdges(arg1).size();
		}
		
	}
	
	public class FragmentInDegreeComparator implements Comparator<Fragment> {
		
		public int compare(Fragment arg0, Fragment arg1) {
			return getFragInEdges(arg0).size() - getFragInEdges(arg1).size();
		}
		
	}
	
	/**
	 * A helper class containing fragments and layouting them
	 * relative to their box.
	 * 
	 * @author Michaela Regneri
	 *
	 */
	public class FragmentBox {

		private Map<Fragment, Integer> fragToXPos; //positions within the box
		private Map<Set<Fragment>, FragmentBox> children; // boxes contained in this box

		private Set<String> fragmentsString; // string representation of my fragment's nodes
		private Set<Fragment> frags; // my fragments
		private Set<String> freefragments; // the free fragments contained in the wcc I consist of
		
		private int width; // my width (after layout)

		/**
		 * A fragment box for a wcc of a graph
		 * @param wcc
		 * @param freefrags
		 */
		FragmentBox( Set<String> wcc, Set<String> freefrags ) {
			
			fragmentsString = new HashSet<String>(wcc);	
			freefragments = freefrags;
			fragToXPos = new HashMap<Fragment, Integer>();
			
			
			children = new HashMap<Set<Fragment>, FragmentBox>();

			Set<String> childnodes = new HashSet<String>(wcc);
			childnodes.removeAll(freefragments);
			
			

			
			//TODO refactor the box computation
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
					freeChildFrag.addAll(childwcc);
				}
				
				Set<Fragment> childroots = convertStringsToFragments(freeChildFrag);
				
				// a new childbox
				children.put(childroots, new FragmentBox(childwcc, freeChildFrag));
			}

			frags = convertStringsToFragments(fragmentsString);
		//	frags.removeAll(leaflayer.keySet());
			
			
			
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
			int x = 0; // the first x is 0
			
			for(Fragment frag : possibleRoots ) {
				if(bestCost == -1 ) {
					// first loop
					bestCost = fragBoxDFS(0, new HashSet<Fragment>(), frag, 0, null);
					bestRoot = frag;
				} else {
					int nextCrossCount = fragBoxDFS(0, new HashSet<Fragment>(), frag, 0, null);
					if(nextCrossCount < bestCost) {
						bestCost = nextCrossCount;
						bestRoot = frag;
					}
				}
				
				fragToXPos.clear();
			}
			
			fragBoxDFS(0, new HashSet<Fragment>(), bestRoot, 0, null);
			} else {
				if(! frags.isEmpty())
				fragToXPos.put(frags.iterator().next(), 0);
			}
			
			Set<Fragment> free = convertStringsToFragments(freefrags);
			if(free.size() == 1) {
				Fragment top = free.iterator().next();
				if(getFragInEdges(top).size() == 0) {
					fragToXPos.put(top, (width - fragWidth.get(top))/2);
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
				DefaultGraphCell lastroot) {

			int cross = crossings;
			int nextX = x;
			int myX = x;
			DefaultGraphCell currentRoot = getFragRoot(current);
			int childWidth = 0;
			

			if(! visited.contains(current)) {
				visited.add(current);

			
			// 0. Compute the hole by which we entered; if it's not the left one,
			//    assume an edge crossing

			if( lastroot != null ) {
				boolean lefthole = true; // the first hole is the left hole.
				
				for( DefaultGraphCell hole : getFragHoles(current) ) {
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
			
			
			/*
			 * Placing the recent fragment.
			 * It can be the case that I belong actually to a childbox of this box.
			 */
			FragmentBox myBox = getBoxForFrag(current);
			if(myBox == null) {
				// this is probably the case iff 
				// I am a 'single'
				fragToXPos.put(current, myX);
				nextX += fragWidth.get(current) + fragmentXDistance;

			} else {
				for(Fragment frag : myBox.frags) {
					visited.add(frag);
					fragToXPos.put(frag, myBox.getBoxXPos(frag) + myX);
				}
				nextX += myBox.getWidth() + fragmentXDistance;
			}
			
			
			// 1. compute my unseen parents.
			List<Fragment> parents = new ArrayList<Fragment>();
			for(DefaultEdge edge : getFragInEdges(current)) {
				Fragment par = graph.getSourceFragment(edge);
				if(frags.contains(par) && (! visited.contains(par))) {
					parents.add(par);
				}
			}
			
			if(parents.size() > 1) {
				// this is to make sure that I start placing my parents which only 
				// dominate myself right above me.
				Collections.sort(parents, new FragmentOutDegreeComparator());
				for(Fragment par : parents) {
					cross += fragBoxDFS(nextX, visited, par, 0, currentRoot);
					nextX += fragWidth.get(par) + fragmentXDistance;
				}
				
			} else {
				for(Fragment par : parents) {
					cross += fragBoxDFS(nextX, visited, par, 0, currentRoot);
				}
			}
			

			// 2. compute my unseen children.
			Set<Fragment> childfrags = new HashSet<Fragment>();
			for(DefaultEdge edge : getFragOutEdges(current)) {
				Fragment child = graph.getTargetFragment(edge);
				if(! visited.contains(child)) {
					childfrags.add(child);
				}
			}

			
			List<Fragment> childboxparents = new ArrayList<Fragment>();
			
			for(Fragment child : childfrags) {
				FragmentBox childbox = getBoxForFrag(child);
				
				// merging the box of my child, if there is one.
				if(childbox != null) {
					for(Fragment cbf : childbox.frags) {
						visited.add(cbf);
						fragToXPos.put(cbf,
								childbox.getBoxXPos(cbf) + nextX);
					}
					nextX += fragmentXDistance + childbox.getWidth();
					childWidth += childbox.getWidth();
					
					for(DefaultEdge edge : getFragInEdges(child)) {
						Fragment parent = graph.getSourceFragment(edge);
						if(! visited.contains(parent) &&  frags.contains(parent) ){
							childboxparents.add(parent);
						}
					}
					
					
				} else {
					// no childbox. this should never happen. However, if it does - 
					// just go on.
					cross += fragBoxDFS(nextX, visited, child, 0, currentRoot);
					childWidth += fragWidth.get(child);
					nextX += fragWidth.get(child) + fragmentXDistance;
				}
			}
			
			// 3. place my brothers -- the parents of my childboxes.
			if(childboxparents.size() > 1) {
				// this is to make sure that I start placing my parents which only 
				// dominate myself right above me.
				Collections.sort(childboxparents, new FragmentOutDegreeComparator());
				for(Fragment par : childboxparents) {
					cross += fragBoxDFS(nextX, visited, par, 0, currentRoot);
					nextX += fragWidth.get(par) + fragmentXDistance;
				}

			} else {
				
				for(Fragment par : childboxparents) {
					cross += fragBoxDFS(nextX, visited, par, 0, currentRoot);
				}
			}



				width = nextX - fragmentXDistance;

			} 

			return cross;
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
						(getFragDegree(frag) == 1 || convertStringsToFragments(freefragments).contains(frag)) &&
						// AND I have no children
						getFragOutEdges(frag).size() == 0) {
					theroots.add(frag);
				}
			}

			return theroots;
		}

		

		int getWidth() {
			return width;
		}
		
		int getBoxXPos(Fragment frag) {
			return fragToXPos.get(frag);
		}

	}


}



