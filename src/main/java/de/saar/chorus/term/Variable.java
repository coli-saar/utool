/*
 * @(#)Variable.java created 11.05.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.term;

import java.util.HashSet;
import java.util.Set;

public class Variable extends Term {
    private String name;

    public Variable(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
    

    public String toString() {
        return name;
    }

    public Type getType() {
        return Type.VARIABLE;
    }
    public boolean equals(Object obj) {
        if( obj instanceof Variable ) {
            Variable vo = (Variable) obj;
            return name.equals(vo.name);
        } else {
            return false;
        }
    }

    public boolean hasSubterm(Term other) {
        return equals(other);
    }

    public Substitution getUnifier(Term other) {
        if( !other.hasSubterm(this)) {
            return new Substitution(this, other);
        } else {
            return null;
        }
    }

    public Set<Variable> getVariables() {
        Set<Variable> ret = new HashSet<Variable>();
        ret.add(this);
        return ret;
    }

	@Override
	public String toLispString() {
		return name;
	}
    

    
}
