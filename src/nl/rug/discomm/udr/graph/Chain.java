package nl.rug.discomm.udr.graph;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org._3pq.jgrapht.Edge;

import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.EdgeType;
import de.saar.chorus.domgraph.graph.NodeData;
import de.saar.chorus.domgraph.graph.NodeType;

public class Chain extends DomGraph {
	
	int lastIndex, edgeIndex;
	
	public Chain() {
		super();
		lastIndex = 0;
		edgeIndex = 0;
	}
	
	public Chain(int n) {
		super();
		edgeIndex= 0;
		makeChain(n);
		lastIndex = n;
		
	}
	
	
	public int getLength() {
		return lastIndex;
	}
	public boolean addWeightedDominanceEdge(String src, String tgt, double weight) {
		
		Set<Edge> toDelete = new HashSet<Edge>();
		if(isAllowedEdge(src,tgt,weight,toDelete)) {
			for(Edge edge : toDelete) {
				remove(edge);
			}
			
			int nextedge = ++edgeIndex;
			addEdge(src,tgt,new EdgeData(weight,nextedge));
			int sindex = getHoleIndex(src);
			int tindex = getRootIndex(tgt);
			
			// additional DomEdges to the "unfree" fragments, but all 
			// with the same index.
			// TODO what happens with doubled edges here?  
			if(sindex < tindex) {
				for(int i = sindex +1; i < tindex; i++) {
					String nexttgt = i +"x";
					addEdge(src, nexttgt, new EdgeData(weight,nextedge));
				}
			} else {
				for( int i = tindex +1; i < sindex; i++) {
					addEdge(src, i +"x", new EdgeData(weight,nextedge));
				}
			}
			return true;
		}
		
		return false;
	}
	
	
	public boolean addDominanceEdge(String src, String tgt) {
		Set<Edge> debugEdges = new HashSet<Edge>();
		if(! isAllowedEdge(src,tgt,1,debugEdges)) {
			return false;
		} else {
			addEdge(src,tgt, new EdgeData(EdgeType.DOMINANCE, ++edgeIndex));
		}
		return true;
	}
	
	private int getHoleIndex(String hole) {
		return Integer.parseInt(hole.substring(0, hole.length() - 2));
	}
	
	private int getRootIndex(String root) {
		return Integer.parseInt(root.substring(0,root.length()-1));
	}
	
	private boolean isAllowedEdge(String src, String tgt, double weight, Set<Edge> delete) {
		
		boolean left = src.endsWith("l");
		int s = getHoleIndex(src);
		int t = getRootIndex(tgt);
		
		if(left && s <= t) {
			return false;
		}
		if((! left) && s > t) {
			return false;
		}
		
		// last possibility: the edge is not allowed because of another dominance edge
		List<Edge> inEdges = getInEdges(getRoot(src), EdgeType.DOMINANCE);
		Set<Integer> visited = new HashSet<Integer>();
		double counterweight = 1;
		
		/*
		 * Compute the weight indicating how likely it is, that the restrictive edges 
		 * do not hold. The product of all Gegenwahrscheinlichkeiten of the edge weights
		 * tells us how likely the new edge is not forbidden. 
		 * Edges with the same index result from the same dominance edge and do not count
		 * more than once.
		 */
		for(Edge e : inEdges) {
			String parent = (String) e.getSource();
			EdgeData data = ((nl.rug.discomm.udr.graph.EdgeData) getData(e));
			int parindex = getHoleIndex(parent);
		
			if(! visited.contains(data.getIndex())) {
				visited.add(data.getIndex());
				if( (left && parindex < s) ||
						((!left) && parindex > s) ) {
					double w = data.getWeight();
					if(w == 1) {
						// for contradictory edges with weight 1 - first come, first served...
						return false;
					} else {
						delete.add(e);
						for(Edge edge : getOutEdges(parent, EdgeType.DOMINANCE)) {
							EdgeData pdata = ((nl.rug.discomm.udr.graph.EdgeData) getData(edge));
							if(pdata.getIndex() == data.getIndex()) {
								delete.add(edge);
							}
						}
						counterweight = counterweight * (1-w);
					}
				}
			}
			
			
		}
		
		if( (counterweight == 1) || (counterweight > (1-weight))) {
			return true;
		}
		
		
		return false;
	}
	
	public void addFragment() {
		
		if(lastIndex > 0) {
		lastIndex++;
		
    	
    	String upper_root =  lastIndex + "x";
		String upper_lefthole = lastIndex + "xl";
		String upper_righthole = lastIndex + "xr";
		
		addNode(upper_root, new NodeData(NodeType.LABELLED));
		
		addNode(upper_lefthole, new NodeData(NodeType.UNLABELLED));
		addNode(upper_righthole, new NodeData(NodeType.UNLABELLED));
		
		addEdge(upper_root, upper_lefthole, new EdgeData(EdgeType.TREE, ++edgeIndex));
		addEdge(upper_root, upper_righthole, new EdgeData(EdgeType.TREE, ++edgeIndex));
		
		// dominance edge to previous lower fragment
		addEdge(upper_lefthole,  (lastIndex -1)+"y", new EdgeData(EdgeType.DOMINANCE, ++edgeIndex));
		
		// lower fragment
		String lower =  lastIndex + "y";
		addNode(lower, new NodeData(NodeType.LABELLED));
		
		// dominance edge to new lower fragment
    	addEdge(upper_righthole, lower, new EdgeData(EdgeType.DOMINANCE, ++edgeIndex));
		} else {
			makeChain(1);
			lastIndex++;
		}
	}
	
	
	
	
	private void makeChain(int length) {
    	String upper_root, upper_lefthole, upper_righthole;
    	String lower;
        
        
        clear();
    	
    	lower = "0y";
    	addNode("0y", new NodeData(NodeType.LABELLED));
    	
    	
    	for( int i = 1; i <= length; i++ ) {
    		// upper fragment
    		upper_root =  i +"x";
    		upper_lefthole =  i + "xl";
    		upper_righthole =  i + "xr";
    		
    		addNode(upper_root, new NodeData(NodeType.LABELLED));
    		
    		addNode(upper_lefthole, new NodeData(NodeType.UNLABELLED));
    		addNode(upper_righthole, new NodeData(NodeType.UNLABELLED));
    		
    		addEdge(upper_root, upper_lefthole, new EdgeData(EdgeType.TREE, ++edgeIndex));
    		addEdge(upper_root, upper_righthole, new EdgeData(EdgeType.TREE, ++edgeIndex));
    		
    		// dominance edge to previous lower fragment
    		addEdge(upper_lefthole, lower, new EdgeData(EdgeType.DOMINANCE, ++edgeIndex));
    		
    		// lower fragment
    		lower =  i + "y";
    		addNode(lower, new NodeData(NodeType.LABELLED));
    		
    		// dominance edge to new lower fragment
        	addEdge(upper_righthole, lower, new EdgeData(EdgeType.DOMINANCE, ++edgeIndex));
    	}
    }

}
