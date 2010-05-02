package edu.mit.techniques.FOL;

import java.util.Vector;

public class Universal extends Quantifier {

	public Universal (Variable v, Sentence s){
		super(v,s);
	}

	public Sentence eliminateEquivalences(){
		return new Universal(v, s.eliminateEquivalences());
	}

	public Sentence eliminateImplications(){
		return new Universal(v, s.eliminateImplications());
	}

	// Return an equivalent sentence
	public Sentence driveInNegations(){
		return new Universal(v, s.driveInNegations());
	}

	// Return the negation, by changing universal to existential and
	// negating the body
	public Sentence negate(){
		return new Existential(v, s.negate());
	}

	public Sentence substitute(Substitution s) {
		throw new RuntimeException("Should not be substitution variables in universal statements");
	} // end of METHOD substitute()

	// Just substitute new for old in the body
	public Sentence substituteVariable(Variable oldVar, Term newVar){
		return new Universal(v, s.substituteVariable(oldVar, newVar));
	}


	public Sentence renameVariables (){
		Variable newVar = v.rename();
		return new Universal(newVar, s.renameVariables().substituteVariable(v, newVar));
	}

	// Here's a quantifier!
	public void extractQuantifications (Vector quantifications){
		quantifications.addElement(new Quantification(Quantification.UNIVERSAL, v));
		s.extractQuantifications(quantifications);
	}

	public Sentence removeQuantifiers (Vector quantifications){
		return s.removeQuantifiers(quantifications);
	}

	public ClauseList makeClauses(){
		throw new RuntimeException("Can't make clauses if we still have quantifiers");
	}


	public String toString(){
		return "A" + v + "." + s + "";
	}

    public String toMace() {
	return "(all " + v + " " + s.toMace() + ")";
    }

    public String toDFG() {
	return "forall([" + v + "]," + s.toDFG() + ")";
    }

}
