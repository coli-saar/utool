/*
 * @(#)DomGraphGXLCodec.java 
 * 
 * Copyright (c) 2004, Alexander Koller
 * Based on JGraphGXLCodec, Copyright (c) 2001-2004, Gaudenz Alder and opheamro
 * All rights reserved. 
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation 
 *   and/or other materials provided with the distribution.
 * - Neither the name of JGraph nor the names of its contributors may be used
 *   to endorse or promote products derived from this software without specific
 *   prior written permission.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package de.saar.coli.chorus.leonardo;

import java.io.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import javax.xml.parsers.*;

import org.jgraph.graph.*;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Converts dominance graphs from and to GXL representations.
 * 
 * TODO Document the GXL subset that we use here, once it has been fixed.
 * 
 * @author Alexander
 * 
 */
class DomGraphGXLCodec {
	
	/**
	 * Reads a GXL description of a dominance graph from a file and writes it
	 * into a JDomGraph object. Any previous contents of the JDomGraph object
	 * are deleted in the process. 
	 * 
	 * @param inputStream the stream from which we read the GXL document.
	 * @param graph the graph into which we write the dominance graph we read.
	 * @throws ParserConfigurationException if there was an internal error in the parser configuration.
	 * @throws IOException if an error occurred while reading from the stream.
	 * @throws SAXException if the input wasn't well-formed XML.
	 */
	public static void decode(Reader inputStream, JDomGraph graph)
			throws ParserConfigurationException, IOException, SAXException {
		// id lookup
		Map<String, DefaultGraphCell> ids = new HashMap<String, DefaultGraphCell>();
	
		// set up XML parser
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();

		// Parse the input file to get a Document object
		Document doc = db.parse(new InputSource(inputStream));
		
		
		graph.clear();
		Element gxl = (Element) doc.getDocumentElement(); // First gxl element

		NodeList graph_list = gxl.getChildNodes();
		if (graph_list.getLength() == 0) {
			return;
		}

		for (int graph_index = 0; graph_index < graph_list.getLength(); graph_index++) {
			Node graph_node = graph_list.item(graph_index);
			if (graph_node.getNodeName().equals("graph")) {
			    // the "graph" element in the XML file
				Element graph_elem = (Element) graph_node;
				
				// set graph id
				String graphId = getAttribute(graph_elem, "id");
				if( graphId != null )
				    graph.setName(graphId);
				
				NodeList list = graph_elem.getChildNodes();

				// Loop Children
				for (int i = 0; i < list.getLength(); i++) {
					Node node = list.item(i);  // a "node" or "edge" element
					String id = getAttribute(node, "id");
					String edgeOrNode = node.getNodeName();
					String type = getType(node);
					Map<String,String> attrs = getStringAttributes(node);

 					// TODO: check for unique ID
					
					if( edgeOrNode.equals("node") ) {
						NodeData data;
						
						if( type.equals("hole") ) {
							data = new NodeData(NodeType.unlabelled, id, graph );
						} else {
							data = new NodeData(NodeType.labelled, id, attrs.get("label"), graph);
						}

						// add popup menu
						NodeList children = node.getChildNodes();
						for (int j = 0; j < children.getLength(); j++) {
							Node popupNode = children.item(j); 
							if( popupNode.getNodeName().equals("popup") ) {
							    data.addMenuItem(getAttribute(popupNode, "id"),
							            		 getAttribute(popupNode, "label"));
							}
						}

						DefaultGraphCell vertex = graph.addNode(data);
						ids.put(id, vertex);
						
						
					} else if( edgeOrNode.equals("edge")) {
						EdgeData data;
						
						if( type.equals("solid")) {
							data = new EdgeData(EdgeType.solid, id, graph );
						} else {
							data = new EdgeData(EdgeType.dominance, id, graph );
						}
						
						// add popup menu
						NodeList children = node.getChildNodes();
						for (int j = 0; j < children.getLength(); j++) {
							Node popupNode = children.item(j); 
							if( popupNode.getNodeName().equals("popup") ) {
							    data.addMenuItem(getAttribute(popupNode, "id"),
							            		 getAttribute(popupNode, "label"));
							}
						}

						String src = getAttribute(node, "from");
						String tgt = getAttribute(node, "to");

						DefaultGraphCell vSrc = ids.get(src);
						DefaultGraphCell vTgt = ids.get(tgt);
						
						if( (vSrc != null) && (vTgt != null)) {
						    graph.addEdge(data, vSrc, vTgt);
						}
				
					}
				}
			}
		} // end of document loop
	}

	/**
	 * Retrieve the attribute with name "attr" from the XML node.
	 * 
	 * @param node a node in the XML document
	 * @param attr the attribute whose value we want to retrieve
	 * @return the attribute's value, or null if it is undefined.
	 */
	private static String getAttribute(Node node, String attr) {
	    if( node != null )
	        if( node.getAttributes() != null )
	            if( node.getAttributes().getNamedItem(attr) != null )
	                return node.getAttributes().getNamedItem(attr).getNodeValue();
	            
	    return null;
	}
	
	/**
	 * Read the "type" attribute from a GXL node.
	 * 
	 * @param node the DOM node whose type attribute we want to read.
	 * @return the node's type attribute, or null if it doesn't have one.
	 */
	private static String getType(Node node) {
		NodeList children = node.getChildNodes();
		for (int j = 0; j < children.getLength(); j++) {
			Node type = children.item(j); 
			if( type.getNodeName().equals("type") ) {
				return getAttribute(type, "xlink:href");
			}
		}
		
		return null;		
	}
	
	/**
	 * Read the string-valued attributes of a GXL node into a Map.
	 * The resulting map will have one key/value pair for each attribute/value
	 * pair in the GXL representation. The method will ignore all attributes
	 * whose value types aren't "string".
	 * 
	 * @param node the DOM node whose attributes we want to read. 
	 * @return a Map of attribute/value pairs.
	 */
	private static Map<String,String> getStringAttributes(Node node) {
		Map<String,String> ret = new HashMap<String,String>();
		
		NodeList children = node.getChildNodes();
		for (int j = 0; j < children.getLength(); j++) {
			Node attr = children.item(j); // an "attr" element which is a child
                                          // of the node
			if( attr.getNodeName().equals("attr") ) {
				String key = getAttribute(attr, "name");
				NodeList values = attr.getChildNodes(); // a "string" element
                                                        // which is a child of
                                                        // attr
				for (int k = 0; k < values.getLength(); k++) {
					if (values.item(k).getNodeName().equals("string")) {
						Node labelNode = values.item(k).getFirstChild();
						if (labelNode != null)
							ret.put(key, labelNode.getNodeValue());
					}
				}
			}
		}
		
		return ret;
	}

	
	
	
	/**
	 * Write a dominance graph onto an output stream as a GXL document.
	 * 
	 * TODO Implement this!
	 * 
	 * @param os the output stream to which the document is written.
	 * @param graph the dominance graph that is to be encoded.
	 */
	public static void encode(OutputStream os, JDomGraph graph) {
	    
	}
	
	// TODO: Port encoding methods below.
	/*
     * 
     * 
     * static transient Hashtable hash; // Create a GXL-representation for the
     * specified cells. public static String encode(JGraph graph, Object[]
     * cells) { hash = new Hashtable(); String gxl = " <gxl> <graph>"; // Create
     * external keys for nodes for (int i = 0; i < cells.length; i++) if
     * (JGraphUtilities.isVertex(graph, cells[i])) hash.put(cells[i], new
     * Integer(hash.size())); // Convert Nodes Iterator it =
     * hash.keySet().iterator(); while (it.hasNext()) { Object node = it.next();
     * gxl += encodeVertex(graph, hash.get(node), node); } // Convert Edges int
     * edges = 0; for (int i = 0; i < cells.length; i++) if
     * (graph.getModel().isEdge(cells[i])) gxl += encodeEdge(graph, new
     * Integer(edges++), cells[i]); // Close main tags gxl += "\n </graph>
     * </gxl>"; return gxl; }
     * 
     * public static String encodeVertex(JGraph graph, Object id, Object vertex) {
     * String label = graph.convertValueToString(vertex); return "\n\t <node
     * id=\"node" + id.toString() + "\">" + "\n\t\t <attr name=\"Label\">" +
     * "\n\t\t\t <string>" + label + " </string>" + "\n\t\t </attr>" + "\n\t
     * </node>"; }
     * 
     * public static String encodeEdge(JGraph graph, Object id, Object edge) {
     * GraphModel model = graph.getModel(); String from = ""; if
     * (model.getSource(edge) != null) { Object source =
     * hash.get(model.getParent(model.getSource(edge))); if (source != null)
     * from = "node" + source.toString(); } String to = ""; if
     * (model.getTarget(edge) != null) { Object target =
     * hash.get(model.getParent(model.getTarget(edge))); if (target != null) to =
     * "node" + target.toString(); } if (from != null && to != null) { String
     * label = graph.convertValueToString(edge); return "\n\t <edge id=\"edge" +
     * id.toString() + "\"" + " from=\"" + from + "\"" + " to=\"" + to + "\">" +
     * "\n\t\t <attr name=\"Label\">" + "\n\t\t\t <string>" + label + "
     * </string>" + "\n\t\t </attr>" + "\n\t </edge>"; } else return ""; }
     */
}

