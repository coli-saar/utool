package edu.mit.techniques.PL;

import java.util.*;

/** Represents an atomic variable.  Variables are named with single
    characters. */
public class Variable extends Sentence {

  private String symbol;

  /** Constructs a <code>Variable</code> with name <code>symbol</code>. */
  public Variable(char symbol){
	this.symbol = "" + symbol;
  }
  public Variable(String symbol){
	this.symbol = "" + symbol;
  }

  /** Variables are considered equal if they were constructed with
     the same name. */
  public boolean equals(Object o){
	return (o instanceof Variable)
	  ? symbol.equals(((Variable) o).symbol)
	  : false;
  }

  public int hashCode(){
	return symbol.hashCode();
  }

  public Set getVariables(){
	Set set = new HashSet();
	set.add(this);
	return set;
  }

  public Boolean isSatisfied(Interpretation interpretation){
	return (!interpretation.containsKey(this))
	  ? null
	  : (Boolean) interpretation.get(this);
  }
  
  public String toString(){
	return symbol;
  }

  public Object clone(){
	// No need for a deep copy
	return super.clone();
  }

}
