package de.saar.chorus.ubench.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import de.saar.chorus.ubench.ServerOptions;

public class JDomGraphPreferencePane extends JFrame
				implements ActionListener {
	
	JTabbedPane tabs;
	JPanel servertab;
	JCheckBox warmup, logging;
	JRadioButton port2802, systemerrout, ownport, ownlog;
	JTextField port, logfile;
	JButton ok, apply, cancel, browse;
	String logfilepath;
	
	public JDomGraphPreferencePane() {
		super("Settings");
		
		tabs = new JTabbedPane();
		servertab = new JPanel();
		servertab.setLayout(new GridLayout(0,1));
		
		JPanel checkboxes = new JPanel();
		checkboxes.setLayout(new GridLayout(1,2));
		warmup = new JCheckBox("Start with warm-up");
		logging = new JCheckBox("Print logging messages");
		checkboxes.add(warmup);
		checkboxes.add(logging);
		checkboxes.setBorder(
				new TitledBorder(new LineBorder(Color.GRAY, 1, true), 
				"General",
				TitledBorder.LEADING,
				TitledBorder.ABOVE_TOP));
		servertab.add(checkboxes);
		
		JPanel portsettings = new JPanel();
		portsettings.setLayout(new GridLayout(1,2));
		
		ButtonGroup portgroup = new ButtonGroup();
		port2802 = new JRadioButton("2802 (default)");
		portgroup.add(port2802);
		portsettings.add(port2802);
		
		JPanel portpanel = new JPanel();
		portpanel.setLayout(new BoxLayout(portpanel, BoxLayout.X_AXIS));
		ownport = new JRadioButton("Use Port: ");
		portgroup.add(ownport);
		port = new JTextField(4);
		portpanel.add(ownport);
		JPanel help1 = new JPanel();
		portpanel.add(port);
		portsettings.add(portpanel);
		
		portsettings.setBorder(
				new TitledBorder(new LineBorder(Color.GRAY, 1, true), 
						"Server Port",
						TitledBorder.LEADING,
						TitledBorder.ABOVE_TOP));
		
		servertab.add(portsettings);
		
		JPanel log = new JPanel();
		log.setLayout(new BoxLayout(log, 
				BoxLayout.LINE_AXIS));
		ButtonGroup loggroup = new ButtonGroup();
		systemerrout = new JRadioButton("System.out");
		log.add(systemerrout);
		loggroup.add(systemerrout);
		JPanel logfilepanel = new JPanel();
		logfilepanel.setLayout(new BoxLayout(logfilepanel, 
				BoxLayout.LINE_AXIS));
		ownlog = new JRadioButton("Use logfile: ");
		loggroup.add(ownlog);
		logfilepanel.add(ownlog);
		
		JPanel help = new JPanel();
		help.setLayout(new BoxLayout(help, BoxLayout.LINE_AXIS));
		logfile = new JTextField(10);
		help.add(logfile);
		browse = new JButton("Browse...");
		browse.setMargin(new Insets(1,1,1,1));
		browse.setActionCommand("browse");
		browse.addActionListener(this);
		
		help.add(browse);
		
		logfilepanel.add(help);
		log.add(logfilepanel);
		
		log.setBorder(new TitledBorder(new LineBorder(Color.GRAY, 1, true), 
				"Logging Output",
				TitledBorder.LEADING,
				TitledBorder.ABOVE_TOP));
		servertab.add(log);
		
		tabs.add(servertab, "Server");
		add(tabs, BorderLayout.CENTER);
		
		ok = new JButton("OK");
		ok.setActionCommand("ok");
		ok.addActionListener(this);
		
		apply = new JButton("Apply");
		apply.setActionCommand("apply");
		apply.addActionListener(this);
		
		cancel = new JButton("Cancel");
		cancel.setActionCommand("cancel");
		cancel.addActionListener(this);
		
		JPanel bottom = new JPanel();
		bottom.add(apply);
		bottom.add(ok);
		bottom.add(cancel);
		add(bottom, BorderLayout.SOUTH);
		
		initValues();
		
		pack();
		validate();
	}
	
	private void initValues() {
		if(ServerOptions.isWarmup()) {
			warmup.setSelected(true);
		} else {
			warmup.setSelected(false);
		}
		
		if(ServerOptions.isLogging()) {
			logging.setSelected(true);
		} else {
			logging.setSelected(false);
			ownlog.setSelected(false);
		}
		
		if(ServerOptions.getPort() == 2802) {
			port2802.setSelected(true);
		} else {
			port.setText(String.valueOf(ServerOptions.getPort()));
			ownport.setSelected(true);
		}
		
		systemerrout.setSelected(true);
		
	}
	
	public void applySettings() {
		ServerOptions.setWarmup(warmup.isSelected());
		ServerOptions.setLogging(logging.isSelected());
		if(port2802.isSelected()) {
			ServerOptions.setPort(2802);
		} else {
			ServerOptions.setPort(Integer.parseInt
					(port.getText()));
		}
		if(systemerrout.isSelected()) {
			ServerOptions.setLogwriter( 
					new PrintWriter(System.err, true));
		} else {
			if(logfilepath == null) {
				logfilepath = logfile.getText();
			}
			try {
				FileWriter writer = 
					new FileWriter(new File(logfilepath), true);
				ServerOptions.setLogwriter(new PrintWriter(writer));
				
			} catch (IOException e) {
				JOptionPane.showMessageDialog(Ubench.getInstance().getWindow(),
						e.getMessage(),
						"File Error",
						JOptionPane.ERROR_MESSAGE);
			}
		}
		
	}

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if(command.equals("ok")) {
			applySettings();
			setVisible(false);
		} else if(command.equals("apply")) {
			applySettings();
		} else if(command.equals("cancel")) {
			setVisible(false);
		} else if(command.equals("browse")) {
			JFileChooser fc = 
				new JFileChooser(System.getProperty("user.dir"));
			int ret = fc.showOpenDialog(this);
			if(ret == JFileChooser.APPROVE_OPTION ) {
				File lf = fc.getSelectedFile();
				logfilepath = lf.getAbsolutePath();
				logfile.setText(lf.getName());
			}
		
		}
		
	}
}
