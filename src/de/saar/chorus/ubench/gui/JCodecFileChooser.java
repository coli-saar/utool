package de.saar.chorus.ubench.gui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;

import de.saar.basic.GenericFileFilter;
import de.saar.chorus.domgraph.codec.CodecManager;

public class JCodecFileChooser extends JFileChooser
			implements PropertyChangeListener {

	JPanel empty;
	boolean input;
	CodecManager manager;
	
	public JCodecFileChooser(boolean input) {
		super();
		this.input = input;
		addPropertyChangeListener(this);
		manager = Ubench.getInstance().getCodecManager();
		empty = new JPanel();
		empty.setLayout(new BoxLayout(empty, BoxLayout.PAGE_AXIS));
		empty.add(new JLabel("No Codec"));
		empty.add(new JLabel("Selected."));
		setAccessory(empty);
	}
	
	public JCodecFileChooser(String path, boolean input) {
		super(path);
		this.input = input;
		addPropertyChangeListener(this);
		manager = Ubench.getInstance().getCodecManager();
		empty = new JPanel();
		empty.setLayout(new BoxLayout(empty, BoxLayout.PAGE_AXIS));
		empty.add(new JLabel("No Codec"));
		empty.add(new JLabel("Selected."));
		setAccessory(empty);
	}
	
	public void propertyChange(PropertyChangeEvent evt) {
		String prop = evt.getPropertyName();
		String codecname = null;
		JPanel newAcc = new JPanel();
		if (JFileChooser.SELECTED_FILE_CHANGED_PROPERTY.equals(prop)) {
			File file = (File) evt.getNewValue();
			
			if(file != null) {
				if(input) {
					codecname = manager.getInputCodecNameForFilename(file.getName());
				} else {
					codecname = manager.getOutputCodecNameForFilename(file.getName());
				}
			}
		} else if(JFileChooser.FILE_FILTER_CHANGED_PROPERTY.equals(prop)) {
			FileFilter filter = getFileFilter();
			try {
				codecname = ((GenericFileFilter) filter).getName(); 
			} catch(ClassCastException e) {
				System.err.println("All Codecs Selected.");
			}
		} else {
			return;
		}
		
		if(codecname != null) {
		
			if(input) {
				newAcc = new JCodecOptionPane(manager.getInputCodecOptionTypes(codecname));
			} else {
				newAcc = new JCodecOptionPane(manager.getOutputCodecOptionTypes(codecname));
			}
		} 
		setAccessory(newAcc);
		validate();
		codecname = null;
	}
}


