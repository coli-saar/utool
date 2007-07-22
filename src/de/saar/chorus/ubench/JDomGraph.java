/*
 * Created on 28.07.2004
 *
 */
package de.saar.chorus.ubench;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.ToolTipManager;

import org.jgraph.JGraph;
import org.jgraph.graph.AttributeMap;
import org.jgraph.graph.ConnectionSet;
import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.DefaultPort;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.GraphModel;
import org.jgraph.util.JGraphUtilities;

import de.saar.chorus.domgraph.layout.LayoutOptions.LabelType;
import de.saar.chorus.ubench.gui.Preferences.LayoutType;

/**
 * A Swing component that represents a labelled dominance graph.
 * 
 * 
 * @author Alexander Koller
 * @author Michaela Regneri
 * 
 */
public class JDomGraph extends JGraph implements Cloneable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3205330183133471528L;

	// Dominance edges don't belong to any fragment, so we remember them separately.
	private Set<DefaultEdge> dominanceEdges;
	
	
	
	// The set of DomGraphPopupListeners that have been registered for this graph.
	private Set<DomGraphPopupListener> popupListeners;
	
	// If a popup menu is currently open, the cell the menu belongs to.
	private DefaultGraphCell activePopupCell;
	
	private Rectangle boundingBox;
	
	  // the nodes and edges of the graph
    private Set<DefaultGraphCell> nodes;
    private Set<DefaultEdge> edges;
    private Map<String,DefaultGraphCell> nameToNode;
  
    // A name or ID for the graph (displayed in window title, id attribute in graph element)
    private String name;
	
	
//	private boolean hnc;
	
	private LayoutType layouttype;
	private LabelType labeltype;
	private Font nodeFont;
	private Font upperBoundFont;
	
	// This listener draws a popup menu when the right mouse button is ed.
	/*private class PopupListener extends MouseAdapter {
		public void mousePressed(MouseEvent e) {
			maybeShowPopup(e);
		}
		
		public void mouseReleased(MouseEvent e) {
			maybeShowPopup(e);
		}
		
		private void maybeShowPopup(MouseEvent e) {
			if ((e != null) && e.isPopupTrigger()) {
				int x = e.getX(), y = e.getY();
				
				activePopupCell = findNodeOrEdgeAt(x,y);
				
				if( activePopupCell != null ) {
					JPopupMenu popup = new JPopupMenu();
					Fragment frag = findFragment(activePopupCell);
					
					if( activePopupCell instanceof DefaultEdge ) {
						// This instanceof test has to come first, because
						// DefaultEdge is a subclass of DefaultGraphCell.
						EdgeData data = (EdgeData) activePopupCell.getUserObject();
						JMenuItem item = data.getMenu();
						
						if( item != null )
							popup.add(item);	                        
					} else {
						NodeData data = (NodeData) activePopupCell.getUserObject();
						JMenuItem item = data.getMenu();
						
						if( item != null )
							popup.add(item);
					}  
					
					if( frag != null ) {
						JMenuItem item = frag.getMenu();
						
						if( item != null ) {
							popup.add(item);
						}
					}
					
					popup.show(e.getComponent(), e.getX(), e.getY());	                    
				}
			}
			
		}
	}*/
	
	/**
	 * Sets up an empty dominance graph.
     * 
	 * @param origin the <code>DomGraph</code> represented here.  
	 */
	public JDomGraph() {
		super();
		
		//myDomGraph = origin;
		boundingBox = new Rectangle();
		nodes = new HashSet<DefaultGraphCell>();
	    edges = new HashSet<DefaultEdge>();
	    nameToNode = new HashMap<String,DefaultGraphCell>();
	    nodeFont = GraphConstants.DEFAULTFONT.deriveFont(Font.PLAIN, 17);
		dominanceEdges = new HashSet<DefaultEdge>();
		upperBoundFont = GraphConstants.DEFAULTFONT
		.deriveFont(Font.BOLD, 17);
		// set up popup handling
		popupListeners = new HashSet<DomGraphPopupListener>();
		//addMouseListener(new PopupListener());	
		clear();

		setAntiAliased(true); 
		
		// set up tooltip handling
		ToolTipManager.sharedInstance().registerComponent(this);
	}
	
	
	

	
	public Font getUpperBoundFont() {
		return upperBoundFont;
	}

	public void setLayoutType(LayoutType lt) {
		 if(layouttype != lt) {
			layouttype = lt;
		} 
	}
	
	public LayoutType getLayoutType() {
		return layouttype;
	}
	
	/**
	 * Remove all nodes and edges in the graph.
	 */
	public void clear() {
			getModel().remove(JGraphUtilities.getAll(this));
	        nodes.clear();
	        edges.clear();
	        nameToNode.clear();
	        name = null;
	}
	
	
	
	
	
	
	/**
	 * Compute JGraph attributes for a node of the given type.
	 * (So far, the type is ignored.)
	 * 
	 * @param type the node type.
	 * @return an AttributeMap object with reasonable style information.
	 */
	protected AttributeMap defaultNodeAttributes(NodeType type) {
		AttributeMap map = getModel().createAttributes();
		
		if( type.equals(NodeType.labelled) ) {
			// labelled nodes
			GraphConstants.setBounds(map, map.createRect(0, 0, 30, 30));
			
			GraphConstants.setBackground(map, Color.white);
			GraphConstants.setForeground(map, Color.black);
			GraphConstants.setFont(map, nodeFont);
		} else {
			//holes
			//TODO how to draw smaller rectangles??
			GraphConstants.setBounds(map, map.createRect(0, 0, 15, 15));
			
			//GraphConstants.setBorder(map, BorderFactory.createLineBorder(Color.black));
			GraphConstants.setSize(map, new Dimension(15,15));
			GraphConstants.setBackground(map, Color.white);
			GraphConstants.setForeground(map, Color.black);
			GraphConstants.setFont(map, nodeFont);
		}
		return map;
	}
	
	/**
	 * Compute JGraph attributes for a node of the given type.
	 * 
	 * @param type the edge type.
	 * @return an AttributeMap object with reasonable style information.
	 */
	protected AttributeMap defaultEdgeAttributes(EdgeType type) { 
		if (type.getType() == EdgeType.solidVal) {
			AttributeMap solidEdge = getModel().createAttributes();
			GraphConstants.setLineEnd(solidEdge, GraphConstants.ARROW_NONE);
			GraphConstants.setEndSize(solidEdge, 10);
			GraphConstants.setLineWidth(solidEdge, 1.7f);
			GraphConstants.setOpaque(solidEdge, true);
			return solidEdge;
		} else {
			AttributeMap domEdge = getModel().createAttributes();
			GraphConstants.setLineEnd(domEdge, GraphConstants.ARROW_CLASSIC);
			GraphConstants.setEndSize(domEdge, 10);
			GraphConstants.setLineColor(domEdge, Color.RED);
			GraphConstants.setLineWidth(domEdge, 1.2f);
			GraphConstants.setDashPattern(domEdge, new float[] { 3, 3 });
			GraphConstants.setOpaque(domEdge, true);
			return domEdge;
		}
		
	}
	
	
    /**
     * Adds a new node to the graph (and the underlying model). The attributes
     * of the new node are computed automatically from the node data.
     * 
     * @param data the data for the new node.
     * @return a new DefaultGraphCell object in this graph.
     */
    public DefaultGraphCell addNode(String name, NodeData data) {
        DefaultGraphCell ret = new DefaultGraphCell(data);
        GraphModel model = getModel();
        
        AttributeMap style = defaultNodeAttributes(data.getType());

        Map attributes = new HashMap();
        attributes.put(ret, style);
        
        DefaultPort port = new DefaultPort();
        ret.add(port);
        
        model.insert(new Object[] { ret, port }, attributes, new ConnectionSet(), null, null);
        
        nodes.add(ret);
        nameToNode.put(name, ret);
        
        return ret;
    }
	/**
	 * Add some sample nodes and edges to the graph. 
	 */
	public void addSampleData() {
		// nodes
		DefaultGraphCell nodeX = addNode("X", new NodeData(NodeType.labelled, "X", "f", this));
		DefaultGraphCell nodeX1 = addNode("X1", new NodeData(NodeType.unlabelled, "X1", this));
		DefaultGraphCell nodeY = addNode("Y", new NodeData(NodeType.labelled, "Y", "b", this));
		
		// edges
		addEdge(new EdgeData(EdgeType.solid, "x-x1", this), nodeX, nodeX1);
		addEdge(new EdgeData(EdgeType.dominance, "x1-y", this), nodeX1, nodeY);
	}
	
	/**
	 * Once the fragments have been computed, add some sample menu items to them.
	 */
	/*public void addSampleFragmentMenus() {
		for( Fragment frag : fragments ) {
			frag.addMenuItem("fFoo", "F Foo Foo");
			frag.addMenuItem("fBar", "F Bar Bar");
			frag.addMenuItem("fBaz", "F Baz Baz");
		}
	}*/
	
	 /**
     * Adds a new edge to the graph (and the underlying model). The edge goes from the
     * 0-th port of the node src to the 0-th port of the node tgt. The style attributes
     * of the new edge are computed automatically from the edge data.
     * 
     * @param data the data for the new edge.
     * @param src the node cell at which the edge should start.
     * @param tgt the node cell at which the edge should end.
     * @return a new DefaultEdge object in this graph.
     */
    public DefaultEdge addEdge(EdgeData data, DefaultGraphCell src, DefaultGraphCell tgt) {
        
        DefaultEdge ret = new DefaultEdge(data);
        
        
        GraphModel model = getModel();
        AttributeMap style = defaultEdgeAttributes(data.getType());
        Map attributes = new HashMap();
        attributes.put(ret, style);
        
    
        ConnectionSet cs = new ConnectionSet();
        cs.connect(ret, src.getChildAt(0), tgt.getChildAt(0));
        model.insert(new Object[] { ret }, attributes, cs, null, null );
        
        edges.add(ret);
        
        return ret;
    }
	
	/**
	 * @return Returns the dominanceEdges.
	 */
	public Set<DefaultEdge> getDominanceEdges() {
		return dominanceEdges;
	}
	
	
	
	//// popup handling methods
	
	/**
	 * Add a popup listener. This listener will be called every time
	 * the user selects an item from a (node or fragment) popup menu.
	 * 
	 * @param listener
	 */
	public void addPopupListener(DomGraphPopupListener listener) {
		popupListeners.add(listener);
	}
	
	/**
	 * Remove a popup listener.
	 * 
	 * @param listener
	 */
	public void removePopupListener(DomGraphPopupListener listener) {
		popupListeners.remove(listener);
	}
	
	/**
	 * Notify all registered popup listeners that the user selected
	 * a popup menu item. This means that the <code>popupSelected</code>
	 * methods of these listener objects are called.
	 * 
	 * @param menuItemLabel the label of the selected menu item.
	 */
/*	void notifyPopupListeners(String menuItemLabel) {
		for( DomGraphPopupListener listener : popupListeners ) {
			listener.popupSelected(activePopupCell, 
					findFragment(activePopupCell), 
					menuItemLabel);
		}
		
		activePopupCell = null;
	}*/
	
	
	
	
	//// tooltips
	
	/**
	 * @return Returns the boundingBox.
	 */
	public Rectangle getBoundingBox() {
		return boundingBox;
	}
	
	/**
	 * @param boundingBox The boundingBox to set.
	 */
	public void setBoundingBox(Rectangle boundingBox) {
		this.boundingBox = boundingBox;
	}
	
	

	/**
	 * Clones this graph.
	 * @return the clone
	 */
	public JDomGraph clone() {
		
		// setting up a new graph
		JDomGraph clone = new JDomGraph();
		
		// copy the nodes by creating new (equivalent)
		// node data
		for(DefaultGraphCell cell : nodes ) {
			NodeData cellData = getNodeData(cell);
			NodeData cloneData;
			if( cellData.getType().equals(NodeType.labelled)) {
				cloneData = new NodeData(NodeType.labelled, cellData.getName(), cellData.getLabel(), clone); 
			} else {
				cloneData = new NodeData(NodeType.unlabelled, cellData.getName(), clone); 
			}
			cloneData.addMenuItem(cellData.getMenuLabel(), cellData.getName());
			clone.addNode(cellData.getName(), cloneData);
		}
		
		// copy the edges by creating new (equivalent)
		// edge data
		for (DefaultEdge edge : edges ) {
			
			EdgeData cellData = getEdgeData(edge);
			EdgeData cloneData;
			
			if( cellData.getType().equals(EdgeType.solid)) {
				cloneData = new EdgeData(EdgeType.solid, cellData.getName(), clone );
			} else {
				cloneData = new EdgeData(EdgeType.dominance, cellData.getName(), clone );
			}
			
			cloneData.addMenuItem(cellData.getMenuLabel(), cellData.getName());
			clone.addEdge(cloneData, clone.getNodeForName(getNodeData(getSourceNode(edge)).getName()), 
					clone.getNodeForName(getNodeData(getTargetNode(edge)).getName()));
		}
		
		// setting the scale
		clone.setScale(getScale());
		return clone;
	}
	
	/**
     * Returns the source node of an edge. 
     * @param edge the edge
     * @return the edge's source node
     */
    public DefaultGraphCell getSourceNode(DefaultEdge edge) {
        return (DefaultGraphCell) JGraphUtilities.getSourceVertex(this, edge);
    }
    
    
    /**
     * Returns the target node of and edge.
     * @param edge the edge
     * @return the edge's target node
     */
    public DefaultGraphCell getTargetNode(DefaultEdge edge) {
        return (DefaultGraphCell) JGraphUtilities.getTargetVertex(this, edge);
    }

    /**
     * Removes all dominance edges from this graph.
     *
     */
    public void clearDominanceEdges() {
    	for(DefaultEdge edge : dominanceEdges) {
			getModel().remove(new Object[]{ edge });
			edges.remove(edge);
		}
    	dominanceEdges.clear();
    }
    
    /**
     * Get the node data of a node cell.
     * 
     * @param node
     * @return the node data.
     */
    public NodeData getNodeData(DefaultGraphCell node) {
    	if (node.getUserObject() instanceof NodeData) {
			return (NodeData) node.getUserObject();
		} else {
			return null;
		}
    }
    
    /**
     * Get the edge data of an edge cell.
     * 
     * @param edge
     * @return the edge data.
     */
    public EdgeData getEdgeData(DefaultEdge edge) {
    	if (edge.getUserObject() instanceof EdgeData) {
			return (EdgeData) edge.getUserObject();
		} else {
			return null;
		}
    }
    
    /**
     * Look up the node with the specified name.
     * 
     * @param name
     * @return that node.
     */
    public DefaultGraphCell getNodeForName(String name) {
        return nameToNode.get(name);
    }

    
	

	public LabelType getLabeltype() {
		return labeltype;
	}
	
	


	public void setLabeltype(LabelType labeltype) {
		if(this.labeltype != labeltype) {
			this.labeltype = labeltype;
			for( DefaultGraphCell node : nodes ) {
				getNodeData(node).setShowLabel(labeltype);
			}
			
		}
	}





	public Set<DefaultEdge> getEdges() {
		return edges;
	}





	public void setEdges(Set<DefaultEdge> edges) {
		this.edges = edges;
	}





	public LayoutType getLayouttype() {
		return layouttype;
	}





	public void setLayouttype(LayoutType layouttype) {
		this.layouttype = layouttype;
	}





	public String getName() {
		return name;
	}





	public void setName(String name) {
		this.name = name;
	}





	public Set<DefaultGraphCell> getNodes() {
		return nodes;
	}





	public void setNodes(Set<DefaultGraphCell> nodes) {
		this.nodes = nodes;
	}





	public void setDominanceEdges(Set<DefaultEdge> dominanceEdges) {
		this.dominanceEdges = dominanceEdges;
	}

    /**
     * Compute the outgoing edges for a node. 
     * The returned list lists the complete edges (with types).
     * The list is sorted before returning by the <code> EdgeSortingComparator </code>
     * 
     * @param node the node to compute the outgoing edges for
     * @return the sorted list of out-edges
     */
    public List<DefaultEdge> getOutEdges(DefaultGraphCell node) {
        List<DefaultEdge> ret = new ArrayList<DefaultEdge>();
        
        for( DefaultEdge edge : edges ) {
            if( getSourceNode(edge) == node )
                ret.add(edge);
        }

        return ret;
    }




	public void setUpperBoundFont(Font upperBoundFont) {
		this.upperBoundFont = upperBoundFont;
	}
	
	
	
	
}
