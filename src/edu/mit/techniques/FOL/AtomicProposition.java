package edu.mit.techniques.FOL;

import java.util.Vector;

public class AtomicProposition extends Proposition {

  /////
  // VARIABLES
  private String name;

  /////
  // CONSTRUCTORS
  public AtomicProposition (String _name){
    name = _name;
  }

  /////
  // OTHER METHODS

  /**
   * from Sentence abstract class.
   *
   * Cannot replace AtomicProposition
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

    public ClauseList makeClauses() {
	return new ClauseList(new PositiveLiteral(this));
    }

    public Symbol getRelationSymbol() {
	return new Symbol(name);
    }

    public int getArity() {
	return 0;
    }

    public TermList getTerms() {
	return null; // empty list
    }

  /**
   * Taken from abstract method in Proposition.
   *
   * If <p> is an AtomicProposition and equals <this> then return an empty
   * substitution, otherwise return null.
   */
  public Substitution unify (Proposition p) {
    debugPrint("AtomicProposition: ");
    if (p instanceof AtomicProposition &&
        ((AtomicProposition)p).name.equals(name)) {
      debugPrintln("UNIFIED: same name: " + name);
      return Substitution.getEmpty();
    }
    debugPrintln("NULL");
    return null;
  } // end of METHOD unify()

  /**
   * from Proposition abstract class.
   *
   * No variables are in an AtomicProposition.  Therefore, return empty Vector.
   */
  public Vector obtainVariables() {
    return new Vector();
  } // end of METHOD obtainVariables

	public String toString(){
		return name;
	}

} // end of CLASS AtomicProposition
