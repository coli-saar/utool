/*
 * Main.java
 */

package de.saar.chorus.XTAGLexicon;

import java.io.File;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.helpers.DefaultHandler;
import java.util.*;
import java.io.*;

public class XDGMain {
    
    private Lexicon lexicon;
    private Converter converter;
    
    private String xmlPath;
    
    public XDGMain(String xmlPath) {
        this.xmlPath = xmlPath;
    }
    
    /**
     * Reads in the lexicon and saves it in lexicon
     * builds a new Converter and saves it in converter
     */
    public void run (){
        converter = new Converter();
        lexicon = new Lexicon(new POSScaler());
        parse(xmlPath + "/trees.xml", new TreeHandler(lexicon));
        parse(xmlPath + "/families.xml", new FamilyHandler(lexicon));
        parse(xmlPath + "/morphology.xml", new MorphHandler(lexicon));
        parse(xmlPath + "/syntax.xml", new SyntHandler(lexicon));
    }
    
    /**
     * Looks up the user query in the lexicon 
     * and writes the result in a StringBuffer
     * @param result the initial empty StringBuffer to be filled
     * @param lookUp the user query, the first element
     * in lookUp is the filter
     * @return an XDG Grammar of the query
     */
    public StringBuffer lookUp(StringBuffer result, String lookUp){
        String[] sentence = lookUp.split(" ");
        boolean filterAll = false;
        boolean filterCat = false;
        if (!sentence[0].equals("none")){
            if (sentence[0].equals("all")){
                filterAll = true;}
            else {filterCat = true;}
        }
        Set<Tree> treeSet = new HashSet<Tree>();
        try{
            for (int i = 1; i<sentence.length;i++){
                String word = sentence[i];
                for (Tree tree : lexicon.lookup(word)) {
                    Node root = tree.getRoot();
                    if (filterAll){
                        if (! root.containsEmpty()) {
                            treeSet.add(tree);}
                    }
                    else {
                        if (filterCat){
                            if (! root.containsEmpty(sentence[0])) {
                                treeSet.add(tree);}
                        }
                        else {treeSet.add(tree);}
                    }
                }
                
                // treeSet.addAll(lexicon.lookup(word));
            }
            converter.convert(treeSet);
            converter.printXDG(result);
        }
        catch (Exception e){
            result.append("Error: "+e.getMessage());
            e.printStackTrace();}
        return result;
    }
    
    
    /**
     * parse an given xml-file using the given handler
     * @param filename the xml-file
     * @param handler the handler
     */
    public void parse(String filename, DefaultHandler handler) {
        try {	
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            
            parser.parse(new File(filename), handler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}
