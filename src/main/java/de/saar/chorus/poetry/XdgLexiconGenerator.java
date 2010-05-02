/*
 * @(#)XdgLexiconGenerator.java created 21.07.2005
 * 
 * Copyright (c) 2005 Alexander Koller
 *  
 */

package de.saar.chorus.poetry;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Set;

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
import de.saar.chorus.poetry.stress.StressPattern;
import de.saar.chorus.poetry.stress.WordPronunciation;

public class XdgLexiconGenerator {
    private String tagPath;
    private String cmuDictFilename;
    
    private Lexicon tagLexicon;
    private CmuDict dict;
    
    private StressPattern pattern;


    
    public static void main(String[] args) {
        XdgLexiconGenerator gen = new XdgLexiconGenerator();
        gen.run(args);
    }
    
    public XdgLexiconGenerator() {
        tagPath = "projects/XTagLexicon/xml";
        cmuDictFilename = "projects/poetry/cmudict.0.6.gz";
    }
    
    public void run(String[] words) {
        Converter conv = new Converter();
        
        // fix stress pattern
        //pattern = new StressPattern("w,s,w,w,s,w,w,s,w");
        pattern = new StressPattern(words[0]);
    	
        // load lexicons
        System.err.println("Loading TAG grammar ...");
        loadTagLexicon();
        
        System.err.println("Loading pronunciation dictionary ...");
        loadCmuDict();
        
        System.err.println("Ok");
        
        // convert words into XDG lexicon entries
        try {
            for( int i = 1; i < words.length; i++ ) {
            	conv.convert(tagLexicon.lookup(words[i]));
            }
        } catch (Exception e) {
        	System.err.println("[Exception] " + e);
        	System.exit(1);
        }
        
        // now print an XDG grammar
        StringWriter w = new StringWriter();
        XdgWriter xdgWriter = new XdgWriter();
        
        try {
        	xdgWriter.printHeader(w, conv.getAddresses(), conv.getLabels());
        	
        	for( XDGEntry entry : conv.getXdgEntries() ) {
        		Node node = conv.getNodeForEntry(entry);
        		List<WordPronunciation> prons = dict.lookup(node.getAnchor());
        		
        		System.err.println("\n\nTree: " + node + " (anchor: " + node.getAnchor() + ")");
        		System.err.println("pronunciations: ");
        		for( WordPronunciation pron : prons ) {
        			System.err.println("   " + pron);
        		}
        	
        		for( WordPronunciation pron : prons ) {
        			Set<Integer> startPositions = pattern.pronFitsAtPositions(pron);

        			for( Integer start : startPositions ) {
        				xdgWriter.printEntry(w, entry, node, 
        							start.intValue(), pron.numSyllables() );	
        			}
        		}
        	}
        	
        	xdgWriter.printEnd(w, pattern.size());
        } catch(IOException e) {
        	System.err.println("this code should never be reached (because it's a StringWriter)");
        }
        
        System.out.println(w.toString());
    }
    
    private void loadCmuDict() {
    	System.err.println("Loading pronunciation dictionary " + cmuDictFilename + " ...");
        dict = new CmuDict();
        dict.read(cmuDictFilename, false);
    }

    private void loadTagLexicon() {
    	System.err.println("Loading TAG lexicon in " + tagPath + " ...");
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
