/*
 * @(#)Term.java created 11.05.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.term;

import java.io.StringReader;

import org.testng.annotations.Test;

import de.saar.chorus.term.parser.ParseException;
import de.saar.chorus.term.parser.TermParser;

public abstract class Term {
    public static Term parse(String string) {
        try {
            TermParser p = new TermParser(new StringReader(string));
            return p.term();
        } catch(ParseException e) {
            System.err.println(e);
            return null;
        }
    }
    
    public boolean isVariable() {
        return getType() == Type.VARIABLE;
    }
    
    public boolean isConstant() {
        return getType() == Type.CONSTANT;
    }
    
    public boolean isCompound() {
        return getType() == Type.COMPOUND;
    }
    
    public abstract Type getType();
    
    public abstract boolean hasSubterm(Term other);
    
    public abstract Substitution getUnifier(Term other);
    
    public Term unify(Term other) {
        Substitution mgu = getUnifier(other);
        
        if( mgu == null ) {
            return null;
        } else {
            return mgu.apply(this);
        }
    }

    // TODO this is a hack
    public int hashCode() {
        return toString().hashCode();
    }
    
    
    
    
    
    
    
    

    /***************************************************************
     * UNIT TESTS
     ***************************************************************/

    @Test(groups = {"Term"})
    public static class UnitTests {
        private Term varx = parse("X12");
        private Term consta = parse("a");
        private Term comp = parse("f(X,a)");
        
        
        /** parsing **/
        
        public void parseVar() {
            assert varx.isVariable();
        }
        
        public void goodVar() {
            assert ((Variable) varx).getName().equals("X12");
        }
        
        public void parseConstant() {
            assert consta.isConstant();
        }
        
        public void goodConstant() {
            assert ((Constant) consta).getName().equals("a");
        }
        
        public void parseCompound() {
            assert comp.isCompound();
        }
        
        public void goodCompound() {
            Compound c = (Compound) comp;
            assert c.getLabel().equals("f");
            assert c.getSubterms().size() == 2;
            assert c.equals(new Compound("f", new Term[] {
                    new Variable("X"), new Constant("a")
            }));
        }
        
        /** toString **/
        public void tostring() {
            assert varx.toString().equals("X12");
            assert consta.toString().equals("a");
            assert comp.toString().equals("f(X,a)");
        }
        
        /** equals and hashcode **/
        
        public void equals() {
            assert varx.equals(new Variable("X12"));
            assert consta.equals(new Constant("a"));
            assert comp.equals(new Compound("f", new Term[] { new Variable("X"), new Constant("a") }));
        }
        
        public void notEquals() {
            assert !varx.equals(new Variable("X13"));
            assert !consta.equals(new Constant("b"));
            assert !comp.equals(new Compound("g", new Term[] { new Variable("X"), new Constant("a") }));
            assert !comp.equals(new Compound("f", new Term[] { new Variable("X") }));
            assert !comp.equals(new Compound("f", new Term[] { new Variable("X"), new Constant("b") }));
        }

        public void hashcode() {
            assert varx.hashCode() == (new Variable("X12")).hashCode();
            assert consta.hashCode() == (new Constant("a")).hashCode();
            assert comp.hashCode() ==
                (new Compound("f", new Term[] { new Variable("X"), new Constant("a") })).hashCode();
        }
            
        
        
        /** unification **/
        
        public void unify1() {
            Term a = Term.parse("f(X,g(X),Y)");
            Term b = Term.parse("f(h(k),Y,Z)");
            
            assert a.unify(b).equals(Term.parse("f(h(k),g(h(k)),g(h(k)))"));
        }
        
        public void nonunify1() {
            Term a = Term.parse("f(X,X)");
            Term b = Term.parse("f(a,b)");
            
            assert (a.getUnifier(b) == null) : "unifier is " + a.getUnifier(b);
        }
        
        public void nonunify2() {
            Term a = Term.parse("f(X,Y,b)");
            Term b = Term.parse("f(a,X,Y)");
            
            assert (a.getUnifier(b) == null) : "unifier is " + a.getUnifier(b);
        }
        
        public void nonunify3() {
            // a bit deeper
            Term a = Term.parse("f(X,g(a,X),g(a,d))");
            Term b = Term.parse("f(h(b,c),Z,Z)");
            
            assert (a.getUnifier(b) == null) : "unifier is " + a.getUnifier(b);
        }
    }
}
