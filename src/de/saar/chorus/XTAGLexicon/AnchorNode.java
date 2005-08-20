package de.saar.chorus.XTAGLexicon;

import java.io.*;
import java.util.*;


public final class AnchorNode extends LeafNode {
	
	public AnchorNode (String cat, String index, Node mother) {
		super(cat, index, mother);
	}
	
	public AnchorNode (String cat, String index) {
		super(cat, index);
	}
	
	/**
	 * determines, wether this Node equals an Object 
	 * @param o the Object
	 * @return true, if this Node and the Object are the same
	 */
	public boolean equals(Object o){
		if (o instanceof AnchorNode){
			return ((AnchorNode)o).getCat().equals(cat);}
		else { return false;}
	}
	
	/**
	 * replace this AnchorNode by its anchor 
	 * @param anchors the anchors
	 * @param lookUp the word the user is searching for
	 * @return the replacement
	 */ 
	public Node copyAndReplace(List<Anchor> anchors, String lookUp){
		Node newAnchorNode = new InnerNode(this.cat, this.index);
		TerminalNode newTerminalNode = new TerminalNode(cat, null);
		
		for (Iterator<Anchor> it = anchors.iterator();
		it.hasNext();) {
			Anchor anchor = it.next();
			if (anchor.getPos().equals(cat)){
				newTerminalNode.setIsAnchor(true);
				if (anchor.isSpecial()){
					newTerminalNode.setCat(lookUp);
				}
				else {
					newTerminalNode.setCat(anchor.getStem());}
			}
		}
		newAnchorNode.addChild(newTerminalNode);
		return newAnchorNode;
	}
	
	/**
	 * print the node in a StringBuffer xml-style
	 * @param result the StringBuffer to print into
	 * @param distance an argument used for the proper indention
	 */
	public void printXMLInBuffer(StringBuffer result, String distance) {
		result.append(distance+"<anchor cat=\""+cat+"\" word=\""+index+"\"/>");
	}

	public void printXML(Writer result, String distance) throws IOException {
		result.append(distance+"<anchor cat=\""+cat+"\" word=\""+index+"\"/>");
	}

	/**
	 * print node to the command-line lisp-style
	 */
	public void printLisp() {
		System.out.print("(");
		System.out.print(cat);
		System.out.print(" ");
		System.out.print(index+")");
	}
	/**
	 * print the node in a StringBuffer lisp-style
	 * @param result the StringBuffer to print into
	 */
	public void printLispInBuffer(StringBuffer result) {
		result.append("(");
		result.append(cat);
		result.append(" ");
		result.append(index+")");
	}

	public String getAnchor() {
		return getCat();
	}

    /*
	for (Iterator<Anchor> it = anchors.iterator();
	     it.hasNext();) {
	    Anchor anchor = it.next();
	    if (anchor.getPos().equals(cat)){
		newTerminalNode.setIsAnchor(true);
		if (anchor.isSpecial()){
		    newTerminalNode.setCat(lookUp);
		}
		else {
		    newTerminalNode.setCat(anchor.getStem());}
	    }
        */

	public String toString() {
		return "A:" + getCat();
	}
	
}
