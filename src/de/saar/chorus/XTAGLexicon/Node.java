import java.io.*;
import java.util.*;

public abstract class Node {
    
    // Kategorie
    protected String cat;
    // Index
    protected String index;
    // Mutter
    protected Node mother;
    //Adresse
    protected String address;

    protected boolean isAdj;

    public Node(String cat, String index) {
	this.cat = cat;
	this.index = index;
	this.mother = null;
    }

  
    public Node(String cat, String index, Node mother) {
	this.cat = cat;
	this.index = index;
	this.mother = mother;

	if (mother != null) {
	    mother.addChild(this);
	}
    }

    public boolean isAdj() {
	return false; 
    }

    public boolean isRoot() {
	return mother == null; 
    }

    public void setMother (Node node) {
	mother = node;
    }

    public Node getMother () {
	return mother;
    }

    public void setAddress (String add) {
	address = add;
    }

    public String getAddress () {
	return address;
    }

    public String getCat (){
	return cat;
    }

    public void setWord(String newIndex){
	index = newIndex;
    }

    public boolean isLeftAux (){
	return false;}
    public boolean isRightAux () {
	return false;}

    public void replaceChild(Node child,Node newChild){}

    public abstract List<Node> getChildren ();

    public abstract Node copyAndReplace(List<Anchor> anchors, String lookUp);

    public void lexicalize(List<Node> nodes, String lookUp){}
    public void printXDGInBuffer(StringBuffer result, String distance) {}

    public abstract void printXML();
    public abstract void printXMLInBuffer(StringBuffer result, String distance);
    public abstract void printLisp();
    public abstract void printLispInBuffer(StringBuffer result);
    public abstract void addChild(Node node);
}
