/*
 * Created on 28.07.2004
 *
 */
package de.saar.chorus.ubench.jdomgraph;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.ToolTipManager;

import org.jgraph.JGraph;
import org.jgraph.graph.AttributeMap;
import org.jgraph.graph.CellView;
import org.jgraph.graph.ConnectionSet;
import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.DefaultPort;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.GraphModel;
import org.jgraph.graph.ParentMap;
import org.jgraph.util.JGraphUtilities;

import de.saar.chorus.domgraph.layout.LayoutOptions.LabelType;
import de.saar.chorus.ubench.Preferences.LayoutType;
import de.saar.chorus.ubench.jdomgraph.Fragment.FragmentUserObject;

/**
 * A Swing component that represents a labelled dominance graph.
 * 
 * 
 * @author Alexander Koller
 * @author Michaela Regneri
 * 
 */
public class JDomGraph extends JGraph implements Cloneable {
	
	private static final long serialVersionUID = 3205330183133471528L;

	// Dominance edges don't belong to any fragment, so we remember them separately.
	private Set<DefaultEdge> dominanceEdges;
	
	private Rectangle boundingBox;
	
	  // the nodes and edges of the graph
    private Set<DefaultGraphCell> nodes;
    private Set<DefaultEdge> edges;
    private Set<Fragment> fragments;
    private Map<String,DefaultGraphCell> nameToNode;
  
    // A name or ID for the graph (displayed in window title, id attribute in graph element)
    private String name;
	
	
//	private boolean hnc;
	
	private LayoutType layouttype;
	private LabelType labeltype;
	private Font nodeFont;
	private Font upperBoundFont;
	
	private boolean fragmented;
	
	
	
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
	    fragments = new HashSet<Fragment>();
	    nameToNode = new HashMap<String,DefaultGraphCell>();
	    nodeFont = GraphConstants.DEFAULTFONT.deriveFont(Font.PLAIN, 17);
	    fragmented = false;
		dominanceEdges = new HashSet<DefaultEdge>();
		upperBoundFont = GraphConstants.DEFAULTFONT.deriveFont(Font.BOLD, 17);
		
		clear();

		getGraphLayoutCache().setSelectsAllInsertedCells(false);
		getGraphLayoutCache().setSelectLocalInsertedCells(false);
		
		
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
		AttributeMap map = new AttributeMap();
		
		if( type.equals(NodeType.labelled) ) {
			// labelled nodes
			GraphConstants.setBounds(map, map.createRect(0, 0, 30, 30));
			
			GraphConstants.setBackground(map, Color.white);
			GraphConstants.setForeground(map, Color.black);
			GraphConstants.setFont(map, nodeFont);
		} else {
			//holes
			//TODO how to draw smaller rectangles??
			GraphConstants.setBounds(map, map.createRect(0, 0, 30, 25));
			
			//GraphConstants.setBorder(map, BorderFactory.createLineBorder(Color.black));
			GraphConstants.setSize(map, new Dimension(15,15));
			GraphConstants.setBackground(map, Color.white);
			GraphConstants.setForeground(map, Color.black);
			GraphConstants.setFont(map, nodeFont);
		}
		
		
		return map;
	}
	
	public Fragment addFragment(Fragment fragment) {
		 if(! fragmented) {
			 fragmented = true;
		 }
		if(! fragments.contains(fragment)) {
			getGraphLayoutCache().insertGroup(new DefaultGraphCell(
					fragment.getFragmentUserObject()), fragment.getAllCells().toArray());
			
		}
	
		return fragment;
	}
	
	
	
	/**
	 * Compute JGraph attributes for a node of the given type.
	 * 
	 * @param type the edge type.
	 * @return an AttributeMap object with reasonable style information.
	 */
	protected AttributeMap defaultEdgeAttributes(EdgeType type) { 
		if (type.getType() == EdgeType.solidVal) {
			AttributeMap solidEdge = new AttributeMap();
			GraphConstants.setLineEnd(solidEdge, GraphConstants.ARROW_NONE);
			GraphConstants.setEndSize(solidEdge, 10);
			GraphConstants.setLineWidth(solidEdge, 1.2f);
			GraphConstants.setOpaque(solidEdge, true);
			return solidEdge;
		} else {
			AttributeMap domEdge = new AttributeMap();
			GraphConstants.setLineEnd(domEdge, GraphConstants.ARROW_CLASSIC);
			GraphConstants.setEndSize(domEdge, 5);
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
    	DefaultGraphCell cell = getNodeForName(name);
    	if(cell == null) {
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
    	return cell;
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

		Object[] foundedges = JGraphUtilities.getEdgesBetween(this, src, tgt);

		if(foundedges.length <= 0) {
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
		
		// if we arrive here, there is at least one edge in 'foundedges'
		return (DefaultEdge) foundedges[0];
	}
	
	/**
	 * @return Returns the dominanceEdges.
	 */
	public Set<DefaultEdge> getDominanceEdges() {
		return dominanceEdges;
	}
	
	
	
	
	
	
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





	/**
	 * This overrides the <code>getToolTipText(MouseEvent)</code> method of
	 * JGraph. As JGraph overwrites the original <code>Component</code> method
	 * but ignores the tooltips returned by the user
	 * objects of its cells, the tooltips are reconfigured here.
	 * 
	 * @return the tooltip text of the recent node
	 * @see org.jgraph.JGraph#getToolTipText(MouseEvent)
	 */
	public String getToolTipText(MouseEvent e) {
		
		// get the uppermost node under the mouse pointer
		DefaultGraphCell cellfront = (DefaultGraphCell) 
					getFirstCellForLocation(e.getX(), e.getY());
		
		
		// if there is actually a graph cell under the mousepointer...
		if(cellfront != null) {
			

			
			// find its associated user object
			Object uo = cellfront.getUserObject();

			if( uo instanceof NodeData ) {
				// if it is a node, return the node's tooltip text
				return ( (NodeData) uo).getToolTipText();
			} else {

				// no node at te front, so look at the cell behind it
				DefaultGraphCell cellback = (DefaultGraphCell)
							getNextCellForLocation(cellfront, e.getX(), e.getY());
			
				if( cellback != null ) {
				Object uob = cellback.getUserObject();
				
				// if it is a node (the fragment was at the front),
				// return the node's tooltip text
					if(uob instanceof NodeData) {
						return ( (NodeData) uob).getToolTipText();
					} else {
					// no node under the mouse pointer, so 
					// let's display the fragment's tool tip (in case there is one).
						if( uob instanceof FragmentUserObject ) {
							return ( (FragmentUserObject) uo ).getToolTipText();
						}
					}
				} else {
					// nothing behind the front cell and the front
					// cell is no node, but perhaps the front is part of a fragment
					if( uo instanceof FragmentUserObject ) {
						return ( (FragmentUserObject) uo ).getToolTipText();
					}
				}
			} 

			// if we are here, the uppermost cell is either a dominance edge
			// or a tree edge or nothing of both, but in the range of a fragment.
			// for the two latter cases, show the fragment tooltip.
			if( uo instanceof FragmentUserObject ) {
				return ( (FragmentUserObject) uo ).getToolTipText();
			}

		}
		
		// if there is no cell under the mouse pointer, 
		// do what the JGraph would do by default.
		return super.getToolTipText(e);

	}
	
	
	
	
	
	
}
