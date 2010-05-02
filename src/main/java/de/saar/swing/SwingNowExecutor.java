package de.saar.swing;

import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

public class SwingNowExecutor {
	public static void executeAndWait(Runnable r) {
		if( java.awt.EventQueue.isDispatchThread() ) {
			r.run();
		} else {
			try {
				SwingUtilities.invokeAndWait(r);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static void execute(Runnable r) {
		if( java.awt.EventQueue.isDispatchThread() ) {
			r.run();
		} else {
			SwingUtilities.invokeLater(r);
		}
	}
}
