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
public class ChartViewerListener implements ActionListener, ItemListener {

	private ChartViewer viewer;
	private Map<Object, String> eventSources;
	
	ChartViewerListener(ChartViewer cv) {
		viewer = cv;
		eventSources = new HashMap<Object,String>();
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
		} else if (command.equals("loadeqs")) {
			
			boolean askforreduction = true;
			if(Ubench.getInstance().isEquationSystemLoaded()) {
				askforreduction = false;
			}
			String eqsname = loadEquationSystem(false);
			if( eqsname != null && askforreduction ) {
				int yesno = JOptionPane.showConfirmDialog(viewer, 
						"The Equation System " + eqsname + " is loaded now." + 
						System.getProperty("line.separator") + 
						"Would you like to eliminate the equivalences immediately?", 
						"Ready to Eliminate Equivalences", JOptionPane.YES_NO_OPTION, 
						JOptionPane.QUESTION_MESSAGE);
				if(yesno == JOptionPane.YES_OPTION) {
					viewer.reduceChart();
					viewer.refreshChartWindow();
				}
			}
			
		}else if( command.equals("elred")) {
			 
				if(! Ubench.getInstance().isEquationSystemLoaded() ) {
					loadEquationSystem(true);
				}
				viewer.reduceChart();
				viewer.refreshChartWindow();
				
		} else if( command.equals("solvechart")) {
			Chart chart = viewer.getChart();
			DomGraph firstForm = (DomGraph) viewer.getDg().clone();
			SolvedFormIterator sfi = new SolvedFormIterator(chart,firstForm);
			firstForm.setDominanceEdges(sfi.next());
			
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
		}

	}
	
	private String loadEquationSystem(boolean preliminary) {
		String toReturn = null;
		if(preliminary) {
			JOptionPane.showMessageDialog(viewer,
					"You have to specify a xml file that contains your equation system" + 
					System.getProperty("line.separator") + 
					" before Utool can eliminate equivalences.",
					"Please load an equation system",
					JOptionPane.INFORMATION_MESSAGE);
		}
		JFileChooser fc = new JFileChooser(System.getProperty("user.dir"));
		fc.setDialogTitle("Choose the equation system input file");
		fc.setFileFilter(Ubench.getInstance().getListener().new XMLFilter());
		
		int fcVal = fc.showOpenDialog(viewer);	
		
		if(fcVal == JFileChooser.APPROVE_OPTION){
			
			viewer.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			
			File file = fc.getSelectedFile();
			EquationSystem eqs = new EquationSystem();
			try {
				eqs.read(new FileReader(file));
				Ubench.getInstance().setEquationSystem(eqs);
				toReturn = file.getName();
			} catch( Exception ex ) {
				JOptionPane.showMessageDialog(viewer,
						"The Equation System cannot be parsed." + 
						System.getProperty("line.separator") + 
						"Either the input file is not valid or it contains syntax errors.",
						"Error while Loading Equation System",
						JOptionPane.ERROR_MESSAGE);
			}
			viewer.setCursor(Cursor.getDefaultCursor());
			viewer.refreshStatusBar();
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
	public void itemStateChanged(ItemEvent e) {
		String command = lookupEventSource(e.getSource());
		if(command.equals("autoreduce")) {
			if(e.getStateChange() == ItemEvent.SELECTED ) {
				Ubench.getInstance().reduceAutomatically = true;
				if(! Ubench.getInstance().isEquationSystemLoaded() ) {
					loadEquationSystem(true);
				}
				if(! viewer.isReduced() ) {
					viewer.reduceChart();
					viewer.refreshChartWindow();
				}
			} else {
				Ubench.getInstance().reduceAutomatically = false;
			}
		}
	}

}
