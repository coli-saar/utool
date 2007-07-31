package de.saar.chorus.ubench;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import de.saar.chorus.domgraph.chart.Chart;
import de.saar.chorus.domgraph.chart.ChartSolver;
import de.saar.chorus.domgraph.chart.SolvedFormIterator;
import de.saar.chorus.domgraph.chart.SolvedFormSpec;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.domgraph.layout.JDomGraphCanvas;
import de.saar.chorus.domgraph.layout.LayoutAlgorithm;
import de.saar.chorus.domgraph.layout.LayoutOptions;
import de.saar.chorus.ubench.Preferences.LayoutType;
import de.saar.chorus.ubench.chartviewer.ChartViewer;
import de.saar.chorus.ubench.jdomgraph.JDomGraph;

/**
 * A <code>JPanel</code> displaying a <code>JDomGraph</code>,
 * providing several informations on the graph needed by other
 * GUI-classes.
 * 
 * @see JGraphTab
 * @see JSolvedFormTab
 * 
 * @author Alexander Koller
 * @author Michaela Regneri
 *
 */
class JDomGraphTab extends JGraphTab  {


	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// graph information concerning solving and identity
	boolean solvable, isSolvedYet; 
	private Chart chart;

	/**
	 * Constructor to set up a tab with a dominance graph.
	 * The graph is solved if necessary.
	 * 
	 * @param theGraph the graph
	 * @param name the name for the tab
	 * @param paintNow if set to true, the graph is layoutet at once
	 */
	JDomGraphTab(JDomGraph theGraph, DomGraph origin, String name, 
			boolean paintNow, CommandListener lis, NodeLabels labels) throws Exception {

		super(theGraph, origin, name, lis, labels);
		// initializing
		graphName = name;

		solvable = true;
		solvedForms = -1;
		setBackground(Color.WHITE);

		if(Preferences.isAutoCount()) {
			Ubench.getInstance().showProgressBar();
			solve();
		} 
		
		if( Preferences.getInstance().getLayoutType().isApplicable(this) ) {
			setLayoutType(Preferences.getInstance().getLayoutType());
		} else {
			setLayoutType(LayoutType.JDOMGRAPH);
		}
		
		statusBar = new DominanceGraphBar();
		barCode = Ubench.getInstance().getStatusBar().insertBar(statusBar);
		setMinimumSize(statusBar.getMinimumSize());
		
		theGraph.setMarqueeHandler(new DomgraphMarqueeHandler(this, origin, labels));
		
	}

	/**
	 * Destructively changes the dominance graph underlying this JDomGraphTab.
	 * This method updates the graph display, re-solves the graph, and updates
	 * the status bar as necessary. 
	 * 
	 * @param domgraph
	 * @param labels
	 */
	public void setDominanceGraph(DomGraph domgraph, NodeLabels labels) throws Exception {
		this.domGraph = domgraph;
		this.nodeLabels = labels;
		
		
		
		isSolvedYet = false;
		if(Preferences.isAutoCount()) {
			Ubench.getInstance().showProgressBar();
			solve();
		} 
		
		
		statusBar = new DominanceGraphBar();
		barCode = Ubench.getInstance().getStatusBar().insertBar(statusBar);
		

		cv = null;
		if (! repaintIfNecessary()) {
			graph.clear();
			drawGraph();
		}
		Ubench.getInstance().refresh();
	}
	
	void drawGraph() throws Exception {
		
		if(! domGraph.getAllNodes().isEmpty()) {
		try {
			SwingUtilities.invokeLater(new Thread() {
				public void run() {
					
					
					
					JDomGraphCanvas canvas = new JDomGraphCanvas(graph);
					LayoutAlgorithm drawer = graph.getLayoutType().getLayout();
					drawer.layout(domGraph, nodeLabels, canvas, new LayoutOptions(getLabelType(), 
							Preferences.isRemoveRedundandEdges()));
					canvas.finish();
					
					Ubench.getInstance().refresh(true);
					Preferences.setFitWindowToGraph(false);
				
				}
			});

			

			// error message if layout fails
		} catch (Exception e) {
			//TODO find another way to notify Ubench about an empty tab
			empty = true;
			
			throw new Exception("Layout error: Ubench cannot layout this graph." +
					"Please make shure that your graph is valid.");


		}

		//validate();
		revalidate();
		}
	}

	/**
	 * Solve this tab's graph if it isn't solved yet.
	 *
	 */
	public void solve() {
		if( ! isSolvedYet ) {
			try {

				chart = new Chart();

				if(ChartSolver.solve(domGraph, chart))  {
					solvedForms = chart.countSolvedForms().longValue();
					isSolvedYet = true;
					Ubench.getInstance().setSolvingEnabled(true);
					

				} else {
					solvable = false;
					Ubench.getInstance().setSolvingEnabled(false);
				}

				statusBar = new DominanceGraphBar();
				barCode = Ubench.getInstance().getStatusBar().insertBar(statusBar);
			} catch( OutOfMemoryError e ) {
				chart = null;
				isSolvedYet = false;

				JOptionPane.showMessageDialog(Ubench.getInstance().getWindow(),
						"The solver ran out of memory while solving this graph. "
						+ "Try increasing the heap size with the -Xmx option.",
						"Out of memory",
						JOptionPane.ERROR_MESSAGE);
			} catch( Exception e ) {
				JOptionPane.showMessageDialog(Ubench.getInstance().getWindow(),
						"An error occurred while solving this graph.\n",
						"Dolver Error",
						JOptionPane.ERROR_MESSAGE);
			}
		}

	}



	/**
	 * @return true if the graph is solvable
	 */
	public boolean isSolvable() {
		return solvable;
	}


	/**
	 * @param solvable The solvable to set.
	 */
	public void setSolvable(boolean solvable) {
		this.solvable = solvable;
	}


	/**
	 * @return true if the graph has been solved yet.
	 */
	public boolean isSolvedYet() {
		return isSolvedYet;
	}

	/**
	 * @param isSolvedYet The isSolvedYet to set.
	 */
	void setSolvedYet(boolean isSolvedYet) {
		this.isSolvedYet = isSolvedYet;
	}

	void displayChart() {

		Cursor waitcursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);

		changeCursorGlobally(waitcursor);

		if(cv == null ) {
			if(!isSolvedYet) {
				isSolvedYet = true;
				chart = new Chart();
				ChartSolver.solve(domGraph, chart);
			}

			cv = new ChartViewer( chart, 
					domGraph, defaultName, graph, nodeLabels);
		} else {
			ChartViewer temp = new ChartViewer( chart, 
					domGraph, defaultName, graph, nodeLabels);
		}

		changeCursorGlobally(Cursor.getDefaultCursor());
	}

	/**
	 * Repaints the graph if its layout is not consistent
	 * with the recent layout preferences.
	 */
     boolean repaintIfNecessary() throws Exception {
		if (recentLayout == null || Preferences.mustUpdateLayout(recentLayout) 
				|| ! layout.isApplicable(this) ) {
			
			if(Preferences.getInstance().getLayoutType().isApplicable(this)) {
				setLayoutType(Preferences.getInstance().getLayoutType());
			} else {
				setLayoutType(LayoutType.JDOMGRAPH);
			}
			graph.clear();
			drawGraph();
			updateRecentLayout();
			validate();
			return true;
		}
		return false;
	}



	/**
	 * A <code>JPanel</code> representing a status bar for 
	 * a dominance graph, to be inserted into the <code>CardLayout</code>
	 * of <code>JDomGraphStatusBar</code>.
	 * 
	 * @author Michaela Regneri
	 *
	 */
	private class DominanceGraphBar extends JPanel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private JPanel classified; // the panel for the classify symbols
		private JButton solve; 	// for solving 

		// the text labels
		private JLabel 	numberOfForms, 	// indicates how many solved forms there are
		norm, 		  	// indicates normality (graphs)
		hn, 			// indicates hypernormality (graphs)
		ll; 		 	// indicates leaf labeling (graphs)


		private Set<JLabel> classifyLabels;


		private GridBagLayout layout = new GridBagLayout();


		/**
		 * Sets up a new <code>DominanceGraphBar</code> by
		 * initalizing the fields and doing the layout.
		 * 
		 */
		private DominanceGraphBar() {
			super(); 
			setLayout(layout);

//			solve button
			solve = new JButton("SOLVE");
			solve.setActionCommand("solve");
			solve.addActionListener(listener);
			solve.setPreferredSize(new Dimension(80,25));

			GridBagConstraints solveConstraints = new GridBagConstraints();
			solveConstraints.weightx = 0;
			solveConstraints.weighty = 0;
			solveConstraints.anchor = GridBagConstraints.WEST;
			solveConstraints.insets = new Insets(0,10,0,10);

			layout.setConstraints(solve, solveConstraints);
			add(solve);



			if( ! (solvable && solvedForms > 0) ) {
				solve.setEnabled(false);
			}

			numberOfForms = new JLabel("", SwingConstants.LEFT);

			GridBagConstraints nofConstraint = new GridBagConstraints();
			nofConstraint.fill = GridBagConstraints.HORIZONTAL;


			if( isSolvedYet ) {
				if(solvedForms != 1 ) {
					numberOfForms.setText("This graph has " + String.valueOf(solvedForms) + " solved forms.");
				} else {
					numberOfForms.setText("This graph has " + String.valueOf(solvedForms) + " solved form.");
				}
			} else {

				if(solvable) {
					numberOfForms.setText("This graph has an unknown number of solved forms."); 
				} else {
					numberOfForms.setText("This graph is unsolvable."); 
				}

			}


			nofConstraint.weightx = 1.0;
			nofConstraint.weighty = 1.0;
			nofConstraint.anchor = GridBagConstraints.CENTER;


			layout.setConstraints(numberOfForms, nofConstraint);
			add(numberOfForms);

			/*
			 * Every label is set up with its "standard" character
			 * and the tooltip-text gets a new position (above the
			 * symbol itself).
			 */

			classified = new JPanel();
			classifyLabels = new HashSet<JLabel>();

			ll = new JLabel("L") {
				/**
				 * 
				 */
				private static final long serialVersionUID = 1L;

				public Point getToolTipLocation(MouseEvent e) {

					Point p1 = ll.getLocation();
					Point toReturn = new Point(p1.x, p1.y-25);
					return toReturn;
				}
			};
			ll.setForeground(Color.RED);
			classifyLabels.add(ll);

			hn = new JLabel("H") {
				/**
				 * 
				 */
				private static final long serialVersionUID = 4038867197509964748L;

				public Point getToolTipLocation(MouseEvent e) {
					Point p1 =hn.getLocation();
					Point toReturn = new Point(p1.x, p1.y-25);
					return toReturn;
				}
			};
			hn.setForeground(Color.RED);
			classifyLabels.add(hn);

			norm = new JLabel("N") {
				/**
				 * 
				 */
				private static final long serialVersionUID = -4917910242640525112L;

				public Point getToolTipLocation(MouseEvent e) {
					Point p1 = norm.getLocation();
					Point toReturn = new Point(p1.x, p1.y-25);
					return toReturn;
				}
			};
			norm.setForeground(Color.RED);
			classifyLabels.add(norm);





			if(domGraph.isNormal()) {
				norm.setText("N");
				norm.setToolTipText("Normal");

			} else if (domGraph.isWeaklyNormal()) {
				norm.setText("n");
				norm.setToolTipText("Weakly Normal");
			} else {
				norm.setText("-");
				norm.setToolTipText("Not Normal");
			}



			if(domGraph.isHypernormallyConnected()) {
				hn.setText("H");
				hn.setToolTipText("Hypernormally Connected");
			} else {
				hn.setText("-");
				hn.setToolTipText("Not Hypernormally Connected");
			}

			if(domGraph.isLeafLabelled()) {
				ll.setText("L");
				ll.setToolTipText("Leaf-Labelled");
			} else {
				ll.setText("-");
				ll.setToolTipText("Not Leaf-Labelled");
			}

			classified.setAlignmentY(SwingConstants.HORIZONTAL);
			classified.add(new JLabel("Classify: "));
			classified.add(norm);

			classified.add(ll);
			classified.add(hn);

			classified.setForeground(Color.RED);
			classified.setAlignmentX(SwingConstants.LEFT);

			GridBagConstraints classConstraints = new GridBagConstraints();
			classConstraints.anchor = GridBagConstraints.EAST;
			classConstraints.weightx = 0;
			classConstraints.weighty = 0;


			layout.setConstraints(classified,classConstraints);
			add(classified);



		}
	}



	private void changeCursorGlobally(Cursor cursor) {
		SwingUtilities.getRoot(this).setCursor(cursor);
		graph.setCursor(cursor);
		statusBar.setCursor(cursor);
		Ubench.getInstance().getWindow().setCursor(cursor);
	}


	/**
	 * Returns a <code>JGraphTab</code> identic to this one
	 * but containing clones of the <code>DomGraph</code> and the 
	 * <code>JDomGraph</code>
	 */
	public JGraphTab clone() {
		JDomGraph jdomCl = graph.clone();
		DomGraph domCl = (DomGraph) domGraph.clone();
		try {
		JDomGraphTab myClone = new JDomGraphTab(jdomCl, domCl,
				defaultName,true, listener, nodeLabels);
		return myClone;
		} catch( Exception e ) {
			// actually, this cannot happen
			JOptionPane.showMessageDialog(Ubench.getInstance().getWindow(),
					e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			return null;
		}
		
	}

	/**
	 * Creates a tab displaying the first solved form
	 * of this dominance graph.
	 * 
	 * @return the complete tab with the first solved form.
	 */
	JSolvedFormTab createFirstSolvedForm() {

		// solve if not solved yet
		if(! isSolvedYet ) {
			solve();
		}

		if( isSolvedYet ) {

			// setting up the first solved form:
			solvedFormIterator = new SolvedFormIterator(chart,domGraph);
			SolvedFormSpec spec = solvedFormIterator.next();
			DomGraph firstForm = domGraph.makeSolvedForm(spec);
			NodeLabels labels = nodeLabels.makeSolvedForm(spec);


			// setting up the tab
			JSolvedFormTab sFTab = new JSolvedFormTab(new JDomGraph(), 
					defaultName  + "  SF #1", 
					solvedFormIterator, domGraph, firstForm,
					1, solvedForms, 
					graphName, 
					listener, labels);

			return sFTab;
		} else {
			return null;
		}



	}

	public Chart getChart() {
		return chart;
	}




}
