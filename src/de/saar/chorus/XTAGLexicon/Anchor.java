/*
 * Anchor.java
 */

package de.saar.chorus.XTAGLexicon;

public class Anchor {

    private String root;
    private String pos;
    private boolean special;

    public Anchor (String root, String pos, boolean special){
	this.root = root;
	this.pos = pos;
	this.special = special;
    }

    public String getRoot() {
	return root;
    }

    public String getPos() {
	return pos;
    }

    public int hashCode() {
	return root.hashCode(); 
    }

    public boolean isSpecial (){
	return special;
    }

}
