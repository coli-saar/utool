package de.saar.chorus.newubench;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JTabbedPane;

import de.saar.chorus.domgraph.chart.SolvedFormIterator;
import de.saar.chorus.domgraph.codec.InputCodec;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;

public class TabManager {
	private List<UbenchTab> tabs;
	private JTabbedPane tabbedPane;
	
	public TabManager() {
		tabbedPane = new JTabbedPane();
		tabs = new ArrayList<UbenchTab>();
	}
	
	private void add(String label, UbenchTab tab) {
		tabs.add(tab);
		
		tabbedPane.addTab(label, tab);
		tabbedPane.setSelectedComponent(tab);
		
		tabbedPane.validate();
	}
	
	public void addDomGraphTab(String label, DomGraph graph, NodeLabels labels) {
		add(label, new GraphTab(label, graph, labels));
	}
	
	public void addDomGraphTab(String label, Reader reader, InputCodec inputCodec) {
		DomGraph graph = new DomGraph();
		NodeLabels labels = new NodeLabels();
		
		if( FileUtilities.genericLoadGraph(reader, inputCodec, graph, labels) ) {
			addDomGraphTab(label, graph, labels);
		}
	}
	
	@SuppressWarnings("unchecked")
	public void addSolvedFormTab(String label, SolvedFormIterator sfi, int numSolvedForms, DomGraph graph, NodeLabels labels) {
		add(label, new SolvedFormTab(label, sfi, numSolvedForms, graph, labels));
	}
	
	public JTabbedPane getTabbedPane() {
		return tabbedPane;
	}
	
	public UbenchTab getCurrentTab() {
		return (UbenchTab) tabbedPane.getSelectedComponent();
	}
	
	public void closeCurrentTab() {
		int index = tabbedPane.getSelectedIndex();
		
		tabbedPane.remove(index);
		tabs.remove(index);
	}
	
	public void duplicateCurrentTab() {
		int index = tabbedPane.getSelectedIndex();
		String label = tabbedPane.getTitleAt(index);
		UbenchTab tab = (UbenchTab) tabbedPane.getSelectedComponent();
		
		UbenchTab newTab = tab.duplicate();
		add(label, newTab);
	}
	
	public void addGraphFromFilechooser() {
		Map<String, String> codecOptions = new HashMap<String, String>();
		File file = FileUtilities.getFileFromOpenFileChooser(Ubench.getInstance().getInputCodecFileFilters(), codecOptions);
		
		if( file != null ) {
			try {
				addDomGraphTab(file.getName(), new FileReader(file), Ubench.getInstance().getCodecManager().getInputCodecForFilename(file.getName(), codecOptions));
			} catch (FileNotFoundException e) {
				AuxiliaryWindows.showErrorMessage("The file " + file + " couldn't be read.", "Error loading file");
			}
		}
	}
				
		

}
