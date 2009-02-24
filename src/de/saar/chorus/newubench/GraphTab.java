package de.saar.chorus.newubench;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import de.saar.chorus.domgraph.chart.Chart;
import de.saar.chorus.domgraph.chart.ChartSolver;
import de.saar.chorus.domgraph.chart.ConcreteRegularTreeGrammar;
import de.saar.chorus.domgraph.chart.GraphBasedNonterminal;
import de.saar.chorus.domgraph.chart.SolvedFormIterator;
import de.saar.chorus.domgraph.chart.SolverNotApplicableException;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.ubench.Preferences.LayoutType;

public class GraphTab extends UbenchTab {
	private DomGraph graph;
	private NodeLabels labels;
	private Chart chart;
	private ConcreteRegularTreeGrammar<? extends GraphBasedNonterminal> reducedChart;

	public GraphTab(String label, DomGraph graph, NodeLabels labels) {
		super(label);

		this.graph = graph;
		this.labels = labels;
		
		jgraph.setLayouttype(LayoutType.JDOMGRAPH);

		drawGraph(graph, labels);
		setUnsolvedStatusBar();

		if( true ) { // Preferences.isAutoCount()
			
		}
	}

	
	
	private void setSolvedStatusBar() {
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.LINE_AXIS));
		
		JButton solveButton = new JButton("SOLVE");
		solveButton.setActionCommand(CommandListener.SOLVE);
		solveButton.addActionListener(Ubench.getInstance().getCommandListener());
		
		p.add(solveButton);
		p.add(new JLabel("This graph has " + reducedChart.countSolvedForms() + " solved form(s)."));
		p.add(Box.createHorizontalGlue());
		p.add(new GraphClassificationPanel(graph));
		
		setStatusBar(p);
	}
	
	private void setUnsolvedStatusBar() {
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.LINE_AXIS));
		
		JButton solveButton = new JButton("SOLVE");
		solveButton.setActionCommand(CommandListener.SOLVE);
		solveButton.addActionListener(Ubench.getInstance().getCommandListener());
		
		p.add(solveButton);
		p.add(new JLabel("This graph hasn't been solved yet."));
		p.add(Box.createHorizontalGlue());
		p.add(new GraphClassificationPanel(graph));
		
		setStatusBar(p);
	}
	
	private void solve() {
		if( chart == null ) {
			chart = new Chart(labels);

			setSolvingInProgressStatusBar();
			
			// TODO - move this into another thread so it doesn't block the UI

			try {
				ChartSolver.solve(graph, chart);
			} catch (SolverNotApplicableException e) {
				e.printStackTrace();
			}

			reducedChart = chart;
			setSolvedStatusBar();
		}
	}
	
	@SuppressWarnings("unchecked")
	public void showFirstSolvedForm() {
		solve();
		
		SolvedFormIterator sfi = new SolvedFormIterator(reducedChart, graph);
		Ubench.getInstance().getTabManager().addSolvedFormTab("SF of " + label, sfi, reducedChart.countSolvedForms().intValue(), graph, labels);
	}
	
	private static final long serialVersionUID = -6342451939382113666L;
}
