/*
 * @(#)Equations.java created 06.02.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.equivalence;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class EquationSystem extends DefaultHandler {
    private List<Equation> equations;
    
    // for XML parsing
    private List<FragmentWithHole> currentEquivalenceGroup;
    private FragmentWithHole currentEquivalencePartner;
    
    public EquationSystem() {
        super();
        
        equations = new ArrayList<Equation>();
        currentEquivalenceGroup = null;
        currentEquivalencePartner = null;
    }
    
    public void add(FragmentWithHole fh1, FragmentWithHole fh2) {
        equations.add(new Equation(fh1,fh2));
    }
    
    public void addEquivalenceClass(Collection<FragmentWithHole> fhs) {
        for( FragmentWithHole fh1 : fhs ) {
            for( FragmentWithHole fh2 : fhs) {
                add(fh1, fh2);
            }
        }
    }
    
    public void clear() {
        equations.clear();
    }
    
    public boolean contains(Equation eq) {
        return equations.contains(eq);
    }
    
    public int size() {
        return equations.size();
    }
    
    public void read(Reader reader) 
    throws ParserConfigurationException, SAXException, IOException {
        SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
        saxParser.parse( new InputSource(reader), this );
    }

    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if( qName.equals("equivalencegroup")) {
            currentEquivalenceGroup = new ArrayList<FragmentWithHole>();
        } else if( qName.equals("equivalencepartners")) {
            int hole = Integer.parseInt(attributes.getValue("hole"));
            currentEquivalencePartner =
                new FragmentWithHole(attributes.getValue("label"), hole);
        } else if( qName.equals("quantifier") ) {
            int hole = Integer.parseInt(attributes.getValue("hole"));
            FragmentWithHole fh =
                new FragmentWithHole(attributes.getValue("label"), hole);
            
            if( currentEquivalencePartner != null ) {
                add(currentEquivalencePartner, fh);
            } else if( currentEquivalenceGroup != null ) {
                currentEquivalenceGroup.add(fh);
            }
        }
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        if( qName.equals("equivalencegroup")) {
            addEquivalenceClass(currentEquivalenceGroup);
            currentEquivalenceGroup = null;
        } else if( qName.equals("equivalencepartners")) {
            currentEquivalencePartner = null;
        }
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        for( Equation eq : equations ) {
            buf.append("  " + eq + "\n");
        }
        return buf.toString();
    }
    
}
