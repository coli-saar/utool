package edu.mit.techniques.FOL;

import java.util.Vector;

public class Existential extends Quantifier {

	public Existential (Variable v, Sentence s){
		super(v,s);
	}

	public Sentence eliminateEquivalences(){
		return new Existential(v, s.eliminateEquivalences());
	}

	public Sentence eliminateImplications(){
		return new Existential(v, s.eliminateImplications());
	}

	// Return negation of this sentence by changing to a universal and
	// negating body
	public Sentence negate(){
		return new Universal(v, s.negate());
	}

	// Return an equivalent sentence with negations driven in
	public Sentence driveInNegations(){
		return new Existential(v, s.driveInNegations());
	}

  public Sentence substitute(Substitution s) {
    throw new RuntimeException("Should not substituting variables in existentials");
  } // end of METHOD substitute()

  // Substitute old for new in body
  public Sentence substituteVariable(Variable oldVar, Term newVar){
    return new Existential(v, s.substituteVariable(oldVar, newVar));
  } // end of METHOD substituteVariable()


	// Make a new variable.  Then, rename any variables quantified in the body.
	// Finally, substitute new for old throughout the body.
	// Have to do this in the right order to get scoping right.
	public Sentence renameVariables (){
		Variable newVar = v.rename();
		return new Existential(newVar, s.renameVariables().substituteVariable(v, newVar));
	}

	// Here's a quantifier!
	public void extractQuantifications (Vector quantifications){
		quantifications.addElement(new Quantification(Quantification.EXISTENTIAL, v));
		s.extractQuantifications(quantifications);
	}

	public Sentence removeQuantifiers (Vector quantifications){
		return s.removeQuantifiers(quantifications);
	}

	public ClauseList makeClauses(){
		throw new RuntimeException("Can't make clauses if we still have quantifiers");
	}

	public String toString(){
		return "E" + v + "." + s + "";
	}


    public String toMace() {
	return "(exists " + v + " " + s.toMace() + ")";
    }

    public String toDFG() {
	return "exists([" + v + "]," + s.toDFG() + ")";
    }

}
