/**
 * @file   Document.java
 * @author Alexander Koller
 * @date   Wed May 21 15:34:47 2003
 * 
 * @brief  Representation of all sentences in a whole document (= corpus).
 * 
 * @todo The methods should probably throw a specific exception, instead of
 * simply passing along the electric.xml.* one.
 */


package de.saar.chorus.corpus.tree;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;
import java.util.zip.GZIPInputStream;

import electric.xml.Element;
import electric.xml.Elements;
import electric.xml.XPath;



/**
 * Representation of all sentences in a whole document (= corpus).
 *
 * The document must conform to the Tiger XML export format. Each sentence
 * (elements at positions /corpus/body/s) must specify its words under the
 * path graph/terminals. Each sentence may further contain the following
 * pieces of information:
 *
 *  - a syntax tree, under graph/nonterminals;
 *  - a topology tree, under topology;
 *  - a scope graph, under scope (not yet implemented).
 * 
 */
public class Document {
    /** The XML document underlying this more abstract one. */
    protected electric.xml.Document doc;

    /** A vector of MultiTree representations of sentences. */
    protected Vector sentences; 

    /** A hashtable mapping sentence IDs to MultiTrees. */
    protected Hashtable sentHash;



    /** 
     * Construct a document from an XML document.
     * 
     * @param doc an Electric XML document.
     */
    Document(electric.xml.Document doc) { 
	initialize(doc);
    }

    /** 
     * Construct a document from a File.
     *
     * The file can be gzip-compressed; in this case, this constructor expects
     * that the filename ends in .gz.
     * 
     * @param f a File that will be parsed as an XML document.
     */
    Document(File f) throws electric.xml.ParseException {
	initialize(f);
    }

    /** 
     * Construct a document from a filename.
     * 
     * The file can be gzip-compressed; in this case, this constructor expects
     * that the filename ends in .gz.
     * 
     * @param filename the filename of an XML document.
     */
    Document(String filename) throws electric.xml.ParseException { 
	initialize(new File(filename));
    }

    /** 
     * Initialize the internal information from a parsed document.
     *
     * More concretely, this means that each sentence is converted into
     * a MultiTree in turn.
     * 
     * @param doc an Electric XML document adhering to the above specification.
     */
    protected void initialize(electric.xml.Document doc)  {
	this.doc = doc;

	Elements sents = doc.getRoot().getElements(new XPath("/corpus/body/s"));
	sentences = new Vector(sents.size());
	sentHash = new Hashtable(sents.size());

	while( sents.hasMoreElements() ) {
	    MultiTree mt = new MultiTree(sents.next(), this);
	    sentences.add(mt);
	    sentHash.put(mt.getID(), mt);
	}
    }

    /** 
     * Initialize the internal information from a file.
     *
     * The file can be gzip-compressed; in this case, this constructor expects
     * that the filename ends in .gz.
     * 
     * @param f the name of an XML file containing the information.
     */
    protected void initialize(File f) throws electric.xml.ParseException {
	InputStream is = null;

	try {

	    if( f.getCanonicalPath().endsWith(".gz") ) {
		    FileInputStream fis = new FileInputStream(f);
		    is = new GZIPInputStream(fis);
	    } else {
		is = new FileInputStream(f);
	    }
	} catch(Exception e) {
	    e.printStackTrace();
	    System.exit(1);
	}

	initialize(new electric.xml.Document(is));
    }

    /** 
     * A string representation of the document.
     * 
     * @return a string representation of the document.
     */
    public String toString() {
	return "(document with " + sentences.size() + " sentences)";
    }

    /** 
     * Return an enumeration over all sentences in the document.
     * 
     * @return An enumeration over all sentences in this document.
     */
    public MultiTrees getSentences() {
	return new MultiTrees(sentences);
    }

    /** 
     * Return the total number of sentences in the document.
     * 
     * @return The number of sentences.
     */
    public int numSentences() {
	return sentences.size();
    }

    /** 
     * Return the n-th sentence in the corpus.
     *
     * The first sentence has index 0.
     * 
     * @param idx the index of the sentence we want.
     * 
     * @return That sentence.
     */
    public MultiTree getSentenceAt(int idx) {
	return (MultiTree) sentences.get(idx);
    }

    /** 
     * Return the sentence for a given sentence ID.
     * 
     * @param id the ID of the sentence to retrieve.
     * 
     * @return A MultiTree object representing the annotations for that sentence.
     */
    public MultiTree getSentenceWithId(String id) {
	return (MultiTree) sentHash.get(id);
    }

    /** 
     * Return the XML element for a given sentence ID.
     *
     * The method returns a reference to this element, so any changes you make to
     * the element may change the XML document that is output. It doesn't affect
     * the abstract tree representations.
     *
     * In the future, the XML representing the sentences and their annotations may
     * be regenerated when the document is output, so don't rely on this.
     * 
     * @param id the ID of the sentence to retrieve.
     * 
     * @return An XML element representing the annotations for that sentence.
     */
    public Element getElementWithId(String id) {
	return doc.getElement(new XPath("/corpus/body/s[@id=\"" + id + "\"]"));
    }

    /** 
     * Return the IDs of all sentences in the corpus.
     * 
     * @return A set of String objects representing the IDs.
     */
    public Set getAllIDs() {
	return sentHash.keySet();
    }

    /** 
     * Compute a string representation of the XML document underlying the corpus.
     * 
     * @return That.
     */
    public String toXMLString() {
	return doc.toString();
    }
}
