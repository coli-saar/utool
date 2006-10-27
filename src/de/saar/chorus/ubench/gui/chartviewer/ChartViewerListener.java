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
 * 
 * @author Michaela Regneri
 *
 */
public class ChartViewerListener implements ActionListener {

	private ChartViewer viewer;
	private Map<Object, String> eventSources;
	private File lastpath;
	
	ChartViewerListener(ChartViewer cv) {
		viewer = cv;
		eventSources = new HashMap<Object,String>();
		lastpath = new File(System.getProperty("user.dir"));
	}
	
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		
		if( command.equals("delSplit") ) {
			Split selectedSplit = viewer.getSelectedSplit();
			if( selectedSplit != null ) {
				try {
					Chart chart = viewer.getChart();
					Set<String> subgraph = viewer.getSubgraphForMarkedSplit();
					List<Split> splits = new ArrayList<Split>(chart.getSplitsFor(subgraph));
					
					splits.remove(selectedSplit);
					chart.setSplitsForSubgraph(subgraph, splits);
					viewer.refreshChartWindow();
					
				} catch(UnsupportedOperationException ex) {
					JOptionPane.showMessageDialog(viewer,
							"You cannot delete the selected Split." + 
							System.getProperty("line.separator") + 
							"There has to be at least one Split left for each subgraph.",
							"Split not deleted",
							JOptionPane.ERROR_MESSAGE);
				}
				
			}
		} else if( command.equals("elred")) {
				EquationSystem eqs; 
				String name;
				if(Ubench.getInstance().isEquationSystemLoaded() ) {
					
					int yesno = JOptionPane.showConfirmDialog(viewer, 
							"The equation system " + 
							Ubench.getInstance().getEqsname() + " is loaded," + 
							System.getProperty("line.separator") + 
							"would you like to use it to reduce the chart?" + 
							System.getProperty("line.separator") + 
							"Press \"Yes\" to use " + 
							Ubench.getInstance().getEqsname() + " to reduce the chart,"  + 
							System.getProperty("line.separator") + 
									" press \"No\" to load another equation system.", 
							"Ready to Eliminate Equivalences", JOptionPane.YES_NO_OPTION, 
							JOptionPane.QUESTION_MESSAGE);
					if(yesno == JOptionPane.YES_OPTION ) {
						eqs = Ubench.getInstance().getEquationSystem();
						name = Ubench.getInstance().getEqsname();
					} else {
						eqs = new EquationSystem();
						 name = loadEquationSystem(true, eqs);
					}
				} else {
					eqs = new EquationSystem();
					name = loadEquationSystem(true, eqs);
				}
				
				
				viewer.reduceChart(eqs, name);
				viewer.refreshChartWindow();
				
		} else if( command.equals("solvechart")) {
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
			viewer.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			viewer.resetChart();
			viewer.setCursor(Cursor.getDefaultCursor());
		} else if ( command.equals("chartinfo") ) {
			viewer.showInfoPane();
		} else if ( command.equals("closechart") ) {
			viewer.setVisible(false);
		}

	}
	
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
		JFileChooser fc = new JFileChooser(lastpath);
		fc.setDialogTitle("Choose the equation system input file");
		fc.setFileFilter(Ubench.getInstance().getListener().new XMLFilter());
		
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
			lastpath = file.getParentFile();
			viewer.setCursor(Cursor.getDefaultCursor());
			viewer.refreshTitleAndStatus();
		}
		
		return toReturn;
	}

	public void registerEventSource(Object source, String command) {
		eventSources.put(source, command);
	}
	
	private String lookupEventSource(Object source) {
		return eventSources.get(source);
	}
	/* (non-Javadoc)
	 * @see java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
	 */
	

}
