package nl.rug.discomm.udr.codec.sdrt;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

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

@CodecMetadata(name="sdrt-rosie-input", extension=".sdrs.xml", experimental=true)
public class SDRTDialogueInputCodec extends InputCodec {

	@Override
	public void decode(Reader reader, DomGraph graph, NodeLabels labels)
			throws IOException, ParserException, MalformedDomgraphException {
	
		try {
			SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
			parser.parse(new InputSource(reader), new SDRTDialogueHandler(graph, labels));		
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
	
	private class SDRTDialogueHandler extends DefaultHandler {
		private DomGraph graph;
		private NodeLabels labels;
		private int depth;
		private List<Stack<String>> rootsToName, holesToPlug;
		private String lastRoot, lastLeaf;
		private int leafindex, relindex;
		private Set<String> taken, leafMarkers;
		private List<String> fatherless;
		private List<Boolean> placeholder;
		
		
		public SDRTDialogueHandler(DomGraph graph, NodeLabels labels) {
			this.graph = graph;
			this.labels = labels;
			depth = -1;
			rootsToName = new ArrayList<Stack<String>>();
			holesToPlug = new ArrayList<Stack<String>>();
			lastRoot = null;
			lastLeaf = null;
			leafindex = 0;
			relindex = 1;
			taken = new HashSet<String>();
			leafMarkers = new HashSet<String>();
			
			leafMarkers.add("ind");
			leafMarkers.add("int");
			leafMarkers.add("imp");
			leafMarkers.add("irr");
			leafMarkers.add("pause");
			leafMarkers.add("pls");
			
			fatherless = new ArrayList<String>();
			placeholder = new ArrayList<Boolean>();
			
		}

		@Override
		public void endElement(String uri, String localName, String name)
		throws SAXException {
			if(name.equals("node")) {

				depth--;
				if( depth < fatherless.size() - 2 ) {
					Stack<String> holes = holesToPlug.get(depth+1);

					if(fatherless.get(depth+2) != null 
							&& (holes.size() == 1) ) {
						String leaf = fatherless.get(depth +2);
						graph.addEdge(holes.pop(), leaf, new EdgeData(EdgeType.DOMINANCE));
						fatherless.remove(depth+2);
						fatherless.add(depth+1, null);
						taken.add(leaf);
					}
				} 
			}
		}
		

		@Override
		public void startElement(String uri, String localName, String name,
				Attributes attributes) throws SAXException {
			if(name.equals("node")) {
				depth++;
				if(depth == rootsToName.size()) {
					rootsToName.add(depth, new Stack<String>());
					holesToPlug.add(depth, new Stack<String>());
				}
				String rel = attributes.getValue("pod");
				System.err.println("Relation: " + rel);
				
				if( leafMarkers.contains(rel) ) {
					
					String leaf = "y" + leafindex;
					graph.addNode(leaf, new NodeData(NodeType.LABELLED));
					labels.addLabel(leaf, attributes.getValue("item-id"));
					leafindex++;
					int d = depth -1;
					
					Stack<String> holes = holesToPlug.get(d);
					if((holes.size() == 1) && (d != 0) ) {
						System.err.println(holes);
					//	graph.addEdge(holes.pop(), leaf, new EdgeData(EdgeType.DOMINANCE));
						System.err.println("...plugged in hole!");
					} else {
						System.err.println("...passed on!");
						
						lastLeaf = leaf;
					}
					lastRoot = leaf;
					
					if(fatherless.size() < depth) {
						for( int i = fatherless.size(); i<=depth; i++) {
							fatherless.add(null);
						}
					} 
					
					fatherless.add(depth,leaf);
					
						
					placeholder.add(depth,false);
					
				} else if( rel.equals("dseg") ) {
					rootsToName.get(depth).add(createRelationFragment(false,placeholder.get(depth-1)));
					placeholder.add(depth, true); 
				} else if( rel.equals("top") ) {
					// do nothing?
					placeholder.add(false);
					createTopFragment();
				}  else {
					if(rootsToName.get(depth).isEmpty()) {
						String nextroot = createRelationFragment(true, placeholder.get(depth-1));
						labels.addLabel(nextroot, rel);
					} else {
						String lr = rootsToName.get(depth).pop();
						labels.addLabel(lr, rel);
					}
					placeholder.add(depth,false);
				}
			}
		}
		
		
		private void createTopFragment() {
			String root = "xtop";
			String hole = "th";
			
			graph.addNode(root, new NodeData(NodeType.LABELLED));
			graph.addNode(hole, new NodeData(NodeType.UNLABELLED));
			graph.addEdge(root,hole, new EdgeData(EdgeType.TREE));
			
			labels.addLabel(root, "TOP");
			holesToPlug.get(depth).push(hole);
		}
		
		/**
		 * 
		 * @param plug
		 * @return the root of the relation fragment
		 */
		private String createRelationFragment(boolean plugroot, boolean plughole) {
			String upper_root = "x" + relindex;
			String upper_lefthole = "xl" + relindex;
			String upper_righthole = "xr" + relindex;
			
			
		
			graph.addNode(upper_root, new NodeData(NodeType.LABELLED));
			
			graph.addNode(upper_lefthole, new NodeData(NodeType.UNLABELLED));
			graph.addNode(upper_righthole, new NodeData(NodeType.UNLABELLED));
			
			graph.addEdge(upper_root, upper_lefthole, new EdgeData(EdgeType.TREE));
			graph.addEdge(upper_root, upper_righthole, new EdgeData(EdgeType.TREE));
			relindex++;
			holesToPlug.get(depth).push(upper_righthole);
			
			if( plugroot && lastRoot != null ) {
				System.err.println(lastRoot + " <-- " + upper_lefthole + "?");
				if(! taken.contains(lastRoot)) {
					graph.addEdge(upper_lefthole, lastRoot, new EdgeData(EdgeType.DOMINANCE));
					if(fatherless.contains(lastRoot)) {
						int index = fatherless.lastIndexOf(lastRoot);
						fatherless.remove(index);
						fatherless.add(index,null);
					}
		
					taken.add(lastRoot);
					System.err.println(upper_lefthole + " --> " + lastRoot);
				}
			} else {
				holesToPlug.get(depth).push(upper_lefthole);
			}
			
			int d = depth -1;
			Stack<String> holes = holesToPlug.get(d);
			
			if(plughole || ( (! plughole) && (holes.size() == 1) )) {
				graph.addEdge(holes.pop(), upper_root, new EdgeData(EdgeType.DOMINANCE));
			} else {
				lastRoot = upper_root;
			}
			
			
		
			
			return upper_root;
		}
		
	}

}
