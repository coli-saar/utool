/*
 * Created on 07.02.2005
 */
package de.saar.chorus.leonardo;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.*;

import javax.swing.undo.UndoableEdit;

import org.jgraph.JGraph;
import org.jgraph.graph.*;
import org.jgraph.layout.JGraphLayoutAlgorithm;
import org.jgraph.util.JGraphUtilities;

/**
 * @author Michaela
 *
 */
public class DomGraphLayout extends JGraphLayoutAlgorithm {
	
	//I don't know which of these I'll really need...
	
	private JDomGraph graph; 
	private JGraph auxGraph;
	
	private DefaultGraphCell root;
	
	private Set<DefaultGraphCell> nodes;
	private Set<DefaultEdge> allEdges;
	private Set<Fragment> fragments;
	
	private Map<Fragment,Integer> fragXpos;
	private Map<Fragment,Integer> fragYpos;
	
	private Map<Fragment,Integer> fragWidth;
	private Map<Fragment,Integer> fragHeight;
	
	private Map<DefaultGraphCell,DefaultGraphCell> parent;
	private Map<DefaultGraphCell,List<DefaultGraphCell>> children;
	
	
	/*
	 * NOTE: that will mark the middle of a node.
	 * a note is within a rectangle of 30 x 30,
	 * so the points will be calculated as follows:
	 * 
	 * x left bottom: point x - 15
	 * y left bottom: point y - 15
	 * 
	 * x right up: point x + 15
	 * y right up: point y + 15
	 * 
	 * may be kind of unaesthetic, but should work and
	 * prevents me of confusing the maps all the time :-).
	 */
	private Map<DefaultGraphCell,Integer> relXpos;
	private Map<DefaultGraphCell,Integer> relYpos;
	
	private Map<DefaultGraphCell,Integer> xPos;
	private Map<DefaultGraphCell,Integer> yPos;
	
	private Map<DefaultGraphCell,Integer> depth;
	
	
	public DomGraphLayout(JDomGraph gr) {
		this.graph = gr;
		
		auxGraph = new JGraph();
		root = new DefaultGraphCell();
		
		nodes = new HashSet<DefaultGraphCell>();
		allEdges = new HashSet<DefaultEdge>();
		
		fragments = new HashSet<Fragment>();
		
		fragXpos = new HashMap<Fragment, Integer>();
		fragYpos = new HashMap<Fragment, Integer>();
		
		fragWidth = new HashMap<Fragment, Integer>();
		fragHeight = new HashMap<Fragment, Integer>();
		
		relXpos = new HashMap<DefaultGraphCell,Integer>();
		relYpos = new HashMap<DefaultGraphCell,Integer>();
		
		xPos = new HashMap<DefaultGraphCell,Integer>();
		yPos = new HashMap<DefaultGraphCell,Integer>();
		
		depth = new HashMap<DefaultGraphCell,Integer>();
		
	}
	
	
	/*
	  
	 private void chartNodes(Set<DefaultEdge> edges){
		HashSet<DefaultGraphCell> visitedPar = new HashSet<DefaultGraphCell>();
		HashSet<DefaultGraphCell> visitedChi = new HashSet<DefaultGraphCell>();
		
		for(DefaultEdge edg : edges){
			DefaultGraphCell src = (DefaultGraphCell) JGraphUtilities.getSourceVertex(graph, edg);
			DefaultGraphCell tgt = (DefaultGraphCell) JGraphUtilities.getTargetVertex(graph, edg);
			
			if(! visitedPar.contains(src)){
				visitedPar.add(src);
				visitedChi.add(src);
				
				List<DefaultGraphCell> newChild = new ArrayList<DefaultGraphCell>();
				newChild.add(tgt);
				children.put(src, newChild);
				
			} else {
				children.get(src).add(tgt);
			}
			
			if(!visitedChi.contains(tgt)){
				visitedChi.add(tgt);
				
				parent.put(tgt,src);
				
				visitedPar.add(tgt);
				List<DefaultGraphCell> stillLeaf = new ArrayList<DefaultGraphCell>();
				children.put(tgt, stillLeaf);
				
			} else {
				parent.put(tgt,src);
			}
		}
	} */
	
//	private boolean isFragLeaf(DefaultGraphCell node, Fragment frag) {
//		
//		if((children.get(node).size() == 0))
//			return true;
//		
//		for(DefaultGraphCell child : children.get(node))
//		{
//			if(frag.getNodes().contains(child))
//				return false;
//		}
//		return false;
//	}
	
	
	private DefaultGraphCell computeFragRoot(Fragment frag) {
		
		DefaultGraphCell recentRoot = null;
		
		ArrayList visited = new ArrayList();
		
		for(DefaultEdge edg : frag.getEdges()){
			
			recentRoot =
				(DefaultGraphCell) JGraphUtilities.getSourceVertex(graph, edg);
			
			if((parent.get(recentRoot) == null) || 
					(! frag.getNodes().contains(parent.get(recentRoot)))) {
				depth.put(recentRoot, new Integer(0));
				return recentRoot;
			} else continue;
		}
		
		return recentRoot;
	}
	
	/*
	 * Note: I know this method does not fit in here.
	 * It's just easier to define it here...
	 */
	private int getMaxDepth(Fragment frag) {
		int dep = 0;
		
		for(DefaultGraphCell node : frag.getNodes()) {
			if(dep < depth.get(node).intValue())
				dep = depth.get(node).intValue();
		}
		
		return dep;
		
	}
	
	/**
	 * computes the relative y-positions of a
	 * fragment node by its depth.
	 * NOTE: can only be used after "computeXPositions"
	 * because the depth is computed there.
	 *
	 */
	private void computeYPositions(){
		for(Fragment frag : fragments) {
			int maxDep = getMaxDepth(frag);
			
			for(DefaultGraphCell node : frag.getNodes()){
				int nodeDep = depth.get(node).intValue();
				
				//60: a node's height is 30, so there should
				//be at least one "node" space - perhaps 
				//more, to be tested.
				int yVal = (maxDep - nodeDep) * 60; 
				relYpos.put(node, new Integer(yVal));
			}
		}
	}
	
	private void computeXPositions(){
		
		DefaultGraphCell recRoot = null;
		
		
		for(Fragment frag : fragments){
			
			int recDep = 0;
			//note: that does not mean "depp", but "depth".
			//:-)
			
			int recX = 30;
			
			Set<DefaultGraphCell> toCompute = frag.getNodes();
			
			recRoot = computeFragRoot(frag);
			toCompute.remove(recRoot);
			
			Stack<DefaultGraphCell> restedNodes = new Stack<DefaultGraphCell>();
			restedNodes.push(recRoot);
			
			//compute the leafs positions.
			for(DefaultGraphCell lf : frag.getLeafs()) {
				relXpos.put(lf, new Integer(recX));
				recX += 60;
			}
			
			fragWidth.put(frag, new Integer(recX + 30));
			
			
			/*
			 * put all nodes in a stack.
			 * if the fragment consists of just one 
			 * node, the loop won't start, so we 
			 * don't need to care about this case.
			 */
			while(! (toCompute.size() == 0)) {
				recDep++;
				
				/*
				 * so our root has at least one children.
				 */
				for(DefaultGraphCell nd : frag.getChildren(recRoot)) {
					
					//remembering the depth.
					depth.put(nd, new Integer(recDep));
					
					//in this order, a node's children should be
					//above the node itself... 
					restedNodes.push(nd);
					
					/*
					 * If I can rely on the order in the nodes-array,
					 * this could work.
					 * I guess it won't, but I leave it until I find
					 * (means: understand) something better.
					 */
					for(DefaultGraphCell cl : toCompute){
						if(frag.isLeaf(cl)){
							toCompute.remove(cl);
						} else {
							recRoot = cl;
							break;
						}
					}
				}
			}
			
			/*
			 * if it works as it is supposed to, every node
			 * is preceded by its children.
			 * so the recursion should work... 
			 */
			while(! restedNodes.empty()) {
				DefaultGraphCell recNode = restedNodes.pop();
				
				if(frag.isLeaf(recNode)) 
					continue;
				else {
					int nodesX = 0;
					/*
					 * if a node is no leaf, it should have at least
					 * one child...
					 */
					for(DefaultGraphCell chld : frag.getChildren(recNode))
					{
						nodesX += relXpos.get(chld).intValue();
					}
					
					//position in the middle of its children.
					nodesX = (int) nodesX/(frag.getChildren(recNode).size());
					relXpos.put(recNode, new Integer(nodesX));
				}
			}
			
			
			
			
		}
		
		
	}
	
	
	
	
	public void run(JGraph arg0, Object[] arg1, int arg2) {
		
		
		
	}
}
