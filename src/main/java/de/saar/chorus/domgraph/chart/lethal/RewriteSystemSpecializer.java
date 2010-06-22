/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.chorus.domgraph.chart.lethal;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import de.saar.basic.CartesianIterator;
import de.saar.chorus.domgraph.chart.lethal.RewriteSystem.Rule;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.EdgeType;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.term.Compound;
import de.saar.chorus.term.Constant;
import de.saar.chorus.term.Term;
import de.saar.chorus.term.Variable;
import de.uni_muenster.cs.sev.lethal.symbol.common.RankedSymbol;
import de.uni_muenster.cs.sev.lethal.symbol.standard.StdNamedRankedSymbol;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 *
 * @author koller
 */
public class RewriteSystemSpecializer {
    private DomGraph graph;
    private NodeLabels labels;
    private ListMultimap<String, String> symbolNodes;
    private ListMultimap<String, RankedSymbol> labelToRankedSymbol;

    public RewriteSystemSpecializer(DomGraph graph, NodeLabels labels) {
        this.graph = graph;
        this.labels = labels;

        symbolNodes = ArrayListMultimap.create();
        labelToRankedSymbol = ArrayListMultimap.create();
        collectSymbols(graph, labels);
    }

    private void collectSymbols(DomGraph graph, NodeLabels labels) {
//        symbolArities.clear();
        symbolNodes.clear();

        for (String node : graph.getAllNodes()) {
            String label = labels.getLabel(node);

            if (label != null) {
//                symbolArities.put(label, graph.outdeg(node, EdgeType.TREE));
                symbolNodes.put(label, label + "_" + node);
                labelToRankedSymbol.put(label, new StdNamedRankedSymbol(label + "_" + node, graph.outdeg(node, EdgeType.TREE)));
            }
        }
    }

    public List<String> getSpecializedLabels(String label) {
        return symbolNodes.get(label);
    }

    public List<RankedSymbol> getSpecializedRankedSymbols(String label) {
        return labelToRankedSymbol.get(label);
    }

    public Collection<String> getAllLabels() {
        return symbolNodes.keySet();
    }

    public RewriteSystem specialize(RewriteSystem trs, Comparator<Term> termOrder) {
        RewriteSystem specialized = new RewriteSystem(true);

        for (Rule rule : trs.getAllRules()) {
            List<Term> lhss = specialize(rule.lhs);
            List<Term> rhss = specialize(rule.rhs);

            for (Term lhs : lhss) {
                for (Term rhs : rhss) {
                    if (termOrder.compare(lhs, rhs) <= 0) {
                        specialized.addRule(lhs, rhs, rule.annotation);
                    } else {
                        specialized.addRule(rhs, lhs, rule.annotation);
                    }
                }
            }
        }

        return specialized;
    }

    public RewriteSystem specialize(RewriteSystem trs) {
        if( trs.isOrdered() ) {
            return specialize(trs, new DummyComparator());
        } else {
            throw new UnsupportedOperationException("Trying to specialize an unordered rewrite system without an explicit term ordering.");
        }
    }

    private static class DummyComparator implements Comparator<Term> {
        public int compare(Term o1, Term o2) {
            return 0;
        }
    }

    private List<Term> specialize(Term lhs) {
        List<Term> ret = new ArrayList<Term>();

        if (lhs instanceof Variable) {
            ret.add(lhs);
            return ret;
        } else if (lhs instanceof Constant) {
            for( String s : symbolNodes.get(((Constant)lhs).getName()) ) {
                ret.add(new Constant(s));
            }
            return ret;
        } else if( lhs instanceof Compound) {
            Compound c = (Compound) lhs;
            List<List<Term>> specializedSubterms = new ArrayList<List<Term>>();

            for( Term sub : c.getSubterms() ) {
                specializedSubterms.add(specialize(sub));
            }

            for( String s : symbolNodes.get(c.getLabel())) {
                CartesianIterator<Term> it = new CartesianIterator<Term>(specializedSubterms);

                while(it.hasNext()) {
                    List<Term> subterms = it.next();
                    ret.add(new Compound(s, subterms));
                }
            }

            return ret;
        } else if( lhs instanceof WildcardTerm ) {
            throw new UnsupportedOperationException("noch nicht implementiert");
        } else {
            return null;
        }
    }
}
