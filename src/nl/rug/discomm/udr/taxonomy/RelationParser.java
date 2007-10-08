package nl.rug.discomm.udr.taxonomy;

import java.io.FileInputStream;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import nl.rug.discomm.udr.taxonomy.RelationFeatures.Feature;
import nl.rug.discomm.udr.taxonomy.RelationFeatures.RelationType;
import nl.rug.discomm.udr.taxonomy.RelationFeatures.ValueType;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;



public class RelationParser {

	private static RelationTaxonymy tax;
	
	public static void parseRelations(String filein)  throws IOException {
		try {
			SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
			parser.parse(new InputSource(new FileInputStream(filein)), new RelationHandler());		
		} catch(IOException e) {
			throw e;
		} catch(SAXException e) {
			e.printStackTrace();
		} catch(ParserConfigurationException e) {
			e.printStackTrace();
		}
	}
	
	
	private static class RelationHandler extends DefaultHandler {

		@Override
		public void endDocument() throws SAXException {
			System.err.println("Done!");
		}

		
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			if(qName.equals("feature") ) {
				
				ValueType type;
				String r = attributes.getValue("return");
				if(r.equals("boolean")) {
					type = ValueType.BOOLEAN;
				} else if(r.equals("string")) {
					type= ValueType.STRING;
				} else {
					type = null;
				}
				RelationFeatures.addFeature(attributes.getValue("name"), type);
			} else if(qName.equals("relation")) {
				RelationType type = null;
				String t = attributes.getValue("type");
				if(t.equals("rst")) {
					type = RelationType.RST;
				} else if(t.equals("sdrt")) {
					type = RelationType.SDRT;
				}
				RelationData data = new RelationData(attributes.getValue("name"),type);
				System.out.println(data.getName());
				for(Feature feat : RelationFeatures.getFeatures() ) {
					String val = attributes.getValue(feat.getName());
					
					if(val.equals("null")) {
						val = null;
					}
					System.out.println(feat.getName() + " : " + val);
					data.putFeature(feat, val);
				}
				tax.addRelation(data);
			}
		}

		@Override
		public void endElement(String uri, String localName, String name)
				throws SAXException {
			if(name.equals("feature")) {
				tax = RelationTaxonymy.getInstance();
			}
		}
		
		
		
	}
}
