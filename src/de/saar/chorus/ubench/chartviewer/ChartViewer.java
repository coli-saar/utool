package de.saar.chorus.ubench.chartviewer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import org.jgraph.graph.AttributeMap;
import org.jgraph.graph.CellView;

import de.saar.chorus.domgraph.chart.Chart;
import de.saar.chorus.domgraph.chart.ConcreteRegularTreeGrammar;
import de.saar.chorus.domgraph.chart.DecoratedNonterminal;
import de.saar.chorus.domgraph.chart.GraphBasedNonterminal;
import de.saar.chorus.domgraph.chart.Split;
import de.saar.chorus.domgraph.chart.SubgraphNonterminal;
import de.saar.chorus.domgraph.equivalence.EquationSystem;
import de.saar.chorus.domgraph.equivalence.rtg.QuantifierMarkedNonterminal;
import de.saar.chorus.domgraph.equivalence.rtg.RtgRedundancyElimination;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.domgraph.graph.DomGraph.PreprocessingException;
import de.saar.chorus.ubench.Ubench;
import de.saar.chorus.ubench.jdomgraph.JDomGraph;

/**
 * A <code>JFrame</code> containing a GUI for visualising a
 * <code>Chart</code> of a dominance graph and
 * highlighting parts of it in a <code>JDomGraph</code>.
 *
 * @see de.saar.chorus.domgraph.chart.Chart
 * @see de.saar.chorus.ubench.jdomgraph.JDomGraph
 *
 * @author Michaela Regneri
 * @author Alexander Koller
 *
 */
public class ChartViewer extends JFrame implements ListSelectionListener  {


	/**
	 *
	 */
	private static final long serialVersionUID = 2614540458877105512L;
	private final JTable prettyprint;	// the chart as JTable
	private ConcreteRegularTreeGrammar<GraphBasedNonterminal> chart;
	//private Chart chart; 		// the chart to work with
	private final Chart chartcopy; 	// a safety copy

	private DomGraph dg; // the graph belonging to the chart
	private JDomGraph jdg; // the Graph to highlight the nodes in
	// the node labels object of the DomGraph
	private NodeLabels labels;
	private Map<CellView, AttributeMap> backup;

	// splits and their string representation
	private final Map<Split<GraphBasedNonterminal>, String> nameToSplit;

	// splits in the order they appear in the table
	private final List<Split<GraphBasedNonterminal>> orderedSplits;

	// subgraphs in the order they appear in the table
	private final List<GraphBasedNonterminal> subgraphs;

	// the numbers ordering the splits of a subgraph
	private final List<Integer> splitNumbers;

	// subgraphs, complete and represented only by roots
	private final Map<Set<String>, GraphBasedNonterminal> rootsToSubgraphs;

	private boolean reduced;

	/*
	 * for determining the cell widths
	 */
	private String longestSplit;
	private GraphBasedNonterminal biggestSubgraph;
	private int lastIndex;

	// keeping track of the currently marked field
	private int currentrow;
	private int currentcolumn;
	private boolean modified;

	/*
	 * Status bar, showing the number of splits, subgraphs
	 * and solved forms
	 */
	private JPanel statusbar; 	// panel on the bottom
	private JLabel solvedforms, isred, red; // text on the bottom
	private JButton solve;

	// counting solved forms, splits and subgraphs
	private BigInteger noOfSolvedForms;
	private int noOfSplits;
	private int noOfSubgraphs;

	private final String graphName;
    private String eqsname;

	// the ActionListener responsible for actions
	// triggered via the Window Menu
	private final ChartViewerListener listener;
	private final ChartViewerMenu menu;

	/**
	 * A new ChartViewer
	 *
	 * @param c the Chart
	 * @param g the DomGraph
	 * @param title the name of the graph
	 * @param jg the visible JDomGraph
	 * @param la the NodeLabels object
	 */
	public ChartViewer(Chart c, DomGraph g,
			String title, JDomGraph jg,
			NodeLabels la) {

		// some initialising
		super("Chart of " + title);

		graphName = title;
		labels = la;
		listener = new ChartViewerListener(this);
		chartcopy = c;
		chart = (ConcreteRegularTreeGrammar<GraphBasedNonterminal>) c.clone();
		dg = g;
		jdg = jg;
		modified = false;
		reduced = false;
		
		// TODO actually I think it's impossible that this should be the only way.
	
		backup = new HashMap<CellView, AttributeMap>();
		for(CellView view : jdg.getGraphLayoutCache().getCellViews()) {
			AttributeMap copy = (AttributeMap) view.getAllAttributes().clone();
			backup.put(view,copy);
		}
		
		// automatical chart reduction is selected
		if(Ubench.getInstance().reduceAutomatically) {
			if( ! dg.isNormal() ) {
				// error if a not normal graph is loaded
				JOptionPane.showMessageDialog(this,
						"This is the chart of a graph which is not normal," +
						System.getProperty("line.separator") +
						"so Utool cannot eliminate redundancies.",
						"Server Error",
						JOptionPane.ERROR_MESSAGE);
			} else if( ! dg.isHypernormallyConnected()) {
				// error if a not hnc graph is loaded
				JOptionPane.showMessageDialog(this,
						"This is the chart of a graph which is not hypernormally" +
						System.getProperty("line.separator") +
						"connected, so Utool cannot eliminate redundancies.",
						"Server Error",
						JOptionPane.ERROR_MESSAGE);
			} else {
				reduceChart(Ubench.getInstance().getEquationSystem(),
						Ubench.getInstance().getEqsname());
			}
		}

		nameToSplit = new HashMap<Split<GraphBasedNonterminal>,String>();
		rootsToSubgraphs = new HashMap<Set<String>, GraphBasedNonterminal>();
		subgraphs = new ArrayList<GraphBasedNonterminal>();
		orderedSplits = new ArrayList<Split<GraphBasedNonterminal>>();
		splitNumbers = new ArrayList<Integer>();
		currentrow = -1;
		currentcolumn = -1;


		JLabel instruction = new JLabel(
		"Click on a split to highlight it in the graph window.");

		// filling the Lists and Maps used by the
		// Table model
		calculateChartTable();




		// initialising and customising the JTable
		prettyprint = new JTable(new ChartTableModel()) {

			private static final long serialVersionUID = 1L;

			@Override
            public String getToolTipText(MouseEvent e) {
				java.awt.Point p = e.getPoint();
				int rowIndex = rowAtPoint(p);
				int colIndex = columnAtPoint(p);
				int realColumnIndex = convertColumnIndexToModel(colIndex);

				// Tooltips for subgraphs
				if(realColumnIndex == 0) {
					Set<String> subgraph = subgraphs.get(rowIndex).getNodes();
					if( subgraph != null && (! subgraph.isEmpty())) {
						int num = chart.countSolvedFormsFor(
								rootsToSubgraphs.get(subgraph)).intValue();
						if(num == 1) {
							return "This subgraph has 1 solved form.";
						}
						return "This subgraph has " +
						num + " solved forms.";
					}
				}
				return null;
			}
		};

		// single-cell-selection and listening to
		// changes of this selection
		prettyprint.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		prettyprint.setColumnSelectionAllowed(true);
		prettyprint.setRowSelectionAllowed(true);
		prettyprint.setCellSelectionEnabled(true);
		prettyprint.getSelectionModel().addListSelectionListener(this);
		prettyprint.getColumnModel().getSelectionModel().addListSelectionListener(this);


		// layout
		add(instruction);
		JScrollPane printPane = new JScrollPane(prettyprint);
		add(printPane);

		noOfSolvedForms = chart.countSolvedForms();
		noOfSplits = chart.size();

		// column width
		if(noOfSplits > 0) {
            initColumnSizes();
        }


		// the information text on the bottom
		makeStatusBar();

		// menu.
		menu = new ChartViewerMenu(listener);
		setJMenuBar(menu);


		addWindowFocusListener(new ChartViewerFocusListener());
		pack();
		validate();
		setLocationRelativeTo(Ubench.getInstance().getWindow());

		setVisible(true);

	}

	/**
	 * Refreshes the title of this window depending on
	 * whether or not the chart is modified / reduced.
	 *
	 */
	void refreshTitle() {
		if(reduced) {
			setTitle("Chart of " + graphName+ " (reduced)");
		} else if(modified) {
			setTitle("Chart of " + graphName + " (modified)");
		} else {
			setTitle("Chart of " + graphName);
		}
	}

	/**
	 * The main method for creating the data structure
	 * representing the visible Chart table internally.
	 *
	 */
	private void calculateChartTable() {

		Set<String> roots = dg.getAllRoots();
		Set<GraphBasedNonterminal> visited = new HashSet<GraphBasedNonterminal>();

		for (GraphBasedNonterminal fragset : chart.getToplevelSubgraphs()) {
			corSubgraph(fragset, roots, visited);
		}

		if(!subgraphs.isEmpty()) {
			biggestSubgraph = subgraphs.get(0);
		} else {
			biggestSubgraph = new SubgraphNonterminal();
		}
		lastIndex = orderedSplits.size();

	}

	/**
	 * Recursive helper method to fill the lists for each
	 * subgraph.
	 *
	 * @param subgraph the current subgraph
	 * @param roots the roots of the current subgraph
	 * @param visited the set of visited subgraphs
	 */
	private void corSubgraph(GraphBasedNonterminal subgraph, Set<String> roots, Set<GraphBasedNonterminal> visited) {
		GraphBasedNonterminal s = new SubgraphNonterminal(subgraph.getNodes()); // TODO - is this really necessary? - ak

		Set<GraphBasedNonterminal> toVisit = new HashSet<GraphBasedNonterminal>();

		if (!visited.contains(subgraph)) {

			// a new subgraph
			visited.add(subgraph);
		//	System.err.println(subgraph);
			s.getNodes().retainAll(roots);
			rootsToSubgraphs.put(s.getNodes(), subgraph);

			if (chart.getSplitsFor(subgraph) != null) {

				// counting the number of subgraphs with
				// splits (for the
				// information in the status bar)
				noOfSubgraphs++;

				List<Split<GraphBasedNonterminal>> splits = chart.getSplitsFor(subgraph);

				// for numbering the splits in the table,
				// count the splits of each subgraph
				int splitcount = 0;

				// for each split of the current subgraph...
				for (Split<GraphBasedNonterminal> split : splits ) {

					/*
					 * Adding the subgraph to the subgraph-list
					 * for each split.
					 * This is to retrieve the subgraph for
					 * a marked split (even if the subgraph
					 * is shown only once in the table) by
					 * just using the index of the currently
					 * marked row.
					 */
					subgraphs.add(subgraph);
					splitcount++;

					// to compute the string representations of
					// each split in the chart.
					String nextSplit = corSplit(split, roots);

					// retrieving the longest split
					// found so far
					if( longestSplit == null ) {
						longestSplit = nextSplit;
					} else {
						if(nextSplit.length() >
						longestSplit.length() ) {
							longestSplit = nextSplit;
						}
					}

					nameToSplit.put(split, nextSplit);

					// updating the lists of splits and
					// split numbers
					splitNumbers.add(splitcount);
					orderedSplits.add(split);

					toVisit.addAll(split.getAllSubgraphs());
				}

				// this is to create an empty line after
				// each subgraph "paragraph" in the table
				subgraphs.add(new SubgraphNonterminal());
				orderedSplits.add(null);
				splitNumbers.add(0);


				for (GraphBasedNonterminal sub : toVisit) {
					corSubgraph(sub, roots, visited);
				}

			}
		}
	}

	/**
	 * This computes a String representation of
	 * a split.
	 * @param split the split
	 * @param roots the list of nodes to show finally
	 * @return the String representing the split
	 */
	private String corSplit(Split<GraphBasedNonterminal> split, Set<String> roots) {
		StringBuffer ret = new StringBuffer("<" + split.getRootFragment());
		Map<String, List<Set<String>>> map = new HashMap<String, List<Set<String>>>();

		for (String hole : split.getAllDominators()) {
			List<Set<String>> x = new ArrayList<Set<String>>();
			map.put(hole, x);

			for (GraphBasedNonterminal wcc : split.getWccs(hole)) {
				GraphBasedNonterminal copy = new SubgraphNonterminal(wcc.getNodes());
				copy.getNodes().retainAll(roots);
				x.add(copy.getNodes());
			}
		}

		ret.append(" " + map);
		ret.append(">");
		return ret.toString();
	}






	/**
	 * This overrides the "setVisible" method to
	 * make sure that the marking disappears when
	 * the window is closed.
	 *
	 * @Override
	 *
	 */
	@Override
    public void setVisible(boolean b) {
		super.setVisible(b);

		FormatManager.unmark(jdg,backup);
	}


	/**
	 * This handels selection changes of the table
	 * and is responsible for marking splits and subgraphs
	 * in the table itself and in the main window.
	 */
	public void valueChanged(ListSelectionEvent	 e) {

		if (e.getValueIsAdjusting()) {
            return;
        }

		// the selected cell
		int row = prettyprint.getSelectedRow();
		int col = prettyprint.getSelectedColumn();

		if(row == currentrow && col == currentcolumn ) {
			// no real selection change
			return;
		} else {
			// the last selected cell


			int lastrow = currentrow;
			int lastcolumn = currentcolumn;

			// updating the indices
			currentrow = row;
			currentcolumn = col;
			if(lastrow > -1 && lastcolumn > -1 ) {
				if(lastcolumn == 1 ) {
					// updating cellview of the last selected
					// split if the last selected field was a number
					((AbstractTableModel) prettyprint.getModel())
					.fireTableCellUpdated(lastrow,++lastcolumn);
				} else {
//					updating cellview of the last selcted field
					// (if there was one)
					((AbstractTableModel) prettyprint.getModel())
					.fireTableCellUpdated(lastrow,lastcolumn);
				}
			}


			if( currentcolumn == 1 ) {
				// updating cellview of the currently selected
				// split if the selected field is a number
				((AbstractTableModel) prettyprint.getModel())
				.fireTableCellUpdated(row,col +1 );
			} else {
				//	 updating the cellview of the currently selected field
				((AbstractTableModel) prettyprint.getModel())
				.fireTableCellUpdated(row,col);
			}


			markGraph(row, col);
		}
	}

	private void markGraph(int row, int col) {
		if( (col >= 1) && (row > -1)  ) {
			// a split or the number of a split
			// is seleced
			SwingUtilities.invokeLater( new Thread() {

				@Override
                public void run() {
					Split<GraphBasedNonterminal> selectedSplit = orderedSplits.get(
							prettyprint.getSelectedRow());


					if(selectedSplit != null ) {
						// marking the split in the main window

						markSplit(selectedSplit);
						if(selectedSplit.getAllSubgraphs().isEmpty()) {
							FormatManager.refreshGraphLayout(jdg);
						}

					} else {
						// and empty row -> unmarking the graph
						FormatManager.unmark(jdg,backup);
					}
				}} );
		} else if (col == 0) {
			// a subgraph is selected
			if(row > -1) {
				SwingUtilities.invokeLater( new Thread() {

					@Override
                    public void run() {
						Set<String> subgraph = subgraphs.get(
								prettyprint.getSelectedRow()).getNodes();

						if( ! subgraph.isEmpty() ) {
							// marking the subgraph in the main window
							FormatManager.markSubgraph(subgraph, jdg, dg);

						} else {
							// and empty row -> unmarking the graph
							FormatManager.unmark(jdg,backup);
						}
					}
				});
			}
		} else {
			// nothing marked -> unmarking the graph
			FormatManager.unmark(jdg,backup);
		}

	}

	/**
	 * This is to init the widths of each column according to
	 * the longest entry of a column.
	 * TODO SWING apparently does not really care about this but
	 *      restricts the widths to a certain maximum of which only
	 *      SWING knows the origin...
	 *
	 */
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

	/**
	 * Mark a split in the main window by
	 * retrieving the colors via <code>FormatManager</code>.
	 *
	 * @param split the <code>Split</code> to mark.
	 */
	public void markSplit(Split<GraphBasedNonterminal> split) {

		// indexing the subgraphs
		// within the split (starting at 0).
		int subgraphindex = -1;

		// this makes the graph grey at first.
		FormatManager.shadeGraph(jdg);

		Set<String> dominators = new HashSet<String>(split.getAllDominators());
		String root = split.getRootFragment();

		// the root fragment has a special color.
		if(!root.equals("")) {

			FormatManager.markRootFragment(root, jdg, dg);
		}


		// iterating over the holes of the root fragment
		for(String hole : dominators) {

			List<GraphBasedNonterminal> wccs = new ArrayList<GraphBasedNonterminal>(split.getWccs(hole));

			// for each subgraph of a hole...
			for( GraphBasedNonterminal subg : wccs) {

				// count it
				subgraphindex++;
				Set<String> wcc = new HashSet<String>(subg.getNodes());

				// color it together with the hole
				// TODO decide which color the hole should
				// get when there are several subgraphs
				wcc.add(hole);

				FormatManager.markSubgraph(wcc, jdg, subgraphindex, dg);

			}
			// repainting the graph
			FormatManager.refreshGraphLayout(jdg);
		}
	}

	/**
	 * An <code>AbstractTableModel</code> representing a
	 * <code>Chart</code> in a <code>JTable</code>.
	 *
	 * @author Michaela Regneri
	 *
	 */
	class ChartTableModel extends AbstractTableModel {



		private static final long serialVersionUID = -7173102655466766081L;

		/**
		 * The column headers.
		 */
		@Override
        public String getColumnName(int column) {
			if(column == 0) {
				return "Subgraph";
			} else if (column == 1) {
				return "No.";
			} else if( column == 2 ) {
				return "Splits";
			} else {
                return "";
            }
		}

		/**
		 * The number of Columns.
		 * @return 3.
		 */
		public int getColumnCount() {
			return 3;
		}

		/**
		 * The number of rows.
		 */
		public int getRowCount() {
			return orderedSplits.size();
		}


		/**
		 * Retrieving the value of a field.
		 */
		public Object getValueAt(int rowIndex, int columnIndex) {

			if(columnIndex == 0) {
				// a subgraph
				GraphBasedNonterminal sub = subgraphs.get(rowIndex);
				Set<String> toShow = sub.getNodes();
				if(splitNumbers.get(rowIndex) == 1) {
					// subgraphs are only shown
					// for the first of its splits
					if(rowIndex == currentrow &&
							columnIndex == currentcolumn) {
						// a marked subgraph
						return FormatManager.getHTMLforMarkedSubgraph(toShow);
					}
					// a subgraph that is not marked
		/*		try {
					
					return toShow.toString() + "<b>" + 
							((QuantifierMarkedNonterminal) sub).getPreviousQuantifier() + "</b>";
				} catch( ClassCastException e) {
				//	System.err.println("noe" + toShow);
					return toShow;
				}*/
					
					if(sub instanceof QuantifierMarkedNonterminal) {
						return "<html>" + toShow.toString() + "<sub>" + 
						((QuantifierMarkedNonterminal) sub).getPreviousQuantifier()+ "</sub></html>";
					} else {
					
						return toShow;
					}
					
				} else {
					// as subgraph not to show.
					return "";
				}


			} else if(columnIndex == 2) {
				// a split
				Split<GraphBasedNonterminal> next = orderedSplits.get(rowIndex);

				if( next != null) {
					if( rowIndex == currentrow &&
							currentcolumn >= 1 ) {
						// a marked split
						return FormatManager.getHTMLforMarkedSplit(next, subgraphs.get(rowIndex).getNodes());
					}
					// a split not marked
					return  nameToSplit.get(next);
				} else {
					// and empty row
					return " ";
				}
			} else if(columnIndex == 1) {
				// the number of a split (relative to the splits in its subgraph)
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

		@Override
        public Class getColumnClass(int c) {
			return getValueAt(0, c).getClass();
		}

	}


	/**
	 * @return Returns the chart.
	 */
	ConcreteRegularTreeGrammar<GraphBasedNonterminal> getChart() {
		return  chart;
	}

	/**
	 * @param chart The chart to set.
	 */
	void setChart(ConcreteRegularTreeGrammar<GraphBasedNonterminal> chart) {
		this.chart = chart;
	}

	/**
	 * @return Returns the dg.
	 */
	DomGraph getDg() {
		return dg;
	}

	/**
	 * @param dg The dg to set.
	 */
	void setDg(DomGraph dg) {
		this.dg = dg;
	}

	/**
	 * @return Returns the jdg.
	 */
	JDomGraph getJdg() {
		return jdg;
	}

	/**
	 * @param jdg The jdg to set.
	 */
	void setJdg(JDomGraph jdg) {
		this.jdg = jdg;
	}

	/**
	 * Refreshing the window after the chart has
	 * changed.
	 *
	 */
	void refreshChartWindow() {
		// fire data change.
		((AbstractTableModel) prettyprint.getModel()).fireTableDataChanged();

		// emptying the data structure and resetting
		// the indices.
		prettyprint.getSelectionModel().setSelectionInterval(-1,-1);
		currentcolumn = -1;
		currentrow = -1;
		splitNumbers.clear();
		orderedSplits.clear();
		rootsToSubgraphs.clear();
		longestSplit = "";
		biggestSubgraph = new SubgraphNonterminal();
		lastIndex = -1;
		subgraphs.clear();
		noOfSubgraphs = 0;

		// rebuilding the data structure
		calculateChartTable();
		initColumnSizes();

		// refreshing the status bar
		noOfSolvedForms = chart.countSolvedForms();
		noOfSplits = chart.size();

		solvedforms.setText("   This chart has " + noOfSolvedForms
				+ " solved form" + (BigInteger.ONE.equals(noOfSolvedForms) ? "" : "s")
				+ ".");

		refreshTitleAndStatus();
		validate();



	}

	/**
	 *
	 * @return the currrently selected split if there is one, null otherwise
	 */
	Split<GraphBasedNonterminal> getSelectedSplit() {
		int row = prettyprint.getSelectedRow();
		int col = prettyprint.getSelectedColumn();

		if( (col >= 1) && (row > -1)  ) {
			Split<GraphBasedNonterminal> selectedSplit = orderedSplits.get(prettyprint.getSelectedRow());
			return selectedSplit;
		} return null;
	}

	/**
	 *
	 * @return the subgraph the currently selected split belongs to,
	 * @return null if there is no split selected
	 */
	GraphBasedNonterminal getSubgraphForMarkedSplit() {
		int row = prettyprint.getSelectedRow();

		if( row > -1 ) {

			return rootsToSubgraphs.get(
					subgraphs.get(row));

		} return null;
	}



	/**
	 * @return Returns the labels.
	 */
	NodeLabels getLabels() {
		return labels;
	}

	/**
	 * @param labels The labels to set.
	 */
	void setLabels(NodeLabels labels) {
		this.labels = labels;
	}

	/**
	 * Setting the chart to the "original" chart
	 * using the pointer to the copy.
	 *
	 */
	void resetChart() {
		chart = (ConcreteRegularTreeGrammar<GraphBasedNonterminal>) chartcopy.clone();
		reduced = false;
		modified = false;
		if(Ubench.getInstance().reduceAutomatically) {
			reduceChart(
					Ubench.getInstance().getEquationSystem(),
					Ubench.getInstance().getEqsname());
		}
		refreshChartWindow();
		if(reduced) {
			isred = new JLabel("<html><font color=\"green\">" +
			"&#8730;</font></html>");
			isred.setToolTipText("Redundancy has been eliminated.");
			red.setToolTipText("Redundancy has been eliminated.");
		} else {
			isred= new JLabel("<html><font color=\"red\">" +
			"X</font></html>");
			isred.setToolTipText("Redundancy has not been eliminated.");
			red.setToolTipText("Redundancy has not been eliminated.");
		}

	}

	void refreshTitleAndStatus() {

		if(reduced) {
			isred.setText("<html><font color=\"green\">" +
			"&#8730;</font></html>");
			isred.setToolTipText("Redundancy has been eliminated.");
			red.setToolTipText("Redundancy has been eliminated.");
		} else {
			isred.setText("<html><font color=\"red\">" +
			"X </font></html>");
			isred.setToolTipText("Redundancy has not been eliminated.");
			red.setToolTipText("Redundancy has not been eliminated.");
		}

		setMinimumSize(new Dimension((int) (statusbar.getPreferredSize().width * 1.2),
				getPreferredSize().height)
		);

		if( (! noOfSolvedForms.equals(chartcopy.countSolvedForms())) ||
				(noOfSubgraphs !=
					chartcopy.countSubgraphs()) ||
					(noOfSplits != chartcopy.size())) {
			modified = true;
		}
		refreshTitle();
		validate();
	}

	/**
	 * Shows some information on the displayed chart.
	 * This concerns number of subgraphs, splits, solved
	 * forms, status of modification / reduction and names.
	 *
	 */
	void showInfoPane() {
		StringBuffer infotext = new StringBuffer();
		infotext.append("This is the chart of " + graphName + ".");
		infotext.append(System.getProperty("line.separator"));
		infotext.append("  ");
		infotext.append(System.getProperty("line.separator"));

		infotext.append("This chart has " + noOfSolvedForms + " solved form" +
				(BigInteger.ONE.equals(noOfSolvedForms)? "" : "s") +
				".");
		infotext.append(System.getProperty("line.separator"));
		infotext.append("It contains " + noOfSubgraphs + " subgraphs and " +
				noOfSplits + " splits.");
		infotext.append(System.getProperty("line.separator"));
		infotext.append(System.getProperty("line.separator"));

		if( modified && (! reduced) ) {
			infotext.append("The chart has been modified:");
			infotext.append(System.getProperty("line.separator"));
			infotext.append("Some splits have been deleted.");
			infotext.append(System.getProperty("line.separator"));
		}
		if( reduced ) {
			infotext.append("The chart has been reduced with");
			infotext.append(System.getProperty("line.separator"));
			infotext.append("the equation system " + eqsname + ".");
			infotext.append(System.getProperty("line.separator"));
			infotext.append(System.getProperty("line.separator"));
		}

		if( reduced || modified ) {
			infotext.append("The original chart had " +
					chartcopy.countSolvedForms() + " solved forms.");
			infotext.append(System.getProperty("line.separator"));
			infotext.append("It contained " +
					chartcopy.countSubgraphs() + " subgraphs and " +
					chartcopy.size() + " splits.");
			infotext.append(System.getProperty("line.separator"));
		}

		infotext.append(System.getProperty("line.separator"));
		infotext.append("  ");
		JOptionPane.showMessageDialog(this, infotext, "Chart Information",
				JOptionPane.INFORMATION_MESSAGE);
	}

	/**
	 * Reduces the chart of this viewer with the
	 * given equation system which has the given title.
	 *
	 * @param eqs the equation system
	 * @param eqsn the name of the equation system
	 */
	void reduceChart(EquationSystem eqs, String eqsn) {

		if(eqs == null ) {
			/*
			 * TODO actually, this should never happen,
			 * hovever it should be handeled in an
			 * proper and appropriate way.
			 */
			return;
		}
		if(! reduced ) {
			try {
				
				
				Chart toElim = (Chart) chartcopy.clone();
				//IndividualRedundancyElimination elim;

				//elim = new IndividualRedundancyElimination(
				//		((DomGraph) dg.clone()).preprocess(), labels,
				//		eqs);
				RtgRedundancyElimination elim = new RtgRedundancyElimination(((DomGraph) dg.clone()).preprocess(), 
						labels, eqs);

				ConcreteRegularTreeGrammar<? extends GraphBasedNonterminal> out = new ConcreteRegularTreeGrammar<DecoratedNonterminal<SubgraphNonterminal, String>>();
				elim.eliminate(toElim, (ConcreteRegularTreeGrammar<DecoratedNonterminal<SubgraphNonterminal, String>>) out); //TA
				reduced = true;
				eqsname = eqsn;
				
		
				chart.clear();
				chart = (ConcreteRegularTreeGrammar<GraphBasedNonterminal>) out;
			
				
				if(statusbar != null) {
                    refreshTitleAndStatus();
                }
				
				
				
			} catch (PreprocessingException e) {
				// This shouldn't happen -- can't reduce chart of an unsolvable graph
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * This is to activate / deactivate the
	 * menu item for reducing the chart with a
	 * globally loaded equation system.
	 *
	 * @param isloaded if set to true, the item is enabled
	 */
	public void setEQSLoaded(boolean isloaded) {
		menu.setGlobalEQSenabled(isloaded);
	}

	/**
	 * The Menu Bar for the chart window.
	 *
	 * @author Michaela Regneri
	 *
	 */
	class ChartViewerMenu extends JMenuBar {


		/**
		 *
		 */
		private static final long serialVersionUID = -5447306859976469307L;
		ChartViewerListener lis;
		JMenu chartmenu, splitmenu;
		JMenuItem elred, reset, delete,
		firstsolvedform,
		elredglobal, info, close;

		ChartViewerMenu(ChartViewerListener li) {

			lis=li;

			chartmenu = new JMenu("Chart");

			firstsolvedform = new JMenuItem("Show first solved form");
			firstsolvedform.setActionCommand("solvechart");
			firstsolvedform.addActionListener(lis);
			chartmenu.add(firstsolvedform);

			chartmenu.addSeparator();

			elred = new JMenuItem("Reduce chart...");
			elred.setActionCommand("elred");
			elred.addActionListener(lis);
			elred.setMnemonic(KeyEvent.VK_R);
			elred.setAccelerator(KeyStroke.getKeyStroke("alt R"));
			chartmenu.add(elred);

			elredglobal = new JMenuItem("Reduce with global equation system");
			elredglobal.addActionListener(lis);
			elredglobal.setActionCommand("elredglobal");
			elredglobal.setEnabled(Ubench.getInstance().isEquationSystemLoaded());
			chartmenu.add(elredglobal);

			chartmenu.addSeparator();
			reset = new JMenuItem("Reset to original chart");
			reset.setActionCommand("resetchart");
			reset.addActionListener(lis);
			chartmenu.add(reset);


			info = new JMenuItem("Show chart information...");
			info.setActionCommand("chartinfo");
			info.addActionListener(lis);
			chartmenu.add(info);

			close = new JMenuItem("Close");
			close.setActionCommand("closechart");
			close.addActionListener(lis);
			chartmenu.add(close);


			chartmenu.validate();
			add(chartmenu);

			splitmenu = new JMenu("Split");


			delete = new JMenuItem("Delete marked split");
			delete.setActionCommand("delSplit");
			delete.setMnemonic(KeyEvent.VK_DELETE);
			delete.setAccelerator(KeyStroke.getKeyStroke("DELETE"));
			delete.addActionListener(lis);
			splitmenu.add(delete);
			splitmenu.validate();
			add(splitmenu);

		}

		void setGlobalEQSenabled(boolean en) {
			elredglobal.setEnabled(en);
		}
	}

	/**
	 * This listener makes sure that the visible graph
	 * is marked according to the chart window
	 * that becomes focused.
	 *
	 * @author Michaela Regneri
	 *
	 */
	private class ChartViewerFocusListener implements WindowFocusListener {

		public void windowGainedFocus(WindowEvent e) {


			int row = prettyprint.getSelectedRow();
			int col = prettyprint.getSelectedColumn();

			markGraph(row,col);
		}

		public void windowLostFocus(WindowEvent e) {
		}

	}

	/**
	 * Responsible for the appearance of the
	 * status bar at the bottom.
	 *
	 */
	private void makeStatusBar() {

		GridBagLayout layout = new GridBagLayout();
		statusbar = new JPanel(layout);
		solvedforms = new JLabel("This chart has "
				+ noOfSolvedForms + " solved form" +
				(BigInteger.ONE.equals(noOfSolvedForms)? "" : "s") + ".");

		solve = new JButton("SOLVE");
		solve.addActionListener(listener);
		solve.setActionCommand("solvechart");

		GridBagConstraints solveConstraints = new GridBagConstraints();
		solveConstraints.weightx = 0;
		solveConstraints.weighty = 0;
		solveConstraints.anchor = GridBagConstraints.WEST;
		solveConstraints.insets = new Insets(2,5,2,10);

		layout.setConstraints(solve, solveConstraints);
		statusbar.add(solve);

		if(BigInteger.ZERO.equals(noOfSolvedForms)) {
			solve.setEnabled(false);
		}

		GridBagConstraints nofConstraint = new GridBagConstraints();
		nofConstraint.fill = GridBagConstraints.HORIZONTAL;
		nofConstraint.weightx = 1.0;
		nofConstraint.weighty = 1.0;
		nofConstraint.anchor = GridBagConstraints.CENTER;


		layout.setConstraints(solvedforms, nofConstraint);
		statusbar.add(solvedforms);


		JPanel chartstate = new JPanel();
		red = new JLabel("Red:");
		isred = new JLabel();
		refreshTitleAndStatus();
		chartstate.add(red);
		chartstate.add(isred);
		GridBagConstraints classConstraints = new GridBagConstraints();
		classConstraints.anchor = GridBagConstraints.EAST;
		classConstraints.weightx = 0;
		classConstraints.weighty = 0;


		layout.setConstraints(chartstate,classConstraints);
		statusbar.add(chartstate);
		add(statusbar,BorderLayout.SOUTH);

	}
	/**
	 * @return Returns the reduced.
	 */
	boolean isReduced() {
		return reduced;
	}

	/**
	 * @param reduced The reduced to set.
	 */
	void setReduced(boolean reduced) {
		this.reduced = reduced;
	}


}
