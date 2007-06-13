/*
 * @(#)Compound.java created 11.05.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.term;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.saar.basic.StringTools;

public class Compound extends Term {
    private String label;
    private List<Term> subterms;
    
    
    public Compound(String label, List<Term> subterms) {
        this.label = label;
        this.subterms = subterms;
    }
    
    public Compound(String label, Term[] subterms) {
        this(label, Arrays.asList(subterms));
    }


    public String getLabel() {
        return label;
    }


    public List<Term> getSubterms() {
        return subterms;
    }

    
    public String toString() {
        return label + "(" + StringTools.join(subterms, ",") + ")";
    }


    public Type getType() {
        return Type.COMPOUND;
    }
    
    private boolean isLocallyEqual(Compound other) {
        if( !label.equals(other.label)) {
            return false;
        }
        
        if( subterms.size() != other.subterms.size() ) {
            return false;
        }
        
        return true;
    }

    public boolean equals(Object o) {
        if (o instanceof Compound) {
            Compound co = (Compound) o;
            
            if( !isLocallyEqual(co)) {
                return false;
            }
            
            for( int i = 0; i < subterms.size(); i++ ) {
                if( !subterms.get(i).equals(co.subterms.get(i))) {
                    return false;
                }
            }
            
            return true;
        } else {
            return false;
        }
    }
    
    
    @Override
	public int hashCode() {
    	return toLispString().hashCode();
	}

	public boolean hasSubterm(Term other) {
        if( equals(other)) {
            return true;
        } else {
            for( Term subterm : subterms ) {
                if( subterm.hasSubterm(other)) {
                    return true;
                }
            }
            
            return false;
        }
    }

    public Substitution getUnifier(Term other) {
        switch(other.getType()) {
        case VARIABLE:
            return new Substitution((Variable) other, this);
            
        case CONSTANT:
            return null;
            
        case COMPOUND:
            Compound com = (Compound) other;
            Substitution subst = new Substitution();
            
            if( !isLocallyEqual(com) ) {
                return null;
            }
            
            for( int i = 0; i < subterms.size(); i++ ) {
                Substitution here = subterms.get(i).getUnifier(com.subterms.get(i));
                
                if( here == null ) {
                    return null;
                }
                
                subst = subst.concatenate(here);
                if( !subst.isValid() ) {
                    return null;
                }
            }
            
            return subst;
        }
        
        // unreachable
        return null;
    }

    public Set<Variable> getVariables() {
        Set<Variable> ret = new HashSet<Variable>();
        
        for( Term subterm : subterms ) {
            ret.addAll(subterm.getVariables());
        }
        
        return ret;
    }

	@Override
	public String toLispString() {
		StringBuffer buf = new StringBuffer("(" + label);
		
		for( Term subterm : subterms ) {
			buf.append(" " + subterm.toLispString());
		}
		
		buf.append(")");
		
		return buf.toString();
	}
    
}
