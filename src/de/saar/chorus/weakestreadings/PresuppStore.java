


package de.saar.chorus.weakestreadings;

import edu.mit.techniques.FOL.*;
import edu.mit.techniques.FOL.parser.*;

import java.util.*;


class PresuppStore {
    protected Sentence sent;
    protected Hashtable store;
    protected Hashtable usedStore;

    PresuppStore() {
	store = new Hashtable();
	usedStore = new Hashtable();
	sent = null;
    }

    PresuppStore(Sentence sent) {
	store = new Hashtable();
	usedStore = new Hashtable();
	this.sent = sent;
    }

    Sentence getSentence() {
	return sent;
    }

    void setSentence(Sentence s) {
	//System.err.println("Set sentence: " + s);
	sent = s;
    }

    void addToStore(String var, Sentence addme) {
	//System.err.println("Add to store for " + var + ": " + addme);

	if( store.containsKey(var) ) {
	    ((Vector) store.get(var)).add(addme);
	} else {
	    Vector v = new Vector();
	    v.add(addme);

	    store.put(var, v);
	}
    }

    void addToUsedStore(String var, Sentence addme) {
	//System.err.println("Add to store for " + var + ": " + addme);

	if( store.containsKey(var) ) {
	    ((Vector) usedStore.get(var)).add(addme);
	} else {
	    Vector v = new Vector();
	    v.add(addme);

	    usedStore.put(var, v);
	}
    }


    void addToStore(Sentence addme) {
	Variable[] freeVars = addme.freeVariables();

	if( freeVars.length != 1 ) {
	    addToStore("sentence", addme);
	    //	    System.err.println("WARNING! != 1 free variables in " + addme);
	} else {
	    addToStore(freeVars[0].getName(), addme);
	}
    }

    Vector getStoreFor(String var) {
	return (Vector) store.get(var);
    }

    Vector getUsedStoreFor(String var) {
	return (Vector) usedStore.get(var);
    }

    Sentence getAsSentence(String var) {
	Vector sents = getStoreFor(var);

	if( sents == null ) 
	    return Sentence.TRUE;

	else 
	    return Auxiliary.listAnd((Sentence[]) 
				     sents.toArray(new Sentence[0]));
    }
	

    Vector extractFromStore(String var) {
	//System.err.println("Extract fmlas for fv " + var);
	//System.err.println("Result: " + store.get(var));

	Vector ret = (Vector) store.remove(var);

	//	System.err.println("us: " + usedStore);
	//	System.err.println("var: " + var + " // ret: " + ret);

	if( ret != null ) usedStore.put( var, ret );

	//	System.err.println("new us: " + usedStore);
	

	return ret;
    }

    Sentence extractAsSentence(String var) {
	Vector sents = extractFromStore(var);

	if( sents == null ) 
	    return Sentence.TRUE;

	else 
	    return Auxiliary.listAnd((Sentence[]) 
				     sents.toArray(new Sentence[0]));
    }

    void merge(PresuppStore ps) {
	for( Enumeration en = ps.store.keys();
	     en.hasMoreElements() ; ) {
	    String var = (String) en.nextElement();
	    Vector psStore = ps.getStoreFor(var);

	    for( Enumeration sents = psStore.elements();
		 sents.hasMoreElements(); ) {
		addToStore( var, (Sentence) sents.nextElement() );
	    }
	}


	for( Enumeration en = ps.usedStore.keys();
	     en.hasMoreElements() ; ) {
	    String var = (String) en.nextElement();
	    Vector psStore = ps.getUsedStoreFor(var);

	    for( Enumeration sents = psStore.elements();
		 sents.hasMoreElements(); ) {
		addToUsedStore( var, (Sentence) sents.nextElement() );
	    }
	}
    }

    Sentence projectEverything() {
	Sentence ret = getSentence();

	//	System.err.println("projectEverything:");

	
	for( Enumeration en = store.elements();
	     en.hasMoreElements(); ) {
	    for( Enumeration it = ((Vector) en.nextElement()).elements();
		 it.hasMoreElements(); ) {
		
		Sentence s = (Sentence) it.nextElement();

		//		System.err.println("pe extract: " + s);

		if( !s.isSentenceType("TrueProposition") )
		    ret = new Conjunction(ret, s);
	    }
	}
	

	return ret;
    }


    Sentence projectEverythingPol(boolean polarity) {
	Sentence ret = getSentence();

	//	System.err.println("Remaining store: " + store);

	for( Enumeration en = store.elements();
	     en.hasMoreElements(); ) {
	    for( Enumeration it = ((Vector) en.nextElement()).elements();
		 it.hasMoreElements(); ) {
		
		Sentence s = (Sentence) it.nextElement();

		if( !s.isSentenceType("TrueProposition") )
		    if( polarity )
			ret = new Conjunction(ret, 
					      s);
		    else
			ret = new Implication(
					      s,
					      ret);
	    }
	}
	

	return ret;
    }



    Sentence projectEverythingPolPatched(boolean polarity) {
	Sentence ret = getSentence();

	Vector rest = new Vector();

	//	System.err.println("pepp store: " + store);
	//	System.err.println("pepp ustore: " + usedStore);
	
	for( Enumeration en = usedStore.elements();
	     en.hasMoreElements(); ) {

	    Vector entries = (Vector) en.nextElement();
	    Sentence entriesAsSent = Auxiliary.listAnd( (Sentence[]) entries.toArray(new Sentence[0]) );
	    rest.add(entriesAsSent);
	}

	Sentence restAsSent = Auxiliary.existentialClosure(Auxiliary.listAnd( (Sentence[]) rest.toArray(new Sentence[0]) ));

	if( polarity )
	    ret = new Conjunction(ret, restAsSent);
	else
	    ret = new Implication(restAsSent, ret);

	for( Enumeration en = store.elements();
	     en.hasMoreElements(); ) {
	    for( Enumeration it = ((Vector) en.nextElement()).elements();
		 it.hasMoreElements(); ) {
		
		Sentence s = (Sentence) it.nextElement();

		if( !s.isSentenceType("TrueProposition") )
		    if( polarity )
			ret = new Conjunction(ret, s);
		    else
			ret = new Implication(s, ret);
	    }
	}
	

	//	System.err.println("fertig: " + ret);
	return ret;
    }



    Sentence projectEverythingImp() {
	Sentence ret = getSentence();

	//	System.err.println("projectEverything:");

	
	for( Enumeration en = store.elements();
	     en.hasMoreElements(); ) {
	    for( Enumeration it = ((Vector) en.nextElement()).elements();
		 it.hasMoreElements(); ) {
		
		Sentence s = (Sentence) it.nextElement();

		//		System.err.println("pe extract: " + s);

		if( !s.isSentenceType("TrueProposition") )
		    ret = new Implication(s, ret);
	    }
	}
	

	return ret;
    }
}

