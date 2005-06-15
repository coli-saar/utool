import java.io.*;
import java.util.*;

public final class TerminalNode extends LeafNode {
    
   public TerminalNode(String cat, String index, Node mother) {
	super(cat, index, mother);
    }

    public TerminalNode(String cat, String index) {
	super(cat, index);
    }

    /**
     * determines, wether this Node equals an Object 
     * @param o the Object
     * @return true, if this Node and the Object are the same
     */
    public boolean equals(Object o){
	if (o instanceof TerminalNode){
	    return ((TerminalNode)o).getCat().equals(cat);}
	else { return false;}
    }
    
    /**
     * tests, if this Terminal is an anchor
     * @return true, if this Terminal is an anchor
     */
    public boolean isAnchor(){
	return (! (cat.equals("") || cat.equals("PRO") || cat.equals(null)));}
 
    /**
     * copy the node 
     * @param anchors the anchors
     * @param lookUp the word the user is searching for
     * @return the copied node
     */ 
    public Node copyAndReplace(List<Anchor> anchors, String lookUp){
	return new TerminalNode(cat, index);
    }

    public void setCat(String newCat){
	this.cat = newCat;
    }

    /** 
     * Tests, if this node is an empty TerminalNode
     * @return true, if it is an empty Terminal
     */
    public boolean containsEmpty() {
	return cat.equals("");
    }

    /**
     * tests, if this node is an empty TerminalNode
     * and the cat of the mother is equal to a given cat
     * @param mothercat the cat of the mother
     * @return true, if it is an empty Terminal
     * and the cat of the mother equals mothercat
     */
    public boolean containsEmpty(String mothercat) {
	return cat.equals("") && mothercat.equals(mother.cat);
    }
   
    /**
     * print the node in a StringBuffer xml-style
     * @param result the StringBuffer to print into
     * @param distance an argument used for the proper indention
     */
    public void printXMLInBuffer(StringBuffer result, String distance) {
	result.append(distance+"<terminal cat=\""+cat+"\"/>");
    }

    /**
     * print node to the command-line lisp-style
     */
    public void printLisp() {
	System.out.print(cat);
    }

    /**
     * print the node in a StringBuffer lisp-style
     * @param result the StringBuffer to print into
     */
    public void printLispInBuffer(StringBuffer result) {
	result.append(cat);
    }

}
