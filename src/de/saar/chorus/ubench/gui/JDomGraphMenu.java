package de.saar.chorus.ubench.gui;

import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
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

	// the upper menus
	private JMenu fileMenu,   // operating on files
				  graphMenu,  // operating on the visible graph	
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
					  previous;
    
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

        // item for utool-export
        saveUtool = new JMenuItem("Export...");
        saveUtool.setActionCommand("saveUtool");
        saveUtool.setMnemonic(KeyEvent.VK_E);
        saveUtool.setAccelerator(KeyStroke.getKeyStroke("ctrl E"));
        saveUtool.addActionListener(listener);
        fileMenu.add(saveUtool);
        graphSpecificItems.add(saveUtool);
        
        // item for pdf-export
		pdfPrint = new JMenuItem("Export as PDF...");
		pdfPrint.setActionCommand("pdf");
		pdfPrint.addActionListener(listener);
		graphSpecificItems.add(pdfPrint);
		fileMenu.add(pdfPrint);
		
		// item for quitting Leonardo
		quit = new JMenuItem("Quit");
		quit.setActionCommand("quit");
		quit.setAccelerator(KeyStroke.getKeyStroke("ctrl Q"));
		quit.addActionListener(listener);
		fileMenu.add(quit);
		
		fileMenu.validate();
		add(fileMenu);
		
		//	graph Menu
		graphMenu = new JMenu("Graph");
		graphMenu.setMnemonic(KeyEvent.VK_G);
		
		// item to duplicate the visible graph
		duplicate = new JMenuItem("Duplicate");
		duplicate.setActionCommand("dup");
		duplicate.setMnemonic(KeyEvent.VK_D);
		duplicate.setAccelerator(KeyStroke.getKeyStroke("ctrl D"));
		duplicate.addActionListener(listener);
		graphMenu.add(duplicate);
		
		
		
		// item to close the graph
		close = new JMenuItem("Close");
		close.setActionCommand("shut");
		close.setAccelerator(KeyStroke.getKeyStroke("ctrl W"));
		close.addActionListener(listener);
		graphMenu.add(close);
		
		// item to close all loaded graphs
		closeAll = new JMenuItem("Close All Graphs");
		closeAll.setActionCommand("closeAll");
		closeAll.addActionListener(listener);
		graphMenu.add(closeAll);
		
		graphMenu.validate();
		add(graphMenu);
		graphSpecificItems.add(graphMenu);
		
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
		viewMenu.validate();
		add(viewMenu);
		
		
		// utool-Menu
		utoolMenu = new JMenu("Solving");
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
       
        
        solve = new JMenuItem("Solve");
        solve.setActionCommand("solve");
        solve.addActionListener(listener);
        utoolMenu.addSeparator();
        utoolMenu.add(solve);
        
        next = new JMenuItem("Show next solved form");
        next.setActionCommand("plus");
        next.addActionListener(listener);
        next.setEnabled(false);
        
        previous = new JMenuItem("Show previous solved form");
        previous.setActionCommand("minus");
        previous.addActionListener(listener);
        previous.setEnabled(false);
        
        utoolMenu.add(next);
        utoolMenu.add(previous);
        
		add(utoolMenu);
        graphSpecificItems.add(cSolvForms);
        
        helpMenu = new JMenu("Help");
        helpMenu.setActionCommand("help");
        helpMenu.addActionListener(listener);
        helpMenu.setMnemonic(KeyEvent.VK_H);
        
        about = new JMenuItem("About...");
        about.setActionCommand("about");
        about.addActionListener(listener);
        about.setMnemonic(KeyEvent.VK_A);
        
        helpMenu.add(about);
        add(helpMenu);
        
	}
	
	/**
	 * Enable or disable the countSolvedForms menu item
	 * @param b set to true the item gets enabled
	 */
    void setCountSfEnabled(boolean b) {
    	if( Main.getVisibleTab() != null )
    		cSolvForms.setEnabled(b);
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
		countAndSolve.setEnabled(b);
		solve.setEnabled(b);
		cSolvForms.setEnabled(false);
	}
	
	public void setPlusMinusEnabled(boolean plus, boolean minus) {
		next.setEnabled(plus);
		previous.setEnabled(minus);
	}
	
}
