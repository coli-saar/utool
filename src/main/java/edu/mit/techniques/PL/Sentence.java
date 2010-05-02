package edu.mit.techniques.PL;

import java.util.Set;

/** Abstract class representing a sentence in propositional logic. It
    is the base class of all the propositional connectives and
    variables. */
abstract public class Sentence implements Cloneable{

  /** Returns the set of all <code>Variable</code>s appearing in this
      <code>Sentence</code>. */
  abstract public Set getVariables();
  
  /** Evaluates the truth value of this <code>Sentence</code> under
	  <code>interpretation</code>. If the truth value of the sentence
	  is undetermined by <code>interpretation</code>, returns
	  <code>null</code>. */
  abstract public Boolean isSatisfied(Interpretation interpretation);

  /** <code>Sentence</code>s perform a deep copy when cloning. */
  public Object clone() {
	try{
	  return super.clone();
	}
	catch(CloneNotSupportedException e){
	  return null; // won't happen
	}
  }

}
