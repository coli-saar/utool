
package de.saar.chorus.xmltree;

import javax.swing.*;
import java.io.*;

import electric.xml.*;




/**
 * A scrollable component that displays an XML document as a tree.
 *
 * You can simply pass an Electric XML document to the constructor and 
 * then add the XMLTree object to some Swing container.
 *
 */
public class XMLTree extends JScrollPane {
    /**
     * Constructs an XMLTree from an XML document.
     *
     * @param d the XML document, as provided e.g. by the Electric XML parser.
     */
    public XMLTree(Document d) {
	super(new XMLTreePanel(d));
    }


    /**
     * A main program to test the class.
     *
     * @param args the command-line arguments. We expect that the first argument
     * is the name of an XML file that should be displayed.
     */
    public static void main(String args[]) {
	Document d;
	try {
	    d = new Document(new File(args[0]));

	    JFrame f = new JFrame("XML");
	    //	    XMLTree x = new XMLTree(d);
	    XMLTreePanel x = new XMLTreePanel(d);
	    JScrollPane p = new JScrollPane(x);
	    f.getContentPane().add(p);

	    f.pack();
	    //	    f.setSize(new Dimension(500,300));
	    f.show();
	} catch(Exception e) {
	    e.printStackTrace();
	}
    }
	

}


	


	

