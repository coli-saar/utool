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


public class CodecTools {
    
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
    
    public static String varify(String label) {
        if( !label.startsWith("_") && !Character.isUpperCase(label.charAt(0)) ) {
            return "_" + label;
        } else {
            return label;
        }
    }
    
    public static void graphLabelsConsistencyAssertion(DomGraph graph, NodeLabels labels) {
        for( String node : graph.getAllNodes() ) {
            if( graph.getData(node).getType() == NodeType.LABELLED ) {
                assert (labels.getLabel(node) != null) : "no label for labelled node " + node;
            }
        }
    }

}
