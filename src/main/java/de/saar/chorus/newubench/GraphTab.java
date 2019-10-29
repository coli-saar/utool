package de.saar.chorus.newubench;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

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
import de.saar.chorus.domgraph.chart.SolvedFormSpec;
import de.saar.chorus.domgraph.codec.MalformedDomgraphException;
import de.saar.chorus.domgraph.codec.MultiOutputCodec;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.ubench.Preferences.LayoutType;
import de.saar.swing.ResumingSwingThread;

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

			setInProgressStatusBar("Computing RTG ...", 0);
			boolean success = false;

			try {
				success = ChartSolver.solve(graph, chart);
			} catch(Throwable e) {
				AuxiliaryWindows.showErrorMessage("An error occurred while solving " + label + ": " + e, "Error while solving graph");
				success = false;
			}

			if( success ) {
				reducedChart = chart;
				setSolvedStatusBar();
			} else {
				chart = null;
				reducedChart = null;
				setUnsolvedStatusBar();
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private int printAllSolvedForms(Writer writer, MultiOutputCodec codec) throws IOException, MalformedDomgraphException {
		int count = 0;
		SolvedFormIterator sfi = new SolvedFormIterator(reducedChart, graph);

		codec.print_header(writer);
		codec.print_start_list(writer);

		while(sfi.hasNext()) {
			SolvedFormSpec spec = sfi.next();
			count++;

			if( count > 1 ) {
				codec.print_list_separator(writer);
			}

			codec.encode(graph.makeSolvedForm(spec), labels, writer);
			updateProgressBar(count);
		}

		codec.print_end_list(writer);
		codec.print_footer(writer);
		
		return count;
	}

	public void saveAllSolvedFormsToFilechooser() {
		final Map<String,String> codecOptions = new HashMap<String,String>();
		final File file = FileUtilities.getFileFromExportFileChooser(Ubench.getInstance().getMultiOutputCodecFileFilters(), codecOptions);
		
		if( file != null ) {
			new ResumingSwingThread<Integer>() { // GraphTab for the dummy return value
				@Override
				public Integer executeAsynchronously() {
					int count = 0;
					solve();
					
					if( reducedChart != null ) {
						setInProgressStatusBar("Computing solved forms...", reducedChart.countSolvedForms().intValue());

						try {
							count = printAllSolvedForms(new FileWriter(file), (MultiOutputCodec) Ubench.getInstance().getCodecManager().getOutputCodecForFilename(file.getName(),codecOptions));
						} catch (IOException e) {
							AuxiliaryWindows.showErrorMessage("An error occurred while saving a solved form: " + e, "Error during save");
						} catch (MalformedDomgraphException e) {
							AuxiliaryWindows.showErrorMessage("A solved form couldn't be saved with this codec: " + e, "Error during save");
						} finally {
							setSolvedStatusBar();
						}
					}
					
					return count;
				}

				@Override
				public void thenWhat(Integer count) {
					if( reducedChart != null ) {
						AuxiliaryWindows.showInformationMessage("Successfully saved " + count + " solved forms.", "Saved solved forms");
					}
				}
			}.start();
		}
	}


	@SuppressWarnings("unchecked")
	public void showFirstSolvedForm() {
		new ResumingSwingThread<Object>() {
			@Override
			public Object executeAsynchronously() {
				solve();
				return this;
			}

			@Override
			public void thenWhat(Object dummy) {
				if( reducedChart != null ) {
					SolvedFormIterator sfi = new SolvedFormIterator(reducedChart, graph);
					Ubench.getInstance().getTabManager().addSolvedFormTab("SF of " + label, sfi, reducedChart.countSolvedForms().intValue(), graph, labels);
				}
			}
		}.start();
	}

	private static final long serialVersionUID = -6342451939382113666L;

	@Override
	public DomGraph getGraph() {
		return graph;
	}



	@Override
	public NodeLabels getNodeLabels() {
		return labels;
	}



	@Override
	@SuppressWarnings("unchecked")
	public UbenchTab duplicate() {
		GraphTab ret = new GraphTab(label, graph, labels);
		
		if( chart != null ) {
			ret.chart = (Chart) chart.clone();
			ret.reducedChart = (ConcreteRegularTreeGrammar<? extends GraphBasedNonterminal>) reducedChart.clone();
			
			ret.setSolvedStatusBar();
		}
		
		return ret;
	}
}
