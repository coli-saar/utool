package de.saar.chorus.newubench;

import java.awt.BorderLayout;
import java.io.IOException;
import java.io.Writer;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import de.saar.chorus.domgraph.codec.MalformedDomgraphException;
import de.saar.chorus.domgraph.codec.OutputCodec;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.domgraph.layout.JDomGraphCanvas;
import de.saar.chorus.domgraph.layout.LayoutAlgorithm;
import de.saar.chorus.domgraph.layout.LayoutOptions;
import de.saar.chorus.domgraph.layout.LayoutOptions.LabelType;
import de.saar.chorus.jgraph.JScrollableJGraph;
import de.saar.chorus.ubench.jdomgraph.JDomGraph;
import de.saar.swing.SwingNowExecutor;

abstract public class UbenchTab extends JPanel {
	private JPanel graphPanel, statusBarPanel;
	protected JDomGraph jgraph;
	protected String label;
	private JProgressBar currentProgressBar;

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

	protected void setStatusBar(final JComponent statusBar) {
		SwingNowExecutor.execute(new Runnable() {
			public void run() {
				statusBarPanel.removeAll();
				statusBarPanel.add(statusBar, BorderLayout.CENTER);
				statusBarPanel.validate();
			}
		});
	}



	protected void setInProgressStatusBar(final String message, int max) {
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.LINE_AXIS));

		p.add(new JLabel(message + "    "));

		currentProgressBar = new JProgressBar();
		if( max == 0 ) {
			currentProgressBar.setIndeterminate(true);
		} else {
			currentProgressBar.setMaximum(max);
			currentProgressBar.setValue(0);
		}

		p.add(currentProgressBar);
		setStatusBar(p);
	}

	protected void updateProgressBar(final int n) {
		SwingNowExecutor.execute(new Runnable() {
			public void run() {
				currentProgressBar.setValue(n);
			}
		});
	}

	protected void drawGraph(final DomGraph graph, final NodeLabels labels) {
		if(! graph.getAllNodes().isEmpty()) {
			SwingUtilities.invokeLater(new Thread() {
				public void run() {
					try {
						jgraph.clear();
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

	public void printGraph(Writer buf, OutputCodec codec) throws IOException, MalformedDomgraphException {
		codec.print_header(buf);
		codec.encode(getGraph(), getNodeLabels(), buf);
		codec.print_footer(buf);
	}

	abstract protected DomGraph getGraph();
	abstract protected NodeLabels getNodeLabels();
	abstract public UbenchTab duplicate();

	private static final long serialVersionUID = -5841770553185589414L;
}
