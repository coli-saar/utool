package de.saar.chorus.XTAGLexicon;

import java.util.*;
import java.io.*;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

public class TreeHandler extends DefaultHandler {

    // Das Lexikon
    private Lexicon lexicon;
    // Der aktuelle Knoten
    private Node node;
    // Der aktuelle Baumname
    private String tree;

    //
    // Konstruktor
    //

    public TreeHandler(Lexicon lexicon) {
	this.lexicon = lexicon;
	this.node = null;
	this.tree = null;
    }
    
    public void startElement(String namespaceURI,
			     String sName,
                             String qName,
			     Attributes attrs)
	throws SAXException
    {
	String name = sName.equals("") ? qName : sName;

	if (name.equals("tree")) {
	    tree = attrs.getValue("id");
	}
	else if (name.equals("node")) {
	    String cat = attrs.getValue("cat");
	    String index = attrs.getValue("index");
	    node = new InnerNode(cat, index, node);
	}
	else if (name.equals("leaf")) {
	    String type = attrs.getValue("type");
	    String cat = attrs.getValue("cat");
	    String index = attrs.getValue("index");

	    if (type.equals("substitution")) {
		add(new SubstitutionNode(cat, index));
	    }
	    else if (type.equals("foot")) {
		add(new FootNode(cat, index));
	    }
	    else if (type.equals("anchor")) {
		if (index == null) {
		    add(new AnchorNode(cat, index));
		} else {
		    add(new AnchorNode(cat + index, index));
		}
	    }
	    else if (type.equals("terminal")) {
		add(new TerminalNode(cat, index));
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
	    lexicon.addTree(tree, node);
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
