/*

Assume input file format as follows:


<probabilities>
  <root lexentry="12" count="27" />
  ....

  <labelling lexentry="12" edgelabel="abcd" count="2987" />
  ....

  <dependency lexentryMother="12" lexentryDaughter="13"
  edgelabel="abcd" count="982734" />
  ....
</probabilities>




This Evaluation class computes the costs as described in the Nancy paper,
from an XML file with absolute counts as shown above.

The costs it returns are negative logarithms of the frequencies.



Memory requirements: About 100 MB heap for an input file with 900.000 entries.
Java max. heap size can be increased with e.g. -Xmx256m

*/


package de.saar.coli.chorus.oracle.sxdg;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import de.saar.coli.chorus.oracle.Evaluator;



class BilexicalEvaluator extends DefaultHandler implements Evaluator<SxdgReflection> {
    // At the moment, these Maps have no internal structure; the key components
    // (e.g. lexentry plus edgelabel) are concatenated to obtain the real key.
    // This is to avoid the considerable memory overhead involved in having
    // a tree of Maps.

    // String (lexentry id) -> Double
    private Map<String,Double> rootProb; 


    // String (lexentry_edgelabel) -> Double
    private Map<String,Double> labelProb;

    // String <l'_l_edgelabel> -> Double
    private Map<String,Double> dependencyProb;

    // records lexical entries
    private HashSet<String> lexCount;

    // memoisation of previous results
    private Map<SxdgReflection,Double> evaluations;



    private int sumRootCounts, sumLabelCounts, sumDepCounts, sumLexEntries;


    

    public double evaluate(SxdgReflection info) {
	if( evaluations.containsKey(info) )
	    return evaluations.get(info);

	else {
	    double ret = 0;

	    // compute, for each node, the minimal probability among
	    // (a) putting the node at the root with a given lex. entry, and
	    // (b) having a certain incoming edge. Then sum over everything.
	    for( String node : info.getNodeSet() ) {
		double minThisNode = 0;
		boolean first = true; // true if we have never assigned anything to minThisNode

		for( String lexentry : info.getLexEntries(node) ) {
		    // root probabilities
		    double rootprob = getRootProb(lexentry);
		    if( first || (rootprob < minThisNode) ) {
			minThisNode = rootprob;
			first = false;
		    }

		    // incoming edge probabilities
		    Set<String> incomingLabels = info.getIncomingEdgeLabels(node);

		    if( incomingLabels != null ) {
			for( String edgelabel : incomingLabels ) {
			    for( String mother : info.getIncomingEdges(node, edgelabel) ) {
				for( String motherLex : info.getLexEntries(mother) ) {
				    double edgeprob = 
					getDependencyProb(lexentry, motherLex, edgelabel)
					+ getLabelProb(motherLex, edgelabel);
				    if( first || (edgeprob < minThisNode) ) {
					minThisNode = edgeprob;
					first = false;
				    }
				}
			    }
			}
		    }
		}
		    
		ret += minThisNode;
	    }

	    return ret;
	}
    }


    public BilexicalEvaluator(String probXmlFilename) {
	super();

	rootProb = new HashMap<String,Double>();
	labelProb = new HashMap<String,Double>();
	dependencyProb = new HashMap<String,Double>();
	lexCount = new HashSet<String>();
	evaluations = new HashMap<SxdgReflection,Double>();

	sumRootCounts = sumLabelCounts = sumDepCounts = sumLexEntries = 0;	

	// Read the probability input file.
        SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
            SAXParser saxParser = factory.newSAXParser();

	    System.err.println("Parsing probability file " + probXmlFilename + "...");
            saxParser.parse( new File(probXmlFilename), this );


	    System.err.println("Finished parsing: " +
			       rootProb.keySet().size() + " roots, " +
			       labelProb.keySet().size() + " labels, " +
			       dependencyProb.keySet().size() + " dependencies.");
        } catch (Throwable t) {
            t.printStackTrace();
	    System.exit(1);
        }

	sumLexEntries = lexCount.size();

	// Compute all the relative frequencies.
	System.err.print ("Computing relative frequencies ... ");
	computeRelative( rootProb, sumRootCounts );
	computeRelative( labelProb, sumLabelCounts );
	computeRelative( dependencyProb, sumDepCounts );
	System.err.println("done.");

    }
    
    // Methods for extending DefaultHandler, i.e. callback methods for
    // the SAX parser.

    public void startElement(String namespaceURI,
                             String sName, // simple name (localName)
                             String qName, // qualified name
                             Attributes attrs)
	throws SAXException
    {
        String eName = sName; // element name
        if ("".equals(eName)) eName = qName; // namespaceAware = false

	if( eName.equals("root") ) {
	    String lexentry = attrs.getValue("lexentry");
	    int count = getIntegerAttribute(attrs, "count");

	    rootProb.put(lexentry, new Double(count));
	    lexCount.add(lexentry);
	    sumRootCounts += count;
	} else if( eName.equals("labelling") ) {
	    String lexentry = attrs.getValue("lexentry");
	    String edgelabel = attrs.getValue("edgelabel");
	    int count = getIntegerAttribute(attrs, "count");

	    putLabelProb( lexentry, edgelabel, count );
	    lexCount.add(lexentry);
	    sumLabelCounts += count;
	} else if( eName.equals("dependency") ) {
	    String leDaughter = attrs.getValue("lexentryDaughter");
	    String leMother = attrs.getValue("lexentryMother");
	    String edgelabel = attrs.getValue("edgelabel");
	    int count = getIntegerAttribute(attrs, "count");

	    putDependencyProb( leDaughter, leMother, edgelabel, count);
	    lexCount.add(leDaughter);
	    lexCount.add(leMother);
	    sumDepCounts += count;
	}
    }

    private int getIntegerAttribute(Attributes attrs, String attrName) {
	String val = attrs.getValue(attrName);

	if( val == null )
	    return -1;
	else
	    return Integer.decode(val).intValue();
    }




    // probability model related methods

    private void computeRelative( Map<String,Double> probMap, double total ) {
	for( String key : probMap.keySet() ) 
	    probMap.put(key, probMap.get(key) / total );
    }




    // accessor functions for the probability Maps.



    // root prob accessor function
    double getRootProb(String lexid) {
	if( rootProb.containsKey(lexid) )
	    return -Math.log((sumLexEntries/(sumLexEntries+1.0) * rootProb.get(lexid)));
	else
	    return -Math.log(1/(sumLexEntries+1.0));
    }

    // label prob accessor functions
    double getLabelProb(String lexid, String edgelabel) {
	String key = lexid + "_" + edgelabel;
	
	if( labelProb.containsKey(key))
	    return -Math.log(labelProb.get(key));
	else
	    return -Math.log(0.5);
    }

    private void putLabelProb(String lexid, String edgelabel, double prob) {
	String key = lexid + "_" + edgelabel;
	labelProb.put(key, new Double(prob));
    }



    // dependency prob accessor functions
    double getDependencyProb(String childname, String mothername, String edgelabel) {
	String key = childname + "_" + mothername + "_" + edgelabel;
	if( dependencyProb.containsKey(key) )
	    return -Math.log((sumLexEntries/(sumLexEntries+1.0)) * dependencyProb.get(key));
	else
	    return -Math.log(1/(sumLexEntries+1.0));
    }

    private void putDependencyProb(String childname, String mothername, String edgelabel,
				   double prob) {
	String key = childname + "_" + mothername + "_" + edgelabel;
	dependencyProb.put(key, prob);
    }
}
