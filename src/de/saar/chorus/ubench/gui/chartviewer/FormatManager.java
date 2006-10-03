package de.saar.chorus.ubench.gui.chartviewer;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
	
	private static Map<Split,String> splitToMarkedHTML = 
		new HashMap<Split,String>();
	
	private static Map<Set<String>,String> subgraphToMarkedHTML = 
		new HashMap<Set<String>,String>();	
	
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
	
	public static void markSubgraph(Set<String> roots, JDomGraph graph, int subgraphindex) {
		int color = subgraphindex % subgraphcolors.length;
		
		markSubgraph(roots, subgraphcolors[color], graph, false);
	}
	
	public static void markSubgraph(Set<String> roots, JDomGraph graph) {
		
		markSubgraph(roots, subgraphcolors[subgraphcolorindex],
				graph, true);
		
		refreshGraphLayout(graph);
		
	}
	
	public static String getHTMLforMarkedSubgraph(Set<String> subgraph) {
		if( subgraphToMarkedHTML.containsKey(subgraph) ) {
			return subgraphToMarkedHTML.get(subgraph);
		} else {
			String htmlsubgraph = "<html><font color=\"#" + 
			Integer.toHexString(subgraphcolors[subgraphcolorindex].getRGB()).substring(2) +
			"\"><b>" + subgraph + "</b></font></html>";
			
			subgraphToMarkedHTML.put(subgraph, htmlsubgraph);
			return htmlsubgraph;
		}
	}
	
	public static String getHTMLforMarkedSplit(Split split, Set<String> roots) {
		
		if( splitToMarkedHTML.containsKey(split) ) {
			return splitToMarkedHTML.get(split);
		} else {
			
			StringBuffer htmlsplit = new StringBuffer();
			
			htmlsplit.append("<html><b>&lt;<font color=\"#" + 
					Integer.toHexString(rootcolor.getRGB()).substring(2)
					+"\">" + split.getRootFragment() + "</font> ");
			
			boolean firsthole = true;
			for (String hole : split.getAllDominators()) {
				if(firsthole) {
					firsthole = false;
				} else {
					htmlsplit.append(", ");
				}
				htmlsplit.append("<font color=\"#" + 
						Integer.toHexString(subgraphcolors[subgraphcolorindex].getRGB()).substring(2)
						+ "\">" +hole +"=[</font>");
				List<Set<String>> x = new ArrayList<Set<String>>();
				
				boolean firstsubgraph = true;
				for (Set<String> wcc : split.getWccs(hole)) {
					if(firstsubgraph) {
						firstsubgraph = false;
					} else {
						htmlsplit.append(", ");
					}
					Set<String> copy = new HashSet<String>(wcc);
					copy.retainAll(roots);
					htmlsplit.append("<font color=\"#" + 
							Integer.toHexString(subgraphcolors[subgraphcolorindex].getRGB()).substring(2)
							+ "\">" + copy +"</font>");
					x.add(copy);
					if( ++subgraphcolorindex >= subgraphcolors.length) {
						subgraphcolorindex = 0;
					}
				}
				subgraphcolorindex--;
				htmlsplit.append("<font color=\"#" + 
						Integer.toHexString(subgraphcolors[subgraphcolorindex].getRGB()).substring(2)
						+ "\">]</font>");
				if( ++subgraphcolorindex >= subgraphcolors.length) {
					subgraphcolorindex = 0;
				}
			}
			
			
			htmlsplit.append("&gt;");
			htmlsplit.append("</b></html>");
			subgraphcolorindex = 0;
			
			splitToMarkedHTML.put(split,htmlsplit.toString());
			return htmlsplit.toString();
		}
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
	
	public static void markGraph(Color color, JDomGraph graph) {
		for (DefaultGraphCell node : graph.getNodes()) {
			markNode(node, color, graph, markedNodeFont);
		}
		
		for (DefaultEdge edge : graph.getEdges()) {
			markEdge(edge, color, graph, markedEdgeWidth);
		}
		
		refreshGraphLayout(graph);
	}
	
	
	
	public static void shadeGraph(JDomGraph graph) {
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
	
	public static void markRootFragment(Fragment root, JDomGraph graph) {
		for( DefaultGraphCell rfn : root.getNodes()) {
			markNode(rfn, rootcolor, graph, markedNodeFont);
			for( DefaultEdge edg : graph.getOutEdges(rfn) ) {
				markEdge(edg, rootcolor, graph, markedEdgeWidth);
			}
		}
	}
	
	public static void refreshGraphLayout(JDomGraph graph) {
		JGraphUtilities.applyLayout(graph, new JDomGraphDummyLayout(graph));
	}
	
}