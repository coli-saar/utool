package nl.rug.discomm.udr.codec.rst;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org._3pq.jgrapht.Edge;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import de.saar.chorus.domgraph.codec.CodecMetadata;
import de.saar.chorus.domgraph.codec.InputCodec;
import de.saar.chorus.domgraph.codec.MalformedDomgraphException;
import de.saar.chorus.domgraph.codec.ParserException;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.EdgeData;
import de.saar.chorus.domgraph.graph.EdgeType;
import de.saar.chorus.domgraph.graph.NodeData;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.domgraph.graph.NodeType;

@CodecMetadata(name="rst-rs2-input", extension=".rs2", experimental=true)
public class RSTRs2InputCodec extends InputCodec {

	private static final String SEGMENT ="segment", GROUP ="group", PARENT="parent", RELATION="relname";
	
	@Override
	public void decode(Reader reader, DomGraph graph, NodeLabels labels)
			throws IOException, ParserException, MalformedDomgraphException {
		
		
		try {
			SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
			parser.parse(new InputSource(reader), new Rs2Handler(graph, labels));		
		} catch(IOException e) {
			throw e;
		} catch(SAXException e) {
			if( (e.getException() != null) && (e.getException() instanceof MalformedDomgraphException) ) {
				throw (MalformedDomgraphException) e.getException();
			} else {			
				throw new ParserException(e);
			}
		} catch(ParserConfigurationException e) {
			throw new ParserException(e);
		}
	}
	
	private class Rs2Handler extends DefaultHandler {
		
		private DomGraph graph;
		private NodeLabels labels;
		private Map<Integer,Stack<String>> idToHoles;
		private Map<Integer, String> idToRoots;
		private Map<Integer, Integer> childrenToParents;
		private Map<String, Integer> rootToDepth;
		private String lastSegment, lastRoot;
		private int relindex, leafindex;
		
		Rs2Handler(DomGraph g, NodeLabels l) {
			graph = g;
			labels = l;
			idToHoles = new HashMap<Integer,Stack<String>>();
			idToRoots = new HashMap<Integer,String>();
			childrenToParents = new HashMap<Integer,Integer>();
			rootToDepth = new HashMap<String,Integer>();
			lastSegment = null; 
			relindex = 0;
			leafindex = 0;
		}
		
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			if(qName.equals(SEGMENT) ||
					qName.equals(GROUP)) {
				
				int id = Integer.parseInt(attributes.getValue("id"));
				
				String p = attributes.getValue(PARENT);
				int par = -1;
				if(p!= null) {
					par = Integer.parseInt(p);
				}
				String relation = attributes.getValue(RELATION);
				String connectableNode;
				
				if(qName.equals(SEGMENT)) {
					String leaf = "y" + leafindex;
					graph.addNode(leaf, new NodeData(NodeType.LABELLED));
					leafindex++;
					lastSegment = leaf;
					connectableNode = leaf;
				} else {
					connectableNode  = idToRoots.get(id);
				}
				
				
				String rootToLabel;
				
				if( idToHoles.containsKey(par) ) {
					
					Stack<String> empty = idToHoles.get(par);
					if( empty.isEmpty() ) {
						makeSandwichFragment(par);
					} 
					rootToLabel = idToRoots.get(par);
					graph.addEdge(empty.pop(), connectableNode, new EdgeData(EdgeType.DOMINANCE));
					 
				} else if( childrenToParents.containsKey(par) ) {
					// this should only happen for nuclear relations, so actually I should not have
					// to worry about the stack being empty?
					graph.addEdge(
							idToHoles.get(childrenToParents.get(par)).pop()
							, connectableNode, new EdgeData(EdgeType.DOMINANCE));
					rootToLabel = idToRoots.get(childrenToParents.get(par));
				} else if(par > -1 ){
					System.err.println("Creating Frag from parent id " + par + " with relation " + relation);
					createRelationFragment(par);
					graph.addEdge(idToHoles.get(par).pop(), connectableNode, new EdgeData(EdgeType.DOMINANCE));
					childrenToParents.put(id, par);
					rootToLabel = idToRoots.get(par);
				} else {
					rootToLabel = null;
				}
				
				

			
				if( (rootToLabel != null) && (! relation.equals("span") ) ) {
					if(labels.getLabel(rootToLabel) == null) {
						System.err.println(rootToLabel + " --> " + relation);
						labels.addLabel(rootToLabel,relation);
					}
				}
				
			} 
		}
		
		

		@Override
		public void endDocument() throws SAXException {
			if(labels.getLabel(lastRoot) == null) {
				labels.addLabel(lastRoot, "ROOT");
				Set<String> openHoles = new HashSet<String>();
				for( String hole : graph.getHoles(lastRoot) ) {
					if(graph.getOutEdges(hole, EdgeType.DOMINANCE).size() == 0) {
						openHoles.add(hole);
					}
				}
				for(String hole : openHoles) {
					graph.remove(hole);
				}
				
			}
		}

		@Override
		public void characters(char[] arg0, int arg1, int arg2)
				throws SAXException {
			if((lastSegment != null) && labels.getLabel(lastSegment) == null) {
				labels.addLabel(lastSegment, new String(arg0, arg1, arg2));
				System.err.println(lastSegment + " --> " + labels.getLabel(lastSegment));
			}
		}
		
		private void makeSandwichFragment(int id) {
			String root = idToRoots.get(id);
			List<String> children = graph.getChildren(root, EdgeType.TREE);
			List<Edge> edges = graph.getOutEdges(root, EdgeType.TREE);
			for(Edge edge : edges) {
				graph.remove(edge);
			}
			int depth = rootToDepth.get(root);
			String sandwich = root + "mc" + depth ;
			
			depth++;
			rootToDepth.put(root, depth);
			graph.addNode(sandwich, new NodeData(NodeType.LABELLED));
			labels.addLabel(sandwich, labels.getLabel(root));
			System.err.println(sandwich + " -s-> " + labels.getLabel(root));
			
			for(String child : children) {
				graph.addEdge(sandwich,child, new EdgeData(EdgeType.TREE));
			}
			String plughole = root + "l" + depth;
			graph.addNode(plughole, new NodeData(NodeType.UNLABELLED));
			graph.addEdge(root, plughole, new EdgeData(EdgeType.TREE));
			graph.addEdge(plughole,sandwich, new EdgeData(EdgeType.DOMINANCE));
			
			String hole = root + "r" + depth;
			graph.addNode(hole, new NodeData(NodeType.UNLABELLED));
			graph.addEdge(root, hole, new EdgeData(EdgeType.TREE));
			idToHoles.get(id).push(hole);
		}
		
		private void createRelationFragment(int id) {
			String upper_root = "x" + relindex;
			String upper_lefthole = "xl" + relindex;
			String upper_righthole = "xr" + relindex;
			
			
			idToRoots.put(id, upper_root);
			Stack<String> holes = new Stack<String>();
			holes.push(upper_righthole);
			holes.push(upper_lefthole);
			idToHoles.put(id, holes);
			rootToDepth.put(upper_root, 1);
			
			graph.addNode(upper_root, new NodeData(NodeType.LABELLED));
			
			graph.addNode(upper_lefthole, new NodeData(NodeType.UNLABELLED));
			graph.addNode(upper_righthole, new NodeData(NodeType.UNLABELLED));
			
			graph.addEdge(upper_root, upper_lefthole, new EdgeData(EdgeType.TREE));
			graph.addEdge(upper_root, upper_righthole, new EdgeData(EdgeType.TREE));
			lastRoot = upper_root;
			
			relindex++;
		}
		
	}

}
