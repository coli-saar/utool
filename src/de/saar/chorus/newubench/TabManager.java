package de.saar.chorus.newubench;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

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
}
