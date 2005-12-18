package edu.mit.techniques.PL;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/** Represents a conjunction of a list of sentences. */
public class Conjunction extends Sentence {

  private List clauses = new ArrayList();

  /** Constructs a conjunction from a <code>List</code> of
      <code>Disjunction</code>s. */
  public Conjunction(List clauses){
	this.clauses.addAll(clauses);
  }
  
  /** Returns the clauses of this Conjunction. NOTE: mutating the
      returned List will mutate this Conjunction!  Be sure you
      understand what you are doing if you choose to do this. */
  public List getClauses(){
	return clauses;
  }

  /** Returns the set of variables used in this Conjunction. */
  public Set getVariables(){
    Set props = new HashSet();

    Iterator i = clauses.iterator();
	while(i.hasNext())
	  props.addAll(((Sentence)i.next()).getVariables());
	return props;
  }
  
  public Boolean isSatisfied(Interpretation interpretation){
	boolean undetermined = false;
	boolean result = true;
    Iterator i = clauses.iterator();
	while(i.hasNext()){
	  Boolean subResult = ((Sentence)i.next()).isSatisfied(interpretation);
	  if (subResult == null)
		undetermined |= true;
	  else
		result &= subResult.booleanValue();
	}
	if(!result)
	  return Boolean.FALSE;
	else if(undetermined)
	  return null;
	else
	  return Boolean.TRUE;
  }

  public String toString(){
	StringBuffer sb = new StringBuffer();
	Iterator i = clauses.iterator();
	if(i.hasNext())
	  sb.append(i.next().toString());	  
	while(i.hasNext())
	  sb.append(" " + CNF.wedge + " " + i.next().toString());
	return sb.toString();
  }

  public Object clone(){
	Conjunction newConjunction = (Conjunction) super.clone();
	newConjunction.clauses = new ArrayList();
	Iterator iter = clauses.iterator();
	while(iter.hasNext())
	  newConjunction.clauses.add(((Sentence) iter.next()).clone());
	return newConjunction;
  }

}
