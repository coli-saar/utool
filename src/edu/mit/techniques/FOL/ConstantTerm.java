package edu.mit.techniques.FOL;

import java.util.Vector;

public class ConstantTerm extends Term {

  ////
  // VARIABLES
  private String name;

  /////
  // CONSTRUCTOR
  public ConstantTerm (String _name){
    name = _name;
  }

  /////
  // OTHER METHODS

    public Symbol getFunctionSymbol() {
	return new Symbol(name);
    }

  /**
   * Taken from abstract method in Term.  Checks for equality.  If not equal,
   * returns null, else returns <s>.
   */
  public Substitution unify(Term t, Substitution s) {
    Sentence.debugPrint("ConstantTerm: ");
    if (equals(t)) {
      Sentence.debugPrintln("UNIFIED: name: " + name);
      return s;
    }
    // if the other is a variable, call Variable's unify()
    else if (t instanceof Variable)
      return ((Variable)t).unify(this, s);
    else {
      Sentence.debugPrintln("NULL");
      return null;
    }
  } // end of METHOD unify()


	public Term removeQuantifiers (Vector quantifications){
		return this;
	}

  public Term substituteVariable(Variable oldVar, Term newVar){
    return this;
  } // end of METHOD substituteVariable()

  /**
   * From Term abstract class.
   *
   * returns an empty Vector, because a ConstantTerm has no variables in it.
   */
  public Vector obtainVariables() {
    return new Vector();
  } // end of METHOD obtainVariables()

  /**
   * From Term abstract class
   *
   * since <s> is all indexed by Variables, this should return itself
   */
  public Term substitute(Substitution s) {
    return this;
  } // end of METHOD substitute()

  public boolean equals(Object o) {
    if (o != null) {
      if (o instanceof ConstantTerm) {
        return name.equals(((ConstantTerm)o).name);
      }
      else
        return false;
    }
    else
      return false;
  } // end of METHOD equals()

	public String toString(){
		return name;
	}

} // end of CLASS ConstantTerm
