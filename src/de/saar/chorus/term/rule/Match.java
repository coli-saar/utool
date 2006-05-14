/*
 * @(#)Match.java created 11.05.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.term.rule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.saar.chorus.term.Substitution;
import de.saar.chorus.term.Term;

public class Match implements Cloneable {
    private Rule rule;
    private Substitution ruleSubstitution;
    private List<Term> termlist;
    
    private Map<Term,Integer> termPositions;
    private Substitution unifier;
    
    private List<Term> termStack;
    private List<Substitution> unifierStack;
    
    public Match(Rule rule, Substitution ruleSubstitution, List<Term> termlist) {
        termPositions = new HashMap<Term,Integer>();
        unifier = new Substitution();
        
        termStack = new ArrayList<Term>();
        unifierStack = new ArrayList<Substitution>();
        
        this.ruleSubstitution = ruleSubstitution;
        this.rule = rule;
        this.termlist = termlist;
    }
    
    

    public List<Term> apply() {
        List<Term> ret = new ArrayList<Term>(termlist.size());
        Set<Integer> matchedPositions = getMatchedPositions();
        
        // copy all remaining entries of the term list, applying
        // the unifier to them
        for( int i = 0; i < termlist.size(); i++ ) {
           if( !matchedPositions.contains(i) ) {
               ret.add(unifier.apply(termlist.get(i)));
           }
        }
                
        // add instances of the RHS terms
        for( Term term : ruleSubstitution.apply(rule.getRhs()) ) {
            ret.add(unifier.apply(term));
        }
        
        return ret;
    }
    

    public Set<Integer> getPositions() {
        return new HashSet<Integer>(termPositions.values());
    }
    
    public Substitution getUnifier() {
        return unifier;
    }
    

    
    
    
    /**
     * Returns the set of matched positions in the list.
     * 
     * @return
     */
    public Set<Integer> getMatchedPositions() {
        return new HashSet<Integer>(termPositions.values());
    }
    
    public int getTermPosition(Term term) {
        return termPositions.get(term);
    }
    
    void addTerm(Term term, int pos, Substitution unif) {
        termStack.add(term);
        unifierStack.add(unifier);
        
        termPositions.put(term, pos);
        unifier = unifier.concatenate(unif);
    }
    
    void removeMostRecentTerm() {
        Term term = pop(termStack);
        termPositions.remove(term);
        
        unifier = pop(unifierStack);
    }
    
    public boolean isCompatible(Substitution other) {
        return unifier.concatenate(other).isValid();
    }
    
    public String toString() {
        return termPositions + " (unifier = " + unifier + ")";
    }
    
    public Object clone() {
        Match ret = new Match(rule, ruleSubstitution, termlist);
        
        ret.unifier = (Substitution) unifier.clone();
        for (Substitution u : unifierStack) {
            ret.unifierStack.add((Substitution) u.clone());
        }
        
        ret.termPositions.putAll(termPositions);
        ret.termStack.addAll(termStack);
        
        return ret;
    }
    
    
    
    
    
    private static <E> E pop(List<E> list) {
        return list.remove(list.size()-1);
    }

    public Substitution getRuleSubstitution() {
        return ruleSubstitution;
    }
    
    


    }
