
package de.saar.chorus.oracle;


public class StateEvaluation {
    protected String stateName;
    protected double eval;

    StateEvaluation(String stateName, double eval) {
	this.stateName = stateName;
	this.eval = eval;
    }

    public String getStateName() {
	return stateName;
    }

    public double getEval() {
	return eval;
    }
}

