/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.chorus.domgraph.chart.lethal;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.SetMultimap;
import de.saar.basic.CartesianIterator;
import de.saar.chorus.domgraph.chart.lethal.RewriteSystem.Rule;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.EdgeType;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.term.Compound;
import de.saar.chorus.term.Constant;
import de.saar.chorus.term.Term;
import de.saar.chorus.term.Variable;
import de.uni_muenster.cs.sev.lethal.symbol.standard.StdNamedRankedSymbol;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author koller
 */
public class RewriteSystemSpecializer {

    private ListMultimap<String, String> symbolNodes;
    private Map<String, Integer> symbolArities;
    private ListMultimap<String, StdNamedRankedSymbol> labelToRankedSymbol;
    private Set<String> allAnnotations;

    public RewriteSystemSpecializer(DomGraph graph, NodeLabels labels, Annotator annotator) {
        symbolNodes = ArrayListMultimap.create();
        symbolArities = new HashMap<String, Integer>();
        labelToRankedSymbol = ArrayListMultimap.create();
        collectSymbols(graph, labels);
        allAnnotations = annotator.getAllAnnotations();
    }

    private void collectSymbols(DomGraph graph, NodeLabels labels) {
        symbolArities.clear();
        symbolNodes.clear();

        for (String node : graph.getAllNodes()) {
            String label = labels.getLabel(node);

            if (label != null) {
                String symbol = label + "_" + node;
                int arity = graph.outdeg(node, EdgeType.TREE);

                symbolArities.put(label, arity);
                symbolNodes.put(label, symbol);
                labelToRankedSymbol.put(label, new StdNamedRankedSymbol(symbol, arity));
            }
        }
    }

    public List<String> getSpecializedLabels(String label) {
        return symbolNodes.get(label);
    }

    public List<StdNamedRankedSymbol> getSpecializedRankedSymbols(String label) {
        return labelToRankedSymbol.get(label);
    }

    public Collection<String> getAllLabels() {
        return symbolNodes.keySet();
    }

    public RewriteSystem specialize(RewriteSystem trs, Comparator<Term> termOrder) {
        RewriteSystem specialized = new RewriteSystem(true);
        SetMultimap<String, String> usedNodesForLabels = HashMultimap.create();

        for (Rule unspecializedRule : trs.getAllRules()) {
            List<Rule> wildcardEliminatedRules = specializeWildcards(unspecializedRule);

            for (Rule rule : wildcardEliminatedRules) {
                usedNodesForLabels.clear();
                List<Term> lhss = specialize(rule.lhs, usedNodesForLabels);
                usedNodesForLabels.clear();
                List<Term> rhss = specialize(rule.rhs, usedNodesForLabels);

                for (Term lhs : lhss) {
                    for (Term rhs : rhss) {
                        if (!isEqualUpToVariables(lhs, rhs)) {
                            if (termOrder.compare(lhs, rhs) <= 0) {
                                addRule(specialized, lhs, rhs, rule.annotation);
                            } else {
                                addRule(specialized, rhs, lhs, rule.annotation);
                            }
                        }
                    }
                }
            }
        }

        return specialized;
    }

    private void addRule(RewriteSystem specialized, Term lhs, Term rhs, String annotation) {
        if (annotation != null) {
            specialized.addRule(lhs, rhs, annotation);
        } else {
            for (String ann : allAnnotations) {
                specialized.addRule(lhs, rhs, ann);
            }
        }


    }

    private boolean isEqualUpToVariables(Term lhs, Term rhs) {
        if (lhs.getClass() != rhs.getClass()) {
            return false;
        }

        if (lhs instanceof Variable) {
            return true;
        } else if (lhs instanceof Constant) {
            return ((Constant) lhs).getName().equals(((Constant) rhs).getName());
        } else if (lhs instanceof Compound) {
            Compound cl = (Compound) lhs;
            Compound cr = (Compound) rhs;

            if (!cl.getLabel().equals(cr.getLabel())) {
                return false;
            }

            if (cl.getSubterms().size() != cr.getSubterms().size()) {
                return false;
            }

            for (int i = 0; i < cl.getSubterms().size(); i++) {
                if (!isEqualUpToVariables(cl.getSubterms().get(i), cr.getSubterms().get(i))) {
                    return false;
                }
            }

            return true;
        } else {
            return false;
        }
    }

    public RewriteSystem specialize(RewriteSystem trs) {
        if (trs.isOrdered()) {
            return specialize(trs, new DummyComparator());
        } else {
            throw new UnsupportedOperationException("Trying to specialize an unordered rewrite system without an explicit term ordering.");
        }
    }

    private List<Rule> specializeWildcards(Rule rule) {
        List<Rule> ret = new ArrayList<Rule>();

        if (!containsWildcard(rule.lhs)) {
            ret.add(rule);
        } else {
            for (String f : labelToRankedSymbol.keySet()) {
                List<Variable> variables = new ArrayList<Variable>();
                int arity = symbolArities.get(f);

                for (int i = 0; i < arity; i++) {
                    variables.add(new Variable("WW" + (i + 1)));
                }

                for (int i = 0; i < arity; i++) {
                    Term lhs = specializeWildcards(rule.lhs, f, arity, i, variables);
                    Term rhs = specializeWildcards(rule.rhs, f, arity, i, variables);
                    ret.add(new Rule(lhs, rhs, rule.annotation, rule.oriented));
                }
            }
        }

        return ret;
    }

    private boolean containsWildcard(Term term) {
        if (term instanceof Variable) {
            return false;
        } else if (term instanceof Constant) {
            return false;
        } else if (term instanceof Compound) {
            for (Term sub : ((Compound) term).getSubterms()) {
                if (containsWildcard(sub)) {
                    return true;
                }
            }

            return false;
        } else if (term instanceof WildcardTerm) {
            return true;
        } else {
            return false;
        }
    }

    private Term specializeWildcards(Term term, String f, int arity, int wildcardChildPos, List<Variable> variables) {
        if (term instanceof Variable) {
            return term;
        } else if (term instanceof Constant) {
            return term;
        } else if (term instanceof Compound) {
            List<Term> newSub = new ArrayList<Term>();

            for (Term sub : ((Compound) term).getSubterms()) {
                newSub.add(specializeWildcards(sub, f, arity, wildcardChildPos, variables));
            }

            return new Compound(((Compound) term).getLabel(), newSub);
        } else if (term instanceof WildcardTerm) {
            // I assume that a term can contain only one wildcard, therefore my subterm needs no further processing
            Term sub = ((WildcardTerm) term).getSubterm();
            List<Term> subterms = new ArrayList<Term>();

            for (int i = 0; i < arity; i++) {
                if (i == wildcardChildPos) {
                    subterms.add(sub);
                } else {
                    subterms.add(variables.get(i));
                }
            }

            return new Compound(f, subterms);
        } else {
            throw new UnsupportedOperationException("Not yet implemented");
        }
    }

    private List<Term> specialize(Term term, SetMultimap<String, String> usedNodesForLabel) {
        List<Term> ret = new ArrayList<Term>();

        if (term instanceof Variable) {
            ret.add(term);
            return ret;
        } else if (term instanceof Constant) {
            String n = ((Constant) term).getName();
            for (String node : symbolNodes.get(n)) {
                if (!usedNodesForLabel.get(n).contains(node)) {
                    ret.add(new Constant(node));
                }
            }
            return ret;
        } else if (term instanceof Compound) {
            Compound c = (Compound) term;

            for (String node : symbolNodes.get(c.getLabel())) {
                if (!usedNodesForLabel.get(c.getLabel()).contains(node)) {
                    List<List<Term>> specializedSubterms = new ArrayList<List<Term>>();

                    usedNodesForLabel.put(c.getLabel(), node);

                    for (Term sub : c.getSubterms()) {
                        specializedSubterms.add(specialize(sub, usedNodesForLabel));
                    }

                    CartesianIterator<Term> it = new CartesianIterator<Term>(specializedSubterms);

                    while (it.hasNext()) {
                        List<Term> subterms = it.next();
                        ret.add(new Compound(node, subterms));
                    }

                    usedNodesForLabel.remove(c.getLabel(), node);
                }
            }

            return ret;
        } else {
            throw new UnsupportedOperationException("Trying to specialize unsupported term: " + term);
        }
    }
}
