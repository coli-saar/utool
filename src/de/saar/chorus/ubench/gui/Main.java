/*
 * Created on 27.07.2004
 *
 */
package de.saar.chorus.ubench.gui;

import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.ToolTipManager;

import de.saar.chorus.domgraph.codec.CodecManager;
import de.saar.chorus.domgraph.codec.InputCodec;
import de.saar.chorus.domgraph.codec.basic.Chain;
import de.saar.chorus.domgraph.codec.domcon.DomconGxlInputCodec;
import de.saar.chorus.domgraph.codec.domcon.DomconGxlOutputCodec;
import de.saar.chorus.domgraph.codec.domcon.DomconOzInputCodec;
import de.saar.chorus.domgraph.codec.domcon.DomconOzOutputCodec;
import de.saar.chorus.domgraph.codec.domcon.DomconUdrawOutputCodec;
import de.saar.chorus.domgraph.codec.holesem.HolesemComsemInputCodec;
import de.saar.chorus.domgraph.codec.mrs.MrsPrologInputCodec;
import de.saar.chorus.domgraph.codec.plugging.DomconOzPluggingOutputCodec;
import de.saar.chorus.domgraph.codec.plugging.LkbPluggingOutputCodec;
import de.saar.chorus.domgraph.codec.term.OzTermOutputCodec;
import de.saar.chorus.domgraph.codec.term.PrologTermOutputCodec;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.domgraph.utool.ExitCodes;
import de.saar.chorus.ubench.DomGraphGXLCodec;
import de.saar.chorus.ubench.DomGraphTConverter;
import de.saar.chorus.ubench.Fragment;
import de.saar.chorus.ubench.JDomGraph;
import de.saar.getopt.ConvenientGetopt;

/**
 * The main program for Leonardo.
 * 
 * At the moment, this program can be run in three modes:
 * <ol>
 * <li><strong>Standalone mode </strong>: Expects the name of a file containing
 * a GXL description of a dominance graph on the command line. Parses the file
 * and displays the dominance graph.
 * <li><strong>Dummy mode </strong>: Without any command-line arguments, the
 * program will display a small sample graph.
 * <li><strong>Server mode </strong>: Accepts socket connections on the
 * specified port. If the client sends a GXL description of a graph (on a single
 * line), it will draw this graph. Clicks on popup menus are translated into
 * messages of the form
 * 
 * <blockquote><code>
 * &lt;popupClicked clickedOn="..." type="..." menulabel="..." /&gt;,
 * </code>
 * </blockquote>
 * 
 * which are sent back to the client. "clickedOn" is the name of the node or
 * edge that triggered the popup menu. "type" is either "node" or "edge",
 * depending on the type of the popup trigger. "menulabel" is the menu label
 * that was specified in the GXL description.
 * <p>
 * 
 * The client can also send a message <code>&lt;close /&gt;</code>, which
 * will instruct Leonardo to close the current socket (and window), and accept a
 * new socket connection.
 * </ol>
 * 
 * 
 * TODO Distinguish holes and roots, and draw holes differently (as circles?).
 * <p>
 * 
 * TODO Extend GXL format so nodes and perhaps edges can be given visual
 * attributes, e.g. colours. This is used quite a bit in the CHORUS demo, and we
 * should support it.
 * 
 * @author Alexander Koller
 * @author Michaela Regneri
 *  
 */
public class Main  {
	
	// the tabs in their order of insertion
	private static ArrayList<JDomGraphTab> tabs = new ArrayList<JDomGraphTab>();
	
	// the pane containing the tabs
	private static JDomTabbedPane tabbedPane ;
	
	// the scaling slider
	private static JDomGraphSlider slider = new JDomGraphSlider();
	
	// the status bar
	private static JDomGraphStatusBar statusBar;
	
	// the manager for tooltips (needed to shorten the stanard delay)
	private static ToolTipManager ttm = ToolTipManager.sharedInstance();
	
	// the main window
	private static JFrame window;
    
	// the main listener for menus and buttons
	private static CommandListener listener;
    
	private static Map<DomGraph, NodeLabels> graphToLabels;
	
	// the menu bar 
	private static JDomGraphMenu menuBar;
    
    /**
     * Aligning the slider with the currently shown graph.
     * (if there is one).
     */
	public static void resetSlider() {
		if(getVisibleGraph() != null) {
			slider.setValue((int) (Main.getVisibleGraph().getScale()*100));
		} else {
			// if there is no graph to show, the slider
			// is set to 100%.
			slider.setValue(100);
		}
	}
	
	/**
	 * Showing the bar for the solving
	 * process.
	 */
	public static void showProgressBar() {
		statusBar.showProgressBar();
	}
	
	/**
	 * @return the main window itself
	 */
	public static JFrame getWindow() {
		return window;
	}
	
	/**
	 * Closing all tabs.
	 */
	public static void closeAllTabs() {
		tabbedPane.removeAll();
		refresh();
	}
	
	/**
	 * Closing Leonardo.
	 */
	public static void quit() {
		System.exit(0);
	}
	
	/**
	 * Returning the tab height (considering the possible
	 * minimization of the window).
	 * 
	 * @return the height
	 */
	public static double getTabHeight() {
		int index = tabbedPane.getSelectedIndex();
		double windowScale = 1;
		if( window.getState() != JFrame.NORMAL)
			windowScale =(double) window.getHeight()/window.getMaximizedBounds().height;
		
		
		if( index > -1 ) {
			Rectangle tabRect = tabbedPane.getBoundsAt(
					tabbedPane.getSelectedIndex());
			return  tabbedPane.getHeight()*windowScale - tabRect.height;
		}
		
		return (window.getHeight() - 100)*windowScale;
	}
	
	/**
	 * Returning the tab width (considering the possible
	 * minimization of the window).
	 * 
	 * @return the width
	 */
	public static double getTabWidth() {
		double windowScale = 1;
		if( window.getState() != JFrame.NORMAL)
			windowScale =(double) window.getWidth()/window.getMaximizedBounds().width;
		
		return tabbedPane.getWidth()*windowScale;
	}
	
	/**
	 * Refreshes the menu, the slider and the status bar.
	 */
	public static void refresh() {
	    if(getVisibleTab() != null) {
	    	getVisibleTab().repaintIfNecessary();
	    } else {
	    	menuBar.setGraphSpecificItemsEnabled(false);
	    }
		resetSlider();
		statusBar.refresh();
	}
	
	/**
	 * Closes the currently shown tab (if there
	 * is one).
	 */
	public static void closeCurrentTab() {
		int index = tabbedPane.getSelectedIndex();
		
		if(index > -1) {
			tabbedPane.remove(index);
			tabs.remove(index);
		
		}
		refresh();
	}
	
	/**
	 * @return the currently shown tab or null if there is none
	 */
	public static JDomGraphTab getVisibleTab() {
		int index = tabbedPane.getSelectedIndex();
		
		if( index > -1 )
			return(tabs.get(tabbedPane.getSelectedIndex()));
		else
			return null;
	}
	
	/**
	 * 
	 * @return the index of the currently shown tab
	 */
	public static int getVisibleTabIndex() {
		return tabbedPane.getSelectedIndex();
	}
	
	/**
	 * Adding a complete tab to the window 
	 * 
	 * @param tab the tab to ad
	 * @param showNow if set to true, the tab will be displayed at once
	 */
	public static void addTab(JDomGraphTab tab, boolean showNow) {
		
		addTab(tab, showNow, tabbedPane.getTabCount());
		
	}
	
	/**
	 * Adding a complete tab to the window 
	 * 
	 * @param tab the tab to ad
	 * @param showNow if set to true, the tab will be displayed at once
	 */
	public static void addTab(JDomGraphTab tab, boolean showNow, int ind) {
		
		int index;
		
		
		// registering the tab 
		tabs.add(tab);
		
		if( ind < (tabs.size() -1)) {
			index = ind;
		
			tabbedPane.insertTab(tab.getDefaultName(), null,  new JScrollPane(tab),  tab.getDefaultName(), index);
		} else {
			tabbedPane.addTab(tab.getDefaultName(), new JScrollPane(tab));
			index = tabs.size() -1;
		}
		
		tabs.add(ind, tab);
		ttm.registerComponent(tab.getGraph());
		
		
		// if it's the first tab added, the graph menus get enabled
		if( tabbedPane.getTabCount() == 1 ) {
			menuBar.setGraphSpecificItemsEnabled(true);
		}
		
		// if the tab shall be shown, the selected index is
		// the last one
		if(showNow) {
			tabbedPane.setSelectedIndex(index);
		}
		
        tabbedPane.validate();

        // aligning with preferences...
        
        // fitting?
		if(Preferences.isFitToWindow())
			tab.fitGraph();
		
		// solving?
		if(Preferences.isAutoCount() && Preferences.utoolPresent()) {
		
			statusBar.showProgressBar();
			
			tab.solve();
		}
        
		refresh();
	}
    
	/**
	 * Adding a new tab to the window displaying the
	 * given <code>JDomGraph</Code>
	 * 
	 * @param graph the graph to display
	 * @param label the name for the tab
	 * @param paintNow if set to true, the graph is layoutet at once
	 * @param showNow if set to true, the tab will be shown after creating
	 * @return the tab or null if an error occured while setting up the tab
	 */
    public static JDomGraphTab addNewTab(JDomGraph graph, String label, 
    		DomGraph origin, boolean paintNow, boolean showNow, NodeLabels labels) {
        
    	// the new tab
    	JDomGraphTab tab = new JDomGraphTab(graph,  origin, label,paintNow, listener, labels);
        if( tab.getGraph() != null ) {
            
        	// tab sucessfully created
        	addTab(tab, showNow);
            return tab;
        } else {
        	// something went wrong (the tab contains no graph)
            return null;
        }

    } 
    
    /**
     * Adding a new tab to the window displaying the
	 * given <code>JDomGraph</Code>, given the index indicating
	 * where to insert it in the tab.
	 * 
     * @param graph the graph to display
     * @param label the name of the tab
     * @param paintNow if set to true the graph is layoutet at once
     * @param showNow if set to true the graph is shown immidiately
     * @param index indicating on which place of the tab the new tab shall be inserted
     * @return the new tab or null if anything fails
     */
    public static JDomGraphTab addNewTab(JDomGraph graph, String label, 
    		DomGraph origin, boolean paintNow, boolean showNow, int index,
    		NodeLabels labels) {
    	
    	// the new tab
    	JDomGraphTab tab = new JDomGraphTab(graph, origin, label, paintNow, listener, labels);
    	if( tab.getGraph() != null ) {
    		
    		// tab sucessfully created
    		addTab(tab, showNow, index);
    		return tab;
    	} else {
    		// something went wrong (the tab contains no graph)
    		return null;
    	}
    	
    } 
    
	
    /**
     * @return the <code>JDomGraph</code> currently displayed
     */
	private static JDomGraph getVisibleGraph() {
		int index = tabbedPane.getSelectedIndex();
		
		if( index > -1 )
			return(tabs.get(tabbedPane.getSelectedIndex()).getGraph());
		else
			return null;
	}
    
	/**
	 * 
	 * @param filename
	 * @return
	 */
    public static JDomGraph genericLoadGraph(String filename, DomGraph graph, NodeLabels nl) {
      
            return importGraph(filename, graph, nl);
        
    }
	
    /**
     * 
     * @param fileName
     * @return
     */
	public static JDomGraph loadGraph(String fileName) {
		JDomGraph loadedGraph = new JDomGraph();
		try {
			File gxl = new File(fileName);	
			Reader input = new FileReader(gxl);
			DomGraphGXLCodec.decode(input, loadedGraph);
			for( Fragment frag : loadedGraph.getFragments() ) {
				System.out.println(frag);
			}
		} catch (IOException e ) {
			System.err.println("File can't be found");		
		} catch (Exception e) {
			System.err.println("Error while parsing " + fileName + ":");
        	e.printStackTrace(System.err);
        	System.exit(1);
		}
	
		return loadedGraph;
	}
	
	/**
	 * 
	 * @param filename
	 * @return
	 */
    public static JDomGraph importGraph(String filename, DomGraph graph, NodeLabels nl) {
        
        CodecManager codecManager = new CodecManager();
        registerAllCodecs(codecManager);
        InputCodec inputCodec = codecManager.getInputCodecForFilename(filename);
        
        NodeLabels nodeLabels = new NodeLabels();
        try {
            inputCodec.decodeFile(filename, graph, nl);
        } catch(Exception e) {
        	JOptionPane.showMessageDialog(window,
                    "An error occurred while loading this graph\n(perhaps the file " +
                    "doesn't exist,\nor the input codec couldn't be determined or was " +
                    "unable to parse the graph).",
                    "Error during import",
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }
        
        graphToLabels.put(graph,nl);
        DomGraphTConverter conv = new DomGraphTConverter(graph, nl);
        return conv.getJDomGraph();
        
     /*   
    	DomSolver solver = new DomSolver();
        boolean ok = solver.loadGraph(filename);
        
        if( !ok ) {
            JOptionPane.showMessageDialog(window,
                    "An error occurred while loading this graph\n(perhaps the file " +
                    "doesn't exist,\nor the input codec couldn't be determined or was " +
                    "unable to parse the graph).",
                    "Error during import",
                    JOptionPane.ERROR_MESSAGE);
            return null;
        } else {
            DomGraphConverter conv = new DomGraphConverter(solver, solver.getGraph());
            return conv.toJDomGraph();
        }*/
    }    
    
    /**
     * Starting Ubench, optionally with files to open
     * on command line
     * 
     * @param args command line arfuments
     */
	public static void main(String[] args)  {
        // try to load Utool integration library
        try {
            System.loadLibrary("DomgraphSwig");
            Preferences.setUtoolPresent(true);
        } catch(UnsatisfiedLinkError e) {
            System.err.println("Error while loading libdomgraph library: " + e.getMessage());
            Preferences.setUtoolPresent(false);
        }
        
        
        // parse command-line arguments
        ConvenientGetopt getopt = 
            new ConvenientGetopt("Ubench",
                "java -jar Ubench.jar [options] [filename]",
            	"If Leonardo doesn't run in server mode, specify a filename on the command line"
                + "\nto display the graph.");
        
        getopt.addOption('s', "server", ConvenientGetopt.NO_ARGUMENT,
        		"Run in server mode", null);
        getopt.addOption('p', "port", ConvenientGetopt.REQUIRED_ARGUMENT,
                		"Use port <arg> for server mode", "4300");
        
        getopt.parse(args);
        
        // extract arguments
        boolean serverMode = getopt.hasOption('s');
        int port = Integer.parseInt(getopt.getValue('p'));
        
        graphToLabels = new HashMap<DomGraph, NodeLabels>();
        
        // set up the window
        window = makeWindow();
        
        
        listener = new CommandListener();
        tabbedPane = new JDomTabbedPane(listener);
        menuBar = new JDomGraphMenu(listener);
        window.setJMenuBar(menuBar);
        statusBar = new JDomGraphStatusBar(listener);
     
        
        ttm.registerComponent(slider);
        ttm.registerComponent(statusBar);
        ttm.setInitialDelay(100);
        ttm.setLightWeightPopupEnabled(false);
        
        window.add(slider, BorderLayout.EAST);
        window.add(tabbedPane, BorderLayout.CENTER);
        window.add(statusBar, BorderLayout.SOUTH);

        tabbedPane.copyShortcuts(slider);
        tabbedPane.copyShortcuts(statusBar);
        
        menuBar.setGraphSpecificItemsEnabled(false);
        
        window.setTitle("Underspecification Workbench");
        
        window.doLayout();
        window.pack();
        window.validate();
        
        window.setVisible(true);
        
        
        // load files that were specified on the command line
        for(String file : getopt.getRemaining()) {
        	DomGraph anotherGraph = new DomGraph();
        	NodeLabels labels = new NodeLabels();
            JDomGraph graph = genericLoadGraph(file, anotherGraph, labels);
            if( graph != null ) {
       //     	DomGraphTConverter conv = new DomGraphTConverter(graph);
                JDomGraphTab firstTab = addNewTab(graph, 
                		(new File(file)).getName(),anotherGraph,true, false, labels);
                if( firstTab != null ) {
                    tabbedPane.copyShortcuts(firstTab);
                }
            }
        }
        
        // if the program was started in server mode, start the server thread
        if( serverMode ) {
            new LeonardoServerThread(port).start();
        }        
    }
    
    
    
    /**
     * Create a new JFrame window. The application will be terminated
     * once this window is closed.
     * 
     * @return the new window
     */
    private static JFrame makeWindow() {
        JFrame f = new JFrame("JGraph Test");
		
        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        return f;
    }

    static JDomGraphMenu getMenuBar() {
        return menuBar;
    }

    static JDomGraphStatusBar getStatusBar() {
        return statusBar;
    }

	/**
	 * @return Returns the listener.
	 */
	public static CommandListener getListener() {
		return listener;
	}

	/**
	 * @param listener The listener to set.
	 */
	public static void setListener(CommandListener listener) {
		Main.listener = listener;
	}
	
	private static void registerAllCodecs(CodecManager codecManager) {
        try {
            codecManager.registerCodec(Chain.class);
            codecManager.registerCodec(DomconOzInputCodec.class);
            codecManager.registerCodec(DomconGxlInputCodec.class);
            codecManager.registerCodec(HolesemComsemInputCodec.class);
            codecManager.registerCodec(MrsPrologInputCodec.class);
        
            codecManager.registerCodec(DomconOzOutputCodec.class);
            codecManager.registerCodec(DomconGxlOutputCodec.class);
            
            codecManager.registerCodec(DomconUdrawOutputCodec.class);
            // TBD // codecManager.registerCodec(HolesemComsemOutputCodec.class);
            codecManager.registerCodec(DomconOzPluggingOutputCodec.class);
            codecManager.registerCodec(LkbPluggingOutputCodec.class);
            codecManager.registerCodec(OzTermOutputCodec.class);
            codecManager.registerCodec(PrologTermOutputCodec.class);
        } catch(Exception e) {
            System.err.println("An error occurred trying to register a codec.");
            e.printStackTrace(System.err);
            System.exit(ExitCodes.CODEC_REGISTRATION_ERROR);
        }
    }
	
	public static NodeLabels getLabelsFor(DomGraph gr) {
		return graphToLabels.get(gr);
	}

	
}






/**

test inputs to send over the socket:



        <gxl xmlns:xlink="http://www.w3.org/1999/xlink"><graph id="chain2" edgeids="true" hypergraph="false" edgemode="directed"><!-- OF 1 --><node id="X"><type xlink:href="root" /><attr name="label"><string>f</string></attr><popup id="foo" label="Foo Foo" /><popup id="bar" label="Bar Bar" /><popup id="baz" label="Baz Baz" /></node><node id="X1"><type xlink:href="hole" /><popup id="foo" label="Foo Foo" /><popup id="bar" label="Bar Bar" /><popup id="baz" label="Baz Baz" /></node><edge from="X" to="X1" id="x-x1"><type xlink:href="solid" /></edge><!-- OF 2 --><node id="Y"><type xlink:href="root" /><attr name="label"><string>g</string></attr><popup id="foo" label="Foo Foo" /><popup id="bar" label="Bar Bar" /><popup id="baz" label="Baz Baz" /></node><node id="Y1"><type xlink:href="hole" /><popup id="foo" label="Foo Foo" /><popup id="bar" label="Bar Bar" /><popup id="baz" label="Baz Baz" /></node><edge from="Y" to="Y1" id="Y-Y1"><type xlink:href="solid" /></edge><!-- UF 1 --><node id="Z"><type xlink:href="root" /><attr name="label"><string>a</string></attr><popup id="foo" label="Foo Foo" /><popup id="bar" label="Bar Bar" /><popup id="baz" label="Baz Baz" /></node><!-- dominances --><edge from="X1" to="Z" id="x1-z"><type xlink:href="dominance" /></edge><edge from="Y1" to="Z" id="y1-z"><type xlink:href="dominance" /></edge></graph></gxl>
        <gxl xmlns:xlink="http://www.w3.org/1999/xlink"><graph id="chain2b" edgeids="true" hypergraph="false" edgemode="directed"><!-- OF 1 --><node id="X"><type xlink:href="root" /><attr name="label"><string>g</string></attr><popup id="foo" label="Foo Foo" /><popup id="bar" label="Bar Bar" /><popup id="baz" label="Baz Baz" /></node><node id="X1"><type xlink:href="hole" /><popup id="foo" label="Foo Foo" /><popup id="bar" label="Bar Bar" /><popup id="baz" label="Baz Baz" /></node><edge from="X" to="X1" id="x-x1"><type xlink:href="solid" /></edge><!-- OF 2 --><node id="Y"><type xlink:href="root" /><attr name="label"><string>h</string></attr></node><node id="Y1"><type xlink:href="hole" /><popup id="foo" label="Foo Foo" /><popup id="bar" label="Bar Bar" /><popup id="baz" label="Baz Baz" /></node><edge from="Y" to="Y1" id="Y-Y1"><type xlink:href="solid" /></edge><!-- UF 1 --><node id="Z"><type xlink:href="root" /><attr name="label"><string>b</string></attr><popup id="foo" label="Foo Foo" /><popup id="bar" label="Bar Bar" /><popup id="baz" label="Baz Baz" /></node><!-- dominances --><edge from="X1" to="Z" id="x1-z"><type xlink:href="dominance" /></edge><edge from="Y1" to="Z" id="y1-z"><type xlink:href="dominance" /></edge></graph></gxl>


**/

