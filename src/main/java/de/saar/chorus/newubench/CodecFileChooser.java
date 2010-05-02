package de.saar.chorus.newubench;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;

import de.saar.basic.GenericFileFilter;
import de.saar.chorus.domgraph.codec.CodecManager;

/**
 * A class representing a <code>JFileChooser</code> which can
 * display and control options for Input and Output codecs used
 * in Utool.
 * 
 * This class is responsible for displaying the right kind
 * of dialog and options according to the type of codec (either
 * input or output) and to do display the correct options when
 * the codec selection has changed.
 * 
 * 
 * @author Alexander Koller
 * @author Michaela Regneri
 *
 */
class CodecFileChooser extends JFileChooser
			implements PropertyChangeListener, ActionListener {
	private JPanel empty,	// if there is no codec selected
				   button;  // the panel shown when options are hidden
	
	private boolean input, 				// input codec or not?
					optview;  			// options visible or not?
	
	private CodecManager manager; 		// the active codec manager
	
	private JCodecOptionPane options; 	// the current options panel
	
	private JButton showOptions;		// the button to access the option panel
	
	// a file filter accepting all known codec extensions
	private SeveralExtensionsFilter allKnownTypesFileFilter;
	
	// the default selected file filter
	private FileFilter defaultFileFilter;
	
	// The name of the currently selected codec. It starts out as "null" (for no
	// selection) and is updated every time the user chooses a specific codec from
	// the dropdown menu or selects a file whose extension is associated with a codec.
	private String currentCodec;
	
	/**
	 * The enum class representing the
	 * three diffent file chooser types for the
	 * three different tasks.
	 * 
	 * @author Alexander Koller
	 *
	 */
	static enum Type {
		OPEN                 ("Open USR", true),
		EXPORT               ("Export USR", false),
		EXPORT_SOLVED_FORMS  ("Export solved forms", false);
		
		public String dialogTitle;
		public boolean isInput;

		private Type(String dialogTitle, boolean isInput) {
			this.dialogTitle = dialogTitle;
			this.isInput = isInput;
		}
	}
	

	/**
	 * A new <code>JCodecFileChooser</code> initialised
	 * with the folder to display and its task type.
	 * 
	 * @param path the path for the file view
	 * @param type the task type 
	 */
	CodecFileChooser(String path, Type type) {
		super(path);
		
		setDialogTitle(type.dialogTitle);
		input = type.isInput;
		
		allKnownTypesFileFilter = new SeveralExtensionsFilter();
	
		
		if( input ) {
			setAcceptAllFileFilterUsed(true);
			addChoosableFileFilter(allKnownTypesFileFilter);
			defaultFileFilter = allKnownTypesFileFilter;
		} else {
			setAcceptAllFileFilterUsed(false);
			defaultFileFilter = null;
		}

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
		
		currentCodec = null;
	}
	
	/**
	 * Add all the file filters for codecs this 
	 * file chooser shall accept.
	 * 
	 * @param filters the <code>List</code> of file filters
	 */
	void addCodecFileFilters(List<GenericFileFilter> filters) {
		for( GenericFileFilter ff : filters ) {
			addChoosableFileFilter(ff);
			allKnownTypesFileFilter.addExtension(ff.getExtension());
			
			if( !input && (defaultFileFilter == null)) {
				defaultFileFilter = ff;
				enableOptions(ff.getName());
			}
		}

		if( defaultFileFilter != null ) {
			setFileFilter(defaultFileFilter);
		}
	}


	/**
	 * Returns the selected values of the codec
	 * options in string representation. The map returned 
	 * can be forwarded to the <code>CodecManager</code>
	 * to construct a codec initialised with these options.
	 * 
	 * @return the user selection of options
	 */
	Map<String,String> getCodecOptions() {
		if( options != null) {
			return options.getOptionMap();
		} else return new HashMap<String,String>();
	}

	/**
	 * This is responsible for showing the options 
	 * belonging to the selected codec and for listening
	 * on codec selection changes.
	 */
	public void propertyChange(PropertyChangeEvent evt) {
		String prop = evt.getPropertyName();
		String codecname = null;

		if( isShowing() ) {  // ignore all events that occur before the dialog is displayed
			if (JFileChooser.SELECTED_FILE_CHANGED_PROPERTY.equals(prop)) {
				// If the user selected a file which is associated to a codec, switch
				// the option display to that codec.
				File file = (File) evt.getNewValue();
				
				if( file != null ) {
					if( input ) {
						codecname = manager.getInputCodecNameForFilename(file.getName());
					} else {
						codecname = manager.getOutputCodecNameForFilename(file.getName());
					}
				}

			} else if(JFileChooser.FILE_FILTER_CHANGED_PROPERTY.equals(prop)) {
				// If the user selected a single codec in the dropdown menu, switch
				// to that codec.
				FileFilter filter = getFileFilter();

				if(filter instanceof GenericFileFilter) {
					codecname = ((GenericFileFilter) filter).getName(); 
				}
			}
			
			if( codecname != null ) {
				// Switch the option pane to that codec and enable the options button.
				enableOptions(codecname);
			}
		}
	}
	
	/**
	 * This constructs and shows an JCodecOptionPane of the 
	 * a certain codec.
	 * 
	 * @param codecname the name of the codec
	 */
	private void enableOptions(String codecname) {
		showOptions.setEnabled(true);
		currentCodec = codecname;
		options = new JCodecOptionPane(
				input ? manager.getInputCodecOptionTypes(codecname) 
					  : manager.getOutputCodecOptionTypes(codecname));
		if( input ) {
			for( String parameter : manager.getInputCodecOptionTypes(codecname).keySet() ) {
				options.setDefault(parameter, 
						manager.getInputCodecParameterDefaultValue(codecname, parameter));
			}
		} else {
			for( String parameter : manager.getOutputCodecOptionTypes(codecname).keySet() ) {
				options.setDefault(parameter, 
						manager.getOutputCodecParameterDefaultValue(codecname, parameter));
			}
		}
		
		if( optview ) {
			showOptionAccess(options);
		}
		
		validate();
	}

	/**
	 * Switches between option view and hidden option
	 * view (with the button giving access to the options.)
	 * 
	 * @param show if true, options are shown
	 */
	private void setShowAccessory(boolean show) {
		optview = show;
		if(show)  {
			showOptionAccess(options);
			
		} else {
	
			setAccessory(button);
		}
		validate();
	}

 	
	/**
	 * Displays a <code>JComponent</code> as accessory
	 * of this file chooser.
	 * 
	 * @param optionpane
	 */
	private void showOptionAccess(JComponent optionpane) {
		JPanel helperPanel = new JPanel();
		BoxLayout layout = new BoxLayout(helperPanel, BoxLayout.PAGE_AXIS);
		
		helperPanel.setLayout(layout);
		//helperPanel.add(new JLabel("Codec: " + ((GenericFileFilter) getFileFilter()).getName()));
		//helperPanel.add(new JLabel(" "));

		String  codecname = currentCodec;
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
		JScrollPane optionscrollpane = new JScrollPane(optionpane);
		optionscrollpane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		optionscrollpane.setBorder(new EmptyBorder(0,0,0,0));
		helperPanel.add(optionscrollpane);
		
		// doesn't work...
		
	
		helperPanel.add(new JLabel("     "));
		JButton hide = new JButton("Hide");
		hide.addActionListener(this);
		hide.setActionCommand("hide");
		helperPanel.add(hide);
		 
		setAccessory(helperPanel);
	
		validate();
		
		Dimension dim = new Dimension(
				Math.max(getTextLabelWidth(title), 
						optionscrollpane.getPreferredSize().width),
						optionscrollpane.getPreferredSize().height);
		optionpane.setMinimumSize(dim);
		optionpane.setPreferredSize(dim);
	
		helperPanel.setPreferredSize( new Dimension(
				dim.width + (int) optionscrollpane.getVerticalScrollBar().getWidth(),
				dim.height) );
		optionpane.revalidate();
		optionscrollpane.revalidate();
		revalidate();
	}

	/**
	 * This reactc when either the "Options" or the "Hide" button
	 * is pressed.
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
	 * An estimate of the label with of the codec.
	 * 
	 * @param text
	 * @return
	 */
	private int getTextLabelWidth(String text) {
		JLabel ruler = new JLabel(text);
		return ruler.getMaximumSize().width;
	}
	
	/**
	 * A <code>FileFilter</code> designed to 
	 * accept a succesively added collection of
	 * extensions.
	 * 
	 *
	 */
	private static class SeveralExtensionsFilter extends FileFilter {
		
		Set<String> extensions;
		
		/**
		 *  Empty constructor (just for initialising).
		 *
		 */
		SeveralExtensionsFilter() {
			extensions = new HashSet<String>();
		}
		
		/**
		 * Initialise the Filter with a list of 
		 * extensions to accept.
		 * Please make sure to have a Collection of
		 * extension strings starting with "." !
		 * 
		 * @param ext
		 */
		SeveralExtensionsFilter(Collection<String> ext) {
			extensions = new HashSet<String>(ext);
		}
		
		/**
		 * Add a file extension that shall be accepted
		 * by the filter
		 * 
		 * @param extension the new extension
		 */
		public void addExtension(String extension) {
			if( extension.startsWith(".") ) {
				extensions.add(extension);
			} else {
				extensions.add("."+ extension);
			}
		}
		
		/**
		 * 
		 * @return true if the file has an extension
		 *        contained here or is a folder
		 */
		public boolean accept(File f) {
			
			String fileName = f.getName();
			
			if( f.isDirectory() ) {
				return true;
			} 
			
			for(String extension : extensions ) {
				if(fileName.endsWith(extension) ) {
					return true;
				}
				
			}
			return false;
		}
		
		/**
		 * 
		 */
		public String getDescription() {
			return "All known file types";
		}
		
	}
	
	
	private static final long serialVersionUID = 2420583972471990944L;


	
	private static 
	/**
	 * This represents a UI for selecting or entering values of different
	 * types (according to possible codec options).
	 * Enums are displayed as drop-down menus, boolean values
	 * as checkboxes and everything else as text field.
	 * Default Values can be set. 
	 * This also provides a method to return the values selected is provided.
	 * 
	 * @author Michaela Regneri
	 * @see de.saar.chorus.domgraph.codec.CodecConstructor
	 *
	 */
	class JCodecOptionPane extends JComponent {

		
		private static final long serialVersionUID = 5201435318585726488L;

		private static Map<String, JTextField> texttypes =
			new HashMap<String, JTextField>();
		
		private static Map<String, JCheckBox> booleans = 
			new HashMap<String, JCheckBox>();
		
		private static Map<String, JComboBox> enums =
			new HashMap<String,JComboBox>();
		
		private GridBagLayout layout;
		private GridBagConstraints left, right;
		
		private Map<String, Class> optionTypes;
		int gridy;
		
		/**
		 * A new <code>JCodecOptionPane</code> initalised
		 * with some parameters given in a Map with the
		 * parameter names and their types as classes.
		 * 
		 * @param options
		 */
		JCodecOptionPane(Map<String,Class> options) {
			optionTypes = options;

			layout = new GridBagLayout();
			setLayout(layout);
			
			left = new GridBagConstraints();
			left.anchor = GridBagConstraints.LAST_LINE_START;
			left.weightx = 0;
			left.weighty = 0;
			left.gridx = 0;
			left.fill = GridBagConstraints.HORIZONTAL;
			left.insets = new Insets(0,0,0,5);
			
			right = new GridBagConstraints();
			right.anchor = GridBagConstraints.LINE_END;
			right.weightx = 1;
			right.weighty = 0;
			right.gridx = 1;
			right.fill = GridBagConstraints.HORIZONTAL;
			right.insets = new Insets(0,5,0,0);
			gridy = 0;
			constructOptionPanel();
		}
		
		
		/**
		 * Sets the default value of a certain parameter.
		 * 
		 * @param parameter the parameter name
		 * @param value the default value as string
		 */
		void setDefault(String parameter, String value) {
			if(value != null) {
				if( booleans.containsKey(parameter) ) {
					booleans.get(parameter).setSelected(
							Boolean.parseBoolean(value));
				} else if( enums.containsKey(parameter)) {
					enums.get(parameter).setSelectedItem(value);
				} else if ( texttypes.containsKey(parameter) ) {
					texttypes.get(parameter).setText(value);
				}
			}
		}


		/**
		 * This constructs the panel by iterating over
		 * the parameters and their classes and assinging each
		 * parameter an appropriate SWING component.
		 * The left side in the grid always consists of a 
		 * label containing the parameter name, the right side
		 * is filled with the according SWING component.
		 *
		 */
		private void constructOptionPanel() {
			
			
			// to make sure that the panel always looks
			// the same way
			List<String> optionnames = new ArrayList<String>(optionTypes.keySet());
			Collections.sort(optionnames);
			
			// for each parameter...
			for( String opt : optionnames ) {
		
				// retrieve its type
				Class optclass = optionTypes.get(opt);
				left.gridy = gridy;
				right.gridy = gridy;
				if( optclass == Boolean.TYPE ) {
					// represent a boolean as checkbox 
					JCheckBox box = new JCheckBox();
					JLabel label = new JLabel(opt);
					
					layout.setConstraints(label, left);
					add(label);
					layout.setConstraints(box, right);
					add(box);
					
					
					booleans.put(opt, box);
				} else if( optclass.isEnum() ) {
					
					JLabel label = new JLabel(opt + ":");
					layout.setConstraints(label, left);
					add(label);
					
					// fill all the enum constans of a enum
					// type in a drop down menu
					Object[] constants = optclass.getEnumConstants();
					Vector<String> stringvals = new Vector<String>(constants.length);
					for( Object cos : constants )  {
						stringvals.add(cos.toString());
					}
					JComboBox box = new JComboBox(stringvals);
					layout.setConstraints(box, right);
					add(box);
					enums.put(opt,box);
				} else {
					
					// assing everything else a text field
					JLabel label = new JLabel(opt + ":");
					layout.setConstraints(label, left);
					add(label);
					JTextField line = new JTextField();
					layout.setConstraints(line,right);
					add(line);
					texttypes.put(opt,line);
				}
				gridy++;
			}
			if(optionnames.isEmpty()) {
				
				// construct a field indicating that there
				// are no options
				GridBagConstraints empty = new GridBagConstraints();
				empty.anchor = GridBagConstraints.CENTER;
				empty.fill = GridBagConstraints.VERTICAL;
				empty.gridy = 0;
				
				JLabel firstline = new JLabel("This Codec has no");
				layout.setConstraints(firstline, empty);
				add(firstline);
				
				empty.gridy = 1;
				JLabel secondline = new JLabel("options to set.");
				layout.setConstraints(secondline, empty);
				
				add(firstline);
				add(secondline);
				
			} 
			
			
			doLayout();
			validate();
		}
		
		/**
		 * Returns the user selected options in 
		 * String representation.
		 * 
		 * @return a Map containg the parameter names and their values as string
		 */
		Map<String,String> getOptionMap() {
			Map<String,String> ret = new HashMap<String,String>();
			
			for(String opt : booleans.keySet() ) {
				ret.put(opt, Boolean.toString(booleans.get(opt).isSelected()));
			}
			
			for(String opt : enums.keySet() ) {
				ret.put(opt, enums.get(opt).getSelectedItem().toString());
			}
			
			for(String opt : texttypes.keySet()) {
				ret.put(opt,texttypes.get(opt).getText());
			}
			
			return ret;
		}
		
		/**
		 * Constructs a command line string out of 
		 * the parameters and their values.
		 * 
		 * @return
		 */
		String getOptionString() {
			StringBuffer ret = new StringBuffer();
			boolean first = true;
			
			for(String opt : booleans.keySet() ) {
				if(first) {
					first = false;
				} else {
					ret.append(", ");
				}
				ret.append(opt + ":" + booleans.get(opt).isSelected());
			}
			
			for(String opt : enums.keySet() ) {
				if(first) {
					first = false;
				} else {
					ret.append(", ");
				}
				ret.append(opt + ":" + enums.get(opt).getSelectedItem());
			}
			
			for(String opt : texttypes.keySet() ) {
				if(first) {
					first = false;
				} else {
					ret.append(", ");
				}
				ret.append(opt + ":" + texttypes.get(opt).getText());
			}
			
			return ret.toString();
		}
	}

}


