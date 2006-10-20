package de.saar.basic;

import java.awt.BorderLayout;
import java.awt.Frame;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;

import de.saar.chorus.ubench.gui.JDialogListener;
import de.saar.chorus.ubench.gui.Ubench;

public class WaitingDialog extends JDialog {
	
	private final int tasklength = 100;
	private JProgressBar progressBar;
	private JButton ok;
	private JPanel dialogPane;
		
	public WaitingDialog(String text, Frame owner) {
		super(owner, text, false);
		dialogPane = new JPanel();
		progressBar = new JProgressBar(0, tasklength);
		
		// the OK-Button to press after printing is done
		// (it will close the dialog)
		ok = new JButton("OK");
		ok.setActionCommand("ok");
		
	
		
		// listener for the button 
		ok.addActionListener(new JDialogListener(this));
		
		progressBar.setStringPainted(true); 
		
		
		dialogPane.add(progressBar,BorderLayout.CENTER);
		dialogPane.add(ok,BorderLayout.SOUTH);
		dialogPane.doLayout();
		add(dialogPane);
		pack();
		validate();
//		 locating the panel centered
		setLocation((owner.getWidth() - getWidth())/2,
				(owner.getHeight() - getHeight())/2); 
		

	}
	
	public void beginTask() {
	
		progressBar.setString(""); 
		progressBar.setIndeterminate(true);
		ok.setEnabled(false);
		setVisible(true);
		
	}
	
	public void endTask() {
		progressBar.setMaximum(100);
		progressBar.setIndeterminate(false);
		progressBar.setValue(100);
		progressBar.setString("Finished.");
		
		// new text
		setTitle("Done!");
		
		dialogPane.validate();
		
		// enabling the button that closes
		// the dialog pane.
		ok.setEnabled(true);
	}
	
}
