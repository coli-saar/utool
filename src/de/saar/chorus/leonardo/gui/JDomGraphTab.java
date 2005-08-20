package de.saar.chorus.leonardo.gui;

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

import de.saar.chorus.leonardo.JDomGraph;
import de.saar.chorus.leonardo.utool.JDomGraphConverter;
import de.saar.chorus.libdomgraph.ConstraintClasses;
import de.saar.chorus.libdomgraph.DomSolver;

/**
 * A <code>JPanel</code> displaying a <code>JDomGraph</code>,
 * providing several informations on the graph needed by other
 * GUI-classes.
 * 
 * @author Alexander Koller
 * @author Michaela Regneri
 *
 */
public class JDomGraphTab extends JPanel {
	
	// the grapb is initialized empty
	private JDomGraph graph = new JDomGraph();
	
	// graph information concerning solving and identity
	boolean solvable,  isSolvedForm, isSolvedYet; 
	
	
	// managing solved forms
	long 	solvedForms, // number of solved forms 
			currentForm; // current solved form
	
	// name of graph and the tab (when inserted to a
	// tabed pane)
	private String defaultName, graphName;
	
	// solver of the graph
	private DomSolver solver;
	
	// converter 
	private JDomGraphConverter conv;
	
	// layout preferences of the current graph
    private Preferences recentLayout;
    
    private JPanel statusBar;
    
    private String barCode;
    
    private CommandListener listener;
    
    //  shows the number of the recent solved form
	private JTextField solvedForm;
    
	
	
	/**
	 * Constructor for setting up a tab with a solved form.
	 * 
	 * @param solvedForm the graph
	 * @param name the name for the tab
	 * @param solv the solver related to the graph
	 */
	public JDomGraphTab(JDomGraph solvedForm, String name, 
			DomSolver solv, long form, long allForms, 
			String gName, CommandListener lis) {
		
		// initializing fields
		listener = lis;
		defaultName = name;
		graph = solvedForm;
		solver = solv;
		currentForm = form;
		isSolvedYet = false;
		isSolvedForm = true;
		solvable = false;
		solvedForms = solv.countSolvedForms();
        recentLayout = null;
		graphName = gName;
		
		setBackground(Color.WHITE);
		
		// layouting the graph
		graph.computeFragments();
		
		JFrame f = new JFrame("JGraph Test");
		f.add(graph);
		f.pack();
        
		repaintIfNecessary();
        
		add(graph);
		
		// don't need to solve solved forms...
		isSolvedYet = true;
		
		statusBar = new SolvedFormBar(solvedForms, form, gName);
		barCode = Main.getStatusBar().insertBar(statusBar);
	}
	
	
	/**
	 * Constructor to set up a tab with a dominance graph.
	 * The graph is solved if necessary.
	 * 
	 * @param theGraph the graph
	 * @param name the name for the tab
	 * @param paintNow if set to true, the graph is layoutet at once
	 */
	public JDomGraphTab(JDomGraph theGraph, String name, 
			boolean paintNow, CommandListener lis) {
		
		// initializing fields
		defaultName = name;
		graphName = name;
		graph = null;
		listener = lis;
		
		// a new solver and a new converter initialized
		// with the given graph
		if(Preferences.utoolPresent()) {
			solver = new DomSolver();
			conv = new JDomGraphConverter(solver);
			conv.toDomGraph(theGraph);
		}
		
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
			solver.solve();
			solvedForms = solver.countSolvedForms();
			isSolvedYet = true;
			statusBar = new DominanceGraphBar();
			barCode = Main.getStatusBar().insertBar(statusBar);
		}
	}
	
	public String getBarcode() {
		return barCode;
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
	

	/**
	 * @return Returns the graph.
	 */
	public JDomGraph getGraph() {
		return graph;
	}
	
	public void resetSolvedFormText() {
		solvedForm.setText(String.valueOf(currentForm));
	}

	/**
	 * @deprecated doesn't work :)
	 * @param graph The graph to set.
	 */
	public void setGraph(JDomGraph graph, String newName) {
		if(isSolvedForm) {
			
			remove(this.graph);
			this.graph = graph;
			defaultName = newName;
			//graph.computeFragments();
			JFrame f = new JFrame("JGraph Test");
			f.add(graph);
			f.pack();
			repaintIfNecessary();
			add(graph);
			
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
	 * @return Returns the conv.
	 */
	public JDomGraphConverter getConv() {
		return conv;
	}

	/**
	 * @param conv The conv to set.
	 */
	public void setConv(JDomGraphConverter conv) {
		this.conv = conv;
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
	 * @return Returns the solver.
	 */
	public DomSolver getSolver() {
		
		return solver;
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
	 * @param solver The solver to set.
	 */
	public void setSolver(DomSolver solver) {
		this.solver = solver;
	}
	
	/**
	 * Fit the graph of this tab to the tab size.
	 */
	public void fitGraph() {
		
		// computing tab height (and add a little space...)
		double myHeight = Main.getTabHeight()*0.9;
		double myWidth = Main.getTabWidth()*0.9;
		
		// checking the current (!) graph height
		double graphHeight = graph.getHeight()*(1/graph.getScale());
		double graphWidth = graph.getWidth()*(1/graph.getScale());
		
		// computing the scale
		double scale = Math.min(1 , Math.min((double) myHeight/graphHeight, (double) myWidth/graphWidth));

		graph.setScale(scale);
		
		Main.resetSlider();
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
     * a solved form, to be inserted into the <code>CardLayout</code>
     * of <code>JDomGraphStatusBar</code>.
     * 
     * @author Michaela Regneri
     *
     */
    private class SolvedFormBar extends JPanel {
    	private JPanel classified,
    	formScroll;
    	private JButton sLeft, 	// for showing the next solved form
    					sRight; // for showing the previous solved form
    	
    	
    	
    	private JLabel 	of,  		  // right side of the solved-form-scroller
    					norm,  		  // indicates normality (solved forms)
    					comp,   		  // indicates compactness (solved forms)
    					hn, 		  // indicates hypernormality(solved forms)
    					ll;			  // indicates leaf labeling (solved forms)
    	
    	/**
    	 * Sets up a new <code>SolvedFormBar</code> by
    	 * initalizing the fields and doing the layout.
    	 * 
    	 * @param numOfSolvForms the number of solved forms of the related dominance graph
    	 * @param form the number of this solved form
    	 * @param gN the name of the related dominance grap
    	 */
    	private SolvedFormBar(long numOfSolvForms, long form, String gN) {
    		super(new BorderLayout());
    		
    		formScroll = new JPanel();
    		
    		// button to go forward
    		sLeft = new JButton("<");
    		sLeft.setFont(new Font("SansSerif",Font.BOLD,16));
    		sLeft.setPreferredSize(new Dimension(50,20));
    		sLeft.setActionCommand("minus");
    		sLeft.addActionListener(listener);
    		
    		// button to go backwards
    		sRight = new JButton(">");
    		sRight.setPreferredSize(new Dimension(50,20));
    		sRight.setFont(new Font("SansSerif",Font.BOLD,16));
    		sRight.setActionCommand("plus");
    		sRight.addActionListener(listener);
    		
    		if( currentForm == 1 ) {
    			sLeft.setEnabled(false);
    		} else {
    			sLeft.setEnabled(true);
    		}
    		
    		if( currentForm == Main.getVisibleTab().getSolvedForms() ) {
    			sRight.setEnabled(false);
    		} else {
    			sRight.setEnabled(true);
    		}
    		
    		solvedForm = new JTextField(String.valueOf(form));
    		solvedForm.setColumns(5);
    		solvedForm.setHorizontalAlignment(JTextField.RIGHT);
    		solvedForm.addActionListener(listener);
    		solvedForm.setActionCommand("solvedFormDirectSelection");
    		
    		formScroll.add(sLeft);
    		formScroll.add(solvedForm);
    		formScroll.add(sRight);
    		
    		of = new JLabel("of " + String.valueOf(numOfSolvForms) + " (Graph: " + gN + ")");
    		
    		formScroll.add(of, BorderLayout.EAST);
    		add(formScroll, BorderLayout.CENTER);
    		
    		classified = new JPanel();
    		
    		/*
    		 * Every label is set up with its "standard" character
    		 * and the tooltip-text gets a new position (above the
    		 * symbol itself).
    		 */
    		
    		ll = new JLabel("L") {
    			public Point getToolTipLocation(MouseEvent e) {
    				
    				Point p1 = ll.getLocation();
    				Point toReturn = new Point(p1.x, p1.y-25);
    				return toReturn;
    			}
    		};
    		ll.setForeground(Color.RED);
    		
    		hn = new JLabel("H") {
    			public Point getToolTipLocation(MouseEvent e) {
    				Point p1 =hn.getLocation();
    				Point toReturn = new Point(p1.x, p1.y-25);
    				return toReturn;
    			}
    		};
    		hn.setForeground(Color.RED);
    		
    		norm = new JLabel("N") {
    			public Point getToolTipLocation(MouseEvent e) {
    				Point p1 = norm.getLocation();
    				Point toReturn = new Point(p1.x, p1.y-25);
    				return toReturn;
    			}
    		};
    		norm.setForeground(Color.RED);
    		
    		
    		comp = new JLabel("C") {
    			public Point getToolTipLocation(MouseEvent e) {
    				Point p1 = comp.getLocation();
    				Point toReturn = new Point(p1.x, p1.y-25);
    				return toReturn;
    			}
    		};
    		comp.setForeground(Color.RED);
    		
    		int graphValue = solver.classify();
    		
    		
    		if(( ConstraintClasses.NORMAL & graphValue) == ConstraintClasses.NORMAL) {
    			norm.setText("N");
    			norm.setToolTipText("Normal");
    			
    		} else if ((	ConstraintClasses.WEAKLY_NORMAL & graphValue) == ConstraintClasses.WEAKLY_NORMAL) {
    			norm.setText("n");
    			norm.setToolTipText("Weakly Normal");
    		} else {
    			norm.setText("-");
    			norm.setToolTipText("Not Normal");
    		}
    		
    		
    		if(( ConstraintClasses.COMPACT & graphValue) == ConstraintClasses.COMPACT) {
    			comp.setText("C");
    			comp.setToolTipText("Compact");
    		} else if ((	ConstraintClasses.COMPACTIFIABLE & graphValue) == ConstraintClasses.COMPACTIFIABLE) {
    			comp.setText("c");
    			comp.setToolTipText("compactifiable");
    		} else {
    			comp.setText("-");
    			comp.setToolTipText("Not Compactifiable");
    		}
    		
    		
    		if((ConstraintClasses.HN_CONNECTED & graphValue) == ConstraintClasses.HN_CONNECTED) {
    			hn.setText("H");
    			hn.setToolTipText("Hypernormally Connected");
    		} else {
    			hn.setText("-");
    			hn.setToolTipText("Not Hypernormally Connected");
    		}
    		
    		if((ConstraintClasses.LEAF_LABELLED & graphValue) == ConstraintClasses.LEAF_LABELLED) {
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
    	}
    	
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
    	  * TODO do the layout properly (that is more aesthetic)
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
    		
    		if( Preferences.utoolPresent() ) {
    			int graphValue = solver.classify();
    			
    			
    			if(( ConstraintClasses.NORMAL & graphValue) == ConstraintClasses.NORMAL) {
    				norm.setText("N");
    				norm.setToolTipText("Normal");
    				
    			} else if ((	ConstraintClasses.WEAKLY_NORMAL & graphValue) == ConstraintClasses.WEAKLY_NORMAL) {
    				norm.setText("n");
    				norm.setToolTipText("Weakly Normal");
    			} else {
    				norm.setText("-");
    				norm.setToolTipText("Not Normal");
    			}
    			
    			
    			if(( ConstraintClasses.COMPACT & graphValue) == ConstraintClasses.COMPACT) {
    				comp.setText("C");
    				comp.setToolTipText("Compact");
    			} else if ((	ConstraintClasses.COMPACTIFIABLE & graphValue) == ConstraintClasses.COMPACTIFIABLE) {
    				comp.setText("c");
    				comp.setToolTipText("compactifiable");
    			} else {
    				comp.setText("-");
    				comp.setToolTipText("Not Compactifiable");
    			}
    			
    			
    			if((ConstraintClasses.HN_CONNECTED & graphValue) == ConstraintClasses.HN_CONNECTED) {
    				hn.setText("H");
    				hn.setToolTipText("Hypernormally Connected");
    			} else {
    				hn.setText("-");
    				hn.setToolTipText("Not Hypernormally Connected");
    			}
    			
    			if((ConstraintClasses.LEAF_LABELLED & graphValue) == ConstraintClasses.LEAF_LABELLED) {
    				ll.setText("L");
    				ll.setToolTipText("Leaf-Labelled");
    			} else {
    				ll.setText("-");
    				ll.setToolTipText("Not Leaf-Labelled");
    			}
    			
    		} else {
    			for( JLabel label : classifyLabels ) {
    				label.setText("??");
    				label.setToolTipText("No Classifying available.");
    			}
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
    		
    		if( ! Preferences.utoolPresent() ) {
    			solve.setEnabled(false);
    		}
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
    
    
}
