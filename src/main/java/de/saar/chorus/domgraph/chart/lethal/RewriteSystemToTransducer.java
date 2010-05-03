/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.chorus.domgraph.chart.lethal;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import de.saar.chorus.contexttransducer.ContextTreeTransducer;
import de.saar.chorus.domgraph.chart.lethal.RewriteSystem.Rule;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.EdgeType;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.uni_muenster.cs.sev.lethal.factories.TreeFactory;
import de.uni_muenster.cs.sev.lethal.states.State;
import de.uni_muenster.cs.sev.lethal.symbol.common.BiSymbol;
import de.uni_muenster.cs.sev.lethal.symbol.common.RankedSymbol;
import de.uni_muenster.cs.sev.lethal.symbol.common.Variable;
import de.uni_muenster.cs.sev.lethal.symbol.standard.InnerSymbol;
import de.uni_muenster.cs.sev.lethal.symbol.standard.LeafSymbol;
import de.uni_muenster.cs.sev.lethal.symbol.standard.NamedVariable;
import de.uni_muenster.cs.sev.lethal.symbol.standard.StdNamedRankedSymbol;
import de.uni_muenster.cs.sev.lethal.tree.common.Tree;
import de.uni_muenster.cs.sev.lethal.treeautomata.easy.EasyFTAOps.StdStateBuilder;
import de.uni_muenster.cs.sev.lethal.utils.Pair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author koller
 */
public class RewriteSystemToTransducer {

    private Map<String, Integer> symbolArities;
    private ListMultimap<String, String> symbolNodes;
    private Map<String, State> annotationStates;
    private RewriteSystem weakening, equivalence;
    private Annotator annotator;
    private State qbar;
    private static final StdStateBuilder<String> sb = new StdStateBuilder<String>();
    private static TreeFactory tf = TreeFactory.getTreeFactory();

    public RewriteSystemToTransducer(RewriteSystem weakening, RewriteSystem equivalence, Annotator annotator) {
        this.weakening = weakening;
        this.equivalence = equivalence;
        this.annotator = annotator;

        symbolArities = new HashMap<String, Integer>();
        symbolNodes = ArrayListMultimap.create();

        qbar = sb.convert("qbar");
        annotationStates = new HashMap<String, State>();
        for (String ann : annotator.getAllAnnotations()) {
            annotationStates.put(ann, sb.convert("q_" + ann));
        }
    }

    public ContextTreeTransducer<RankedSymbol, RankedSymbol, State> convert(DomGraph graph, NodeLabels labels) {
        ContextTreeTransducer<RankedSymbol, RankedSymbol, State> ret = new ContextTreeTransducer<RankedSymbol, RankedSymbol, State>();

        // prepare data structures
        collectSymbols(graph, labels);

        // set final state
        ret.addFinalState(annotationStates.get(annotator.getStartAnnotation()));

        // type 1 rules: f(qbar:1,...,qbar:n) -> qbar, f(1,...,n)
        for (String label : symbolNodes.keySet()) {
            for (String node : symbolNodes.get(label)) {
                RankedSymbol f = new StdNamedRankedSymbol(label + "_" + node, symbolArities.get(label));
                BiSymbol<RankedSymbol, Pair<State, Variable>> lf = new InnerSymbol<RankedSymbol, Pair<State, Variable>>(f);
                BiSymbol<RankedSymbol, Variable> rf = new InnerSymbol<RankedSymbol, Variable>(f);

                List<Tree<BiSymbol<RankedSymbol, Pair<State, Variable>>>> lhsArgs = new ArrayList<Tree<BiSymbol<RankedSymbol, Pair<State, Variable>>>>();
                List<Tree<BiSymbol<RankedSymbol, Variable>>> rhsArgs = new ArrayList<Tree<BiSymbol<RankedSymbol, Variable>>>();

                for (int i = 1; i <= symbolArities.get(label); i++) {
                    Variable var = new NamedVariable(Integer.toString(i), i - 1);

                    BiSymbol<RankedSymbol, Pair<State, Variable>> larg = new LeafSymbol<RankedSymbol, Pair<State, Variable>>(new Pair(qbar, var));
                    lhsArgs.add(tf.makeTreeFromSymbol(larg));

                    BiSymbol<RankedSymbol, Variable> rarg = new LeafSymbol<RankedSymbol, Variable>(var);
                    rhsArgs.add(tf.makeTreeFromSymbol(rarg));
                }

                ret.addRule(tf.makeTreeFromSymbol(lf, lhsArgs), qbar, tf.makeTreeFromSymbol(rf, rhsArgs));
            }
        }

        // type 2 rules: f(qbar:1,...,q_a:i,...,qbar:n) -> q_a', f(1,...,n)
        // TODO - process null annotations correctly
        for (String label : symbolNodes.keySet()) {
            for (String node : symbolNodes.get(label)) {
                // build symbols
                RankedSymbol f = makeRankedSymbolForLabel(label, node);
                BiSymbol<RankedSymbol, Pair<State, Variable>> lf = new InnerSymbol<RankedSymbol, Pair<State, Variable>>(f);
                BiSymbol<RankedSymbol, Variable> rf = new InnerSymbol<RankedSymbol, Variable>(f);

                // build RHS and variables
                List<Tree<BiSymbol<RankedSymbol, Variable>>> rhsArgs = new ArrayList<Tree<BiSymbol<RankedSymbol, Variable>>>();
                List<Variable> variables = new ArrayList<Variable>();
                for (int i = 1; i <= symbolArities.get(label); i++) {
                    Variable var = new NamedVariable(Integer.toString(i), i - 1);
                    variables.add(var);

                    BiSymbol<RankedSymbol, Variable> rarg = new LeafSymbol<RankedSymbol, Variable>(var);
                    rhsArgs.add(tf.makeTreeFromSymbol(rarg));
                }

                Tree<BiSymbol<RankedSymbol, Variable>> rhs = tf.makeTreeFromSymbol(rf, rhsArgs);

                // now go through known annotation rules for f
                for (String destAnnotation : annotator.getAllAnnotations()) {
                    State parentState = annotationStates.get(destAnnotation);
                    List<String> childAnnotations = annotator.getChildAnnotations(destAnnotation, label);

                    if (childAnnotations != null) {
                        for (int annPos = 0; annPos < symbolArities.get(label); annPos++) {
                            List<Tree<BiSymbol<RankedSymbol, Pair<State, Variable>>>> lhsArgs = new ArrayList<Tree<BiSymbol<RankedSymbol, Pair<State, Variable>>>>();

                            for (int i = 0; i < symbolArities.get(label); i++) {
                                Variable var = variables.get(i);
                                State childState = (i == annPos) ? annotationStates.get(childAnnotations.get(i)) : qbar;

                                BiSymbol<RankedSymbol, Pair<State, Variable>> larg = new LeafSymbol<RankedSymbol, Pair<State, Variable>>(new Pair(childState, var));
                                lhsArgs.add(tf.makeTreeFromSymbol(larg));
                            }

                            ret.addRule(tf.makeTreeFromSymbol(lf, lhsArgs), parentState, rhs);
                        }
                    }
                }
            }
        }

        // type 3 rules: f(g(qbar:1,...,qbar:n)) -> q_a, g(f(1,...,n))
        for (Rule rule : weakening.getAllRules()) {
            if( !symbolArities.containsKey(rule.f1) || !symbolArities.containsKey(rule.f2)) {
                continue;
            }

            int nextVariable = 1;
            List<Variable> variables1 = new ArrayList<Variable>();
            List<Variable> variables2 = new ArrayList<Variable>();
            Variable footVariable;

            for (int i = 1; i <= symbolArities.get(rule.f1); i++) {
                if (i == rule.n1) {
                    variables1.add(null);
                } else {
                    variables1.add(new NamedVariable(Integer.toString(nextVariable), nextVariable - 1));
                    nextVariable++;
                }
            }

            for (int i = 1; i <= symbolArities.get(rule.f2); i++) {
                if (i == rule.n2) {
                    variables2.add(null);
                } else {
                    variables2.add(new NamedVariable(Integer.toString(nextVariable), nextVariable - 1));
                    nextVariable++;
                }
            }

            footVariable = new NamedVariable(Integer.toString(nextVariable), nextVariable - 1);

            for (String node1 : symbolNodes.get(rule.f1)) {
                RankedSymbol f1 = makeRankedSymbolForLabel(rule.f1, node1);

                for (String node2 : symbolNodes.get(rule.f2)) {
                    RankedSymbol f2 = makeRankedSymbolForLabel(rule.f2, node2);

                    Tree<BiSymbol<RankedSymbol, Pair<State, Variable>>> lhs = tf.makeTreeFromSymbol(lsv(qbar, footVariable));
                    Tree<BiSymbol<RankedSymbol, Variable>> rhs = tf.makeTreeFromSymbol(rv(footVariable));

                    lhs = buildType3Lhs(f1, variables1, buildType3Lhs(f2, variables2, lhs));
                    rhs = buildType3Rhs(f2, variables2, buildType3Rhs(f1, variables1, rhs));

                    ret.addRule(lhs, annotationStates.get(rule.annotation), rhs);

                }
            }
        }

        return ret;
    }

    private void collectSymbols(DomGraph graph, NodeLabels labels) {
        symbolArities.clear();
        symbolNodes.clear();

        for (String node : graph.getAllNodes()) {
            String label = labels.getLabel(node);

            if (label != null) {
                symbolArities.put(label, graph.outdeg(node, EdgeType.TREE));
                symbolNodes.put(label, node);
            }
        }
    }

    private RankedSymbol makeRankedSymbolForLabel(String label, String node) {
        return new StdNamedRankedSymbol(label + "_" + node, symbolArities.get(label));
    }

    private static BiSymbol<RankedSymbol, Pair<State, Variable>> lsv(State s, Variable v) {
        return new LeafSymbol<RankedSymbol, Pair<State, Variable>>(new Pair(s, v));
    }

    private static BiSymbol<RankedSymbol, Pair<State, Variable>> li(RankedSymbol s) {
        return new InnerSymbol<RankedSymbol, Pair<State, Variable>>(s);
    }

    private static BiSymbol<RankedSymbol, Variable> rv(Variable v) {
        return new LeafSymbol<RankedSymbol, Variable>(v);
    }

    private static BiSymbol<RankedSymbol, Variable> ri(RankedSymbol s) {
        return new InnerSymbol<RankedSymbol, Variable>(s);
    }

    private Tree<BiSymbol<RankedSymbol, Pair<State, Variable>>> buildType3Lhs(RankedSymbol f, List<Variable> variables, Tree<BiSymbol<RankedSymbol, Pair<State, Variable>>> lhs) {
        List<Tree<BiSymbol<RankedSymbol, Pair<State, Variable>>>> lhsSub = new ArrayList<Tree<BiSymbol<RankedSymbol, Pair<State, Variable>>>>();

        for (Variable v : variables) {
            if (v == null) {
                lhsSub.add(lhs);
            } else {
                lhsSub.add(tf.makeTreeFromSymbol(lsv(qbar, v)));
            }
        }

        return tf.makeTreeFromSymbol(li(f), lhsSub);
    }

    private Tree<BiSymbol<RankedSymbol, Variable>> buildType3Rhs(RankedSymbol f, List<Variable> variables, Tree<BiSymbol<RankedSymbol, Variable>> rhs) {
        List<Tree<BiSymbol<RankedSymbol, Variable>>> rhsSub = new ArrayList<Tree<BiSymbol<RankedSymbol, Variable>>>();

        for (Variable v : variables) {
            if (v == null) {
                rhsSub.add(rhs);
            } else {
                rhsSub.add(tf.makeTreeFromSymbol(rv(v)));
            }
        }

        return tf.makeTreeFromSymbol(ri(f), rhsSub);
    }
}
