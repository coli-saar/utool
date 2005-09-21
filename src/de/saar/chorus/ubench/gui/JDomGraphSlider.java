package de.saar.chorus.ubench.gui;

import java.awt.event.KeyEvent;

import javax.swing.JSlider;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * An implementation of a <code>JSlider</code> to scale the 
 * visible <code>JDomGraph</code>.
 * 
 * @author Michaela Regneri
 *
 */
public class JDomGraphSlider extends JSlider implements ChangeListener {

	public JDomGraphSlider() {
		
		//the user is supposed not to enlarge
		//the graph, so the scale ends at 100%
		super(JSlider.VERTICAL, 0, 100, 100);
		initialize();
	}
	
	private void initialize() {
		
		// the tooltip shows the recent scaling.
		setToolTipText("Zoom: " + getValue() + "%");
		
		/**
		 * TODO find out how to disable "scrolling"
		 * with up/down resp. left/right.
		 */
		resetKeyboardActions();
		
		addChangeListener(this);
	}

	/**
	 * Overwrites the <code>stateChanged</code> method of 
	 * <code>ChangeListener</code>.
	 * Scales the graph to the recenttly set value.
	 */
	public void stateChanged(ChangeEvent e) {
		int scale = getValue();
		setToolTipText("Zoom: " + scale + "%");
		Main.getVisibleTab().setGraphScale((double) scale/100);
	}
	
	
}
