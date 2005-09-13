/*
 * @(#)GraphScroller.java created 25.08.2005
 * 
 * Copyright (c) 2005 Alexander Koller
 *  
 */

package de.saar.chorus.jgraph;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class GraphScroller extends JPanel implements ActionListener {
    private String apptitle;
    private IGraphSource graphs;
    private Map<ImprovedJGraph,JScrollPane> seen;
    private ImprovedJGraph current;
    
    private JButton next, prev;
    private JTextField indexField;
    private JPanel indexScroller;
    
    private Boolean gotScrollBar;
    /**
     * @param label the label that is displayed before the buttons and text field in the status bar
     * @param graphs a GraphSource
     * @param apptitle if you want the graph scroller to take control of your window title
     * and update it with the current graph name, pass a non-null string for the application
     * title here. 
     */
    public GraphScroller(String label, IGraphSource graphs, String apptitle) {
        super();
        setBackground(Color.WHITE);
        
        this.graphs = graphs;
        this.apptitle = apptitle;
        
        gotScrollBar = false;
        
        seen = new HashMap<ImprovedJGraph,JScrollPane>();
        
        next = new JButton(">");
        next.addActionListener(this);
        
        prev = new JButton("<");
        prev.addActionListener(this);
        
        indexField = new JTextField(5);
        indexField.setText("1");
        indexField.addActionListener(this);
        
        enableButtons();
        
        indexScroller = new JPanel();
        indexScroller.setLayout(new BoxLayout(indexScroller, BoxLayout.X_AXIS));
        indexScroller.add(new JLabel(label + ":"));
        indexScroller.add(prev);
        indexScroller.add(indexField);
        indexScroller.add(next);
        indexScroller.add(new JLabel(" of " + graphs.size()));
        
        setLayout(new BorderLayout());
        add(new JLabel("no graph"),BorderLayout.NORTH);
        add(indexScroller,BorderLayout.SOUTH);
        
        indexScroller.setMaximumSize(new Dimension(1500,indexScroller.getPreferredSize().height));
    }

    public void selectGraph(int idx) {
        current = graphs.get(idx);
        
        removeAll();
        
        setBackground(Color.WHITE);
        validate();
        
        if( seen.containsKey(current) ) {
        	((JFrame) SwingUtilities.getRoot(this)).add(seen.get(current), BorderLayout.NORTH);
        } else {
            JFrame f = new JFrame("JGraph Test");
            f.add(current);
            f.pack();
            
            current.computeLayout();
            current.adjustNodeWidths();

            JScrollPane graphPane = new JScrollPane(current);
            graphPane.setBackground(Color.WHITE);
            add(graphPane, BorderLayout.CENTER);
                
            seen.put(current, graphPane);
        }
        
        
        JFrame f = (JFrame) SwingUtilities.getRoot(this);
        
        if(! gotScrollBar) {
        	f.add(indexScroller,BorderLayout.SOUTH);
        	gotScrollBar = true;
        }
        
        Dimension size = f.getSize();
        
        if( apptitle != null ) {
            if( current.getName() != null ) {
                f.setTitle(current.getName() + " - " + apptitle);
            } else {
                f.setTitle(apptitle);
            }
        }
        f.pack();
      
       
        f.pack();
        f.validate();
        
        f.setSize(size);
        
        f.validate();
    
    }

    
    public void actionPerformed(ActionEvent e) {
        boolean update = false;

        if( e.getSource() == next ) {
            indexField.setText(new Integer(currentIndex() + 1).toString());
            update = true;
        } else if( e.getSource() == prev ) {
            indexField.setText(new Integer(currentIndex() - 1).toString());
            update = true;
        } else if( e.getSource() == indexField ) {
            // TODO check that the value is valid
            update = true;
        }
        
        if( update ) {
            selectGraph(currentIndex()-1);
            enableButtons();
        }
    }
    
    private int currentIndex() {
        return Integer.parseInt(indexField.getText());
    }
    
    private void enableButtons() {
        prev.setEnabled(currentIndex() > 1);
        next.setEnabled(currentIndex() < graphs.size());
    }

}
