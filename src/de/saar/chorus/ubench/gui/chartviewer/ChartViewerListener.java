package de.saar.chorus.ubench.gui.chartviewer;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import de.saar.chorus.domgraph.chart.Chart;
import de.saar.chorus.domgraph.chart.SolvedFormIterator;
import de.saar.chorus.domgraph.chart.Split;
import de.saar.chorus.domgraph.equivalence.EquationSystem;
import de.saar.chorus.domgraph.equivalence.IndividualRedundancyElimination;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.ubench.DomGraphTConverter;
import de.saar.chorus.ubench.JDomGraph;
import de.saar.chorus.ubench.gui.JSolvedFormTab;
import de.saar.chorus.ubench.gui.Ubench;

/**
 * This <code>ActionListener</code> processes all actions
 * of one (and only one) <code>ChartViewer</code>. 
 * 
 * @see de.saar.chorus.ubench.gui.chartviewer.ChartViewer
 * @author Michaela Regneri
 *
 */
public class ChartViewerListener implements ActionListener {

	// the chart viewer
	private ChartViewer viewer;

	
	/**
	 * A new <code>ChartViewerListener</code>
	 * initalised with its <code>ChartViewer</code>.
	 * 
	 * @param cv the chart viewer
	 */
	ChartViewerListener(ChartViewer cv) {
		viewer = cv;
	}
	
	/**
	 * This processes all events occuring within
	 * the <code>ChartViewer</code>.
	 */
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		
		// redundancy elimination with the globally
		// loaded equation system
		if( command.equals("elredglobal") ) {
			
			// the graph is not normal
			if( ! viewer.getDg().isNormal() ) {
				JOptionPane.showMessageDialog(viewer,
						"This chart represents a graph which is not normal," + 
						System.getProperty("line.separator") + 
						"thus Utool cannot eliminate redundancies.",
						"Server Error",
						JOptionPane.ERROR_MESSAGE);
				
				return;
			}
			
			// the graph is not hypernormally connected
			if( ! viewer.getDg().isHypernormallyConnected()) {
				JOptionPane.showMessageDialog(viewer,
						"This chart represents a graph which is not hypernormally" + 
						System.getProperty("line.separator") + 
						"connected, thus Utool cannot eliminate redundancies.",
						"Server Error",
						JOptionPane.ERROR_MESSAGE);
				
				return;
			}
			
			// reduce the chart with the loaded system.
			// this event cannot occur when there is no
			// system loaded in ubench.
			viewer.reduceChart(
					Ubench.getInstance().getEquationSystem(), 
					Ubench.getInstance().getEqsname());
			
			// refreshing the chart
			viewer.refreshChartWindow();
		} else if( command.equals("delSplit") ) {
			// a Split was deleted
			
			Split selectedSplit = viewer.getSelectedSplit();
			if( selectedSplit != null ) {
				try {
					// remove the split from the chart itself
					Chart chart = viewer.getChart();
					Set<String> subgraph = viewer.getSubgraphForMarkedSplit();
					List<Split> splits = new ArrayList<Split>(chart.getSplitsFor(subgraph));
					
					splits.remove(selectedSplit);
					chart.setSplitsForSubgraph(subgraph, splits);
					viewer.refreshChartWindow();
					
				} catch(UnsupportedOperationException ex) {
					// a split which may not been removed
					JOptionPane.showMessageDialog(viewer,
							"You cannot delete the selected Split." + 
							System.getProperty("line.separator") + 
							"There has to be at least one Split left for each subgraph.",
							"Split not deleted",
							JOptionPane.ERROR_MESSAGE);
				}
				
			}
		} else if( command.equals("elred")) {
			// reduce the chart with any equation system
			
			// the graph is not normal
			if( ! viewer.getDg().isNormal() ) {
				JOptionPane.showMessageDialog(viewer,
						"This chart represents a graph which is not normal," + 
						System.getProperty("line.separator") + 
						"thus Utool cannot eliminate redundancies.",
						"Server Error",
						JOptionPane.ERROR_MESSAGE);
				
				return;
			}
			
			// the graph is not hnc
			if( ! viewer.getDg().isHypernormallyConnected()) {
				JOptionPane.showMessageDialog(viewer,
						"This chart represents a graph which is not hypernormally" + 
						System.getProperty("line.separator") + 
						"connected, thus Utool cannot eliminate redundancies.",
						"Server Error",
						JOptionPane.ERROR_MESSAGE);
				
				return;
			}
			
			
			// create a new equation systems and
			// fill it with a file to select
			
				EquationSystem eqs = new EquationSystem();
				String name  = loadEquationSystem(true, eqs);			
				
				// reduce the chart with the system built
				if(eqs != null && name != null) {
				viewer.reduceChart(eqs, name);
				viewer.refreshChartWindow();
				}
				
		} else if( command.equals("solvechart")) {
			// display the first solved form of the chart
			
			Chart chart = viewer.getChart();
			DomGraph firstForm = (DomGraph) viewer.getDg().clone();
			SolvedFormIterator sfi = new SolvedFormIterator(chart,firstForm);
			firstForm = firstForm.withDominanceEdges(sfi.next());
			
			DomGraphTConverter conv = new DomGraphTConverter(firstForm, viewer.getLabels());
			JDomGraph domSolvedForm = conv.getJDomGraph();
			
			JSolvedFormTab sFTab = new JSolvedFormTab(domSolvedForm, 
					viewer.getTitle()  + "  SF #1", 
					sfi, firstForm,
					1, chart.countSolvedForms().longValue(), 
					viewer.getTitle(), 
					Ubench.getInstance().getListener(), 
					viewer.getLabels());
			
			Ubench.getInstance().addTab(sFTab, true);
		} else if ( command.equals("resetchart") ) {
			// display the original chart again
			
			viewer.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			viewer.resetChart();
			viewer.setCursor(Cursor.getDefaultCursor());
		} else if ( command.equals("chartinfo") ) {
			// display information on the chart
			
			viewer.showInfoPane();
		} else if ( command.equals("closechart") ) {
			// close the window
			
			viewer.setVisible(false);
		}

	}
	
	/**
	 * This loads a xml file and reads the content
	 * to a xml file.
	 * 
	 * @param preliminary indicates whether or not to show the info message
	 * @param eqs the equation system to fill
	 * @return the (file) name of the equation system loaded
	 */
	private String loadEquationSystem(boolean preliminary, EquationSystem eqs) {
		String toReturn = null;
		if(preliminary) {
			JOptionPane.showMessageDialog(viewer,
					"You have to specify a xml file that contains your equation system" + 
					System.getProperty("line.separator") + 
					" before Utool can eliminate equivalences.",
					"Please load an equation system",
					JOptionPane.INFORMATION_MESSAGE);
		}
		JFileChooser fc = new JFileChooser();

		fc.setDialogTitle("Choose the equation system input file");
		fc.setFileFilter(Ubench.getInstance().getListener().new XMLFilter());
		fc.setCurrentDirectory(Ubench.getInstance().getLastPath());
		int fcVal = fc.showOpenDialog(viewer);	
		
		if(fcVal == JFileChooser.APPROVE_OPTION){
			
			viewer.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			
			File file = fc.getSelectedFile();
			
			try {
				eqs.read(new FileReader(file));
				//Ubench.getInstance().setEquationSystem(eqs, file.getName());
				toReturn = file.getName();
			} catch( Exception ex ) {
				JOptionPane.showMessageDialog(viewer,
						"The Equation System cannot be parsed." + 
						System.getProperty("line.separator") + 
						"Either the input file is not valid or it contains syntax errors.",
						"Error while loading equation system",
						JOptionPane.ERROR_MESSAGE);
			}


			Ubench.getInstance().setLastPath(file.getParentFile());
			viewer.setCursor(Cursor.getDefaultCursor());
			viewer.refreshTitleAndStatus();
		}
		
		return toReturn;
	}



}
