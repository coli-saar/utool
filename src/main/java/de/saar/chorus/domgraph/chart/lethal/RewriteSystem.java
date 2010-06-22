/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.saar.chorus.domgraph.chart.lethal;

import de.saar.chorus.term.Term;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author koller
 */
public class RewriteSystem {
    private List<Rule> rules;
    private boolean ordered;

    public RewriteSystem(boolean ordered) {
        rules = new ArrayList<Rule>();
        this.ordered = ordered;
    }

    public void addRule(Term lhs, Term rhs, String annotation) {
        rules.add(new Rule(lhs, rhs, annotation, ordered));
    }

    public List<Rule> getAllRules() {
        return rules;
    }


    @Override
    public String toString() {
        return rules.toString();
    }


    public static class Rule {
        public Term lhs, rhs;
        public String annotation;
        public boolean ordered;


        public Rule(Term lhs, Term rhs, String annotation, boolean ordered) {
            this.lhs = lhs;
            this.rhs = rhs;
            this.annotation = annotation;
            this.ordered = ordered;
        }


        @Override
        public String toString() {
            return lhs + (ordered?" -> ":" = ") + rhs + " [" + annotation + "]";
        }


    }
}
