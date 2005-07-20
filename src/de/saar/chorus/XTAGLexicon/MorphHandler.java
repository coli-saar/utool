/*
 * MorphHandler.java
 */
package de.saar.chorus.XTAGLexicon;

import java.util.*;
import java.io.*;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

public class MorphHandler extends DefaultHandler {
    
    // Das Lexikon
    Lexicon lexicon;
    // Aktuelles index-Element
    String index;

    public MorphHandler(Lexicon lexicon) {
	super();
	this.lexicon = lexicon;
	this.index = null;
    }

    public void startElement(String namespaceURI,
                             String sName,
                             String qName,
                             Attributes attrs)
	throws SAXException
    {
	String element = sName.equals("") ? qName : sName;

	if (element.equals("index")) {
	    index = attrs.getValue("id");
	}
	else if (element.equals("entry")) {
	    String root = attrs.getValue("root");
	    String pos = attrs.getValue("pos");
	    lexicon.addMorph(index, root, pos);
	}
    }

}
