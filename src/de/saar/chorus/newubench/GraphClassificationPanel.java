package de.saar.chorus.newubench;

import javax.swing.JLabel;
import javax.swing.JPanel;

import de.saar.chorus.domgraph.graph.DomGraph;

public class GraphClassificationPanel extends JPanel {
	private JLabel normalLabel, llLabel, hncLabel;
	
	public GraphClassificationPanel() {
		normalLabel = new JLabel("X");
		llLabel = new JLabel("Y");
		hncLabel = new JLabel("Z");
		
		add(normalLabel);
		add(new JLabel("  "));
		add(llLabel);
		add(new JLabel("  "));
		add(hncLabel);
	}
	
	public GraphClassificationPanel(DomGraph graph) {
		this();
		
		analyzeGraph(graph);
	}
	
	public void analyzeGraph(DomGraph graph) {
		
	}

	private static final long serialVersionUID = -7127679535879306421L;
}
