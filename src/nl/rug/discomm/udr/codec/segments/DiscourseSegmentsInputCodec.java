package nl.rug.discomm.udr.codec.segments;



import java.io.IOException;
import java.io.Reader;

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


@CodecMetadata(name="discourse-segments", extension=".ds.xml", experimental=true)
public class DiscourseSegmentsInputCodec extends InputCodec {

	
	

	
	@Override
	public void decode(Reader reader, DomGraph graph, NodeLabels labels) throws IOException, ParserException, MalformedDomgraphException {
		
		
		try {
			SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
			parser.parse(new InputSource(reader), new SegmentHandler(graph, labels));		
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
	
	
	private class SegmentHandler extends DefaultHandler {
		
		private String lastSegment;
		private NodeLabels labels;
		private DomGraph graph;
		private boolean firstSegment;
		
		SegmentHandler(DomGraph graph, NodeLabels labels) {
			this.labels = labels;
			this.graph = graph;
			this.graph.clear();
			lastSegment = "";
			firstSegment = true;
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			if(qName.equals("segment")) {
				String lower = attributes.getValue("string");
				String counter = attributes.getValue("id");
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
				
				lastSegment = lower;
			}
		}
		
	}
	

	
}
