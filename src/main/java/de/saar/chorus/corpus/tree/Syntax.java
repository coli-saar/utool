/**
 * @file   Syntax.java
 * @author Alexander Koller
 * @date   Wed May 21 15:33:58 2003
 * 
 * @brief  Representation of a syntax tree.
 * 
 * 
 */


package de.saar.chorus.corpus.tree;

import electric.xml.Element;


/**
 * Representation of a syntax tree.
 * 
 */
public class Syntax extends Tree {
    Syntax(Element e, Words words, MultiTree mt) {
	super(e,words,mt);
    }
}

