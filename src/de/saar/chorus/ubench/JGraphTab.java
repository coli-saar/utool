package de.saar.chorus.ubench;

import java.awt.Color;

import javax.swing.JFrame;
import javax.swing.JPanel;

import de.saar.chorus.domgraph.chart.SolvedFormIterator;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.domgraph.layout.LayoutOptions.LabelType;
import de.saar.chorus.jgraph.JScrollableJGraph;
import de.saar.chorus.ubench.Preferences.LayoutType;
import de.saar.chorus.ubench.chartviewer.ChartViewer;
import de.saar.chorus.ubench.jdomgraph.JDomGraph;


/**
 * A <code>JPanel</code> displaying a <code>JGraph</code>,
 * either a dominance graph or a solved form, and
 * providing several informations on the graph needed by other
 * GUI-classes.
 * Everything what can be set up independent from the kind of
 * graph to display (dominance graph or solved form) is 
 * initialised here.
 * 
 * @see JDomGraphTab
 * @see JSolvedFormTab
 * 
 * @author Alexander Koller
 * @author Michaela Regneri
 *
 */

abstract class JGraphTab extends JScrollableJGraph {
	//	the grapb is initialized empty
	protected JDomGraph graph;
	protected LayoutType layout;
	protected boolean empty;
	
	protected DomGraph domGraph;

	protected NodeLabels nodeLabels;

	//	managing solved forms
	protected long solvedForms; // number of solved forms 

	// name of graph and the tab (when inserted to a
	// tabed pane)
	protected String defaultName, graphName;

	// solvedFormIterator of the graph
	protected SolvedFormIterator solvedFormIterator;

	// layout preferences of the current graph
	protected Preferences recentLayout;

	// the status bar
	protected JPanel statusBar;

	// the key of the status bar within the 
	// window startus bar's card layout
	protected String barCode;

	// the listener
	protected CommandListener listener;
	
	protected ChartViewer cv;
	
	protected LabelType labelType;

	// the tabs have to define their clone-methods themselves
	public abstract JGraphTab clone();

	abstract void drawGraph() throws Exception;
	
	
	
	
	/**
	 * A new <code>JGraphTab</code>
	 * 
	 * @param jdg the graph to display as <code>JDomGraph</code>
	 * @param dg the underlying <code>DomGraph</code>
	 * @param name the tab's name
	 * @param lis the <code>ActionListener</code> for this tab
	 * @param lab storage for the nodelables of the graph to display
	 */
	JGraphTab(JDomGraph jdg, DomGraph dg, String name,
			CommandListener lis, NodeLabels lab) {
		super(jdg);
		empty = false;
		graph = jdg;
		domGraph = dg;
		labelType = Preferences.getInstance().getLabelType();
		graph.setLabeltype(labelType);
		defaultName = name;
		listener = lis;
		nodeLabels = lab;
		setBackground(Color.WHITE);
	}
	
	
	/*** Getters and setters equal for both kinds of tabs***/
	
	/**
	 * @return graph the <code>JDomGraph</code>
	 */
	 JDomGraph getGraph() {
		return graph;
	}

	/**
	 * 
	 * @return solvedForms the number of solved forms
	 */
	public long getSolvedForms() {
		return solvedForms;
	}

	/**
	 * 
	 * @return nodeLabels the <code>NodeLabels</code> object
	 */
	NodeLabels getNodeLabels() {
		return nodeLabels;
	}

	/**
	 * Scales the graph so as to fit in the 
	 * recent window.
	 */
	void fitGraph() {

		JDomGraph graph = getGraph();

		//		computing tab height (and add a little space...)
		double myHeight = Ubench.getInstance().getTabHeight() * 0.9;
		double myWidth = Ubench.getInstance().getTabWidth() * 0.9;

		// checking the current (!) graph height
		double graphHeight = graph.getHeight() * (1 / graph.getScale());
		double graphWidth = graph.getWidth() * (1 / graph.getScale());

		// computing the scale
		double scale = Math.min(1, Math.min((double) myHeight / graphHeight,
				(double) myWidth / graphWidth));

		graph.setScale(scale);

		Ubench.getInstance().resetSlider();
	}

	/**
	 * @return Returns the barCode.
	 */

    String getBarCode() {
		return barCode;
	}

	/**
	 * Scales the graph with the given factor.
	 * @param scale the scale factor
	 */
	void setGraphScale(double scale) {
		graph.setScale(scale);
	}

	/**
	 * @return the scale (percentage of the original one)
	 */
	double getGraphScale() {
		return graph.getScale();
	}

	/**
	 * @return Returns the defaultName.
	 */
	String getDefaultName() {
		return defaultName;
	}

	/**
	 * @param defaultName The defaultName to set.
	 */
	void setDefaultName(String defaultName) {
		this.defaultName = defaultName;
	}

	/**
	 * @return Returns the domGraph.
	 */
	DomGraph getDomGraph() {
		return domGraph;
	}

	/**
	 * @param domGraph The domGraph to set.
	 */
	void setDomGraph(DomGraph domGraph) {
		this.domGraph = domGraph;
	}

	/**
	 * @return Returns the graphName.
	 */
	String getGraphName() {
		return graphName;
	}

	/**
	 * @param graphName The graphName to set.
	 */
	void setGraphName(String graphName) {
		this.graphName = graphName;
	}

	/**
	 * @return Returns the listener.
	 */
	CommandListener getListener() {
		return listener;
	}

	/**
	 * @param listener The listener to set.
	 */
	void setListener(CommandListener listener) {
		this.listener = listener;
	}

	/**
	 * @return Returns the recentLayout.
	 */
	Preferences getRecentLayout() {
		return recentLayout;
	}

	/**
	 * @return Returns the solvedFormIterator.
	 */
	SolvedFormIterator getSolvedFormIterator() {
		return solvedFormIterator;
	}

	/**
	 * @param solvedFormIterator The solvedFormIterator to set.
	 */
	void setSolvedFormIterator(SolvedFormIterator solvedFormIterator) {
		this.solvedFormIterator = solvedFormIterator;
	}

	/**
	 * @return Returns the statusBar.
	 */
	JPanel getStatusBar() {
		return statusBar;
	}

	/**
	 * @param statusBar The statusBar to set.
	 */
	void setStatusBar(JPanel statusBar) {
		this.statusBar = statusBar;
	}

	/**
	 * @param barCode The barCode to set.
	 */
	void setBarCode(String barCode) {
		this.barCode = barCode;
	}

	/**
	 * @param graph The graph to set.
	 */
	void setGraph(JDomGraph graph) {
		this.graph = graph;
	}

	/**
	 * @param nodeLabels The nodeLabels to set.
	 */
	void setNodeLabels(NodeLabels nodeLabels) {
		this.nodeLabels = nodeLabels;
	}

	/**
	 * @param solvedForms The solvedForms to set.
	 */
	void setSolvedForms(long solvedForms) {
		this.solvedForms = solvedForms;
	}

	/**
	 * @param recentLayout The recentLayout to set.
	 */
	void setRecentLayout(Preferences recentLayout) {
		this.recentLayout = recentLayout;
	}

	/**
	 * Repaints the graph if its layout is not consistent
	 * with the recent layout preferences.
	 */
	boolean repaintIfNecessary() throws Exception {
		if ((recentLayout == null)
				|| Preferences.mustUpdateLayout(recentLayout)) {
			
			graph.setLabeltype(Preferences.getInstance().getLabelType());
			graph.setLayoutType(Preferences.getInstance().getLayoutType());
			updateRecentLayout();
			return true;
		}
		return false;
	}
	
	LabelType getLabelType() {
		return graph.getLabeltype();
	}
	
	void setLabelType(LabelType lt) throws Exception {
		graph.setLabeltype(lt);
		drawGraph();
	}

	/**
	 * Updates the graph's layout preferences
	 * by adopting the recent global layout 
	 * preferences.
	 */
	void updateRecentLayout() {
		if (recentLayout == null) {
			recentLayout = Preferences.getInstance().clone();
		} else {
			Preferences.getInstance().copyTo(recentLayout);
		}
		recentLayout.setLayoutType(graph.getLayoutType());
	}

	
	
	void setLayoutType(LayoutType lt) throws Exception {
		layout = lt;
		graph.setLayouttype(lt);
		
		drawGraph();
	}
	
	LayoutType getLayoutType() {
		return layout;
	}
	
	/*** methods for hiding JDomGraph from classes in leonardo.gui ***/

	/**
	 * @return number of the graph's nodes
	 */
	int numGraphNodes() {
		return graph.getNodes().size();
	}

	/**
	 * @return a clone of the displayed graph
	 */
	JDomGraph getCloneOfGraph() {
		return graph.clone();
	}

	/**
	 * Resets the layout to its initial 
	 * version.
	 */
	void resetLayout() throws Exception {
		graph.setScale(1);
		graph.clear();
		drawGraph();
	}

	/**
	 * Overwrites the <code>finalize</code>-Method of 
	 * <code>Object</code>.
	 * Removes the related status bar of this tab from the
	 * <code>JDomGraphStatusBar</code> of <code>Ubench</code>.
	 */
	protected void finalize() throws Throwable {
		try {
			Ubench.getInstance().getStatusBar().removeBar(statusBar);
		} finally {
			super.finalize();
		}
	}

	
	abstract void displayChart(); 
	
	/**
	 * 
	 * @return true if the graph to display cannot be drawn
	 */
	boolean isEmpty() {
		return empty;
	}


	/**
	 * @return Returns the hasActiveChartViewer.
	 */
	boolean hasVisibleChartViewer() {
		return (cv != null  && cv.isVisible() == true);
	}

	JFrame getChartViewer() {
		return cv;
	}
	
	void enableGlobalEQS(boolean en) {
		if(cv != null ) {
			cv.setEQSLoaded(true);
		}
	}
  
	void focusChart() {
		if(cv != null) 
			cv.toFront();
	}
	

}
