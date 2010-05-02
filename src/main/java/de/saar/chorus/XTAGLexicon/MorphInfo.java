/*
 * MorphInfo.java
 */

package de.saar.chorus.XTAGLexicon;


/*
 * An object of this class represents a single line of morph_english.db.flat.
 * 
 */

public class MorphInfo {
    private String root;
    private String pos;
    
    private String agr; // value of the agreement feature
    
    
    public MorphInfo (String root, String pos, String agr){
        this.root = root;
        this.pos = pos;
        this.agr = agr;
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

    public String getAgr() {
        return agr;
    }
}
