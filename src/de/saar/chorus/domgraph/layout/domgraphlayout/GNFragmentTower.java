package de.saar.chorus.domgraph.layout.domgraphlayout;

import static de.saar.chorus.domgraph.layout.DomGraphLayoutParameters.fragmentYDistance;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A special part for the layout of a dominance graph.
 * 
 * TODO Insert a tower definition here.
 * 
 * @author Alexander Koller
 * @author Michaela Regneri
 *
 */

/*
 * Coordinate systems in the class GNFragmentTower
 * ---------------------------------------------
 * 
 * - The user can add a sequence of "backbone" fragments to the tower.
 *   These backbone fragments will all be placed above each other.
 * - There can also be "leaf" fragments, each of which belongs to
 *   a specific backbone fragment. When adding the leaf fragment,
 *   the user must specify an X offset; the leaf fragment will be
 *   moved right by this offset, starting at the position of the
 *   backbone fragment it belongs to. 
 * - The internal coordinate system of the class places the first
 *   (= lowest) backbone fragment at y-position 0, and then counts
 *   _up_ from this position (i.e. (0,10) is _above_ (0,0)).
 * - Because the lowest backbone fragment can have leaf fragments
 *   associated with it, it is possible that a fragment is placed
 *   at a negative Y position. The maximum Y position used in this
 *   way is recorded in minYCoordinate.
 * - The method place() gets passed the coordinates for the left upper
 *   corner of the tower. It converts the internal coordinate system
 *   into absolute positions in the Swing coordinate system.
 */


public class GNFragmentTower {

	private String foot;
	
    // all fragments (including leaves) in this tower
	private Set<String> fragments; 
    
    private Set<String> backboneFragments;
    
    // x-offset of leaf fragments relative to their father
	private Map<String,Integer> leafXOffsets;
    
    // fragment -> the leaves that belong to this fragment
    private Map<String, List<String>> leavesForFragment;
	
	private Map<String,Integer> yPositions;
	private Map<String, Integer> fragmentToHeight;
	
	private int maximalWidth;
    
    
    private int placedAtX, placedAtY;
    
    // the set of fragments that are dominance parents of a fragment in
    // the tower, but don't belong to the tower themselves
    private Set<String> dominanceParents;
    
    // the y-position for the lower left corner of the next backbone fragment
    private int nextBackboneY;
    
    // the minimum y-position for the lower left corner of any fragment.
    // This is <= 0; it can be less than zero if the lowest backbone
    // fragment has children.
    private int minYCoordinate;
	
    
    /**
     * Creating a new <code>GNFragmentTower</code>.
     */
	GNFragmentTower() {
		foot = null;
		leafXOffsets = new HashMap<String,Integer>();
		fragmentToHeight = new HashMap<String,Integer>();
		
		dominanceParents = new HashSet<String>();
		fragments = new HashSet<String>();
        backboneFragments = new HashSet<String>();
		yPositions = new HashMap<String, Integer>();
        leavesForFragment = new HashMap<String, List<String>>();
		
		placedAtX = placedAtY = -1;
		
		maximalWidth = 0;
        
        nextBackboneY = 0;
        minYCoordinate = 0;
	}
	
	/**
	 * Add a <code>Fragment</code> to the tower
	 * 
	 * @param frag the fragment to add
	 * @param yPos the y-position of the fragment relative to the tower (from bottom left)
	 * @param height the height of the fragment
	 */
	private void addFragment(String frag, int yPos, int height) {
	
		fragments.add(frag);
		fragmentToHeight.put(frag,height);
		yPositions.put(frag, yPos);
	}
    
	/**
	 * Add a non-leaf fragment to the tower.
	 * 
	 * @param frag
	 * @param width
	 * @param height
	 */
    void addBackboneFragment(String frag, int width, int height) {
        
		addFragment(frag, nextBackboneY, height);
        backboneFragments.add(frag);
        
        nextBackboneY += height + fragmentYDistance;
        
        if( width > maximalWidth) {
            maximalWidth = width;
        }
    }
    
    /**
     * Add a leaf fragment that hangs under belongsTo. The fragment
     * belongsTo must have been added to the tower before.
     * 
     * @param frag
     * @param belongsTo
     * @param xOffset
     * @param width
     * @param height
     */
    void addLeafFragment(String frag, String belongsTo, int xOffset, 
                            int width, int height) {
        Collection<String> leavesOfBelongsTo;
        int ypos = yPositions.get(belongsTo) - fragmentYDistance - height;
        
        addFragment(frag, ypos, height);
        leafXOffsets.put(frag,xOffset);
        
        if( ypos < minYCoordinate ) {
            minYCoordinate = ypos;
        }
        
        
        if( leavesForFragment.containsKey(belongsTo) ) {
            leavesOfBelongsTo = leavesForFragment.get(belongsTo);
        } else {
            leavesOfBelongsTo = new ArrayList<String>();
        }
        
        leavesOfBelongsTo.add(frag);
        
        if( width + xOffset > maximalWidth ) {
            maximalWidth = width + xOffset;
        }
    }        
    
    /**
     * Adding all leaf fragments of a given fragment to
     * the tower. The map contains the leafs amd their x-offsets
     * relative to their parent fragment.
     * 
     * @param fragsWithOffsets the leafs mapped to their offsets
     * @param belongsTo the parent fragment
     * @param widths the leafs mapped to their widths
     * @param heights the leafs mapped to their heights
     */
    void addAllLeafFragments(Map<String,Integer> fragsWithOffsets, 
                                String belongsTo,  
                                Map<String,Integer> widths,
                                Map<String,Integer> heights
                                ) {
        for( String frag : fragsWithOffsets.keySet() ) {
            addLeafFragment(frag, belongsTo, fragsWithOffsets.get(frag).intValue(),
                                widths.get(frag), heights.get(frag));
        }
    }
	
	/**
	 * Place all fragments of this tower into
	 * given maps for y- and x-positions.
	 * 
	 * @param xStart x-coordinate of the left upper tower corner
	 * @param yStart y-coordinate of the left upper tower corner
	 * @param xMap the map to write the x-positions in
	 * @param yMap the map to write the y-positions in
	 */
	void place(int xStart, int yStart, 
					Map<String,Integer> xMap, Map<String,Integer> yMap) {
        int myX;
        
		// iterate over all fragments (including leaves) and place them
        for( String frag : fragments ) {
            // compute my y position (NB yPositions counts from bottom)
            int myY = yStart + getHeight() + minYCoordinate
                - (yPositions.get(frag) + fragmentToHeight.get(frag));
            
            yMap.put(frag, myY);
            
            // compute x position
            if(leafXOffsets.containsKey(frag)) {
                // leaf fragments are moved right by the offset
                myX = xStart + leafXOffsets.get(frag);
            } else {
                // backbone fragments just use the given x-position
                myX = xStart;
            }
            
            xMap.put(frag, myX);
        }
        
        placedAtX = xStart;
        placedAtY = yStart;
    }
    
	/**
	 * TODO comment me!
	 * 
	 * @param xoff
	 * @param yoff
	 * @param xMap
	 * @param yMap
	 */
    public void translate(int xoff, int yoff,
                           Map<String,Integer> xMap, Map<String,Integer> yMap) {
        
		for( String frag : fragments ) {
            if( xoff != 0 ) {
				int xMovement = xoff;
				if(xMap.containsKey(frag)) {
					xMovement += xMap.remove(frag);
				}
				xMap.put(frag, xMovement);
            }
            if( yoff != 0 ) {
				int yMovement = yoff;
				if(yMap.containsKey(frag)) {
					yMovement += yMap.remove(frag);
				}
                yMap.put(frag, yMovement);
            }
        }
    }
    
    /**
     * Compute the bounding box of the tower. If the tower hasn't been
     * placed at a specific screen position yet, the box will start at
     * (0,0).
     * 
     * @return the bounding box
     */
    public Rectangle getBox() {
        if( placedAtX < 0 ) {
          
            return new Rectangle(0, 0, maximalWidth, getHeight());
        } else {
            return new Rectangle(placedAtX, placedAtY, maximalWidth, getHeight());
        }
    }
    
    /**
     * @return the tower's lowest fragment
     */
	String getFoot() {
		return foot;
	}
	
	/**
	 * 
	 * @param base
	 */
	void setFoot(String base) {
		this.foot = base;
	}
	
	/**
	 * 
	 * @return all dominance parents of the tower
	 */
    public Set<String> getDominanceParents() {
        return dominanceParents;
    }
    
    /**
     * Add a dominance father to the tower
     * @param parent the parent to add
     */
    public void addDominanceParent(String parent) {
        dominanceParents.add(parent);
    }
    
    

    /**
     * 
     * @return the tower fragments
     */
	Set<String> getFragments() {
		return fragments;
	}
	
	/**
	 * 
	 * @return the (maximal) with of the tower
	 */
	int getWidth() {
		return maximalWidth;
	}
	

	/**
	 * 
	 * @return the height of the tower
	 */
	int getHeight() {
        return nextBackboneY - fragmentYDistance - minYCoordinate;
	}
	

	/**
	 * 
	 * @return true if there are no fragments in the tower
	 */
	public boolean isEmpty() {
        return fragments.isEmpty();
	}
	
}
