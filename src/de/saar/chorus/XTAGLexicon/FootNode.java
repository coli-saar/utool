package de.saar.chorus.XTAGLexicon;

import java.io.*;
import java.util.*;

public final class FootNode extends LeafNode {


    public FootNode(String cat, String index, Node mother) {
	super(cat, index, mother);
    }

    public FootNode(String cat, String index) {
	super(cat, index);
    }

    /**
     * determines, wether this Node equals an Object 
     * @param o the Object
     * @return true, if this Node and the Object are the same
     */
    public boolean equals(Object o){
	if (o instanceof FootNode){
	    return ((FootNode)o).getCat().equals(cat);}
	else { return false;}
    }

    /**
     * copy the node  
     * @param anchors the anchors
     * @param lookUp the word the user is searching for
     * @return the copied node
     */ 
    public Node copyAndReplace(List<Anchor> anchors, String lookUp) {
	return new FootNode(cat, index);
    }

    
    /**
     * print the node in a StringBuffer xml-style
     * @param result the StringBuffer to print into
     * @param distance an argument used for the proper indention
     */
    public void printXMLInBuffer(StringBuffer result, String distance) {
	result.append(distance+"<foot cat=\""+cat+"\"/>");;
    }

    /**
     * print node to the command-line lisp-style
     */
    public void printLisp() {
	System.out.print("*");
	System.out.print(cat);
    }

    /**
     * print the node in a StringBuffer lisp-style
     * @param result the StringBuffer to print into
     */
    public void printLispInBuffer(StringBuffer result) {
	result.append("*");
	result.append(cat);
    }

}

