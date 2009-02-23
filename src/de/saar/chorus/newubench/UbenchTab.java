package de.saar.chorus.newubench;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

import de.saar.chorus.domgraph.layout.LayoutOptions.LabelType;
import de.saar.chorus.jgraph.JScrollableJGraph;
import de.saar.chorus.ubench.Preferences.LayoutType;
import de.saar.chorus.ubench.jdomgraph.JDomGraph;

abstract public class UbenchTab extends JPanel {
	private JPanel graphPanel, statusBarPanel;
	protected JDomGraph jgraph;
	
	protected UbenchTab() {
		setLayout(new BorderLayout());
		
		graphPanel = new JPanel();
		graphPanel.setLayout(new BorderLayout());
		//graphPanel.setBorder(new EtchedBorder());
		add(graphPanel, BorderLayout.CENTER);
		
		statusBarPanel = new JPanel();
		add(statusBarPanel, BorderLayout.SOUTH);
		
		statusBarPanel.add(new JLabel("** this is the status bar **"));
		
		jgraph = new JDomGraph();
		jgraph.setLabeltype(LabelType.LABEL);
		jgraph.setLayouttype(LayoutType.JDOMGRAPH);
		graphPanel.add(new JScrollableJGraph(jgraph), BorderLayout.CENTER);
	}
	
	private static final long serialVersionUID = -5841770553185589414L;
}
