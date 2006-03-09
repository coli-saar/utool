package de.saar.chorus.ubench.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import de.saar.chorus.domgraph.chart.Chart;
import de.saar.chorus.domgraph.chart.ChartSolver;
import de.saar.chorus.domgraph.chart.SolvedFormIterator;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.ubench.JDomGraph;

/**
 * A <code>JPanel</code> displaying a <code>JDomGraph</code>,
 * providing several informations on the graph needed by other
 * GUI-classes.
 * 
 * @author Alexander Koller
 * @author Michaela Regneri
 *
 */
public class JDomGraphTab extends JGraphTab  {
	
	// the grapb is initialized empty
	private JDomGraph graph = new JDomGraph();
	
	private DomGraph domGraph;
	
	
	private NodeLabels nodeLabels;
	
	// graph information concerning solving and identity
	boolean solvable,  isSolvedForm, isSolvedYet; 
	
	
	// managing solved forms
	long 	solvedForms, // number of solved forms 
			currentForm; // current solved form
	
	// name of graph and the tab (when inserted to a
	// tabed pane)
	private String defaultName, graphName;
	
	// solvedFormIterator of the graph
	private SolvedFormIterator solvedFormIterator;

	
	// layout preferences of the current graph
    private Preferences recentLayout;
    
    private JPanel statusBar;
    
    private String barCode;
    
    private CommandListener listener;
    
    //  shows the number of the recent solved form
	private JTextField solvedForm;
    
	
	
	

	
	
	
	/**
	 * Constructor to set up a tab with a dominance graph.
	 * The graph is solved if necessary.
	 * 
	 * @param theGraph the graph
	 * @param name the name for the tab
	 * @param paintNow if set to true, the graph is layoutet at once
	 */
	public JDomGraphTab(JDomGraph theGraph, DomGraph origin, String name, 
			boolean paintNow, CommandListener lis, NodeLabels labels) {
		
		// initializing fields
		defaultName = name;
		graphName = name;
		graph = null;
		listener = lis;
		domGraph = origin;
		nodeLabels = labels;
		
		Chart chart = new Chart();
		ChartSolver solver = new ChartSolver(origin, chart);
		
		solvedFormIterator = new SolvedFormIterator(chart, origin);
		// a new solvedFormIterator and a new converter initialized
		// with the given graph
	/*	if(Preferences.utoolPresent()) {
			solvedFormIterator = new DomSolver();
			conv = new JDomGraphConverter(solvedFormIterator);
			conv.toDomGraph(theGraph);
		}*/
		
		isSolvedYet = false;
		isSolvedForm = false;
		solvable = true;
		solvedForms = -1;
		setBackground(Color.WHITE);
		
		try {
			
			// graph layout
            graph = theGraph;
            
            // comute fragments 
            theGraph.computeFragments();
			
            // if it should be painted directly, the 
            // graph is layoutet.
			if(paintNow) {
				JFrame f = new JFrame("JGraph Test");
				f.add(theGraph);
				f.pack();
				repaintIfNecessary();
			}

            add(graph);
            
            // error message if layout fails
		} catch (Exception e) {
			JOptionPane.showMessageDialog(Main.getWindow(),
					"An error occurred while laying out this graph.",
					"Error during layout",
					JOptionPane.ERROR_MESSAGE);
		}
        
		statusBar = new DominanceGraphBar();
		barCode = Main.getStatusBar().insertBar(statusBar);
	}
	
	/**
	 * Solve this tab's graph if it isn't solved yet.
	 *
	 */
	public void solve() {
		if( ! isSolvedYet ) {
			Chart chart = new Chart();
			ChartSolver solver = new ChartSolver(domGraph, chart);
			if(solver.solve()) {
				solvedForms = chart.countSolvedForms().longValue();
				isSolvedYet = true;
			}
			statusBar = new DominanceGraphBar();
			solvedFormIterator = new SolvedFormIterator(chart,domGraph);
			barCode = Main.getStatusBar().insertBar(statusBar);
		}
	
	}
	
	
	/**
	 * @return Returns the isSolvedForm.
	 */
	public boolean isSolvedForm() {
		return isSolvedForm;
	}
	

	/**
	 * @param isSolvedForm The isSolvedForm to set.
	 */
	public void setSolvedForm(boolean isSolvedForm) {
		this.isSolvedForm = isSolvedForm;
	}
	

	
	public void resetSolvedFormText() {
		solvedForm.setText(String.valueOf(currentForm));
	}

	/**
	
	

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
	 * @return Returns the number of solved forms.
	 */
	public long getSolvedForms() {
		return solvedForms;
	}
	

	/**
	 * @param solvedForms The solvedForms to set.
	 */
	public void setSolvedForms(long solvedForms) {
		this.solvedForms = solvedForms;
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
	public void setSolvedYet(boolean isSolvedYet) {
		this.isSolvedYet = isSolvedYet;
	}

	

	/**
	 * @return Returns the solvedForm.
	 */
	public JTextField getSolvedForm() {
		return solvedForm;
	}

	/**
	 * @param solvedForm The solvedForm to set.
	 */
	public void setSolvedForm(JTextField solvedForm) {
		this.solvedForm = solvedForm;
	}

	
	
	
	/**
	 * @return Returns the currentForm.
	 */
	public long getCurrentForm() {
		return currentForm;
	}
	/**
	 * @param currentForm The currentForm to set.
	 */
	public void setCurrentForm(long currentForm) {
		this.currentForm = currentForm;
	}
	/**
	 * @return Returns the myGraph.
	 */
	public String getGraphName() {
		return graphName;
	}
	/**
	 * @param myGraph The myGraph to set.
	 */
	public void setGraphName(String myGraph) {
		this.graphName = myGraph;
	}

    

    /**
     * @return Returns the recentLayout.
     */
    public Preferences getRecentLayout() {
        return recentLayout;
    }
    
    
    
    
    
    /**
     * @param recentLayout The recentLayout to set.
     */
    public void setRecentLayout(Preferences recentLayout) {
        this.recentLayout = recentLayout;
    }
    
     /**
      * Repaints the graph if its layout is not consistent
      * with the recent layout preferences.
      */
    public void repaintIfNecessary() {
        if( (recentLayout == null) || Preferences.mustUpdateLayout(recentLayout) ) {
            graph.setShowLabels(Preferences.getInstance().isShowLabels());
            graph.computeLayout();
            
            graph.adjustNodeWidths();
            updateRecentLayout();
        }
    }
    
    /**
     * Updates the graph's layout preferences
     * by adopting the recent global layout 
     * preferences.
     */
    public void updateRecentLayout() {
        if( recentLayout == null ) {
            recentLayout =  Preferences.getInstance().clone();
        } else {
            Preferences.getInstance().copyTo(recentLayout);
        }
    }
    
    
    /*** methods for hiding JDomGraph from classes in leonardo.gui ***/
    
    /**
     * @return number of the graph's nodes
     */
    public int numGraphNodes() {
        return graph.getNodes().size();
    }
    
    /**
     * @return a clone of the displayed graph
     */
    public JDomGraph getCloneOfGraph() {
        return graph.clone();
    }
    
    /**
     * Resets the layout to its initial 
     * version.
     */
    public void resetLayout() {
        graph.setScale(1);
        graph.computeLayout();
        graph.adjustNodeWidths();
    }
    
    /**
     * Changes the graph's scale.
     * 
     * @param s the scale (percentage of the original one)
     */
    public void setGraphScale(double s) {
        graph.setScale(s);
    }
    
    /**
     * @return the scale (percentage of the original one)
     */
    public double getGraphScale() {
        return graph.getScale();
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
    	private JPanel classified; // the panel for the classify symbols
    	private JButton solve; 	// for solving 
    	
    	// the text labels
    	private JLabel 	numberOfForms, 	// indicates how many solved forms there are
    					norm, 		  	// indicates normality (graphs)
    					comp, 		  	// indicates compactness (graphs)
    					hn, 			// indicates hypernormality (graphs)
    					ll; 		 	// indicates leaf labeling (graphs)
    	
    	
    	private Set<JLabel> classifyLabels;
    	
    	private BorderLayout layout = new BorderLayout();
    	
    	/**
    	  * Sets up a new <code>SolvedFormBar</code> by
    	  * initalizing the fields and doing the layout.
    	  * 
    	  * TODO do the layout properly (that is 'more aesthetic')
    	  */
    	private DominanceGraphBar() {
    		super(); 
    		setLayout(layout);
    		
    		numberOfForms = new JLabel("");
    		
    		layout.setHgap(50);
    		layout.setVgap(5);
    		
    		if( isSolvedYet ) {
    			numberOfForms.setText("This graph has " + String.valueOf(solvedForms) + " solved forms.");
    		} else {
    			numberOfForms.setText("This graph has an unknown number of solved forms.");    		
    			}
    		
    		
    		add(numberOfForms, BorderLayout.CENTER);
    		layout.addLayoutComponent(numberOfForms,BorderLayout.CENTER);
    		
    		// solve button
    		solve = new JButton("SOLVE");
    		solve.setActionCommand("solve");
    		solve.addActionListener(listener);
    		solve.setPreferredSize(new Dimension(80,25));
    		
    		add(solve, BorderLayout.WEST);
    		layout.addLayoutComponent(solve, BorderLayout.WEST);
    		/*
    		 * Every label is set up with its "standard" character
    		 * and the tooltip-text gets a new position (above the
    		 * symbol itself).
    		 */
    		
    		classified = new JPanel();
    		classifyLabels = new HashSet<JLabel>();
    		
    		ll = new JLabel("L") {
    			public Point getToolTipLocation(MouseEvent e) {
    				
    				Point p1 = ll.getLocation();
    				Point toReturn = new Point(p1.x, p1.y-25);
    				return toReturn;
    			}
    		};
    		ll.setForeground(Color.RED);
    		classifyLabels.add(ll);
    		
    		hn = new JLabel("H") {
    			public Point getToolTipLocation(MouseEvent e) {
    				Point p1 =hn.getLocation();
    				Point toReturn = new Point(p1.x, p1.y-25);
    				return toReturn;
    			}
    		};
    		hn.setForeground(Color.RED);
    		classifyLabels.add(hn);
    		
    		norm = new JLabel("N") {
    			public Point getToolTipLocation(MouseEvent e) {
    				Point p1 = norm.getLocation();
    				Point toReturn = new Point(p1.x, p1.y-25);
    				return toReturn;
    			}
    		};
    		norm.setForeground(Color.RED);
    		classifyLabels.add(norm);
    		
    		comp = new JLabel("C") {
    			public Point getToolTipLocation(MouseEvent e) {
    				Point p1 = comp.getLocation();
    				Point toReturn = new Point(p1.x, p1.y-25);
    				return toReturn;
    			}
    		};
    		comp.setForeground(Color.RED);
    		classifyLabels.add(comp);
    		
    		
    			
    			
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
        		
        		
        		if(domGraph.isCompact()) {
        			comp.setText("C");
        			comp.setToolTipText("Compact");
        		} else if (domGraph.isCompactifiable()) {
        			comp.setText("c");
        			comp.setToolTipText("compactifiable");
        		} else {
        			comp.setText("-");
        			comp.setToolTipText("Not Compactifiable");
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
        		classified.add(comp);
        		
        		classified.add(ll);
        		classified.add(hn);
        		
        		classified.setForeground(Color.RED);
        		classified.setAlignmentX(SwingConstants.LEFT);
        		
        		add(classified, BorderLayout.EAST);
    			
    		
    		
    		
    			//solve.setEnabled(false);
    		
    	}
    }


    
    /**
     * Overwrites the <code>finalize</code>-Method of 
     * <code>Object</code>.
     * Removes the related status bar of this tab from the
     * <code>JDomGraphStatusBar</code> of <code>Main</code>.
     */
    protected void finalize() throws Throwable {
    	try {
    		Main.getStatusBar().removeBar(statusBar);
    	} finally {
    		super.finalize();
    	}
    }


	/**
	 * @param solvedFormIterator The solvedFormIterator to set.
	 */
	public void setSolvedFormIterator(SolvedFormIterator solvedFormIterator) {
		this.solvedFormIterator = solvedFormIterator;
	}


	/**
	 * @return Returns the domGraph.
	 */
	public DomGraph getDomGraph() {
		return domGraph;
	}


	/**
	 * @param domGraph The domGraph to set.
	 */
	public void setDomGraph(DomGraph domGraph) {
		this.domGraph = domGraph;
	}
	/**
	 * @return Returns the nodeLabels.
	 */
	public NodeLabels getNodeLabels() {
		return nodeLabels;
	}


	/**
	 * @param nodeLabels The nodeLabels to set.
	 */
	public void setNodeLabels(NodeLabels nodeLabels) {
		this.nodeLabels = nodeLabels;
	}

	/* (non-Javadoc)
	 * @see de.saar.chorus.ubench.gui.JGraphTab#clone()
	 */
	@Override
	public JGraphTab clone() {
		JDomGraph jdomCl = graph.clone();
		DomGraph domCl = (DomGraph) domGraph.clone();
		
		JDomGraphTab myClone = new JDomGraphTab(jdomCl, domCl,
				defaultName,true, listener, nodeLabels);
		
		return myClone;
	}

	/**
	 * @return Returns the barCode.
	 */
	public String getBarCode() {
		return barCode;
	}

	/**
	 * @param barCode The barCode to set.
	 */
	public void setBarCode(String barCode) {
		this.barCode = barCode;
	}

	/**
	 * @return Returns the defaultName.
	 */
	public String getDefaultName() {
		return defaultName;
	}

	/**
	 * @param defaultName The defaultName to set.
	 */
	public void setDefaultName(String defaultName) {
		this.defaultName = defaultName;
	}

	/**
	 * @return Returns the graph.
	 */
	public JDomGraph getGraph() {
		return graph;
	}

	/**
	 * @param graph The graph to set.
	 */
	public void setGraph(JDomGraph graph) {
		this.graph = graph;
	}

	/**
	 * @return Returns the listener.
	 */
	public CommandListener getListener() {
		return listener;
	}

	/**
	 * @param listener The listener to set.
	 */
	public void setListener(CommandListener listener) {
		this.listener = listener;
	}

	/**
	 * @return Returns the statusBar.
	 */
	public JPanel getStatusBar() {
		return statusBar;
	}

	/**
	 * @param statusBar The statusBar to set.
	 */
	public void setStatusBar(JPanel statusBar) {
		this.statusBar = statusBar;
	}

	/**
	 * @return Returns the solvedFormIterator.
	 */
	public SolvedFormIterator getSolvedFormIterator() {
		return solvedFormIterator;
	}

	
    
}
