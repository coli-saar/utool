package edu.mit.techniques.FOL;

import java.util.*;



/** Represents a Sentence in first-order logic.  The Sentences all
    have a number of simplification methods. */
abstract public class Sentence implements Cloneable{

	/** The True proposition. */
 	public static Proposition TRUE = new TrueProposition();

	/** The False proposition. */
	public static Proposition FALSE = new FalseProposition();

	static private boolean _debugPrint = true;

	/** cloning returns a deep copy of a sentence. */
	public Object clone(){
		try{
			return super.clone();
		}
		catch(CloneNotSupportedException e){ // won't happen
			return null;
		}
	}

    /** Output the sentence in a format that is appropriate for Mace and Otter. */
    abstract public String toMace();

    /** Output the sentence in DFG syntax. */
    abstract public String toDFG();

	/** Removes all equivalence Connectives from the Sentence.  This
		is the first stage of simplification. */
	abstract public Sentence eliminateEquivalences();
  
	/** Removes all Implication Connectives from the Sentence.  This is 
		the second stage of simplification. */
	abstract public Sentence eliminateImplications();
  
	/** Drive in negations to get an equivalent Sentence. */
	abstract public Sentence driveInNegations();

	/** Simplifies this sentence by removing subtrees that are known
        to be true or false. */
	abstract public Sentence simplify();

	/** Return the negation of this sentence by applying deMorgan's law. */
	abstract public Sentence negate();

	/** Returns a list of clauses that is equivalent to this Sentence. */
	public ClauseList clausalForm(){
		Sentence processedSentence =
			eliminateEquivalences().eliminateImplications()
			.driveInNegations().renameVariables();
		
		// Move the quantifiers to the front.  Also change free vars to constants
		Vector quantifications = new Vector();
		processedSentence.extractQuantifications(quantifications);
		Sentence body = processedSentence.removeQuantifiers(quantifications);
	  
		// Skolemize
		body = skolemize(quantifications, body);
	  
		// Put into clausal form
		return body.makeClauses();
	}

	abstract public Sentence substitute(Substitution s);

	abstract public Sentence substituteVariable (Variable oldVar, Term newVar);

	abstract public Sentence renameVariables();

	abstract public void extractQuantifications (Vector quantifications);

	// Remove the quantifiers *and* convert free variables to constants
	abstract public Sentence removeQuantifiers(Vector quantifications);

	abstract public ClauseList makeClauses();

	private Sentence skolemize (Vector quantifications, Sentence body){
		// Walk through the quantifications, looking for existentials;  substitute
		// skolem functions into the body

		for (int i = 0; i < quantifications.size(); i++){
			Quantification qi = (Quantification) quantifications.elementAt(i);
			if (qi.isExistential()){

				// Make a list of all the universally quantified variables outside this
				// scope
				TermList terms = null;
				for (int j = 0; j < i; j++){
					Quantification qj = (Quantification) quantifications.elementAt(j);
					if (qj.isUniversal()){
						terms = new TermList(qj.variable(),terms);
					}
				}

				// Make the skolem function and substitute it in
				if (terms == null){
					// Just a constant
					body = body.substituteVariable(qi.variable(),
												   new ConstantTerm((new Symbol()).name()));
				} else {
					body = body.substituteVariable(qi.variable(),
												   new Function (new Symbol(), terms));
				}
			}
		}

		// Don't need the quantifications any more!
		return body;
	}

    abstract public Sentence[] subformulas();
    abstract public void setSubformula(int i, Sentence s);

    // Compute all free variables of the formula. General case: Set union
    // of the subformulas' free variables. Overload this for atoms and
    // quantifiers. Implementation has some overhead for the creation of
    // hashtables and enumerations; might be made more efficient.
    public Variable[] freeVariables() {
	Sentence[] s = subformulas();
	Hashtable vars = new Hashtable(); // keys are variable names
	Variable[] ret;

	for( int i = 0; i < s.length; i++ ) {
	    Variable[] subvars = s[i].freeVariables();
	    for( int j = 0; j < subvars.length; j++ )
		vars.put(subvars[j].getName(), "x");
	}

	ret = new Variable[vars.size()];
	int i = 0;
	for( Enumeration e = vars.keys(); e.hasMoreElements(); )
	    ret[i++] = new Variable((String) e.nextElement());

	return ret;
    }





	/**
	 * for debugging
	 */
	static public void debugPrintln(String s) {
		if (_debugPrint)
			System.out.println(s);
	} // end of METHOD debugPrintln()

	static public void debugPrint(String s) {
		if (_debugPrint)
			System.out.print(s);
	} // end of METHOD debugPrint()



    public boolean isSentenceType(String superclass) {
	try {
	    return Class.forName("edu.mit.techniques.FOL." + superclass).isAssignableFrom(getClass());
	} catch(Exception e) {
	    return false;
	}
    }


} // end of CLASS Sentence
