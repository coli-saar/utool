package de.saar.chorus.ubench.gui.chartviewer;

import java.awt.Color;
import java.awt.Font;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.GraphModel;
import org.jgraph.util.JGraphUtilities;

import de.saar.chorus.domgraph.chart.Split;
import de.saar.chorus.ubench.EdgeType;
import de.saar.chorus.ubench.Fragment;
import de.saar.chorus.ubench.JDomGraph;
import de.saar.chorus.ubench.NodeType;

public class FormatManager {
	
	// some constants
	private final static Color standardNodeForeground = Color.BLACK;
	
	private final static Color standardNodeBackground = Color.WHITE;
	
	private final static Font standardNodeFont = GraphConstants.DEFAULTFONT
	.deriveFont(Font.PLAIN, 17);
	
	private final static Font markedNodeFont = GraphConstants.DEFAULTFONT
	.deriveFont(Font.BOLD, 17);
	
	private final static Color standardSolidEdgeColor = Color.BLACK;
	
	private final static float standardSolidEdgeWidth = 1.7f;
	
	private final static Color standardDomEdgeColor = Color.RED;
	
	private final static float standardDomEdgeWidth = 1.2f;
	
	private final static float markedEdgeWidth = 2.5f;
	
	private final static Color deactivatedColor = Color.LIGHT_GRAY;
	
	
	private final static Color[] subgraphcolors = {
			Color.BLUE, new Color(163, 0, 163),
			new Color(255,153,51), new Color(255,51,51),
			Color.CYAN		
			};
	
	private static int subgraphcolorindex = 0;
	
	private final static Color rootcolor = new Color(0, 204, 51);
	
	/**
	 * 
	 * @param node
	 * @param color
	 */
	private static void markNode(DefaultGraphCell node, Color color,
			JDomGraph graph, Font font) {
		GraphConstants.setForeground(graph.getModel().getAttributes(node),
				color);
		GraphConstants.setFont(graph.getModel().getAttributes(node), font);
		
	}
	
	/**
	 * 
	 * @param edge
	 * @param color
	 */
	private static void markEdge(DefaultEdge edge, Color color, JDomGraph graph,
			float width) {
		GraphConstants
		.setLineColor(graph.getModel().getAttributes(edge), color);
		GraphConstants
		.setLineWidth(graph.getModel().getAttributes(edge), width);
		
	}
	
	/**
	 * 
	 * @param node
	 * @param b
	 */
	private static void unmarkNode(DefaultGraphCell node, JDomGraph graph) {
		
		GraphModel model = graph.getModel();
		
		GraphConstants.setForeground(model.getAttributes(node),
				standardNodeForeground);
		GraphConstants.setBackground(model.getAttributes(node),
				standardNodeBackground);
		GraphConstants.setFont(model.getAttributes(node), standardNodeFont);
	}
	
	/**
	 * 
	 * @param edge
	 * @param b
	 */
	private static void unmarkEdge(DefaultEdge edge, JDomGraph graph) {
		
		GraphModel model = graph.getModel();
		EdgeType type = graph.getEdgeData(edge).getType();
		boolean isSolid = type == EdgeType.solid;
		
		if (isSolid) {
			GraphConstants.setLineColor(model.getAttributes(edge),
					standardSolidEdgeColor);
			GraphConstants.setLineWidth(model.getAttributes(edge),
					standardSolidEdgeWidth);
		} else {
			GraphConstants.setLineColor(model.getAttributes(edge),
					standardDomEdgeColor);
			GraphConstants.setLineWidth(model.getAttributes(edge),
					standardDomEdgeWidth);
		}
		
	}
	
	/**
	 * 
	 * @param b
	 */
	public static void unmark(JDomGraph graph) {
		
		for (DefaultEdge edge : graph.getEdges()) {
			unmarkEdge(edge, graph);
		}
		for (DefaultGraphCell node : graph.getNodes()) {
			unmarkNode(node, graph);
		}
		
		refreshGraphLayout(graph);
	}
	
	public static void markSubgraph(Set<String> roots, JDomGraph graph) {
		markSubgraph(roots, subgraphcolors[subgraphcolorindex],
				graph, true);
		refreshGraphLayout(graph);
	}
	
	private static void markSubgraph(Set<String> roots, Color color, 
			JDomGraph graph, boolean shadeRemaining) {
		
		if(shadeRemaining) {
			shadeGraph(graph);
		}
		
		Set<Fragment> toMark = new HashSet<Fragment>();
		for (String otherNode : roots) {
			
			DefaultGraphCell gc = graph.getNodeForName(otherNode);
			if (graph.getNodeData(gc).getType() != NodeType.unlabelled) {
				Fragment frag = graph.findFragment(gc);
				toMark.add(frag);
			} else {
				markNode(gc, color, graph, markedNodeFont);
			}
			
			for (DefaultEdge edg : graph.getOutEdges(gc)) {
				
				if (graph.getEdgeData(edg).getType() == EdgeType.dominance) {
					markEdge(edg, color, graph, markedEdgeWidth);
					Fragment tgt = graph.getTargetFragment(edg);
					if (tgt != null) {
						toMark.add(tgt);
					}
				} else {
					markEdge(edg, color, graph, markedEdgeWidth);
				}
				
			}
		}
		
		for (Fragment frag : toMark) {
			for (DefaultGraphCell gc : frag.getNodes()) {
				markNode(gc, color, graph, markedNodeFont);
				for (DefaultEdge edg : graph.getOutEdges(gc)) {
					
					markEdge(edg, color, graph, markedEdgeWidth);
					
				}
			}
			
		}
		
	}
	
	private static void markGraph(Color color, JDomGraph graph) {
		for (DefaultGraphCell node : graph.getNodes()) {
			markNode(node, color, graph, markedNodeFont);
		}
		
		for (DefaultEdge edge : graph.getEdges()) {
			markEdge(edge, color, graph, markedEdgeWidth);
		}
		
	}
	
	
	
	private static void shadeGraph(JDomGraph graph) {
		for (DefaultGraphCell node : graph.getNodes()) {
			markNode(node, deactivatedColor, graph, standardNodeFont);
		}
		
		for (DefaultEdge edge : graph.getEdges()) {
			if(graph.getEdgeData(edge).getType() == EdgeType.solid) {
				markEdge(edge, deactivatedColor, graph, standardSolidEdgeWidth);
			} else {
				markEdge(edge, deactivatedColor, graph, standardDomEdgeWidth);
			}
		}
	}
	
	public static String markSplit(Split split, String splitAsString,
			JDomGraph graph) {
		
		StringBuffer coloredSplit = new StringBuffer();
	//	coloredSplit.append("<html>");
		
		shadeGraph(graph);
		
		Set<String> dominators = split.getAllDominators();
		String root = split.getRootFragment();
	//	coloredSplit.append("&lt;<div style='color:" + rootcolor.getRGB() + "font-face:bold'>" + root +"</div> \\{");
		
		
		if(!root.equals("")) {
			DefaultGraphCell rootNode = graph.getNodeForName(root);
			Fragment rootFrag = graph.findFragment(rootNode);
			for( DefaultGraphCell rfn : rootFrag.getNodes()) {
				markNode(rfn, rootcolor, graph, markedNodeFont);
				for( DefaultEdge edg : graph.getOutEdges(rfn) ) {
					markEdge(edg, rootcolor, graph, markedEdgeWidth);
				}
			}
		}
		
		for(String hole : dominators) {
			
			//jdg.markNode(jdg.getNodeForName(hole), colors.get(colorindex));
			List<Set<String>> wccs = split.getWccs(hole);
			for( Set<String> wcc : wccs) {
				wcc.add(hole);
				
	//			coloredSplit.append("<div style='color:'" + 
	//					subgraphcolors[subgraphcolorindex].getRGB() + 
	//					"font-face:bold'>" +hole + "=[" + wcc + "]");
								
				markSubgraph(wcc, subgraphcolors[subgraphcolorindex], 
						graph, false);
				if(++subgraphcolorindex == subgraphcolors.length) {
					subgraphcolorindex = 0;
				} 
			}
			
			subgraphcolorindex--;
			
			if(++subgraphcolorindex == subgraphcolors.length) {
				subgraphcolorindex = 0;
			} 
			
		} 
		subgraphcolorindex = 0;
		refreshGraphLayout(graph);
		return coloredSplit.toString();
		
	}
	
	private static void refreshGraphLayout(JDomGraph graph) {
		JGraphUtilities.applyLayout(graph, new JDomGraphDummyLayout(graph));
	}
	
}
