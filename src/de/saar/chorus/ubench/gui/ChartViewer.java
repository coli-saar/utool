package de.saar.chorus.ubench.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphCell;

import de.saar.chorus.domgraph.chart.Chart;
import de.saar.chorus.domgraph.chart.Split;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.ubench.Fragment;
import de.saar.chorus.ubench.JDomGraph;

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
public class ChartViewer extends JFrame implements ActionListener {
	
	private JPanel prettyprint; // Component for the text representation
	
	private Chart chart; // the chart itself
	
	private DomGraph dg; // the graph belonging to the chart
	private JDomGraph jdg; // the Graph to highlight the nodes in
	
	private boolean splitMarked; // indicates whether or not a split is currently highlighted
	
	// redefining some colors
	private Color myGreen;
	
	private Color purple;
	
	private Color redbrown;
	
	private Color lightbrown;
	
	private ButtonGroup radioButtons;
	
	/**
	 * A new ChartViewer 
	 * 
	 * @param c
	 * @param g
	 * @param title
	 */
	ChartViewer(Chart c, DomGraph g, String title, JDomGraph jg) {
		// some initialising
		super("Chart of " + title);
		chart = c;
		dg = g;
		jdg = jg;
		splitMarked = false;
		myGreen = new Color(0, 204, 51);
		purple = new Color(163, 0, 163);
		lightbrown = new Color(255,153,51);
		redbrown = new Color(255,51,51);
		
		GridLayout layout = new GridLayout(0,2);
		prettyprint = new JPanel(layout);
		radioButtons = new ButtonGroup();
		
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
		
		
		// layout
		add(instruction, BorderLayout.NORTH);
		add(new JScrollPane(prettyprint), BorderLayout.CENTER);
		Dimension preferred = new Dimension((int) prettyprint.getPreferredSize().width, 
				(int) (Ubench.getInstance().getTabHeight() * 0.75));
		setPreferredSize(preferred);
		
		//TODO perhaps this isn't such a good idea...
		setAlwaysOnTop(true);
		setLocationRelativeTo(Ubench.getInstance().getWindow());
		pack();
		validate();
		setVisible(true);
	}
	
	private String chartOnlyRootsHTML() {
		StringBuffer ret = new StringBuffer();
		Set<String> roots = dg.getAllRoots();
		Set<Set<String>> visited = new HashSet<Set<String>>();
		ret.append("<html><table border=\"0\" style='font-family:Arial; font-size:12pt; color:#000000'>");
		for (Set<String> fragset : chart.getToplevelSubgraphs()) {
			ret.append(corSubgraph(fragset, roots, visited));
		}
		ret.append("</table></html>");
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
				JTextPane nextSubgraph = new JTextPane();
				nextSubgraph.setContentType("text/html");
				nextSubgraph.setEditable(false);
				prettyprint.add(nextSubgraph);
				
				nextSubgraph.setText("<html><div style='font-family:Arial; font-size:13pt; color:#000000'>" +sgs + "  &#8594;</div></html>");
				ret.append("<tr>" + sgs + " <td>&#8594;</td><td> ");
				for (Split split : chart.getSplitsFor(subgraph)) {
					if (first) {
						first = false;
					} else {
						JTextPane empty = new JTextPane();
						empty.setText("  ");
						empty.setEditable(false);
						prettyprint.add(empty);
						ret.append(whitespace);
					}
					
					String nextSplit = corSplit(split, roots);
					JRadioButton splitButton = new JRadioButton(nextSplit);
					splitButton.setActionCommand(nextSplit);
					splitButton.addActionListener(this);
					//splitButton.setHorizontalTextPosition(AbstractButton.LEADING);
					splitButton.setBackground(Color.WHITE);
					splitButton.setRolloverEnabled(true);
					prettyprint.add(splitButton);
					radioButtons.add(splitButton);
					
					ret.append(corSplit(split, roots) + "</td></tr>");
					toVisit.addAll(split.getAllSubgraphs());
				}

				prettyprint.add(new JTextPane());
				prettyprint.add(new JTextPane());
				
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
		StringBuffer ret = new StringBuffer("<" + split.getRootFragment());
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
		ret.append(">");
		return ret.toString();
	}
	
	/**
	 * 
	 */
	/* public void caretUpdate(CaretEvent e) {
		String marked = prettyprint.getSelectedText();
		
		
		
		// a split is selected
		if ((marked != null) && marked.matches("[ \t\n\f\r]*<.*>")) {
			
			jdg.setMarked(false);
			splitMarked = true;
			
			// retrieving the split's nodes
			
			Pattern twoHolePat = Pattern.compile("<(.+) \\{(.+)=\\{\\{(.+)\\}\\}, (.+)=\\{\\{(.+)\\}\\}\\}>");
			Matcher twoHoleMatcher = twoHolePat.matcher(marked);
			
			Pattern oneHolePat = Pattern.compile("<(.+) \\{(.+)=\\{\\{(.+)\\}\\}\\}>");
			Matcher oneHoleMatcher = oneHolePat.matcher(marked);
			
			Set<String> blueBag = new HashSet<String>();
			Set<String> redBag = new HashSet<String>();
			//String blueHole;
			//String redHole;
			String root = "";
			if( twoHoleMatcher.find() ) {
				root = twoHoleMatcher.group(1);
				//blueHole = splitMatcher.group(2);
				
				blueBag.add(twoHoleMatcher.group(2));
				StringTokenizer bluetok = new StringTokenizer(twoHoleMatcher.group(3),
				" {},=<>\t\n\f\r");
				while( bluetok.hasMoreTokens() ) {
					blueBag.add(bluetok.nextToken());
				}
				
				//redHole = splitMatcher.group(4);
				redBag.add(twoHoleMatcher.group(4));
				StringTokenizer redtok = new StringTokenizer(twoHoleMatcher.group(5),
				" {},=<>\t\n\f\r");
				while( redtok.hasMoreTokens() ) {
					redBag.add(redtok.nextToken());
				}
			} else if( oneHoleMatcher.find()) {
				
				root = oneHoleMatcher.group(1);
				
				blueBag.add(oneHoleMatcher.group(2));
				
				StringTokenizer bluetok = new StringTokenizer(oneHoleMatcher.group(3),
				" {},=<>\t\n\f\r");
				while( bluetok.hasMoreTokens() ) {
					blueBag.add(bluetok.nextToken());
				}
			}
			
			// TODO move the following anywhere else (Tab?)
			// changing the color of nodes and edges
						
			jdg.markGraph(Color.LIGHT_GRAY);
			
			if(!root.equals("")) {
				DefaultGraphCell rootNode = jdg.getNodeForName(root);
				Fragment rootFrag = jdg.findFragment(rootNode);
				for( DefaultGraphCell rfn : rootFrag.getNodes()) {
					jdg.markNode(rfn, myGreen);
					for( DefaultEdge edg : jdg.getOutEdges(rfn) ) {
						jdg.markEdge(edg, myGreen);
					}
				}
			}
			jdg.markWcc(blueBag, Color.BLUE, Color.BLUE);
			jdg.markWcc(redBag, purple, purple);
			
			jdg.computeLayout();
			jdg.adjustNodeWidths();
			jdg.setMarked(true);
			
		} else {
			if (splitMarked) {
				jdg.setMarked(false);
				splitMarked = false;
			}
		}
		
	}*/
	
	
	
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

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		
		String marked = e.getActionCommand();
		System.err.println(marked);
		jdg.setMarked(false);
		splitMarked = true;
		
		// retrieving the split's nodes
		
		Pattern twoHolePat = Pattern.compile("<(.+) \\{(.+)=\\[\\[(.+)\\]\\], (.+)=\\[\\[(.+)\\]\\]\\}>");
		Matcher twoHoleMatcher = twoHolePat.matcher(marked);
		
		Pattern oneHolePat = Pattern.compile("<(.+) \\{(.+)=\\[\\[(.+)\\]\\]\\}>");
		Matcher oneHoleMatcher = oneHolePat.matcher(marked);
		
		Set<String> blueBag = new HashSet<String>();
		Set<String> redBag = new HashSet<String>();
		//String blueHole;
		//String redHole;
		String root = "";
		if( twoHoleMatcher.find() ) {
			
			System.err.println(", has two holes.");
			root = twoHoleMatcher.group(1);
			//blueHole = splitMatcher.group(2);
			
			blueBag.add(twoHoleMatcher.group(2));
			StringTokenizer bluetok = new StringTokenizer(twoHoleMatcher.group(3),
			" {},=<>[]\t\n\f\r");
			while( bluetok.hasMoreTokens() ) {
				blueBag.add(bluetok.nextToken());
			}
			
			//redHole = splitMatcher.group(4);
			redBag.add(twoHoleMatcher.group(4));
			StringTokenizer redtok = new StringTokenizer(twoHoleMatcher.group(5),
			" {},=<>[]\t\n\f\r");
			while( redtok.hasMoreTokens() ) {
				redBag.add(redtok.nextToken());
			}
		} else if( oneHoleMatcher.find()) {
			
			root = oneHoleMatcher.group(1);
			
			blueBag.add(oneHoleMatcher.group(2));
			
			StringTokenizer bluetok = new StringTokenizer(oneHoleMatcher.group(3),
			" {},=<>[]\t\n\f\r");
			while( bluetok.hasMoreTokens() ) {
				String next = bluetok.nextToken();
				System.err.println(next +"\n");
				blueBag.add(next);
			}
		}
		
		// TODO move the following anywhere else (Tab?)
		// changing the color of nodes and edges
					
		jdg.markGraph(Color.LIGHT_GRAY);
		
		if(!root.equals("")) {
			DefaultGraphCell rootNode = jdg.getNodeForName(root);
			Fragment rootFrag = jdg.findFragment(rootNode);
			for( DefaultGraphCell rfn : rootFrag.getNodes()) {
				jdg.markNode(rfn, myGreen);
				for( DefaultEdge edg : jdg.getOutEdges(rfn) ) {
					jdg.markEdge(edg, myGreen);
				}
			}
		}
		jdg.markWcc(blueBag, Color.BLUE, Color.BLUE);
		jdg.markWcc(redBag, purple, purple);
		
		jdg.computeLayout();
		jdg.adjustNodeWidths();
		jdg.setMarked(true);
		
	} 
		
	
	
}
