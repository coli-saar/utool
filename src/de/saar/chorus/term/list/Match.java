/*
 * @(#)Match.java created 11.05.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.term.list;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.testng.annotations.Test;

import de.saar.chorus.term.Constant;
import de.saar.chorus.term.Substitution;
import de.saar.chorus.term.Term;
import de.saar.chorus.term.Variable;

public class Match implements Cloneable {
    private Map<Term,Integer> termPositions;
    private Substitution unifier;
    
    private List<Term> termStack;
    private List<Substitution> unifierStack;
    
    public Match() {
        termPositions = new HashMap<Term,Integer>();
        unifier = new Substitution();
        
        termStack = new ArrayList<Term>();
        unifierStack = new ArrayList<Substitution>();
    }

    public Set<Integer> getPositions() {
        return new HashSet<Integer>(termPositions.values());
    }
    
    public Substitution getUnifier() {
        return unifier;
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
        Match ret = new Match();
        
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
    
    public static List<Match> match(List<Term> termlist, List<Term> patterns) {
        List<Match> matches = new ArrayList<Match>();
        
        if( patterns.size() <= termlist.size() ) {
            collectMatches(new Match(), termlist, patterns, 0, matches);
        }

        return matches;
    }
    
    private static void collectMatches(Match match, List<Term> termlist, List<Term> patterns, int patternidx, List<Match> matches) {
        if( patternidx < patterns.size() ) {
            Term pattern = patterns.get(patternidx);
            Set<Integer> matchedPositions = match.getPositions();
            
            for( int i = 0; i < termlist.size(); i++ ) {
                if( !matchedPositions.contains(i) ) {
                    Substitution mgu = pattern.getUnifier(termlist.get(i));
                    
                    if( mgu != null ) {
                        if( match.isCompatible(mgu)) {
                            match.addTerm(pattern, i, mgu);
                            collectMatches(match, termlist, patterns, patternidx+1, matches);
                            match.removeMostRecentTerm();
                        }
                    }
                }
            }
        } else {
            matches.add((Match) match.clone());
        }
    }
    
    


    
    
    /***************************************************************
     * UNIT TESTS
     ***************************************************************/

    @Test(groups = { "Term" })
    public static class UnitTests {
        private Term t1 = Term.parse("subst(n,R,rabbit(adj_n(T)))");
        private Term t2 = Term.parse("uniq(R,A)");

        private List<Term> patterns = Arrays.asList(new Term[] { t1, t2 });
        
        
        public void shortlist() {
            assert match(new ArrayList<Term>(), patterns).isEmpty();
        }
        
        
        public void ok() {
            List<Term> termlist = Arrays.asList(new Term[] {
               Term.parse("subst(n,r,rabbit(adj_n(t)))"),
               Term.parse("uniq(r,a)")
            });
            
            List<Match> matches = match(termlist, patterns);
            
            assert matches.size() == 1;
            assert matches.get(0).termPositions.get(t1) == 0;
            assert matches.get(0).termPositions.get(t2) == 1;
            
            Substitution target = new Substitution();
            target.addSubstitution(new Variable("R"), new Constant("r"));
            target.addSubstitution(new Variable("A"), new Constant("a"));
            target.addSubstitution(new Variable("T"), new Constant("t"));
            
            assert target.equals(matches.get(0).unifier);
        }
        
        public void multi() {
            Term f = Term.parse("f(X,Y)");
            Term g = Term.parse("g(Y,Z)");
            List<Term> patterns = Arrays.asList(new Term[] {
                    f,
                    g
            });
            
            List<Term> termlist = Arrays.asList(new Term[] {
                    Term.parse("f(a,b)"),
                    Term.parse("f(c,d)"),
                    Term.parse("g(l,f)"),
                    Term.parse("g(b,f)"),
                    Term.parse("f(k,l)"),
                    Term.parse("g(d,a)"),
                    Term.parse("g(e,a)")
            });
            
            List<Match> matches = match(termlist, patterns);
            
            assert matches.size() == 3;
            
            assert matches.get(0).termPositions.get(f) == 0 : "0/f"; 
            assert matches.get(0).termPositions.get(g) == 3 : "0/g";
            assert matches.get(1).termPositions.get(f) == 1 : "1/f"; 
            assert matches.get(1).termPositions.get(g) == 5 : "1/g";
            assert matches.get(2).termPositions.get(f) == 4 : "2/f"; 
            assert matches.get(2).termPositions.get(g) == 2 : "2/g";
        }
        
        public void fail() {
            List<Term> termlist = Arrays.asList(new Term[] { 
                Term.parse("subst(n,r,rabbit(adj_n(t)))"),
                Term.parse("uniq(s,a)")
            });
            
            assert match(termlist, patterns).isEmpty();
        }
    }
}
