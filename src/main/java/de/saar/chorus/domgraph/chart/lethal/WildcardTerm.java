/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.saar.chorus.domgraph.chart.lethal;

import de.saar.chorus.term.Substitution;
import de.saar.chorus.term.Term;
import de.saar.chorus.term.Type;
import de.saar.chorus.term.Variable;
import java.util.Set;

/**
 *
 * @author koller
 */
public class WildcardTerm extends Term {
    private Term sub;

    public WildcardTerm(Term sub) {
        this.sub = sub;
    }

    public Term getSubterm() {
        return sub;
    }



    @Override
    public Type getType() {
        return null;
    }

    @Override
    public boolean hasSubterm(Term term) {
        return sub.hasSubterm(term);
    }

    @Override
    public Substitution getUnifier(Term term) {
        throw new UnsupportedOperationException("Unification is not defined on wildcards.");
    }

    @Override
    protected boolean buildMatchingSubstitution(Term term, Substitution sbstn) {
        throw new UnsupportedOperationException("Unification is not defined on wildcards.");
    }

    @Override
    public Set<Variable> getVariables() {
        return sub.getVariables();
    }

    @Override
    public String toLispString() {
        return "(* " + sub.toLispString() + ")";
    }

    public String toString() {
        return "*[" + sub.toString() + "]";
    }

}
