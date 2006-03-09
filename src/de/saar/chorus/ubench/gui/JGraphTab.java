package de.saar.chorus.ubench.gui;

import javax.swing.JPanel;

import de.saar.chorus.domgraph.chart.SolvedFormIterator;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.ubench.JDomGraph;

public abstract class JGraphTab extends JPanel  {
//	the grapb is initialized empty
	private JDomGraph graph;
	
	private DomGraph domGraph;
	
	
	private NodeLabels nodeLabels;
//	managing solved forms
	long 	solvedForms, // number of solved forms 
	currentForm; // current solved form
	
	// name of graph and the tab (when inserted to a
	// tabed pane)
	private String defaultName, graphName;
	
	// solvedFormIterator of the graph
	private SolvedFormIterator solvedFormIterator;
	
	
	// layout preferences of the current graph
	private Preferences recentLayout;
	private JPanel statusBar;
	
	private String barCode;
	
	private CommandListener listener;
	
	public abstract JDomGraph getGraph() ;
	public abstract void repaintIfNecessary();
	public abstract void resetLayout();
	public abstract JGraphTab clone();
	
	public  abstract long getSolvedForms() ;
	public abstract NodeLabels getNodeLabels() ; 
	
	public void fitGraph() {
		
		JDomGraph graph = getGraph();
		
//		computing tab height (and add a little space...)
		double myHeight = Main.getTabHeight()*0.9;
		double myWidth = Main.getTabWidth()*0.9;
		
		// checking the current (!) graph height
		double graphHeight = graph.getHeight()*(1/graph.getScale());
		double graphWidth = graph.getWidth()*(1/graph.getScale());
		
		// computing the scale
		double scale = Math.min(1 , Math.min((double) myHeight/graphHeight, (double) myWidth/graphWidth));
		
		graph.setScale(scale);
		
		Main.resetSlider();
	}
	/**
	 * @return Returns the barCode.
	 */
	public abstract String getBarCode() ;
	
	public abstract void setGraphScale(double scale);
	/**
	 * @param barCode The barCode to set.
	 */
	public abstract void setBarCode(String barCode);
	/**
	 * @return Returns the currentForm.
	 */
	public abstract long getCurrentForm() ;
	/**
	 * @param currentForm The currentForm to set.
	 */
	public abstract void setCurrentForm(long currentForm) ;
	/**
	 * @return Returns the domGraph.
	 */
	public abstract DomGraph getDomGraph();
	/**
	 * @param domGraph The domGraph to set.
	 */
	public abstract void setDomGraph(DomGraph domGraph) ;
	/**
	 * @return Returns the graphName.
	 */
	public abstract String getGraphName() ;
	/**
	 * @param graphName The graphName to set.
	 */
	public abstract void setGraphName(String graphName) ;
	/**
	 * @return Returns the listener.
	 */
	public abstract CommandListener getListener() ;
	/**
	 * @param listener The listener to set.
	 */
	public  abstract void setListener(CommandListener listener);
	/**
	 * @return Returns the recentLayout.
	 */
	public abstract Preferences getRecentLayout() ;
	/**
	 * @param recentLayout The recentLayout to set.
	 */
	public abstract void setRecentLayout(Preferences recentLayout) ;
	/**
	 * @return Returns the solvedFormIterator.
	 */
	public abstract SolvedFormIterator getSolvedFormIterator() ;
	/**
	 * @param solvedFormIterator The solvedFormIterator to set.
	 */
	public abstract void setSolvedFormIterator(SolvedFormIterator solvedFormIterator) ;
	/**
	 * @return Returns the statusBar.
	 */
	public abstract JPanel getStatusBar() ;
	/**
	 * @param statusBar The statusBar to set.
	 */
	public  abstract void setStatusBar(JPanel statusBar);
	/**
	 * @param defaultName The defaultName to set.
	 */
	public abstract void setDefaultName(String defaultName);
	/**
	 * @param graph The graph to set.
	 */
	public abstract void setGraph(JDomGraph graph) ;
	/**
	 * @param nodeLabels The nodeLabels to set.
	 */
	public abstract void setNodeLabels(NodeLabels nodeLabels) ;
	/**
	 * @param solvedForms The solvedForms to set.
	 */
	public abstract void setSolvedForms(long solvedForms);
	
	/**
     * @return number of the graph's nodes
     */
    public abstract int numGraphNodes() ;
    
    /**
     * @return a clone of the displayed graph
     */
    public abstract JDomGraph getCloneOfGraph() ;
	/**
	 * @return Returns the defaultName.
	 */
	public abstract String getDefaultName() ;
}
