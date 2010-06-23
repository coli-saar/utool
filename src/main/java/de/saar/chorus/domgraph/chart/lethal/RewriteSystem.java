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
    private boolean oriented;

    public RewriteSystem(boolean oriented) {
        rules = new ArrayList<Rule>();
        this.oriented = oriented;
    }

    public void addRule(Term lhs, Term rhs, String annotation) {
        rules.add(new Rule(lhs, rhs, annotation, oriented));
    }

    public List<Rule> getAllRules() {
        return rules;
    }

    public boolean isOrdered() {
        return oriented;
    }


    


    @Override
    public String toString() {
        return rules.toString();
    }


    public static class Rule {
        public Term lhs, rhs;
        public String annotation;
        public boolean oriented;


        public Rule(Term lhs, Term rhs, String annotation, boolean oriented) {
            this.lhs = lhs;
            this.rhs = rhs;
            this.annotation = annotation;
            this.oriented = oriented;
        }


        @Override
        public String toString() {
            return lhs + (oriented?" -> ":" = ") + rhs + " [" + annotation + "]";
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Rule other = (Rule) obj;
            if (this.lhs != other.lhs && (this.lhs == null || !this.lhs.equals(other.lhs))) {
                return false;
            }
            if (this.rhs != other.rhs && (this.rhs == null || !this.rhs.equals(other.rhs))) {
                return false;
            }
            if ((this.annotation == null) ? (other.annotation != null) : !this.annotation.equals(other.annotation)) {
                return false;
            }
            if (this.oriented != other.oriented) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            return hash;
        }


        
    }
}
