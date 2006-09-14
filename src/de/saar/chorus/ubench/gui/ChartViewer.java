package de.saar.chorus.ubench.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Insets;
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
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
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
		"Click on a split to highlight it in the graph window.");
		
				
		chartOnlyRootsHTML();
		// layout
		add(instruction, BorderLayout.NORTH);
		add(new JScrollPane(prettyprint), BorderLayout.CENTER);
		Dimension preferred = new Dimension((int) prettyprint.getPreferredSize().width, 
				(int) Ubench.getInstance().getTabHeight());
		setPreferredSize(preferred);
		
		//TODO perhaps this isn't such a good idea...
		setAlwaysOnTop(true);
		setLocationRelativeTo(Ubench.getInstance().getWindow());
		pack();
		validate();
		setVisible(true);
	}
	
	private void chartOnlyRootsHTML() {
		
		Set<String> roots = dg.getAllRoots();
		Set<Set<String>> visited = new HashSet<Set<String>>();
		
		for (Set<String> fragset : chart.getToplevelSubgraphs()) {
			corSubgraph(fragset, roots, visited);
		}
		
	}
	
	private void corSubgraph(Set<String> subgraph, Set<String> roots,
			Set<Set<String>> visited) {
		Set<String> s = new HashSet<String>(subgraph);
		boolean first = true;
		
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
				
				for (Split split : chart.getSplitsFor(subgraph)) {
					if (first) {
						first = false;
					} else {
						JTextPane empty = new JTextPane();
						empty.setText("  ");
						empty.setEditable(false);
						prettyprint.add(empty);
					}
					
					String nextSplit = corSplit(split, roots);
					JButton splitButton = new JButton(nextSplit);
					splitButton.setActionCommand(nextSplit);
					splitButton.addActionListener(this);
					
					splitButton.setBackground(Color.WHITE);
					splitButton.setBorderPainted(false);
					splitButton.setHorizontalAlignment(AbstractButton.LEFT);
					splitButton.setMargin(new Insets(0,0,0,0));
					splitButton.setRolloverEnabled(true);
					prettyprint.add(splitButton);
					radioButtons.add(splitButton);
				
					toVisit.addAll(split.getAllSubgraphs());
				}

				prettyprint.add(new JTextPane());
				prettyprint.add(new JTextPane());
				
				for (Set<String> sub : toVisit) {
					corSubgraph(sub, roots, visited);
				}
			}
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
