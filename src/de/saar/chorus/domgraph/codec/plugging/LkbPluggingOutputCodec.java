/*
 * @(#)LkbPluggingOutputCodec.java created 03.02.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.codec.plugging;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.saar.basic.StringTools;
import de.saar.chorus.domgraph.codec.MalformedDomgraphException;
import de.saar.chorus.domgraph.codec.PluggingOutputCodec;
import de.saar.chorus.domgraph.graph.DomEdge;
import de.saar.chorus.domgraph.graph.DomGraph;


/**
 * An output codec for pluggings in Lisp syntax. This output format
 * is compatible with the <a href="http://wiki.delph-in.net/moin/LkbTop">LKB</a>
 * grammar development system: You can use Utool/Java as a drop-in replacement
 * for the MRS solver that is built into LKB if you use the mrs-prolog input
 * codec and this output codec.<p>
 * 
 * An example output looks as follows:<br/>
 * {@code ((x x y) (y x y) (u u v) (v u v))}
 * 
 * @author Alexander Koller
 * @author Stefan Thater
 *
 */
public class LkbPluggingOutputCodec extends PluggingOutputCodec {
    public static String getName() {
        return "plugging-lkb";
    }
    
    public static String getExtension() {
        return ".lkbplug.lisp";
    }

    public void encode_plugging(DomGraph graph, Collection<DomEdge> domedges,
            Writer writer) throws IOException, MalformedDomgraphException {
        List<String> edgeStrings = new ArrayList<String>();
        
        for( DomEdge edge : domedges ) {
            String v1 = edge.getSrc().substring(1);
            String v2 = edge.getTgt().substring(1);
            
            edgeStrings.add("(" + v1 + " " + v1 + " " + v2 + ")");
            edgeStrings.add("(" + v2 + " " + v1 + " " + v2 + ")");
        }
        
        writer.write("( " + StringTools.join(edgeStrings, " ") + ")\n");
        writer.flush();
    }

    

    public void print_header(Writer writer) throws IOException {
    }

    public void print_footer(Writer writer) throws IOException {
        writer.flush();
    }

    public void print_start_list(Writer writer) throws IOException {
        writer.write("(");
    }

    public void print_end_list(Writer writer) throws IOException {
        writer.write(")");
    }

    public void print_list_separator(Writer writer) throws IOException {
    }
}
