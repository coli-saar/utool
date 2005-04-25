import java.io.*;
import java.util.*;

public final class AnchorNode extends LeafNode {
    
    public AnchorNode (String cat, String index, Node mother) {
	super(cat, index, mother);
    }

    public AnchorNode (String cat, String index) {
	super(cat, index);
    }


    public boolean equals(Object o){
	if (o instanceof AnchorNode){
	    return ((AnchorNode)o).getCat().equals(cat);}
	else { return false;}
    }

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
		    newTerminalNode.setCat(anchor.getRoot());}
	    }
	}
	newAnchorNode.addChild(newTerminalNode);
	return newAnchorNode;
    }


    public void printXML() {
	throw new Error("Not implemented");
    }

 public void printXMLInBuffer(StringBuffer result, String distance) {
	result.append(distance+"<anchor cat=\""+cat+"\" word=\""+index+"\"/>");
    }

    public void printLisp() {
	System.out.print("(");
	System.out.print(cat);
	System.out.print(" ");
	System.out.print(index+")");
    }

 public void printLispInBuffer(StringBuffer result) {
	result.append("(");
	result.append(cat);
	result.append(" ");
	result.append(index+")");
    }

    public String getCat (){
	return cat;
    }

    public void setWord(String newIndex){
	index = newIndex;
    }

 public List<Node> getChildren(){
	throw new Error ("oops");
    }

}
