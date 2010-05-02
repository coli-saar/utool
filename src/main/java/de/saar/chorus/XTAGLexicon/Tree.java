/*
 * @(#)Tree.java created 19.09.2005
 * 
 * Copyright (c) 2005 Alexander Koller
 *  
 */


/*
 * TODO: Move lexicalisation into this class.
 *     Tree lexicalise(LexicalInfo?? word)
 */

package de.saar.chorus.XTAGLexicon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Tree {
    private String name;
    
    private List<Node> nodes; // first element is root
    private Map<String,Node> namesToNodes;
    //private List<Anchor> anchors;
    
    private List<UnificationEquation> equations;
    
    private Set<Tree> strictlyLexicalisedParts; // only used internally in splitIntoStrictlyLexicalisedParts[Rec]
    private Tree strictlyLexMainPart;
    
    public Tree(String name) {
        this.name = name;
        nodes = new ArrayList<Node>();
        namesToNodes = new HashMap<String,Node>();
        equations = new ArrayList<UnificationEquation>();
        //anchors = new ArrayList<Anchor>();
    }
    
    public void addNode(String name, Node node) {
        nodes.add(node);
        namesToNodes.put(name,node);
    }
    
    public void addNode(Node node) {
        addNode(xtagNodeName(node.getCat(), node.getIndex()), node);
    }
    
    public void addEquation(UnificationEquation eq) {
        equations.add(eq);
    }
    
    public List<UnificationEquation> getEquations() {
        return equations;
    }
    
    public Node getRoot() {
        return nodes.get(0);
    }
    
    public Node getNodeForName(String name) {
        return namesToNodes.get(name);
    }
    
    public Collection<Node> getNodes() {
        return nodes;
    }
    
    public String getName() {
        return name;
    }
    
    public static String xtagNodeName(String cat, String index) {
        if( "".equals(cat))
            cat = null;
        
        if( "".equals(index))
            index = null;
        
        if( index == null )
            return cat;
        
        if( cat == null )
            return null;
        
        return cat + "_" + index;
    }
    
    
    public Set<Tree> splitIntoStrictlyLexicalisedParts(String inflectedMainAnchor) {
        strictlyLexicalisedParts = new HashSet<Tree>();
        strictlyLexMainPart = new Tree(name);
        strictlyLexicalisedParts.add(strictlyLexMainPart);
        
        splitIntoStrictlyLexicalisedPartsRec(getRoot(), inflectedMainAnchor);
        
        return strictlyLexicalisedParts;
    }
    
    
    
    private Node splitIntoStrictlyLexicalisedPartsRec(Node node, String inflectedMainAnchor) {
        Node copy = null;
        
        if( node instanceof InnerNode ) {
            Node child = node.getChildren().get(0);
            String childCat = child.getCat();
            
            if( (child instanceof TerminalNode)
                    && (childCat != null)
                    && !childCat.equals(inflectedMainAnchor)
                    && !"PRO".equals(childCat)
                    && !"".equals(childCat) ) {
                // node is a former AnchorNode that has a non-empty terminal child.
                // -> make a new tree for this and replace it with a substitution
                // node in the main tree
                String rootlabel = node.getCat() + "/" + childCat;
                String index = node.getIndex();
                
                InnerNode newRoot = new InnerNode(rootlabel, index);
                TerminalNode newTerm = new TerminalNode(childCat, null);
                newRoot.addChild(newTerm);
                
                Tree newTree = new Tree("split " + name + " at " + rootlabel);
                newTree.addNode(newRoot);
                newTree.addNode(newTerm);
                strictlyLexicalisedParts.add(newTree);
                
                copy = new SubstitutionNode(rootlabel, index);
                strictlyLexMainPart.addNode(copy);
                return copy;
            }
        }
        
        // Otherwise, just copy the node and move on
        copy = node.clone();
        strictlyLexMainPart.addNode(copy);

        if( node instanceof InnerNode ) {
            for( Node child : node.getChildren() ) {
                copy.addChild(splitIntoStrictlyLexicalisedPartsRec(child, inflectedMainAnchor));
            }
        }

        return copy;
    }

    /**
     * Instantiate the anchor nodes of this tree with actual words. The main
     * anchor is treated specially in that its word is inflected and can't be taken
     * from the lexicon directly. So it is passed as an extra parameter. 
     * 
     * @param anchors a list of Anchor entries, i.e. a mapping of node names to words
     * @param inflectedMainAnchor the inflected word form for the main anchor
     * @return a deep copy of this Tree 
     */
    public Tree instantiate(List<Anchor> anchors, String inflectedMainAnchor) {
        Tree ret = new Tree(name + " (" + inflectedMainAnchor + ")");
        Map<String,String> anchorTable = new HashMap<String,String>(); // nodename -> string
        
        for( Anchor anchor : anchors ) {
            if( anchor.isSpecial() ) {
                anchorTable.put(anchor.getReferredNodeName(), inflectedMainAnchor);
            } else {
                anchorTable.put(anchor.getReferredNodeName(), anchor.getStem());
            }
        }
        
        instantiateRec(getRoot(), ret, anchorTable);
        
        return ret;
    }
    
    private Node instantiateRec(Node node, Tree ret, Map<String,String> anchorTable) {
        Node copy = null;
        
        if(node instanceof InnerNode) {
            copy = node.clone();
            ret.addNode(copy);

            for( Node child : node.getChildren() ) {
                copy.addChild(instantiateRec(child, ret, anchorTable));
            }
        } else if( node instanceof AnchorNode ) {
            copy = new InnerNode(node.cat, node.index);
            ret.addNode(copy);
            
            TerminalNode newTerminalNode = new TerminalNode(node.cat, null);
            newTerminalNode.setIsAnchor(true);
            
            newTerminalNode.setCat(anchorTable.get(node.getCat()));
            
            copy.addChild(newTerminalNode);
        } else {
            copy = node.clone();
            ret.addNode(copy);
        }
        
        return copy;
    }
    
    public String toString() {
        return "<tree: " + getRoot() + ">";
    }
}
