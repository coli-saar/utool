package de.saar.chorus.newubench;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JTabbedPane;

import de.saar.chorus.domgraph.chart.SolvedFormIterator;
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
	
	public void addSolvedFormTab(String label, SolvedFormIterator sfi, DomGraph graph, NodeLabels labels) {
		add(label, new SolvedFormTab(label, sfi, graph, labels));
	}
	
	public JTabbedPane getTabbedPane() {
		return tabbedPane;
	}
	
	public UbenchTab getCurrentTab() {
		return (UbenchTab) tabbedPane.getSelectedComponent();
	}
}
