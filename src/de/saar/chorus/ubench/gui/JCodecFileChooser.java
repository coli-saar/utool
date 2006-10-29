package de.saar.chorus.ubench.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
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
			
			if(filter instanceof GenericFileFilter) {
				codecname = ((GenericFileFilter) filter).getName(); 
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
		}  else {
			showOptions.setEnabled(false);
			if(optview) {
				showOptionAccess(empty);
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
		//helperPanel.add(new JLabel("Codec: " + ((GenericFileFilter) getFileFilter()).getName()));
		//helperPanel.add(new JLabel(" "));

		String  codecname = ((GenericFileFilter) getFileFilter()).getName();
		String title;
		if(codecname == null) {
			title = "Options";
		} else {
			title = "Options: " + codecname;
		}
		
		optionpane.setBorder(new TitledBorder(
				new LineBorder(Color.GRAY, 1, true), 
				title,
				TitledBorder.CENTER,
				TitledBorder.ABOVE_TOP));
		helperPanel.add(optionpane);
		
		// doesn't work...
		optionpane.setMinimumSize(new Dimension(
				getTextLabelWidth(title), 
				optionpane.getMinimumSize().height)); 
	
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
	
	/**
	 * TODO ganz boeser hack. mach das anders.
	 * 
	 * @param text
	 * @return
	 */
	private int getTextLabelWidth(String text) {
		JLabel ruler = new JLabel(text);
		return ruler.getMaximumSize().width;
	}
}


