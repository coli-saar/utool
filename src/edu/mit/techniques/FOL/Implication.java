package edu.mit.techniques.FOL;

import java.util.Vector;

public class Implication extends Connective {

	public Implication(Sentence s1, Sentence s2){
		super(s1, s2);
	}

	public Sentence eliminateEquivalences (){
		return new Implication(s1.eliminateEquivalences(),
		                       s2.eliminateEquivalences());
	}

	public Sentence eliminateImplications (){
		return new Disjunction(new Negation(s1.eliminateImplications()),
		                       s2.eliminateImplications());
	}

	public Sentence driveInNegations(){
		throw new RuntimeException("It's silly to drive in negations if you haven't already"
									+ " eliminated implications.");
	}

	public Sentence simplify(){
		throw new RuntimeException("It's silly to simplify if you haven't "
								   + "already eliminated implications.");
	}

	public Sentence negate(){
		throw new RuntimeException("It's silly to drive in negations if you haven't already"
									+ " eliminated implications.");
	}

	public void extractQuantifications(Vector quantifications){
		throw new RuntimeException("It's not sensible to extract quantifications if you "
									+ "haven't already eliminated implications.");
	}

	public Sentence removeQuantifiers(Vector quantifications){
		throw new RuntimeException("It's not sensible to remove quantifiers if you "
									+ "haven't already eliminated implications.");
	}

	public ClauseList makeClauses(){
		throw new RuntimeException("Can't make clauses if we still have implications");
	}

  public Sentence substitute(Substitution s) {
    throw new RuntimeException("Should not be substituting variables in implications");
  }

	public Sentence substituteVariable(Variable oldVar, Term newVar){
		return new Implication(s1.substituteVariable(oldVar, newVar),
							   s2.substituteVariable(oldVar, newVar));
	}

	public Sentence renameVariables(){
		return new Implication(s1.renameVariables(),
							   s2.renameVariables());
	}


	public String toString(){
		return "(" + s1 + " -> " + s2 + ")";
	}

    public String toMace() {
	return "(" + s1.toMace() + " -> " + s2.toMace() + ")";
    }

    public String toDFG() {
	return "implies(" + s1.toDFG() + "," + s2.toDFG() + ")";
    }

}
