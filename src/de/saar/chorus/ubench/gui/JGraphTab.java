package de.saar.chorus.ubench.gui;

import java.awt.Color;

import javax.swing.JPanel;

import de.saar.chorus.domgraph.chart.SolvedFormIterator;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.jgraph.JScrollableJGraph;
import de.saar.chorus.ubench.JDomGraph;

public abstract class JGraphTab extends JScrollableJGraph {
	//	the grapb is initialized empty
	protected JDomGraph graph;

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

	// the tabs have to define their clone-methods themselves
	public abstract JGraphTab clone();

	public JGraphTab(JDomGraph jdg, DomGraph dg, String name,
			CommandListener lis, NodeLabels lab) {
		super(jdg);
		graph = jdg;
		domGraph = dg;
		defaultName = name;
		listener = lis;
		nodeLabels = lab;
		setBackground(Color.WHITE);
	}
	
	
	/*** Getters and setters equal for both kinds of tabs***/
	
	/**
	 * @return graph the <code>JDomGraph</code>
	 */
	public JDomGraph getGraph() {
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
	public NodeLabels getNodeLabels() {
		return nodeLabels;
	}

	/**
	 * Scales the graph so as to fit in the 
	 * recent window.
	 */
	public void fitGraph() {

		JDomGraph graph = getGraph();

		//		computing tab height (and add a little space...)
		double myHeight = Main.getTabHeight() * 0.9;
		double myWidth = Main.getTabWidth() * 0.9;

		// checking the current (!) graph height
		double graphHeight = graph.getHeight() * (1 / graph.getScale());
		double graphWidth = graph.getWidth() * (1 / graph.getScale());

		// computing the scale
		double scale = Math.min(1, Math.min((double) myHeight / graphHeight,
				(double) myWidth / graphWidth));

		graph.setScale(scale);

		Main.resetSlider();
	}

	/**
	 * @return Returns the barCode.
	 */
	public String getBarCode() {
		return barCode;
	}

	/**
	 * Scales the graph with the given factor.
	 * @param scale the scale factor
	 */
	public void setGraphScale(double scale) {
		graph.setScale(scale);
	}

	/**
	 * @return the scale (percentage of the original one)
	 */
	public double getGraphScale() {
		return graph.getScale();
	}

	/**
	 * @return Returns the defaultName.
	 */
	public String getDefaultName() {
		return defaultName;
	}

	/**
	 * @param defaultName The defaultName to set.
	 */
	public void setDefaultName(String defaultName) {
		this.defaultName = defaultName;
	}

	/**
	 * @return Returns the domGraph.
	 */
	public DomGraph getDomGraph() {
		return domGraph;
	}

	/**
	 * @param domGraph The domGraph to set.
	 */
	public void setDomGraph(DomGraph domGraph) {
		this.domGraph = domGraph;
	}

	/**
	 * @return Returns the graphName.
	 */
	public String getGraphName() {
		return graphName;
	}

	/**
	 * @param graphName The graphName to set.
	 */
	public void setGraphName(String graphName) {
		this.graphName = graphName;
	}

	/**
	 * @return Returns the listener.
	 */
	public CommandListener getListener() {
		return listener;
	}

	/**
	 * @param listener The listener to set.
	 */
	public void setListener(CommandListener listener) {
		this.listener = listener;
	}

	/**
	 * @return Returns the recentLayout.
	 */
	public Preferences getRecentLayout() {
		return recentLayout;
	}

	/**
	 * @return Returns the solvedFormIterator.
	 */
	public SolvedFormIterator getSolvedFormIterator() {
		return solvedFormIterator;
	}

	/**
	 * @param solvedFormIterator The solvedFormIterator to set.
	 */
	public void setSolvedFormIterator(SolvedFormIterator solvedFormIterator) {
		this.solvedFormIterator = solvedFormIterator;
	}

	/**
	 * @return Returns the statusBar.
	 */
	public JPanel getStatusBar() {
		return statusBar;
	}

	/**
	 * @param statusBar The statusBar to set.
	 */
	public void setStatusBar(JPanel statusBar) {
		this.statusBar = statusBar;
	}

	/**
	 * @param barCode The barCode to set.
	 */
	public void setBarCode(String barCode) {
		this.barCode = barCode;
	}

	/**
	 * @param graph The graph to set.
	 */
	public void setGraph(JDomGraph graph) {
		this.graph = graph;
	}

	/**
	 * @param nodeLabels The nodeLabels to set.
	 */
	public void setNodeLabels(NodeLabels nodeLabels) {
		this.nodeLabels = nodeLabels;
	}

	/**
	 * @param solvedForms The solvedForms to set.
	 */
	public void setSolvedForms(long solvedForms) {
		this.solvedForms = solvedForms;
	}

	/**
	 * @param recentLayout The recentLayout to set.
	 */
	public void setRecentLayout(Preferences recentLayout) {
		this.recentLayout = recentLayout;
	}

	/**
	 * Repaints the graph if its layout is not consistent
	 * with the recent layout preferences.
	 */
	public void repaintIfNecessary() {
		if ((recentLayout == null)
				|| Preferences.mustUpdateLayout(recentLayout)) {
			graph.setShowLabels(Preferences.getInstance().isShowLabels());
			graph.computeLayout();

			graph.adjustNodeWidths();
			updateRecentLayout();
		}
	}

	/**
	 * Updates the graph's layout preferences
	 * by adopting the recent global layout 
	 * preferences.
	 */
	public void updateRecentLayout() {
		if (recentLayout == null) {
			recentLayout = Preferences.getInstance().clone();
		} else {
			Preferences.getInstance().copyTo(recentLayout);
		}
	}

	/*** methods for hiding JDomGraph from classes in leonardo.gui ***/

	/**
	 * @return number of the graph's nodes
	 */
	public int numGraphNodes() {
		return graph.getNodes().size();
	}

	/**
	 * @return a clone of the displayed graph
	 */
	public JDomGraph getCloneOfGraph() {
		return graph.clone();
	}

	/**
	 * Resets the layout to its initial 
	 * version.
	 */
	public void resetLayout() {
		graph.setScale(1);
		graph.computeLayout();
		graph.adjustNodeWidths();
	}

	/**
	 * Overwrites the <code>finalize</code>-Method of 
	 * <code>Object</code>.
	 * Removes the related status bar of this tab from the
	 * <code>JDomGraphStatusBar</code> of <code>Main</code>.
	 */
	protected void finalize() throws Throwable {
		try {
			Main.getStatusBar().removeBar(statusBar);
		} finally {
			super.finalize();
		}
	}

}
