package de.saar.chorus.ubench.gui.chartviewer;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;

import org.jgraph.JGraph;
import org.jgraph.graph.AttributeMap;
import org.jgraph.graph.CellView;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.GraphConstants;
import org.jgraph.layout.JGraphLayoutAlgorithm;

import de.saar.chorus.ubench.JDomGraph;

public class JDomGraphDummyLayout extends JGraphLayoutAlgorithm {

	private JDomGraph graph;
	
	JDomGraphDummyLayout(JDomGraph jdg) {
		graph = jdg;
	}
	
	/**
	 * places a node at a given position and remembers
	 * the information in a given Attribute Map.
	 * @param node, the node to place
	 * @param x the x-value of the upper left corner
	 * @param y the y-value of the upper left corner
	 * @param viewMap hte viewMap to save the position in
	 */
	private void placeNodeAt(DefaultGraphCell node, int x, int y, 
			Map<DefaultGraphCell,AttributeMap> viewMap) {
		
		CellView view = graph.getGraphLayoutCache().getMapping(node, false);
		Rectangle2D rect = (Rectangle2D) view.getBounds().clone();
		Rectangle bounds =  new Rectangle((int) rect.getX(),
				(int) rect.getY(), (int) rect.getWidth(), (int) rect.getHeight());
		
		bounds.x = x;
		bounds.y = y;
		
		AttributeMap map = graph.getModel().createAttributes();
		GraphConstants.setBounds(map, (Rectangle2D) bounds.clone());
		
		viewMap.put(node, map);
	}
	
	/**
	 * places the nodes in the graph model.
	 * Not meaningful without having computed
	 * the fragment graph as well as the relative
	 * x- and y-positions.
	 */
	private void placeNodes() {
		//the view map to save all the node's positions.
		Map<DefaultGraphCell,AttributeMap> viewMap = new 
		HashMap<DefaultGraphCell,AttributeMap>();
		
		
		//place every node on its position
		//and remembering that in the viewMap.
		for(DefaultGraphCell node : graph.getNodes() ) {
			int x = (int) graph.getCellBounds(node).getMinX() + 1;
			int y = (int) graph.getCellBounds(node).getMinY() + 1 ;
			
			placeNodeAt(node, x, y, viewMap);
		}
		//updating the graph.
		graph.getGraphLayoutCache().edit(viewMap, null, null, null);
		
		for(DefaultGraphCell node : graph.getNodes() ) {
			int x = (int) graph.getCellBounds(node).getMinX() - 1;
			int y = (int) graph.getCellBounds(node).getMinY() - 1 ;
			
			placeNodeAt(node, x, y, viewMap);
		}
		//updating the graph.
		graph.getGraphLayoutCache().edit(viewMap, null, null, null);
	}
	
	
	public void run(JGraph jgraph, Object[] cells, int step) {
		
		placeNodes();
	}

}
