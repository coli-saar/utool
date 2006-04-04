/*
 * @(#)JScrollableJGraph.java created 04.03.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.jgraph;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.jgraph.JGraph;

import de.saar.chorus.ubench.gui.Ubench;

public class JScrollableJGraph extends JPanel {
    private JGraphSlider slider;
    private JScrollPane scrollpane;
    
    public JScrollableJGraph(JGraph graph) {
        super(new BorderLayout());
        
        slider = new JGraphSlider(graph);
        scrollpane = new JScrollPane(graph);
        
        add(scrollpane, BorderLayout.CENTER);
        add(slider, BorderLayout.EAST);
        validate();
    }
    
    /**
     * Aligning the slider with the currently shown graph.
     * (if there is one).
     */
	public void resetSlider() {
		slider.resetSlider();
	}
}
