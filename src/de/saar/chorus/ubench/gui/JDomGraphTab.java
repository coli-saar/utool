package de.saar.chorus.ubench.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import de.saar.chorus.domgraph.chart.Chart;
import de.saar.chorus.domgraph.chart.ChartSolver;
import de.saar.chorus.domgraph.chart.SolvedFormIterator;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.ubench.DomGraphTConverter;
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
	

	// graph information concerning solving and identity
	boolean solvable, isSolvedYet; 
	Chart chart;
	DomGraph compactGraph;
	
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
		
		super(theGraph, origin, name, lis, labels);
	
		// initializing
		graphName = name;
		compactGraph = domGraph.compactify();
		
		
		solvable = true;
		solvedForms = -1;
		setBackground(Color.WHITE);
		
		if(Preferences.isAutoCount()) {
		
				solve();
			}
		
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
            try {
                chart = new Chart();
                ChartSolver solver = new ChartSolver(compactGraph, chart);
                
                if(solver.solve()) {
                    solvedForms = chart.countSolvedForms().longValue();
                    isSolvedYet = true;
                }
                
                statusBar = new DominanceGraphBar();
                barCode = Main.getStatusBar().insertBar(statusBar);
            } catch( OutOfMemoryError e ) {
                chart = null;
                isSolvedYet = false;
                
                JOptionPane.showMessageDialog(Main.getWindow(),
                        "The solver ran out of memory while solving this graph. "
                        + "Try increasing the heap size with the -Xmx option.",
                        "Out of memory",
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
	public void setSolvedYet(boolean isSolvedYet) {
		this.isSolvedYet = isSolvedYet;
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
    			
    		
    		
    	}
    }





	/**
	 * Returns a <code>JGraphTab</code> identic to this one
	 * but containing clones of the <code>DomGraph</code> and the 
	 * <code>JDomGraph</code>
	 */
	public JGraphTab clone() {
		JDomGraph jdomCl = graph.clone();
		DomGraph domCl = (DomGraph) domGraph.clone();
		
		JDomGraphTab myClone = new JDomGraphTab(jdomCl, domCl,
				defaultName,true, listener, nodeLabels);
		
		return myClone;
	}


	public JSolvedFormTab createFirstSolvedForm() {
		if(! isSolvedYet ) {
			solve();
		}
	
		if( isSolvedYet ) {
		    DomGraph firstForm = (DomGraph) domGraph.clone();
		    
		    solvedFormIterator = new SolvedFormIterator(chart,domGraph);
		    firstForm.setDominanceEdges(solvedFormIterator.next());
		    
		    DomGraphTConverter conv = new DomGraphTConverter(firstForm, nodeLabels);
		    JDomGraph domSolvedForm = conv.getJDomGraph();
		    JSolvedFormTab sFTab = new JSolvedFormTab(domSolvedForm, 
		            defaultName  + "  SF #1", 
		            solvedFormIterator, firstForm,
		            1, solvedForms, 
		            graphName, 
		            listener, nodeLabels);
            
            return sFTab;
        } else {
            return null;
        }
        
		
		
	}
	
	
    
}
