package edu.mit.techniques.FOL;

import java.util.Vector;

public class Negation extends Sentence {

	Sentence s1;

	public Negation (Sentence s){
		s1 = s;
	}

	public Object clone(){
		Negation s = (Negation) super.clone();
		s.s1 = (Sentence)s1.clone();
		return s;
	}

	public Sentence eliminateEquivalences(){
		return new Negation (s1.eliminateEquivalences());
	}

	public Sentence eliminateImplications(){
		return new Negation (s1.eliminateImplications());
	}

	// Return a sentence that's equivalent to this one
	public Sentence driveInNegations(){
		return s1.negate();
	}

	public Sentence simplify(){
		s1 = s1.simplify();
		if(s1 instanceof TrueProposition)
			return Sentence.FALSE;
		if(s1 instanceof FalseProposition)
			return Sentence.TRUE;
		return this;
	}

	// Return a sentence that's the negative of this one.
	public Sentence negate(){
		return s1.driveInNegations();
	}

	public Sentence substituteVariable(Variable oldVar, Term newVar){
		return new Negation (s1.substituteVariable(oldVar, newVar));
	}

	public Sentence substitute(Substitution s) {
		return new Negation(s1.substitute(s));
	}

	public Sentence renameVariables(){
		return new Negation (s1.renameVariables());
	}

	public void extractQuantifications (Vector quantifications){
		s1.extractQuantifications(quantifications);
	}

	public Sentence removeQuantifiers (Vector quantifications){
		return new Negation(s1.removeQuantifiers(quantifications));
	}

	// If this s is not a proposition, we haven't done the previous
	// steps properly
	public ClauseList makeClauses(){
	    if (s1 instanceof FalseProposition){
		// Return false
		return new ClauseList(new Clause());
	    } else if (s1 instanceof TrueProposition){
		// Return true
		return new ClauseList();
	    } else {
		return new ClauseList(new NegativeLiteral((Proposition) s1));
	    }
	}

	public String toString(){
		return "~" + s1;
	}

    public String toMace() {
	return "-" + s1.toMace();
    }

    public String toDFG() {
	return "not(" + s1.toDFG() + ")";
    }


    public Sentence[] subformulas() {
	Sentence[] ret = {s1};
	return ret;
    }

    public void setSubformula(int i, Sentence s) {
	s1 = s;
    }


}
