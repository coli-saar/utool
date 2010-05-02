

package de.saar.chorus.weakestreadings;

import edu.mit.techniques.FOL.CompoundProposition;
import edu.mit.techniques.FOL.Sentence;
import edu.mit.techniques.FOL.Symbol;
import edu.mit.techniques.FOL.TermList;
import edu.mit.techniques.FOL.Variable;



class SimpleAtom extends SimpleFormula {
    protected String predicate;

    SimpleAtom(String predicate) {
	this.predicate = predicate;
    }

    SimpleAtom() {
	this.predicate = atomGenerator();
    }

    protected static String varNameLR(int lrVar, String deflt) {
	if( lrVar == 0 )
	    return deflt;
	else
	    return varNameFor(lrVar);
    }

    protected Sentence sentenceRecursion(int leftVar, int rightVar) {
	Variable x = new Variable(varNameLR(leftVar, "x"));
	Variable y = new Variable(varNameLR(rightVar, "z"));

	TermList terms = new TermList(x, new TermList(y, null));
	
	return new CompoundProposition(new Symbol(predicate), terms);
    }
}
