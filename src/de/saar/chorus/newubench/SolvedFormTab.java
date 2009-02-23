package de.saar.chorus.newubench;

import java.math.BigInteger;
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

@SuppressWarnings("unchecked")
public class SolvedFormTab extends UbenchTab {
	private DomGraph baseGraph, currentSolvedForm;
	private NodeLabels labels;
	private SolvedFormIterator sfi;
	private List<SolvedFormSpec> computedSolvedForms;
	private JTextField sfSelectionTextField;
	private int numSolvedForms, currentSolvedFormIndex;

	protected SolvedFormTab(String label, SolvedFormIterator sfi, int numSolvedForms, DomGraph graph, NodeLabels labels) {
		super(label);
		
		this.baseGraph = graph;
		this.labels = labels;
		this.sfi = sfi;
		this.numSolvedForms = numSolvedForms;

		currentSolvedFormIndex = 0;
		
		jgraph.setLayouttype(LayoutType.TREELAYOUT);
		
		computedSolvedForms = new ArrayList<SolvedFormSpec>();
		setSolvedFormStatusBar();

		sfSelectionTextField.setText("1");
		showSolvedForm(0);
	}
	
	public void showSolvedForm(int index) {
		SolvedFormSpec spec = getSolvedForm(index);
		currentSolvedForm = baseGraph.makeSolvedForm(spec);
		drawGraph(currentSolvedForm, labels);
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
			showSolvedForm(currentSolvedFormIndex);
			sfSelectionTextField.setText("" + (currentSolvedFormIndex+1));
		}
	}
	
	public void showPreviousSolvedForm() {
		if( currentSolvedFormIndex > 0 ) {
			currentSolvedFormIndex--;
			showSolvedForm(currentSolvedFormIndex);
			sfSelectionTextField.setText("" + (currentSolvedFormIndex+1));
		}
	}
	
	
	private SolvedFormSpec getSolvedForm(int index) {
		for( int i = computedSolvedForms.size(); i <= index; i++ ) {
			computedSolvedForms.add(sfi.next());
		}
		
		return computedSolvedForms.get(index);
	}
	
	private void setSolvedFormStatusBar() {
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.LINE_AXIS));

		JButton nextButton = new JButton(">");
		nextButton.setActionCommand(CommandListener.NEXT);
		nextButton.addActionListener(Ubench.getInstance().getCommandListener());
		
		JButton prevButton = new JButton("<");
		prevButton.setActionCommand(CommandListener.PREVIOUS);
		prevButton.addActionListener(Ubench.getInstance().getCommandListener());

		sfSelectionTextField = new JTextField(5);
		sfSelectionTextField.setActionCommand(CommandListener.JUMP_TO_SF);
		sfSelectionTextField.addActionListener(Ubench.getInstance().getCommandListener());
		
		p.add(new JLabel("Solved form "));
		p.add(prevButton);
		p.add(sfSelectionTextField);
		p.add(nextButton);
		p.add(new JLabel(" of " + numSolvedForms));
		p.add(Box.createHorizontalGlue());
		p.add(new GraphClassificationPanel(currentSolvedForm));
		
		setStatusBar(p);
	}
	

	private static final long serialVersionUID = 3795056217569335558L;
}
