package edu.mit.techniques.FOL;

import java.util.Vector;

public final class TrueProposition extends Proposition {

	/**
	 * from Sentence abstract class
	 */
	public Sentence substitute(Substitution s) {
		return this;
	} // end of METHOD substitute()

	public Sentence substituteVariable (Variable oldVar, Term newVar){
		return this;
	}

	public Sentence removeQuantifiers (Vector quantifications){
		return this;
	}

	public String toString(){
		return "T";
	}

    public Symbol getRelationSymbol() {
	return null;
    }

    public TermList getTerms() {
	return null; // empty list
    }

    public int getArity() {
	return 0;
    }

    // Return an empty list of clauses
    public ClauseList makeClauses() {
	return new ClauseList();
    }

	public Substitution unify(Proposition p) {
		/*    if (p instanceof TrueProposition)
			  return Substitution.getEmpty();
		*/
		return null;
	} // end of METHOD unify()

	/**
	 * from Proposition abstract class.
	 *
	 * No variables are in an TrueProposition.  Therefore, return empty Vector.
	 */
	public Vector obtainVariables() {
		return new Vector();
	} // end of METHOD obtainVariables


    public String toDFG() {
	return "true";
    }

    public String toMace() {
	return "(helpfulatom | -helpfulatom)";
    }


} // end of METHOD TrueProposition
