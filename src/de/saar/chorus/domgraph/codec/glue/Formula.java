/*
 * @(#)Formula.java created 22.02.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.codec.glue;

import java.util.ArrayList;
import java.util.List;

class Formula {
    public enum Type {
        ATOM,
        VARIABLE,
        IMPLICATION
    }
    
    private Type type;
    private String symbol;
    private List<Formula> subformulas;
    
    public Formula(Type type, Formula sub1, Formula sub2) {
        this.type = type;
        this.subformulas = new ArrayList<Formula>(2);
        subformulas.add(sub1);
        subformulas.add(sub2);
        symbol = null;
    }
    
    public Formula(Type type, Formula sub1) {
        this.type = type;
        this.subformulas = new ArrayList<Formula>(1);
        subformulas.add(sub1);
        symbol = null;
    }

    public Formula(Type type, String symbol) {
        this.type = type;
        this.subformulas = null;
        this.symbol = symbol;
    }

    public List<Formula> getSubformulas() {
        return subformulas;
    }

    public String getSymbol() {
        return symbol;
    }

    public Type getType() {
        return type;
    }
    
    public String toString() {
        return toString(false);
    }
    
    private String toString(boolean addBrackets) {
        switch(type) {
        case ATOM:
        case VARIABLE:
            return symbol;
            
        case IMPLICATION:
            if( addBrackets ) {
                return "(" + subformulas.get(0).toString(true)
                + " -o " + subformulas.get(1).toString(true) + ")";
            } else {
                return subformulas.get(0).toString(true)
                + " -o " + subformulas.get(1).toString(true);
            }
        }
        
        return null;
    }
    
    
    
    
    
    

}
