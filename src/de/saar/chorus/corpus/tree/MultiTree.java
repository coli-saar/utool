/**
 * @file   MultiTree.java
 * @author Alexander Koller
 * @date   Wed May 21 13:48:08 2003
 * 
 * @brief  A MultiTree represents a collection of trees on different levels of
 * representation for the same sentence.
 * 
 * 
 */


package de.saar.chorus.corpus.tree;

import java.util.*;

import electric.xml.Element;
import electric.xml.Child;
import electric.xml.Children;
import electric.xml.XPath;



/** 
 * A collection of trees for one sentence.
 *
 * A MultiTree represents a collection of trees on different levels of
 * representation for the same sentence -- in particular, the Tiger-style
 * syntax tree and a topology tree as produced by Christian's tool.
 * 
 * @todo Add a scope graph.
 */
public class MultiTree {
    /** The syntax tree, or null if there isn't one. */
    protected Syntax syntax;

    /** The topology tree, or null if there isn't one. */
    protected Topology topology;

    /** A hashtable mapping the word IDs (Strings) to Word objects. */
    protected Words words;

    /** The ID of this sentence. */
    protected String id;

    /** The document from which this sentence came. */
    protected Document container;

    
    /**
     * Create a new tree collection (i.e. trees on various levels) from
     * an XML element that represents a single sentence.
     *
     * @param e the XML element.
     */
    MultiTree(Element e, Document d) {
	container = d;
	id = e.getAttribute("id");

	words = new Words();

	// read words into word hashtable
	Element terminals = e.getElement(new XPath("graph/terminals"));
	for( Children eWords = terminals.getChildren();
	     eWords.hasMoreElements(); ) {
	    Element child = (Element) eWords.next();
	    
	    words.put(child.getAttribute("id"), new Word(child, words));
	}

	// read trees -- if they are there!
	Element synroot = e.getElement(new XPath("graph/nonterminals"));

	if( synroot != null )
	    syntax = new Syntax(synroot, words, this);

	Element toproot = e.getElement(new XPath("topology"));
	if( toproot != null )
	    topology = new Topology(toproot, words, this);
    }
	

    public Document getDocument() {
	return container;
    }

    public Syntax getSyntax() {
	return syntax;
    }

    public Topology getTopology() {
	return topology;
    }

    public String getID() {
	return id;
    }
}
