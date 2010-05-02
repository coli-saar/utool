package edu.mit.techniques.FOL;

public class PositiveLiteral extends Literal {

	public PositiveLiteral (Proposition p){
		_proposition = p;
	}

	public boolean isPositive(){
		return true;
	}

	public boolean isNegative(){
		return false;
	}

	public String toString(){
		return proposition().toString();
	}

}
