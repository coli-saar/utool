package edu.mit.techniques.FOL;

public class Quantification {

// A quantifier and its variable

	// A quantifier can be either existential or universal
	public static boolean EXISTENTIAL = true;
	public static boolean UNIVERSAL = false;
	
	private boolean quantifier;
	private Variable var;
	
	public Quantification (boolean _quantifier, Variable _var){
		quantifier = _quantifier;
		var = _var;
	}
	
	public boolean isExistential(){
		return quantifier == EXISTENTIAL;
	}
	
	public boolean isUniversal(){
		return quantifier == UNIVERSAL;
	}
	
	public Variable variable(){
		return var;
	}
	
	public String toString(){
		if (quantifier == EXISTENTIAL){
			return "E " + var.toString();
		} else {
			return "A " + var.toString();
		}
	}
}
		
	
	
