/**
 * @file   Topology.java
 * @author Alexander Koller
 * @date   Wed May 21 15:33:58 2003
 * 
 * @brief  Representation of a topology tree.
 * 
 * 
 */

package de.saar.chorus.corpus.tree;

import electric.xml.Element;



/**
 * Representation of a topology tree.
 * 
 */
public class Topology extends Tree {
    Topology(Element e, Words words, MultiTree mt) {
	super(e,words,mt);
    }
}

