package de.saar.chorus.oracle;

import java.util.*;
import java.util.regex.*;
import java.io.*;

// Use SAX rather than EXML to parse the input file to keep the 
// memory requirements constant rather than linear in the size of the file.
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;


public class Message {
    private String type;
    private Map<String,String> entries;
    private String data;

    // patterns for splitting the XML string into its components
    private static Pattern topElementPattern = 
	Pattern.compile("^<(\\S+)\\s+([^>]+)>(.*)");
    private static Pattern dataPattern = 
	Pattern.compile("^(.*?)</[^>]+>$");

    // SAX parsing objects
    private static SAXParserFactory factory = SAXParserFactory.newInstance();
    private static SAXParser saxParser;
    static {
	factory.setNamespaceAware(true);

	try {
	    saxParser = factory.newSAXParser();
	} catch(Exception e) {
	    System.err.println("Error while creating parser!");
	    e.printStackTrace(System.err);
	    System.exit(1);
	}
    }

    // element handler for SAX parser
    private class ElementHandler extends DefaultHandler {
	public void startElement(String namespaceURI,
				 String sName, // simple name (localName)
				 String qName, // qualified name
				 Attributes attrs)
	    throws SAXException
	{
	    type = sName;
	    
	    for( int i = 0; i < attrs.getLength(); i++ ) {
		entries.put(attrs.getLocalName(i), attrs.getValue(i));
	    }
	}
    }
    DefaultHandler elementHandler;
	





    public Message(String encoded) {
	elementHandler = new ElementHandler();

        decode(encoded);
    }

    public Message(String type, Map<String,String> entries) {
        this.type = type;
        if( entries == null )
            this.entries = new HashMap<String,String>();
        else
            this.entries = new HashMap<String,String>(entries);

        data = null;

	elementHandler = new ElementHandler();
    }

    public String getData() {
        return data;
    }

    public String getType() {
        return type;
    }

    public String getArgument(String arg) {
        return entries.get(arg);
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setArgument(String key, String val) {
        entries.put(key, val);
    }

    public void clearArguments() {
        entries = new HashMap<String,String>();
    }




    // encoding and decoding of Message and XML
    public void decode(String encoded) {
        clearArguments();

	try {
	    Matcher topElementM = topElementPattern.matcher(encoded);
	    if( !topElementM.matches() )
		throw new Exception("Top-element pattern didn't match!");

	    if( topElementM.group(2).endsWith("/") ) {
		// top element was of form <foo a=b c=d />
		data = null;
		saxParser.parse(new InputSource(new StringReader(encoded)),
				elementHandler);
	    } else {
		// top element was of form <foo a=b c=d>
		String shortDoc = 
		    "<" + topElementM.group(1) + " " + topElementM.group(2) + "/>";
		saxParser.parse(new InputSource(new StringReader(shortDoc)),
				elementHandler);
		
		Matcher dataM = dataPattern.matcher(topElementM.group(3));
		if( !dataM.matches() )
		    throw new Exception("Data pattern didn't match!");
		data = dataM.group(1);
	    }

	} catch(Exception e) {
            System.err.println("Couldn't decode ill-formed XML string: " + encoded);
            e.printStackTrace(System.err);
        }
    }

    public String encode() {
        StringBuffer ret = new StringBuffer("<" + type);

        for( String key : entries.keySet() )
            ret.append(" " + key + "=\"" + entries.get(key) + "\"");

        if( data != null ) {
            ret.append(" > ");
            ret.append(data);
            ret.append(" </" + type + ">");
        } else {
            ret.append(" />");
        }

        return ret.toString();
    }

}



