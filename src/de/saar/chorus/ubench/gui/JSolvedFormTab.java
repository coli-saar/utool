package de.saar.chorus.ubench.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import de.saar.chorus.domgraph.chart.Chart;
import de.saar.chorus.domgraph.chart.ChartSolver;
import de.saar.chorus.domgraph.chart.SolvedFormIterator;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.domgraph.layout.JDomGraphCanvas;
import de.saar.chorus.domgraph.layout.LayoutAlgorithm;
import de.saar.chorus.domgraph.layout.LayoutOptions;
import de.saar.chorus.ubench.JDomGraph;
import de.saar.chorus.ubench.chartviewer.ChartViewer;

/**
 * A <code>JPanel</code> displaying a <code>JDomGraph</code>
 * which is in solved form and providing several informations on the graph needed by other
 * GUI-classes.
 * 
 * @see JGraphTab
 * @see JDomGraphTab
 * 
 * @author Alexander Koller
 * @author Michaela Regneri
 *
 */
public class JSolvedFormTab extends JGraphTab {

	private DomGraph theSolvedForm;
	/**
	 * 
	 */
	private static final long serialVersionUID = -8144093325748384465L;

	long currentForm; // current solved form
	   
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
			SolvedFormIterator solv, DomGraph origin, DomGraph originalForm, 
			long form, long allForms, 
			String gName, CommandListener lis, NodeLabels labels) {
		
		super(solvedForm, origin, name, lis, labels);
		// initializing fields
		solvedFormIterator = solv;
		theSolvedForm = originalForm;
		currentForm = form;
		solvedForms = solv.getChart().countSolvedForms().longValue();
        recentLayout = null;
		graphName = gName;
		layout = Preferences.LayoutType.TREELAYOUT;
		// layouting the graph
		scrollpane = new JScrollPane(graph);
        add(scrollpane, BorderLayout.CENTER);
        
		
		statusBar = new SolvedFormBar(solvedForms, form, gName);
		barCode = Ubench.getInstance().getStatusBar().insertBar(statusBar);
        
		
	}
	
	void drawGraph() {
		JDomGraphCanvas canvas = new JDomGraphCanvas(graph);
		LayoutAlgorithm drawer = layout.getLayout();
		
		// this is a tree, there won't be redundand edges.
		// saving time by hard-coding this...
		drawer.layout(theSolvedForm, nodeLabels, canvas,new LayoutOptions(getLabelType(), 
				false));
		Ubench.getInstance().refresh();
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
   	/**
	 * 
	 */
	private static final long serialVersionUID = -3995240911677086695L;
	private JPanel classified,
   	formScroll;
   	private JButton sLeft, 	// for showing the next solved form
   					sRight; // for showing the previous solved form
   	
   	
   	
   	private JLabel 	solvedFormNo, // left side of the solved-form-scroller
   					of,  		  // right side of the solved-form-scroller
   					norm,  		  // indicates normality (solved forms)
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
   		super();
   		GridBagLayout layout = new GridBagLayout();
   		setLayout(layout);
   		
   		GridBagLayout fSlayout = new GridBagLayout();
   		formScroll = new JPanel(fSlayout);
   		
   		
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
   		
   		if( currentForm == Ubench.getInstance().getVisibleTab().getSolvedForms() ) {
   			sRight.setEnabled(false);
   		} else {
   			sRight.setEnabled(true);
   		}
   		
   		solvedForm = new JTextField(String.valueOf(form));
   		solvedForm.setColumns(5);
   		solvedForm.setHorizontalAlignment(JTextField.RIGHT);
   		solvedForm.addActionListener(listener);
   		solvedForm.setActionCommand("solvedFormDirectSelection");
   		
   		
   		solvedFormNo = new JLabel("Solved form ");
   		formScroll.add(solvedFormNo);
   		
   		GridBagConstraints buttonConstraints = new GridBagConstraints();
   		buttonConstraints.insets = new Insets(0,10,0,10);
   		
   		fSlayout.setConstraints(sLeft,buttonConstraints);
   		fSlayout.setConstraints(sRight,buttonConstraints);
   		
   		formScroll.add(sLeft);
   		formScroll.add(solvedForm);
   		formScroll.add(sRight);
   		
   		
   		of = new JLabel("of " + String.valueOf(numOfSolvForms) + " (Source: " + gN + ")");
   		
   		
   		formScroll.add(of);
   		
   		GridBagConstraints fSConstraints = new GridBagConstraints();
   		fSConstraints.anchor = GridBagConstraints.CENTER;
   		fSConstraints.weightx = 1.0;
   		fSConstraints.gridx = 3;
   		layout.setConstraints(formScroll, fSConstraints);
   		
   		add(formScroll);
   		
   		classified = new JPanel();
   		
   		/*
   		 * Every label is set up with its "standard" character
   		 * and the tooltip-text gets a new position (above the
   		 * symbol itself).
   		 */
   		
   		ll = new JLabel("L") {
   			/**
			 * 
			 */
			private static final long serialVersionUID = 8577612771757000214L;

			public Point getToolTipLocation(MouseEvent e) {
   				
   				Point p1 = ll.getLocation();
   				Point toReturn = new Point(p1.x, p1.y-25);
   				return toReturn;
   			}
   		};
   		ll.setForeground(Color.RED);
   		
   		hn = new JLabel("H") {
   			/**
			 * 
			 */
			private static final long serialVersionUID = 4662083424900606811L;

			public Point getToolTipLocation(MouseEvent e) {
   				Point p1 =hn.getLocation();
   				Point toReturn = new Point(p1.x, p1.y-25);
   				return toReturn;
   			}
   		};
   		hn.setForeground(Color.RED);
   		
   		norm = new JLabel("N") {
   			/**
			 * 
			 */
			private static final long serialVersionUID = 1253480187037359978L;

			public Point getToolTipLocation(MouseEvent e) {
   				Point p1 = norm.getLocation();
   				Point toReturn = new Point(p1.x, p1.y-25);
   				return toReturn;
   			}
   		};
   		norm.setForeground(Color.RED);
   		
   	
   		
   		
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
   		
   		setMaximumSize(new Dimension(getMaximumSize().width, 21));
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
	 * @param solvedForm The solvedForm to set.
	 */
	public void setSolvedForm(JTextField solvedForm) {
		this.solvedForm = solvedForm;
	}

	

/**
 * Returns a <code>JGraphTab</code> identic to this one
 * but containing clones of the <code>DomGraph</code> and the 
 * <code>JDomGraph</code>
 */
public JGraphTab clone() {
	DomGraph cl = (DomGraph) domGraph.clone();
	DomGraph sf = (DomGraph) theSolvedForm.clone();
	JSolvedFormTab myClone = new JSolvedFormTab(graph.clone(), defaultName, solvedFormIterator,
			cl, sf, currentForm, solvedForms, graphName, listener, nodeLabels);
	
	return myClone;
}

@Override
public void displayChart() {
	Chart c = new Chart();
	ChartSolver.solve(domGraph,c);
	cv = new ChartViewer(c, domGraph,
			defaultName, graph, nodeLabels);
}
   
}
