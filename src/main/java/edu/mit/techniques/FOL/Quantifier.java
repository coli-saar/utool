package edu.mit.techniques.FOL;

import java.util.Vector;


abstract public class Quantifier extends Sentence {

	Sentence s;
	Variable v;

	public Quantifier(Variable v, Sentence s){
		this.v = v;
		this.s = s;
	}

	public Object clone(){
		Quantifier q = (Quantifier) super.clone();
		q.s = (Sentence)s.clone();
		q.v = (Variable)v.clone();
		return q;
	}

	public Sentence simplify(){
		throw new RuntimeException("simplify() should not be called on"
								   + " Sentences containing Quantifiers.");
	}


    public Variable[] freeVariables() {
	Variable[] subvars = s.freeVariables();
	String varname = v.getName();
	Vector myvars = new Vector(subvars.length); 
	// need to be this complicated to allow spurious quantification

	for( int i = 0; i < subvars.length; i++ )
	    if( !subvars[i].getName().equals(varname) )
		myvars.add(subvars[i]);

	return (Variable[]) myvars.toArray(new Variable[0]);
    }


    public Variable variable() {
	return v;
    }

    public Sentence[] subformulas() {
	Sentence[] ret = {s};
	return ret;
    }

    public void setSubformula(int i, Sentence s) {
	this.s = s;
    }
}
