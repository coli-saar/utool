package de.saar.chorus.XTAGLexicon;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class TreeHandler extends DefaultHandler {
    
    // Das Lexikon
    private Lexicon lexicon;
    // Der aktuelle Knoten
    private Node node;
    // Der aktuelle Baumname
    private String treename;
    
    // der aktuelle Baum
    private Tree tree;
    
    //
    // Konstruktor
    //
    
    public TreeHandler(Lexicon lexicon) {
        this.lexicon = lexicon;
        this.node = null;
        this.treename = null;
    }
    
    public void startElement(String namespaceURI,
            String sName,
            String qName,
            Attributes attrs)
    throws SAXException
    {
        String name = sName.equals("") ? qName : sName;
        
        if (name.equals("tree")) {
            treename = attrs.getValue("id");
            tree = new Tree(treename);
        }
        else if (name.equals("node")) {
            String cat = attrs.getValue("cat");
            String index = attrs.getValue("index");
            node = new InnerNode(cat, index, node);
            tree.addNode(node);
        }
        else if (name.equals("leaf")) {
            String type = attrs.getValue("type");
            String cat = attrs.getValue("cat");
            String index = attrs.getValue("index");
            
            if (type.equals("substitution")) {
                Node n = new SubstitutionNode(cat, index); 
                tree.addNode(Tree.xtagNodeName(cat,index), n);
                add(n);
            }
            else if (type.equals("foot")) {
                Node n = new FootNode(cat, index); 
                tree.addNode(Tree.xtagNodeName(cat,index), n);
                add(n);
            }
            else if (type.equals("anchor")) {
                if (index == null) {
                    Node n = new AnchorNode(cat, index); 
                    tree.addNode(Tree.xtagNodeName(cat,index), n);
                    add(n);
                } else {
                    Node n = new AnchorNode(cat + index, index); 
                    tree.addNode(Tree.xtagNodeName(cat,index), n);
                    add(n);
                }
            }
            else if (type.equals("terminal")) {
                Node n = new TerminalNode(cat, index); 
                tree.addNode(Tree.xtagNodeName(cat,index), n);
                add(n);
            }
            else {
                throw new Error("invalid node type");
            }
        }
    }
    
    public void endElement(String namespaceURI,
            String sName,
            String qName)
    throws SAXException
    {
        String name = sName.equals("") ? qName : sName;
        
        if (name.equals("tree")) {
            lexicon.addTree(tree);
            node = null;
        }
        else if (name.equals("node")) {
            if (node.isRoot() == false) {
                node = node.getMother();
            }
        }
    }
    
    public void add(Node child) {
        if (node != null) {
            node.addChild(child);
        } else {
            node = child;
        }
    }
}
