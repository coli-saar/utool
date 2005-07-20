/*
 * MorphInfo.java
 */

package de.saar.chorus.XTAGLexicon;

public class MorphInfo {

    private String root;
    private String pos;

    public MorphInfo (String root, String pos){
	this.root = root;
	this.pos = pos;
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
}
