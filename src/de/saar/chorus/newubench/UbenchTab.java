package de.saar.chorus.newubench;

import java.awt.BorderLayout;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.domgraph.layout.JDomGraphCanvas;
import de.saar.chorus.domgraph.layout.LayoutAlgorithm;
import de.saar.chorus.domgraph.layout.LayoutOptions;
import de.saar.chorus.domgraph.layout.LayoutOptions.LabelType;
import de.saar.chorus.jgraph.JScrollableJGraph;
import de.saar.chorus.ubench.jdomgraph.JDomGraph;

abstract public class UbenchTab extends JPanel {
	private JPanel graphPanel, statusBarPanel;
	protected JDomGraph jgraph;
	protected String label;
	
	protected UbenchTab(String label) {
		this.label = label;
		
		setLayout(new BorderLayout());
		
		graphPanel = new JPanel();
		graphPanel.setLayout(new BorderLayout());
		//graphPanel.setBorder(new EtchedBorder());
		add(graphPanel, BorderLayout.CENTER);
		
		statusBarPanel = new JPanel();
		statusBarPanel.setLayout(new BorderLayout());
		//statusBarPanel.setBorder(new LineBorder(Color.black));
		add(statusBarPanel, BorderLayout.SOUTH);
		
		setStatusBar(new JLabel("** this is the status bar **"));
		
		jgraph = new JDomGraph();
		jgraph.setLabeltype(LabelType.LABEL);
		graphPanel.add(new JScrollableJGraph(jgraph), BorderLayout.CENTER);
	}
	
	protected void setStatusBar(JComponent statusBar) {
		statusBarPanel.removeAll();
		statusBarPanel.add(statusBar, BorderLayout.CENTER);
		statusBarPanel.validate();
	}
	

	
	protected void setSolvingInProgressStatusBar() {
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		
		JProgressBar progress = new JProgressBar();
		progress.setString("Solving ...");
		progress.setIndeterminate(true);
		
		p.add(progress, BorderLayout.CENTER);
		setStatusBar(p);
	}
	
	protected void drawGraph(final DomGraph graph, final NodeLabels labels) {
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
					} catch (Exception e) {
						throw new UnsupportedOperationException(e);
					}
				}
			});
		}
	}
	
	
	
	
	private static final long serialVersionUID = -5841770553185589414L;
}
