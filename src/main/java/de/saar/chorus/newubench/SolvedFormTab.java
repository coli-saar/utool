package de.saar.chorus.newubench;

import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import de.saar.chorus.domgraph.chart.SolvedFormIterator;
import de.saar.chorus.domgraph.chart.SolvedFormSpec;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.ubench.Preferences.LayoutType;
import de.saar.swing.ResumingSwingThread;

@SuppressWarnings("unchecked")
public class SolvedFormTab extends UbenchTab {
	private DomGraph baseGraph, currentSolvedForm;
	private NodeLabels labels;
	private SolvedFormIterator sfi;
	private List<SolvedFormSpec> computedSolvedForms;
	private JTextField sfSelectionTextField;
	private int numSolvedForms, currentSolvedFormIndex;
	private JPanel statusBarPanel;

	protected SolvedFormTab(String label, SolvedFormIterator sfi, int numSolvedForms, DomGraph graph, NodeLabels labels) {
		super(label);
		
		this.baseGraph = graph;
		this.labels = labels;
		this.sfi = sfi;
		this.numSolvedForms = numSolvedForms;

		currentSolvedFormIndex = 0;
		
		jgraph.setLayouttype(LayoutType.TREELAYOUT);
		
		computedSolvedForms = new ArrayList<SolvedFormSpec>();

		showSolvedForm(0);
	}
	
	private SolvedFormTab(String label, int numSolvedForms, DomGraph baseGraph, NodeLabels labels) {
		super(label);
		
		this.baseGraph = baseGraph;
		this.labels = labels;
		this.numSolvedForms = numSolvedForms;
		
		jgraph.setLayouttype(LayoutType.TREELAYOUT);
	}
	
	private void showSolvedForm(final int index) {
		if( index > computedSolvedForms.size()) {
			setInProgressStatusBar("Computing solved form...", index - computedSolvedForms.size());
		}
		
		new ResumingSwingThread<SolvedFormSpec>() {
			@Override
			public SolvedFormSpec executeAsynchronously() {
				return getSolvedForm(index);
			}

			@Override
			public void thenWhat(SolvedFormSpec spec) {
				currentSolvedForm = baseGraph.makeSolvedForm(spec);
				drawGraph(currentSolvedForm, labels);
				
				setSolvedFormStatusBar();
			}
		}.start();
	}
	
	public void showSelectedSolvedForm() {
		int x = Integer.parseInt(sfSelectionTextField.getText()) - 1;
		
		if( (x >= 0) && (x < numSolvedForms) ) {
			currentSolvedFormIndex = x;
			showSolvedForm(currentSolvedFormIndex);
		}
	}
	
	public void showNextSolvedForm() {
		if( currentSolvedFormIndex < numSolvedForms-1 ) {
			currentSolvedFormIndex++;
			sfSelectionTextField.setText("" + (currentSolvedFormIndex+1));
			showSolvedForm(currentSolvedFormIndex);
		}
	}
	
	public void showPreviousSolvedForm() {
		if( currentSolvedFormIndex > 0 ) {
			currentSolvedFormIndex--;
			sfSelectionTextField.setText("" + (currentSolvedFormIndex+1));
			showSolvedForm(currentSolvedFormIndex);
		}
	}
	
	
	private SolvedFormSpec getSolvedForm(int index) {
		int len = computedSolvedForms.size();
		for( int i = 0; i <= index - len; i++ ) {
			computedSolvedForms.add(sfi.next());
			updateProgressBar(i);
		}
		
		return computedSolvedForms.get(index);
	}
	
	private void setSolvedFormStatusBar() {
		if( statusBarPanel == null ) {
			statusBarPanel = new JPanel();
			statusBarPanel.setLayout(new BoxLayout(statusBarPanel, BoxLayout.LINE_AXIS));

			JButton nextButton = new JButton(">");
			nextButton.setActionCommand(CommandListener.NEXT);
			nextButton.addActionListener(Ubench.getInstance().getCommandListener());

			JButton prevButton = new JButton("<");
			prevButton.setActionCommand(CommandListener.PREVIOUS);
			prevButton.addActionListener(Ubench.getInstance().getCommandListener());

			sfSelectionTextField = new JTextField(5);
			sfSelectionTextField.setText(String.valueOf(currentSolvedFormIndex+1));
			sfSelectionTextField.setMaximumSize(sfSelectionTextField.getPreferredSize());
			sfSelectionTextField.setActionCommand(CommandListener.JUMP_TO_SF);
			sfSelectionTextField.addActionListener(Ubench.getInstance().getCommandListener());

			statusBarPanel.add(new JLabel("Solved form "));
			statusBarPanel.add(prevButton);
			statusBarPanel.add(sfSelectionTextField);
			statusBarPanel.add(nextButton);
			statusBarPanel.add(new JLabel(" of " + numSolvedForms));
			statusBarPanel.add(Box.createHorizontalGlue());
			statusBarPanel.add(new GraphClassificationPanel(currentSolvedForm));
		}
		
		setStatusBar(statusBarPanel);
		validate();
	}
	

	private static final long serialVersionUID = 3795056217569335558L;

	@Override
	public DomGraph getGraph() {
		return currentSolvedForm;
	}

	@Override
	public NodeLabels getNodeLabels() {
		return labels;
	}

	@Override
	public UbenchTab duplicate() {
		SolvedFormTab ret = new SolvedFormTab(label, numSolvedForms, baseGraph, labels);
		
		ret.sfi = new SolvedFormIterator(sfi.getChart(), baseGraph);
		ret.computedSolvedForms = new ArrayList<SolvedFormSpec>(computedSolvedForms);
		ret.currentSolvedFormIndex = currentSolvedFormIndex;
		
		ret.showSolvedForm(currentSolvedFormIndex);
		
		return ret;
	}
}
