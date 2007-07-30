package de.saar.chorus.ubench.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jgraph.graph.AttributeMap;
import org.jgraph.graph.BasicMarqueeHandler;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.GraphConstants;
import org.jgraph.util.JGraphUtilities;

import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.EdgeData;
import de.saar.chorus.domgraph.graph.EdgeType;
import de.saar.chorus.domgraph.graph.NodeData;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.domgraph.graph.NodeType;
import de.saar.chorus.ubench.JDomGraph;
import de.saar.chorus.ubench.chartviewer.JDomGraphDummyLayout;

public class DomgraphMarqueeHandler extends BasicMarqueeHandler {
	private JDomGraph jgraph;
	
	private DomGraph graph;
	private NodeLabels labels;
	private JDomGraphTab tab;
		
	private Point2D start, current;
	private DefaultGraphCell sourceNode, targetNode;

	private int numHoles;
	private boolean addingDominanceEdge;
	
	// for gensym
	private static final String gensymPrefix = "uN";
	private int gensymNext = 1;

	private static final int MIN_HEIGHT_FOR_HOLES = 50;
	private static final int NODE_XSEP = 20;
	private static final int NODE_RADIUS = 10;

	public DomgraphMarqueeHandler(JDomGraphTab tab, DomGraph graph, NodeLabels labels) {
		super();
		this.tab = tab;
		this.jgraph = tab.getGraph();
		this.graph = graph;
		this.labels = labels;
	}

	@Override
	public boolean isForceMarqueeEvent(MouseEvent e) {
		if( e.isAltDown() ) {
			start = e.getPoint();
			addingDominanceEdge = (jgraph.getPortViewAt(e.getX(), e.getY()) != null);
			
			if( addingDominanceEdge ) {
				sourceNode = getCellAt(start);
				highlightNode(sourceNode, Color.LIGHT_GRAY);
			}
			
			return true;
		}
		
		return super.isForceMarqueeEvent(e);
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if( start != null ) {
			Graphics g = jgraph.getGraphics();

			if( addingDominanceEdge ) {
				drawPhantomLine(g, start, current, Color.black, jgraph.getBackground());
				
				current = e.getPoint();
				DefaultGraphCell newTarget = getCellAt(current);
				
				if( targetNode != null && targetNode != newTarget ) {
					unHighlightNode(targetNode);
				}
				
				targetNode = newTarget;

				if( targetNode != null && targetNode != sourceNode ) {
					highlightNode(targetNode, Color.green);
				}
				
				drawPhantomLine(g, start, current, jgraph.getBackground(), Color.black);
				
				
			} else {
				drawPhantomFragment(g, start, current, Color.black, jgraph.getBackground());
				current = e.getPoint();
				drawPhantomFragment(g, start, current, jgraph.getBackground(), Color.black);
			}
		} else {
			super.mouseDragged(e);
		}
	}
	
	@Override
	public void mouseReleased(MouseEvent arg0) {
		if( start != null ) {
			if( addingDominanceEdge ) {
				if( targetNode != null ) {
					graph.addEdge(jgraph.getNodeData(sourceNode).getName(),
							jgraph.getNodeData(targetNode).getName(),
							new EdgeData(EdgeType.DOMINANCE));
					tab.setDominanceGraph(graph, labels);
				}
			} else {
				if( numHoles >= 0 ) {
					// add root to graph
					String rootName = gensym();
					graph.addNode(rootName, new NodeData(NodeType.LABELLED));
					labels.addLabel(rootName, numHoles == 0 ? "a" : "f" + numHoles);
					
					for( int i = 0; i < numHoles; i++ ) {
						String name = gensym();
						graph.addNode(name, new NodeData(NodeType.UNLABELLED));
						labels.addLabel(name, "a");
						graph.addEdge(rootName, name, new EdgeData(EdgeType.TREE));
					}
					
					tab.setDominanceGraph(graph, labels);
				}
				
			}
		}
		
		unHighlightNode(sourceNode);
		unHighlightNode(targetNode);
		start = current = null;
		sourceNode = targetNode = null;
		
		super.mouseReleased(arg0);
	}
	
	

	private void drawPhantomLine(Graphics g, Point2D from, Point2D to, Color fg, Color bg) {
		g.setColor(fg);
		g.setXORMode(bg);

		if( from != null && to != null ) {
			
			g.drawLine((int) from.getX(), (int) from.getY(), (int) to.getX(), (int) to.getY());
		}
	}

	private void drawPhantomFragment(Graphics g, Point2D from, Point2D to, Color fg, Color bg) {
		g.setColor(fg);
		g.setXORMode(bg);
		
		if( from != null && to != null ) {
			int x1 = (int) Math.min(from.getX(), to.getX());
			int y1 = (int) Math.min(from.getY(), to.getY());
			int x2 = (int) Math.max(from.getX(), to.getX());
			int y2 = (int) Math.max(from.getY(), to.getY());

			if( from.getX() > to.getX() || from.getY() > to.getY() ) {
				numHoles = -1;
			} else {
				// draw the root
				g.drawOval((x1+x2)/2 - NODE_RADIUS, y1, NODE_RADIUS*2, NODE_RADIUS*2);

				numHoles = (x2-x1+NODE_XSEP)/(NODE_RADIUS*2 + NODE_XSEP);
				
				if( y2-y1 < MIN_HEIGHT_FOR_HOLES ) {
					numHoles = 0;
				}
				
				if( numHoles >= 1) {
					// also draw some holes
					int holesTotalWidth = numHoles * NODE_RADIUS*2 + (numHoles-1) * NODE_XSEP;
					int offset = (x2-x1-holesTotalWidth)/2;

					for( int i = 0; i < numHoles; i++ ) {
						g.drawOval(x1 + offset + i*(NODE_RADIUS*2+NODE_XSEP),
								y2-NODE_RADIUS*2, NODE_RADIUS*2, NODE_RADIUS*2);
						g.drawLine((x1+x2)/2, y1+NODE_RADIUS, x1 + offset + i*(NODE_RADIUS*2+NODE_XSEP) + NODE_RADIUS, y2-NODE_RADIUS);
					}
				}
			}
		}
	}
	
	private DefaultGraphCell getCellAt(Point2D point) {
		if( point == null ) {
			return null;
		}
		
		Set<Object> cells = new HashSet<Object>();    
        Object cell = jgraph.getFirstCellForLocation((int) point.getX(), (int) point.getY());
        
        while( (cell != null) && !cells.contains(cell) ) {
            cells.add(cell);

            if( jgraph.getNodeData((DefaultGraphCell) cell) != null ) {
            	return (DefaultGraphCell) cell;
            }

            cell = jgraph.getNextCellForLocation(cell, (int) point.getX(), (int) point.getY());
        }
        
        return null;
	}
	
	private void highlightNode(DefaultGraphCell node, Color color) {
		Map<DefaultGraphCell,AttributeMap> viewMap = new HashMap<DefaultGraphCell,AttributeMap>();
		
		AttributeMap map = jgraph.getModel().getAttributes(node);
		GraphConstants.setBackground(map, color);
		viewMap.put(node, map);
		
		jgraph.getGraphLayoutCache().edit(viewMap, null, null, null);
	}
	
	// TODO - restore the correct background color as per the node's default attributes
	private void unHighlightNode(DefaultGraphCell node) {
		if( node != null ) {
			Map<DefaultGraphCell,AttributeMap> viewMap = new HashMap<DefaultGraphCell,AttributeMap>();
			
			AttributeMap map = jgraph.getModel().getAttributes(node);
			GraphConstants.setBackground(map, Color.WHITE);
			
			viewMap.put(node, map);
			
			jgraph.getGraphLayoutCache().edit(viewMap, null, null, null);
		}
	}

	private String gensym() {
		return gensymPrefix + (gensymNext++);
	}
	
	
}
