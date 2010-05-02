package de.saar.swing;

import javax.swing.SwingUtilities;

abstract public class ResumingSwingThread<E> extends Thread {
	abstract public E executeAsynchronously();
	abstract public void thenWhat(E result);
	
	@Override
	public void run() {
		final E result = executeAsynchronously();
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				thenWhat(result);
			}
		});
	}
}
