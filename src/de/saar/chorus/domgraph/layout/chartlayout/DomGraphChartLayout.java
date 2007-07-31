package de.saar.chorus.domgraph.layout.chartlayout;

import static de.saar.chorus.domgraph.layout.DomGraphLayoutParameters.fragmentXDistance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org._3pq.jgrapht.Edge;

import de.saar.chorus.domgraph.chart.Chart;
import de.saar.chorus.domgraph.chart.ChartSolver;
import de.saar.chorus.domgraph.chart.Split;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.EdgeType;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.domgraph.layout.Canvas;
import de.saar.chorus.domgraph.layout.DomGraphLayoutParameters;
import de.saar.chorus.domgraph.layout.FragmentLayoutAlgorithm;

/**
 * This is a draft for a new chart-based layout algorithm.
 * 
 * NOTE: Fragments are now represented by their ROOT NODE
 * 
 * 
 * @author Alexander Koller
 * @author Michaela Regneri
 */
public class DomGraphChartLayout extends FragmentLayoutAlgorithm {

	

	
	private Chart chart;
	// the fragment positions
	
	private List<Set<String>> layers;
	private Map<Integer,Integer> toExtent;
	
	private Map<String, String> leaflayer; // a leaf mapped to its
														// parent hole

	private Map<String, Integer> fragmentToLayer;

	private Set<String> oneHoleFrags;

	private List<Set<String>> fraglayers;

	

	// the absolute position of a node in the graph
	private Map<String, Integer> xPos;

	private Map<String, Integer> yPos;

	private String movedRoot;

	private int yOffset;

	private Set<Edge> lightDominanceEdges;
	
	/**
	 * Initializes a new dominance graph layout of a given dominanc graph.
	 * 
	 * @param gr
	 *            the graph to compute the layout for
	 */
	public DomGraphChartLayout() {
		layers = new ArrayList<Set<String>>();
		fraglayers = new ArrayList<Set<String>>();
		fragmentToLayer = new HashMap<String, Integer>();

		movedRoot = null;
		yOffset = 0;
		// all the other fields are initialized empty

		oneHoleFrags = new HashSet<String>();

		xPos = new HashMap<String, Integer>();
		yPos = new HashMap<String, Integer>();

		lightDominanceEdges = new HashSet<Edge>();

		leaflayer = new HashMap<String, String>();
	}

	
	/**
	 * This computed the layers of the graph. The layers consist of fragments,
	 * whose maximal depth (the longest path from a root via dominance edges) 
	 * corresponds to the layer. Thus layer 0 contains all roots of the fragment graph,
	 * and so on.
	 *
	 */
	private void fillLayers() {

		// retrieving the toplevel subgraphs from the chart.
		List<Set<String>> toplevel = new ArrayList<Set<String>>(chart
				.getToplevelSubgraphs());
		Set<String> roots = domgraph.getAllRoots();

		// the free fragments in the toplevel subgraph
		// are the root fragments of the graph and
		for (Set<String> tls : toplevel) {
			if (chart.containsSplitFor(tls)) {
				// root fragments are in layer 0, this computes the other layers
				// recursively
				fillLayer(0, new ArrayList<Split>(chart.getSplitsFor(tls)),
						new HashSet<String>());
			} else {
				// if the chart is empty, all fragments are 
				// considered as root fragments
				Set<String> leaves = new HashSet<String>(tls);
				addToLayer(leaves, 0);
			}
		}

		// iterate over the layers
		for (int i = 0; i < layers.size(); i++) {

			Set<String> layer = new HashSet<String>(layers.get(i));
			layer.retainAll(roots);
			fraglayers.add(i, new HashSet<String>());

			for (String node : layer) {

				String frag = domgraph.getRoot(node);
				
				
					if(! leaflayer.containsKey(frag)) {
						fraglayers.get(i).add(frag);
					} else {
						int height;
						if(toExtent.containsKey(i-1)) {
							height = Math.max(toExtent.get(i-1), fragHeight.get(frag));
						} else {
							height =  fragHeight.get(frag);
						}
						toExtent.put(i -1, height);
					}
						
					
				fragmentToLayer.put(frag, i);
			}

		}

	}

	/**
	 * Recursive helper method to assign a layer to each fragment in the given 'layer'
	 * and deeper. 
	 * 
	 * @param layer the layer depth to start with
	 * @param splits the initial set of splits for the start layer
	 * @param visited the nodes visited so far
	 */
	private void fillLayer(int layer, List<Split> splits, Set<String> visited) {

		Set<String> recent = new HashSet<String>();
		Set<Set<String>> remainingSubgraphs = new HashSet<Set<String>>();

		int nextLayer = layer + 1;

		// iterating over the splits for this layer
		for (Split split : splits) {
			String root = split.getRootFragment();
			if (!visited.contains(root)) {
				
				// the split's root is part of the fragments in the
				// current layer
				visited.add((root));
				recent.add(root);
				recent.addAll(new HashSet<String>(split.getAllDominators()));
				
				// we have to visited the split's subgraph in the next loop.
				remainingSubgraphs.addAll(new ArrayList<Set<String>>(split
						.getAllSubgraphs()));
			}

		}

		// fill the current layer
		addToLayer(recent, layer);

		// iterate over the subgraphs and assign layers
		// to their nodes
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

	/**
	 * Helper method adding a set of nodes to a given layer
	 * 
	 * @param recent the nodes to fill the layer with
	 * @param ind the layer's number
	 */
	private void addToLayer(Set<String> recent, int ind) {
		
		// if the layer does not exist it, create it and
		// add the nodes
		if (layers.size() <= ind) {
			layers.add(ind, recent);

		} else {
			// if the layer already exists, 
			// add all nodes to it.
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
	 * 
	 *
	 */
	private void computeOneHoleFrags() {
		for (String frag : fragments) {
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
	 * This method determines "one-hole fragments" which are treated in a 
	 * in a special way during layout. Fragments of this kind are fragments which have 
	 * exactly one hole with at most one outgoing edge and fragments with exactly two holes,
	 * whereby the left hole's child is a leaf without other dominance parents.
	 * 
	 * Returned is (for layout purposes) the width of the fragment's child, if the fragment 
	 * is a one-hole fragment and has a child. If there is no child, the width of the hole is returned.
	 * If the fragment is not a one-hole fragment, -1 is returned.
	 * 
	 * @param frag the fragment to check
	 * @return the width of the fragment's child if the frag is a one-hole fragment, -1 otherwise
	 */
	private int isOneHoleFrag(String frag) {
		List<String> holes = getFragHoles(frag);
		
		/*
		 * Fragments with one hole fulfill the conditions, if
		 * they have at most one outgoing edge.
		 */
		if (holes.size() == 1) {
			String hole = holes.get(0);
			List<String> children = domgraph.getChildren(hole, EdgeType.DOMINANCE);
			
			// one outg. edge?
			if (children.size() == 1) {
				String child = children.get(0);
					
				
				// child of the hole is a leaf?
				if (getFragDegree(child) == 1) {
					return fragWidth.get(child);
				} else {
					return canvas.getNodeWidth(nodeToLabel.get(hole));
				}
			}
		} else if (holes.size() == 2) {
			// 2 fragment holes
			
			boolean first = true;
			for (int i = 0; i< holes.size(); i++) {
				String hole = holes.get(i);
				// the child of the left hole has to be a leaf.
				if (first) {
					first = false;
					List<String> children = domgraph.getChildren(hole, EdgeType.DOMINANCE);
					
					if (children.size() == 1) {
						// one edge out of the left hole
						
						String child = children.get(0);
				
						
						if(getFragDegree(child ) == 1) {
							leaflayer.put(child, hole);
							return fragWidth.get(child);
						} else {
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
	public void computeFragmentPositions() {

		computeOneHoleFrags();
		fillLayers();
		
		int yFragPos = 0;
		int yPosToAdd = 0;
		
		
		Set<String> visited = new HashSet<String>();

		// compute y-positions by using the layers.
		
		// for all (main) layers...
		for (int i = 0; i < fraglayers.size(); i++) {
			Set<String> layer = fraglayers.get(i);
			
			// iterate over the layer's fragments...
			for (String frag : layer) {
				if (!visited.contains(frag) &&
						! leaflayer.containsKey(frag)) {
					visited.add(frag);
					
					// and place them according to their depth.
					fragYpos.put(frag, yFragPos);
					yPosToAdd = Math.max(fragHeight.get(frag), yPosToAdd);
					
					
				}
			}
			if(toExtent.containsKey(i)) {
				yPosToAdd += toExtent.get(i);
			}
			yFragPos += yPosToAdd + DomGraphLayoutParameters.fragmentYDistance;
		}
		
		// for all leave fragments (entries in leaf layers)
		for(Map.Entry<String, String> leafWithParent : leaflayer.entrySet()){
			
			String leaf = leafWithParent.getKey();
			String sourceHole = leafWithParent.getValue();
			String parent = domgraph.getRoot(sourceHole);
			// place the leaf fragment relative to its parent hole.
			int y = fragYpos.get(parent) + fragHeight.get(parent)
					+ DomGraphLayoutParameters.leafYDistance;
			fragYpos.put(leaf, y);
		}
		

		int rightBorder = 0; 	// the right border of the recent fragment box
		int xoffset = 0;		// the x-position, where the next fragment box may start
		String topFragment = null; // a variable for a top fragment, if there is one.
		
		// determining the top fragment
		if(! fraglayers.isEmpty() ) {
			if (fraglayers.get(0).size() == 1) {
				topFragment = fraglayers.get(0).iterator().next();
			}
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
			Set<String> seen = new HashSet<String>();
			for (String bfrag : box.frags) {
				String boxfrag = domgraph.getRoot(bfrag);
				if(! seen.contains(boxfrag) ) {
					seen.add(boxfrag);
				int x = box.getBoxXPos(boxfrag) + xoffset;
				fragXpos.put(boxfrag, x);
				rightBorder = Math.max(rightBorder, x + fragWidth.get(boxfrag));
			}
				}
			
			// the next toplevel box is placed to the right of the last one.
			xoffset = rightBorder + fragmentXDistance;
		}

		// placing the top fragment in the middle of the graph.
		if (topFragment != null) {
			
			fragXpos.remove(topFragment);
			fragXpos.put(topFragment, rightBorder / 2
					- fragWidth.get(topFragment) / 2);
			for (Edge edge : getFragOutEdges(topFragment)) {
				lightDominanceEdges.add(edge);
			}
		}

	}

	/**
	 * Helper method that moves  all fragments in x direction by a given value.
	 * 
	 * @param x value indicating the graph movement.
	 */
	private void moveGraph(int x) {
		Set<String> seen = new HashSet<String>();
		for (String node : domgraph.getAllNodes()) {

			if (xPos.containsKey(node)) {
				int old = xPos.get(node);
				xPos.remove(node);
				xPos.put(node, old + x);
			} else {
				String frag = domgraph.getRoot(node);
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
	public void computeNodePositions() {
	
		int movegraph = 0;
		for (String node : domgraph.getAllNodes()) {

			String nodeFrag = domgraph.getRoot(node);

			int x = relXtoRoot.get(node);
			int offset = fragOffset.get(nodeFrag)
					- canvas.getNodeWidth(nodeToLabel.get(node)) / 2;
			int xMovement;
			if (leaflayer.containsKey(nodeFrag)) {
				String sourceHole = leaflayer.get(nodeFrag);
				String parent = domgraph.getRoot(sourceHole);

				xMovement = relXtoParent.get(sourceHole)
						- canvas.getNodeWidth(nodeToLabel.get(node)) / 2
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
	public void placeNodes() {

		// place every node on its position
		// and remembering that in the viewMap.
		for (String node : domgraph.getAllNodes()) {
			int x = xPos.get(node).intValue();
			int y = yPos.get(node).intValue();

			canvas.drawNodeAt(x, y, node, nodelabels.getLabel(node), 
					domgraph.getData(node), nodeToLabel.get(node));
		}

	}

	public void drawEdges() {
		for(Edge edge: domgraph.getAllEdges()) {
			String src = (String) edge.getSource();
			String tgt = (String) edge.getTarget();
			if( domgraph.getData(edge).getType().equals(de.saar.chorus.domgraph.graph.EdgeType.TREE)) {
				canvas.drawTreeEdge(src, tgt);
			} else if (lightDominanceEdges.contains(edge) ) {
				canvas.drawLightDominanceEdge(src, tgt);
			} else {
				canvas.drawDominanceEdge(src, tgt);
			}
		}
	}

	

	
	/****** Getters and Setters ********/
	
	
	
	
	
	/**** some helper methods ****/
	
	

	/**
	 * Comparator sorting a collection of fragments ascending
	 * according to their number of outgoing edges. One-Hole fragments count
	 * as having fewer outgoing edges when if the actual number is equal.
	 * 
	 * @author Michaela Regneri
	 *
	 */
	public class FragmentOutDegreeComparator implements Comparator<String> {

		/**
		 * @return 1 if the first fragment has more outgoing edges, 
		 * 	       0 if the number of outgoing edges is equal, -1 otherwise
		 */
		public int compare(String arg0, String arg1) {
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
	
	boolean isForest(Set<String> subgraph) {
		
		for(String node : subgraph) {
			if(domgraph.getParents(node, null).size() > 1) {
				return false;
			}
		}
		
		return true;
	}
	

	/**
	 * Comparator sorting a Collection of Fragments according to their 
	 * number of incombing edges.
	 * @author Michaela Regneri
	 *
	 */
	public class FragmentInDegreeComparator implements Comparator<String> {

		public int compare(String arg0, String arg1) {
			return getFragInEdges(arg0).size() - getFragInEdges(arg1).size();
		}

	}

	public void initialise(DomGraph graph, NodeLabels labels, Canvas canv) {
		super.initialise(graph, labels, canv);
		
		
		chart = new Chart();
		toExtent = new HashMap<Integer,Integer>();
		
		// checking whether or not the DomGraph is weakly normal
		if (domgraph.isWeaklyNormal()) {
			// if so, proceed with the "normal" chart
			ChartSolver.solve(domgraph, chart);
		} else {
			// if the graph is not wn, compute the chart of the wn backbone.
			DomGraph wnbackbone = domgraph.makeWeaklyNormalBackbone();
			ChartSolver.solve(wnbackbone, chart);
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
		FragmentBox current = new FragmentBox(fragmentsString);
		

		/*
		 * Computing the complete current subgraph in order to check whether it 
		 * is a tree. If it is a tree, a different layout style is chosen, and 
		 * the child boxes are not computed.
		 */
		Set<String> mySubgraph = new HashSet<String>();
		for (String frag : current.getFrags()) {
		//mySubgraph.addAll(domgraph.getFragment(frag));
			mySubgraph.add(frag);
		}
		
		if (isForest(mySubgraph)) {
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
				

				// a new childbox
				current.putChild(freeChildFrag, makeFragmentBox(childwcc,
						freeChildFrag));
			}

			
			/*
			 * Layout: Determine the best root and then execute the final DFS.
			 */
			
			if (current.getFrags().size() > 1) {
				
				// compute the "allowed" roots which we want to be on the left-hand side.
				Set<String> possibleRoots = getPossibleRoots(current, freefragments);
				if (possibleRoots.isEmpty()) {
					// if there are no "good" roots, consider all fragments as possible roots.
					possibleRoots.addAll(current.getFrags());
				}

		
				
				// initialising the best root and the number of crossings.
				String bestRoot = possibleRoots.iterator().next();
				int bestCost = -1;

				// try out all the roots and store the one leading to a layout 
				// with as few crossings as possible.
				for (String frag : possibleRoots) {
					if (bestCost == -1) {
						// first loop
						bestCost = fragBoxDFS(current, 0,
								new HashSet<String>(), frag, 0, null);
						bestRoot = frag;
					
						
					} else {
						// not the first loop.
						int nextCrossCount = fragBoxDFS(current, 0,
								new HashSet<String>(), frag, 0, null);
						
						if (nextCrossCount < bestCost) {
							bestCost = nextCrossCount;
							bestRoot = frag;
						}
					}
					
					// reset the storages in the FragmentBox
					current.clear();
				}
				
				// final DFS
				fragBoxDFS(current, 0, new HashSet<String>(), bestRoot, 0,
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
	private int fragBoxDFS(FragmentBox box, int x, Set<String> visited,
			String current, int crossings, String lastroot) {

		// initialising
		Set<String> frags = box.getFrags(); // the fragments to consider
		int cross = crossings; // counter for crossings
		int nextX = x;		   // the x for the next DFS loop
		int myX = x;		   // x for the current fragment
		int rightX = x; 	   // x to the right of the current fragment
		
		// root of the current fragment
		String currentRoot = domgraph.getRoot(current);

		// storage for the parent fragments of the current fragment
		List<String> parents = new ArrayList<String>();
		List<String> wnparents = new ArrayList<String>();

		// Compute the hole by which we entered; if it's not the left one,
		// assume an edge crossing

		if (lastroot != null) {
			boolean lefthole = true; // the first hole is the left hole.
			boolean openhole = false; //found an open hole (-> wn edge) on my way
			boolean sourcefound = false;
			List<String> openholes = domgraph.getOpenHoles(currentRoot);
			holes : for (String hole : getFragHoles(currentRoot)) {
				for (String child : domgraph.getChildren(hole, EdgeType.DOMINANCE)) {
					
					if(lastroot.equals(domgraph.getRoot(child)) ) {	
						sourcefound = true;
						if (! lefthole) {
							cross++; // if the last fragment was not the
							// child of the left hole...
						}
						break holes;
					}
					
					
				}
				if ( openholes.contains(lastroot) ) {
					openhole = true;
				} 
				if (lefthole) {
					lefthole = false;
				}
			}
			if(openhole && ! sourcefound) {
				cross++;
			}
		}

		if (!visited.contains(currentRoot)) {
			/*
			 * Placing the recent fragment. It can be the case that I belong
			 * actually to a childbox of this box.
			 */
			FragmentBox myBox = box.getBoxForFrag(currentRoot);
			
			
			if (myBox == null) {
				// there is no childbox for myself.
				visited.add(currentRoot);
				
				// if I am _no_ "one-hole fragment"
				if (!oneHoleFrags.contains(currentRoot)) {
					// place me at my designated x-position if it is not
					// smaller than the next possible x position in my layer
					myX = Math.max(x, box.getNextPossibleX()[fragmentToLayer
							.get(currentRoot)]);
				} else {
					// if I am a one-hole fragment, just fill up the next space
					// in the layer.
					myX = box.getNextPossibleX()[fragmentToLayer.get(currentRoot)];
				}
				
				// place myself
				box.setBoxXPos(currentRoot, myX);
				
				// update the next possible x-position in my layer
				box.getNextPossibleX()[fragmentToLayer.get(currentRoot)] = myX
						+ fragWidth.get(currentRoot) + fragmentXDistance;

				
				// compute my parents
				for(String fragnode : domgraph.getFragment(currentRoot)) {
					for(String par : domgraph.getParents(fragnode,EdgeType.DOMINANCE)) {
						String parfrag = domgraph.getRoot(par);

						if (frags.contains(parfrag) && (!visited.contains(parfrag))) {
							if(domgraph.isHole(fragnode)) {
								wnparents.add(parfrag);
							} else {
								parents.add(parfrag);
							}
							
						}
					}
				}

			} else {
				// the current fragment belongs to a box, so I have to place
				// this fragment and the other fragments of the box.
				
				// the first fragment is assumed to be the rightmost one, so
				// we treat it separately.
				boolean first = true;

				
				
				
				// check the box fragments from the left to the right
				for (String frag : myBox.getSortedFragments()) {
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
				
					for(String node : domgraph.getFragment(frag)) {
						for(String par : domgraph.getParents(node, EdgeType.DOMINANCE)) {
							String parfrag = domgraph.getRoot(par);
							if (frags.contains(parfrag) && (!visited.contains(parfrag))
									&& (!myBox.frags.contains(parfrag))) {
								parents.add(parfrag);
							}
						}
					}
					
				}

			}
			
			
			/* placing my parents */
			
			// the x to start my parent DFS with.
			nextX = box.getBoxXPos(currentRoot) + fragmentXDistance
					+ fragWidth.get(currentRoot);

			if (getFragHoles(currentRoot).size() == 1) {
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
				for (String par : parents) {
					cross += fragBoxDFS(box, nextX, visited, par, 0,
							currentRoot);
					nextX = box.getBoxXPos(par) + fragWidth.get(par)
							+ fragmentXDistance;
				}

			} else {
				// if I have only one parent, I don't need any sorting.
				for (String par : parents) {
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
			Set<String> childfrags = new HashSet<String>();
			for(String node : domgraph.getFragment(currentRoot)) {
				for(String child : domgraph.getChildren(node, EdgeType.DOMINANCE)) {
					String childfrag = domgraph.getRoot(child);
					if (!visited.contains(childfrag) ) {
						childfrags.add(childfrag);
					}
				}
			}
			
			
	

			// iterate over the children
			for (String child : childfrags) {
				
				if (!visited.contains(child)) {
					
					// store the parents of the children's boxes, if there are some.
					List<String> childboxparents = new ArrayList<String>();
					
					// the roots for the boxparents
					List<String> childroots = new ArrayList<String>();
					
					
					// checking whether the child is part of a box.
					FragmentBox childbox = box.getBoxForFrag(child);

					if (childbox != null) {

						// the child is in a box;
						// merge the box into my own.
						
						boolean first = true;
						for (String cbf : childbox.getSortedFragments()) {
							
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
							for(String nodefrag : domgraph.getFragment(cbf)) {
								for(String parent : domgraph.getParents(nodefrag,EdgeType.DOMINANCE)) {
									String parfrag = domgraph.getRoot(parent);
									if (!visited.contains(parfrag)
											&& frags.contains(parfrag)
											&& (!childbox.frags.contains(parfrag))) {
										childboxparents.add(parfrag);
										childroots.add(domgraph.getRoot(cbf));
									}
								}
							}
							
							
						}
						nextX = box.getBoxXPos(child) + fragWidth.get(child)
								+ fragmentXDistance;

						// sort the parents according to their number of outedges.
						Collections.sort(childboxparents,
								new FragmentOutDegreeComparator());
						
						// place the parents of the childbox.
						
						for(int i = 0; i < childboxparents.size(); i++) {
							String boxpar = childboxparents.get(i);
							String realroot = childroots.get(i);
							cross += fragBoxDFS(box, nextX, visited, boxpar, 0,
								realroot);
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
		
		for(String wnp : wnparents) {
			if(! visited.contains(wnp)) {
				cross += fragBoxDFS(box, nextX, visited, wnp, 0, currentRoot);
			}
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
		String root = getRoot(box);
		if (root != null) {
			fragBoxTreeLayoutDFS(box, root, new HashSet<String>(), 0);
		} else {
			return false;
		}
		return true;
	}

	/**
	 * Determine the root for a fragment box whose nodes form a forrest.
	 * @return the root, null if the nodes are not a forrest.
	 */
	String getRoot(FragmentBox box) {

		Set<String> frags = box.getFrags();
		for (String frag : frags) {
			if (getFragInEdges(frag).size() == 0)
				return frag;

			boolean parent = false;
			for (Edge edge : getFragInEdges(frag)) {
				String par = domgraph.getRoot((String) edge.getSource()) ;
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
	int fragBoxTreeLayoutDFS(FragmentBox box, String cur,
			Set<String> visited, int xStart) {
		String current = domgraph.getRoot(cur);
		if (!visited.contains(current)) {
			visited.add(current);
			
			// determine my children
			List<String> childfrags = new ArrayList<String>();
			for (Edge edge : getFragOutEdges(current)) {
				String child = domgraph.getRoot( (String) edge.getTarget());
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
			for (String child : childfrags) {
				
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
	Set<String> getPossibleRoots(FragmentBox box, Set<String> freefragments) {
		Set<String> theroots = new HashSet<String>();

		for (String frag : box.getFrags()) {
			if ( // I have one and only one parent, or my parents are not in
					// the box.
			( getFragInEdges(frag).size() == 1 || freefragments.contains(frag) )
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

		private Map<String, Integer> fragToXPos; // positions within the box
		
		private Map<Set<String>, FragmentBox> children; // boxes contained
															// in this box

		private Set<String> frags; // my fragments

		private int[] nextPossibleX; // the leftmost free position for a
										// fragment in a slot.

		private int width; // my width (after layout)
	
		
		/**
		 * 
		 * @return
		 */
		Set<String> getFrags() {
			return frags;
		}

		/**
		 * 
		 * @param frags
		 */
		void setFrags(Set<String> frags) {
			this.frags = frags;
		}
		
		/**
		 * 
		 * @return
		 */
		int[] getNextPossibleX() {
			return nextPossibleX;
		}

		/**
		 * 
		 * @param nextPossibleX
		 */
		void setNextPossibleX(int[] nextPossibleX) {
			this.nextPossibleX = nextPossibleX;
		}

		/**
		 * 
		 * @param width
		 */
		void setWidth(int width) {
			this.width = width;
		}
		
		/**
		 * 
		 * @param parents
		 * @param chbox
		 */
		void putChild(Set<String> parents, FragmentBox chbox) {
			children.put(parents, chbox);
		}

		/**
		 * 
		 *
		 */
		void clear() {
			nextPossibleX = new int[fraglayers.size()];
			fragToXPos.clear();
		}

		/**
		 * A fragment box for a wcc of a graph
		 */
		FragmentBox(Set<String> subgraph) {

			fragToXPos = new HashMap<String, Integer>();
			nextPossibleX = new int[fraglayers.size()];
			frags = subgraph;
			children = new HashMap<Set<String>, FragmentBox>();
		}

		/**
		 * Returns the FragmentBox a child of mine belongs to. Returns null if
		 * the fragment is no child of mine.
		 * 
		 * @param frag
		 * @return
		 */
		FragmentBox getBoxForFrag(String frag) {

			for (Set<String> rootSet : children.keySet()) {
				if (rootSet.contains(frag))
					return children.get(rootSet);
			}
			return null;
		}

		/**
		 * 
		 * @return
		 */
		List<String> getSortedFragments() {
			List<String> fraglist = new ArrayList<String>(frags);
			fraglist.retainAll(domgraph.getAllRoots());
			Collections.sort(fraglist, new FragmentXComparator());
			return fraglist;
		}

		/**
		 * 
		 * @author Michaela Regneri
		 * 
		 */
		private class FragmentXComparator implements Comparator<String> {

			/**
			 * 
			 */
			 public int compare(String arg0, String arg1) {
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
		int getBoxXPos(String frag) {
			return fragToXPos.get(frag);
		}

		/**
		 * 
		 * @param frag
		 * @param x
		 */
		void setBoxXPos(String frag, int x) {
			fragToXPos.put(frag, x);
		}

	}

}
