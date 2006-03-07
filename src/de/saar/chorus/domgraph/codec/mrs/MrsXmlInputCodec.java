package de.saar.chorus.domgraph.codec.mrs;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import de.saar.chorus.domgraph.codec.InputCodec;
import de.saar.chorus.domgraph.codec.MalformedDomgraphException;
import de.saar.chorus.domgraph.codec.ParserException;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;

public class MrsXmlInputCodec extends InputCodec {
	
	private MrsCodec codec;
	
	public MrsXmlInputCodec()
	{
		super();
	}
	
	public static String getName()
	{
		return "mrs-xml";
	}
	
	public static String getExtension()
	{
		return ".mrs.xml";
	}
	
	private class XmlParser extends DefaultHandler {
		
		private Stack<String> parents;
		private Map<String,String> attrs;
		private String attr;
		private String top;
		private String label;
		private String value;
		private String hi;
		private String lo;
		private String handle;
		
		public XmlParser()
		{
			super();
			this.parents = new Stack<String>();
			this.attrs = new TreeMap<String,String>();
			this.top = "";
			this.handle = "";
			this.label = "";
			this.value = "";
			this.hi = "";
			this.lo = "";
		}
		
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
		{
			if (qName.equals("var")) {
				String vid = attributes.getValue("vid");
				String parent = parents.peek();
				
				try {
					if (vid.startsWith("x")) {
						codec.tellVariable(vid);
					} else if (vid.startsWith("h")) {
						codec.tellHandle(vid);
					} 
				} catch (MalformedDomgraphException e) {
					throw new SAXException(e);
				}
				
				if (parent.equals("mrs")) {
					this.top = vid;
				} else if (parent.equals("ep")) {
					this.handle = vid;
				} else if (parent.equals("fvpair")) {
					this.value = vid;
				} else if (parent.equals("hi")) {
					this.hi = vid;
				} else if (parent.equals("lo")) {
					this.lo = vid;
				}
			} else if (qName.equals("ep")) {
				attrs = new TreeMap<String,String>();
			}
			
			parents.push(qName);
		}
		
		public void endElement (String uri, String name, String qName) throws SAXException
		{
			try {
				if (qName.equals("mrs")) {
					codec.setTopHandleAndFinish(top);	
				} else if (qName.equals("hcons")) {
					codec.addDomEdge(hi, lo);  			
				} else if (qName.equals("ep")) {
					codec.addRelation(handle, label, attrs);			
				} else if (qName.equals("fvpair")) {
					attrs.put(attr, value);
				}
			} catch (MalformedDomgraphException e) {
				throw new SAXException(e);
			}
			parents.pop();
		}
		
		public void characters (char ch[], int start, int length)
		{
			String parent = parents.peek();
			
			if (parent.equals("pred")) {
				this.label = new String(ch, start, length);
			} else if (parent.equals("rargname")) {
				this.attr = new String(ch, start, length);
			} else if (parent.equals("constant")) {
				this.value = new String(ch, start, length);
			}
		}
	}
	
	public void decode(Reader inputStream, DomGraph graph, NodeLabels labels) throws IOException, ParserException, MalformedDomgraphException
	{
		codec = new MrsCodec(graph, labels);
		
		try {
			SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
			parser.parse(new InputSource(inputStream), new XmlParser());
		}  catch(IOException e) {
			throw e;
		} catch(Throwable e) {
			throw new ParserException(e);
		}
	}
	
}
