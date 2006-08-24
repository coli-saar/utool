package de.saar.chorus.ubench.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphCell;

import de.saar.chorus.domgraph.chart.Chart;
import de.saar.chorus.domgraph.chart.Split;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.ubench.EdgeType;
import de.saar.chorus.ubench.Fragment;
import de.saar.chorus.ubench.JDomGraph;
import de.saar.chorus.ubench.NodeType;

/**
 * A <code>JFrame</code> containign a GUI for visualising a 
 * <code>Chart</code> of a dominance graph and 
 * highlighting parts of it in a <code>JDomGraph</code>.
 * 
 * @see de.saar.chorus.domgraph.chart.Chart
 * @see de.saar.chorus.ubench.JDomGraph
 * @author Michaela Regneri
 * @author Alexander Koller
 *
 */
public class ChartViewer extends JFrame implements CaretListener {

	private JTextPane prettyprint; // Component for the text representation

	private Chart chart; // the chart itself

	private DomGraph dg; // the graph belonging to the chart

	private boolean splitMarked; // indicates whether or not a split is currently highlighted

	// redefining some colors
	private Color myGreen;

	private Color purple;

	/**
	 * A new ChartViewer 
	 * 
	 * @param c
	 * @param g
	 * @param title
	 */
	ChartViewer(Chart c, DomGraph g, String title) {
		// some initialising
		super("Chart of " + title);
		chart = c;
		dg = g;
		splitMarked = false;
		myGreen = new Color(0, 204, 51);
		purple = new Color(163, 0, 163);
		prettyprint = new JTextPane();
		prettyprint.addCaretListener(this);

		prettyprint.setContentType("text/html");

		/*
		 * the label indicating the (only) 
		 * functinality.
		 * If we add more functionality, this should become
		 * a menu.
		 */
		JLabel instruction = new JLabel(
				"Mark a split to highlight it in the graph window.");

		// computing the String representation of 
		// the chart.
		String textchart = chartOnlyRootsHTML();
		StringBuffer htmlprint = new StringBuffer();

		textchart = textchart.replace("[", "{");
		textchart = textchart.replace("]", "}");

		htmlprint.append(textchart);
		prettyprint.setText(htmlprint.toString());
		prettyprint.setEditable(false);

		// layout
		add(instruction, BorderLayout.NORTH);
		add(new JScrollPane(prettyprint), BorderLayout.CENTER);
		Dimension preferred = new Dimension((int) (Ubench.getInstance()
				.getTabWidth() / 1.5), (int) Ubench.getInstance()
				.getTabHeight());
		setPreferredSize(preferred);
		//TODO perhaps this isn't such a good idea...
		setAlwaysOnTop(true);
		pack();
		validate();
		setVisible(true);
	}

	private String chartOnlyRootsHTML() {
		StringBuffer ret = new StringBuffer();
		Set<String> roots = dg.getAllRoots();
		Set<Set<String>> visited = new HashSet<Set<String>>();
		ret
				.append("<html><font face=\"Arial, Arial Black\" color=\"aqua\"><table border=\"0\">");
		for (Set<String> fragset : chart.getToplevelSubgraphs()) {
			ret.append(corSubgraph(fragset, roots, visited));
		}
		ret.append("</table></font></html>");
		return ret.toString();
	}

	private String corSubgraph(Set<String> subgraph, Set<String> roots,
			Set<Set<String>> visited) {
		Set<String> s = new HashSet<String>(subgraph);
		StringBuffer ret = new StringBuffer();
		boolean first = true;
		String whitespace = "<td></td><td></td>";
		Set<Set<String>> toVisit = new HashSet<Set<String>>();

		if (!visited.contains(subgraph)) {
			visited.add(subgraph);

			s.retainAll(roots);
			String sgs = s.toString();

			if (chart.getSplitsFor(subgraph) != null) {
				ret.append("<tr>" + sgs + " <td>&#8594;</td><td> ");
				for (Split split : chart.getSplitsFor(subgraph)) {
					if (first) {
						first = false;
					} else {
						ret.append(whitespace);
					}

					ret.append(corSplit(split, roots) + "</td></tr>");
					toVisit.addAll(split.getAllSubgraphs());
				}

				for (Set<String> sub : toVisit) {
					ret.append(corSubgraph(sub, roots, visited));
				}
			}

			return ret.toString();
		} else {
			return "";
		}
	}

	private String corSplit(Split split, Set<String> roots) {
		StringBuffer ret = new StringBuffer("&lt;" + split.getRootFragment());
		Map<String, List<Set<String>>> map = new HashMap<String, List<Set<String>>>();

		for (String hole : split.getAllDominators()) {
			List<Set<String>> x = new ArrayList<Set<String>>();
			map.put(hole, x);

			for (Set<String> wcc : split.getWccs(hole)) {
				Set<String> copy = new HashSet<String>(wcc);
				copy.retainAll(roots);
				x.add(copy);
			}
		}

		ret.append(" " + map);
		ret.append("&gt;");
		return ret.toString();
	}

	/**
	 * 
	 */
	public void caretUpdate(CaretEvent e) {
		String marked = prettyprint.getSelectedText();

		// a split is selected
		if ((marked != null) && marked.matches("[ \t\n\f\r]*<.*>")) {
			Ubench.getInstance().getVisibleTab().getGraph().setMarked(false);
			splitMarked = true;

			// retrieving the split's nodes
			StringTokenizer tok = new StringTokenizer(marked, " {},=<>\t\n\f\r");
			String root;
			List<String> remainingNodes = new ArrayList<String>();
			if (tok.countTokens() > 0) {
				root = tok.nextToken();
				while (tok.hasMoreTokens()) {
					remainingNodes.add(tok.nextToken());
				}

				// TODO move the following anywhere else (Tab?)
				// changing the color of nodes and edges
				JDomGraph graph = Ubench.getInstance().getVisibleTab()
						.getGraph();

				DefaultGraphCell rootNode = graph.getNodeForName(root);
				for (DefaultGraphCell rfn : graph.findFragment(rootNode)
						.getNodes()) {
					graph.markNode(rfn, myGreen);
				}
				for (DefaultEdge edg : graph.getOutEdges(rootNode)) {
					graph.markEdge(edg, myGreen);

				}

				// the prettyprint contains only roots, so we have
				// to retrieve the other nodes of the fragments to
				// mark.
				Set<Fragment> toMark = new HashSet<Fragment>();

				for (String otherNode : remainingNodes) {
					DefaultGraphCell gc = graph.getNodeForName(otherNode);
					if (graph.getNodeData(gc).getType() != NodeType.unlabelled) {
						Fragment frag = graph.findFragment(gc);
						toMark.add(frag);
					} else {
						graph.markNode(gc, Color.blue);
					}

					for (DefaultEdge edg : graph.getOutEdges(gc)) {

						if (graph.getEdgeData(edg).getType() == EdgeType.dominance) {
							graph.markEdge(edg, purple);
							Fragment tgt = graph.getTargetFragment(edg);
							if (tgt != null) {
								toMark.add(tgt);
							}
						} else {
							graph.markEdge(edg, Color.blue);
						}

					}
				}

				for (Fragment frag : toMark) {
					for (DefaultGraphCell gc : frag.getNodes()) {
						graph.markNode(gc, Color.blue);
						for (DefaultEdge edg : graph.getOutEdges(gc)) {
							if (graph.getEdgeData(edg).getType() == EdgeType.dominance) {
								graph.markEdge(edg, purple);

							} else {
								graph.markEdge(edg, Color.blue);
							}
						}
					}

				}

				graph.computeLayout();
				graph.adjustNodeWidths();
				graph.setMarked(true);

			}
		} else {
			if (splitMarked) {
				Ubench.getInstance().getVisibleTab().getGraph()
						.setMarked(false);
				splitMarked = false;
			}
		}

	}

	/**
	 * This overrides the "setVisible" method to 
	 * make sure that the highlighting dissapperas when
	 * this window is closed.
	 * 
	 * @Override
	 * 
	 */
	public void setVisible(boolean b) {
		super.setVisible(b);
		Ubench.getInstance().getVisibleTab().getGraph().setMarked(false);
	}

}
