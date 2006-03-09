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
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import de.saar.chorus.domgraph.chart.SolvedFormIterator;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.ubench.JDomGraph;


public class JSolvedFormTab extends JGraphTab {
//	 the grapb is initialized empty
	private JDomGraph graph = new JDomGraph();
	
	private DomGraph domGraph;
	
	
	private NodeLabels nodeLabels;
//	 managing solved forms
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
	 * Constructor for setting up a tab with a solved form.
	 * 
	 * @param solvedForm the graph
	 * @param name the name for the tab
	 * @param solv the solvedFormIterator related to the graph
	 */
	public JSolvedFormTab(JDomGraph solvedForm, String name, 
			SolvedFormIterator solv, DomGraph origin, long form, long allForms, 
			String gName, CommandListener lis, NodeLabels labels) {
		
		// initializing fields
		listener = lis;
		defaultName = name;
		graph = solvedForm;
		domGraph = origin;
		nodeLabels = labels;
		solvedFormIterator = solv;
		currentForm = form;
		solvedForms = solv.getChart().countSolvedForms().longValue();
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
		
		statusBar = new SolvedFormBar(solvedForms, form, gName);
		barCode = Main.getStatusBar().insertBar(statusBar);
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
   	}
   	
   }
	
	public void resetSolvedFormText() {
		solvedForm.setText(String.valueOf(currentForm));
	}
	
	/**
	 * @return Returns the solvedForm.
	 */
	public JTextField getSolvedForm() {
		return solvedForm;
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
	 * @return Returns the graphName.
	 */
	public String getGraphName() {
		return graphName;
	}



	/**
	 * @param graphName The graphName to set.
	 */
	public void setGraphName(String graphName) {
		this.graphName = graphName;
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
	 * @return Returns the solvedFormIterator.
	 */
	public SolvedFormIterator getSolvedFormIterator() {
		return solvedFormIterator;
	}



	/**
	 * @param solvedFormIterator The solvedFormIterator to set.
	 */
	public void setSolvedFormIterator(SolvedFormIterator solvedFormIterator) {
		this.solvedFormIterator = solvedFormIterator;
	}



	/**
	 * @return Returns the solvedForms.
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
	 * @param solvedForm The solvedForm to set.
	 */
	public void setSolvedForm(JTextField solvedForm) {
		this.solvedForm = solvedForm;
	}

	

/* (non-Javadoc)
	 * @see de.saar.chorus.ubench.gui.JGraphTab#getCloneOfGraph()
	 */
	@Override
	public JDomGraph getCloneOfGraph() {
		// TODO Auto-generated method stub
		return graph.clone();
	}



	/* (non-Javadoc)
	 * @see de.saar.chorus.ubench.gui.JGraphTab#numGraphNodes()
	 */
	@Override
	public int numGraphNodes() {
		// TODO Auto-generated method stub
		return graph.getNodes().size();
	}



/* (non-Javadoc)
 * @see de.saar.chorus.ubench.gui.JGraphTab#clone()
 */
@Override
public JGraphTab clone() {
	DomGraph cl = (DomGraph) domGraph.clone();
	
	JSolvedFormTab myClone = new JSolvedFormTab(graph.clone(), defaultName, solvedFormIterator,
			cl, currentForm, solvedForms, graphName, listener, nodeLabels);
	
	return myClone;
}
   
}
