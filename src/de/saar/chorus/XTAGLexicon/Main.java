/*
 * Main.java
 */

import java.io.File;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.helpers.DefaultHandler;

public class Main {
    
    public static void main (String args[]){
	Lexicon lexicon = new Lexicon(new POSScaler());

	System.err.println("reading trees.xml ...");
	parse("trees.xml", new TreeHandler(lexicon));
	System.err.println("reading families.xml ...");
	parse("families.xml", new FamilyHandler(lexicon));
	System.err.println("reading morphology.xml ...");
	parse("morphology.xml", new MorphHandler(lexicon));
	System.err.println("reading syntax.xml ...");
	parse("syntax.xml", new SyntHandler(lexicon));

	for (int i = 0; i < args.length; ++i) {
	    System.out.println(args[i]+":");
	    try{
		for (Node root : lexicon.lookup(args[i])) {
		    root.printLisp();
		    System.out.print("\n");
		}
	    }
	    catch (Exception e){
		System.out.println(e.getMessage());}
	}
    }


    public static void parse(String filename, DefaultHandler handler) {
	try {	
	    SAXParserFactory factory = SAXParserFactory.newInstance();
	    SAXParser parser = factory.newSAXParser();
	
	    parser.parse(new File(filename), handler);
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
    
}
