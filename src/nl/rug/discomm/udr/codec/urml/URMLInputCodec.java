package nl.rug.discomm.udr.codec.urml;

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

@CodecMetadata(name="urml-input", extension=".urml.xml", experimental=true)
public class URMLInputCodec extends InputCodec {

	@Override
	public void decode(Reader reader, DomGraph graph, NodeLabels labels)
			throws IOException, ParserException, MalformedDomgraphException {
		
		try {
			SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
			InputSource in = new InputSource(reader);
			in.setEncoding("UTF-8");
			parser.parse(new InputSource(reader), new URMLHandler(graph, labels));		
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

	
	private class URMLHandler extends DefaultHandler {
		
		private DomGraph graph;
		private NodeLabels labels;
		private Set<String> relationTags, domTags;
		private String nodeToLabel, lastRoot;
		private StringBuffer currentLabel;
		private boolean collectLabel;
		private Stack<String> holesToPlug;
		private Map<String, String> domEdges;
		private Map<String, Integer> rootsToDepth; // for binarization
		
		URMLHandler(DomGraph g, NodeLabels l) {
			graph = g;
			labels = l;
			collectLabel = false;
			holesToPlug = new Stack<String>();
			
			relationTags = new HashSet<String>();
			relationTags.add("hypRelation");
			relationTags.add("parRelation");
			relationTags.add("relation");
			
			domTags = new HashSet<String>();
			domTags.add("nucleus");
			domTags.add("satellite");
			domTags.add("element");
			
			domEdges = new HashMap<String, String>();
			rootsToDepth = new HashMap<String, Integer>();
		}

		@Override
		public void characters(char[] ch, int start, int length)
				throws SAXException {
			if( collectLabel ) {
				currentLabel.append(new String(ch, start,length));
			}
		}

		@Override
		public void startElement(String uri, String localName, String name,
				Attributes attributes) throws SAXException {
			
			if( name.equals("segment") ) {
				String next = attributes.getValue("id");
				nodeToLabel = next;
				graph.addNode(next, new NodeData(NodeType.LABELLED));
				currentLabel = new StringBuffer();
			} else if ( name.equals("sign") ) {
				collectLabel = true;
			} else if( relationTags.contains(name) ) {
				String root = attributes.getValue("id");
				String label = attributes.getValue("type");
				if( label == null ) {
					label = "rel";
				}
				createRelationFragment(root, label);
			} else if( domTags.contains(name) ) {
				if( holesToPlug.isEmpty() ) {
					makeSandwichFragment(lastRoot);
				}
				domEdges.put(holesToPlug.pop(), attributes.getValue("id"));
			}
		}

		@Override
		public void endElement(String uri, String localName, String name)
				throws SAXException {
			if( name.equals("segment") ) {
				labels.addLabel(nodeToLabel, currentLabel.toString());
				collectLabel = false;
			}
		}
		
		
		
		
		@Override
		public void endDocument() throws SAXException {
			for( Map.Entry<String, String> dompair : domEdges.entrySet()) {
				graph.addEdge(dompair.getKey(), dompair.getValue(), new EdgeData(EdgeType.DOMINANCE));
			}
		}

		
		private void makeSandwichFragment(String root) {
			List<String> children = graph.getChildren(root, EdgeType.TREE);
			List<Edge> edges = graph.getOutEdges(root, EdgeType.TREE);
			for(Edge edge : edges) {
				graph.remove(edge);
			}
			int  d = rootsToDepth.get(root);
			String sandwich = root + "mc" + d ;
			
			d++;
			rootsToDepth.put(root, d);
			graph.addNode(sandwich, new NodeData(NodeType.LABELLED));
			labels.addLabel(sandwich, labels.getLabel(root));
			
			for(String child : children) {
				graph.addEdge(sandwich,child, new EdgeData(EdgeType.TREE));
			}
			String plughole = root + "l" + d;
			graph.addNode(plughole, new NodeData(NodeType.UNLABELLED));
			graph.addEdge(root, plughole, new EdgeData(EdgeType.TREE));
			graph.addEdge(plughole,sandwich, new EdgeData(EdgeType.DOMINANCE));
			
			String hole = root + "r" + d;
			graph.addNode(hole, new NodeData(NodeType.UNLABELLED));
			graph.addEdge(root, hole, new EdgeData(EdgeType.TREE));
			holesToPlug.push(hole);
		}
		
		public void createRelationFragment(String root, String type) {
			String upper_root = root;
			String upper_lefthole = root + "hl";
			String upper_righthole = root + "hr";
			
			
		
			graph.addNode(upper_root, new NodeData(NodeType.LABELLED));
			labels.addLabel(upper_root, type);
			rootsToDepth.put(upper_root, 1);
			lastRoot = upper_root;
			
			graph.addNode(upper_lefthole, new NodeData(NodeType.UNLABELLED));
			graph.addNode(upper_righthole, new NodeData(NodeType.UNLABELLED));
			
			graph.addEdge(upper_root, upper_lefthole, new EdgeData(EdgeType.TREE));
			graph.addEdge(upper_root, upper_righthole, new EdgeData(EdgeType.TREE));
			
			holesToPlug.push(upper_righthole);
			holesToPlug.push(upper_lefthole);
		}
		
		
	}
}
