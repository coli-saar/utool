package edu.mit.techniques.FOL;

public class NegativeLiteral extends Literal {

	public NegativeLiteral (Proposition p){
		_proposition = p;
	}

	public boolean isPositive(){
		return false;
	}

	public boolean isNegative(){
		return true;
	}

	public String toString(){
		return "~" + proposition().toString();
	}

}
