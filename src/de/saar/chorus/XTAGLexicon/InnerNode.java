import java.io.*;
import java.util.*;

public final class InnerNode extends Node {
    
    private List<Node> children;

    public InnerNode(String cat, String index, Node mother) {
	super(cat, index, mother);
	children = new ArrayList<Node>();
    }

    public InnerNode(String cat, String index) {
	super(cat, index);
	children = new ArrayList<Node>();
    }
    
    /**
     * Kopieren
     */
    
    
    public Node copyAndReplace(List<Anchor> anchors, String lookUp){
	Node copiedNode = new InnerNode(cat, index);
	for (Iterator<Node> it = children.iterator();
	     it.hasNext();){
	    Node copiedChild = it.next().copyAndReplace(anchors, lookUp);
	    copiedNode.addChild(copiedChild);}
	return copiedNode;
    }

    /**
     * Fuegt ein Kind hinzu und vermerkt im Kind,
     * das dieser Knoten die Mutter ist
     */
    
    public void addChild(Node node) {
	node.setMother(this);
	children.add(node);
    }
    
    
    public void deleteChild(Node node){
	children.remove(node);
    }

    /**
     * Rekursive Ausgabe des Baums (XML)
     */

    public void printXML() {
	System.out.println("InnerNode.printXML: not implemented.");
    }


    public void printXMLInBuffer(StringBuffer result, String distance) {
	result.append(distance+"<node cat=\""+cat+"\">\n");
	for (Iterator<Node> it = children.iterator(); it.hasNext();){
	    it.next().printXMLInBuffer(result, (distance+" "));
	    result.append("\n");}
	result.append(distance+"</node>");
    }


    /**
     * Rekursive Ausgabe des Baums (Lisp-artig)
     */
    
    public void printLisp() {
	System.out.print("(");
	System.out.print(cat);
	for (Node child : children) {
	    System.out.print(" ");
	    child.printLisp();
	}
	System.out.print(")");
    }
    
  public void printLispInBuffer(StringBuffer result) {
      result.append("(");
	result.append(cat);
	for (Node child : children) {
	    result.append(" ");
	    child.printLispInBuffer(result);
	}
	result.append(")");
    }

    public List<Node> getChildren (){
	return children;
    }

}
