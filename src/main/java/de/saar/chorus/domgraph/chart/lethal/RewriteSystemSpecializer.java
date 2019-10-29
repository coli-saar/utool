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
import de.uni_muenster.cs.sev.lethal.symbol.standard.StdNamedRankedSymbol;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
    private Set<String> allNodeLabels;

    public RewriteSystemSpecializer(DomGraph graph, NodeLabels labels, Annotator annotator) {
        symbolNodes = ArrayListMultimap.create();
        symbolArities = new HashMap<String, Integer>();
        labelToRankedSymbol = ArrayListMultimap.create();
        collectSymbols(graph, labels);
        allAnnotations = annotator.getAllAnnotations();

        allNodeLabels = new HashSet<String>();
        for (String node : graph.getAllNodes()) {
            allNodeLabels.add(labels.getLabel(node));
        }
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

    public RewriteSystem specialize(RewriteSystem trs) {
        return specialize(trs, new DummyComparator());
    }

    public RewriteSystem specialize(RewriteSystem trs, Comparator<Term> termOrder) {
        int countApplicableRules = 0;
        Stopwatch stopwatch = new Stopwatch();
        stopwatch.start("specialize");

        RewriteSystem specialized = new RewriteSystem(true);
//        SetMultimap<String, String> usedNodesForLabels = HashMultimap.create();

        for (Rule unspecializedRule : trs.getAllRules()) {
            if (isApplicableToDomgraph(unspecializedRule)) {
                countApplicableRules++;
                List<Rule> wildcardEliminatedRules = specializeWildcards(unspecializedRule);

                for (Rule rule : wildcardEliminatedRules) {
                    Map<String, String> indicesWithLabels = new HashMap<String, String>();
                    CompoundWithIndex.collectAllIndices(rule.lhs, indicesWithLabels);

                    List<String> sortedIndices = new ArrayList<String>(indicesWithLabels.keySet());
                    Collections.sort(sortedIndices);

                    List<List<String>> matchingNodeNames = new ArrayList<List<String>>();
                    for (String index : sortedIndices) {
                        matchingNodeNames.add(symbolNodes.get(indicesWithLabels.get(index)));
                    }

                    Iterator<List<String>> it = new CartesianIterator<String>(matchingNodeNames);

                    while (it.hasNext()) {
                        List<String> nodenames = it.next();

                        if (alldifferent(nodenames)) {
                            Map<String, String> indexToNodename = new HashMap<String, String>();
                            for (int i = 0; i < sortedIndices.size(); i++) {
                                indexToNodename.put(sortedIndices.get(i), nodenames.get(i));
                            }

                            Term lhs = specialize(rule.lhs, indexToNodename);
                            Term rhs = specialize(rule.rhs, indexToNodename);

                            if (termOrder.compare(lhs, rhs) >= 0) {
                                addRule(specialized, lhs, rhs, rule.annotation);
                            } else {
                                addRule(specialized, rhs, lhs, rule.annotation);
                            }

                        }
                    }
                }
            }
        }

        stopwatch.report("specialize", countApplicableRules + " of " + trs.getAllRules().size() + " applicable");

        return specialized;
    }

    private Term specialize(Term term, Map<String, String> indexToNodename) {
        if (term instanceof Constant || term instanceof Variable) {
            return term;
        } else if (term instanceof CompoundWithIndex) {
            CompoundWithIndex c = (CompoundWithIndex) term;
            List<Term> subSpecialized = new ArrayList<Term>();

            for (Term sub : c.getSubterms()) {
                subSpecialized.add(specialize(sub, indexToNodename));
            }

            return new Compound(indexToNodename.get(c.getIndex()), subSpecialized);
        } else {
            // wildcard or compound
            throw new UnsupportedOperationException("Trying to specialize subterm " + term + " of illegal type");
        }
    }

    private static boolean alldifferent(Collection<String> values) {
        Set<String> x = new HashSet<String>(values);

        return values.size() == x.size();
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
        } else if (term instanceof CompoundWithIndex) {
            List<Term> newSub = new ArrayList<Term>();

            for (Term sub : ((CompoundWithIndex) term).getSubterms()) {
                newSub.add(specializeWildcards(sub, f, arity, wildcardChildPos, variables));
            }

            return new CompoundWithIndex(((Compound) term).getLabel(), newSub, ((CompoundWithIndex) term).getIndex());
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

            return new CompoundWithIndex(f, subterms, "_w");
        } else {
            throw new UnsupportedOperationException("Not yet implemented");
        }
    }

    private boolean isApplicableToDomgraph(Rule unspecializedRule) {
        return allNodeLabels.containsAll(unspecializedRule.getAllLabels());
    }
}
