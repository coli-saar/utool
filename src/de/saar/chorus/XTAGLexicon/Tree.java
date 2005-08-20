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
import java.util.List;
import java.util.Map;

public class Tree {
    private class FeatureEquation {
        
    }
    
    private String name;
    
    private List<Node> nodes; // first element is root
    private Map<String,Node> namesToNodes;
    
    private List<FeatureEquation> equations;
    
    public Tree(String name) {
        this.name = name;
        nodes = new ArrayList<Node>();
        namesToNodes = new HashMap<String,Node>();
        equations = new ArrayList<FeatureEquation>();
    }
    
    public void addNode(String name, Node node) {
        nodes.add(node);
        namesToNodes.put(name,node);
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
    
}
