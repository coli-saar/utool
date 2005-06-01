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
	parse("../xml/trees.xml", new TreeHandler(lexicon));
	parse("../xml/families.xml", new FamilyHandler(lexicon));
	parse("../xml/morphology.xml", new MorphHandler(lexicon));
	parse("../xml/syntax.xml", new SyntHandler(lexicon));
    }

    public StringBuffer lookUp(StringBuffer result, String lookUp){
	String[] sentence = lookUp.split(" ");
	Set<Node> treeSet = new HashSet<Node>();
	try{
	    for (String word : sentence){
		/** STTH */
		for (Node node : lexicon.lookup(word)) {
		    if (! node.containsEmpty("V")) {
			treeSet.add(node);
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
