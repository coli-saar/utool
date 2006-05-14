/*
 * @(#)Rule.java created 13.05.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.term.rule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.testng.annotations.Test;

import de.saar.chorus.term.Constant;
import de.saar.chorus.term.Substitution;
import de.saar.chorus.term.Term;
import de.saar.chorus.term.Variable;


/*
 * TODO:
 *  - move apply method into class Match
 */



public class Rule {
    private static int nextVariableIndex = 1;
    
    private List<Term> lhs, rhs;
    private Set<Variable> variables;
    
    public Rule(List<Term> lhs, List<Term> rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
        
        variables = new HashSet<Variable>();
        for( Term term : lhs ) {
            variables.addAll(term.getVariables());
        }
        for( Term term : rhs ) {
            variables.addAll(term.getVariables());
        }
    }
    
    public Rule(Term[] lhs, Term[] rhs) {
        this(Arrays.asList(lhs), Arrays.asList(rhs));
    }
    

    public List<Match> match(List<Term> termlist) {
        List<Match> matches = new ArrayList<Match>();
        Substitution subst = getNewRuleInstance();
        
        if( lhs.size() <= termlist.size() ) {
            collectMatches(new Match(this, subst, termlist), termlist, subst.apply(lhs), 0, matches);
        }

        return matches;
    }
    
    
    public List<Term> getLhs() {
        return lhs;
    }

    public List<Term> getRhs() {
        return rhs;
    }

    private void collectMatches(Match match, List<Term> termlist, List<Term> patterns, int patternidx, List<Match> matches) {
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
    
    
    private static Variable getNewVariable() {
        return new Variable("_R" + (nextVariableIndex++));
    }
    
    private Substitution getNewRuleInstance() {
        Substitution ret = new Substitution();
        
        for( Variable var : variables ) {
            ret.addSubstitution(var, getNewVariable());
        }
        
        return ret;
    }
    
    

    
    /***************************************************************
     * UNIT TESTS
     ***************************************************************/

    @Test(groups = { "Term" })
    public static class UnitTests {
        /** data set 1 **/
        
        private Term t1 = Term.parse("subst(n,R,rabbit(adj_n(T)))");
        private Term t2 = Term.parse("uniq(R,A)");
        
        private Term t3 = Term.parse("f(R,a,T)");
        private Term t4 = Term.parse("g(A,R)");
        private Term t5 = Term.parse("h(R,X)");
        
        private Term t1match = Term.parse("subst(n,r,rabbit(adj_n(t)))");
        private Term t2match = Term.parse("uniq(r,a)");
        private Term t3match = Term.parse("f(r,a,t)");
        private Term t4match = Term.parse("g(a,r)");
        private Term t5match = Term.parse("h(r,X)");
        
        
        private Rule r1 = new Rule(new Term[] { t1, t2 }, new Term[] { t3, t4 } );
        private Rule r1b = new Rule(new Term[] { t1, t2 }, new Term[] { t3 } );
        private Rule r1c = new Rule(new Term[] { t1, t2 }, new Term[] { t3, t4, t5 } );
        
        private List<Term> termlistRabbits = Arrays.asList(new Term[] {
                Term.parse("subst(n,r,rabbit(adj_n(t)))"),
                Term.parse("uniq(r,a)")
             });

        
        
        /** data set 2 **/
        
        Term f = Term.parse("f(X,Y)");
        Term g = Term.parse("g(Y,Z)");
        Term h = Term.parse("h(X,Z)");
        Term k = Term.parse("k(X,W)");
        Term l = Term.parse("l(W)");

        Term fab = Term.parse("f(a,b)");
        Term fcd = Term.parse("f(c,d)");
        Term glf = Term.parse("g(l,f)");
        Term gbf = Term.parse("g(b,f)");
        Term fkl = Term.parse("f(k,l)");
        Term gda = Term.parse("g(d,a)");
        Term gea = Term.parse("g(e,a)");

        Rule r2 = new Rule( new Term[] { f, g }, new Term[] { h } );
        Rule r2b = new Rule( new Term[] { f, g }, new Term[] { k, l } );
        
        List<Term> termlistForR2 = Arrays.asList(new Term[] {
                fab, fcd, glf, gbf, fkl, gda, gea
        });
        
        
        
        
        
        /** matching **/
        
        public void shortlist() {
            assert r1.match(new ArrayList<Term>()).isEmpty();
        }

        public void fail() {
            List<Term> termlistRabbitsVariant = Arrays.asList(new Term[] { 
                Term.parse("subst(n,r,rabbit(adj_n(t)))"),
                Term.parse("uniq(s,a)")
            });
            
            assert r1.match(termlistRabbitsVariant).isEmpty();
        }

        
        public void ok() {
            List<Match> matches = r1.match(termlistRabbits);
            
            assert matches.size() == 1 : "size";
            
            assert matches.get(0).getRuleSubstitution() != null : "null substitution";
            Substitution termsubst = matches.get(0).getRuleSubstitution(); 
            
            Term t1s = termsubst.apply(t1);
            Term t2s = termsubst.apply(t2);
            
            
            assert matches.get(0).getTermPosition(t1s) == 0;
            assert matches.get(0).getTermPosition(t2s) == 1;
            
            Substitution target = new Substitution();
            target.addSubstitution((Variable) termsubst.apply(new Variable("R")), new Constant("r"));
            target.addSubstitution((Variable) termsubst.apply(new Variable("A")), new Constant("a"));
            target.addSubstitution((Variable) termsubst.apply(new Variable("T")), new Constant("t"));
            
            assert target.equals(matches.get(0).getUnifier());
        }
        
        public void matchedPositions() {
            List<Match> matches = r1.match(termlistRabbits);
            
            assert matches.get(0).getMatchedPositions().equals(new HashSet<Integer>(Arrays.asList(new Integer[] { 0, 1 })));
        }
        
        public void multi() {
            List<Match> matches = r2.match(termlistForR2);
            
            assert matches != null : "null";
            assert matches.size() == 3 : "size";
            
            assert matches.get(0).getRuleSubstitution() != null : "null substitution";
            
            Term fs = matches.get(0).getRuleSubstitution().apply(f);
            Term gs = matches.get(0).getRuleSubstitution().apply(g);
            
            assert matches.get(0).getTermPosition(fs) == 0 : "0/f"; 
            assert matches.get(0).getTermPosition(gs) == 3 : "0/g";
            assert matches.get(1).getTermPosition(fs) == 1 : "1/f"; 
            assert matches.get(1).getTermPosition(gs) == 5 : "1/g";
            assert matches.get(2).getTermPosition(fs) == 4 : "2/f"; 
            assert matches.get(2).getTermPosition(gs) == 2 : "2/g";
        }
        
        
        public void matchWithVariables() {
            List<Term> termlist = Arrays.asList(new Term[] {
               Term.parse("f(X)"),
               Term.parse("g(X)")
            });
            
            Rule r = new Rule(new Term[] { Term.parse("f(a)") },
                    new Term[] { Term.parse("h(b)")
            });
            
            List<Match> matches = r.match(termlist);
            
            assert matches != null : "null";
            assert matches.size() == 1 : "size";
            
            Match match = matches.get(0);
            Substitution subst = match.getUnifier();
            
            assert subst.apply(new Variable("X")).equals(new Constant("a")) : "subst";
            assert subst.apply(match.getRuleSubstitution().apply(new Variable("X"))).equals(new Constant("a")) : "subst2";
        }
        
        
        /** rule instances **/

        public void ruleInstanceNotNull() {
            assert r1.getNewRuleInstance() != null; 
        }
        
        public void ruleInstanceNotOriginal() {
            assert ! r1.getNewRuleInstance().apply(t1).equals(t1);
        }
        
        public void ruleInstanceUnifiable() {
            assert r1.getNewRuleInstance().apply(t1).getUnifier(t1) != null;
            assert r1.getNewRuleInstance().apply(t1).getUnifier(t1).isValid();
        }
        
        public void ruleInstancesDifferent() {
            assert !r1.getNewRuleInstance().equals(r1.getNewRuleInstance());
        }
    
        
        

        
        /** application **/
        
        public void applyBasics() {
            List<Term> copy = new ArrayList<Term>(termlistRabbits);
            List<Match> matches = r1.match(termlistRabbits);
            List<Term> result = matches.get(0).apply();
            
            assert result != null : "is null";
            assert result != termlistRabbits : "is equal";
            assert termlistRabbits.equals(copy) : "argument changed";
        }

        public void apply1() {
            List<Match> matches = r1.match(termlistRabbits);
            List<Term> result = matches.get(0).apply();
            
            List<Term> intended = Arrays.asList(new Term[] { t3match, t4match });
            assert intended.equals(result) : "result list is " + result;
        }
        
        public void applyShrinking() {
            List<Match> matches = r1b.match(termlistRabbits);
            List<Term> result = matches.get(0).apply();
            
            List<Term> intended = Arrays.asList(new Term[] { t3match});
            assert intended.equals(result);
        }
        
        public void applyFreeVar() {
            List<Match> matches = r1c.match(termlistRabbits);
            Match match = matches.get(0);
            List<Term> result = match.apply();
            
            assert result.size() == 3;
            assert result.get(0).equals(t3match);
            assert result.get(1).equals(t4match);
            
            assert !result.get(2).equals(t5match);
            assert result.get(2).equals(match.getRuleSubstitution().apply(t5match));
        }
        
        public void applyWithRest() {
            List<Match> matches = r2.match(termlistForR2);
            List<Term> result = matches.get(0).apply();
            
            assert result.equals(Arrays.asList(new Term[] {
                    fcd, glf, fkl, gda, gea, Term.parse("h(a,f)")    
            }));
        }
        
        public void applyTwice() {
            Rule secondRule = new Rule(
                    new Term[] { 
                            Term.parse("g(X,Y)"),
                            Term.parse("k(Y,X)")
                    },
                    new Term[] {
                            Term.parse("m(X)")
                    });
            
            
            List<Match> matches = r2b.match(termlistForR2);
            List<Term> result1 = matches.get(0).apply();
            //  fcd, glf, fkl, gda, gea, k(a,_R1), l(_R1)
            
            List<Match> matches2 = secondRule.match(result1);
            assert !matches2.isEmpty() : "matches2 is empty";
            
            List<Term> result = matches2.get(0).apply();
            assert result.equals(Arrays.asList(new Term[] {
                fcd, glf, fkl, gea, Term.parse("l(d)"), Term.parse("m(d)")
            })) : "wrong result: " + result;
        }
        

        
        public void applyWithVariables() {
            List<Term> termlist = Arrays.asList(new Term[] {
               Term.parse("f(X)"),
               Term.parse("g(X)")
            });
            
            Rule r = new Rule(new Term[] { Term.parse("f(a)") },
                    new Term[] { Term.parse("h(b)")
            });
            
            List<Match> matches = r.match(termlist);
            List<Term> result = matches.get(0).apply();
            
            assert result.equals(Arrays.asList(new Term[] { 
                Term.parse("g(a)"), Term.parse("h(b)")    
            }));
        }
        

        public void applyWithVariables2() {
            List<Term> termlist = Arrays.asList(new Term[] {
               Term.parse("f(X)"),
               Term.parse("sync(a)"),
               Term.parse("g(X)")
            });
            
            Rule r = new Rule(new Term[] { Term.parse("f(Z)"), Term.parse("sync(Z)") },
                    new Term[] { Term.parse("h(b)")
            });
            
            List<Match> matches = r.match(termlist);
            List<Term> result = matches.get(0).apply();
            
            assert result.equals(Arrays.asList(new Term[] { 
                Term.parse("g(a)"), Term.parse("h(b)")    
            }));
        }
    }
}
