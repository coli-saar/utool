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
    
    String root, pos, agr;
    
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
            root = null;
            pos = null;
            agr = null;
        }
        else if (element.equals("entry")) {
            root = attrs.getValue("root");
            pos = attrs.getValue("pos");
        } else if( element.equals("feature") ) {
            agr = attrs.getValue("value");
        }
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        String element = localName.equals("") ? qName : localName;
        
        if( element.equals("entry")) {
            lexicon.addMorph(index, root, pos, agr);
        }
    }
    
    
    
}
