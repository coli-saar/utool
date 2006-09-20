package de.saar.chorus.ubench.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

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
	private List<Integer> splitNumbers;
	
	private String longestSplit;
	private Set<String> biggestSubgraph;
	private int lastIndex;
	
	
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
		splitNumbers = new ArrayList<Integer>();
		
		
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
		
		prettyprint.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		prettyprint.setColumnSelectionAllowed(true);
		prettyprint.setRowSelectionAllowed(true);
		prettyprint.setCellSelectionEnabled(true);
		prettyprint.getSelectionModel().addListSelectionListener(this);
		
		prettyprint.getColumnModel().getSelectionModel().addListSelectionListener(this);
		initColumnSizes();
		
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
	
		biggestSubgraph = subgraphs.get(0);
	    lastIndex = orderedSplits.size();
		
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
				
				
				List<Split> splits = chart.getSplitsFor(subgraph);
				noOfSplits.add(splits.size());
				
				int splitcount = 0;
				for (Split split : splits ) {
					
					
					
					subgraphs.add(s);
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
					
					if( longestSplit == null ) {
						longestSplit = nextSplit;
					} else {
						if(nextSplit.length() > 
						longestSplit.length() ) {
							longestSplit = nextSplit;
						}
					}
					
					nameToSplit.put(split, nextSplit);
					splitNumbers.add(splitcount);
					orderedSplits.add(split);
					
					
					
					toVisit.addAll(split.getAllSubgraphs());
				}
				subgraphs.add(new HashSet<String>());
				orderedSplits.add(null);
				noOfSplits.add(1);
				splitNumbers.add(0);
				
				
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
		
		int row = prettyprint.getSelectedRow();
		int col = prettyprint.getSelectedColumn();
		
		
		if( (col >= 1) && (row > -1)  ) {
			
			
			Split selectedSplit = orderedSplits.get(
					prettyprint.getSelectedRow());
			
			// retrieving the split's nodes
			
			// TODO move the following anywhere else (Tab?)
			// changing the color of nodes and edges
			if(selectedSplit != null ) {
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
			} else {
				jdg.setMarked(false);
			}
			
		} else if (col == 0) {
			if(row > -1) {
				Set<String> subgraph = subgraphs.get(
						row);
				
				if( ! subgraph.isEmpty() ) {
					jdg.markGraph(Color.LIGHT_GRAY);
					jdg.markWcc(subgraph, colors.get(colorindex), 
							colors.get(colorindex));
				} else {
					jdg.setMarked(false);
				}
			}
		} else {
			jdg.setMarked(false);
		}
		
		jdg.computeLayout();
		jdg.adjustNodeWidths();
		
		jdg.setMarked(true);
		
	} 
	
	private void initColumnSizes() {
        AbstractTableModel model = (ChartTableModel)prettyprint.getModel();
        TableColumn column = null;
        Component comp = null;
        int headerWidth = 0;
        int cellWidth = 0;
        Object[] longValues = {biggestSubgraph, lastIndex, longestSplit};
        
        TableCellRenderer headerRenderer =
        	prettyprint.getTableHeader().getDefaultRenderer();

        for (int i = 0; i < 3; i++) {
            column = prettyprint.getColumnModel().getColumn(i);

            comp = headerRenderer.getTableCellRendererComponent(
                                 null, column.getHeaderValue(),
                                 false, false, 0, 0);
            headerWidth = comp.getPreferredSize().width;

            
            comp = prettyprint.getDefaultRenderer(model.getColumnClass(i)).
                             getTableCellRendererComponent(
                            		 prettyprint, longValues[i],
                                 false, false, 0, i);
            cellWidth = comp.getPreferredSize().width;

                      
            column.setPreferredWidth(Math.max(headerWidth, cellWidth));
        }
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
				return "No.";
			} else if( column == 2 ) {
				return "Splits";
			} else return "";
		}
		
		/* (non-Javadoc)
		 * @see javax.swing.table.TableModel#getColumnCount()
		 */
		public int getColumnCount() {
			return 3;
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
				
				Set<String> toShow = subgraphs.get(rowIndex);
				if(splitNumbers.get(rowIndex) == 1) {
					return toShow;
				} else {
					return "";
				}
				
				
			} else if(columnIndex == 2) {
				Split next = orderedSplits.get(rowIndex);
				
				if( next != null) {
					return  nameToSplit.get(next);
				} else {
					return " ";
				}
			} else if(columnIndex == 1) {
				Integer splitnumber = splitNumbers.get(rowIndex);
				if(splitnumber == 0) {
					return null;
				}
				else {
					return splitnumber;
				}
			}
			return null;
		}
		
		 public Class getColumnClass(int c) {
		        return getValueAt(0, c).getClass();
		    }
		
	}
	
}
