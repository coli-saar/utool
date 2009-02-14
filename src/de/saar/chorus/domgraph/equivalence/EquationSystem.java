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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A representation for a term equation system. Such an equation
 * system can be used as input for the redundancy elimination
 * algorithm.
 *
 * @author Alexander Koller
 *
 */
public class EquationSystem extends DefaultHandler {
    //private final Collection<Equation> equations;
    private final Collection<FragmentWithHole> wildcards;
    private Set<String> wildcardLabels;
    
    private Set<String> equationStrings;

    // for XML parsing
    private List<FragmentWithHole> currentEquivalenceGroup;
    private FragmentWithHole currentEquivalencePartner;

    public EquationSystem() {
        super();

        // equations = new HashSet<Equation>();
        wildcards = new HashSet<FragmentWithHole>();
        wildcardLabels = new HashSet<String>();
        
        equationStrings = new HashSet<String>();

        currentEquivalenceGroup = null;
        currentEquivalencePartner = null;

    }
    
	private String constructKey(String llabel, int lhole, String rlabel, int rhole) {
		return llabel + "/" + lhole + "-" + rlabel + "/" + rhole ;
	}

    
    public boolean permutes(String f, int i, String g, int k) {
    	return isWildcard(f,i) || isWildcard(g, k) || equationStrings.contains(constructKey(f, i, g, k));
    }

    public boolean isWildcard(String label, int holeindex) {
        return wildcards.contains(new FragmentWithHole(label, holeindex));
    }

    public boolean isWildcardLabel(String label) {
    	return wildcardLabels.contains(label);
    }


    /**
     * Add an equation between two label-hole pairs.
     *
     * @param fh1 a label-hole pair
     * @param fh2 another label-hole pair
     */
    public void add(FragmentWithHole fh1, FragmentWithHole fh2) {
    	equationStrings.add(constructKey(fh1.getRootLabel(), fh1.getHoleIndex(), fh2.getRootLabel(), fh2.getHoleIndex()));
    }
    
    public void addWildcard(FragmentWithHole fh) {
        wildcards.add(fh);
        wildcardLabels.add(fh.getRootLabel());
    }

    /**
     * Add equations between any two members of a collection
     * of label-hole pairs.
     *
     * @param fhs a collection of label-hole pairs
     */
    public void addEquivalenceClass(Collection<FragmentWithHole> fhs) {
        for( FragmentWithHole fh1 : fhs ) {
            for( FragmentWithHole fh2 : fhs) {
                add(fh1, fh2);
            }
        }
    }

    /**
     * Returns the number of equations.
     *
     * @return the number of equations
     */
    public int size() {
        return equationStrings.size();
    }

    /**
     * Reads an equation system from an XML specification. The specification
     * can use the following constructions:
     * <ul>
     * <li> Define a group of label-hole pairs that are equivalent with
     *      each other:<br/>
     *      {@code <equivalencegroup>}<br/>
     *       &nbsp; {@code <quantifier label="a" hole="0" />} <br/>
     *       &nbsp; {@code <quantifier label="a" hole="1" />} <br/>
     *      {@code </equivalencegroup>}
     * <li> Define a single label-hole pair that is equivalent with
     * <i>everything</i> (useful for e.g. proper names):<br/>
     *      {@code <permutesWithEverything label="proper_q" hole="1" />}
     * </ul>
     *
     *
     * @param reader a reader from which the specification is read
     * @throws ParserConfigurationException if an error occurred while
     * configuring the XML parser
     * @throws SAXException if an error occurred while parsing
     * @throws IOException if an I/O error occurred while reading
     * from the reader.
     */
    public void read(Reader reader)
    throws ParserConfigurationException, SAXException, IOException {
        SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
        saxParser.parse( new InputSource(reader), this );
    }

    @Override
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
        } else if( qName.equals("permutesWithEverything")) {
            FragmentWithHole frag =
                new FragmentWithHole(
                        attributes.getValue("label"),
                        Integer.parseInt(attributes.getValue("hole")));
            addWildcard(frag);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if( qName.equals("equivalencegroup")) {
            addEquivalenceClass(currentEquivalenceGroup);
            currentEquivalenceGroup = null;
        } else if( qName.equals("equivalencepartners")) {
            currentEquivalencePartner = null;
        }
    }

    /**
     * Returns a string representation of this equation system.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        for( String eq : equationStrings ) {
            buf.append("  " + eq + "\n");
        }
        return buf.toString();
    }

}
