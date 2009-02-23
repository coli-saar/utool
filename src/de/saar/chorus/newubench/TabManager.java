package de.saar.chorus.newubench;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JTabbedPane;

import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;

public class TabManager {
	private List<UbenchTab> tabs;
	private JTabbedPane tabbedPane;
	
	public TabManager() {
		tabbedPane = new JTabbedPane();
		tabs = new ArrayList<UbenchTab>();
	}
	
	public void addDomGraphTab(String label, DomGraph graph, NodeLabels labels) {
		UbenchTab tab = new GraphTab(label, graph, labels);
		tabs.add(tab);
		
		tabbedPane.addTab(label, tab);
		tabbedPane.setSelectedComponent(tab);
		
		tabbedPane.validate();
	}
	
	public JTabbedPane getTabbedPane() {
		return tabbedPane;
	}
}
