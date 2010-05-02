

package de.saar.chorus.weakestreadings;

import edu.mit.techniques.FOL.Sentence;
import edu.mit.techniques.FOL.Variable;



class SimpleAll extends SimpleFormula {
    SimpleAll(int var, SimpleFormula left, SimpleFormula right) {
	this.left = left;
	this.right = right;
	this.var = var;
    }

    SimpleAll(int var) {
	this.var = var;
	this.left = new SimpleAtom();
	this.right = new SimpleAtom();
    }

    protected Sentence sentenceRecursion(int leftVar, int rightVar) {
	return Auxiliary.forall(new Variable(varName()),
				left.sentenceRecursion(leftVar, var),
				right.sentenceRecursion(var, rightVar));
    }
}
