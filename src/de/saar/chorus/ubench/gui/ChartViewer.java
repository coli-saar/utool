package de.saar.chorus.ubench.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphCell;

import de.saar.chorus.domgraph.chart.Chart;
import de.saar.chorus.domgraph.chart.Split;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.ubench.Fragment;
import de.saar.chorus.ubench.JDomGraph;

/**
 * A <code>JFrame</code> containing a GUI for visualising a 
 * <code>Chart</code> of a dominance graph and 
 * highlighting parts of it in a <code>JDomGraph</code>.
 * 
 * @see de.saar.chorus.domgraph.chart.Chart
 * @see de.saar.chorus.ubench.JDomGraph
 * @author Michaela Regneri
 * @author Alexander Koller
 *
 */
public class ChartViewer extends JFrame implements ListSelectionListener  {
	
	//private JPanel prettyprint; // Component for the text representation
	private JTable prettyprint;
	private Chart chart; // the chart itself
	
	private DomGraph dg; // the graph belonging to the chart
	private JDomGraph jdg; // the Graph to highlight the nodes in
	
	private boolean splitMarked; // indicates whether or not a split is currently highlighted
	
	// redefining some colors
	private Color myGreen;
	
	private Color purple;
	
	private Color redbrown;
	
	private Color lightbrown;
	
	List<Color> colors;
	int colorindex;
	
	private ButtonGroup radioButtons;
	
	private Map<Split, String> nameToSplit;
	private List<Split> orderedSplits;
	private List<Set<String>> subgraphs;
	private List<Integer> noOfSplits;
	
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
		
		colors = new ArrayList<Color>();
		colors.add(Color.blue);
		colors.add(purple);
		colors.add(Color.CYAN);
		colors.add(Color.RED);
		colors.add(redbrown);
		colors.add(lightbrown);
		
		colorindex = 0;
		
		//GridLayout layout = new GridLayout(0,2);
		
		radioButtons = new ButtonGroup();
		nameToSplit = new HashMap<Split,String>();
		subgraphs = new ArrayList<Set<String>>();
		noOfSplits = new ArrayList<Integer>();
		orderedSplits = new ArrayList<Split>();
		
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
		
		prettyprint = new JTable(new ChartTableModel());
		prettyprint.setCellSelectionEnabled(true);
		prettyprint.getSelectionModel().addListSelectionListener(this);
		JScrollPane printPane = new JScrollPane(prettyprint);
		add(printPane);
		
		
		
		//TODO perhaps this isn't such a good idea...
		setAlwaysOnTop(true);
		//setLocationRelativeTo(Ubench.getInstance().getWindow());
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
	//			prettyprint.add(nextSubgraph);
				
				nextSubgraph.setText("<html content=\"text/html; " +
						"charset=UTF-8\"><div style='" +
						"font-size:15pt; color:#000000'>" +
						"<font face=\"Arial Unicode MS\">" 
						+sgs + "  &#8594;</font></div></html>");
				
				List<Split> splits = chart.getSplitsFor(subgraph);
				
				noOfSplits.add(splits.size());
				
				int splitcount = 0;
				for (Split split : splits ) {
					subgraphs.add(subgraph);
					splitcount++;
					if (first) {
						
						first = false;
					} else {
		
						JTextPane empty = new JTextPane();
						empty.setText("  ");
						empty.setEditable(false);
					//	prettyprint.add(empty);
					}
					
					String nextSplit = corSplit(split, roots);
					
					nameToSplit.put(split, splitcount + ". " + nextSplit);
					orderedSplits.add(split);
					
	/*				JButton splitButton = new JButton(nextSplit);
					splitButton.setActionCommand(nextSplit);
					splitButton.addActionListener(this);
					
					splitButton.setBackground(Color.WHITE);
					splitButton.setBorderPainted(false);
					splitButton.setHorizontalAlignment(AbstractButton.LEFT);
					splitButton.setMargin(new Insets(0,0,0,0));
					splitButton.setRolloverEnabled(true);
					splitButton.setFont(new Font("Arial Unicode MS", Font.PLAIN, 15));
					
					prettyprint.add(splitButton);
					radioButtons.add(splitButton);*/
				
					toVisit.addAll(split.getAllSubgraphs());
				}

			//	prettyprint.add(new JTextPane());
			//	prettyprint.add(new JTextPane());
				
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
	public void valueChanged(ListSelectionEvent	 e) {
		
		
		
		//String marked = orderedSplits.get(
			//	prettyprint.getSelectedColumn());
		
		
		
		if(prettyprint.getSelectedColumn() == 1 ) {
			
			System.err.println("Clicked on Split: " + orderedSplits.get(
					prettyprint.getSelectedColumn()));
			
		Split selectedSplit = orderedSplits.get(
				prettyprint.getSelectedColumn());
		
		// retrieving the split's nodes
		
		// TODO move the following anywhere else (Tab?)
		// changing the color of nodes and edges
					
		jdg.markGraph(Color.LIGHT_GRAY);
		
		Set<String> dominators = selectedSplit.getAllDominators();
		String root = selectedSplit.getRootFragment();
		
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
		
		for(String hole : dominators) {
			
			//jdg.markNode(jdg.getNodeForName(hole), colors.get(colorindex));
			List<Set<String>> wccs = selectedSplit.getWccs(hole);
			for( Set<String> wcc : wccs) {
				wcc.add(hole);
				jdg.markWcc(wcc, colors.get(colorindex), colors.get(colorindex));
			}
			colorindex++;
		}
		colorindex = 0;
		
		} else if (prettyprint.getSelectedColumn() == 0) {
			Set<String> subgraph = subgraphs.get(
					prettyprint.getSelectedRow());
			jdg.markGraph(Color.LIGHT_GRAY);
			jdg.markWcc(subgraph, colors.get(colorindex), 
					colors.get(colorindex));
		}
		
		jdg.computeLayout();
		jdg.adjustNodeWidths();
		
		jdg.setMarked(true);
		
	} 
		
	class ChartTableModel extends AbstractTableModel {

		/* (non-Javadoc)
		 * @see javax.swing.table.AbstractTableModel#getColumnName(int)
		 */
		@Override
		public String getColumnName(int column) {
			if(column == 0) {
				return "Subgraph";
			} else if (column == 1) {
				return "Splits";
			} else return "";
		}

		/* (non-Javadoc)
		 * @see javax.swing.table.TableModel#getColumnCount()
		 */
		public int getColumnCount() {
			return 2;
		}

		/* (non-Javadoc)
		 * @see javax.swing.table.TableModel#getRowCount()
		 */
		public int getRowCount() {
				return orderedSplits.size();
		}

		/* (non-Javadoc)
		 * @see javax.swing.table.TableModel#getValueAt(int, int)
		 */
		public Object getValueAt(int rowIndex, int columnIndex) {
			if(columnIndex == 0) {
			
				int splitcount = 0;
				for(int i = 0; i < noOfSplits.size(); i++ ) {
					if(rowIndex == splitcount) {
						return subgraphs.get(i);
					} else {
						splitcount += noOfSplits.get(i);
						if( rowIndex < splitcount ) {
							return "";
						}
					}
					
				}			
			} else {
				return nameToSplit.get(orderedSplits.get(rowIndex));
			}
			return null;
		}
		
	}
	
}
