package edu.mit.techniques.FOL;

import java.util.Vector;

public class Conjunction extends Connective {

	public Conjunction(Sentence s1, Sentence s2){
		super(s1, s2);
	}

	public Sentence eliminateEquivalences (){
		return new Conjunction (s1.eliminateEquivalences(),
						        s2.eliminateEquivalences());
	}

	public Sentence eliminateImplications (){
		return new Conjunction (s1.eliminateImplications(),
								s2.eliminateImplications());
	}

	// Return an equivalent sentence
	public Sentence driveInNegations(){
		return new Conjunction (s1.driveInNegations(),
								s2.driveInNegations());
	}

	public Sentence simplify(){
		s1 = s1.simplify();
		s2 = s2.simplify();

		if(s1 instanceof FalseProposition || s2 instanceof FalseProposition)
			return Sentence.FALSE;
		if(s1 instanceof TrueProposition)
			return s2;
		if(s2 instanceof TrueProposition)
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
   
	//  Return the negation of this sentence by applying deMorgan's law
	public Sentence negate (){
		return new Disjunction (s1.negate(),
								s2.negate());
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
		return new Conjunction (s1.substitute(s), s2.substitute(s));
	} // end of METHHOD substitute()

	// Substitute variable new for variable old throughout
	public Sentence substituteVariable (Variable oldVar, Term newVar){
		return new Conjunction (s1.substituteVariable(oldVar, newVar),
							    s2.substituteVariable(oldVar, newVar));
	}

	// Rename any variables that are quantified within the components
	// of the conjunction
	public Sentence renameVariables (){
		return new Conjunction (s1.renameVariables(),
								s2.renameVariables());
	}

	// Return the sentence minus the quantifiers
	public Sentence removeQuantifiers (Vector quantifications){
		return new Conjunction (s1.removeQuantifiers(quantifications),
								s2.removeQuantifiers(quantifications));
	}

	// Return a list of clauses
	// Conjunction is easy:  just concatenate the clause lists of the
	// two conjuncts
	public ClauseList makeClauses (){
	    ClauseList c1 = s1.makeClauses();
	    ClauseList c2 = s2.makeClauses();

	    // If either one is false, return false
	    if (c1.isFalse()){
		return c1;
	    }
	    if (c2.isFalse()){
		return c2;
	    }
	    // If one is true, return the other
	    if (c1.isTrue()){
		return c2;
	    }
	    if (c2.isTrue()){
		return c1;
	    }
	    
	    // Otherwise
	    c1.nconc(c2);
	    return c1;
	}

	public String toString(){
		return "(" + s1 + " & " + s2 + ")";
	}


    public String toMace() {
	return "(" + s1.toMace() + " & " + s2.toMace() + ")";
    }

    public String toDFG() {
	return "and(" + s1.toDFG() + "," + s2.toDFG() + ")";
    }


}
