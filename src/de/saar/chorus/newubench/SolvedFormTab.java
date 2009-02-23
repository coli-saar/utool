package de.saar.chorus.newubench;

import java.util.ArrayList;
import java.util.List;

import de.saar.chorus.domgraph.chart.SolvedFormIterator;
import de.saar.chorus.domgraph.chart.SolvedFormSpec;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.ubench.Preferences.LayoutType;

@SuppressWarnings("unchecked")
public class SolvedFormTab extends UbenchTab {
	private DomGraph baseGraph, currentSolvedForm;
	private NodeLabels labels;
	private SolvedFormIterator sfi;
	private List<SolvedFormSpec> computedSolvedForms;

	protected SolvedFormTab(String label, SolvedFormIterator sfi, DomGraph graph, NodeLabels labels) {
		super(label);
		
		this.baseGraph = graph;
		this.labels = labels;
		this.sfi = sfi;
		
		jgraph.setLayouttype(LayoutType.TREELAYOUT);
		
		computedSolvedForms = new ArrayList<SolvedFormSpec>();
		showSolvedForm(0);
	}
	
	public void showSolvedForm(int index) {
		SolvedFormSpec spec = getSolvedForm(index);
		currentSolvedForm = baseGraph.makeSolvedForm(spec);
		drawGraph(currentSolvedForm, labels);
	}
	
	private SolvedFormSpec getSolvedForm(int index) {
		for( int i = computedSolvedForms.size(); i <= index; i++ ) {
			computedSolvedForms.add(sfi.next());
		}
		
		return computedSolvedForms.get(index);
	}
	

	private static final long serialVersionUID = 3795056217569335558L;
}
