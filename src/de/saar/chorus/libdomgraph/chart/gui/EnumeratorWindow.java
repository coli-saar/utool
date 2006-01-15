package de.saar.chorus.libdomgraph.chart.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import de.saar.chorus.ubench.JDomGraph;

public class EnumeratorWindow extends JFrame {

	List<JDomGraph> solvedForms;
	JPanel buttonPanel;
	Set<JButton> buttons;
	StandardListener listener;
	
	public EnumeratorWindow( List<JDomGraph> solvedForms ) {
		super("Solved Form Enumerator");
		this.solvedForms = solvedForms;
		buttonPanel = new JPanel();
		buttons = new HashSet<JButton>();
		listener = new StandardListener();
		for( int i=0; i < solvedForms.size(); i++ ) {
			JButton recentButton = new JButton(String.valueOf(i + 1));
			recentButton.setActionCommand(String.valueOf(i));
			recentButton.addActionListener(listener);
			buttons.add(recentButton);
			buttonPanel.add(recentButton);
		}
		
		
		buttonPanel.validate();
		add(buttonPanel, BorderLayout.CENTER);
		add(new JTextArea("Chose solved form."), BorderLayout.SOUTH);
		pack();
		validate();
	}
	
	public void windowClosing(WindowEvent e) {
       System.exit(0);
	}
	
	class StandardListener implements ActionListener {

		/* (non-Javadoc)
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		public void actionPerformed(ActionEvent e) {
			int sF = Integer.parseInt(e.getActionCommand());
			JFrame sFWindow = new JFrame("Solved form No. " + (sF+1));
			
			JDomGraph nextSolvedForm = solvedForms.get(sF);
			JFrame helperWindow = new JFrame();
			helperWindow.add(nextSolvedForm);
			helperWindow.pack();
			nextSolvedForm.computeLayout();
			nextSolvedForm.adjustNodeWidths();
			sFWindow.add(nextSolvedForm);
			sFWindow.pack();
			sFWindow.validate();
			sFWindow.validate();
			sFWindow.setVisible(true);
		}
		
	}
}
