package de.saar.basic;

import java.awt.Component;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

public class GUIUtilities {

	public static int confirmFileOverwriting(JFileChooser fc, Component parent) {
		
		int fcVal = fc.showSaveDialog(parent);
		
		while(fcVal == JFileChooser.APPROVE_OPTION) {
			if(fc.getSelectedFile().exists()) {
				int ync = 
					JOptionPane.showConfirmDialog(parent, "The file " + 
							fc.getSelectedFile().getName() +  " already exists." +
									System.getProperty("line.separator") + 
									"Do you want to overwrite it?", 
									"Overwrite existing file", JOptionPane.YES_NO_CANCEL_OPTION);
				
				switch(ync) {
				case JOptionPane.YES_OPTION : return fcVal;
				case JOptionPane.CANCEL_OPTION : return JFileChooser.CANCEL_OPTION;
				case JOptionPane.NO_OPTION : {
					fc.setVisible(true);
					fcVal = fc.showSaveDialog(parent);
				}  				}
			} else {
				break;
			}
		}
		
		return fcVal;
		
	}
}
