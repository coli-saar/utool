/*
 * Main.java
 */

import java.io.File;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.helpers.DefaultHandler;
import java.util.*;
import java.io.*;

public class XDGMain {
    
    private Lexicon lexicon;
    private Converter converter;

    public void run (){
	converter = new Converter();
	lexicon = new Lexicon(new POSScaler());
	parse("trees.xml", new TreeHandler(lexicon));
	parse("families.xml", new FamilyHandler(lexicon));
	parse("morphology.xml", new MorphHandler(lexicon));
	parse("syntax.xml", new SyntHandler(lexicon));
    }

    public StringBuffer lookUp(StringBuffer result, String lookUp){
	String[] sentence = lookUp.split(" ");
	Set<Node> treeSet = new HashSet<Node>();
	try{
	    for (String word : sentence){
		treeSet.addAll(lexicon.lookup(word));}
	    converter.convert(treeSet);
	    int counter = 1;
	    result.append("<?xml version=\"1.0\"?>\n<lexicon>\n");
	    converter.printInBuffer(result);
	    result.append("</lexicon>");
	}
	catch (Exception e){
	    result.append("Error: "+e.getMessage());
	    e.printStackTrace();}
	return result;
    }



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
