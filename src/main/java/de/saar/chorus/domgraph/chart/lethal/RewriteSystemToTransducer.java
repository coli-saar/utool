/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.chorus.domgraph.chart.lethal;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import de.saar.chorus.contexttransducer.ContextTreeTransducer;
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
        for (String label : symbolNodes.keySet()) {
            for (String node : symbolNodes.get(label)) {
                // build symbols
                RankedSymbol f = new StdNamedRankedSymbol(label + "_" + node, symbolArities.get(label));
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
        

        return ret;
    }

    private void collectSymbols(DomGraph graph, NodeLabels labels) {
        symbolArities.clear();
        symbolNodes.clear();

        for (String node : graph.getAllNodes()) {
            String label = labels.getLabel(node);

            if (label != null) {
                symbolArities.put(label, graph.outdeg(node, EdgeType.TREE));

                if (graph.isRoot(node)) {
                    symbolNodes.put(label, node);
                }
            }
        }
    }
}
