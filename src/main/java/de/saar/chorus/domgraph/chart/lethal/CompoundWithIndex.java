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
import java.util.Set;

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
                throw new UnsupportedOperationException("Trying to assign index to term " + c + ", but constructor " + c.getLabel() + " was already assigned an index.");
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
        } else {
            throw new UnsupportedOperationException("trying to assign indices to term " + term + " of unknown type " + term.getClass());
        }
    }

    public static void collectAllIndices(Term term, Set<String> allIndices) {
        if( term instanceof CompoundWithIndex ) {
            CompoundWithIndex c = (CompoundWithIndex) term;
            List<Term> sub = new ArrayList<Term>();

            for( Term subterm : c.getSubterms() ) {
                collectAllIndices(subterm, allIndices);
            }

            allIndices.add(c.getIndex());
        } else if( term instanceof Compound ) {
            Compound c = (Compound) term;
            List<Term> sub = new ArrayList<Term>();

            for( Term subterm : c.getSubterms() ) {
                collectAllIndices(subterm, allIndices);
            }
        }
    }
}
