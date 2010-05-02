package edu.mit.techniques.FOL;

import java.util.Vector;

public class Disjunction extends Connective {

	public Disjunction(Sentence s1, Sentence s2){
		super(s1, s2);
	}

	public Sentence eliminateEquivalences (){
		return new Disjunction(s1.eliminateEquivalences(),
							   s2.eliminateEquivalences());
	}

	public Sentence eliminateImplications (){
		return new Disjunction(s1.eliminateImplications(),
							   s2.eliminateImplications());
	}

	public Sentence simplify(){
		s1 = s1.simplify();
		s2 = s2.simplify();

		if(s1 instanceof TrueProposition || s2 instanceof TrueProposition)
			return Sentence.TRUE;
		if(s1 instanceof FalseProposition)
			return s2;
		if(s2 instanceof FalseProposition)
			return s1;

		String s1str = s1.toString();
		String s2str = s2.toString();
		int compare = s1str.compareTo(s2str);		
		if(compare == 0)
			return s1;
		else if (compare > 0){
			Sentence temp = s1;
			s1 = s2;
			s2 = temp;
		}

		return this;
	}

	public Sentence negate (){
		return new Conjunction(s1.negate(),
							   s2.negate());
	}

	// Return an equivalent sentence with negations driven in
	public Sentence driveInNegations(){
		return new Disjunction(s1.driveInNegations(),
							   s2.driveInNegations());
	}

	// Extract the quantifications and add them to the vector
	public void extractQuantifications (Vector quantifications){
		s1.extractQuantifications(quantifications);
		s2.extractQuantifications(quantifications);
	}

	/**
	 * from Sentence abstract class
	 */
	public Sentence substitute(Substitution s) {
		return new Disjunction(s1.substitute(s), s2.substitute(s));
	} // end of METHOD substitute

	public Sentence substituteVariable (Variable oldVar, Term newVar){
		return new Disjunction(s1.substituteVariable(oldVar, newVar),
							   s2.substituteVariable(oldVar, newVar));
	}

	public Sentence renameVariables(){
		return new Disjunction(s1.renameVariables(),
							   s2.renameVariables());
	}

	public Sentence removeQuantifiers (Vector quantifications){
		return new Disjunction(s1.removeQuantifiers(quantifications),
							   s2.removeQuantifiers(quantifications));
	}

	// This one's a bit tricky.  If s1 converts to (a v b) ^ (c v d) and
	// s2 converts to (e v f) ^ (g v h) then we need to generate
	// (a v b v e v f) ^ (a v b v g v h) ^ (c v d v e v f) ^ (c v d v g v h)

	public ClauseList makeClauses(){
		ClauseList c1 = s1.makeClauses();
		ClauseList c2 = s2.makeClauses();

	    // If either one is true, return true
		if (c1.isTrue()) {
		    return c1;
		}
		if (c2.isTrue()) {
		    return c2;
		}

	    // If one is false, return the other
	    if (c1.isFalse()){
		return c2;
	    }
	    if (c2.isFalse()){
		return c1;
	    }
	    
		ClauseList result = new ClauseList();

		for (int i = 0; i < c1.size(); i++){
			for (int j = 0; j < c2.size(); j++){
				Clause ci = c1.clauseAt(i);
				Clause cj = c2.clauseAt(j);
				Clause cat = ci.concatenate(cj);
				result.addClause(cat);
			}
		}

		return result;
	}

	public String toString(){
		return "(" + s1 + " v " + s2 + ")";
	}


    public String toMace() {
	return "(" + s1.toMace() + " | " + s2.toMace() + ")";
    }

    public String toDFG() {
	return "or(" + s1.toDFG() + "," + s2.toDFG() + ")";
    }

}
