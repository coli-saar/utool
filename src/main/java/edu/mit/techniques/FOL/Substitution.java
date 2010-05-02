package edu.mit.techniques.FOL;

import java.util.Hashtable;
import java.util.Vector;

public class Substitution {

  /* stores the Variable to Term bindings */
  private Hashtable _table;

  /* empty set of bindings */
  static private Substitution EMPTYBINDINGS = new Substitution();

	// Represent a substitution as two vectors:  one of variables, the other of
	// terms.  Might be nicer to use a vector of pairs, but it's too much trouble
	// to make a new datatype just for that....

	private Vector variables;
	private Vector terms;

  private Substitution() {
    _table = new Hashtable();


		variables = new Vector();
		terms = new Vector();
  }

  ////
  // ACCESSOR METHODS

  /**
   * obtains the Empty (representing no current bindings) object
   */
  static public Substitution getEmpty() {
    return EMPTYBINDINGS;
  } // end of METHOD getEmpty()

  /**
   * get current binding of the given Variable.
   */
  public Object getBinding(Variable var) {
    return _table.get(var);
  }


  ////
  // REPORTER METHODS

  /**
   * determines if the given Variable is currently bound.
   */
  public boolean isBound(Variable var) {
    return (getBinding(var) != null);
  } // end of METHOD isBound()

  /**
   * determine if this Substitution object represents the Empty object
   */
  public boolean isEmpty() {
    return (this == EMPTYBINDINGS);
  } // end of METHOD isEmpty()


  ////
  // MUTATING METHODS

  /**
   * extends this Substitution object with an association between the given
   * Variable and value.  Method changes the Substitution object.  Will not
   * work well for the various theorem provers which may want to share a
   * Substitution instance (or part thereof) among many lines of reasoning.
   */
  public Substitution addBinding(Variable var, Object val) {
      Substitution result = this;
      if (this == EMPTYBINDINGS) {
        result = new Substitution();
      }
      result._table.put(var, val);
      System.out.println(result);
      return result;
  } // end of METHOD addBinding()

	// Compose another substitution with this one, side-effecting this one

	public void compose (Substitution s2) {

	}

	// Apply substitution s2 to this one

	public void apply (Substitution s2){


	}

	// Add a new binding to a substitution (!! by side-effect)

	public void bind (Variable variable, Term term){
		variables.addElement(variable);
		terms.addElement(term);
	}

	// Look up a variable in this substitution.  Return the associated term,
	// or null if it doesn't occur.

	// Would be much more efficient if we had hashed the variables when reading
	// them (so that we could use object equivalence rather than name equivalence)
/*
	public Term lookUp (Variable variable){
		for (int i = 0; i < variables.size(); i++){
			if (variables.elementAt(i).equals(variable)){
				return (Term)terms.elementAt(i);
			}
		}
		return null;
	}
*/
	// The other thing we can do with a substitution is apply it to a sentence
	// We'll put that method in the sentence class.




  public String toString() {
    if (this == EMPTYBINDINGS) {
      return "[Bindings empty]";
    }
    else {
      return "[Bindings: " + _table.toString() + "]";
    }
  } // end of METHOD toString()

} // end of CLASS Substitution
