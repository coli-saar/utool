package de.saar.chorus.ubench.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;


/**
 * 
 * A <code>JMenuBar</code> for the workbench window
 * containing several menus to operate on graphs and files
 * they are stored in.
 * 
 * @author Alexander Koller
 * @author Michaela Regneri
 */
public class JDomGraphMenu extends JMenuBar {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5896778846575447247L;

	// the upper menus
	private JMenu fileMenu,   // operating on files
				  viewMenu,   // (general) view preferences 
				  utoolMenu,  // solving
				  helpMenu;	  // Help / About	 
	
	// the menu items
	private JMenuItem 
		              loadGraph, 	// load a graph (from any supported file type)
	  				  quit, 	 	// close the workbench
					  saveUtool,	 // export a graph to a "utool" format
					  pdfPrint, 	 // export a graph to a pdf
					  close,    	 // close the visible graphs
					  closeAll, 	 // close all graphs (but not the window!)
					  duplicate,	 // duplicate the visible graph
					  cSolvForms,    // solve the visible graph
					  countAndSolve, // checkbox indicating whether or not to 
					  				 // solve every loaded graph at once
					  				 
					  showLabels,    // checkbox, indicating whether to show
					  				 // node labels or node names
					  resetLayout,   // drawing the "first" layout again
					  fitAll,        // checkbox indicating whether or not the recent
									 // and all further loaded graphs shall be zoomed
									 // out until fitting the window
				
					  about,
					  solve,
					  next,
					  previous,
					  pictureExport,
					  print,
					  loadExample,
					  saveAll,
					  displayChart,
					  displayCodecs;
    
	private JToggleButton server, serverdebug;
	
	// the listener for the menu(s)
	private CommandListener listener;
	
	// the items to deactivate if there is no graph visible
	private Set<JMenuItem> graphSpecificItems;
    
	/**
	 * Initializing the menu with a listener.
	 * @param listener the listener for the menu 
	 */
	public JDomGraphMenu(CommandListener listener) {
		super();
        this.listener = listener;
        graphSpecificItems = new HashSet<JMenuItem>();
		initialize();
		
	}
	
	/**
	 * Setting up the whole menubar.
	 */
	private void initialize() {
		
		// file Menu
		fileMenu = new JMenu("File");
		fileMenu.setMnemonic(KeyEvent.VK_F);
		
	
		// item for xml-import
		loadGraph = new JMenuItem("Open...");
		loadGraph.setMnemonic(KeyEvent.VK_O);
		loadGraph.setAccelerator(KeyStroke.getKeyStroke("ctrl O"));
		loadGraph.setActionCommand("loadGXL");
		loadGraph.addActionListener(listener);
		fileMenu.add(loadGraph);

		loadExample = new JMenuItem("Open Example...");
		loadExample.setActionCommand("loadExample");
		loadExample.addActionListener(listener);
		fileMenu.add(loadExample);
		
		
        // item for utool-export
        saveUtool = new JMenuItem("Export...");
        saveUtool.setActionCommand("saveUtool");
        saveUtool.setMnemonic(KeyEvent.VK_E);
        saveUtool.setAccelerator(KeyStroke.getKeyStroke("ctrl E"));
        saveUtool.addActionListener(listener);
        fileMenu.add(saveUtool);
        graphSpecificItems.add(saveUtool);
        
        saveAll = new JMenuItem("Export Solved Forms...");
        saveAll.setActionCommand("saveAll");
        saveAll.addActionListener(listener);
        graphSpecificItems.add(saveAll);
        
        fileMenu.add(saveAll);
        
        
        // item for pdf-export
		pdfPrint = new JMenuItem("Export as PDF...");
		pdfPrint.setActionCommand("pdf");
		pdfPrint.addActionListener(listener);
		graphSpecificItems.add(pdfPrint);
		fileMenu.add(pdfPrint);
		 // item for pdf-export
		pictureExport = new JMenuItem("Export as Picture...");
		pictureExport.setActionCommand("pic");
		pictureExport.addActionListener(listener);
		graphSpecificItems.add(pictureExport);
		fileMenu.add(pictureExport);
        
		print = new JMenuItem("Print...");
		print.setActionCommand("print");
		print.addActionListener(listener);
		graphSpecificItems.add(print);
		fileMenu.add(print);
        
        fileMenu.addSeparator();
        
        
        
        // item to duplicate the visible graph
        duplicate = new JMenuItem("Duplicate Tab");
        duplicate.setActionCommand("dup");
        duplicate.setMnemonic(KeyEvent.VK_D);
        duplicate.setAccelerator(KeyStroke.getKeyStroke("ctrl D"));
        duplicate.addActionListener(listener);
        fileMenu.add(duplicate);
        
        // item to close the graph
        close = new JMenuItem("Close Tab");
        close.setActionCommand("shut");
        close.setAccelerator(KeyStroke.getKeyStroke("ctrl W"));
        close.addActionListener(listener);
        fileMenu.add(close);
        
        // item to close all loaded graphs
        closeAll = new JMenuItem("Close All Tabs");
        closeAll.setActionCommand("closeAll");
        closeAll.addActionListener(listener);
        fileMenu.add(closeAll);
        
        
        fileMenu.addSeparator();
        
        
		// item for quitting Leonardo
		quit = new JMenuItem("Quit");
		quit.setActionCommand("quit");
		quit.setAccelerator(KeyStroke.getKeyStroke("ctrl Q"));
		quit.addActionListener(listener);
		fileMenu.add(quit);
		
		fileMenu.validate();
		add(fileMenu);
		
        
        
        
		
		// view Menu
		viewMenu = new JMenu("View");
		viewMenu.setMnemonic(KeyEvent.VK_V);

        // Checkbox indicating label/name - view
		showLabels = new JCheckBoxMenuItem("Show Node Labels");
		showLabels.addItemListener(listener);
		listener.registerEventSource(showLabels, "showLabels");
		viewMenu.add(showLabels);
		
		// Checkbox indicating whether the graphs should be
		// scaled town in order to fit the window
		fitAll = new JCheckBoxMenuItem("Fit Graphs To Window");
		fitAll.addItemListener(listener);
		fitAll.setAccelerator(KeyStroke.getKeyStroke("ctrl F"));
        listener.registerEventSource(fitAll,"fitAll");
		viewMenu.add(fitAll);
		
		// item to rebuild the initial layout again
		resetLayout = new JMenuItem("Reset Layout");
		resetLayout.setActionCommand("resL");
		resetLayout.addActionListener(listener);
		viewMenu.add(resetLayout);	
		graphSpecificItems.add(resetLayout);
		
		displayChart = new JMenuItem("Display Chart");
		displayChart.setMnemonic(KeyEvent.VK_C);
		displayChart.setAccelerator(KeyStroke.getKeyStroke("alt C"));
		displayChart.setActionCommand("chartView");
		displayChart.addActionListener(listener);
		viewMenu.add(displayChart);
		
		viewMenu.validate();
		add(viewMenu);
		
		
		// utool-Menu
		utoolMenu = new JMenu("Solver");
		utoolMenu.setMnemonic(KeyEvent.VK_S);
		
		// checkbox indicating whether all graphs loaded
		// shall be solved at once
		countAndSolve = new JCheckBoxMenuItem("Count Solved Forms Automatically");
		countAndSolve.addItemListener(listener);
        listener.registerEventSource(countAndSolve, "countAndSolve");
		
		utoolMenu.add(countAndSolve);
		
		// item to solve the visible graph "manually".
		// Doesn't make sense if "countAndSolve" is selected!
		cSolvForms = new JMenuItem("Count Solved Forms");
		cSolvForms.setActionCommand("cSF");
		cSolvForms.addActionListener(listener);
		utoolMenu.add(cSolvForms);
		
		utoolMenu.add(cSolvForms);
		graphSpecificItems.add(cSolvForms);

		
		
		
		showLabels.setSelected(true);

		
        countAndSolve.setSelected(true);
        cSolvForms.setEnabled(false);
       
        
        solve = new JMenuItem("Show first solved form");
        solve.setActionCommand("solve");
        solve.addActionListener(listener);
        graphSpecificItems.add(solve);
        utoolMenu.addSeparator();
        utoolMenu.add(solve);
        
        next = new JMenuItem("Show next solved form");
        next.setActionCommand("plus");
        graphSpecificItems.add(next);
        next.addActionListener(listener);
        next.setEnabled(false);
        
        previous = new JMenuItem("Show previous solved form");
        previous.setActionCommand("minus");
        previous.addActionListener(listener);
        graphSpecificItems.add(previous);
        previous.setEnabled(false);
        
        utoolMenu.add(next);
        utoolMenu.add(previous);
        
       
        
        
		add(utoolMenu);
        graphSpecificItems.add(cSolvForms);
        
        helpMenu = new JMenu("Help");
        helpMenu.setActionCommand("help");
        helpMenu.addActionListener(listener);
        helpMenu.setMnemonic(KeyEvent.VK_H);
        
        displayCodecs = new JMenuItem("Show all codecs...");
        displayCodecs.setActionCommand("showcodecs");
        displayCodecs.addActionListener(listener);
        
        helpMenu.add(displayCodecs);
        
        about = new JMenuItem("About...");
        about.setActionCommand("about");
        about.addActionListener(listener);
        about.setMnemonic(KeyEvent.VK_A);
        
        helpMenu.add(about);
        add(helpMenu);
        
        server = new JToggleButton(new ImageIcon("projects/Domgraph/pictures/Ch5.gif")) {
        	public String getToolTipText() {
        		if(isSelected()) {
        			return ("Server Running.");
        		} else {
        			return ("Server Off.");
        		}
        	}
        };
        server.setActionCommand("server");
        server.addActionListener(listener);
        server.setBackground(Color.GRAY);
        server.setOpaque(false);
        server.setIconTextGap(1);
        server.setMargin(new Insets(1,1,1,1));
        add(Box.createHorizontalGlue());
        add(server);
        
        serverdebug = new JToggleButton("DS");
        serverdebug.setActionCommand("serverd");
        serverdebug.addActionListener(listener);
        add(serverdebug);
        
        
	}
	
	/**
	 * Enable or disable the countSolvedForms menu item
	 * @param b set to true the item gets enabled
	 */
    void setCountSfEnabled(boolean b) {
    	if( Ubench.getInstance().getVisibleTab() != null )
    		cSolvForms.setEnabled(b);
    }
	
    void setServerButtonPressed(boolean b) {
        server.setSelected(b);
    }
    
    boolean isServerButtonPressed() {
    	return server.isSelected();
    }
    boolean isServerDButtonPressed() {
    	return serverdebug.isSelected();
    }
    
	/**
	 * Enable or disable the items that operate
	 * on the visible graph.
	 * @param b set to false the items get disabled
	 */
	public void setGraphSpecificItemsEnabled(boolean b) {
		for( JMenuItem item : graphSpecificItems ) {
		
			item.setEnabled(b);
			if( item.equals(cSolvForms) ) {
				if( b && countAndSolve.isSelected() ) {
					item.setEnabled(false);
				}
			} 
				
		}
    }
	
	public void setSolvingEnabled(boolean b) {
		solve.setEnabled(b);
		if(! countAndSolve.isSelected() )
			cSolvForms.setEnabled(b);
	}
	
	public void setPlusMinusEnabled(boolean plus, boolean minus) {
		next.setEnabled(plus);
		previous.setEnabled(minus);
	}
	
	public void setSaveAllEnables(boolean b) {
		saveAll.setEnabled(b);
	}
}
