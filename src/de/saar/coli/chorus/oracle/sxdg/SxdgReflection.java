
package de.saar.coli.chorus.oracle.sxdg;

import de.saar.coli.chorus.oracle.*;

import java.util.*;
import java.io.*;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;


class SxdgReflection {
    // String (nodename) -> Set (of Strings specifying lexentry IDs)
    private Map<String,Set<String>> lexEntries; 

    // String (nodename) -> 
    //     Map<String (edgelabel) -> Set (of Strings: mother nodenames)>
    private Map<String,Map<String,Set<String>>> incomingEdges; 

    // SAX parsing objects
    private static SAXParserFactory factory = SAXParserFactory.newInstance();
    private static SAXParser saxParser;
    static {
	factory.setNamespaceAware(true);

	try {
	    saxParser = factory.newSAXParser();
	} catch(Exception e) {
	    e.printStackTrace(System.err);
	    System.exit(1);
	}
    }

    // SAX parsing handlers
    private class ReflectionConstructor extends DefaultHandler {
	private String activeNodeId;

	public void startElement(String namespaceURI,
				 String sName, // simple name (localName)
				 String qName, // qualified name
				 Attributes attrs)
	    throws SAXException
	{
	    if( sName.equals("node") ) {
		activeNodeId = attrs.getValue("id");
	    } else if( sName.equals("entry") ) {
		addLexEntry( activeNodeId, attrs.getValue("id") );
	    } else if( sName.equals("mother") ) {
		addIncomingEdge( activeNodeId, 
				 attrs.getValue("label"),
				 attrs.getValue("id") );
	    }
	    
	}	
    }
    private DefaultHandler reflectionConstructor = new ReflectionConstructor();


    private class DiffConstructor extends DefaultHandler {
	private String activeNodeId;

	public void startElement(String namespaceURI,
				 String sName, // simple name (localName)
				 String qName, // qualified name
				 Attributes attrs)
	    throws SAXException
	{
	    if( sName.equals("node") ) {
		activeNodeId = attrs.getValue("id");
	    } else if( sName.equals("entry") ) {
		removeLexEntry( activeNodeId, attrs.getValue("id") );
	    } else if( sName.equals("mother") ) {
		removeIncomingEdge( activeNodeId, 
				    attrs.getValue("label"),
				    attrs.getValue("id") );
	    }
	    
	}	
    }
    private DefaultHandler diffConstructor = new DiffConstructor();










    public SxdgReflection() {
        lexEntries = new HashMap<String,Set<String>>();
        incomingEdges = new HashMap<String,Map<String,Set<String>>>();
    }

    public SxdgReflection(String elt, SortedEvaluatingSearchSpace<SxdgReflection> space) {
        lexEntries = new HashMap<String,Set<String>>();
        incomingEdges = new HashMap<String,Map<String,Set<String>>>();

	if( elt.indexOf("mode=\'diff\'") >= 0 ) {
	    System.err.println("Attempting to create reflection from diff, but don't know parent!");
        } else if( elt.indexOf("mode=\'complete\'") >= 0 ) {
            constructFromReflection(elt);
        } else if( elt.indexOf("mode=\"diff\"") >= 0 ) {
	    System.err.println("Attempting to create reflection from diff, but don't know parent!");
        } else if( elt.indexOf("mode=\"complete\"") >= 0 ) {
            constructFromReflection(elt);
        } 
    }


    public SxdgReflection(String elt, SortedEvaluatingSearchSpace<SxdgReflection> space, SxdgReflection parent) {
        lexEntries = new HashMap<String,Set<String>>();
        incomingEdges = new HashMap<String,Map<String,Set<String>>>();

	if( elt.indexOf("mode=\'diff\'") >= 0 ) {
            constructFromDiff(elt, space, parent);
        } else if( elt.indexOf("mode=\'complete\'") >= 0 ) {
            constructFromReflection(elt);
        } else if( elt.indexOf("mode=\"diff\"") >= 0 ) {
            constructFromDiff(elt, space, parent);
        } else if( elt.indexOf("mode=\"complete\"") >= 0 ) {
            constructFromReflection(elt);
        } 
    }


    public Set<String> getLexEntries(String nodename) {
        if( lexEntries.containsKey(nodename) )
            return lexEntries.get(nodename);
        else {
            Set<String> ret = new HashSet<String>();
            lexEntries.put(nodename, ret);
            return ret;
        }
    }

    public Set<String> getNodeSet() {
        return lexEntries.keySet();
    }

    public Set<String> getAllEdgeLabels() {
        Set<String> edgelabels = new HashSet<String>();

        for( String nodename : getNodeSet() ) {
	    Set<String> localLabels = getIncomingEdgeLabels(nodename);
	    if( localLabels != null )
		edgelabels.addAll(localLabels);
        }

        return edgelabels;
    }

    public Set<String> getIncomingEdgeLabels(String nodename) {
        if( incomingEdges.containsKey(nodename) ) {
	    Set<String> ret = new HashSet<String>();

	    for( String edgelabel :  incomingEdges.get(nodename).keySet() ) {
		if( !getIncomingEdges(nodename, edgelabel).isEmpty() )
		    ret.add(edgelabel);
	    }

	    return ret;
	}
        else
            return null;
    }


    public Set<String> getIncomingEdges(String nodename, String edgelabel) {
        Map<String,Set<String>> entryForNode;

        if( incomingEdges.containsKey(nodename) )
            entryForNode = incomingEdges.get(nodename);
        else {
            entryForNode = new HashMap<String,Set<String>>();
            incomingEdges.put(nodename, entryForNode);
        }

        if( entryForNode.containsKey(edgelabel) )
            return entryForNode.get(edgelabel);
        else {
            Set<String> entryForEdgelabel = new HashSet<String>();
            entryForNode.put(edgelabel, entryForEdgelabel);
            return entryForEdgelabel;
        }
    }



    private void copy(SxdgReflection parent) {
	// deep, deep clone of the collections.
	lexEntries = new HashMap<String,Set<String>>();
	incomingEdges = new HashMap<String,Map<String,Set<String>>>();

	for( Map.Entry<String,Set<String>> entry : parent.lexEntries.entrySet() ) {
	    Set<String> newVal = new HashSet<String>();
	    newVal.addAll(entry.getValue());
	    lexEntries.put(entry.getKey(), newVal);
	}

	for( Map.Entry<String,Map<String,Set<String>>> entry :
		 parent.incomingEdges.entrySet() ) {
	    Map<String,Set<String>> newVal = new HashMap<String,Set<String>>();
	    for( Map.Entry<String,Set<String>> subEntry : 
		     entry.getValue().entrySet() ) {
		Set<String> newVal2 = new HashSet<String>();
		newVal2.addAll(subEntry.getValue());
		newVal.put(subEntry.getKey(), newVal2);
	    }
	    incomingEdges.put(entry.getKey(), newVal);
	}
    }


    private void constructFromDiff(String elt, SortedEvaluatingSearchSpace<SxdgReflection> space,
				   SxdgReflection parent ) {

	copy(parent);

	try {
	    saxParser.parse(new InputSource(new StringReader(elt)),
			    diffConstructor);
	} catch(Exception e) {
            System.err.println("Couldn't decode ill-formed XML data string: " + elt);
            e.printStackTrace(System.err);
        }
    }





    private void constructFromReflection(String elt) {
	try {
	    saxParser.parse(new InputSource(new StringReader(elt)),
			    reflectionConstructor);
	} catch(Exception e) {
            System.err.println("Couldn't decode ill-formed XML data string: " + elt);
            e.printStackTrace(System.err);
        }
    }

            

        


    private void addLexEntry(String nodename, String lexID) {
        Set<String> entryForNode = getLexEntries(nodename); 

        entryForNode.add(lexID);
    }

    private void addIncomingEdge(String nodename, String edgelabel, String mothername) {
        Set<String> entryForEdgelabel = getIncomingEdges(nodename, edgelabel);

        entryForEdgelabel.add(mothername);
    }

    private void removeLexEntry(String nodename, String lexID) {
	Set<String> entryForNode = getLexEntries(nodename);

	if( entryForNode.isEmpty() ) {
	    System.err.println("Attempting to remove lexentry " +
			       lexID + " from node " + nodename +
			       ", but the node has no lexentries!");
	} else {
	    entryForNode.remove(lexID);
	}

	// DON'T delete the entry from lexEntries here, even if the
	// entry set becomes empty! We don't want to delete nodes.
    }    

    private void removeIncomingEdge(String nodename, String edgelabel, String mothername) {
        Set<String> entryForEdgelabel = getIncomingEdges(nodename, edgelabel);

	if( entryForEdgelabel.isEmpty() ) {
	    System.err.println("Attempting to remove incoming edge into "
			       + nodename + " with label " + edgelabel +
			       " from " + mothername + ", but the node "
			       + "has no such incoming edges!");
	} else {
	    entryForEdgelabel.remove(mothername);
	}
    }



    public String toString() {
        return "<sdxgReflection for " + lexEntries.keySet().size() + " nodes>";
    }
}

