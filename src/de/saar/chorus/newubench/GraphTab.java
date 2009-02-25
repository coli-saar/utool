package de.saar.chorus.newubench;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import de.saar.basic.GUIUtilities;
import de.saar.basic.GenericFileFilter;
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
	public void saveAllSolvedFormsToFilechooser() {
		final CodecFileChooser fc = new CodecFileChooser(
				Ubench.getInstance().getLastPath().getAbsolutePath(),
				CodecFileChooser.Type.EXPORT);

		fc.addCodecFileFilters(Ubench.getInstance().getMultiOutputCodecFileFilters());
		fc.setCurrentDirectory(Ubench.getInstance().getLastPath());
		fc.setAcceptAllFileFilterUsed(false);		

		int fcVal = GUIUtilities.confirmFileOverwriting(fc, Ubench.getInstance().getWindow());

		if( fcVal == JFileChooser.APPROVE_OPTION ) {
			File file = fc.getSelectedFile();
			Ubench.getInstance().setLastPath( file.getParentFile() );

			String defaultExtension = ((GenericFileFilter) fc.getFileFilter()).getExtension();
			if( !file.getName().endsWith(defaultExtension) ) {
				file = new File(file.getAbsolutePath() + defaultExtension);
			}
			
			final File finalFile = file; // grr


			new ResumingSwingThread() {
				@Override
				public void executeAsynchronously() {
					solve();
					
					if( reducedChart != null ) {
						setInProgressStatusBar("Computing solved forms...", reducedChart.countSolvedForms().intValue());

						try {
							int count = 0;
							SolvedFormIterator sfi = new SolvedFormIterator(reducedChart, graph);
							MultiOutputCodec codec = (MultiOutputCodec) Ubench.getInstance().getCodecManager().getOutputCodecForFilename(finalFile.getName(),fc.getCodecOptions());
							Writer writer = new FileWriter(finalFile);

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
						} catch (IOException e) {
							JOptionPane
							.showMessageDialog(
									Ubench.getInstance().getWindow(),
									"An error occurred while saving a solved form: " + e,
									"Error during save", JOptionPane.ERROR_MESSAGE);
						} catch (MalformedDomgraphException e) {
							JOptionPane
							.showMessageDialog(
									Ubench.getInstance().getWindow(),
									"A solved form couldn't be saved with this codec: " + e,
									"Error during save", JOptionPane.ERROR_MESSAGE);
						} finally {
							setSolvedStatusBar();
						}
					}
				}

				@Override
				public void thenWhat() {
					if( reducedChart != null ) {
						JOptionPane
						.showMessageDialog(
								Ubench.getInstance().getWindow(),
								"Successfully saved " + reducedChart.countSolvedForms() + " solved forms.",
								"Saved solved forms", JOptionPane.INFORMATION_MESSAGE);
					}
				}
			}.start();
		}
	}


	@SuppressWarnings("unchecked")
	public void showFirstSolvedForm() {
		new ResumingSwingThread() {
			@Override
			public void executeAsynchronously() {
				solve();
			}

			@Override
			public void thenWhat() {
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
}
