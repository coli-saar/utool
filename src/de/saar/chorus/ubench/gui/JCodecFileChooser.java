package de.saar.chorus.ubench.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;

import de.saar.basic.GenericFileFilter;
import de.saar.chorus.domgraph.codec.CodecManager;

public class JCodecFileChooser extends JFileChooser
			implements PropertyChangeListener, ActionListener {

	JPanel empty, button;
	boolean input, optview;
	CodecManager manager;
	JCodecOptionPane options;
	JButton showOptions;
	

	public JCodecFileChooser(String path, boolean input) {
		super(path);
		this.input = input;
		addPropertyChangeListener(this);
		manager = Ubench.getInstance().getCodecManager();
		empty = new JPanel();

		empty.add(new JLabel("No Codec" +
				System.getProperty("line.separator")
				+ " selected."));
		
		showOptions = new JButton("Options");
		showOptions.setActionCommand("showOptions");
		showOptions.setEnabled(false);
		showOptions.addActionListener(this);
		optview = false;
		button = new JPanel();
		button.add(showOptions, BorderLayout.CENTER);
		setAccessory(button);
		//setAccessory(empty);
	}
	
	
	
	
	public Map<String,String> getCodecOptions() {
		if( options != null) {
			return options.getOptionMap();
		} else return new HashMap<String,String>();
	}
	
	public void propertyChange(PropertyChangeEvent evt) {
		String prop = evt.getPropertyName();
		String codecname = null;
		JComponent newAcc = new JPanel();
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
				options = new JCodecOptionPane(manager.getInputCodecOptionTypes(codecname));
				
				newAcc = options;
			} else {
				options = new JCodecOptionPane(manager.getOutputCodecOptionTypes(codecname));
				newAcc = options;
			}
			if(showOptions != null ) {
				showOptions.setEnabled(true);
			}
			if(optview) {
				showOptionAccess(newAcc);
			}
		} 
		
		validate();
		codecname = null;
	}
	
	private void setShowAccessory(boolean show) {
		optview = show;
		if(show)  {
			showOptionAccess(options);
			
		} else {
	
			setAccessory(button);
		}
		validate();
	}
	
	private void showOptionAccess(JComponent optionpane) {
		JPanel helperPanel = new JPanel();
		BoxLayout layout = new BoxLayout(helperPanel, BoxLayout.PAGE_AXIS);
	
		helperPanel.setLayout(layout);
		
		helperPanel.add(optionpane);
		
	
		helperPanel.add(new JLabel("     "));
		JButton hide = new JButton("Hide");
		hide.addActionListener(this);
		hide.setActionCommand("hide");
		helperPanel.add(hide);
		JPanel helper2 = new JPanel();
		helper2.add(helperPanel);
		setAccessory(helper2);
		validate();
	}

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equals("showOptions")) {
			setShowAccessory(true);
		} else if(e.getActionCommand().equals("hide")) {
			setShowAccessory(false);
		}
		
	}
}


