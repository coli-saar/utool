/**
 * @file   XMLTreePanel.java
 * @author Alexander Koller
 * @date   Wed May 21 15:23:58 2003
 * 
 * @brief  A subclass of JPanel that displays an XML document as a tree.
 * 
 * TODO: drawSubtree() doesn't work properly if the document contains comments!!
 * 
 */

package de.saar.coli.chorus.xmltree;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;
import java.io.*;

import electric.xml.*;


/**
 * A specialized JPanel that contains the image of an XML document as a tree.
 *
 * This panel is typically much larger than the displayable screen size, and
 * cannot be scrolled on its own. It can, however, be inserted into a scrollpane.
 */

class XMLTreePanel extends JPanel {
    private Document doc;

    private static final int xgap = 10;         //< Horizontal gap between adjacent subtrees. 
    private static final int ygap = 30;         //< Vertical separation between nodes.

    private static final int xLeftMargin = 30;  //< Margin left open on the right and left.

    private Class 
	elementClass,                            //< The class electric.xml.Element.
	textClass;                               //< The class electric.xml.Text.

    private Graphics2D g2;                       //< Graphics context from the last call of paint().
    private FontMetrics fontmetrics;             //< Font metrics for the panel.



    /** This class encapsulates the dimensions of a subtree. */
    class SubtreeDimensions {
	public int 
	    width, //< width of the subtree
	    height, //< maximum Y coordinate of the subtree (this is not really the height!)
	    mid;   //< x-position where the incoming line should be attached

	SubtreeDimensions(int width, int height, int mid) {
	    this.width = width;
	    this.mid = mid;
	    this.height = height;
	}
    };
	    



    /**
     * Constructs an XMLTreePanel from an XML document.
     *
     * @param doc the XML document as provided by Electric XML.
     */
    XMLTreePanel(Document doc) {
	this.doc = doc;
	fontmetrics = getFontMetrics(getFont());

	try {
	    elementClass = Class.forName("electric.xml.Element");
	    textClass = Class.forName("electric.xml.Text");
	} catch(Exception e) {
	    e.printStackTrace();
	    System.exit(1);
	}

	// compute initial dimensions
	g2 = null;
	SubtreeDimensions dim = drawSubtree(doc.getRoot(), xLeftMargin, ygap);
	setPreferredSize(new Dimension(2*xLeftMargin + dim.width, dim.height));
    }


    
    /**
     * Draws the document in the panel.
     *
     * This method is called by Swing every time the panel is redrawn.
     *
     * @param g the graphics context to draw into.
     */
    public void paint(Graphics g) {
	super.paint(g);
	
	int xNextElement = xLeftMargin;
	Element root = doc.getRoot();
	g2 = (Graphics2D) g;

	SubtreeDimensions dim = drawSubtree(root, xLeftMargin, ygap);
	g2.drawString(root.getName(), 
		      dim.mid - fontmetrics.stringWidth(root.getName())/2,
		      ygap - fontmetrics.getDescent() - 2);

    }


    

    /**
     * Draws the subtree starting in a given XML element.
     *
     * @param e the root of the subtree that should be drawn.
     * @param xpos X coordinate of the top left corner of where the subtree should be drawn.
     * @param ypos Y coordinate of the top left corner of where the subtree should be drawn.
     * @return the dimensions of the newly drawn subtree.
     */
    private SubtreeDimensions drawSubtree(Element e, int xpos, int ypos) {
	Children children = e.getChildren();
	int xNextElement = xpos;
	int xFirstChild = xpos, xLastChild = xpos;
	int idx = 0;
	int maxChildrenHeight = 0;

	Child sub = children.first();

	if( (sub != null) && (sub.getClass() == textClass) && (children.size() == 1) ) {
	    // If my only child is a text leaf, I don't need to draw a line
	    // for it ...
	    return drawCData(((Text) sub).getString(), xpos, ypos);
	}

	else {
	    // ... otherwise, I do. Iterate through all subtrees, draw them,
	    // and then draw a connecting horizontal line over all of them.
	    
	    while(children.hasMoreElements()) {
		SubtreeDimensions dims;
		boolean draw = false;
		sub = children.next();

		if( sub.getClass() == elementClass ) {
		    Element subAsEl = (Element) sub;
		    
		    dims = drawSubtree(subAsEl, xNextElement, ypos + ygap);
		    
		    // edge label for connecting vertical line
		    if( g2 != null )
			g2.drawString(subAsEl.getName(), dims.mid+1, ypos+(ygap/2));

		    draw = true;
		} else if( sub.getClass() == textClass ) {
		    // We ignore comments etc. (which are neither of class Text
		    // nor Element)
		    String cdata = ((Text) sub).getString();
		    dims = drawCData(cdata, xNextElement, ypos + ygap);
		    draw = true;
		} else {
		    dims = new SubtreeDimensions(0, ypos, xpos);
		}
			
		if( draw ) {
		    // connecting vertical line
		    if(  (g2 != null) )
			g2.draw(new Line2D.Double(dims.mid, ypos, dims.mid, ypos+ygap)); 
		    
		    if( (idx++) == 0 )
			xFirstChild = dims.mid;
		    xLastChild = dims.mid;
		    
		    xNextElement += dims.width + xgap;

		    if( maxChildrenHeight < dims.height )
			maxChildrenHeight = dims.height;
		}
	    }

	    if( idx > 0 )
		xNextElement -= xgap; // no gap after last element

	    if( g2 != null )
		// horizontal connector
		g2.draw(new Line2D.Double(xFirstChild, ypos, xLastChild, ypos)); 
		
	    return new SubtreeDimensions(xNextElement - xpos, 
					 maxChildrenHeight,
					 (xLastChild + xFirstChild) / 2);
	}
    }

    /**
     * Draws the contents of an XML text node.
     *
     * @param s the string that should be drawn.
     * @param xpos X position at which the string should be drawn.
     * @param ypos Y position of the top left corner of the string.
     * @return the dimensions of the text-node subtree.
     */
    private SubtreeDimensions drawCData(String s, int xpos, int ypos) {
	String cdata = (s==null)?"*":s;

	if( g2 != null )
	    g2.drawString(cdata, xpos, ypos + fontmetrics.getAscent());

	return new SubtreeDimensions(fontmetrics.stringWidth(cdata),
				     ypos + fontmetrics.getHeight(),
				     xpos + fontmetrics.stringWidth(cdata)/2);
    }
    


}
