package edu.mit.techniques.FOL;

import java.util.Vector;

public final class FalseProposition extends Proposition {

	public String toString(){
		return "F";
	}

	/**
	 * from Sentence abstract class
	 */
	public Sentence substitute(Substitution s) {
		return this;
	}

	public Sentence substituteVariable(Variable oldVar, Term newVar){
		return this;
	}

	public Sentence removeQuantifiers (Vector quantifications){
		return this;
	}

	public Substitution unify(Proposition p) {
		/*    if (p instanceof FalseProposition)
			  return Substitution.getEmpty();
		*/
		return null;
	} // end of METHOD unify()

    public Symbol getRelationSymbol() {
	return null;
    }

    public TermList getTerms() {
	return null; // empty list
    }

    public int getArity() {
	return 0;
    }

    // Return a list of clauses containing the empty clause (which is false)
    public ClauseList makeClauses() {
	return new ClauseList(new Clause());
    }

	/**
	 * from Proposition abstract class.
	 *
	 * No variables are in an FalseProposition.  Therefore, return
	 * empty Vector.  */
	public Vector obtainVariables() {
		return new Vector();
	} // end of METHOD obtainVariables


    public String toDFG() {
	return "false";
    }

    public String toMace() {
	return "(helpfulatom & -helpfulatom)";
    }


} // end of METHOD FalseProposition
