/*
 * Created on 28.07.2004
 *
 */
package de.saar.chorus.leonardo;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;

import java.awt.Rectangle; //mr test
import java.awt.geom.Rectangle2D; //mr test


import javax.swing.*;

import org.jgraph.JGraph;
import org.jgraph.graph.*;
import org.jgraph.layout.*;
import org.jgraph.layout.SugiyamaLayoutAlgorithm;
import org.jgraph.util.JGraphUtilities;

/**
 * A Swing component that represents a dominance graph.
 * <p>
 * 
 * TODO Eventually, this shouldn't be a subclass of JGraph, but rather it should
 * use a private JGraph field and then offer delegate methods to be a subclass
 * of JComponent.
 *  
 */
class JDomGraph extends JGraph {
    // the nodes and edges of the graph
    private Set<DefaultGraphCell> nodes;
    private Set<DefaultEdge> edges;
    
    // map node names to nodes.
    private Map<String,DefaultGraphCell> nameToNode;
    
    // The fragments of the graph. This only makes sense after computeFragments
    // has been called.
    private Set<Fragment> fragments;
    // Dominance edges don't belong to any fragment, so we remember them separately.
    private Set<DefaultEdge> dominanceEdges;
    
    // Map nodes and solid edges to the fragments they belong to.
    private Map<DefaultGraphCell,Fragment> nodeToFragment;
    private Map<DefaultEdge,Fragment> edgeToFragment; 
    
    // The set of DomGraphPopupListeners that have been registered for this graph.
    private Set<DomGraphPopupListener> popupListeners;
    
    // If a popup menu is currently open, the cell the menu belongs to.
    private DefaultGraphCell activePopupCell;
    
    // A name or ID for the graph (displayed in window title, id attribute in graph element)
    private String name;
    
    
    
    // This listener draws a popup menu when the right mouse button is ed.
    private class PopupListener extends MouseAdapter {
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
    }
    
    
	/**
	 * Sets up an empty dominance graph.  
	 */
	public JDomGraph() {
		super();
		
		nodes = new HashSet<DefaultGraphCell>();
		edges = new HashSet<DefaultEdge>();
		nameToNode = new HashMap<String,DefaultGraphCell>();
		
		fragments = new HashSet<Fragment>();
		nodeToFragment = new HashMap<DefaultGraphCell,Fragment>();
		dominanceEdges = new HashSet<DefaultEdge>();
		edgeToFragment = new HashMap<DefaultEdge,Fragment>();
		
		// set up popup handling
		popupListeners = new HashSet<DomGraphPopupListener>();
		addMouseListener(new PopupListener());		
		
		clear();
		
		// set up tooltip handling
		ToolTipManager.sharedInstance().registerComponent(this);
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
	    
	    clearFragments();
	}
	
	/**
	 * Remove all information about fragments. This includes deleting
	 * the fragment cells from the graph model.
	 */
	private void clearFragments() {
	    for( Fragment frag : fragments ) {
	        getModel().remove(new Object[] { frag.getGroupObject() } );
	    }
	    
	    fragments.clear();
	    nodeToFragment.clear();
	    dominanceEdges.clear();
	    edgeToFragment.clear();
	}

	


	/**
	 * Compute JGraph attributes for a node of the given type.
	 * (So far, the type is ignored.)
	 * 
	 * @param type the node type.
	 * @return an AttributeMap object with reasonable style information.
	 */
	private AttributeMap defaultNodeAttributes(NodeType type) {
		GraphModel model = getModel();
		AttributeMap map = model.createAttributes();
		GraphConstants.setBounds(map, map.createRect(0, 0, 30, 30));
		GraphConstants.setBorder(map, BorderFactory.createLineBorder(Color.black));
		GraphConstants.setBackground(map, Color.white);
		GraphConstants.setForeground(map, Color.black);
		GraphConstants.setFont(map, GraphConstants.DEFAULTFONT.deriveFont(
				Font.BOLD, 12));
		GraphConstants.setOpaque(map, true);

		return map;
	}

	/**
	 * Compute JGraph attributes for a node of the given type.
	 * 
	 * @param type the edge type.
	 * @return an AttributeMap object with reasonable style information.
	 */
	private AttributeMap defaultEdgeAttributes(EdgeType type) { 
		if (type.getType() == EdgeType.solidVal) {
			AttributeMap solidEdge = getModel().createAttributes();
			GraphConstants.setLineEnd(solidEdge, GraphConstants.ARROW_SIMPLE);
			GraphConstants.setEndSize(solidEdge, 10);
			return solidEdge;
		} else {
			AttributeMap domEdge = getModel().createAttributes();
			GraphConstants.setLineEnd(domEdge, GraphConstants.ARROW_SIMPLE);
			GraphConstants.setEndSize(domEdge, 10);
			GraphConstants.setDashPattern(domEdge, new float[] { 3, 3 });
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
	public DefaultGraphCell addNode(NodeData data) {
		DefaultGraphCell ret = new DefaultGraphCell(data);
		GraphModel model = getModel();

		AttributeMap style = defaultNodeAttributes(data.getType());
		Map attributes = new HashMap();
		attributes.put(ret, style);

		DefaultPort port = new DefaultPort();
		ret.add(port);
		
		model.insert(new Object[] { ret, port }, attributes, new ConnectionSet(), null, null);
		
		nodes.add(ret);
		nameToNode.put(data.getName(), ret);

		return ret;
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
	public DefaultEdge addEdge(EdgeData data, DefaultGraphCell src,	DefaultGraphCell tgt) {
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
	 * Add some sample nodes and edges to the graph. 
	 */
	public void addSampleData() {
		// nodes
		DefaultGraphCell nodeX = addNode(new NodeData(NodeType.labelled, "X", "f", this));
		DefaultGraphCell nodeX1 = addNode(new NodeData(NodeType.unlabelled, "X1", this));
		DefaultGraphCell nodeY = addNode(new NodeData(NodeType.labelled, "Y", "b", this));

		// edges
		addEdge(new EdgeData(EdgeType.solid, "x-x1", this), nodeX, nodeX1);
		addEdge(new EdgeData(EdgeType.dominance, "x1-y", this), nodeX1, nodeY);
	}
	
	/**
	 * Once the fragments have been computed, add some sample menu items to them.
	 */
	public void addSampleFragmentMenus() {
	    for( Fragment frag : fragments ) {
	        frag.addMenuItem("fFoo", "F Foo Foo");
	        frag.addMenuItem("fBar", "F Bar Bar");
	        frag.addMenuItem("fBaz", "F Baz Baz");
	    }
	}
	
	/**
	 * prints position and dimensions of all 
	 * fragments and their node on screen.
	 * NOTE: made it public (MR)
	 *
	 */
	public void printPositions() {
		int frCount = 1;
		
		//putting height and widht of fragments and nodes on screen,
		//just for comparing.
		
		for(Fragment frag : fragments)
		{
			DefaultGraphCell recCell = frag.getGroupObject();
			Rectangle2D karton = GraphConstants.getBounds(recCell.getAttributes());
			System.out.print("Fragment No. " + frCount + " " + recCell.toString());
			System.out.println(" is at " + karton);
			frCount++;
			
			for( DefaultGraphCell recNode : frag.getNodes()) {
				Rectangle2D nodeRect = GraphConstants.getBounds(recNode.getAttributes());
				System.out.print("  Node " + getNodeData(recNode).getName() + "(" + recNode + ")");
				System.out.println(" is at " + nodeRect);
			}
			
			System.out.println("");
		}
		
		System.out.println("");
	}

	/**
	 * Apply a layout algorithm to this graph. Before the first call of
	 * this method, the graph won't be display properly because all nodes
	 * will be in the same place.
	 */
	public void computeLayout() {
		
		System.out.println("Fragments and nodes _before_ Sugiyama:");
		printPositions();
		

	    
		JGraphUtilities.applyLayout(this, new SugiyamaLayoutAlgorithm());
		
		System.out.println("Fragments and nodes with Sugiyama one Time: ");
		printPositions();
		
		//It seems to make a difference whether the layout algorithm
		//is applied two times or one time. To show this, the algorithm
		//is used a second time here.
		
		JGraphUtilities.applyLayout(this, new SugiyamaLayoutAlgorithm());
		
		//mr: some experimental code ...
		
		System.out.println("Fragments and nodes with Sugiyama two Times: ");
		printPositions();
		
		//changing layout: 
		//every fragment (means: its nodes) is arranged with tree layout.
		
		Iterator<Fragment> frIt = fragments.iterator();
	
		
		while(frIt.hasNext())
		{
			computeFragmentLayout(frIt.next(), new TreeLayoutAlgorithm());
			
		}
		
		//putting height and width on screen again and finding something -
		//strange? The fragments seem to grow.
		
		System.out.println("Fragments with TreeLayout after Sugiyama: ");
		printPositions();
	
		
	    
	    // TODO Put a more intelligent layout algorithm here.
	    // In principle, JGraphUtilities offers a method for layout out only
	    // parts of the graph; this method requires an array of cells
	    // (nodes, edges, and ports) as its second argument. However, 
	    // it doesn't seem to be possible to layout the fragments internally
	    // first and then the fragment graph second -- the result is a bunch
	    // of fragments in the same screen location. We do get a layout
	    // if we first layout the fragment graph and then the fragments internally,
	    // but that looks awful. So we stick to Sugiyama for the time being.
	    //
	    // Note: Sugiyama has the old problem of swapping child nodes freely.
	}
	
	public void computeFragmentLayout(Fragment frag, JGraphLayoutAlgorithm lay)
	{
		Set allCells = frag.getAllCells();
		Object[] recentCells = allCells.toArray();
		
		JGraphUtilities.applyLayout(this, recentCells, lay);
	}
	
	
	/**
	 * Get all nodes of the dominance graph.
	 * 
	 * @return the set of all nodes.
	 */
	public Set<DefaultGraphCell> getNodes() {
	    return nodes;
	}
	
	/**
	 * Get all edges of the dominance graph.
	 * 
	 * @return the set of all edges.
	 */
	public Set<DefaultEdge> getEdges() {
	    return edges;
	}
	
	/**
	 * Group the nodes of the graph into the maximal fragments.
	 * A fragment is a set of nodes that are connected by solid edges.
	 * Maximal fragments are maximal elements with respect to node set inclusion.
	 * This method adds new cells to the graph, one for each fragment,
	 * and adds the nodes and solid edges that belong to the fragment to 
	 * this cell (as a group).
	 */
	public void computeFragments() {
	    clearFragments();

	    // initially, put each nodes into a fragment of its own.
	    for( DefaultGraphCell node : nodes ) {
	        Fragment f = new Fragment(this);
	        f.add(node);
	        fragments.add(f);
	        nodeToFragment.put(node, f);
	    }
	    
	    // Now iterate over the edges. If two nodes are connected by a solid edge,
	    // merge their fragments.
	    for( DefaultEdge edge : getEdges() ) {
	        if( getEdgeData(edge).getType() == EdgeType.solid ) {
	            // merge fragments
	            Fragment sFrag = nodeToFragment.get(JGraphUtilities.getSourceVertex(this, edge));
	            Fragment tFrag = nodeToFragment.get(JGraphUtilities.getTargetVertex(this, edge));
	            
	            if( sFrag.size() > tFrag.size() ) {
	                mergeInto( sFrag, tFrag );

	                sFrag.add(edge);
	                edgeToFragment.put(edge, sFrag);
	            } else {
	                mergeInto( tFrag, sFrag );

	                tFrag.add(edge);
	                edgeToFragment.put(edge, tFrag);
	            }
	        } else {
	            dominanceEdges.add(edge);
	        }
	    }
	    
	    
	    // insert fragment cells into the graph.
	    for( Fragment frag : fragments ) {
	    	
	    	addTestMenu(frag);
	    	
	        getModel().insert( new Object[] { frag.getGroupObject() },
	                			null, null, null, null );
	    }
	    
	    //MR: for testing a listener for the sample layout menu;
	    //perhaps the listener in general should be added somewhere
	    //else.
	    this.addPopupListener(new FragmentLayoutChangeListener());
	}
	
	
	/**
	 * This adds a hard coded menu to a fragment.
	 * Serves to check optical and internal dimension
	 * changes of fragments. (Michaela)
	 * 
	 * @param frag, the fragment to add the menu to
	 */
	private void addTestMenu(Fragment frag)
	{
		frag.addMenuItem("tree", "Tree Layout");
		frag.addMenuItem("sug", "Sugiyama Layout");
		frag.addMenuItem("allSug", "ALL Sugiyama Layout");
		frag.addMenuItem("allTree", "ALL Tree Layout");
		frag.addMenuItem("prInf", "print dimensions / position");
	}

	/**
	 * Merge one fragment into another. This means that all nodes of the "from"
	 * fragment become nodes of the "into" fragment, and the "from" fragment
	 * is deleted.
	 * 
	 * @param into
	 * @param from
	 */
	private void mergeInto(Fragment into, Fragment from ) {
        into.addAll(from);
        
        for( DefaultGraphCell node : from.getNodes() ) {
            nodeToFragment.put(node, into);
        }
        
        for( DefaultEdge edge : from.getEdges() ) {
            edgeToFragment.put(edge, into);
        }

        fragments.remove(from);
	}
	
	
	/**
	 * Get the node data of a node cell.
	 * 
	 * @param node
	 * @return the node data.
	 */
	public NodeData getNodeData(DefaultGraphCell node) {
	    return (NodeData) node.getUserObject();
	}
	
	/**
	 * Get the edge data of an edge cell.
	 * 
	 * @param edge
	 * @return the edge data.
	 */
	public EdgeData getEdgeData(DefaultEdge edge) {
	    return (EdgeData) edge.getUserObject();
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

	/**
	 * Get the set of all fragments.
	 * 
     * @return the fragments.
     */
	public Set<Fragment> getFragments() {
	    return fragments;
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
	void notifyPopupListeners(String menuItemLabel) {
        for( DomGraphPopupListener listener : popupListeners ) {
            listener.popupSelected(activePopupCell, 
                    				findFragment(activePopupCell), 
                    				menuItemLabel);
        }
        
        activePopupCell = null;
     }
	
	
	
	
	//// tooltips
	
	/* 
	 * Show tooltips for nodes.
	 * 
	 * @see javax.swing.JComponent#getToolTipText(java.awt.event.MouseEvent)
	 */
	public String getToolTipText(MouseEvent e) {
	    if(e != null) {
	        // Fetch Cell under Mousepointer
	        DefaultGraphCell c = findNodeOrEdgeAt(e.getX(), e.getY());
	        if (c != null) {
	            if( !(c instanceof DefaultEdge) ) {
	                NodeData data = getNodeData(c);
	                return data.getMenuLabel();
	            }
	        }
	    } 
	    
	    return null;
	}	
	
	
	/**
	 * Return the node at the mouse position (x,y). This method
	 * only works after fragments have been computed.
	 * 
	 * @param x
	 * @param y
	 * @return reference to the node cell; null if there is no node at the position.
	 */
	private DefaultGraphCell findNodeOrEdgeAt(int x, int y) {
	    Set<Object> cells = new HashSet<Object>();	  
	    Object cell = getFirstCellForLocation(x,y);
	    
        while( (cell != null) && !cells.contains(cell) ) {
            cells.add(cell);
            
            if( nodeToFragment.containsKey(cell) ) {
                return (DefaultGraphCell) cell;
            } else if( cell instanceof DefaultEdge ) {
                return (DefaultGraphCell) cell;
            }

        	cell = getNextCellForLocation(cell, x, y);            
        }
        
        return null;
	}
	
	/**
	 * Return the fragment a node or edge belongs to. This method
	 * is only meaningful after fragments have been computed.
	 * 
	 * @param cell a node or edge in the graph
	 * @return the fragment it belongs to; null if it doesn't belong to any fragment (e.g. a dominance edge)
	 */
	private Fragment findFragment(DefaultGraphCell cell) {
	    if( nodeToFragment.containsKey(cell)) {
	        return nodeToFragment.get(cell);
	    } else if( edgeToFragment.containsKey(cell)) {
	        return edgeToFragment.get(cell);
	    } else
	        return null;
	}




    /**
     * Set the name (= ID) of the dominance graph. This name could e.g. be displayed in the
     * window title. 
     * 
     * @param name new name of the graph.
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Get the name (= ID) of the dominance graph.
     * 
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }
}