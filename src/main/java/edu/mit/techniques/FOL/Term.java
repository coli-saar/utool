package edu.mit.techniques.FOL;

import java.util.Vector;

abstract public class Term implements Cloneable {

	public Object clone(){
		try{
			return super.clone();
		}
		catch(CloneNotSupportedException e){ //won't happen
			return null;
		}
	}

	abstract public Term substituteVariable (Variable oldVar, Term newVar);

	// Convert free vars to constants
	abstract public Term removeQuantifiers (Vector quantifications);

	/**
	 * Given two terms, return their most general unifier, or null if
	 * they don't unify.  <s> may be mutated.  */
	abstract public Substitution unify(Term t, Substitution s);

	/**
	 * returns a Vector of Terms that are Variables.  If this Term
	 * isn't a Variable, an empty Vector is returned.  */
	abstract public Vector obtainVariables();

	abstract public Term substitute (Substitution s);


    public boolean isTermType(String superclass) {
	try {
	    return Class.forName("edu.mit.techniques.FOL." + superclass).isAssignableFrom(getClass());
	} catch(Exception e) {
	    return false;
	}
    }
} // end of ABSTRACT CLASS Term
