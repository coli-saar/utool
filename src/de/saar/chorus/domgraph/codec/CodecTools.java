/*
 * @(#)CodecTools.java created 03.02.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.codec;

import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.domgraph.graph.NodeType;


/**
 * Methods that are useful in the implementation of codecs. 
 * 
 * @author Alexander Koller
 *
 */
public class CodecTools {
    
    /**
     * Computes a string that is a valid Oz or Prolog atom from
     * the argument. If the argument starts with a lowercase letter
     * and all later symbols are letters, digit, or underscore,
     * the argument itself is returned; otherwise, the argument
     * is surrounded by quotes '...'. 
     * 
     * @param label 
     * @return
     */
    public static String atomify(String label) {
        boolean must_atomify = false;
        
        for( int i = 0; i < label.length(); i++ ) {
            if( !Character.isLetterOrDigit(label.charAt(i)) && (label.charAt(i) != '_') ) {
                must_atomify = true;
            }
        }
        
        if( Character.isUpperCase(label.charAt(0)) ||
                Character.isDigit(label.charAt(0)) ||
                (label.charAt(0) == '_') ) {
            must_atomify = true;
        }
        
        return must_atomify ? ("\'" + label + "'") : label;
    }
    
    /**
     * Computes a string that is a valid Prolog variable from
     * the argument. If the argument starts with an uppercase
     * letter or an underscore, it is returned directly; otherwise,
     * it is prefixed with an underscore.<p>
     * 
     * Note that the returned string will still not be a valid
     * Prolog variable name if it contains symbols that are not
     * valid in Prolog variable names.
     * 
     * @param label
     * @return
     */
    public static String varify(String label) {
        if( !label.startsWith("_") && !Character.isUpperCase(label.charAt(0)) ) {
            return "_" + label;
        } else {
            return label;
        }
    }
    
    /**
     * Asserts that all labelled nodes in the dominance graph actually have
     * labels. If this is not the case and assertions are enabled, then
     * an assertion exception is thrown. This is useful in debugging codecs.
     * 
     * @param graph a dominance graph
     * @param labels a matching labels object
     */
    public static void graphLabelsConsistencyAssertion(DomGraph graph, NodeLabels labels) {
        for( String node : graph.getAllNodes() ) {
            if( graph.getData(node).getType() == NodeType.LABELLED ) {
                assert (labels.getLabel(node) != null) : "no label for labelled node " + node;
            }
        }
    }

}
