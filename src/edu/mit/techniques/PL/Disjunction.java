package edu.mit.techniques.PL;

import java.util.*;

/** Represents a disjunction of a list of sentences. */
public class Disjunction extends Sentence {

  private List clauses = new ArrayList();

  /** Constructs a Disjunction from a <code>List</code> of literals,
      i.e. <code>Variable</code>s and <code>Negation</code>s. */
  public Disjunction(List clauses){
	this.clauses.addAll(clauses);
  }
  
  /** Returns the list of literals of this Disjunction. NOTE: mutating the
      returned List will mutate this Disjunction!  Be sure you
      understand what you are doing if you choose to do this. */
  public List getClauses(){
	return clauses;
  }

  public Set getVariables(){
    Set props = new HashSet();

    Iterator i = clauses.iterator();
	while(i.hasNext())
	  props.addAll(((Sentence)i.next()).getVariables());
	return props;
  }
  
  public Boolean isSatisfied(Interpretation interpretation){
	boolean undetermined = false;
	boolean result = false;
    Iterator i = clauses.iterator();
	while(i.hasNext()){
	  Boolean subResult = ((Sentence)i.next()).isSatisfied(interpretation);
	  if (subResult == null)
		undetermined |= true;
	  else
		result |= subResult.booleanValue();
	}

	if(result)
	  return Boolean.TRUE;
	else
	  return (undetermined) ? null : Boolean.FALSE;
  }

  public String toString(){
	StringBuffer sb = new StringBuffer("(");
	Iterator i = clauses.iterator();
	if(i.hasNext())
	  sb.append(i.next().toString());	  
	while(i.hasNext())
	  sb.append(" " + CNF.vee + " " + i.next().toString());
	sb.append(")");
	return sb.toString();
  }

  public Object clone(){
	Disjunction newDisjunction = (Disjunction) super.clone();
	newDisjunction.clauses = new ArrayList();
	Iterator iter = clauses.iterator();
	while(iter.hasNext())
	  newDisjunction.clauses.add(((Sentence)iter.next()).clone());
	return newDisjunction;
  }

}
