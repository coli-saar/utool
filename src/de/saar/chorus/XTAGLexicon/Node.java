package de.saar.chorus.XTAGLexicon;


import java.io.IOException;
import java.io.Writer;
import java.util.List;

public abstract class Node implements Cloneable {
    
    // Kategorie
    protected String cat;
    // Index
    protected String index;
    // Mutter
    protected Node mother;
    // Adresse
    protected String address;
    // is adjunction possible?
    // true only for special InnerNodes
    protected boolean isAdj;
    // is the node an anchor
    // true only for TerminalNodes bearing
    // a looked up word
    protected boolean isAnchor = false;
    
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
    
    
    /**
     * tests, if the node contains an empty TerminalNode
     * (only overwritten in TerminalNode and InnerNode)
     * @return true, if the node contains an empty Terminal
     */
    public boolean containsEmpty() {
        return false;
    }
    
    /**
     * tests, if the node contains an empty TerminalNode
     * and the cat of the mother is equal to a given cat
     * (only overwritten in TerminalNode and InnerNode)
     * @param mothercat the cat of the mother
     * @return true, if the node contains an empty Terminal
     * and the cat of the mother equals mothercat
     */
    public boolean containsEmpty(String mothercat) {
        return false;
    }
    
    /**
     * only overwritten in InnerNode
     */
    public boolean isAdj() {
        return false; 
    }
    
    /**
     * only overwritten in TerminalNode
     */
    public boolean isAnchor(){
        return isAnchor;
    }
    
    /**
     * tests if the node is a root
     */
    public boolean isRoot() {
        return mother == null; 
    }
    
    public void setIsAnchor (boolean ia){
        isAnchor = ia;
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
    
    public String getIndex() {
        return index;
    }
    
    public void setWord(String newIndex){
        index = newIndex;
    }
    
    /**
     * tests, if the node is a left-aux-tree
     * (only overwritten in InnerNode)
     * @return true, if the node is a left-aux-tree
     */
    public boolean isLeftAux (){
        return false;}
    
    /**
     * tests, if the node is a right-aux-tree
     * (only overwritten in InnerNode)
     * @return true, if the node is a right-aux-tree
     */
    public boolean isRightAux () {
        return false;}
    
    /**
     * replace a child by another child
     * (only overwritten in InnerNode)
     * @param child the old child
     * @param newChild the new child
     */
    public void replaceChild(Node child,Node newChild){}
    
    /**
     * get the children of the node
     * (only useful in InnerNode)
     * @return the children
     */
    public abstract List<Node> getChildren ();
    
    /**
     * copy the node and replace all AnchorNodes by their
     * anchors 
     * @param anchors the anchors
     * @param lookUp the word the user is searching for
     * @return the copied node
     * 
     * @obsolete This is now done by Tree.lexicalise
     * 
    public abstract Node copyAndReplace(List<Anchor> anchors, String lookUp);
    */
    
    /**
     * lexicalize the trees
     * (only overwritten in InnerNode)
     * @param nodes the initially empty set of trees, that
     * are generated by this method
     * @param lookUp the word the user is searching for
     */
    public void lexicalize(List<Node> nodes, String lookUp){}
    
    /**
     * print the node in a StringBuffer xml-style
     * @param result the StringBuffer to print into
     * @param distance an argument used for the proper indention
     */
    public void printXDGInBuffer(StringBuffer result, String distance) {}
    
    
    /**
     * print the node in a StringBuffer xml-style
     * @param result the StringBuffer to print into
     * @param distance an argument used for the proper indention
     */
    public abstract void printXMLInBuffer(StringBuffer result, String distance);
    
    public abstract void printXML(Writer result, String distance) throws IOException;
    
    /**
     * print node to the command-line lisp-style
     */
    public abstract void printLisp();
    
    /**
     * print the node in a StringBuffer lisp-style
     * @param result the StringBuffer to print into
     */
    public abstract void printLispInBuffer(StringBuffer result);
    
    /**
     * adds a child to the node
     * @param node the child
     */
    public abstract void addChild(Node node);
    
    /**
     * returns the anchor of this tree (or null if it doesn't have one)
     * @return
     */
    public abstract String getAnchor();
    
    public abstract String toString();
    
    public abstract Node clone();
    
    public String getName() {
        return Tree.xtagNodeName(cat,index);
    }
}
