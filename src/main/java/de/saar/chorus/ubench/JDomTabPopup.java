package de.saar.chorus.ubench;

import java.awt.Toolkit;
import java.awt.event.KeyEvent;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;

/**
 * A <code>JPopupMenu</code> that allows to duplicate or close the 
 * currently visible tab.
 * 
 * TODO this might as well be a private class of JDomTabbedPane
 * 
 * @author Michaela Regneri
 *
 */
class JDomTabPopup extends JPopupMenu {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3971251434089241635L;

	/**
	 * Sets up the menu and ads the listener
	 * 
	 * @param listener the listener for the menu items
	 */
	JDomTabPopup(CommandListener listener) {
		super();
		
		JMenuItem chartView = new JMenuItem("Display Chart");
		chartView.setMnemonic(KeyEvent.VK_C);
		chartView.setAccelerator(KeyStroke.getKeyStroke("alt C"));
		chartView.setActionCommand("chartView");
		chartView.addActionListener(listener);
		add(chartView);
		
		addSeparator();
		
		// item to duplicate the graph
		JMenuItem duplicate = new JMenuItem("Duplicate");
		duplicate.setMnemonic(KeyEvent.VK_D);
		duplicate.setAccelerator(KeyStroke.getKeyStroke("alt D"));
		duplicate.setActionCommand("dup");
		duplicate.addActionListener(listener);
		add(duplicate);
		
		// item to close the graph
		
		JMenuItem close = new JMenuItem("Close");
		close.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W,Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		close.setActionCommand("shut");
		close.addActionListener(listener);
		add(close);
	}
	
	
}
