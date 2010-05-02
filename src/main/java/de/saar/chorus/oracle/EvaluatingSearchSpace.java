
package de.saar.chorus.oracle;

import java.util.List;


public class EvaluatingSearchSpace<DomainType> extends SearchSpace<DomainType> {
    final protected Evaluator<DomainType> eval;

    public EvaluatingSearchSpace(Evaluator<DomainType> eval) {
        super();
        
        this.eval = eval;
    }

    public Evaluator<DomainType> getEvaluator() {
        return eval;
    }

    public double evaluate(String stateName) {
        return eval.evaluate(getStateForName(stateName));
    }

    public StateEvaluation minLocal(String parentName) {
        List<String> children = getChildren(parentName);
        double minValue;
        String minState = null;

        if( children.size() == 0 ) {
            return null;
        } else {
            minState = children.get(0);
            minValue = evaluate(minState);

            for( String state : children ) {
                double e = evaluate(state);

                if( e < minValue ) {
                    minValue = e;
                    minState = state;
                }
            }

            return new StateEvaluation(minState, minValue);
        }
    }

    public StateEvaluation minGlobal() {
        double minValue;
        String minState = null;

        if( isEmpty() ) {
            return null;
        } else {
            minState = "root";
            minValue = evaluate("root");

            for( String state : states.keySet() ) {
                double e = evaluate(state);

                if( e < minValue ) {
                    minValue = e;
                    minState = state;
                }
            }

            return new StateEvaluation(minState, minValue);
        }
    }
}
