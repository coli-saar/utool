
/**
 * @file   PafelFeatures.java
 * @author Alexander Koller
 * @date   Fri Jul  4 15:24:47 2003
 * 
 * @brief  Extract Pafel features from the Pafel scope annotation.
 *
 *         TODO: A more general treatment of scope annotations.
 * 
 * 
 */

package de.saar.coli.chorus.corpus.tree;

import java.util.*;
import electric.xml.Element;
import electric.xml.Elements;
import electric.xml.XPath;


public class PafelFeatures {
    public static void main(String args[]) {
	try {
	    Document d = new Document(args[0]);
		    Hashtable duplicates = new Hashtable();
	    
	    for( Iterator it = d.getAllIDs().iterator(); it.hasNext(); ) {
		String id = (String) it.next();

		Element el = d.getElementWithId(id); // the XML element for this sentence
		//		System.err.println("el: " + el);

		MultiTree sent = d.getSentenceWithId(id);

		Element pafel = el.getElement(new XPath("scope[@author=\"Pafel\"]"));
		//		System.err.println("pafel: " + pafel);

		System.err.println("\nSentence: " + id);

		if( pafel == null )
		    System.err.println(" --- no scope ---");
		else {



		    ScopeBearer[] sbs = new ScopeBearer[2];
		    Node[] sbsnodes = new Node[2];
		    int sbindex = 0;
		    Hashtable sbHash = new Hashtable();

		    for( Elements bearers = pafel.getElements(new XPath("bearers/constituent"));
			 bearers.hasMoreElements(); ) {
			ScopeBearer sb = new ScopeBearer(bearers.next(), sent.getSyntax());

			sbs[sbindex] = sb;
			sbsnodes[sbindex++] = sb.getConstituent();
			/*
			System.err.println("  bearer " + sb.getConstituent() +
					   " pos (" + sb.getConstituent().startPos()
					   + ":" + sb.getConstituent().endPos() + ")   "
					   + sb.getFeatures());
			*/
		    }


		    ScopeBearer first, second;
		    boolean firstIsEarlier;

		    if( (sbsnodes[0].startPos() < sbsnodes[1].startPos())
			|| ((sbsnodes[0].startPos() == sbsnodes[1].startPos())
			    && (sbsnodes[0].endPos() < sbsnodes[1].endPos()))) {
			// sbs.firstElement() comes earlier in sentence

			first = sbs[0];
			second = sbs[1];
			firstIsEarlier = true;
		    } else {
			first = sbs[1];
			second = sbs[0];
			firstIsEarlier = false;
		    }



		    
		    String[] features = {"exprae", "fokus", "indis", "inprae", "negatt", "noobj", "stdis", "swpat" };
		    String ftstr = "";
		    
		    for( int i = 0; i < features.length; i++ ) {
			if( first.getFeatures().containsKey(features[i]) )
			    ftstr += "yes,";
			else 
			    ftstr += "no,";
		    }

		    for( int i = 0; i < features.length; i++ ) {
			if( second.getFeatures().containsKey(features[i]) )
			    ftstr += "yes,";
			else 
			    ftstr += "no,";
		    }


		    Element rel = pafel.getElement(new XPath("relations/*"));
		    String val = "";

		    if( rel.getName().equals("dominates") ) {
			if( firstIsEarlier )
			    val = "DOMINATES.";
			else
			    val = "SETANIMOD.";
		    } else {
			val = "OTHER.";
			System.err.println("Non-dominance: " + rel.getName());
		    }

		    System.out.println(ftstr + val);

		    if( duplicates.containsKey(ftstr) ) {
			((Vector) duplicates.get(ftstr)).add(val);
		    } else {
			Vector x = new Vector();
			x.add(val);
			duplicates.put(ftstr,x);
		    }
		}
		    
	    }
			
		System.err.println("\n\nDuplicates:\n");
		for( Enumeration keys = duplicates.keys();
		     keys.hasMoreElements(); ) {
		    String key = (String) keys.nextElement();
		    Vector dups = (Vector) duplicates.get(key);

		    System.err.println("\n" + key);

		    for( Enumeration vals = dups.elements(); vals.hasMoreElements() ; ) {
			System.err.println(" --> " + vals.nextElement());
		    }
		
		}

	} catch(Exception e) {
	    e.printStackTrace();
	}
	    
    }
}

		
