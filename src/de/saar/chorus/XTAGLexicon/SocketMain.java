/*
 * Main.java
 */

import java.io.File;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.helpers.DefaultHandler;
import java.util.*;
import java.io.*;

public class SocketMain {
    
    private Lexicon lexicon;

    public void run (){
	lexicon = new Lexicon(new POSScaler());
	parse("trees.xml", new TreeHandler(lexicon));
	parse("families.xml", new FamilyHandler(lexicon));
	parse("morphology.xml", new MorphHandler(lexicon));
	parse("syntax.xml", new SyntHandler(lexicon));
    }

    public StringBuffer lookUp(StringBuffer result, String lookUp){
	try{
	    for (Node root : lexicon.lookup(lookUp)) {
		    root.printXMLInBuffer(result,"");
		    result.append("\n\n");
		    }
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
