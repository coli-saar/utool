package edu.mit.techniques.FOL;

import java.util.Vector;

public class TermList implements Cloneable {

	/////
	// VARIABLES

	private Term _first;
	private TermList _theRest;

	/////
	// CONSTRUCTORS

	public TermList (Term first, TermList _rest){
		_first = first;
		_theRest = _rest;
	}

	public Object clone(){
	   	try{
			TermList t = (TermList) super.clone();
			t._first = (Term) _first.clone();
			if(_theRest != null)
				t._theRest = (TermList) _theRest.clone();
			return t;
		}
		catch(CloneNotSupportedException e){ // won't happen
			return null;
		}
	}

	/////
	// ACCESSOR METHODS

	public Term getFirst() {
		return _first;
	} // end of METHOD getFirst()

	public TermList getRest() {
		return _theRest;
	} // end of METHOD getRest()

	/////
	// REPORTER METHODS

	public int length() {
		int count = 0;
		TermList current = _theRest;

		if (_first != null)
			count = 1;
		else
			return count;

		while (current != null) {
			count++;
			current = current.getRest();
		}

		return count;
	} // end of METHOD length()

	/**
	 * see Proposition.obtainVariables()
	 *
	 * get the variables in this list of terms
	 */
	public Vector obtainVariables() {
		Vector result;
		int resultSize;
		// the variables inside _first
		Vector firstResult = _first.obtainVariables();

		if (_theRest == null)
			return firstResult;

		result = _theRest.obtainVariables();

		if (result.size() == 0)
			return firstResult;

		// we do it this way because otherwise result.size() would change in the
		// course of the for loops
		resultSize = result.size();
		// check to make sure the Variable wasn't already there before
		for (int i = 0; i < resultSize; i++) {
			boolean found = false;
			Term t = (Term)result.elementAt(i);
			Term frt = null;

			for (int j = 0; j < firstResult.size(); j++) {
				frt = (Term)firstResult.elementAt(j);
				if (frt.equals(t)) {
					found = true;
					break;
				}
			}

			if (!found && frt != null)
				result.addElement(frt);
		}

		return result;
	} // end of METHOD obtainVariables


	/////
	// OTHER METHODS

	/**
	 * Assumed that this.length() == tl.length()
	 */
	public Substitution unify(TermList tl, Substitution s) {
		Sentence.debugPrint("TermList: ");
		// defensive programming: check length
		if (length() != tl.length()) {
			Sentence.debugPrintln("lengths diff: NULL");
			return null;
		}

		// try to unify first terms.  if they fail, return null
		if (_first == null || tl._first == null) {
			Sentence.debugPrintln("_first is null: NULL");
			return null;  // egregious error
		}

		Substitution newS = _first.unify(tl._first, s);
		if (newS == null) {
			Sentence.debugPrintln("unifying with first term unsuccessful: NULL");
			return null;
		}

		// if the rest if null, return s
		if (_theRest == null) {
			Sentence.debugPrintln("UNIFIED: terms unify: " + newS.toString());
			return newS;
		}
		else
			// else do rest of the terms
			return _theRest.unify(tl._theRest, newS);
	} // end of METHOD unify()

	public TermList substituteVariable (Variable oldVar, Term newVar){
		if (_theRest == null){
			return new TermList (_first.substituteVariable (oldVar, newVar), null);
		}
		else {
			return new TermList (_first.substituteVariable (oldVar, newVar),
								 _theRest.substituteVariable (oldVar, newVar));
		}
	} // end of METHOD substituteVariable()

	public TermList substitute (Substitution s) {
		if (_theRest == null) {
			return new TermList (_first.substitute(s), null);
		}
		else {
			return new TermList (_first.substitute(s),
								 _theRest.substitute(s));
		}
	} // end of METHOD substitute()

	// Convert free vars to constants
	public TermList removeQuantifiers (Vector quantifications){
		if (_theRest == null){
			return new TermList (_first.removeQuantifiers (quantifications), null);
		} else {
			return new TermList (_first.removeQuantifiers (quantifications),
								 _theRest.removeQuantifiers (quantifications));
		}
	}

	public String toString(){
		if (_theRest == null){
			return _first.toString();
		} else {
			return _first + "," + _theRest;
		}
	}

} // end of CLASS TermList
