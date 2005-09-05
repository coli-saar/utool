/*
 * @(#)UnificationEquation.java created 25.08.2005
 * 
 * Copyright (c) 2005 Alexander Koller
 *  
 */

package de.saar.chorus.XTAGLexicon;

public class UnificationEquation {
    static enum TermType {
        feature, value;
    }
    
    static enum Side {
        top, bottom;
    }
    
    static public class Term {
        public TermType type;
    }
    
    static public class Feature extends Term {
        public String nodename, feature;
        public Side side;
        
        public Feature() {
            type = TermType.feature;
        }
        
        public Feature(String nodename, Side side, String feature) {
            type = TermType.feature;
            this.nodename = nodename;
            this.side = side;
            this.feature = feature;
        }
        
        public String toString() {
            return nodename + "." + side + ":<" + feature + ">";
        }
        
        public boolean equals(Feature f) {
            return toString().equals(f.toString());
        }
        
        public int hashCode() {
            return toString().hashCode();
        }
    }
    
    static public class Value extends Term {
        public String value;
        
        public Value() {
            type = TermType.value;
        }
        
        public String toString() {
            return value;
        }
    }
    
    private Term lhs, rhs;
    
    public UnificationEquation(Term lhs, Term rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
    }

    public Term getLhs() {
        return lhs;
    }

    public Term getRhs() {
        return rhs;
    }
    
    public String toString () {
        return lhs + " = " + rhs;
    }
}
