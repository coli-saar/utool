package edu.mit.techniques.FOL;

import java.util.Vector;

public class CompoundProposition extends Proposition {

	////
	// VARIABLES
	private Symbol _relationSymbol;
	private TermList _terms;


	/////
	// CONSTRUCTORS
	public CompoundProposition (Symbol relationSymbol, TermList terms) {
		_relationSymbol = relationSymbol;
		_terms = terms;
	}

	public Object clone(){
		CompoundProposition p = (CompoundProposition) super.clone();
		p._relationSymbol = (Symbol) _relationSymbol.clone();
		p._terms = (TermList) ((TermList) _terms).clone();
		return p;
	}

	/////
	// ACCESSOR METHODS

	public Symbol getRelationSymbol() {
		return _relationSymbol;
	} // end of METHOD getRelationSymbol()

	public TermList getTerms() {
		return _terms;
	}

    public int getArity() {
	return getTerms().length();
    }

	/////
	// OTHER METHODS

	/**
	 * implemented from Sentence abstract class
	 *
	 */
	public Sentence substitute(Substitution s) {
		return new CompoundProposition(_relationSymbol,
									   _terms.substitute(s));
	} // end of METHOD substitute()

	/**
	 * implemented from interface Proposition
	 */
	public Sentence substituteVariable(Variable oldVar, Term newVar){
		return new CompoundProposition(_relationSymbol,
									   _terms.substituteVariable(oldVar, newVar));
	} // end of METHOD substituteVariable()

	/**
	 * implemented from Proposition interface
	 *
	 * convert free variables to constants
	 */
	public Sentence removeQuantifiers(Vector quantifications){
		return new CompoundProposition (_relationSymbol,
										_terms.removeQuantifiers(quantifications));
	} // end of METHOD removeQuantifiers()

	/**
	 * implemented from Proposition interface
	 *
	 * if <p> is a CompoundProposition, the propositional symbols are equal, and
	 * the terms are unifiable, then return the substitution, else return null.
	 */
	public Substitution unify(Proposition p) {
		//
		debugPrint("CompoundProposition: ");
		if (p instanceof CompoundProposition &&
			((CompoundProposition)p)._relationSymbol.equals(_relationSymbol) &&
			((CompoundProposition)p)._terms.length() == _terms.length()) {
			debugPrintln("relSymb: " + _relationSymbol + " length: " + _terms.length());
			Substitution s = _terms.unify(((CompoundProposition)p)._terms, Substitution.getEmpty());
			return s;
		}
		// failure
		debugPrintln("NULL");
		return null;
	} // end of METHOD unify()

    public ClauseList makeClauses() {
	return new ClauseList(new PositiveLiteral(this));
    }


	/**
	 * from Proposition abstract class.
	 *
	 * get the variables in this proposition.
	 */
	public Vector obtainVariables() {
		return _terms.obtainVariables();
	} // end of METHOD obtainVariables

	public String toString() {
		return _relationSymbol + "(" +
			((_terms == null) ? "" : _terms.toString()) + ")";
	} // end of METHOD toString()


} // end of CLASS CompoundProposition
