package de.saar.swing;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
/*
 * @(#)JStandardFrame.java created 25.08.2005
 * 
 * Copyright (c) 2005 Alexander Koller
 *  
 */

public class JStandardFrame extends JFrame {
    public JStandardFrame(String title) {
        super(title);
        
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
    }
}
