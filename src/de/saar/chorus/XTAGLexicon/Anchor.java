/*
 * Anchor.java
 */

package de.saar.chorus.XTAGLexicon;

public class Anchor {
    
    private String stem;
    private String pos;
    private boolean special;
    
    public Anchor (String root, String pos, boolean special) {
        this.stem = root;
        this.pos = pos;
        this.special = special;
    }
    
    public String getStem() {
        return stem;
    }
    
    public String getPos() {
        return pos;
    }
    
    public int hashCode() {
        return stem.hashCode(); 
    }
    
    public boolean isSpecial() {
        return special;
    }
    
}
