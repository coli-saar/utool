package de.saar.chorus.leonardo.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;

/**
 * A <code>JPopupMenu</code> that allows to duplicate or close the 
 * currently visible tab.
 * 
 * @author Michaela Regneri
 *
 */
public class JDomTabPopup extends JPopupMenu {

	/**
	 * Sets up the menu and ads the listener
	 * 
	 * @param listener the listener for the menu items
	 */
	public JDomTabPopup(CommandListener listener) {
		super();
		
		// item to duplicate the graph
		JMenuItem duplicate = new JMenuItem("Duplicate");
		duplicate.setMnemonic(KeyEvent.VK_D);
		duplicate.setAccelerator(KeyStroke.getKeyStroke("alt D"));
		duplicate.setActionCommand("dup");
		duplicate.addActionListener(listener);
		add(duplicate);
		
		// item to close the graph
		JMenuItem close = new JMenuItem("Close");
		close.setAccelerator(KeyStroke.getKeyStroke("ctrl W"));
		close.setActionCommand("shut");
		close.addActionListener(listener);
		add(close);
	}
	
	
}
