package de.saar.chorus.XTAGLexicon;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class SyntHandler extends DefaultHandler {
    
    private Lexicon lexicon;
    private List<Anchor> anchors;
    private Set<String> trees;
    private Set<String> families;
    private String index;
    private List<UnificationEquation> equations;
    
    public SyntHandler (Lexicon lexicon) {
        this.lexicon = lexicon;
        this.anchors = null;
        this.index = null;
        this.trees = null;
        this.families = null;
        this.equations = null;
    }
    
    public void startElement(String namespaceURI,
            String sName,
            String qName,
            Attributes attrs)
    throws SAXException
    {
        String name = sName.equals("") ? qName : sName;
        
        if (name.equals("index")) {
            index = attrs.getValue("id");
        }
        else if (name.equals("entry")) {
            anchors = new ArrayList<Anchor>();
            trees = new HashSet<String>();
            families = new HashSet<String>();
            equations = new ArrayList<UnificationEquation>();
        }
        else if (name.equals("tree")) {
            trees.add(attrs.getValue("idref"));
        }
        else if (name.equals("family")) {
            families.add(attrs.getValue("idref"));
        }
        else if (name.equals("anchor")) {
            String word = attrs.getValue("word");
            String pos = attrs.getValue("pos");

            anchors.add(new Anchor(word, pos, word.equals(index)));
        } else if( name.equals("feature")) {
            UnificationEquation.Feature ft = new UnificationEquation.Feature(attrs.getValue("node"), UnificationEquation.Side.top, attrs.getValue("ft"));
        }
    }
    
    public void endElement(String namespaceURI,
            String sName,
            String qName)
    throws SAXException
    {
        String name = sName.equals("") ? qName : sName;
        
        if (name.equals("entry")) {
            lexicon.addSyntax(index, anchors, trees, families, equations);
        }
    }
}
