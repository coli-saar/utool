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

//	 managing solved forms
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
			SolvedFormIterator solv, DomGraph origin, long form, long allForms, 
			String gName, CommandListener lis, NodeLabels labels) {
		
		super(solvedForm, origin, name, lis, labels);
		// initializing fields
		solvedFormIterator = solv;
		currentForm = form;
		solvedForms = solv.getChart().countSolvedForms().longValue();
        recentLayout = null;
		graphName = gName;
		
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
	
	JSolvedFormTab myClone = new JSolvedFormTab(graph.clone(), defaultName, solvedFormIterator,
			cl, currentForm, solvedForms, graphName, listener, nodeLabels);
	
	return myClone;
}
   
}
