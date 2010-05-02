package edu.mit.techniques.FOL;

import java.util.Vector;

public class Variable extends Term {

  /**
   * a counter to generate unique identity counts.
   */
  private static long _counter = 0;
  /** the Variable name. */
  private String _name;
  /** the variable name the user gave.  this prevents the program from creating
   *  very long variable names just based on the value of _counter.
   */
  private String _realName;

  ////
  // CONSTRUCTORS

  public Variable (String name){
    this(name, name);
  }

  public Variable (String name, String realName) {
    _name = name;
    _realName = realName;
  }

  /////
  // ACCESSOR METHODS

  public String getName(){
    return _name;
  } // end of METHOD getName()


  /////
  // OTHER METHODS

  /**
   * Implements abstract method from Term.  Attempts to find the mgu of <this>
   * and <t>.
   */
  public Substitution unify(Term t, Substitution s) {
    Sentence.debugPrint("Variable: ");
    // if the substitution contains a binding for <this>, retrieve the binding
    // and try to unify the binding to <t>
    if (s.isBound(this)) {
      Sentence.debugPrintln("getting binding and re-call unify for this");
      return ((Term)s.getBinding(this)).unify(t, s);
    }
    // if <t> is variable and has a binding, retrieve the binding and try to
    // unify the binding to <this>
    else if (t instanceof Variable &&
             s.isBound((Variable)t)) {
      Sentence.debugPrintln("getting binding and re-call unify for t");
      return unify((Term)s.getBinding((Variable)t), s);
    }
    // if <t> contains <this>, then return null; no substitution is available
    else if (occursCheck(t, s)) {
      Sentence.debugPrintln("NULL: occurs check failed");
      return null;
    }
    // bind <this> to <t> and return the mutated <s>.
    else {
      s = s.addBinding(this, t);
      Sentence.debugPrintln("UNIFIED: added binding: [" + this.toString() + ", " + t.toString() + "]");
      return s;
    }
  } // end of METHOD unify()

  /**
   * helper method for unify() that determines if <this> is found within <t>.
   * if so, no substitution is available, and false is returned.
   */
  private boolean occursCheck(Term t, Substitution s) {
    // if <this> is equal to <t> return true
    if (equals(t))
      return true;
    // if <t> is variable and has a binding, see if the binding contains <this>
    else if (t instanceof Variable &&
             s.isBound((Variable)t))
      return occursCheck((Term)s.getBinding((Variable)t), s);
    // if <t> is a function, iterate through all terms to see if they contain
    // <this>
    else if (t instanceof Function) {
      TermList tlt = ((Function)t).getTerms();

      // this iterative loop was done to make the first parameter of
      // occursCheck() more restrictive to Term rather than just Object
      while (tlt != null) {
        Term term = tlt.getFirst();
        if (!occursCheck(term, s))
          return false;
        tlt = tlt.getRest();
      }
      return true;
    }
    else
      return false;
  } // end of METHOD occursCheck()

  /**
   * Generate a new variable whose name is <this._name> with a unique numeric
   * suffix
   */
  public Variable rename (){
    _counter++;
    return new Variable (_realName + _counter, _realName);
  } // end of METHOD rename()


  /**
   * from Term abstract class
   */
  public Term substitute(Substitution s) {
    if (s.isBound(this))
      return (Term)s.getBinding(this);
    else
      return this;
  } // end of METHOD substitute()

  /**
   * implemented from interface Term
   *
   * if oldVar's name is equal to the name of this, replace with newVar.
   */
  public Term substituteVariable (Variable oldVar, Term newVar){
    if (oldVar._name.equals(_name)){
      return newVar;
    }
    else
      return this;
  } // end of METHOD substituteVariable()

	// If this "variable" doesn't occur in the quantifications, replace it with a
	// constant

	public Term removeQuantifiers (Vector quantifications){
		if (isBound(quantifications)){
			return this;
		} else {
			return new ConstantTerm(_name);
		}
	}

	// Does this variable occur in the quantifications?  Be sure to compare names
	// rather than variable objects.

	private boolean isBound (Vector quantifications){
		for (int i = 0; i < quantifications.size(); i++){
			if (isBound((Quantification) quantifications.elementAt(i)))
				return true;
		}
		// Didn't find a binding
		return false;
	}

	// Does this variable occur in this quantification?

	private boolean isBound (Quantification q){
		return q.variable()._name == _name;
	}

  /**
   * from Term abstract class.
   *
   * Since this is sort of the base case, create a new Vector with this as
   * the sole element.
   */
  public Vector obtainVariables() {
    Vector result = new Vector();
    result.addElement(this);
    return result;
  } // end of METHOD obtainVariables()

  public String toString(){
    return _name;
  } // end of METHOD toString()

  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    else if (o instanceof Variable) {
      return _name.equals(((Variable)o)._name);
    }
    else {
      return false;
    }
  } // end of METHOD equals()

  public int hashCode() {
      return (int)_name.hashCode();
  } // end of METHOD hashCode()
}
