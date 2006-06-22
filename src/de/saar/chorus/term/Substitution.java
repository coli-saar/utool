/*
 * @(#)Substitution.java created 11.05.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.term;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.testng.annotations.Test;


public class Substitution implements Cloneable {
    // subst maps variables to terms. INVARIANT: Variables on the LHS
    // of any mapping never occur on the RHS of any mapping.
    private Map<Variable,Term> subst;
    
    private boolean valid;
    
    public Substitution() {
        subst = new HashMap<Variable,Term>();
        valid = true;
    }
    
    public Substitution(Variable v, Term t) {
        subst = new HashMap<Variable,Term>();
        valid = true;
        subst.put(v,t);
    }
    
    public boolean isValid() {
        return valid;
    }

    public void addSubstitution(Variable v, Term t) {
        copy(concatenate(new Substitution(v,t)), this);
    }
    
    
    
    public Term apply(Term t) {
        switch(t.getType()) {
        case VARIABLE:
            Variable v = (Variable) t;
            if( subst.containsKey(v) ) {
                return subst.get(v);
            } else {
                return t;
            }
            
        case CONSTANT:
            return t;
            
        case COMPOUND:
            Compound com = (Compound) t;
            List<Term> newSubterms = new ArrayList<Term>(com.getSubterms().size());
            
            for( Term subterm : com.getSubterms() ) {
                newSubterms.add(apply(subterm));
            }
            
            return new Compound(com.getLabel(), newSubterms);
        }
        
        // unreachable
        return null;
    }
    
    public List<Term> apply(List<Term> terms) {
        List<Term> ret = new ArrayList<Term>(terms.size());
        
        for( Term term : terms ) {
            ret.add(apply(term));
        }
        
        return ret;
    }
    
    
    public Substitution concatenate(Substitution other) {
        Substitution ret = (Substitution) clone();
        Queue<Map.Entry<Variable,Term>> addQueue = new LinkedList<Map.Entry<Variable,Term>>(other.subst.entrySet());
        
        // concatenation inherits invalidity from both sides
        if( !isValid() || !other.isValid() ) {
            ret.valid = false;
            return ret;
        }

        while( !addQueue.isEmpty() ) {
            Map.Entry<Variable,Term> el = addQueue.remove();
            Variable v = el.getKey();
            Term t = el.getValue();
            
            // 1. apply substitution to t to eliminate variables that occur on
            // the LHS of the substitution
            t = ret.apply(t);
            
            if( t.hasSubterm(v)) {
                ret.valid = false;
                break;
            } else {
                if( ret.subst.containsKey(v)) {
                    // 2a. If the LHS occurs in the substitution, then unify
                    // the RHSs. It isn't necessary to apply (v -> unified) to
                    // any other mapping in the substitution because v can't
                    // occur both on the LHS and the RHS.
                    Substitution unifier = t.getUnifier(ret.subst.get(v));
                    
                    if( unifier != null ) {
                        Term unified = unifier.apply(t);
                        
                        if( !unified.hasSubterm(v) ) {
                            ret.subst.put(v, unified);
                            
                            for( Map.Entry<Variable,Term> pair : unifier.subst.entrySet() ) {
                                addQueue.offer(pair);
                            }
                        } else {
                            ret.valid = false;
                            break;
                        }
                    } else {
                        ret.valid = false;
                        break;
                    }
                } else {
                    // 2b. If the LHS doesn't occur in the substitution, then
                    // apply (v -> t) to all RHSs (to eliminate v) and add
                    // (v -> t) to the substitution.
                    Substitution newSubst = new Substitution(v,t);
                    
                    for( Map.Entry<Variable,Term> entry : ret.subst.entrySet() ) {
                        ret.subst.put(entry.getKey(), newSubst.apply(entry.getValue()));
                    }
                    
                    ret.subst.put(v,t);
                }
            }
            
       }
        
        return ret;
    }
    
    public String toString() {
        if( valid )
            return subst.toString();
        
        else
            return "(INVALID)";
    }
    
    public boolean equals(Object o) {
        if (o instanceof Substitution) {
            Substitution substi = (Substitution) o;
            
            return (substi.valid == valid)
            && substi.subst.equals(subst);
        } else {
            return false;
        }
    }

    public Object clone()  {
        Substitution ret = new Substitution();
        
        copy(this,ret);

        return ret;
    }
    
    private static void copy(Substitution from, Substitution to) {
        to.valid = from.valid;
        
        to.subst.clear();
        to.subst.putAll(from.subst);
    }

    
    
    
    
    
    /***************************************************************
     * UNIT TESTS
     ***************************************************************/

    @Test(groups = {"Term"})
    public static class UnitTests {
        private Variable x = new Variable("X"), y = new Variable("Y"), z = new Variable("Z");
        

        public void constructors() {
            Substitution subst1 = new Substitution(new Variable("X"), Term.parse("f(a)"));

            Substitution subst2 = new Substitution();
            subst2.addSubstitution(new Variable("X"), Term.parse("f(a)"));
            
            assert subst1.equals(subst2) : "subst2 is " + subst2;
            
            Substitution subst3 = Term.parse("f(a)").substFor(new Variable("X"));
            assert subst1.equals(subst3) : "subst3 is " + subst3;
            
            Substitution subst4 = Term.parse("f(a)").substFor("X");
            assert subst1.equals(subst4) : "subst4 is " + subst4;
        }
        
        
        public void fx() {
            Term fx = Term.parse("f(X)");
            Substitution subst = Term.parse("g(b,c(d))").substFor("X");
            
            assert subst.apply(fx) != fx;
            assert subst.apply(fx) != null;
            assert subst.apply(fx).equals(Term.parse("f(g(b,c(d)))"));
        }
        
        public void fxy1() {
            Term fxy = Term.parse("f(X,Y)");
            Substitution subst = Term.parse("g(b,c(d))").substFor("X");
            
            assert subst.apply(fxy) != fxy;
            assert subst.apply(fxy) != null;
            assert subst.apply(fxy).equals(Term.parse("f(g(b,c(d)),Y)"));
        }
        
        public void fxy2() {
            Term fxy = Term.parse("f(X,Y)");
            Substitution subst = Term.parse("g(b,c(d))").substFor("X");
            
            subst.addSubstitution(new Variable("Y"), Term.parse("e(f)"));
            
            assert subst.apply(fxy) != fxy;
            assert subst.apply(fxy) != null;
            assert subst.apply(fxy).equals(Term.parse("f(g(b,c(d)),e(f))"));
        }
        
        public void fxx() {
            Term fxx = Term.parse("f(X,X)");
            Substitution subst = Term.parse("g(b,c(d))").substFor("X");
            
            assert subst.apply(fxx) != fxx;
            assert subst.apply(fxx) != null;
            assert subst.apply(fxx).equals(Term.parse("f(g(b,c(d)),g(b,c(d)))"));
            
        }
        
        
        /** addSubstitution **/
        public void addsubst_differentVariables() {
            Substitution subst = new Substitution(x, Term.parse("a"));
            subst.addSubstitution(y, Term.parse("b"));
            
            assert subst.isValid();
            assert subst.subst.size() == 2;
            assert subst.subst.get(x).equals(Term.parse("a")) : "X is " + subst.subst.get(x);
            assert subst.subst.get(y).equals(Term.parse("b")) : "Y is " + subst.subst.get(y);
        }
        
        public void addsubst_substituteOldSubstitution() {
            Substitution subst = new Substitution(new Variable("X"), Term.parse("f(Y)"));
            subst.addSubstitution(new Variable("Y"), Term.parse("g(a)"));
            
            assert subst.isValid();
            assert subst.subst.size() == 2;
            assert subst.subst.get(x).equals(Term.parse("f(g(a))")) : "X is " + subst.subst.get(x);
            assert subst.subst.get(y).equals(Term.parse("g(a)")) : "Y is " + subst.subst.get(y);
        }
        
        public void addsubst_unifiableDuplicateEntries() {
            Substitution subst = new Substitution(new Variable("X"), Term.parse("f(Y,b)"));
            subst.addSubstitution(new Variable("X"), Term.parse("f(a,Z)"));
            
            assert subst.isValid();
            
            assert subst.subst.size() == 3 : "size is " + subst.subst.size();
            assert subst.subst.get(x).equals(Term.parse("f(a,b)")) : "X is " + subst.subst.get(x);
            assert subst.subst.get(y).equals(Term.parse("a")) : "Y is " + subst.subst.get(x);
            assert subst.subst.get(z).equals(Term.parse("b")) : "Z is " + subst.subst.get(x);
        }
        
        public void addsubst_nonUnifiableDuplicateEntries() {
            Substitution subst = new Substitution(new Variable("X"), Term.parse("f(Y,b)"));
            subst.addSubstitution(new Variable("X"), Term.parse("g(a,Z)"));
            
            assert !subst.isValid() : "subst is valid: " + subst;
        }
        
        public void addsubst_nonUnifiableDuplicateEntries2() {
            Substitution subst = new Substitution(new Variable("X"), Term.parse("f(Y,b)"));
            subst.addSubstitution(new Variable("Y"), Term.parse("g(a,X)"));
            
            assert !subst.isValid() : "substitution is " + subst;
        }
        
              
        
        /** concatenate **/
        public void concat() {
            Substitution subst1 = new Substitution(new Variable("X"), Term.parse("f(Y)"));
            subst1.addSubstitution(new Variable("Z"), Term.parse("g(Y,Y)"));
            
            Substitution subst2 = new Substitution(new Variable("Y"), Term.parse("h(a)"));
            subst2.addSubstitution(new Variable("W"), Term.parse("k(X,Z)"));
            
            Substitution concat = subst1.concatenate(subst2);

            assert concat != null;
            assert concat != subst1;
            assert concat != subst2;
            
            Substitution target = new Substitution();
            target.addSubstitution(new Variable("X"), Term.parse("f(h(a))"));
            target.addSubstitution(new Variable("Y"), Term.parse("h(a)"));
            target.addSubstitution(new Variable("Z"), Term.parse("g(h(a),h(a))"));
            target.addSubstitution(new Variable("W"), Term.parse("k(f(h(a)),g(h(a),h(a)))"));
            
            assert concat.equals(target);
            
            assert concat.equals(subst2.concatenate(subst1));
        }
        
        public void concatInvalid() {
            Substitution subst1 = new Substitution(new Variable("X"), Term.parse("f(Y)"));
            
            Substitution subst2 = new Substitution(new Variable("Y"), Term.parse("a"));
            subst2.addSubstitution(new Variable("Y"), Term.parse("b")); // invalid
            
            assert !subst1.concatenate(subst2).isValid();
            assert !subst2.concatenate(subst1).isValid();
        }
        
    }

}