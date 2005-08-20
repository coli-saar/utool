package de.saar.chorus.leonardo.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JDialog;

/**
 * A standard <code>ActionListener</code> to close a <code>JDialog</code>
 * right after the OK button has been pressed.
 * 
 * @author Alexander Koller
 *
 */
public class JDialogListener implements ActionListener {

	JDialog dialog;	// the dialog
	
	/**
	 * A new Instance of <code>JDialogListener</Code>
	 * 
	 * @param di the dialog
	 */
	public JDialogListener(JDialog di) {
		dialog = di;
	}
	
	/**
	 * Overwriting the <code>actionPerformed</code> method
	 * of <code>ActionListener</code>.
	 */
	public void actionPerformed(ActionEvent e) {
		
		// if the dialog's OK-button has caused
		// the action, the dialog is closed.
		if(e.getActionCommand().equals("ok")) {
			dialog.setVisible(false);
		}
		
	}

}
