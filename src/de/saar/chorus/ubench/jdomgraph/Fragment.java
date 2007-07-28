/*
 * Created on 31.07.2004
 *
 */
package de.saar.chorus.ubench.jdomgraph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JLabel;

import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.util.JGraphUtilities;

/**
 * A fragment in a dominance graph. This fragment contains nodes (labelled
 * and unlabelled) and solid edges, such that the contained nodes are all
 * connected by the contained edges.
 * 
 * Fragments of a dominance graph are actually computed by the computeFragments
 * method in JDomGraph.
 * 
 * Objects of this class can serve as popup targets, i.e.
 * they provide a menu item for a popup menu.
 * 
 * @author Alexander Koller
 *
 */
public class Fragment {
    private Set<DefaultGraphCell> nodes;
    private Set<DefaultEdge> edges;
    private DefaultGraphCell groupObject;
    private StringBuilder fragmentName;
    private FragmentUserObject fou;
    
    public Fragment(JDomGraph parent) {
        
        nodes = new HashSet<DefaultGraphCell>();
        edges = new HashSet<DefaultEdge>();
        fragmentName = new StringBuilder("fragment: ");
        fou = new FragmentUserObject(fragmentName);
        groupObject = new DefaultGraphCell(fou);
        
        
    }
    
    
    public boolean contains(DefaultGraphCell cell) {
    	if( nodes.contains(cell) && edges.contains(cell) ) {
    		return true;
    	}
    	return false;
    }
    
    FragmentUserObject getFragmentUserObject() {
    	return fou;
    }
    
    /**
     * Add a node to this fragment.
     * 
     * @param node
     */
    public void add(DefaultGraphCell node) {
        nodes.add(node);
        fragmentName.append(((NodeData)node.getUserObject()).getName() + " ");
        
    }
    
    /**
     * Add a solid edge to this fragment.
     * 
     * @param edge
     */
    public void add(DefaultEdge edge) {
        edges.add(edge);
    }
    
    /**
     * Add all nodes and edges of some other fragment to this one.
     * This is useful when two fragments are merged when computing
     * the maximal fragments of a dominance graph.
     * 
     * @param frag
     */
    public void addAll(Fragment frag) {
        for( DefaultGraphCell node : frag.getNodes() ) {
            add(node);
        }
        
        for( DefaultEdge edge : frag.getEdges() ) {
            add(edge);
        }
    }
    
    /**
     * Get all nodes in this fragment.
     * 
     * @return the set of all nodes.
     */
    public Set<DefaultGraphCell> getNodes() {
        return nodes;
    }
    
    /**
     * Get all cells in this fragment. This includes all nodes, all edges,
     * and the primary ports of the nodes which were used to connect the
     * edges.
     * 
     * @return all cells in this fragment.
     */
    Set<Object> getAllCells() {
        Set<Object> ret = new HashSet<Object>();
        ret.addAll(getNodes());
        ret.addAll(getEdges());
        
        // ports
        for( DefaultGraphCell node : getNodes() ) {
            ret.add(node.getChildAt(0));
        }
        
        return ret;
    }
    
    /**
     * Get all edges in this fragment.
     * 
     * @return the set of all edges.
     */
    public Set<DefaultEdge> getEdges() {
        return edges;
    }

    
    
    /**
     * Get the number of nodes in this fragment.
     * 
     * @return the number of nodes.
     */
    public int size() {
        return nodes.size();
    }

    /**
     * Get the graph cell corresponding to the entire fragment.
     * This will be a group cell to which all the nodes, primary ports,
     * and edges (as given by getAllCells) belong.
     * 
     * @return the cell.
     */
    public DefaultGraphCell getGroupObject() {
        return groupObject;
    }
   
    
    public String toString() {
        return "<" + fragmentName.toString() + ">";
    }

   
    /**
     * This is a data storage for a <code>Fragment</code> to 
     * be read out by a <code>JDomGraph</code>.
     * 
     * @author Michaela Regneri
     *
     */
    public class FragmentUserObject extends JLabel {
    	
    	private StringBuilder fragname; // the (dynamically built) name of the fragment
    	
    	FragmentUserObject(StringBuilder name) {
    		super();
    		fragname = name;
    		setOpaque(false);
    	}
    	
    	/**
    	 * This has to return "null" because the <code>DefaultGraphCell</code>
    	 * representing the fragment may not display its own text.
    	 * 
    	 * @return null
    	 */
    	public String toString() {
    		return null;
    	}

		/**
		 * This overrides the <code>getToopTipText</code> method in 
		 * <code>JComponent</code>. Actually, this is meant to pass the right
		 * tooltip text to the <code>JGraph</code> Object. Unfortunately, with the new
		 * implementation (v. 5.10.1.3), the getToolTipText method of user objects
		 * are not considered anymore. For principle reasons, this will still be the
		 * way to get the tooltip text for a <code>FragmentUserObject</code>.
		 * 
		 * @return the fragment name 
		 * @see javax.swing.JComponent#getToolTipText()
		 */
		public String getToolTipText() {
			return fragname.toString();
		}
    	
    	
    	
    }

	
	public boolean equals(Object arg0) {
		
		return ( arg0 instanceof Fragment && ( (Fragment) arg0).getNodes().equals(nodes) );
	}

	@Override
	public int hashCode() {
		
		return super.hashCode();
	}
	
	
	
	
}
