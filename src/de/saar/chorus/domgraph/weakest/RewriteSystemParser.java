package de.saar.chorus.domgraph.weakest;

import java.io.IOException;
import java.io.Reader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class RewriteSystemParser extends DefaultHandler {
	private static enum State {NONE, READING_ANNOTATOR, READING_REWRITE_SYSTEM};
	private State state;
	private Annotator annotator;
	private RewriteSystem rewriteSystem;
	private String parentAnnotation, parentLabel;
	
	public RewriteSystemParser() {
		state = State.NONE;
	}
	
	public void read(Reader reader, Annotator annotator, RewriteSystem rewriteSystem) throws ParserConfigurationException, SAXException, IOException {
		this.annotator = annotator;
		this.rewriteSystem = rewriteSystem;
		
		SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
        saxParser.parse( new InputSource(reader), this );
	}
	
	@Override
	public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
		if( name.equals("annotator")) {
			state = State.READING_ANNOTATOR;
			annotator.setStart(attributes.getValue("initial"));
		} else if( name.equals("rewriting")) {
			state = State.READING_REWRITE_SYSTEM;
		} else if( name.equals("rule")) {
			switch(state) {
			case READING_ANNOTATOR:
				parentAnnotation = attributes.getValue("annotation");
				parentLabel = attributes.getValue("label");
				break;
			case READING_REWRITE_SYSTEM:
				rewriteSystem.addRule(attributes.getValue("llabel"), Integer.parseInt(attributes.getValue("lhole")),
						attributes.getValue("rlabel"), Integer.parseInt(attributes.getValue("rhole")),
						attributes.getValue("annotation"));
				break;
			}
		} else if( name.equals("hole")) {
			annotator.addRule(parentAnnotation, parentLabel, attributes.getValue("annotation"));
		}
	}
}
