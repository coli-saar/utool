package de.saar.chorus.ubench;

import java.awt.Color;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;

import de.saar.chorus.domgraph.UserProperties;
import de.saar.chorus.domgraph.utool.server.ConnectionManager;


/**
 * 
 * A <code>JMenuBar</code> for the workbench window
 * containing several menus to operate on graphs and files
 * they are stored in.
 * 
 * @author Alexander Koller
 * @author Michaela Regneri
 */
class JDomGraphMenu extends JMenuBar {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5896778846575447247L;

	// the upper menus
	private JMenu fileMenu,   // operating on files
				  viewMenu,   // (general) view preferences 
				  utoolMenu,  // solving
				  helpMenu, layoutMenu, labelMenu;	  // Help / About	 
	
	// the menu items
	private JMenuItem 
		             // loadGraph, 	// load a graph (from any supported file type)
		             // newTab,
	  				//  quit, 	 	// close the workbench
					 // saveUtool,	 // export a graph to a "utool" format
					 // pdfPrint, 	 // export a graph to a pdf
					 // close,    	 // close the visible graphs
					 // closeAll, 	 // close all graphs (but not the window!)
					 // duplicate,	 // duplicate the visible graph
					  cSolvForms,    // solve the visible graph
					  countAndSolve, // checkbox indicating whether or not to 
					  				 // solve every loaded graph at once
					  				 
					  showLabels,    showNames, showBoth,
					//  resetLayout,   // drawing the "first" layout again
					//  fitAll,        // checkbox indicating whether or not the recent
									 // and all further loaded graphs shall be zoomed
									 // out until fitting the window
				
					//  about,
					  solve,
					  next,
					  previous,
					//  pictureExport,
					//  print,
					//  loadExample,
					  saveAll,
					  displayChart,
					  displayCodecs,
					//  loadeqs,
					  autoreduce,
					//  preferences,
					 jdomgraphlayout, chartlayout;
    
	private ServerButton server;
	
	// the listener for the menu(s)
	private CommandListener listener;
	
	// the items to deactivate if there is no graph visible
	private Set<JMenuItem> graphSpecificItems;
    
	/**
	 * Initializing the menu with a listener.
	 * @param listener the listener for the menu 
	 */
	JDomGraphMenu(CommandListener listener) {
		super();
        this.listener = listener;
        graphSpecificItems = new HashSet<JMenuItem>();
		initialize();
		
	}
	
	/**
	 * Setting up the whole menubar.
	 */
	private void initialize() {
		
		int control = 
	        Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
		

    	displayCodecs = new JMenuItem("Show all codecs...");
    	displayCodecs.setActionCommand("showcodecs");
    	displayCodecs.addActionListener(listener);
    	
		
		// File menu
		fileMenu = new JMenu("File");
		fileMenu.setMnemonic(KeyEvent.VK_F);
		add(fileMenu);
		
		makeMenuItem(fileMenu, "Open new tab", "newTab", -1, KeyStroke.getKeyStroke(KeyEvent.VK_T, control));
		makeMenuItem(fileMenu, "Open...", "loadGXL", KeyEvent.VK_O, KeyStroke.getKeyStroke(KeyEvent.VK_O, control));
		makeMenuItem(fileMenu, "Open example...", "loadExample");
		graphSpecificItems.add(makeMenuItem(fileMenu, "Export...", "saveUtool", KeyEvent.VK_E, KeyStroke.getKeyStroke(KeyEvent.VK_E, control)));
		saveAll = makeMenuItem(fileMenu, "Export solved forms...", "saveAll");
		graphSpecificItems.add(saveAll);
		
        if( MacIntegration.isMac() ) {
        	fileMenu.add(displayCodecs);
        }
        
        fileMenu.addSeparator();
        
        graphSpecificItems.add(makeMenuItem(fileMenu, "Export as PDF...", "pdf"));
        graphSpecificItems.add(makeMenuItem(fileMenu, "Export as image...", "pic"));
        graphSpecificItems.add(makeMenuItem(fileMenu, "Print...", "print"));
                
        fileMenu.addSeparator();
        
        makeMenuItem(fileMenu, "Duplicate tab", "dup", KeyEvent.VK_D, KeyStroke.getKeyStroke(KeyEvent.VK_D, control));
        makeMenuItem(fileMenu, "Close tab", "shut", -1, KeyStroke.getKeyStroke(KeyEvent.VK_W, control));
        makeMenuItem(fileMenu, "Close all tabs", "closeAll");
        
        if( !MacIntegration.isMac() ) {
            fileMenu.addSeparator();
            makeMenuItem(fileMenu, "Quit", "quit", -1, KeyStroke.getKeyStroke(KeyEvent.VK_Q, control));
        }
		


		// Edit menu
		JMenu editMenu = new JMenu("Edit");
		editMenu.setMnemonic(KeyEvent.VK_E);
		add(editMenu);
		
        JMenu exportToClipboardMenu = new JMenu("Copy to clipboard");
        editMenu.add(exportToClipboardMenu);
        graphSpecificItems.add(exportToClipboardMenu);
        
        String definput = UserProperties.getDefaultInputCodec();
        String defoutput = UserProperties.getDefaultOutputCodec();
        
        for( String codecname : Ubench.getInstance().getCodecManager().getAllOutputCodecs() ) {
        	if( codecname.equals(defoutput)) {
        		makeMenuItem(exportToClipboardMenu, "as " + codecname, "export-clipboard-" + codecname,
        				-1, KeyStroke.getKeyStroke(KeyEvent.VK_C, control));
        	} else {
        		makeMenuItem(exportToClipboardMenu, "as " + codecname, "export-clipboard-" + codecname);
        	}
        }

        JMenu importFromClipboardMenu = new JMenu("Paste into new tab");
        editMenu.add(importFromClipboardMenu);
        
        for( String codecname : Ubench.getInstance().getCodecManager().getAllInputCodecs() ) {
        	if( codecname.equals(definput)) {
        		makeMenuItem(importFromClipboardMenu, "as " + codecname, "import-clipboard-" + codecname,
        				-1, KeyStroke.getKeyStroke(KeyEvent.VK_V, control));
        	} else {
        		makeMenuItem(importFromClipboardMenu, "as " + codecname, "import-clipboard-" + codecname);
        	}
        }

        

        // View menu
		viewMenu = new JMenu("View");
		viewMenu.setMnemonic(KeyEvent.VK_V);
		add(viewMenu);
		
		labelMenu = new JMenu("Node labels");
		ButtonGroup labelgroup = new ButtonGroup();
		viewMenu.add(labelMenu);

		showLabels = makeCheckboxMenuItem(labelMenu, labelgroup, "Show labels", "showLabels");
		showNames = makeCheckboxMenuItem(labelMenu, labelgroup, "Show names", "showNames");
		showBoth = makeCheckboxMenuItem(labelMenu, labelgroup, "Show names and labels", "showBoth");
		showLabels.setSelected(true);
		
		makeCheckboxMenuItem(viewMenu, null, "Fit graphs to window", "fitAll")
			.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, control));
		
		graphSpecificItems.add(makeMenuItem(viewMenu, "Reset layout", "resL"));
		
		
		layoutMenu = new JMenu("Choose Layout");
		ButtonGroup layoutgroup = new ButtonGroup();
		jdomgraphlayout = makeCheckboxMenuItem(layoutMenu, layoutgroup, "JDomGraph layout", "layoutchange");
		chartlayout = makeCheckboxMenuItem(layoutMenu, layoutgroup, "Chart layout", "layoutchange");
		viewMenu.add(layoutMenu);
		
		switch(Preferences.getInstance().getLayoutType()) {
		case JDOMGRAPH : jdomgraphlayout.setSelected(true); break;
		case CHARTLAYOUT : chartlayout.setSelected(true);
		}
		
		viewMenu.addSeparator();
		
		displayChart = makeMenuItem(viewMenu, "Display chart", "chartView", KeyEvent.VK_C, KeyStroke.getKeyStroke("alt C"));
		graphSpecificItems.add(displayChart);

		
		
		
		// Solver menu
		utoolMenu = new JMenu("Solver");
		utoolMenu.setMnemonic(KeyEvent.VK_S);
		add(utoolMenu);

		countAndSolve = makeCheckboxMenuItem(utoolMenu, null, "Count solved forms automatically", "countAndSolve");
		countAndSolve.setSelected(true);
		
		cSolvForms = makeMenuItem(utoolMenu, "Count solved forms", "cSF");
		graphSpecificItems.add(cSolvForms);
		cSolvForms.setEnabled(false);
		
		utoolMenu.addSeparator();
		
		solve = makeMenuItem(utoolMenu, "Show first solved form", "solve");
		graphSpecificItems.add(solve);
		next = makeMenuItem(utoolMenu, "Show next solved form", "plus");
		next.setEnabled(false);
		graphSpecificItems.add(next);
		previous = makeMenuItem(utoolMenu, "Show previous solved form", "minus");
		previous.setEnabled(false);
		graphSpecificItems.add(previous);
		
        utoolMenu.addSeparator();

        makeMenuItem(utoolMenu, "Load equation system...", "loadeqs");
        autoreduce = makeCheckboxMenuItem(utoolMenu, null, "Reduce chart automatically", "autoreduce");
        autoreduce.setSelected(Ubench.getInstance().reduceAutomatically);
		autoreduce.setEnabled(Ubench.getInstance().isRelativeNormalFormsComputerLoaded());


		
		// Help menu
		// (skip this when running on a Mac)
        
        if( !MacIntegration.isMac() ) {
        	helpMenu = new JMenu("Help");
        	helpMenu.setMnemonic(KeyEvent.VK_H);
        	add(helpMenu);

        	helpMenu.add(displayCodecs);
        	
        	makeMenuItem(helpMenu, "About...", "about");
        	makeMenuItem(helpMenu, "Settings...", "preferences");
        } 
        
        
        
        
        server = new ServerButton(listener);
        add(Box.createHorizontalGlue());
        add(server);      
        
	}
	
	private JMenuItem makeMenuItem(JMenu menu, String label, String command, int keyEvent, KeyStroke keystroke) {
		JMenuItem ret = new JMenuItem(label);
		
		if( keyEvent > -1 ) { ret.setMnemonic(keyEvent); }
		ret.setAccelerator(keystroke);
		ret.setActionCommand(command);
		ret.addActionListener(listener);
		
		menu.add(ret);
		
		return ret;
	}
	
	private JMenuItem makeMenuItem(JMenu menu, String label, String command) {
		JMenuItem ret = new JMenuItem(label);
		
		ret.setActionCommand(command);
		ret.addActionListener(listener);
		
		menu.add(ret);
		
		return ret;
	}
	
	private JCheckBoxMenuItem makeCheckboxMenuItem(JMenu menu, ButtonGroup group, String label, String command) {
		JCheckBoxMenuItem ret = new JCheckBoxMenuItem(label);
		
		ret.addItemListener(listener);
		listener.registerEventSource(ret, command);
		
		menu.add(ret);
		if( group != null ) { group.add(ret); }
		
		return ret;
	}
	
	
	/**
	 * Enable or disable the countSolvedForms menu item
	 * @param b set to true the item gets enabled
	 */
    void setCountSfEnabled(boolean b) {
    	if( Ubench.getInstance().getVisibleTab() != null )
    		cSolvForms.setEnabled(b);
    }
	
   
   
    
	public ServerButton getServerButton() {
		return server;
	}

	
	/**
	 * Enable or disable the items that operate
	 * on the visible graph.
	 * @param b set to false the items get disabled
	 */
	 void setGraphSpecificItemsEnabled(boolean b) {
		for( JMenuItem item : graphSpecificItems ) {
		
			item.setEnabled(b);
			if( item.equals(cSolvForms) ) {
				if( b && countAndSolve.isSelected() ) {
					item.setEnabled(false);
				}
			} 
				
		}
    }
	
	/**
	 * Enables or disables the menu item for solving.
	 * @param b
	 */
	void setSolvingEnabled(boolean b) {
		solve.setEnabled(b);
		chartlayout.setEnabled(b);
		if(! countAndSolve.isSelected() ) {
			cSolvForms.setEnabled(b);
		}
		displayChart.setEnabled(b);
	}
	
	void refresh() {

		switch(Ubench.getInstance().getVisibleTab().getLayoutType()) {
		case JDOMGRAPH : jdomgraphlayout.setSelected(true); break;
		case CHARTLAYOUT : chartlayout.setSelected(true);
		}
		
		switch(Ubench.getInstance().getVisibleTab().getLabelType()) {
		case NAME : showNames.setSelected(true); break;
		case LABEL : showLabels.setSelected(true); break;
		case BOTH : showBoth.setSelected(true);
		}
		
	}
	
	/**
	 * Enables the buttons that allow browsing solved forms
	 * @param plus enables or disables the "forward" button
	 * @param minus enalbes or disables the "backward" button
	 */
	void setPlusMinusEnabled(boolean plus, boolean minus) {
		next.setEnabled(plus);
		previous.setEnabled(minus);
	}
	
	/**
	 * Enables or disables export of solved forms.
	 * @param b if set to true, the menu item for solved form export is enabled
	 */
	void setSaveAllEnabled(boolean b) {
		saveAll.setEnabled(b);
	}
	
	public void setReduceAutomaticallyEnabled(boolean b) {
		autoreduce.setEnabled(b);
	}
	
	static class ServerButton extends JToggleButton {
		/**
		 * 
		 */
		private static final long serialVersionUID = -9122903869661066502L;

	    ServerButton(CommandListener listener) {
			super();
			addActionListener(listener);
			initialise();
		}
		
		void initialise() {
			URL picurl = null;
	        picurl = Thread.currentThread().getContextClassLoader().getResource("projects/Domgraph/pictures/Ch5.gif");
	        if(picurl == null) {
	        	picurl = Thread.currentThread().getContextClassLoader().getResource("pictures/Ch5.gif");
	        }
	        if(picurl == null) {
	        	setText("@");
	        } else {
	        	ImageIcon ic = new ImageIcon(picurl);
	        	setIcon(ic);
	        }
	        
	        setActionCommand("server");
	        setBackground(Color.GRAY);
	        setOpaque(false);
	        setIconTextGap(1);
	        setMargin(new Insets(1,1,1,1));
	        setSelected(ConnectionManager.getState() 
	        		== ConnectionManager.State.RUNNING);
		}
		
		@Override
		public void setSelected(boolean pressed) {
			super.setSelected(pressed);
			
			if(pressed) {
				setToolTipText("The server is running. " +
	        			System.getProperty("line.separator") + 
	        		"Click here to stop it.");
			} else {
				setToolTipText("Click here to start a server " + 
	        			System.getProperty("line.separator") + 
	        			"on port "  + ServerOptions.getPort() + ".");
			}
		}
		
		
	}

	public void setLayoutSelectionEnabled(boolean b) {
		layoutMenu.setEnabled(b);
	}
}
