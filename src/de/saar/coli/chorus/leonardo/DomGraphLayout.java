/*
 * Created on 07.02.2005
 */
package de.saar.coli.chorus.leonardo;

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
	
	private JDomGraph graph; 
	private JGraph auxGraph;
	
	private DefaultGraphCell root;
	
	private Set<DefaultGraphCell> nodes;
	private Set<DefaultEdge> allEdges;
	private Set<Fragment> fragments;
	
	private Map<Fragment,Integer> fragXpos;
	private Map<Fragment,Integer> fragYpos;
	
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
	
	private boolean isFragLeaf(DefaultGraphCell node, Fragment frag) {
		
		if((children.get(node).size() == 0))
			return true;
		
		for(DefaultGraphCell child : children.get(node))
		{
			if(frag.getNodes().contains(child))
				return false;
		}
		return false;
	}
	
	
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
	
	
	private void computePositions(){
		
		DefaultGraphCell recRoot = null;
		
		
		for(Fragment frag : fragments){
			
			int recDep = 0;
			//note: that does not mean "depp", but "depth".
			//:-)
			
			int recX = 30;
			
			Set<DefaultGraphCell> toCompute = frag.getNodes();
			
			//chartNodes(frag.getEdges());
			
			recRoot = computeFragRoot(frag);
			
			Stack<DefaultGraphCell> restedNodes = new Stack<DefaultGraphCell>();
			
			while(! (toCompute.size() == 0)) {
				recDep++;
				for(DefaultGraphCell nd : frag.getChildren(recRoot)) {
					
					depth.put(nd, new Integer(recDep));
					
					restedNodes.push(nd);
					toCompute.remove(nd);
				}
			}
			
			
			
			
			
			
			parent.clear();
			children.clear();
		}
	}
	
	
	
	public void run(JGraph arg0, Object[] arg1, int arg2) {
		
		
		
	}
}
