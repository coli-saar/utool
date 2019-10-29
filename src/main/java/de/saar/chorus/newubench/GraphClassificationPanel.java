package de.saar.chorus.newubench;

import java.awt.Color;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import de.saar.chorus.domgraph.graph.DomGraph;

public class GraphClassificationPanel extends JPanel {
	private JLabel normalLabel, llLabel, hncLabel;
	
	public GraphClassificationPanel() {
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		
		normalLabel = new JLabel("X");
		normalLabel.setForeground(Color.red);
		
		llLabel = new JLabel("Y");
		llLabel.setForeground(Color.red);
		
		hncLabel = new JLabel("Z");
		hncLabel.setForeground(Color.red);
		
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
		if( graph.isNormal() ) {
			normalLabel.setText("N");
			normalLabel.setToolTipText("The graph is normal.");
		} else if( graph.isWeaklyNormal() ) {
			normalLabel.setText("n");
			normalLabel.setToolTipText("The graph is weakly normal.");
		} else {
			normalLabel.setText("-");
			normalLabel.setToolTipText("The graph is not normal.");
		}
		
		if( graph.isLeafLabelled() ) {
			llLabel.setText("L");
			llLabel.setToolTipText("The graph is leaf-labelled.");
		} else {
			llLabel.setText("-");
			llLabel.setToolTipText("The graph is not leaf-labelled.");
		}
		
		if( graph.isHypernormallyConnected() ) {
			hncLabel.setText("H");
			hncLabel.setToolTipText("The graph is hypernormally connected.");
		} else {
			hncLabel.setText("-");
			hncLabel.setToolTipText("The graph is not hypernormally connected.");
		}
	}

	private static final long serialVersionUID = -7127679535879306421L;
}
