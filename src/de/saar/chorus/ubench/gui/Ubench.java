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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.ToolTipManager;

import de.saar.chorus.domgraph.codec.CodecManager;
import de.saar.chorus.domgraph.codec.InputCodec;
import de.saar.chorus.domgraph.codec.MalformedDomgraphException;
import de.saar.chorus.domgraph.codec.ParserException;
import de.saar.chorus.domgraph.codec.domcon.DomconGxlInputCodec;
import de.saar.chorus.domgraph.codec.domcon.DomconGxlOutputCodec;
import de.saar.chorus.domgraph.codec.domcon.DomconOzInputCodec;
import de.saar.chorus.domgraph.codec.domcon.DomconOzOutputCodec;
import de.saar.chorus.domgraph.codec.domcon.DomconUdrawOutputCodec;
import de.saar.chorus.domgraph.codec.glue.GlueInputCodec;
import de.saar.chorus.domgraph.codec.holesem.HolesemComsemInputCodec;
import de.saar.chorus.domgraph.codec.mrs.MrsPrologInputCodec;
import de.saar.chorus.domgraph.codec.plugging.DomconOzPluggingOutputCodec;
import de.saar.chorus.domgraph.codec.plugging.LkbPluggingOutputCodec;
import de.saar.chorus.domgraph.codec.term.OzTermOutputCodec;
import de.saar.chorus.domgraph.codec.term.PrologTermOutputCodec;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.domgraph.utool.ExitCodes;
import de.saar.chorus.ubench.DomGraphTConverter;
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
public class Ubench {
	private static Ubench instance = null;

	// the tabs in their order of insertion
	private ArrayList<JGraphTab> tabs = new ArrayList<JGraphTab>();
    
    // the number of times that tabs with this name were created so far
    private Map<String,Integer> numTabsWithThisName = new HashMap<String,Integer>();

	// the pane containing the tabs
	private JDomTabbedPane tabbedPane;

	// the status bar
	private JDomGraphStatusBar statusBar;

	// the manager for tooltips (needed to shorten the stanard delay)
	private ToolTipManager ttm = ToolTipManager.sharedInstance();

	// the main window
	private JFrame window;

	// the main listener for menus and buttons
	private CommandListener listener;

	// the menu bar
	private JDomGraphMenu menuBar;

	// the codec manager
	private CodecManager codecManager;

    // if true, the next tab addition will resize the main jframe
    // to fit the preferred size of this tab
    private boolean useNextTabToResizeFrame;

    private Ubench() {
//    	 register codecs
		codecManager = new CodecManager();
		registerAllCodecs(codecManager);
		
		
    }
    
	/**
	 * Aligning the slider with the currently shown graph. (if there is one).
	 */
	public void resetSlider() {
		if (getVisibleGraph() != null) {
			getVisibleTab().resetSlider();
		}
	}

	/**
	 * Showing the bar for the solving process.
	 */
	public void showProgressBar() {
		statusBar.showProgressBar();
	}

	/**
	 * @return the main window itself
	 */
	public JFrame getWindow() {
		return window;
	}

	/**
	 * Closing all tabs.
	 */
	public void closeAllTabs() {
		tabbedPane.removeAll();
		refresh();
	}

	/**
	 * Closing Ubench.
	 */
	public void quit() {
		System.exit(0);
	}

	/**
	 * Returning the tab height (considering the possible minimization of the
	 * window).
	 * 
	 * @return the height
	 */
	public double getTabHeight() {
		int index = tabbedPane.getSelectedIndex();
		double windowScale = 1;
		if (window.getState() != JFrame.NORMAL)
			windowScale = (double) window.getHeight()
					/ window.getMaximizedBounds().height;

		if (index > -1) {
			Rectangle tabRect = tabbedPane.getBoundsAt(tabbedPane
					.getSelectedIndex());
			return tabbedPane.getHeight() * windowScale - tabRect.height;
		}

		return (window.getHeight() - 100) * windowScale;
	}

	/**
	 * Returning the tab width (considering the possible minimization of the
	 * window).
	 * 
	 * @return the width
	 */
	public double getTabWidth() {
		double windowScale = 1;
		if (window.getState() != JFrame.NORMAL)
			windowScale = (double) window.getWidth()
					/ window.getMaximizedBounds().width;

		return tabbedPane.getWidth() * windowScale;
	}

	/**
	 * Refreshes the menu, the slider and the status bar.
	 */
	public void refresh() {
		if (getVisibleTab() != null) {
			getVisibleTab().repaintIfNecessary();
			if(getVisibleTab().getClass() == JSolvedFormTab.class ) {
				setSolvingEnabled(false);
			} else {
				if( ((JDomGraphTab) getVisibleTab()).isSolvable()) {
					setSolvingEnabled(true);
				} else {
					setSolvingEnabled(false);
				}
				menuBar.setPlusMinusEnabled(false,false);
			}
		} else {
			menuBar.setGraphSpecificItemsEnabled(false);
		}
		resetSlider();
		statusBar.refresh();
	}

	/**
	 * Closes the currently shown tab (if there is one).
	 */
	public void closeCurrentTab() {
		int index = tabbedPane.getSelectedIndex();

		if (index > -1) {
			tabbedPane.remove(index);
			tabs.remove(index);

		}
		refresh();
	}

	/**
	 * @return the currently shown tab or null if there is none
	 */
	public JGraphTab getVisibleTab() {
		int index = tabbedPane.getSelectedIndex();

		if (index > -1)
			return (tabs.get(tabbedPane.getSelectedIndex()));
		else
			return null;
	}

	/**
	 * 
	 * @return the index of the currently shown tab
	 */
	public int getVisibleTabIndex() {
		return tabbedPane.getSelectedIndex();
	}

	/**
	 * Adding a complete tab to the window
	 * 
	 * @param tab
	 *            the tab to ad
	 * @param showNow
	 *            if set to true, the tab will be displayed at once
	 */
	public void addTab(JGraphTab tab, boolean showNow) {

		addTab(tab, showNow, tabbedPane.getTabCount());

	}

	/**
	 * Adding a complete tab to the window
	 * 
	 * @param tab
	 *            the tab to ad
	 * @param showNow
	 *            if set to true, the tab will be displayed at once
	 */
	public void addTab(JGraphTab tab, boolean showNow, int ind) {

		int index;

		// registering the tab
		tabs.add(tab);

		
		
		if (ind < (tabs.size() - 1)) {
			index = ind;

			tabbedPane.insertTab(tab.getDefaultName(), null, 
					tab, tab.getDefaultName(), index);
		} else {
			tabbedPane.addTab(tab.getDefaultName(), tab);
			index = tabs.size() - 1;
		}

		tabs.add(ind, tab);
		ttm.registerComponent(tab.getGraph());

		// if it's the first tab added, the graph menus get enabled
		if (tabbedPane.getTabCount() == 1) {
			menuBar.setGraphSpecificItemsEnabled(true);
		}

		// if the tab shall be shown, the selected index is
		// the last one
		if (showNow) {
			tabbedPane.setSelectedIndex(index);
		}
		
		if(tabs.size() == 1) {
			tab.setMinimumSize(tab.getGraph().getSize());
			window.pack();
			window.validate();
		}

		tabbedPane.validate();

		// aligning with preferences...

		// fitting?
		if (Preferences.isFitToWindow())
			tab.fitGraph();

		// solving?
		if (Preferences.isAutoCount()) {

			statusBar.showProgressBar();

		}

		refresh();
		
        if( useNextTabToResizeFrame  ) {
            window.pack();
            window.validate();
            
            useNextTabToResizeFrame = false;
        }

	}
	
	public void setSolvingEnabled(boolean b) {
		menuBar.setSolvingEnabled(b);
	}

	/**
	 * Adding a new tab to the window displaying the given
	 * <code>JDomGraph</Code>
	 * 
	 * @param graph the graph to display
	 * @param label the name for the tab
	 * @param paintNow if set to true, the graph is layoutet at once
	 * @param showNow if set to true, the tab will be shown after creating
	 * @return the tab or null if an error occured while setting up the tab
	 */
	public JDomGraphTab addNewTab(JDomGraph graph, String label,
			DomGraph origin, boolean paintNow, boolean showNow,
			NodeLabels labels) {
        
        String normalisedLabel = normaliseTabLabel(label);

		// the new tab
		JDomGraphTab tab = new JDomGraphTab(graph, origin, normalisedLabel, paintNow,
				listener, labels);
		if (tab.getGraph() != null) {

			// tab sucessfully created
			addTab(tab, showNow);
			return tab;
		} else {
			// something went wrong (the tab contains no graph)
			return null;
		}

	}

	private String normaliseTabLabel(String label) {
        String stripSlashes = new File(label).getName();
        
        if( numTabsWithThisName.containsKey(stripSlashes)) {
            int next = numTabsWithThisName.get(stripSlashes) + 1;
            String ret = stripSlashes + "/" + next;
            numTabsWithThisName.put(stripSlashes, next);
            
            return ret;
        } else {
            numTabsWithThisName.put(stripSlashes,1);
            return stripSlashes;
        }
    }

    public boolean addNewTab(String label, DomGraph graph, NodeLabels labels) {
		
		DomGraphTConverter conv = new DomGraphTConverter(graph, labels);
		JDomGraph jDomGraph = conv.getJDomGraph();
		if(jDomGraph == null)
			return false;
		
		JDomGraphTab tab = new JDomGraphTab(jDomGraph, graph, normaliseTabLabel(label),
                true, listener, labels);
		addTab(tab, true);
		return true;
	}
	
	/**
	 * Adding a new tab to the window displaying the given
	 * <code>JDomGraph</Code>, given the index indicating
	 * where to insert it in the tab.
	 * 
	 * @param graph the graph to display
	 * @param label the name of the tab
	 * @param paintNow if set to true the graph is layoutet at once
	 * @param showNow if set to true the graph is shown immidiately
	 * @param index indicating on which place of the tab the new tab shall be inserted
	 * @return the new tab or null if anything fails
	 */
	public JDomGraphTab addNewTab(JDomGraph graph, String label,
			DomGraph origin, boolean paintNow, boolean showNow, int index,
			NodeLabels labels) {

		// the new tab
		JDomGraphTab tab = new JDomGraphTab(graph, origin, normaliseTabLabel(label),
                paintNow, listener, labels);
		if (tab.getGraph() != null) {

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
	private JDomGraph getVisibleGraph() {
		int index = tabbedPane.getSelectedIndex();

		if (index > -1)
			return (tabs.get(tabbedPane.getSelectedIndex()).getGraph());
		else
			return null;
	}

	/**
	 * 
	 * @param filename
	 * @return
	 */
	public JDomGraph genericLoadGraph(String filename, DomGraph graph,
			NodeLabels nl) {

		return importGraph(filename, graph, nl);

	}

	/**
	 * 
	 * @deprecated
	 * @param fileName
	 * @return
	 * 
	 * public static JDomGraph loadGraph(String fileName) { JDomGraph
	 * loadedGraph = new JDomGraph(); try { File gxl = new File(fileName);
	 * Reader input = new FileReader(gxl); DomGraphGXLCodec.decode(input,
	 * loadedGraph); for( Fragment frag : loadedGraph.getFragments() ) {
	 * System.out.println(frag); } } catch (IOException e ) {
	 * System.err.println("File can't be found"); } catch (Exception e) {
	 * System.err.println("Error while parsing " + fileName + ":");
	 * e.printStackTrace(System.err); System.exit(1); }
	 * 
	 * return loadedGraph; }
	 */

	/**
	 * 
	 * @param filename
	 * @return
	 */
	public JDomGraph importGraph(String filename, DomGraph graph,
			NodeLabels nl) {
		InputCodec inputCodec = codecManager.getInputCodecForFilename(filename,
				null);
		if(inputCodec != null ) {
		try {
			inputCodec.decode(new FileReader(filename), graph, nl);
			
		} catch (IOException e) {
			JOptionPane
					.showMessageDialog(
							window,
							"An error occurred while loading this graph\nThe file to load"
									+ "doesn't exist or cannot be opened",
							"Error during import", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
			return null;
		} catch (ParserException pe) {
			JOptionPane
			.showMessageDialog(
					window,
					"An error occurred while loading this graph\nThe input file contains"
					+ "syntax errors",
					"Error during import", JOptionPane.ERROR_MESSAGE);

	return null;
		} catch (MalformedDomgraphException me) {
			JOptionPane
			.showMessageDialog(
					window,
					"An error occurred while loading this graph\n" + 
					"A semantic error occured; the input graph cannot\n" + 
					"be converted into a dominance graph",
					"Error during import", JOptionPane.ERROR_MESSAGE);
	
			return null;
		}
		} else {
			JOptionPane
			.showMessageDialog(
					window,
					"An error occurred while loading this graph\n" + 
					"There is no input codec for importing this file.",
					"Error during import", JOptionPane.ERROR_MESSAGE);
	
			return null;
		}
		DomGraphTConverter conv = new DomGraphTConverter(graph, nl);
		return conv.getJDomGraph();
	}

	/**
	 * Starting Ubench, optionally with files to open on command line
	 * 
	 * @param args
	 *            command line arfuments
	 */
	public static void main(String[] args) {

	
		

		// parse command-line arguments
		ConvenientGetopt getopt = new ConvenientGetopt("Ubench",
				"java -jar Ubench.jar [options] [filename]",
				"If Ubench doesn't run in server mode, specify a filename on the command line"
						+ "\nto display the graph.");

		getopt.parse(args);

		// extract arguments
		        
		// load files that were specified on the command line
		for (String file : getopt.getRemaining()) {
			DomGraph anotherGraph = new DomGraph();
			NodeLabels labels = new NodeLabels();
			JDomGraph graph = getInstance().genericLoadGraph(file, anotherGraph, labels);
			if (graph != null) {
				// DomGraphTConverter conv = new DomGraphTConverter(graph);
				JDomGraphTab firstTab = getInstance().addNewTab(graph, (new File(file))
						.getName(), anotherGraph, true, false, labels);
				
			}
		}
		
	}

	/**
	 * Create a new JFrame window. The application will be terminated once this
	 * window is closed.
	 * 
	 * @return the new window
	 */
	private JFrame makeWindow() {
		JFrame f = new JFrame("JGraph Test");

		f.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		return f;
	}

	JDomGraphMenu getMenuBar() {
		return menuBar;
	}

	JDomGraphStatusBar getStatusBar() {
		return statusBar;
	}

	/**
	 * @return Returns the listener.
	 */
	public CommandListener getListener() {
		return listener;
	}

	/**
	 * @param listener
	 *            The listener to set.
	 */
	public void setListener(CommandListener listener) {
		this.listener = listener;
	}

	private void registerAllCodecs(CodecManager codecManager) {
		try {
			codecManager.registerCodec(GlueInputCodec.class);
			codecManager.registerCodec(HolesemComsemInputCodec.class);
			codecManager.registerCodec(MrsPrologInputCodec.class);
			codecManager.registerCodec(DomconOzInputCodec.class);
			codecManager.registerCodec(DomconGxlInputCodec.class);

			codecManager.registerCodec(DomconOzOutputCodec.class);
			codecManager.registerCodec(DomconGxlOutputCodec.class);
			codecManager.registerCodec(DomconUdrawOutputCodec.class);
			// TBD //
			codecManager.registerCodec(DomconOzPluggingOutputCodec.class);
			codecManager.registerCodec(LkbPluggingOutputCodec.class);
			codecManager.registerCodec(OzTermOutputCodec.class);
			codecManager.registerCodec(PrologTermOutputCodec.class);
		} catch (Exception e) {
			System.err.println("An error occurred trying to register a codec.");
			e.printStackTrace(System.err);
			System.exit(ExitCodes.CODEC_REGISTRATION_ERROR);
		}
	}

	public CodecManager getCodecManager() {
		return codecManager;
	}
	public void initialise() {
//		 set up the window
		window = makeWindow();

		listener = new CommandListener();
		tabbedPane = new JDomTabbedPane(listener);
		menuBar = new JDomGraphMenu(listener);
		window.setJMenuBar(menuBar);
		statusBar = new JDomGraphStatusBar(listener);

		// ttm.registerComponent(slider);
		ttm.registerComponent(statusBar);
		ttm.setInitialDelay(100);
		ttm.setLightWeightPopupEnabled(false);

		window.add(tabbedPane, BorderLayout.CENTER);
		window.add(statusBar, BorderLayout.SOUTH);

		// tabbedPane.copyShortcuts(slider);
		tabbedPane.copyShortcuts(statusBar);

		menuBar.setGraphSpecificItemsEnabled(false);

		window.setTitle("Underspecification Workbench");

		window.doLayout();
		window.setVisible(true);

        useNextTabToResizeFrame = true;
        window.pack();
		window.validate();
	}
	
	public static Ubench getInstance() {
		if( instance == null ) {
			instance = new Ubench();
			instance.initialise();
		}
		
		return instance;
	}

}

/**
 * 
 * test inputs to send over the socket:
 * 
 * 
 * 
 * <gxl xmlns:xlink="http://www.w3.org/1999/xlink"><graph id="chain2"
 * edgeids="true" hypergraph="false" edgemode="directed"><!-- OF 1 --><node
 * id="X"><type xlink:href="root" /><attr name="label"><string>f</string></attr><popup
 * id="foo" label="Foo Foo" /><popup id="bar" label="Bar Bar" /><popup
 * id="baz" label="Baz Baz" /></node><node id="X1"><type xlink:href="hole" /><popup
 * id="foo" label="Foo Foo" /><popup id="bar" label="Bar Bar" /><popup
 * id="baz" label="Baz Baz" /></node><edge from="X" to="X1" id="x-x1"><type
 * xlink:href="solid" /></edge><!-- OF 2 --><node id="Y"><type
 * xlink:href="root" /><attr name="label"><string>g</string></attr><popup
 * id="foo" label="Foo Foo" /><popup id="bar" label="Bar Bar" /><popup
 * id="baz" label="Baz Baz" /></node><node id="Y1"><type xlink:href="hole" /><popup
 * id="foo" label="Foo Foo" /><popup id="bar" label="Bar Bar" /><popup
 * id="baz" label="Baz Baz" /></node><edge from="Y" to="Y1" id="Y-Y1"><type
 * xlink:href="solid" /></edge><!-- UF 1 --><node id="Z"><type
 * xlink:href="root" /><attr name="label"><string>a</string></attr><popup
 * id="foo" label="Foo Foo" /><popup id="bar" label="Bar Bar" /><popup
 * id="baz" label="Baz Baz" /></node><!-- dominances --><edge from="X1"
 * to="Z" id="x1-z"><type xlink:href="dominance" /></edge><edge from="Y1"
 * to="Z" id="y1-z"><type xlink:href="dominance" /></edge></graph></gxl>
 * <gxl xmlns:xlink="http://www.w3.org/1999/xlink"><graph id="chain2b"
 * edgeids="true" hypergraph="false" edgemode="directed"><!-- OF 1 --><node
 * id="X"><type xlink:href="root" /><attr name="label"><string>g</string></attr><popup
 * id="foo" label="Foo Foo" /><popup id="bar" label="Bar Bar" /><popup
 * id="baz" label="Baz Baz" /></node><node id="X1"><type xlink:href="hole" /><popup
 * id="foo" label="Foo Foo" /><popup id="bar" label="Bar Bar" /><popup
 * id="baz" label="Baz Baz" /></node><edge from="X" to="X1" id="x-x1"><type
 * xlink:href="solid" /></edge><!-- OF 2 --><node id="Y"><type
 * xlink:href="root" /><attr name="label"><string>h</string></attr></node><node
 * id="Y1"><type xlink:href="hole" /><popup id="foo" label="Foo Foo" /><popup
 * id="bar" label="Bar Bar" /><popup id="baz" label="Baz Baz" /></node><edge
 * from="Y" to="Y1" id="Y-Y1"><type xlink:href="solid" /></edge><!-- UF 1 --><node
 * id="Z"><type xlink:href="root" /><attr name="label"><string>b</string></attr><popup
 * id="foo" label="Foo Foo" /><popup id="bar" label="Bar Bar" /><popup
 * id="baz" label="Baz Baz" /></node><!-- dominances --><edge from="X1"
 * to="Z" id="x1-z"><type xlink:href="dominance" /></edge><edge from="Y1"
 * to="Z" id="y1-z"><type xlink:href="dominance" /></edge></graph></gxl>
 * 
 * 
 */

