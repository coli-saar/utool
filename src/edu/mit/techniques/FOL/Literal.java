package edu.mit.techniques.FOL;

public abstract class Literal {

	protected Proposition _proposition;

	public Proposition proposition(){
		return _proposition;
	}

	public abstract boolean isPositive();
	public abstract boolean isNegative();

} // end of CLASS Literal
