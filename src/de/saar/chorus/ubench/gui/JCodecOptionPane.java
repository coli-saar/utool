package de.saar.chorus.ubench.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import de.saar.chorus.domgraph.codec.CodecManager;

public class JCodecOptionPane {

	private static Map<String, JTextField> texttypes =
		new HashMap<String, JTextField>();
	
	private static Map<String, JCheckBox> booleans = 
		new HashMap<String, JCheckBox>();
	
	private static Map<String, JComboBox> enums =
		new HashMap<String,JComboBox>();
	
	private static CodecManager codecmanager =
		Ubench.getInstance().getCodecManager();
	
	public static JPanel constructInputCodecPanel(String codec) {
		
		return constructOptionPanel(
				codecmanager.getInputCodecOptionTypes(codec));
	}
	
	public static JPanel constructOutputCodecPanel(String codec) {
		
		return constructOptionPanel(codecmanager.
				getOutputCodecOptionTypes(codec));
	}
	
	private static JPanel constructOptionPanel(Map<String,Class> optionTypes) {
		JPanel optpan = new JPanel();
		
		optpan.setLayout(new BoxLayout(optpan, BoxLayout.PAGE_AXIS));
		
		// to make sure that the panel always looks
		// the same way
		List<String> optionnames = new ArrayList<String>(optionTypes.keySet());
		Collections.sort(optionnames);
		
		for( String opt : optionnames ) {
			Class optclass = optionTypes.get(opt);
					
			if( optclass == Boolean.TYPE ) {
				
				JCheckBox box = new JCheckBox(opt);
				optpan.add(box);
				booleans.put(opt, box);
			} else if( optclass.isEnum() ) {
				//optpan.add(new JPanel();
				//optview.setLayout(new BoxLayout(optview, BoxLayout.PAGE_AXIS));	
				optpan.add(new JLabel(opt + ":"));
				Object[] constants = optclass.getEnumConstants();
				Vector<String> stringvals = new Vector<String>(constants.length);
				for( Object cos : constants )  {
					stringvals.add(cos.toString());
				}
				JComboBox box = new JComboBox(stringvals);
				optpan.add(box);
				enums.put(opt,box);
			} else {
				//optview = new JPanel();
				//optview.setLayout(new BoxLayout(optview, BoxLayout.PAGE_AXIS));	
				optpan.add(new JLabel(opt + ":"));
				JTextField line = new JTextField();
				optpan.add(line);
				texttypes.put(opt,line);
			}
		}
		if(optionnames.isEmpty()) {
			optpan.add(new JLabel("This Codec"));
			optpan.add(new JLabel("has no"));
			optpan.add(new JLabel("options"));
			optpan.add(new JLabel("to set."));
		}
		return optpan;
	}
	
	public static String getOptionString() {
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
		try{
			codecmanager.registerCodec(DummyCodec.class);
		} catch(Exception e) {
			e.printStackTrace();
			return;
		}
	//	JFrame window = new JFrame("Debug Codec Option Panel");
		//window.setLayout(new BoxLayout(window, BoxLayout.PAGE_AXIS));
		JFrame window = new JFrame("dummy-codec" + "(out)");
		window.add(constructOutputCodecPanel("dummy-codec"));
		JButton ok = new JButton("print command");
		ok.addActionListener(new DebugListener());
		ok.setActionCommand("tr");
		window.add(ok, BorderLayout.SOUTH);
		window.pack();
		window.validate();
		window.setVisible(true);
		
	}

	private static class DebugListener implements ActionListener {
	
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
