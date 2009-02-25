package de.saar.swing;

import javax.swing.SwingUtilities;

abstract public class ResumingSwingThread extends Thread {
	abstract public void executeAsynchronously();
	abstract public void thenWhat();
	
	@Override
	public void run() {
		executeAsynchronously();
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				thenWhat();
			}
		});
	}
}
