/**
 * @file   ScopeBearer.java
 * @author Alexander Koller
 * @date   Fri Jul  4 15:32:57 2003
 * 
 * @brief  A scope bearer.
 * 
 * 
 */


package de.saar.chorus.corpus.tree;

import java.util.*;
import electric.xml.Element;
import electric.xml.Elements;
import electric.xml.XPath;


class ScopeBearer {
    protected Node constituent; //< The constituent in the syntax tree.

    /** A vector of Strings specifying the scope features that this
	scope bearer has. Empty for hand-annotated or gold bearers,
	but not for Pafel bearers. */
    protected Hashtable features;  


    ScopeBearer() {
	constituent = null;
	features = new Hashtable();
    }
    
    // el is a "constituent" element in a scope/bearers context.
    ScopeBearer(Element el, Syntax t) {
	constituent = t.nodeForID(el.getAttribute("idref"));
	features = new Hashtable();
	
	Elements fts = el.getElements(new XPath("feature"));
	while( fts.hasMoreElements() ) {
	    Element ft = fts.next();
	    features.put(ft.getAttribute("idref"), new Integer(1));
	}
    }

    public Node getConstituent() {
	return constituent;
    }

    public Hashtable getFeatures() {
	return features;
    }
}
