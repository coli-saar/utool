package edu.mit.techniques.FOL;

import java.util.Vector;

public class Function extends Term {

	/////
	// VARIABLES
	private Symbol _functionSymbol;
	private TermList _terms;

	/////
	// CONSTRUCTOR
	public Function (Symbol functionSymbol, TermList terms) {
		_functionSymbol = functionSymbol;
		_terms = terms;
	}

	public Object clone(){
		Function f = (Function) super.clone();
		f._functionSymbol = (Symbol) _functionSymbol.clone();
		f._terms = (TermList)  _terms.clone();
		return f;
	}

	/////
	// ACCESSOR METHODS
	public Symbol getFunctionSymbol() {
		return _functionSymbol;
	} // end of METHOD getFunctionSymbol()

	public TermList getTerms() {
		return _terms;
	} // end of METHOD getTerms()

	/////
	// OTHER METHODS
	/**
	 * Implementation abstract method from Term.
	 */
	public Substitution unify(Term t, Substitution s) {
		Sentence.debugPrint("Function: ");
		// if <t> is a Variable, call Variable's unify()
		if (t instanceof Variable)
			return ((Variable)t).unify(this, s);
		// first check to make sure <t> is a function
		else if (t instanceof Function) {
			// check the first terms
			Function tf = (Function)t;
			if (!_functionSymbol.equals(tf._functionSymbol)) {
				Sentence.debugPrintln("NULL: functionSymbol not equal");
				return null;
			}

			// term list of <t>
			TermList ttl = ((Function)t).getTerms();
			// term list of <this>
			TermList thistl = _terms;

			// check to make sure they have the same number of terms
			// if not, they cannot unify
			if (ttl.length() != thistl.length())
				return null;

			// iterate through all terms
			while (ttl != null) {
				s = thistl.getFirst().unify(ttl.getFirst(), s);

				if (s == null) {
					Sentence.debugPrintln("NULL: can't unify: " + ttl.getFirst().toString() + ", " + thistl.getFirst().toString());
					return null;
				}

				// go to the next
				ttl = ttl.getRest();
				thistl = thistl.getRest();
			}

			return s;
		}
		else
			return null;
	} // end of METHOD unify()


	public Term substituteVariable(Variable oldVar, Term newVar){
		return new Function (_functionSymbol,
							 _terms.substituteVariable(oldVar, newVar));
	} // end of METHOD substituteVariable()

	/**
	 * from Term abstract class
	 *
	 * only the terms of the function are of concern.  Returns a new Function
	 * object with appropriate terms replaced
	 */
	public Term substitute(Substitution s) {
		return new Function (_functionSymbol,
							 _terms.substitute(s));
	} // end of METHOD substitute()

	/**
	 * from Term abstract class
	 *
	 * retrieves all the Variables and returns them in a Vector.
	 */
	public Vector obtainVariables() {
		return _terms.obtainVariables();
	} // end of METHOD obtainVariables()

	// Convert free vars to contants
	public Term removeQuantifiers (Vector quantifications){
		return new Function (_functionSymbol,
							 _terms.removeQuantifiers(quantifications));
	}

	public String toString(){
		return _functionSymbol + "(" + ((_terms==null)?"":_terms.toString())
			+ ")";
	}

}
