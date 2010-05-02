package de.saar.chorus.XTAGLexicon;

import java.util.HashSet;
import java.util.Set;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;



public class FamilyHandler extends DefaultHandler {

    private Lexicon lexicon;
    private String family;
    private Set<String> trees;

    public FamilyHandler(Lexicon lexicon) {
	super();
	this.lexicon = lexicon;
	this.family = null;
	this.trees = null;
    }

    public void startElement(String namespaceURI,
			     String sName,
                             String qName,
		  
	     Attributes attrs)
	throws SAXException
    {
	String element = sName.equals("") ? qName : sName;

	if (element.equals("family")) {
	    trees = new HashSet<String>();
	    family = attrs.getValue("id");
	}
	else if (element.equals("tree")){
	    trees.add(attrs.getValue("idref"));
	}
    }

    public void endElement(String namespaceURI,
			   String sName,
			   String qName)
	throws SAXException
    {
	String element = sName.equals("") ? qName : sName;

	if (element.equals("family")){
	    lexicon.addFamily(family, trees);
	}
    }

}
