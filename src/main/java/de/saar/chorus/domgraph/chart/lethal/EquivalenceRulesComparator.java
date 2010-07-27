/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.chorus.domgraph.chart.lethal;

import de.saar.basic.Pair;
import de.saar.chorus.term.Compound;
import de.saar.chorus.term.Constant;
import de.saar.chorus.term.Term;
import java.util.Comparator;
import java.util.HashSet;
import java.util.ListIterator;
import java.util.Set;
import java.util.Stack;

/**
 *
 * @author venant
 */
public class EquivalenceRulesComparator implements Comparator<Term> {

    private Set<String> pronounsLabels;

    public EquivalenceRulesComparator() {
        this(new HashSet<String>());
    }

    public EquivalenceRulesComparator(Set<String> pronounsLabels) {
        this.pronounsLabels = pronounsLabels;
    }

    private int compareLabels(String label1, String label2) {
        if (this.pronounsLabels.contains(label1) & !this.pronounsLabels.contains(label2)) {
            return -1;
        }
        if (this.pronounsLabels.contains(label2) & !this.pronounsLabels.contains(label1)) {
            return 1;
        }
        return label1.compareTo(label2);
    }

    public int compare(Term o1, Term o2) {
        Stack<Pair<Term, Term>> todo = new Stack<Pair<Term, Term>>();
        Pair<Term, Term> current;
        Term t1;
        Term t2;
        ListIterator<Term> iterator1;
        ListIterator<Term> iterator2;
        int comp;
        todo.push(new Pair<Term, Term>(o1, o2));
        while (!todo.empty()) {
            current = todo.pop();
            t1 = current.left;
            t2 = current.right;
            switch (t1.getType()) {
                case VARIABLE:
                    if(!t2.isVariable())return -1;else continue;
                case CONSTANT:
                    switch (t2.getType()) {
                        case VARIABLE:
                            return 1;
                        case CONSTANT:
                            return this.compareLabels(((Constant) t1).getName(), ((Constant) t2).getName());
                        case COMPOUND:
                            comp = this.compareLabels(((Constant) t1).getName(), ((Compound) t2).getLabel());
                            return (comp == 0) ? -1 : comp;
                    }
                case COMPOUND:
                    switch (t2.getType()) {
                        case VARIABLE:
                            return 1;
                        case CONSTANT:
                            comp = this.compareLabels(((Compound) t1).getLabel(), ((Constant) t2).getName());
                            return (comp == 0) ? 1 : comp;
                        case COMPOUND:
                            comp = this.compareLabels(((Compound) t1).getLabel(), ((Compound) t2).getLabel());
                            if (comp != 0) {
                                return comp;
                            } else {
                                iterator1 = ((Compound) t1).getSubterms().listIterator();
                                iterator2 = ((Compound) t2).getSubterms().listIterator();
                                while (iterator1.hasNext() && iterator2.hasNext()) {
                                    todo.push(new Pair<Term, Term>(iterator1.next(), iterator2.next()));
                                }
                                if (iterator1.hasNext()) {
                                    return 1;
                                }
                                if (iterator2.hasNext()) {
                                    return -1;
                                }
                            }
                    }
            }
        }
        return 0;
    }
}
