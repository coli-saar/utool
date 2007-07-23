package de.saar.chorus.ubench.gui;

import java.awt.AlphaComposite;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;

import javax.swing.JPanel;

import org.jgraph.graph.DefaultGraphCell;

/**
 * This is stolen from Romain Guy.
 * 
 * @author Michaela
 *
 */
public class MovingDefaultGraphCellPanel extends JPanel {
	
	private AlphaComposite composite;
    private DefaultGraphCell dragged = null;
    private Point location = new Point(0, 0);
    
    public MovingDefaultGraphCellPanel() {
    	setOpaque(false);
        composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);
    }
    
    public void setDefaultGraphCell(DefaultGraphCell dragged)
    {
        this.dragged = dragged;
    }

    public void setPoint(Point location)
    {
        this.location = location;
    }

    public void paintComponent(Graphics g)
    {
        if (dragged == null)
            return;

        Graphics2D g2 = (Graphics2D) g;
        g2.setComposite(composite);
        
        String toPaint = dragged.toString();
        FontMetrics metrics = g2.getFontMetrics();
        g2.drawString(toPaint, 
        		(int) location.getX() - metrics.stringWidth(toPaint), 
        		(int) location.getY() - 15);
    }
}
