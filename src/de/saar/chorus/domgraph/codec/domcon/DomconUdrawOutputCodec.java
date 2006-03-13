/*
 * @(#)DomconUdrawOutputCodec.java created 03.02.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.codec.domcon;

import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org._3pq.jgrapht.Edge;

import de.saar.chorus.domgraph.codec.GraphOutputCodec;
import de.saar.chorus.domgraph.codec.MalformedDomgraphException;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.EdgeType;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.domgraph.graph.NodeType;

public class DomconUdrawOutputCodec extends GraphOutputCodec {
	public static String getName()
	{
		return "domcon-udraw";
	}
	
	public static String getExtension()
	{
		return ".dc.udg";
	}
	
	public void encode_graph(DomGraph graph, NodeLabels labels, Writer writer) throws IOException, MalformedDomgraphException 
	{
		Set<String> indeg0 = new HashSet<String>();
		
		for (String node : graph.getAllRoots()) {
			if (graph.indeg(node) == 0)
				indeg0.add(node);
		}
		
		encodeNodes(indeg0, graph, labels, writer);
		writer.flush();
	}
	
	private void encodeNodes(Set<String> roots, DomGraph graph, NodeLabels labels, Writer writer) throws IOException
	{
		boolean first = true;
		Set<String> refp = new HashSet<String>();
		
		for( String root : roots ) {
			if( !first ) {
				writer.write(",");
			} else {
				first = false;
			}
			encodeNode(root, graph, labels, refp, writer);
		}
	}
	
	private void encodeNode(String root, DomGraph graph, NodeLabels labels, Set<String> refp, Writer writer) throws IOException
	{
		if( !refp.add(root)) {
			writer.write("r(\"" + root + "\")");
		} else {
			writer.write("l(\"" + root + ",n(\"\",[a(\"OBJECT\",\"" + root);
			
			if( graph.getData(root).getType() == NodeType.LABELLED ) {
				writer.write(":" + labels.getLabel(root));
			}
			writer.write("\")],[");
			encodeEdges(graph.getOutEdges(root, null), graph, labels, refp, writer);
			writer.write("]))");
		}
	}
	
	private void encodeEdges(List<Edge> edges, DomGraph graph, NodeLabels labels, Set<String> refp, Writer writer) throws IOException 
	{
		boolean first = true;
		
		for( Edge edge : edges) {
			if( !first ) {
				writer.write(",");
			} else {
				first = false;
			}
			
			encodeEdge(edge, graph, labels, refp, writer);
		}
	}
	
	private void encodeEdge(Edge edge, DomGraph graph, NodeLabels labels, Set<String> refp, Writer writer) throws IOException
	{
		if( graph.getData(edge).getType() == EdgeType.TREE ) {
			writer.append("e(\"\",[a(\"EDGEPATTERN\",\"solid\")],");
		} else {
			writer.write("e(\"\",[a(\"EDGEPATTERN\",\"dotted\")],");
		}
		encodeNode((String) edge.getTarget(), graph, labels, refp, writer);
		writer.write(")");
	}
	
	public void print_header(Writer writer) throws IOException
	{
		writer.write("[");
	}
	
	public void print_footer(Writer writer) throws IOException
	{
		writer.write("]");
	}
	
	public void print_start_list(Writer writer) throws IOException
	{
	}
	
	public void print_end_list(Writer writer) throws IOException
	{
	}
	
	public void print_list_separator(Writer writer) throws IOException
	{
	}
}
