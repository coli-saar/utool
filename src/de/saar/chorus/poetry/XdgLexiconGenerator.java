/*
 * @(#)XdgLexiconGenerator.java created 21.07.2005
 * 
 * Copyright (c) 2005 Alexander Koller
 *  
 */

package de.saar.chorus.poetry;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.helpers.DefaultHandler;

import de.saar.chorus.XTAGLexicon.Converter;
import de.saar.chorus.XTAGLexicon.FamilyHandler;
import de.saar.chorus.XTAGLexicon.Lexicon;
import de.saar.chorus.XTAGLexicon.MorphHandler;
import de.saar.chorus.XTAGLexicon.Node;
import de.saar.chorus.XTAGLexicon.POSScaler;
import de.saar.chorus.XTAGLexicon.SyntHandler;
import de.saar.chorus.XTAGLexicon.TreeHandler;
import de.saar.chorus.XTAGLexicon.XDGEntry;
import de.saar.chorus.poetry.stress.CmuDict;

public class XdgLexiconGenerator {
    private String tagPath;
    private String cmuDictFilename;
    
    private Lexicon tagLexicon;
    private CmuDict dict;
    private List<XDGEntry> xdgLex;


    
    public static void main(String[] args) {
        XdgLexiconGenerator gen = new XdgLexiconGenerator();
        gen.run(args);
    }
    
    public XdgLexiconGenerator() {
        tagPath = "projects/XTagLexicon/xml";
        cmuDictFilename = "c:\\cmudict.0.6";
    }
    
    public void run(String[] words) {
        // load lexicons
        loadTagLexicon();
        loadCmuDict();
        
        // convert words into XDG lexicon entries
        try {
            for( int i = 0; i < words.length; i++ ) {
                for( Node node : tagLexicon.lookup(words[i])) {
                    List<Node> l = new ArrayList<Node>();
                    Converter conv = new Converter();
                    
                    l.add(node);
                    conv.convert(l);
                    
                    xdgLex.addAll(conv.getXdgEntries());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // now print an XDG grammar

        // TODO: This will involve modifying XTAGLexicon.XDGWriter
        // so a feature for admissible positions is put printed, and 
        // the word is just "*" or something.
        // TODO: Also we need to put in an empty lexical entry.
    }
    
    private void loadCmuDict() {
        dict = new CmuDict();
        dict.read(cmuDictFilename, true);
    }

    private void loadTagLexicon() {
        tagLexicon = new Lexicon(new POSScaler());
        parse(tagPath + "/trees.xml", new TreeHandler(tagLexicon));
        parse(tagPath + "/families.xml", new FamilyHandler(tagLexicon));
        parse(tagPath + "/morphology.xml", new MorphHandler(tagLexicon));
        parse(tagPath + "/syntax.xml", new SyntHandler(tagLexicon));
    }

    
    /**
     * parse an given xml-file using the given handler
     * @param filename the xml-file
     * @param handler the handler
     */
    private void parse(String filename, DefaultHandler handler) {
        try {   
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            
            parser.parse(new File(filename), handler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
