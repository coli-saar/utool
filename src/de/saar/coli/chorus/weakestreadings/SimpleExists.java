

package de.saar.coli.chorus.weakestreadings;

import edu.mit.techniques.FOL.*;



class SimpleExists extends SimpleFormula {
    SimpleExists(int var, SimpleFormula left, SimpleFormula right) {
	this.left = left;
	this.right = right;
	this.var = var;
    }

    SimpleExists(int var) {
	this.var = var;
	this.left = new SimpleAtom();
	this.right = new SimpleAtom();
    }

    protected Sentence sentenceRecursion(int leftVar, int rightVar) {
	return Auxiliary.exists(new Variable(varName()),
				left.sentenceRecursion(leftVar, var),
				right.sentenceRecursion(var, rightVar));
    }
}
