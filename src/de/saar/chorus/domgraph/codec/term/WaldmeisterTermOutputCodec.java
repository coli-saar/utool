/*
 * @(#)WaldmeisterTermOutputCodec.java created 25.01.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.codec.term;

import java.util.Map;

import org._3pq.jgrapht.Edge;

import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.EdgeType;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.domgraph.graph.NodeType;

public class WaldmeisterTermOutputCodec extends TermOutputCodec {
    public WaldmeisterTermOutputCodec() {
        super(",");
        setName("term-waldmeister");
        setExtension(".t.wm");
    }
    
    protected String computeTerm(String node, DomGraph graph, NodeLabels labels, Map<String, String> domEdges) {
        boolean first = true;
        
        if( graph.getData(node).getType() == NodeType.UNLABELLED ) {
            return computeTerm(domEdges.get(node), graph, labels, domEdges);
        } else {
            StringBuilder ret = new StringBuilder(labels.getLabel(node) + "_" + node);

            if( graph.outdeg(node) > 0 ) {
                ret.append("(");
                for( Edge e : graph.getOutEdges(node, EdgeType.TREE) ) {
                    if( first ) {
                        first = false;
                    } else {
                        ret.append(separator);
                    }
                    
                    ret.append(computeTerm((String) e.getTarget(), graph, labels, domEdges));
                }
                ret.append(")");
            }
            
            return ret.toString();
        }
    }
}
