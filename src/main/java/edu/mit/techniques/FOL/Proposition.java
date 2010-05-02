package edu.mit.techniques.FOL;

import java.util.Vector;

abstract public class Proposition extends Sentence {

	public Sentence eliminateEquivalences(){
		return this;
	}

	public Sentence eliminateImplications(){
		return this;
	}

	public Sentence driveInNegations(){
		return this;
	}

	public Sentence simplify(){
		return this;
	}

	// We can't negate this internally, so just make a negation
	public Sentence negate(){
		return new Negation(this);
	}

	public abstract Sentence substituteVariable (Variable oldVar, Term newVar);

	// This only applies at the level of quantifiers.  No new vars to rename
	// inside a proposition.
	public Sentence renameVariables(){
		return this;
	}

	// No quantifiers to extract here
	public void extractQuantifications (Vector quantifications){}

	// Get all the Variables in this proposition.  If there are none, return
	// an empty Vector.
	public abstract Vector obtainVariables();

	// Take this opportunity to turn free vars into constants
	public abstract Sentence removeQuantifiers (Vector quantifications);

	// Try to unify <this> with <p>
	public abstract Substitution unify(Proposition p);

    public abstract Symbol getRelationSymbol();
    public abstract TermList getTerms();
    public abstract int getArity();


    public abstract ClauseList makeClauses();
    
    public boolean equals (Proposition p){
	return this.toString().equals(p.toString());
    }

    public String toMace() {
	return toString();
    }

    public String toDFG() {
	return toString();
    }


    public Sentence[] subformulas() {
	return new Sentence[0];
    }

    public void setSubformula(int i, Sentence s) {
	// this doesn't make sense for an atomic proposition
    }

    public Variable[] freeVariables() {
	Vector vars = obtainVariables();

	/*
	for( Enumeration e = vars.elements(); e.hasMoreElements(); ) {
	    Object x = e.nextElement();

	    System.err.println("fv: " + x + " (" + x.getClass().getName() + ")");
	}

	return new Variable[0];
	*/
	return (Variable[]) vars.toArray(new Variable[0]);
    }


}
