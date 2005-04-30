import java.io.*;
import java.util.*;

public final class TerminalNode extends LeafNode {
    
   public TerminalNode(String cat, String index, Node mother) {
	super(cat, index, mother);
    }

    public TerminalNode(String cat, String index) {
	super(cat, index);
    }

    public boolean equals(Object o){
	if (o instanceof TerminalNode){
	    return ((TerminalNode)o).getCat().equals(cat);}
	else { return false;}
    }
    
    public boolean isAnchor(){
	return (! (cat.equals("") || cat.equals("PRO") || cat.equals(null)));}
 

    public Node copyAndReplace(List<Anchor> anchors, String lookUp){
	return new TerminalNode(cat, index);
    }

    public void setCat(String newCat){
	this.cat = newCat;
    }
   
    //public String getCat(){
    //return cat;
    //}

    public void printXML() {
	throw new Error("Not implemented");
    }

    public void printXMLInBuffer(StringBuffer result, String distance) {
	result.append(distance+"<terminal cat=\""+cat+"\"/>");
    }

    public void printLisp() {
	System.out.print(cat);
    }

    public void printLispInBuffer(StringBuffer result) {
	result.append(cat);
    }

 public List<Node> getChildren(){
	throw new Error ("oops");
    }
}
