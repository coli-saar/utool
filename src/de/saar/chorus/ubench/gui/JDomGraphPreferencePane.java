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

import de.saar.chorus.domgraph.utool.server.ConnectionManager;
import de.saar.chorus.ubench.ServerOptions;
import de.saar.chorus.ubench.gui.Preferences.LabelType;
import de.saar.chorus.ubench.gui.Preferences.LayoutType;


/**
 * A window to show and edit options for utool.
 * Up to now, there are only server options to set.
 * 
 * @author Michaela Regneri
 *
 */
public class JDomGraphPreferencePane extends JFrame
				implements ActionListener {
	
	
	private static final long serialVersionUID = 2688760547399503949L;
	
	
	JTabbedPane tabs;	// several tabs for different option typs
	JPanel servertab, layouttab;	// tab for server options
	
	// checkboxes for server warmup and server logging
	JCheckBox warmup, logging;
	
	
	JRadioButton port2802, systemerrout, ownport, ownlog, jdomgraph, chartlayout, sugiyama,
				showNames, showLabels, showBoth;
	JTextField port, logfile;
	JCheckBox allGraphs;
	
	JButton ok, apply, cancel, browse;
	String logfilepath;
	
	/**
	 * A new always-on-top window for showing und 
	 * editing the settings.
	 *
	 */
	public JDomGraphPreferencePane() {
		super("Settings");
		setAlwaysOnTop(true);
		tabs = new JTabbedPane();
		
		// tab for server settings
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
		
		
		
		// tab for layout settings
		layouttab = new JPanel(new GridLayout(0,1));
		
		
		// layout algorithm
		JPanel layoutstyle = new JPanel(new GridLayout(0,2));
		ButtonGroup layoutgroup = new ButtonGroup();
		 
		 
		 chartlayout = new JRadioButton("Chart Layout");
		 layoutgroup.add(chartlayout);
		 layoutstyle.add(chartlayout);
		 
		 jdomgraph = new JRadioButton("JDomGraph Layout");
		 layoutgroup.add(jdomgraph);
		 layoutstyle.add(jdomgraph);
		 
		 sugiyama = new JRadioButton("Sugiyama Layout");
		 layoutgroup.add(sugiyama);
		 layoutstyle.add(sugiyama);

		 layoutstyle.setBorder(new TitledBorder(new LineBorder(Color.GRAY, 1, true), 
				 "Standard Layout",
				 TitledBorder.LEADING,
				 TitledBorder.ABOVE_TOP));

		 layouttab.add(layoutstyle);
		 
		 
		 // labels
		 
		 JPanel labels = new JPanel(new GridLayout(0,2));
		 
		 ButtonGroup labelgroup = new ButtonGroup();
		 
		 showNames = new JRadioButton("Show node names");
		 labelgroup.add(showNames);
		 labels.add(showNames);
		 showLabels = new JRadioButton("Show node labels");
		 labelgroup.add(showLabels);
		 labels.add(showLabels);
		 showBoth = new JRadioButton("Show names and labels");
		 labelgroup.add(showBoth);
		 labels.add(showBoth);
		 

		 labels.setBorder(new TitledBorder(new LineBorder(Color.GRAY, 1, true), 
				 "Node Labeling",
				 TitledBorder.LEADING,
				 TitledBorder.ABOVE_TOP));

		 
		 layouttab.add(labels);
		 

		 
		 JPanel globalChange = new JPanel();
		 allGraphs = new JCheckBox("Apply to all open graphs");
		 globalChange.add(allGraphs);
		 layouttab.add(globalChange);
		 
		 
		 tabs.add(layouttab, ("Layout"));
		 
		 
		 
		 add(tabs, BorderLayout.CENTER);
		// bottom of the tabbed pane
		
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
	
	/**
	 * Initialisation according to the 
	 * values already set.
	 */
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
		

		 switch(Preferences.getInstance().getLayoutType()) {
		 	case JDOMGRAPH : jdomgraph.setSelected(true); break;
		 	case SUGIYAMA : sugiyama.setSelected(true); break;
		 	case CHARTLAYOUT : chartlayout.setSelected(true);
		 }
		 
		 switch(Preferences.getInstance().getLabelType()) {
		 	case LABEL : showLabels.setSelected(true); break;
		 	case NAME : showNames.setSelected(true); break;
		 	case BOTH : showBoth.setSelected(true);
		 }
		 
		 allGraphs.setSelected(Preferences.isGlobalLayoutChange());
		
		
	}
	
	/**
	 * Updating the settings wherever the changes 
	 * made by the user may be relevant.
	 */
	public void applySettings() {
		ServerOptions.setWarmup(warmup.isSelected());
		ServerOptions.setLogging(logging.isSelected());
		if(port2802.isSelected()) {
			ServerOptions.setPort(2802);
		} else {
			ServerOptions.setPort(Integer.parseInt
					(port.getText()));
		}
		
		// TODO figure out how to do this properly
		Ubench.getInstance().getMenuBar().getServerButton().setSelected(
			ConnectionManager.getState() == ConnectionManager.State.RUNNING);
		
		if(systemerrout.isSelected()) {
			ServerOptions.setLogwriter( 
					new PrintWriter(System.err, true));
		} else {
			if(logfilepath == null) {
				logfilepath = logfile.getText();
			}
			try {
				// this appends the logs to the end of the file.
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
		
		Preferences.setGlobalLayoutChange(allGraphs.isSelected());
		
		if(jdomgraph.isSelected()) {
			Preferences.getInstance().setLayoutType(LayoutType.JDOMGRAPH);
		} else if(chartlayout.isSelected()) {
			Preferences.getInstance().setLayoutType(LayoutType.CHARTLAYOUT);
		} else if(sugiyama.isSelected()) {
			Preferences.getInstance().setLayoutType(LayoutType.SUGIYAMA);
		}
		
		if(showLabels.isSelected()) {
			Preferences.getInstance().setLabelType(LabelType.LABEL);
		} else if(showNames.isSelected()) {
			Preferences.getInstance().setLabelType(LabelType.NAME);
		} else if(showBoth.isSelected()) {
			Preferences.getInstance().setLabelType(LabelType.BOTH);
		}
		 if(Ubench.getInstance().getVisibleTab() != null)
			 Ubench.getInstance().getVisibleTab().repaintIfNecessary();
	}

	/**
	 * This processes the events of applying the settings,
	 * closing the window or do both (in reverse order).
	 * Additionally, it shows a file chooser if a logfile
	 * shall be chosen.
	 * 
	 */
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if(command.equals("ok")) {
			// apply the settings and close the window
			applySettings();
			setVisible(false);
		} else if(command.equals("apply")) {
			// just apply the settings
			applySettings();
		} else if(command.equals("cancel")) {
			// just close the window
			setVisible(false);
		} else if(command.equals("browse")) {
			// chose a logfile
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
