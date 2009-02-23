package de.saar.chorus.newubench;

import javax.swing.SwingUtilities;

import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.domgraph.layout.JDomGraphCanvas;
import de.saar.chorus.domgraph.layout.LayoutAlgorithm;
import de.saar.chorus.domgraph.layout.LayoutOptions;

public class GraphTab extends UbenchTab {
	private DomGraph graph;
	private NodeLabels labels;

	public GraphTab(String label, DomGraph graph, NodeLabels labels) {
		super();

		this.graph = graph;
		this.labels = labels;

		drawGraph();
		validate();
	}

	private void drawGraph() {
		if(! graph.getAllNodes().isEmpty()) {
			SwingUtilities.invokeLater(new Thread() {
				public void run() {
					try {
						JDomGraphCanvas canvas = new JDomGraphCanvas(jgraph);
						LayoutAlgorithm drawer = jgraph.getLayoutType().getLayout();
						drawer.layout(graph, labels, canvas, 
								new LayoutOptions(jgraph.getLabeltype(), true)); //XX Preferences.isRemoveRedundantEdges()));
						canvas.finish();
						
						Ubench.getInstance().refresh();
						
						//Ubench.getInstance().refresh(true);
						//Preferences.setFitWindowToGraph(false);
					} catch (Exception e) {
						throw new UnsupportedOperationException(e);
					}
				}
			});
		}
	}
	
	private static final long serialVersionUID = -6342451939382113666L;
}
