package edu.mit.techniques.FOL;

import java.util.*;
import java.io.*;

/**
 * A Clause is a set of Literals, implicitly disjoined.
 */
public class Clause {

    // This should be done with subclasses, ideally, but we're in a hurry
    private boolean isTrue;

    private Vector _literals;

    public Clause (){
	_literals = new Vector();
	isTrue = false;
    }

    public Clause (Literal l){
	this();
	addLiteral(l);
    }

    // copy constructor
    public Clause(Clause c) {
	isTrue = false;
	_literals = (Vector)c._literals.clone();
    }

    public Clause (Vector ls){
	isTrue = false;
	_literals = ls;
    }

    public Clause (Vector ls, boolean status){
	isTrue = status;
	_literals = ls;
    }

    public boolean isTrue(){
	return isTrue;
    }

    public Literal literalAt(int i){
	return (Literal) _literals.elementAt(i);
    }

    public int size(){
	return _literals.size();
    }

    public void addLiteral (Literal newL){
	int containsResult = containsLiteral(newL);

	if (containsResult == 2){
	    _literals.addElement(newL);
	    isTrue = true;
	}
	
	// Add as usual
	if (containsResult == 0){
	    _literals.addElement(newL);
	}

    }

    // This is really ugly, but so it goes.  Would be nice if Jav
    // had multiple return values, like commonLisp

    // Check to see if literal testL is in our list.  Returns:
    // 0 if it is not in the list
    // 1 if it is in the list exactly
    // 2 if it is in the list, but with the opposite sense
    
    private int containsLiteral(Literal testL){
	Proposition p = testL.proposition();
	boolean positive = (testL instanceof PositiveLiteral);

	for (int i = 0; i < _literals.size(); i++) {
	    Literal currentL = literalAt(i);
	    if (currentL.proposition().equals(p)){
		if (positive){
		    if (currentL instanceof PositiveLiteral){
			return 1;
		    } else {
			return 2;
		    }
		} else {
		    if (currentL instanceof PositiveLiteral){
			return 2;
		    } else {
			return 1;
		    }
		}
	    }
	}

	return 0;
    }


    public void removeLiteralAt(int i) {
	_literals.removeElementAt(i);
    }


    // take <s> and substitute in a copy of <this> Clause as necessary.
    public Clause substitute(Substitution s) {
	Clause c = new Clause();
	for (int i = 0; i < _literals.size(); i++) {
	    // Literal oldL = (Literal)_literals.get(i);
	    Literal oldL = literalAt(i);
	    Literal newL;
	    Proposition oldP = oldL.proposition();
	    Proposition newP = (Proposition)oldP.substitute(s);

	    if (oldL.isNegative())
		newL = new NegativeLiteral(newP);
	    else
		newL = new PositiveLiteral(newP);

	    c.addLiteral(newL);
	}
	return c;
    } // end of METHOD substitute()

    // this method renames all of the Variables in a copy of <this> Clause.
    // very inefficient algorithm.
    public Clause renameVariables() {
	Clause c = new Clause(this); // make a copy of it first
	Hashtable oldVars = new Hashtable();
	Vector oldVarV = new Vector();

	// find out all the variables in this clause
	for (int i = 0; i < c._literals.size(); i++) {
	    Literal l = (Literal)c._literals.get(i);
	    Vector lv = l.proposition().obtainVariables();

	    for (int j = 0; j < lv.size(); j++) {
		Variable v = (Variable)lv.get(j);
		if (!oldVarV.contains(v))
		    oldVarV.addElement(v);
	    }
	}
	System.out.println("clause " + c.toString() + ": " + oldVarV.toString());

	// remap
	for (int i = 0; i < oldVarV.size(); i++) {
	    Variable currV = (Variable)oldVarV.get(i);
	    Variable newV = currV.rename();
	    oldVars.put(currV, newV);
	}
	System.out.println(oldVars.toString());

	// go over all the variables again and replace them
	for (int i = 0; i < c._literals.size(); i++) {
	    Literal oldLiteral = (Literal)c._literals.get(i);
	    Proposition t = oldLiteral.proposition();
	    Set oldVarsSet = oldVars.keySet();
	    for (Iterator j = oldVarsSet.iterator(); j.hasNext(); ) {
		Variable oldVar = (Variable)j.next();
		t = (Proposition)t.substituteVariable(oldVar, (Variable)oldVars.get(oldVar));
	    }

	    c._literals.remove(i);
	    if (oldLiteral.isNegative())
		c._literals.insertElementAt(new NegativeLiteral(t), i);
	    else
		c._literals.insertElementAt(new PositiveLiteral(t), i);
	}

	return c;
    } 


    public Clause concatenate (Clause c2) {
	Vector v = (Vector) _literals.clone();
	Clause c1 = new Clause(v, isTrue);
	for (int i = 0; i < c2.size(); i++){
	    c1.addLiteral(c2.literalAt(i));
	}
	return c1;
    } 


    public String toString(){
	if (isTrue){
	    return "T";
	}

	String result;
	if (_literals.size() == 0)
	    result = "F";
	else {
	    result = ((Literal) _literals.elementAt(0)).toString();
	    for (int i = 1; i < _literals.size(); i++) {
		result = result + " v " + ((Literal) _literals.elementAt(i)).toString();
	    }
	}
	return result;
    } // end of METHOD toString()

    public void print(PrintStream ps){

	if (isTrue){
	    ps.print("T");
	    return;
	}

	int numLiterals = _literals.size();
	
	ps.print("(");
	
	if (numLiterals != 0) {
	    // Print the first one
	    ps.print((Literal)  _literals.elementAt(0));
	    
	    // If there are more, print the rest, with "v" between
	    if (numLiterals > 1) {
		for (int i = 1; i < numLiterals; i++){
		    ps.print(" v ");
		    ps.print((Literal) _literals.elementAt(i));
		}
	    }
	}
	ps.print(")");
    }

}
