/*
 * @(#)TermOutputCodec.java created 25.01.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.codec.term;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org._3pq.jgrapht.Edge;

import de.saar.chorus.domgraph.codec.GraphOutputCodec;
import de.saar.chorus.domgraph.codec.MalformedDomgraphException;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.EdgeType;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.domgraph.graph.NodeType;

public class TermOutputCodec extends GraphOutputCodec {
    protected String separator;
    
    public TermOutputCodec(String separator) {
        super();
        this.separator = separator;
    }

    public void encode_graph(DomGraph graph, NodeLabels labels, Writer writer)
            throws IOException, MalformedDomgraphException {
        Map<String,String> domEdges = new HashMap<String,String>(); // top -> bottom
        List<String> terms = new ArrayList<String>();
        boolean first = true;
        
        // check whether graph is in simple solved form
        if( !graph.isSimpleSolvedForm() || !graph.isNormal() ) {
            throw new MalformedDomgraphException("Graph must be a simple normal solved form");
        }

        // build dom-edge map
        for( Edge e : graph.getAllEdges() ) {
            if( graph.getData(e).getType() == EdgeType.DOMINANCE ) {
                domEdges.put((String) e.getSource(), (String) e.getTarget());
            }
        }
        
        // compute top nodes
        for( String node : graph.getAllNodes() ) {
            if( graph.indeg(node) == 0 ) {
                terms.add(computeTerm(node, graph, labels, domEdges));
            }
        }
        
        // output the whole term
        if( terms.size() == 1 ) {
            //System.err.println("write: " + terms.get(0));
            //System.err.println("writer = " + writer);
            //writer.write("hallo\n");
            writer.write(terms.get(0));
        } else {
            writer.write("top" + terms.size() + "(");
            for( String str : terms ) {
                if( first ) {
                    first = false;
                } else {
                    writer.write(separator);
                }
                
                writer.write(str);
            }
            writer.write(")");
        }

	writer.flush();
    }

    protected String computeTerm(String node, DomGraph graph, NodeLabels labels, Map<String, String> domEdges) {
        boolean first = true;
        
        if( graph.getData(node).getType() == NodeType.UNLABELLED ) {
            return computeTerm(domEdges.get(node), graph, labels, domEdges);
        } else if( labels.getLabel(node) == null ) {
            // The node is a fake root introduced by an input codec.
            // In this case, it has exactly one child via a tree edge,
            // and we just move on to the child without doing anything here.
            return computeTerm(graph.getChildren(node,EdgeType.TREE).iterator().next(),
                    graph, labels, domEdges);
        } else {
            String label = labels.getLabel(node);
            StringBuilder ret = new StringBuilder(atomify(label));
            
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

    protected String atomify(String label) {
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


    
    public void print_header(Writer writer) {
    }

    public void print_footer(Writer writer) {
    }

    public void print_start_list(Writer writer) {
    }

    public void print_end_list(Writer writer) {
    }

    public void print_list_separator(Writer writer) throws IOException {
        writer.write("\n");
    }

}
