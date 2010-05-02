package edu.mit.techniques.FOL;

import java.util.Vector;

public class Equivalence extends Connective {

	public Equivalence(Sentence s1, Sentence s2){
		super(s1, s2);
	}

	public Sentence eliminateEquivalences(){
		return new Conjunction(new Implication(s1.eliminateEquivalences(),
											   s2.eliminateEquivalences()),
		                       new Implication(s2.eliminateEquivalences(),
		                                       s1.eliminateEquivalences()));
	}

	public Sentence eliminateImplications(){
		throw new RuntimeException("It's silly to eliminate implications if you haven't already"
									+ " eliminated equivalences.");
	}

	public Sentence driveInNegations(){
		throw new RuntimeException("It's silly to drive in negations if you haven't already"
									+ " eliminated equivalences.");
	}

	public Sentence simplify(){
		throw new RuntimeException("It's silly to simplify if you haven't "
								   + "already eliminated equivalences.");
	}

	public Sentence negate(){
		throw new RuntimeException("It's silly to drive in negations if you haven't already"
									+ " eliminated equivalences.");
	}

	public void extractQuantifications(Vector quantifications){
		throw new RuntimeException("It's not sensible to extract quantifications if you "
									+ "haven't already eliminated equivalences.");
	}

	public Sentence removeQuantifiers(Vector quantifications){
		throw new RuntimeException("It's not sensible to remove quantifiers if you "
									+ "haven't already eliminated equivalences.");
	}

	public ClauseList makeClauses(){
		throw new RuntimeException("Can't make clauses if we still have equivalences");
	}

  /**
   * from Sentence abstract class
   */
  public Sentence substitute(Substitution s) {
    throw new RuntimeException("Should not be substituting variables in equivalences");
  } // end of METHOD substitute()

	public Sentence substituteVariable(Variable oldVar, Term newVar){
		return new Equivalence(s1.substituteVariable(oldVar, newVar),
							   s2.substituteVariable(oldVar, newVar));
	}

	public Sentence renameVariables(){
		return new Equivalence(s1.renameVariables(),
							   s2.renameVariables());
	}

	public String toString(){
		return "(" + s1 + " <-> " + s2 + ")";
	}

    public String toDFG() {
	return "equiv(" + s1.toDFG() + "," + s2.toDFG() + ")";
    }

    public String toMace() {
	return "(" + s1.toMace() + " <-> " + s2.toMace() + ")";
    }
	

}
