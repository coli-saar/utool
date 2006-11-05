package de.saar.chorus.ubench.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;

public class JCodecOptionPane extends JComponent {

	
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
	
	public JCodecOptionPane(Map<String,Class> options) {
		optionTypes = options;

		layout = new GridBagLayout();
		setLayout(layout);
		left = new GridBagConstraints();
		left.anchor = GridBagConstraints.LAST_LINE_START;
		left.weightx = 0;
		left.weighty = 0;
		left.gridx = 0;
		left.fill = GridBagConstraints.HORIZONTAL;
		
		right = new GridBagConstraints();
		right.anchor = GridBagConstraints.LINE_END;
		right.weightx = 1;
		right.weighty = 0;
		right.gridx = 1;
		right.fill = GridBagConstraints.HORIZONTAL;
		
		gridy = 0;
		constructOptionPanel();
	}
	
	

	
	public void setDefault(String parameter, String value) {
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



	private void constructOptionPanel() {
		
		
		// to make sure that the panel always looks
		// the same way
		List<String> optionnames = new ArrayList<String>(optionTypes.keySet());
		Collections.sort(optionnames);
		
		for( String opt : optionnames ) {
			Class optclass = optionTypes.get(opt);
			left.gridy = gridy;
			right.gridy = gridy;
			if( optclass == Boolean.TYPE ) {
				
				JCheckBox box = new JCheckBox();
				JLabel label = new JLabel(opt);
				
				layout.setConstraints(label, left);
				add(label);
				layout.setConstraints(box, right);
				add(box);
				
				
				booleans.put(opt, box);
			} else if( optclass.isEnum() ) {
			
				//optpan.add(new JPanel();
				//optview.setLayout(new BoxLayout(optview, BoxLayout.PAGE_AXIS));	
				JLabel label = new JLabel(opt + ":");
				layout.setConstraints(label, left);
				add(label);
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
				
				//optview.setLayout(new BoxLayout(optview, BoxLayout.PAGE_AXIS));	
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
	
	public Map<String,String> getOptionMap() {
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
	
	public String getOptionString() {
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
