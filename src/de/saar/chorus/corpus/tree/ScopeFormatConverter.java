
/**
 * @file   ScopeFormatConverter.java
 * @author Alexander Koller
 * @date   Wed Jun  4 12:16:15 2003
 * 
 * @brief  One-shot class for converting the scope annotation format.
 * 
 * This converts scope annotations in the classical stbe format (i.e. the children
 * of a (non)bearer are references to words) to the new format, where a (non)bearer
 * has a reference to one constituent as a child. Attributes of the old (non)bearer
 * are taken over, and new attributes "words" (the idrefs of the words in the original
 * annotation) and "surface" (the surface forms of these words) are added.
 *
 * The class contains a main program. You can run it as follows:
 *
 * java -cp build/lib/Tree.jar de.saar.coli.chorus.corpus.tree.ScopeFormatConverter <corpusname>
 *
 */




package de.saar.coli.chorus.corpus.tree;

import java.util.*;
import java.io.*;

import electric.xml.Element;
import electric.xml.Elements;
import electric.xml.XPath;
import electric.xml.Attributes;
import electric.xml.Attribute;



/**
 * One-shot class for converting the scope annotation format.
 * 
 * This converts scope annotations in the classical stbe format (i.e. the children
 * of a (non)bearer are references to words) to the new format, where a (non)bearer
 * has a reference to one constituent as a child. Attributes of the old (non)bearer
 * are taken over, and new attributes "words" (the idrefs of the words in the original
 * annotation) and "surface" (the surface forms of these words) are added.
 *
 * The class contains a main program. You can run it as follows:
 *
 * java -cp build/lib/Tree.jar de.saar.coli.chorus.corpus.tree.ScopeFormatConverter <corpusname>
 * 
 */

public class ScopeFormatConverter {
    public static void main(String args[]) {
	try {
	    Document d = new Document(args[0]);

	    destructivelyConvertScope(d);
	    System.out.println(d.toXMLString());
	} catch(Exception e) {
	    e.printStackTrace();
	}
	    
    }

    /** 
     * Convert all scope elements to the new format.
     * 
     * @param doc the corpus document whose scope elements should be converted.
     */
    public static void destructivelyConvertScope(Document doc) {
	Set ids = doc.getAllIDs();

	// Iteration over sentences
	for( Iterator it = ids.iterator();
	     it.hasNext(); ) {
	    String id = (String) it.next();

	    Element elem = doc.getElementWithId(id);
	    MultiTree sent = doc.getSentenceWithId(id);
	    Syntax syn = sent.getSyntax();

	    Hashtable quantIdsToConsts = new Hashtable();


	    // Determine (or add) bearers and non-bearers.
	    Element bearers = elem.getElement(new XPath("scope/bearers"));

	    if( bearers == null ) {
		// this sentence had no scope information
		Element scope = new Element("scope");
		scope.setParent(elem);
		elem.addChild(scope);

		bearers = new Element("bearers");
		bearers.setParent(scope);
		scope.addChild(bearers);

		Element relations = new Element("relations");
		relations.setParent(scope);
		scope.addChild(relations);
	    }

	    Element nonbearers = new Element("non-bearers");
	    nonbearers.setParent(bearers.getParent());
	    bearers.getParent().addChild(nonbearers);


	    // Iteration over individual scope (non-)bearers
	    Elements scopeEls = elem.removeElements(new XPath("scope/bearers/*"));

	    while( scopeEls.hasMoreElements() ) {
		Element scopeEl = scopeEls.next();

		// Collect the word objects, and extract least upper bounds,
		// IDs, and surface words.
		Elements wordEls = scopeEl.getElements(new XPath("word"));
		String[] wordIds = new String[wordEls.size()];
		String[] wordStrs = new String[wordEls.size()];
		Vector wordVector = new Vector(wordEls.size());
		int i = 0;

		while( wordEls.hasMoreElements() ) {
		    Element wordEl = wordEls.next();

		    wordIds[i] = wordEl.getAttribute("refid");

		    Word word = (Word) syn.nodeForID(wordIds[i]);
		    if( !word.isPunct() )
			wordVector.add(word);

		    wordStrs[i++] = word.yield();
		}

		Node[] words = (Node[]) wordVector.toArray(new Node[0]);
		
		// Construct the attribute values for the new element.
		Node lub = syn.leastUpperBound(words);
		String surface = Auxiliary.join(wordStrs, " ");
		String wordids = Auxiliary.join(wordIds, " ");

		// Add this bearer to the quantifier-ID-to-constituent mapping.
		if( scopeEl.getName().equals("bearer") )
		    quantIdsToConsts.put(scopeEl.getAttribute("id"), lub.getID());

		// Create the new "constituent" element and add it as a child.
		Element newEl = new Element("constituent");

		newEl.setAttribute("idref", lub.getID());

		if( scopeEl.getName().equals("bearer") ) {
		    newEl.setParent(bearers);
		    bearers.addChild(newEl);
		} else {
		    newEl.setParent(nonbearers);
		    nonbearers.addChild(newEl);
		}

		for( Attributes attrs = scopeEl.getAttributeObjects();  // copy attributes
		     attrs.hasMoreElements(); ) {
		    Attribute attr = attrs.next();

		    if( !attr.getName().equals("id") )
			newEl.setAttribute(attr);
		}


		newEl.setAttribute("words", wordids);
		newEl.setAttribute("surface", surface);
	    }

	    // Fix references to (obsolete) quantifier IDs in the relations.
	    Element relations = elem.getElement(new XPath("scope/relations"));
	    Elements rels = elem.removeElements(new XPath("scope/relations/*"));
	    while( rels.hasMoreElements() ) {
		Element oldRel = rels.next();
		String relType = oldRel.getName();
		Element newRel = new Element(relType);

		newRel.setParent(relations);
		relations.addChild(newRel);

		if( relType.equals("dominates") ) {
		    setAttr(newRel, "arg1", oldRel, "upper", quantIdsToConsts);
		    setAttr(newRel, "arg2", oldRel, "lower", quantIdsToConsts);

		    Auxiliary.copyAttributesExcept(oldRel, newRel,
						   new String[] {"upper", "lower"});
		} else if( relType.equals("disjoint") ) {
		    setAttr(newRel, "arg1", oldRel, "first", quantIdsToConsts);
		    setAttr(newRel, "arg2", oldRel, "second", quantIdsToConsts);

		    Auxiliary.copyAttributesExcept(oldRel, newRel,
						   new String[] {"first", "second"});
		} else {
		    System.err.println("**** Unknown reltype: " + relType + " ****");
		}
	    }
	}
    }

    private static void setAttr(Element newRel, String newAttr,
				Element oldRel, String oldAttr,
				Hashtable nameMap) {
	newRel.setAttribute(newAttr,
			    (String) nameMap.get(oldRel.getAttribute(oldAttr)));
    }
}
