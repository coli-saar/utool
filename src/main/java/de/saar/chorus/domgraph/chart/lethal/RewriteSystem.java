/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.saar.chorus.domgraph.chart.lethal;

import de.saar.chorus.term.Compound;
import de.saar.chorus.term.Constant;
import de.saar.chorus.term.Term;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    public String toPrettyString() {
        StringBuffer buf = new StringBuffer();

        for( Rule rule : rules ) {
            buf.append(rule.toString() + "\n");
        }

        return buf.toString();
    }

    public static class Rule {
        public Term lhs, rhs;
        public String annotation;
        public boolean oriented;
        private Set<String> labels;

        public Rule(Term lhs, Term rhs, String annotation, boolean oriented) {
            this.lhs = lhs;
            this.rhs = rhs;
            this.annotation = annotation;
            this.oriented = oriented;

            labels = new HashSet<String>();
            collectLabels(lhs);
            collectLabels(rhs);
        }


        @Override
        public String toString() {
            return "[" + annotation + "] " + lhs + (oriented?" -> ":" = ") + rhs;
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

        public Set<String> getAllLabels() {
            return labels;
        }

        private void collectLabels(Term term) {
            if( term instanceof Constant ) {
                labels.add(((Constant) term).getName());
            } else if( term instanceof Compound ) { // or CompoundWithIndex
                Compound c = (Compound) term;
                labels.add(c.getLabel());
                for( Term t : c.getSubterms() ) {
                    collectLabels(t);
                }
            }
        }
        
    }
}
