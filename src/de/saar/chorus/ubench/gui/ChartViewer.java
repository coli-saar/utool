package de.saar.chorus.ubench.gui;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import de.saar.chorus.domgraph.chart.Chart;
import de.saar.chorus.domgraph.chart.ChartPresenter;
import de.saar.chorus.domgraph.graph.DomGraph;

public class ChartViewer extends JFrame {

	private JTextPane prettyprint;
	private Chart chart;
	private DomGraph dg;
	
	
	ChartViewer(Chart c, DomGraph g, String title) {
		super("Chart of " + title);
		chart = c;
		prettyprint = new JTextPane();
		String textchart = ChartPresenter.chartOnlyRoots(c,g);
		prettyprint.setText(textchart);
		prettyprint.setEditable(false);
		add(new JScrollPane(prettyprint));
		
		//TODO perhaps this isn't such a good idea...
		setAlwaysOnTop(true);
		pack();
		validate();
		setVisible(true);
	}
	
	

}
