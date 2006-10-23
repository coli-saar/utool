package de.saar.chorus.ubench.gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import de.saar.chorus.domgraph.codec.CodecManager;

public class JCodecOptionPane extends JPanel {

	private static Map<String, JTextField> texttypes =
		new HashMap<String, JTextField>();
	
	private static Map<String, JCheckBox> booleans = 
		new HashMap<String, JCheckBox>();
	
	private static Map<String, JComboBox> enums =
		new HashMap<String,JComboBox>();
	
	
	
	private Map<String, Class> optionTypes;
	
	public JCodecOptionPane(Map<String,Class> options) {
		optionTypes = options;
		constructOptionPanel();
	}
	

	
	private void constructOptionPanel() {
		setLayout(new GridLayout(0,2));
		
		// to make sure that the panel always looks
		// the same way
		List<String> optionnames = new ArrayList<String>(optionTypes.keySet());
		Collections.sort(optionnames);
		
		for( String opt : optionnames ) {
			Class optclass = optionTypes.get(opt);
					
			if( optclass == Boolean.TYPE ) {
				
				JCheckBox box = new JCheckBox();
				add(new JLabel(opt));
				add(box);
				
				
				booleans.put(opt, box);
			} else if( optclass.isEnum() ) {
			
				//optpan.add(new JPanel();
				//optview.setLayout(new BoxLayout(optview, BoxLayout.PAGE_AXIS));	
				add(new JLabel(opt + ":"));
				Object[] constants = optclass.getEnumConstants();
				Vector<String> stringvals = new Vector<String>(constants.length);
				for( Object cos : constants )  {
					stringvals.add(cos.toString());
				}
				JComboBox box = new JComboBox(stringvals);
				add(box);
				enums.put(opt,box);
			} else {
				
				//optview.setLayout(new BoxLayout(optview, BoxLayout.PAGE_AXIS));	
				add(new JLabel(opt + ":"));
				JTextField line = new JTextField();
				add(line);
				texttypes.put(opt,line);
			}
		}
		if(optionnames.isEmpty()) {
			add(new JLabel("This Codec"));
					add(new JLabel("has no"));
			add(new JLabel("options"));
			add(new JLabel("to set."));
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
	// this is to debug the Pane
	public static void main(String[] args) {
		Ubench.getInstance();
		CodecManager codecmanager = Ubench.getInstance().getCodecManager();
		System.err.println(codecmanager.getOutputCodecNameForFilename("rondane-1.mrs.pl"));
		try{
			codecmanager.registerCodec(DummyCodec.class);
		} catch(Exception e) {
			e.printStackTrace();
			return;
		}

		JFrame window = new JFrame("dummy-codec" + "(out)");
		JCodecOptionPane dummypane = new JCodecOptionPane(codecmanager.getOutputCodecOptionTypes("dummy-codec"));
		window.add(dummypane);
		JButton ok = new JButton("print command");
		ok.addActionListener(dummypane.new DebugListener());
		ok.setActionCommand("tr");
		window.add(ok, BorderLayout.SOUTH);
		window.pack();
		window.validate();
		window.setVisible(true);
		
	}

	private class DebugListener implements ActionListener {
	
	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		if( e.getActionCommand().equals("tr")) {
			System.err.println(getOptionString());
		}
	}
	
	}
}
