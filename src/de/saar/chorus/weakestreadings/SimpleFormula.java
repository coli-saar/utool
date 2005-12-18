

package de.saar.chorus.weakestreadings;

import edu.mit.techniques.FOL.Sentence;



abstract class SimpleFormula {
    static protected int nextAtomId = 1;

    static protected int nextQuant; // use only in parse() function
    static protected String descInProc; // use only in parse() function

    protected int var;
    protected SimpleFormula left, right; // children

    static protected String atomGenerator() {
	return "a" + (nextAtomId++);
    }

    protected String varName() {
	return varNameFor(var);
    }

    protected static String varNameFor(int i) {
	return "y" + i;
    }

    public Sentence toSentence() {
	return sentenceRecursion(0, 0);
    }

    abstract protected Sentence sentenceRecursion(int leftVar, int rightVar);


    public String toString() {
	return toSentence().toString();
    }

    public static void resetAtomId() {
	nextAtomId = 1;
    }


    // This method is not thread-safe, as there's only one quant. counter
    // for the entire class.
    public static SimpleFormula parse(String desc) {
	nextQuant = 1;
	descInProc = desc;
	resetAtomId();
	return parseRecursion();
    }

    protected static SimpleFormula parseRecursion() {
	if( descInProc.length() == 0 ) {
	    return null; // should never happen
	} else {
	    char c = descInProc.charAt(0);

	    switch(c) {
	    case 'a':
	    case 'e':
		int thisQuant = nextQuant++;
		SimpleFormula left, right;

		if( descInProc.charAt(1) == '(' ) {
		    descInProc = descInProc.substring(2); // skip "a("
		    left = parseRecursion();
		    
		    descInProc = descInProc.substring(1); // skip ","
		    right = parseRecursion();

		    descInProc = descInProc.substring(1); // skip ")"
		} else {
		    descInProc = descInProc.substring(1); // skip "a"

		    left = new SimpleAtom();
		    right = new SimpleAtom();
		}

		if( c == 'a') 
		    return new SimpleAll(thisQuant, left, right);
		else
		    return new SimpleExists(thisQuant, left, right);

	    case 'l':
	    case 'r':
		char rule = descInProc.charAt(1);
		int quant1 = nextQuant++;
		int quant2 = nextQuant++;

		SimpleFormula A, B, C;

		if( (descInProc.length() >= 3) 
		    && (descInProc.charAt(2) == '(') ) {

		    descInProc = descInProc.substring(3); // skip "ra("
		    A = parseRecursion();

		    descInProc = descInProc.substring(1); // skip ","
		    B = parseRecursion();

		    descInProc = descInProc.substring(1); // skip ","
		    C = parseRecursion();

		    descInProc = descInProc.substring(1); // skip ")"
		} else {
		    descInProc = descInProc.substring(2); // skip "ra"

		    A = new SimpleAtom();
		    B = new SimpleAtom();
		    C = new SimpleAtom();
		}
		    

		if( c == 'l' ) {
		    switch(rule) {
		    case 'a': return new SimpleExists(quant1,
						      new SimpleAll(quant2, A, B),
						      C);
		    case 'b': return new SimpleAll(quant1,
						   new SimpleExists(quant2, A, B),
						   C);
		    case 'c': return new SimpleAll(quant1, A,
						   new SimpleAll(quant2, B, C));
		    default: return null; // error
		    }
		} else {
		    switch(rule) {
		    case 'a': return new SimpleAll(quant1, A,
						   new SimpleExists(quant2, B, C));
		    case 'b': return new SimpleExists(quant1, A,
						      new SimpleAll(quant2, B, C));
		    case 'c': return new SimpleAll(quant1,
						   new SimpleAll(quant2, A, B),
						   C);
		    default: return null; // error
		    }
		}


	    case '.':
		descInProc = descInProc.substring(1); // skip "."

		return new SimpleAtom();

	    default:
		System.err.println("Illegal string in parseRecursion: " + descInProc);
		return null;
	    }
	}
    }

}

