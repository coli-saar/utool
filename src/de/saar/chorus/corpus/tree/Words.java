/**
 * @file   Words.java
 * @author Alexander Koller
 * @date   Fri Jul  4 14:29:42 2003
 * 
 * @brief  Representation of the words in a tree.
 * 
 * 
 */


package de.saar.chorus.corpus.tree;

import java.util.*;


class Words {
    protected Hashtable idToWord;
    protected Hashtable idToPosition;
    protected Vector posToId;
    protected int nextPos;

    Words() {
	idToWord = new Hashtable();
	idToPosition = new Hashtable();
	posToId = new Vector();
	nextPos = 1;
    }

    Words(Words ws) {
	// Makes a deep copy of the idToWord hashtable and a shallow copy
	// of the idToPosition hashtable.
	// Copies of words belong to t.

	idToWord = new Hashtable(ws.idToWord.size());
	for( Enumeration en = ws.ids(); en.hasMoreElements(); ) {
	    Object key = en.nextElement();
	    Word oldWord = (Word) ws.idToWord.get(key);

	    idToWord.put(key, new Word(oldWord, this));
	}

	nextPos = ws.nextPos;

	idToPosition = (Hashtable) ws.idToPosition.clone();
	posToId = (Vector) ws.posToId.clone();
    }

    
    public boolean containsEntryForId(String id) {
	return idToWord.containsKey(id);
    }

    public Word get(String id) {
	return (Word) idToWord.get(id);
    }

    public Set keySet() {
	return idToWord.keySet();
    }

    public Enumeration ids() {
	return idToWord.keys();
    }

    public void put(String id, Word w) {
	idToWord.put(id,w);

	idToPosition.put(id, new Integer(nextPos));
	posToId.add(id);

	nextPos++;
    }

    public int getPosition(String id) {
	return ((Integer) idToPosition.get(id)).intValue();
    }

    public Word getWordAt(int pos) {
	return (Word) idToWord.get(posToId.get(pos));
    }
}
