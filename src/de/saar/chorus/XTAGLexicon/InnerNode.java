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
    
    public int hashCode(){
	int hashCode = cat.hashCode();
	for (Iterator<Node> it = children.iterator(); 
	     it.hasNext();){
	    hashCode = hashCode + it.next().hashCode();}
	return hashCode;
    }

    public boolean equals(Object o){
	if (o instanceof InnerNode){
	    if (((InnerNode)o).getCat().equals(cat)){
		List<Node> compareChildren = ((InnerNode)o).getChildren();
		if (compareChildren.size() == children.size()){
		    for (int i=0; i>children.size(); i++){
			if (!children.get(i).equals(compareChildren.get(i))){
			    return false;
			}
		    }
		    return true;
		}
		else 
		    {return false;}
	    }
	    else 
		{return false;}
	}
	else 
	    {return false;}
    }
   
    /**
     * Kopieren und ersetzen der Ankerknoten
     */
    public Node copyAndReplace(List<Anchor> anchors, String lookUp){
	Node copiedNode = new InnerNode(cat, index);
	for (Iterator<Node> it = children.iterator();
	     it.hasNext();){
	    Node copiedChild = it.next().copyAndReplace(anchors, lookUp);
	    copiedNode.addChild(copiedChild);}
	return copiedNode;
    }


    public boolean terminalMustBeReplaced(Node onlyChild, 
					  String childCat, 
					  String lookUp){
	if (onlyChild instanceof TerminalNode && !childCat.equals(lookUp)
	       && !childCat.equals("PRO") && !childCat.equals(null)
	       && !childCat.equals("")){
	       return true;}
	   else {return false;}
    }

    /**
     * Lexikalisierung der Baeume
     */
    public void lexicalize (List<Node> nodes, String lookUp){
	if (children.size() == 1){
	    Node onlyChild = children.get(0);
	    String childCat = onlyChild.getCat();
	    if (this.terminalMustBeReplaced(onlyChild, childCat, lookUp)){
		mother.replaceChild(this, 
				    new SubstitutionNode(cat+"/"+childCat, index));
		Node newMother = new InnerNode(cat+"/"+childCat, index);
		newMother.addChild(this);
		if (!nodes.contains(newMother)){
		    nodes.add(newMother);}
	    }
	}
	else{
	    List<Node> innerNodeChildren = new ArrayList<Node>();
	    for (Iterator<Node> it = children.iterator(); it.hasNext();){
		Node child = it.next();
		if (child instanceof InnerNode){
		    innerNodeChildren.add(child);}
	    }
	    for (Iterator<Node> it = innerNodeChildren.iterator();
		 it.hasNext();){
		it.next().lexicalize(nodes, lookUp);
	    }
	}/**
	    else{
	    for(int i=0; i==children.size(); i++){
	    Node child = children.get(i);
	    if (child instanceof InnerNode){
	    child.lexicalize(nodes, lookUp);}
	    }
	    }*/
    }
    
		

    /**
     * Fuegt ein Kind hinzu und vermerkt im Kind,
     * das dieser Knoten die Mutter ist
     */
    
    public void addChild(Node node) {
	node.setMother(this);
	children.add(node);
    }
    

    /**
     * Ersetzt child durch newChild
     */
    public void replaceChild(Node child, Node newChild){
	int position = children.indexOf(child);
	children.remove(child);
	children.add(position, newChild);
	newChild.setMother(this);
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
