/**
 * @file   Word.java
 * @author Alexander Koller
 * @date   Wed May 21 15:25:27 2003
 * 
 * @brief  A leaf of a tree, representing a word.
 * 
 * 
 */

package de.saar.chorus.corpus.tree;

import java.util.*;

import electric.xml.Element;



/**
 * A leaf of a tree, representing a word.
 * 
 * In addition to a generic node, Words interpret the attributes "word", "pos",
 * and "morph" as meaningful, and have specific accessor functions for them.
 */

public class Word extends Node {
    protected Words words;


    /** 
     * A dummy constructor that should never be called by the user.
     */
    Word(Words w) {
	super();
	words = w;
    }

    /** 
     * A constructor for an empty unconnected word in the tree t.
     * 
     * @param t the tree to which this word belongs.
     *
    Word(Tree t) {
	super(t);
    }
    */

    /**
     * Construct node from the information contained in an XML element.
     * This element must contain attributes "id" and "cat" from which we take
     * the word's ID and category. Further attributes of the element are
     * stored in the word's attributes.
     *
     * The new word is unconnected and belongs to the tree t.
     *
     * @param e the element from which to construct the word.
     * @param t the tree to which the word belongs.
     *
    Word(Element e, Tree t) {
	super(e,t);
    }
    */

    /** 
     * Construct node from the information contained in an XML element.
     * This element must contain attributes "id" and "cat" from which we take
     * the word's ID and category. Further attributes of the element are
     * stored in the word's attributes.
     *
     * The new word is unconnected and doesn't belong to any tree. This
     * constructor is necessary in the computation of the tree from the XML
     * document, when no trees are available yet, but words are.
     * 
     * @param e the element from which to construct the word.
     * 
     */
    Word(Element e, Words w) {
	super(e);
	words = w;
    }

    /** 
     * A copy constructor.
     *
     * The new node has fresh copies of the internal data structures, so
     * it can be moved into a different tree, or connected via different edges,
     * without affecting the original node.
     *
     * However, the constructor doesn't copy the objects that these structures 
     * refer to (e.g. the edges that connect this node to others). So a destructive
     * change to such an object will affect all copies of the node.
     * 
     * @param n the node to be copied.
     */
    Word(Word w) {
	super(w);
	words = w.words;
    }

    Word(Word w, Words ws) {
	super(w);
	words = ws;
    }

    /** 
     * A copy constructor that moves the copy to a different tree.
     *
     * The constructor makes a copy of its first argument using the copy constructor,
     * which see. The only difference is that it makes the new word belong to the tree
     * t. Essentially, this creates a copy of a word in a different tree.
     * 
     * @param w the word to be copied.
     * @param t the tree to which the copy should belong.
     *
    Word(Word w, Tree t) {
	super(w);
	
	tree = t;
    }
    */


    /** 
     * Return the word string contained in this Word.
     * 
     * @return the word itself, as a string.
     */
    public String getWord() {
	return (String) attributes.get("word");
    }

    /** 
     * Return the part-of-speech tag of this Word.
     * 
     * @return the POS tag of the word.
     */
    public String getPOS() {
	return (String) attributes.get("pos");
    }

    /** 
     * Return the morphology information of this Word.
     * 
     * @return  this word's morphology information.
     */
    public String getMorph() {
	return (String) attributes.get("morph");
    }

    
    /** 
     * A string representation of the word.
     * 
     * @return a string representation of the word.
     */
    public String toString() {
	return "[" + id + ":" + getWord() + "]";
    }

    /** 
     * The yield of a word is simply the word itself, as a string.
     * 
     * @return the yield of the subtree below this node.
     */
    public String yield() {
	return getWord();
    }

    /** 
     * Is this word punctuation?
     *
     * This is recognized by the POS tag beginning with the symbol $.
     * 
     * @return Is the word punctuation?
     */    public boolean isPunct() {
	return getPOS().charAt(0) == '$';
    }

    /** 
     * Create a new copy of this node by calling the copy constructor.
     * 
     * @return A new Word that looks just like me.
     */
    public Object clone() {
	return new Word(this);
    }

    public int getPosition() {
	return words.getPosition(getID());
    }

    public int startPos() {
	return getPosition();
    }

    public int endPos() {
	return getPosition();
    }
}
