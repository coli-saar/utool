

package de.saar.coli.chorus.weakestreadings;

import edu.mit.techniques.FOL.*;
import edu.mit.techniques.FOL.parser.*;


class Auxiliary {
    static Sentence forall(Variable x, Sentence restr, Sentence scope) {
	return new Universal(x, new Implication(restr, scope));
    }

    static Sentence exists(Variable x, Sentence restr, Sentence scope) {
	return new Existential(x, new Conjunction(restr, scope));
    }

    
    static Sentence listAnd(Sentence[] sents) {
	Sentence ret;

	if( sents.length > 0 ) {
	    ret  = sents[0];

	    for( int i = 1; i < sents.length; i++ )
		if( !ret.isSentenceType("TrueProposition") )
		    ret = new Conjunction(sents[i], ret);
	    
	    return ret;
	} else
	    return Sentence.TRUE;
    }

    static Sentence universalClosure(Sentence sent) {
	Variable[] fv = sent.freeVariables();
	Sentence ret = sent;

	for( int i = 0; i < fv.length; i++ )
	    ret = new Universal(fv[i], ret);

	return ret;
    }

    static Sentence existentialClosure(Sentence sent) {
	Variable[] fv = sent.freeVariables();
	Sentence ret = sent;

	for( int i = 0; i < fv.length; i++ )
	    ret = new Existential(fv[i], ret);

	return ret;
    }

}
