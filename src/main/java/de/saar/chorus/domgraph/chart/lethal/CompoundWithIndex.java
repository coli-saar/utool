/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.saar.chorus.domgraph.chart.lethal;

import de.saar.basic.StringTools;
import de.saar.chorus.term.Compound;
import de.saar.chorus.term.Constant;
import de.saar.chorus.term.Term;
import de.saar.chorus.term.Variable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author koller
 */
public class CompoundWithIndex extends Compound {
    private String index;
    private static int nextIndex;

    public CompoundWithIndex(String label, List<Term> subterms, String index) {
        super(label, subterms);
        this.index = index;
    }

    public String getIndex() {
        return index;
    }

    @Override
    public String toString() {
        return getLabel() + "#" + index + "(" + StringTools.join(getSubterms(), ",") + ")";
    }

    public static Term assignIndicesToTerm(Term term, Map<String,String> assignedIndices, Map<String,String> previouslyAssignedIndices) {
        nextIndex = 1;
        return _assignIndicesToTerm(term, assignedIndices, previouslyAssignedIndices);
    }

    private static Term _assignIndicesToTerm(Term term, Map<String,String> assignedIndices, Map<String,String> previouslyAssignedIndices) {
        if( term instanceof Constant ) {
            return term;
        } else if( term instanceof Variable ) {
            return term;
        } else if( term instanceof CompoundWithIndex ) {
            CompoundWithIndex c = (CompoundWithIndex) term;
            List<Term> sub = new ArrayList<Term>();

            for( Term subterm : c.getSubterms() ) {
                sub.add(_assignIndicesToTerm(subterm, assignedIndices, previouslyAssignedIndices));
            }

            return new CompoundWithIndex(c.getLabel(), sub, c.getIndex());
        } else if( term instanceof Compound ) {
            Compound c = (Compound) term;
            List<Term> sub = new ArrayList<Term>();
            String index = null;

            if( assignedIndices.containsKey(c.getLabel())) {
                throw new UnsupportedOperationException("Symbol " + c.getLabel() + " occurs twice without index. Please mark one with an explicit index.");
            } else if( previouslyAssignedIndices.containsKey(c.getLabel())) {
                index = previouslyAssignedIndices.get(c.getLabel());
            } else {
                index = "_i" + (nextIndex++);
            }

            assignedIndices.put(c.getLabel(), index);

            for( Term subterm : c.getSubterms() ) {
                sub.add(_assignIndicesToTerm(subterm, assignedIndices, previouslyAssignedIndices));
            }

            return new CompoundWithIndex(c.getLabel(), sub, index);
        } else if( term instanceof WildcardTerm ) {
            WildcardTerm w = (WildcardTerm) term;
            return new WildcardTerm(_assignIndicesToTerm(w.getSubterm(), assignedIndices, previouslyAssignedIndices));
        } else {
            throw new UnsupportedOperationException("trying to assign indices to term " + term + " of unknown type " + term.getClass());
        }
    }

    public static void collectAllIndices(Term term, Map<String,String> allIndices) {
        if( term instanceof CompoundWithIndex ) {
            CompoundWithIndex c = (CompoundWithIndex) term;
            List<Term> sub = new ArrayList<Term>();

            for( Term subterm : c.getSubterms() ) {
                collectAllIndices(subterm, allIndices);
            }

            allIndices.put(c.getIndex(), c.getLabel());
        } else if( term instanceof Compound ) {
            Compound c = (Compound) term;
            List<Term> sub = new ArrayList<Term>();

            for( Term subterm : c.getSubterms() ) {
                collectAllIndices(subterm, allIndices);
            }
        } else if( term instanceof WildcardTerm ) {
            collectAllIndices(((WildcardTerm) term).getSubterm(), allIndices);
        }
    }
}
