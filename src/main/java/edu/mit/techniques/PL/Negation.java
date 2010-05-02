package edu.mit.techniques.PL;

import java.util.Set;

/** Represents the negation of a <code>Variable</code>. */
public class Negation extends Sentence {

  private Variable child;

  /* The negation of <code>sentence</code>. */
  public Negation(Variable variable){
	this.child = variable;
  }

  public Set getVariables(){
	return child.getVariables();
  }

  public Boolean isSatisfied(Interpretation interpretation){
	Boolean childSat = child.isSatisfied(interpretation);
	if(childSat == null)
	  return null;
	else
	  return new Boolean(!childSat.booleanValue());
  }

  public String toString(){
	return "~" + child;
	//return "" + CNF.neg + child;
  }

  public boolean equals(Object o){
    if( o instanceof Negation ){
      return child.equals( ((Negation)o).child );
    }
    return false;
  }

  public int hashCode(){
	return ~child.hashCode();
  }

  public Object clone(){
	Negation newNegation = (Negation) super.clone();
	newNegation.child = (Variable) child.clone();
	return newNegation;
  }

}
