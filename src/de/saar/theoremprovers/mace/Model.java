
package de.saar.coli.theoremprovers.mace;

import java.util.*;


public class Model {
    private int size;

    private Hashtable predicates, functions;



    public static final int TYPE_PREDICATE = 1;
    public static final int TYPE_FUNCTION  = 2;
    public static final int TYPE_NONE      = 0;



    public Model() {
	size = 0;

	predicates = new Hashtable();
	functions  = new Hashtable();
    }

    void setSize(int s) {
	size = s;
    }

    void addPredicate(String label, int arity, int[] table) {
	predicates.put(label, new ModelTable(label, arity, table));
    }
    
    void addFunction(String label, int arity, int[] table) {
	functions.put(label, new ModelTable(label, arity, table));
    }

    public String toString() {
	String ret = "<model size=" + size + ">";

	return ret;
    }


    int getSize() {
	return size;
    }

    int getType(String label) {
	if( predicates.containsKey(label) )
	    return TYPE_PREDICATE;

	else if( functions.containsKey(label) )
	    return TYPE_FUNCTION;
	
	else
	    return TYPE_NONE;
    }


    int getArity(String label) {
	switch(getType(label)) {
	case TYPE_PREDICATE:
	    return ((ModelTable) predicates.get(label)).getArity();

	case TYPE_FUNCTION:
	    return ((ModelTable) functions.get(label)).getArity();

	default:
	    return -1;
	}
    }

    
    int[] getTable(String label) {
	switch(getType(label)) {
	case TYPE_PREDICATE:
	    return ((ModelTable) predicates.get(label)).getTable();

	case TYPE_FUNCTION:
	    return ((ModelTable) functions.get(label)).getTable();

	default:
	    return null;
	}
    }

    
    ModelTable[] getPredicates() {
	ModelTable[] ret = new ModelTable[predicates.size()];
	int i = 0;

	for(Enumeration e = predicates.elements(); e.hasMoreElements(); )
	    ret[i++] = (ModelTable) e.nextElement();

	return ret;
    }


    ModelTable[] getFunctions() {
	ModelTable[] ret = new ModelTable[functions.size()];
	int i = 0;

	for(Enumeration e = functions.elements(); e.hasMoreElements(); )
	    ret[i++] = (ModelTable) e.nextElement();

	return ret;
    }
}
