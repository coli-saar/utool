/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.saar.chorus.domgraph.chart.lethal;

import de.saar.basic.StringOrVariable;
import de.saar.basic.tree.Tree;
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

    public de.up.ling.tree.Tree<StringOrVariable> toTreeWithVariables() {
        // This method was introduced in basics after the last change to Utool,
        // and is never called from within Utool, so we can just use a dummy implementation.

        return null;
    }

    public String toString() {
        return "*[" + sub.toString() + "]";
    }

    @Override
    protected void buildTermWithVariables(Tree<StringOrVariable> tree, String parent) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected void buildTerm(Tree<String> tree, String parent) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
