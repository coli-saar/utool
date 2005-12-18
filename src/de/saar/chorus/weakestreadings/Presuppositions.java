
package de.saar.chorus.weakestreadings;

import java.util.Enumeration;
import java.util.Vector;

import edu.mit.techniques.FOL.Conjunction;
import edu.mit.techniques.FOL.Disjunction;
import edu.mit.techniques.FOL.Existential;
import edu.mit.techniques.FOL.Implication;
import edu.mit.techniques.FOL.Negation;
import edu.mit.techniques.FOL.Quantifier;
import edu.mit.techniques.FOL.Sentence;
import edu.mit.techniques.FOL.Universal;
import edu.mit.techniques.FOL.Variable;


class Presuppositions {
    private static Sentence and(Sentence a, Sentence b) {
	if( a.isSentenceType("TrueProposition") )
	    return b;
	else if( b.isSentenceType("TrueProposition") )
	    return a;
	else
	    return new Conjunction(a,b);
    }

    private static Disjunction or(Sentence a, Sentence b) {
	return new Disjunction(a,b);
    }

    private static Sentence implies(Sentence a, Sentence b) {
	if( a.isSentenceType("TrueProposition") )
	    return b;
	else
	    return new Implication(a,b);
    }

    private static Negation not(Sentence a) {
	return new Negation(a);
    }

    private static Universal forall(Variable v, Sentence s) {
	return new Universal(v,s);
    }

    private static Existential exists(Variable v, Sentence s) {
	return new Existential(v,s);
    }

    private static Universal all(Variable v, Sentence restr, Sentence scope) {
	return forall(v, implies(restr, scope));
    }

    private static Existential exi(Variable v, Sentence restr, Sentence scope) {
	return exists(v, and(restr, scope));
    }
    
    // presuppositions

    // piip0(all(x,R,S)) = all(x,R,piip0(S)) & exists(x,R)
    // piip0(exi(x,R,S)) = exi(x,piip0(R),piip0(S))
    static Sentence piip0(Sentence sent) {
	if( sent.isSentenceType("Universal") ) {
	    Variable bound = ((Quantifier)sent).variable();
	    Sentence conn = sent.subformulas()[0];
	    Sentence R = conn.subformulas()[0];
	    Sentence S = conn.subformulas()[1];

	    // TODO: Abstieg in negative Polaritaet.
	    return and(all(bound, R, piip0(S)),
			exists(bound, R));
	} else if( sent.isSentenceType("Existential") ) {
	    Variable bound = ((Quantifier)sent).variable();
	    Sentence conn = sent.subformulas()[0];
	    Sentence R = conn.subformulas()[0];
	    Sentence S = conn.subformulas()[1];

	    return exi(bound, piip0(R), piip0(S));
	} else {
	    return sent;
	}
    }

    // piip1(all(x,R,S)) = all(x,piip1(R),piip1(S)) & exists(x,R)
    // piip1(exi(x,R,S)) = exi(x,piip1(R),piip1(S))
    static Sentence piip1(Sentence sent) {
	if( sent.isSentenceType("Universal") ) {
	    Variable bound = ((Quantifier)sent).variable();
	    Sentence conn = sent.subformulas()[0];
	    Sentence R = conn.subformulas()[0];
	    Sentence S = conn.subformulas()[1];

	    return and(all(bound, piip1(R), piip1(S)),
		       exists(bound, R));
	} else if( sent.isSentenceType("Existential") ) {
	    Variable bound = ((Quantifier)sent).variable();
	    Sentence conn = sent.subformulas()[0];
	    Sentence R = conn.subformulas()[0];
	    Sentence S = conn.subformulas()[1];

	    return exi(bound, piip1(R), piip1(S));
	} else {
	    return sent;
	}
    }

    // piip2(all(x,R,S)) = all(x,R,piip2(S)) & exists(x,piip2(R))
    // piip2(exi(x,R,S)) = exi(x,piip2(R),piip2(S))
    // Idee: (1) Man braucht fuer Lemma S die Praesuppositionen der inneren Formeln;
    //       (2) Praesuppositionen der inneren Formeln sollten den Kontext ignorieren.
    static Sentence piip2(Sentence sent) {
	if( sent.isSentenceType("Universal") ) {
	    Variable bound = ((Quantifier)sent).variable();
	    Sentence conn = sent.subformulas()[0];
	    Sentence R = conn.subformulas()[0];
	    Sentence S = conn.subformulas()[1];

	    return and(all(bound, R, piip2(S)),
		       exists(bound, piip2(R))); 
	} else if( sent.isSentenceType("Existential") ) {
	    Variable bound = ((Quantifier)sent).variable();
	    Sentence conn = sent.subformulas()[0];
	    Sentence R = conn.subformulas()[0];
	    Sentence S = conn.subformulas()[1];

	    return exi(bound, piip2(R), piip2(S));
	} else {
	    return sent;
	}
    }



    // piip2(all(x,R,S)) = all(x,R,piip2(S)) & exists(x,piip2(R))
    // piip2(exi(x,R,S)) = exi(x,piip2(R),piip2(S))
    // piipPol haengt Praesupp. in negativen Kontexten mit Implikation
    // an, in positiven mit Konjunktion.
    static Sentence piipPol(Sentence sent, boolean polarity) {
	if( sent.isSentenceType("Universal") ) {
	    Variable bound = ((Quantifier)sent).variable();
	    Sentence conn = sent.subformulas()[0];
	    Sentence R = conn.subformulas()[0];
	    Sentence S = conn.subformulas()[1];

	    if( polarity )
		return and(all(bound, piipPol(R, !polarity), 
			       piipPol(S, polarity)),
			   exists(bound, R));
	    else
		return implies(exists(bound, R),
			       all(bound, piipPol(R, !polarity),
				   piipPol(S, polarity)));
			   
	} else if( sent.isSentenceType("Existential") ) {
	    Variable bound = ((Quantifier)sent).variable();
	    Sentence conn = sent.subformulas()[0];
	    Sentence R = conn.subformulas()[0];
	    Sentence S = conn.subformulas()[1];

	    return exi(bound, piipPol(R, polarity), piipPol(S, polarity));
	} else {
	    return sent;
	}
    }

    // piiq2(all(x,R,S)) = exists(x,piiq2(R)) -> all(x,R,piiq2(S))
    // piiq2(exi(x,R,S)) = exi(x,piiq2(R),piiq2(S))
    // Idee: (1) Man braucht fuer Lemma S die Praesuppositionen der inneren Formeln;
    //       (2) Praesuppositionen der inneren Formeln sollten den Kontext ignorieren.
    static Sentence piiq2(Sentence sent) {
	if( sent.isSentenceType("Universal") ) {
	    Variable bound = ((Quantifier)sent).variable();
	    Sentence conn = sent.subformulas()[0];
	    Sentence R = conn.subformulas()[0];
	    Sentence S = conn.subformulas()[1];

	    return implies(exists(bound, piip2(R)),
			   all(bound, R, piip2(S)));
	} else if( sent.isSentenceType("Existential") ) {
	    Variable bound = ((Quantifier)sent).variable();
	    Sentence conn = sent.subformulas()[0];
	    Sentence R = conn.subformulas()[0];
	    Sentence S = conn.subformulas()[1];

	    return exi(bound, piip2(R), piip2(S));
	} else {
	    return sent;
	}
    }


    // piip3(all(x,R,S)) = all(x,R,piip3(S)) & all(x,R,S) & exists(x,piip3(R))
    // piip3(exi(x,R,S)) = exi(x,piip3(R),piip3(S))
    static Sentence piip3(Sentence sent) {
	if( sent.isSentenceType("Universal") ) {
	    Variable bound = ((Quantifier)sent).variable();
	    Sentence conn = sent.subformulas()[0];
	    Sentence R = conn.subformulas()[0];
	    Sentence S = conn.subformulas()[1];

	    return and(and(all(bound, R, piip3(S)),
			   all(bound, R, S)),
		       exists(bound, piip3(R)));
	} else if( sent.isSentenceType("Existential") ) {
	    Variable bound = ((Quantifier)sent).variable();
	    Sentence conn = sent.subformulas()[0];
	    Sentence R = conn.subformulas()[0];
	    Sentence S = conn.subformulas()[1];

	    return exi(bound, piip3(R), piip3(S));
	} else {
	    return sent;
	}
    }


    static Sentence naivePi(Sentence sent) {
	if( sent.isSentenceType("Universal") ) {
	    Variable bound = ((Quantifier)sent).variable();
	    Sentence conn = sent.subformulas()[0];
	    Sentence R = conn.subformulas()[0];
	    Sentence S = conn.subformulas()[1];

	    return and(and(exists(bound, R), 
			   all(bound, R, naivePi(S))),
		       naivePi(R));
	} else if( sent.isSentenceType("Existential") ) {
	    Variable bound = ((Quantifier)sent).variable();
	    Sentence conn = sent.subformulas()[0];
	    Sentence R = conn.subformulas()[0];
	    Sentence S = conn.subformulas()[1];

	    return and(exi(bound, R, naivePi(S)),
		       naivePi(R));
	} else
	    return Sentence.TRUE;
    }

    private static Vector listPi(Sentence sent) {
	Vector ret = new Vector();

	if( sent.isSentenceType("Universal") ) {
	    Variable bound = ((Quantifier)sent).variable();
	    Sentence conn = sent.subformulas()[0];
	    Sentence R = conn.subformulas()[0];
	    Sentence S = conn.subformulas()[1];

	    //Original:
	    ret.add(exists(bound, R));

	    // Hier ist der Versuch, zumindest die Praesuppositionen
	    // zusammenzufalten (i.e. Ex.quantoren zu teilen), wenn man schon nicht 
	    // die Praesupp. mit der Formel zusammenfaltet. Bringt aber nichts:
	    // Kein Entailment fuer b,c in ar,e. (Also genau wie pi selbst.)
	    //	    ret.add(exists(bound, and(R, pi(R))));

	    Vector subPi = listPi(S);
	    for(Enumeration e = subPi.elements(); e.hasMoreElements(); )
		ret.add(all(bound, R, (Sentence) e.nextElement()));

	    // experimental
// 	    for( Enumeration e = listPi(R).elements();
// 		 e.hasMoreElements(); ) 
// 		ret.add(exists(bound, (Sentence) e.nextElement()));
	    

	    ret.addAll(listPi(R)); // Original
	} else if( sent.isSentenceType("Existential") ) {
	    Variable bound = ((Quantifier)sent).variable();
	    Sentence conn = sent.subformulas()[0];
	    Sentence R = conn.subformulas()[0];
	    Sentence S = conn.subformulas()[1];

	    Vector subPi = listPi(S);
	    for(Enumeration e = subPi.elements(); e.hasMoreElements(); )
		ret.add(exi(bound, R, (Sentence)e.nextElement()));

	    ret.addAll(listPi(R));
	} 

	return ret;
    }

    static Sentence pi(Sentence sent) {
	Vector list = listPi(sent);
	Sentence ret = Sentence.TRUE;

	for(Enumeration e = list.elements(); e.hasMoreElements(); )
	    ret = and(ret, (Sentence) e.nextElement());

	return ret;
    }

    
    static Sentence existentialVersion(Sentence sent) {
	if( sent.isSentenceType("Universal") ) {
	    Variable bound = ((Quantifier)sent).variable();
	    Sentence conn = sent.subformulas()[0];
	    Sentence R = conn.subformulas()[0];
	    Sentence S = conn.subformulas()[1];

	    return exi(bound, existentialVersion(R),existentialVersion(S));
	} else {
	    Sentence[] sub = sent.subformulas();
	    Sentence ret = (Sentence) sent.clone();

	    for( int i = 0; i < sub.length; i++ )
		ret.setSubformula(i, existentialVersion(sub[i]));

	    return ret;
	}
    }

    static Sentence projected(Sentence sent) {
	//	System.err.println("project: " + sent);

	PresuppStore store = collectProject(sent);

	//	System.err.println("finished projected: " + store.projectEverything());

	Sentence ret = store.projectEverything();

	//System.err.println("Project: " + sent);
	//	System.err.println("Result: " + ret);

	return ret;
    }

    protected static PresuppStore collectProject(Sentence sent) {
	if( sent.isSentenceType("Universal") ) {
	    Variable bound = ((Quantifier)sent).variable();
	    Sentence conn = sent.subformulas()[0];
	    Sentence R = conn.subformulas()[0];
	    Sentence S = conn.subformulas()[1];

	    PresuppStore Rpre = collectProject(R);
	    PresuppStore Spre = collectProject(S);

	    Sentence fxR = Rpre.extractAsSentence(bound.getName());
	    Sentence fxS = Spre.extractAsSentence(bound.getName());

	    Rpre.merge(Spre);
	    Rpre.addToStore(exists(bound, R));

	    Rpre.setSentence(forall(bound,
				    implies(and(R, fxR), and(S, fxS))));
				    //				    implies(R, and(fxR, and(S, fxS)))));

	    return Rpre;

	} else if( sent.isSentenceType("Existential") ) {
	    Variable bound = ((Quantifier)sent).variable();
	    Sentence conn = sent.subformulas()[0];
	    Sentence R = conn.subformulas()[0];
	    Sentence S = conn.subformulas()[1];

	    PresuppStore Rpre = collectProject(R);
	    PresuppStore Spre = collectProject(S);

	    Sentence fxR = Rpre.extractAsSentence(bound.getName());
	    Sentence fxS = Spre.extractAsSentence(bound.getName());

	    Rpre.merge(Spre);
	    Rpre.setSentence(exists(bound,
				    and(and(and(R, S),
					    fxR),
					fxS)));

	    return Rpre;
	} else {
	    return new PresuppStore(sent);
	}
    }	



    static Sentence projectedPol(Sentence sent, boolean polarity) {
	PresuppStore store = collectProjectPol(sent, polarity);
	Sentence ret = store.projectEverythingPol(polarity);

	return ret;
    }

    protected static PresuppStore collectProjectPol(Sentence sent,
						    boolean polarity) {
	//	System.err.println("cpp " + sent + " (pol " + polarity + ")");

	if( sent.isSentenceType("Universal") ) {
	    Variable bound = ((Quantifier)sent).variable();
	    Sentence conn = sent.subformulas()[0];
	    Sentence R = conn.subformulas()[0];
	    Sentence S = conn.subformulas()[1];

	    PresuppStore Rpre = collectProjectPol(R, !polarity);
	    PresuppStore Spre = collectProjectPol(S, polarity);

	    Sentence fxR = Rpre.extractAsSentence(bound.getName());
	    Sentence fxS = Spre.extractAsSentence(bound.getName());

	    Rpre.merge(Spre);
	    Rpre.addToStore(exists(bound, R));

	    Rpre.setSentence(forall(bound,
				    implies(polarity ? 
					    implies(fxR, 
						    Rpre.getSentence()) :
					    and(fxR, Rpre.getSentence()),

					    polarity ?
					    and(fxS, Spre.getSentence()) :
					    implies(fxS, Spre.getSentence()))));

	    return Rpre;

	} else if( sent.isSentenceType("Existential") ) {
	    Variable bound = ((Quantifier)sent).variable();
	    Sentence conn = sent.subformulas()[0];
	    Sentence R = conn.subformulas()[0];
	    Sentence S = conn.subformulas()[1];

	    PresuppStore Rpre = collectProjectPol(R, polarity);
	    PresuppStore Spre = collectProjectPol(S, polarity);

	    Sentence fxR = Rpre.extractAsSentence(bound.getName());
	    Sentence fxS = Spre.extractAsSentence(bound.getName());

	    Rpre.merge(Spre);
	    Rpre.setSentence(exists(bound,
				    polarity ?
				    and(and(Rpre.getSentence(),
					    Spre.getSentence()),
					and(fxR, fxS)) :
				    implies(and(fxR, fxS),
					    and(Rpre.getSentence(),
						Spre.getSentence()))));

	    return Rpre;
	} else {
	    return new PresuppStore(sent);
	}
    }	





    static Sentence projectedPolPatched(Sentence sent, boolean polarity) {
	PresuppStore store = collectProjectPol(sent, polarity);
	Sentence ret = store.projectEverythingPolPatched(polarity);

	return ret;
    }







    static Sentence projectedImp(Sentence sent) {
	//	System.err.println("project: " + sent);

	PresuppStore store = collectProjectImp(sent);

	//	System.err.println("finished projected: " + store.projectEverything());

	Sentence ret = store.projectEverythingImp();

	//System.err.println("Project: " + sent);
	//	System.err.println("Result: " + ret);

	return ret;
    }

    protected static PresuppStore collectProjectImp(Sentence sent) {
	if( sent.isSentenceType("Universal") ) {
	    Variable bound = ((Quantifier)sent).variable();
	    Sentence conn = sent.subformulas()[0];
	    Sentence R = conn.subformulas()[0];
	    Sentence S = conn.subformulas()[1];

	    PresuppStore Rpre = collectProjectImp(R);
	    PresuppStore Spre = collectProjectImp(S);

	    Sentence fxR = Rpre.extractAsSentence(bound.getName());
	    Sentence fxS = Spre.extractAsSentence(bound.getName());

	    Rpre.merge(Spre);
	    Rpre.addToStore(exists(bound, R));

	    Rpre.setSentence(forall(bound,
				    implies(implies(R, fxR), implies(S, fxS))));
				    //				    implies(R, and(fxR, and(S, fxS)))));

	    return Rpre;

	} else if( sent.isSentenceType("Existential") ) {
	    Variable bound = ((Quantifier)sent).variable();
	    Sentence conn = sent.subformulas()[0];
	    Sentence R = conn.subformulas()[0];
	    Sentence S = conn.subformulas()[1];

	    PresuppStore Rpre = collectProjectImp(R);
	    PresuppStore Spre = collectProjectImp(S);

	    Sentence fxR = Rpre.extractAsSentence(bound.getName());
	    Sentence fxS = Spre.extractAsSentence(bound.getName());

	    Rpre.merge(Spre);
	    Rpre.setSentence(exists(bound,
				    implies(and(fxR, fxS), and(R, S))));

	    return Rpre;
	} else {
	    return new PresuppStore(sent);
	}
    }	
}
