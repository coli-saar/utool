package de.saar.chorus.ubench;

import javax.swing.JDialog;
import javax.swing.JOptionPane;


/**
 * This is a handler for all Exceptions which are thrown in threads besides the
 * main Event Dispatch Thread of Ubench.
 * As we use separate threads for every new graph we load and every "expensive" 
 * oparation on graphs (like exporting them, solving them etc.), we have to handle 
 * different types of exceptions here and show the user a meaningful error message. 
 * 
 * 
 * @author Michaela Regneri
 * @see Thread.UncaughtExceptionHandler
 *
 */
public class DomGraphUnhandledExceptionHandler implements Thread.UncaughtExceptionHandler {

	
	
	/**
	 * 
	 */
	public void uncaughtException(Thread arg0, Throwable arg1) {
		
	
		
		 JOptionPane pane = 
				new JOptionPane(arg1.getMessage(), JOptionPane.ERROR_MESSAGE);
			JDialog dialog = 
				pane.createDialog(Ubench.getInstance().getWindow(), "Error");
			dialog.setModal(false);
			dialog.setVisible(true);
			Ubench.getInstance().refresh();
		
	}

}
