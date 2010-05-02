/*
 * Ubench.java
 */
package de.saar.chorus.XTAGLexicon;

import java.io.File;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.helpers.DefaultHandler;

public class SocketMain {
	
	private Lexicon lexicon;
	
	public void run (){
		lexicon = new Lexicon(new POSScaler());
		parse("../xml/trees.xml", new TreeHandler(lexicon));
		parse("../xml/families.xml", new FamilyHandler(lexicon));
		parse("../xml/morphology.xml", new MorphHandler(lexicon));
		parse("../xml/syntax.xml", new SyntHandler(lexicon));
	}
	
	public StringBuffer lookUp(StringBuffer result, String lookUp){
		try{int counter = 1;
		result.append("<?xml version=\"1.0\"?>\n<lexicon>\n");
		for (Tree tree : lexicon.lookup(lookUp)) {
			result.append(" <----- Baum Nummer "+counter+" ----->\n");
			tree.getRoot().printXMLInBuffer(result," ");
			result.append("\n");
			counter++;
		}
		result.append("</lexicon>");
		}
		catch (Exception e){
			result.append("Error: "+e.getMessage());}
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
