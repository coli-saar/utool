package de.saar.chorus.domgraph.layout;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgraph.graph.AttributeMap;
import org.jgraph.graph.CellView;
import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.GraphConstants;
import org.jgraph.util.JGraphUtilities;

import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.ubench.EdgeData;
import de.saar.chorus.ubench.EdgeType;
import de.saar.chorus.ubench.Fragment;
import de.saar.chorus.ubench.JDomGraph;
import de.saar.chorus.ubench.NodeData;
import de.saar.chorus.ubench.NodeType;

public class JDomGraphCanvas implements Canvas {

	JDomGraph jdomgraph;
	NodeLabels labels;
	Map<DefaultGraphCell, AttributeMap> viewMap;
	Map<DefaultGraphCell, Fragment> nodeToFragment;
	Set<Fragment> fragments;
	Map<DefaultEdge, List<DefaultGraphCell>> treeEdges;
	boolean fragmentscomputed;
	
	public JDomGraphCanvas(JDomGraph graph) {
		jdomgraph = graph;
		viewMap = new HashMap<DefaultGraphCell, AttributeMap>();
		nodeToFragment = new HashMap<DefaultGraphCell, Fragment>();
		fragments = new HashSet<Fragment>();
		treeEdges = new HashMap<DefaultEdge, List<DefaultGraphCell>>();
		fragmentscomputed = false;
	}
	
	
	
	public void drawDominanceEdge(String src, String tgt) {
		
		if(! fragmentscomputed ) {
			computeFragments();
			
//			 insert fragment cells into the graph.
			for( Fragment frag : fragments ) {
				jdomgraph.addFragment(frag);
			}
			fragmentscomputed = true;
		}
		
		DefaultGraphCell source =
			jdomgraph.getNodeForName(src);
		DefaultGraphCell target = 
			jdomgraph.getNodeForName(tgt);
		
		if(JGraphUtilities.getEdgesBetween(jdomgraph, source, target).length == 0 ) {
			EdgeData data =  new EdgeData(EdgeType.dominance, src + " --> " + tgt, jdomgraph );
			jdomgraph.addEdge(data, source, target);
		}

	}

	public void drawNodeAt(int x, int y, String nodename, 
			String label, de.saar.chorus.domgraph.graph.NodeData data, String show) {
		
		DefaultGraphCell node = jdomgraph.getNodeForName(nodename);
		NodeData nodedata;
		if(node == null) {
			NodeType type;

			if(data.getType().equals(de.saar.chorus.domgraph.graph.NodeType.UNLABELLED) ) {
				type = NodeType.unlabelled;
				nodedata = new NodeData(type, nodename, jdomgraph);
			} else {
				type = NodeType.labelled;
				nodedata = new NodeData(type, 
						nodename, label, jdomgraph);
			}
			nodedata.setShowLabel(jdomgraph.getLabeltype());
			node = jdomgraph.addNode(nodename, nodedata);
		} 
		
		CellView view = jdomgraph.getGraphLayoutCache().getMapping(node, false);
		Rectangle2D rect = (Rectangle2D) view.getBounds().clone();
		Rectangle bounds = new Rectangle((int) rect.getX(), (int) rect.getY(),
				(int) getNodeWidth(show), (int) rect.getHeight());

		bounds.x = x;
		bounds.y = y;
		
		

		AttributeMap map = new AttributeMap();
			//jdomgraph.getModel().getAttributes(node);
		GraphConstants.setBounds(map, (Rectangle2D) bounds.clone());

		viewMap.put(node, map);
		jdomgraph.getGraphLayoutCache().edit(viewMap, null, null, null);
		
		
		
	}
	
	private void buildElementaryFragment(DefaultGraphCell node) {

		Fragment f = new Fragment(jdomgraph);
		f.add(node);
		fragments.add(f);
		nodeToFragment.put(node, f);
	}
	
	private void computeFragments() {
		for(DefaultEdge edge : treeEdges.keySet()) {
			List<DefaultGraphCell> pair = treeEdges.get(edge);
			DefaultGraphCell source =
				pair.get(0);

			DefaultGraphCell target =
				pair.get(1);

			if(! nodeToFragment.containsKey(source)) {
				buildElementaryFragment(source);
			}

			if(! nodeToFragment.containsKey(target)) {
				buildElementaryFragment(target);
			}


			Fragment sFrag = nodeToFragment.get(source);
			Fragment tFrag = nodeToFragment.get(target);

			if( sFrag.size() > tFrag.size() ) {
				mergeInto( sFrag, tFrag );

				sFrag.add(edge);
			} else {
				mergeInto( tFrag, sFrag );

				tFrag.add(edge);
			}
		}
	}

	public void drawTreeEdge(String src, String tgt) {
		
		DefaultGraphCell source =
			jdomgraph.getNodeForName(src);
		DefaultGraphCell target = 
			jdomgraph.getNodeForName(tgt);
		
		if( JGraphUtilities.getEdgesBetween(jdomgraph, source, target).length == 0) {
			EdgeData data =  new EdgeData(EdgeType.solid, src + " --> " + tgt, jdomgraph );
			DefaultEdge edge = jdomgraph.addEdge(data, source, target);
			
			List<DefaultGraphCell> pair = new ArrayList<DefaultGraphCell>();
			pair.add(source);
			pair.add(target);
			treeEdges.put(edge, pair);
		}
		
		
	}

	public int getNodeHeight(String label) {
		/*
		 * This has been the standard value for JDomGraph objects.
		 * TODO think about something more elegant here...
		 */
		return 30;
	}

	public int getNodeWidth(String label) {
		
	
		Graphics g = jdomgraph.getGraphics();
    	
        int ret = 30; // a default node width
        
        if( (g != null ) &&  !"".equals(label) ) {
        	g.setFont(jdomgraph.getUpperBoundFont());
        	FontMetrics metrics = g.getFontMetrics();
            double trueWidth = metrics.stringWidth(label);
            if( trueWidth > ret ) {
                ret = (int) trueWidth;
            }
        }
      
        return ret;
	}

	public void drawLightDominanceEdge(String src, String tgt) {
	
		
		if(! fragmentscomputed ) {
			computeFragments();
			
//			 insert fragment cells into the graph.
			for( Fragment frag : fragments ) {
				jdomgraph.addFragment(frag);
			}
			fragmentscomputed = true;
		}
		
		DefaultGraphCell source = jdomgraph.getNodeForName(src);
		DefaultGraphCell target = jdomgraph.getNodeForName(tgt);
		
		DefaultEdge edge = new DefaultEdge();
		
		if( JGraphUtilities.getEdgesBetween(jdomgraph, source, target).length == 0) {
			EdgeData data =  new EdgeData(EdgeType.dominance, src + " --> " + tgt, jdomgraph );
			edge = jdomgraph.addEdge(data, source, target);
		} else {
			for(DefaultEdge edg : jdomgraph.getOutEdges(source)) {
				if(jdomgraph.getTargetNode(edg).equals(target)) {
					edge = edg;
				}
			}
		}
		
		GraphConstants.setLineColor(jdomgraph.getModel()
				.getAttributes(edge), new Color(255, 204, 230));
		
	}
	
	public JDomGraph getGraph() {
		return jdomgraph;
	}
	
	private void mergeInto(Fragment into, Fragment from ) {
		into.addAll(from);
		
		for( DefaultGraphCell node : from.getNodes() ) {
			nodeToFragment.remove(node);
			nodeToFragment.put(node, into);
		}
		
		fragments.remove(from);
	}
	
	
	
	public void finish() {
		jdomgraph.getGraphLayoutCache().edit(viewMap, null, null, null);
		
	//	JGraphUtilities.applyLayout(jdomgraph, new JDomGraphDummyLayout(jdomgraph));
		//jdomgraph.setAntiAliased(true);
		jdomgraph.validate();
	}

}
