/**
 * @file   Auxiliary.java
 * @author Alexander Koller
 * @date   Wed Jun  4 15:37:43 2003
 * 
 * @brief  Some auxiliary functions that are useful in various other classes.
 * 
 * 
 */

package de.saar.chorus.corpus.tree;

import java.util.*;

import electric.xml.Attributes;
import electric.xml.Attribute;
import electric.xml.Element;


/**
 * Some auxiliary functions that are useful in various other classes.
 * 
 */

class Auxiliary {
    /** 
     * Join an array of strings with a separator, similarly to the Perl function.
     * 
     * @param strings an array of strings.
     * @param separator a string to be inserted between each two strings in the array.
     * 
     * @return The joined string.
     */
    static String join(String[] strings, String separator) {
	StringBuffer sb = new StringBuffer();

	for( int i = 0; i < strings.length; i++ ) {
	    if( i > 0 ) sb.append(separator);
	    sb.append(strings[i]);
	}

	return sb.toString();
    }

    /** 
     * Copy all attributes from one XML element to another, except for the ones specified.
     * 
     * @param source the XML element to copy attributes from.
     * @param target the XML element to copy attributes into.
     * @param excluded the names of attributes that should not be copied.
     */
    static void copyAttributesExcept(Element source, Element target, String[] excluded) {
	for( Attributes attrs = source.getAttributeObjects();
	     attrs.hasMoreElements(); ) {
	    boolean copy = true;
	    Attribute attr = attrs.next();
	    String name = attr.getName();

	    for( int i = 0; i < excluded.length; i++ )
		if( excluded[i].equals(name) )
		    copy = false;

	    if( copy )
		target.setAttribute(attr);
	}
    }

}
