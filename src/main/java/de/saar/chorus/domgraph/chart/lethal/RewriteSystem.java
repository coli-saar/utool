/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.saar.chorus.domgraph.chart.lethal;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author koller
 */
public class RewriteSystem {
    private List<Rule> rules;

    public RewriteSystem() {
        rules = new ArrayList<Rule>();
    }

    public void addRule(String f1, int n1, String f2, int n2, String annotation) {
        rules.add(new Rule(f1,n1,f2,n2,annotation));
    }

    public List<Rule> getAllRules() {
        return rules;
    }


    @Override
    public String toString() {
        return rules.toString();
    }


    public static class Rule {
        public String f1, f2;
        public int n1, n2;
        public String annotation;

        public Rule(String f1, int n1, String f2, int n2, String annotation) {
            this.f1 = f1;
            this.f2 = f2;
            this.n1 = n1;
            this.n2 = n2;
            this.annotation = annotation;
        }

        @Override
        public String toString() {
            return f1 + "/" + n1 + " > " + f2 + "/" + n2 + " [" + annotation + "]";
        }


    }
}
