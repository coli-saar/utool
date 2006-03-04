/*
 * @(#)JGraphSlider.java created 04.03.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.jgraph;

import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jgraph.JGraph;

public class JGraphSlider extends JSlider implements ChangeListener {
    private JGraph graph;
    
    public JGraphSlider(JGraph graph) {
        super(JSlider.VERTICAL, 0, 100, 100);
        
        this.graph = graph;
        setToolTipText("Zoom: " + getValue() + "%");
        
        /**
         * TODO find out how to disable "scrolling"
         * with up/down resp. left/right.
         */
        resetKeyboardActions();
        
        addChangeListener(this);
    }
    

    public void stateChanged(ChangeEvent e) {
        
        int scale = getValue();
        setToolTipText("Zoom: " + scale + "%");
        graph.setScale((double) scale/100);
    }
}
