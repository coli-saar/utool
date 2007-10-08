package nl.rug.discomm.udr.codec.segments;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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


@CodecMetadata(name="preds-segmenter", extension=".pr.xml", experimental=true)
public class PredsToSegmentsInputCodec extends InputCodec {

	
	// tags
	static String subcl = "SubjS";
	static String maincl = "KS";
	static String word = "word";
	static String sentence = "sent";
	static String morph = "Morphology";
	static String alt = "StS";
	
	// attributes
	static String start = "start";
	static String end = "end";
	static String reading ="tid";
	static String token = "string";
	static String id = "position";

	
	
	
	
	@Override
	public void decode(Reader reader, DomGraph graph, NodeLabels labels) throws IOException, ParserException, MalformedDomgraphException {
		
		try {
			SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
			parser.parse(new InputSource(reader), new PredsHanlder(graph,labels));		
		} catch(IOException e) {
			throw e;
		}  catch(SAXException e) {
			if( (e.getException() != null) && (e.getException() instanceof MalformedDomgraphException) ) {
				throw (MalformedDomgraphException) e.getException();
			} else {			
				throw new ParserException(e);
			}
		} catch(ParserConfigurationException e) {
			throw new ParserException(e);
		}
	}


	
	private class PredsHanlder extends DefaultHandler {

		List<String> currentSentence;
		int currentSentenceEnd;
		StringBuffer currentSegment;
		int currentreading, counter;
		List<Integer> boundaries;
		boolean inmorph;
		private String lastSegment;
		private NodeLabels labels;
		private DomGraph graph;
		private boolean firstSegment;
		
		public PredsHanlder(DomGraph dg, NodeLabels la) {
			graph = dg;
			graph.clear();
			labels = la;
			currentSentence = new ArrayList<String>();
			currentreading = 1;
			currentSegment = new StringBuffer();
			counter = 0;
			boundaries = new ArrayList<Integer>();
			inmorph = false;
			firstSegment = true;
			lastSegment = "";
			currentSentenceEnd = 0;
		}
		
		
		@Override
		public void endElement(String uri, String name, String qName) throws SAXException {

			if(qName.endsWith(morph)) {
				
				inmorph = false;
			} else if(qName.equals(sentence)) {
				int lastBoundary = 1;
				Collections.sort(boundaries);
				
				for(Integer ind : boundaries) {
					StringBuffer segment = new StringBuffer();
					for(int i = lastBoundary; i <= ind && i < currentSentence.size() ; i++) {
						segment.append(currentSentence.get(i-1) + " ");
					}
					String lower = segment.toString();
					graph.addNode(lower, new NodeData(NodeType.LABELLED));
					labels.addLabel(lower, "c" + counter);
					
					if( ! firstSegment ) {
						String upper_root = "x" + counter;
						String upper_lefthole = "xl" + counter;
						String upper_righthole = "xr" + counter;
						
						
						graph.addNode(upper_root, new NodeData(NodeType.LABELLED));
			    		labels.addLabel(upper_root, "rel");
			    		
			    		graph.addNode(upper_lefthole, new NodeData(NodeType.UNLABELLED));
			    		graph.addNode(upper_righthole, new NodeData(NodeType.UNLABELLED));
			    		
			    		graph.addEdge(upper_root, upper_lefthole, new EdgeData(EdgeType.TREE));
			    		graph.addEdge(upper_root, upper_righthole, new EdgeData(EdgeType.TREE));
			    		
			    		graph.addEdge(upper_lefthole, lastSegment, new EdgeData(EdgeType.DOMINANCE));
			    		graph.addEdge(upper_righthole, lower, new EdgeData(EdgeType.DOMINANCE));
						
					} else {
						firstSegment = false;
					}
			
						lastBoundary = ind+1;
						counter++;
						lastSegment = lower;
				}
				
				
				currentSentence = new ArrayList<String>();
				currentreading = 1;
				boundaries = new ArrayList<Integer>();
			}
		}

		
		//getcharacter data? --> cdata
		
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		  if(qName.equals(morph) ) {
				inmorph = true;
			}	else if(qName.equals(word) && inmorph) {
				currentSentence.add(attributes.getValue(token));
			} else if(qName.equals(maincl)) {
				System.err.println(attributes.getValue(reading));
				if(attributes.getValue(reading) == null ||
						attributes.getValue(reading).startsWith("1")) {
					int clauseend = Integer.parseInt(attributes.getValue(end));
					boundaries.add(clauseend);
					currentSentenceEnd = clauseend;
				}
					
			} else if(qName.equals(subcl)) {
				if(attributes.getValue(reading) == null ||
						attributes.getValue(reading).startsWith("1")) {
					int s = Integer.parseInt(attributes.getValue(start));
					if(s != 1) {
						boundaries.add(Integer.parseInt(attributes.getValue(start)));
						
					}
					int subjend = Integer.parseInt(attributes.getValue(end));
					if(subjend < currentSentenceEnd && 
							! boundaries.contains(subjend)) {
						boundaries.add(subjend);
					}
				}
			} else if (qName.equals(alt)) {
				int s = Integer.parseInt(attributes.getValue(start));
				
				int subjend = Integer.parseInt(attributes.getValue(end));
				if(subjend < currentSentenceEnd && 
						! boundaries.contains(subjend)) {
					boundaries.add(subjend);
				}
			}
		}
		
		

	

		
	}
	
	
}
