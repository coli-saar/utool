/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.chorus.domgraph.chart.lethal;

import de.saar.chorus.contexttransducer.ContextTreeTransducer;
import de.saar.chorus.domgraph.chart.lethal.RewriteSystem.Rule;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.term.Compound;
import de.saar.chorus.term.Constant;
import de.saar.chorus.term.Term;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author koller
 */
public class RewriteSystemToTransducer {

    private static final Stopwatch stopwatch = new Stopwatch();
    private Map<String, State> annotationStates;
    private List<Pair<RewriteSystem, Comparator<Term>>> rewriteSystems;
//    private RewriteSystem weakening, equivalence;
    private Annotator annotator;
    private State qbar;
    private int nextVariable;
    private static final StdStateBuilder<String> sb = new StdStateBuilder<String>();
    private static TreeFactory tf = TreeFactory.getTreeFactory();

    public RewriteSystemToTransducer(Annotator annotator) {
//        this.weakening = weakening;
//        this.equivalence = equivalence;
        rewriteSystems = new ArrayList<Pair<RewriteSystem, Comparator<Term>>>();
        this.annotator = annotator;

        qbar = sb.convert("qbar");
        annotationStates = new HashMap<String, State>();
        for (String ann : annotator.getAllAnnotations()) {
            annotationStates.put(ann, sb.convert("q_" + ann));
        }
    }

    public void addRewriteSystem(RewriteSystem trs, Comparator<Term> comparator) {
        rewriteSystems.add(new Pair<RewriteSystem, Comparator<Term>>(trs, comparator));
    }

    public void addRewriteSystem(RewriteSystem trs) {
        addRewriteSystem(trs, new DummyComparator());
    }

    public ContextTreeTransducer<RankedSymbol, RankedSymbol, State> convert(DomGraph graph, NodeLabels labels) {
        final ContextTreeTransducer<RankedSymbol, RankedSymbol, State> ret = new ContextTreeTransducer<RankedSymbol, RankedSymbol, State>();
        RewriteSystemSpecializer specializer = new RewriteSystemSpecializer(graph, labels, annotator);

        // set final and neutral state
        ret.addFinalState(annotationStates.get(annotator.getStartAnnotation()));
        ret.setNeutralState(qbar);

        // type 1 rules: f(qbar:1,...,qbar:n) -> qbar, f(1,...,n)
        stopwatch.start("ctt type 1 rules");
        for (String label : specializer.getAllLabels()) {
            for (RankedSymbol f : specializer.getSpecializedRankedSymbols(label)) {
                BiSymbol<RankedSymbol, Pair<State, Variable>> lf = new InnerSymbol<RankedSymbol, Pair<State, Variable>>(f);
                BiSymbol<RankedSymbol, Variable> rf = new InnerSymbol<RankedSymbol, Variable>(f);

                List<Tree<BiSymbol<RankedSymbol, Pair<State, Variable>>>> lhsArgs = new ArrayList<Tree<BiSymbol<RankedSymbol, Pair<State, Variable>>>>();
                List<Tree<BiSymbol<RankedSymbol, Variable>>> rhsArgs = new ArrayList<Tree<BiSymbol<RankedSymbol, Variable>>>();

                for (int i = 1; i <= f.getArity(); i++) {
                    Variable var = new NamedVariable(Integer.toString(i), i - 1);

                    BiSymbol<RankedSymbol, Pair<State, Variable>> larg = new LeafSymbol<RankedSymbol, Pair<State, Variable>>(new Pair(qbar, var));
                    lhsArgs.add(tf.makeTreeFromSymbol(larg));

                    BiSymbol<RankedSymbol, Variable> rarg = new LeafSymbol<RankedSymbol, Variable>(var);
                    rhsArgs.add(tf.makeTreeFromSymbol(rarg));
                }

                ret.addRule(tf.makeTreeFromSymbol(lf, lhsArgs), qbar, tf.makeTreeFromSymbol(rf, rhsArgs));
            }
        }
        stopwatch.report("ctt type 1 rules", "ctt type 1 rules");

        // type 2 rules: f(qbar:1,...,q_a:i,...,qbar:n) -> q_a', f(1,...,n)
        // if there is no annotator rule for f and a', then pretend it's a':f(0,...,0) where 0 is neutral state
        stopwatch.start("ctt type 2 rules");
        for (String label : specializer.getAllLabels()) {
            for (RankedSymbol f : specializer.getSpecializedRankedSymbols(label)) {
                BiSymbol<RankedSymbol, Pair<State, Variable>> lf = new InnerSymbol<RankedSymbol, Pair<State, Variable>>(f);
                BiSymbol<RankedSymbol, Variable> rf = new InnerSymbol<RankedSymbol, Variable>(f);

                // build RHS and variables
                List<Tree<BiSymbol<RankedSymbol, Variable>>> rhsArgs = new ArrayList<Tree<BiSymbol<RankedSymbol, Variable>>>();
                List<Variable> variables = new ArrayList<Variable>();
                for (int i = 1; i <= f.getArity(); i++) {
                    Variable var = new NamedVariable(Integer.toString(i), i - 1);
                    variables.add(var);

                    BiSymbol<RankedSymbol, Variable> rarg = new LeafSymbol<RankedSymbol, Variable>(var);
                    rhsArgs.add(tf.makeTreeFromSymbol(rarg));
                }

                Tree<BiSymbol<RankedSymbol, Variable>> rhs = tf.makeTreeFromSymbol(rf, rhsArgs);

                // iterate over all annotations for this label and build appropriate transducer rules
                for (String parentAnnotation : annotator.getAllAnnotations()) {
                    List<String> childAnnotations = annotator.getChildAnnotations(parentAnnotation, label);

                    // if there is no annotation rule for this label and annotation, then pretend it's a:f(0,...,0)
                    if (childAnnotations == null) {
                        childAnnotations = new ArrayList<String>();
                        for (int i = 0; i < f.getArity(); i++) {
                            childAnnotations.add(annotator.getNeutralAnnotation());
                        }
                    }

                    for (int i = 0; i < f.getArity(); i++) {
                        ret.addRule(makeLhsWithOneAnnotationState(f, i, childAnnotations.get(i), variables), annotationStates.get(parentAnnotation), rhs);
                    }
                }
            }
        }
        stopwatch.report("ctt type 2 rules", "ctt type 2 rules");

        // type 3 rules: f(g(qbar:1,...,qbar:n)) -> q_a, g(f(1,...,n))
        /*
        stopwatch.start("ctt type 3 rules");
        for (Pair<RewriteSystem, Comparator<Term>> trsWithComp : rewriteSystems) {
        RewriteSystem specializedWeakening = specializer.specialize(trsWithComp.getFirst(), trsWithComp.getSecond());

        for (Rule rule : specializedWeakening.getAllRules()) {
        Map<de.saar.chorus.term.Variable, Variable> variableMap = new HashMap<de.saar.chorus.term.Variable, Variable>();

        nextVariable = 1;
        Tree<BiSymbol<RankedSymbol, Pair<State, Variable>>> lhs = convertLhs(rule.lhs, variableMap);
        Tree<BiSymbol<RankedSymbol, Variable>> rhs = convertRhs(rule.rhs, variableMap);

        ret.addRule(lhs, annotationStates.get(rule.annotation), rhs);
        }
        }
        stopwatch.report("ctt type 3 rules", "ctt type 3 rules");
         * */

        stopwatch.start("ctt type 3 rules");
        try {
            for (Pair<RewriteSystem, Comparator<Term>> trsWithComp : rewriteSystems) {
                RewriteSystem specializedWeakening = specializer.specialize(trsWithComp.getFirst(), trsWithComp.getSecond());

                final ExecutorService pool = Executors.newFixedThreadPool(2);
//                final ExecutorService pool = Executors.newSingleThreadExecutor();

                for (final Rule rule : specializedWeakening.getAllRules()) {
                    pool.execute(new Runnable() {

                        public void run() {
                            Map<de.saar.chorus.term.Variable, Variable> variableMap = new HashMap<de.saar.chorus.term.Variable, Variable>();

                            nextVariable = 1;
                            Tree<BiSymbol<RankedSymbol, Pair<State, Variable>>> lhs = convertLhs(rule.lhs, variableMap);
                            Tree<BiSymbol<RankedSymbol, Variable>> rhs = convertRhs(rule.rhs, variableMap);

                            synchronized (ret) {
                                ret.addRule(lhs, annotationStates.get(rule.annotation), rhs);
                            }
                        }
                    });
                }
                pool.shutdown();
                pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
            }
        } catch (InterruptedException e) {
        }
        stopwatch.report("ctt type 3 rules", "ctt type 3 rules");

        return ret;
    }

    private Tree<BiSymbol<RankedSymbol, Pair<State, Variable>>> makeLhsWithOneAnnotationState(RankedSymbol f, int childPosition, String childAnnotation, List<Variable> variables) {
        BiSymbol<RankedSymbol, Pair<State, Variable>> lf = new InnerSymbol<RankedSymbol, Pair<State, Variable>>(f);
        List<Tree<BiSymbol<RankedSymbol, Pair<State, Variable>>>> lhsArgs = new ArrayList<Tree<BiSymbol<RankedSymbol, Pair<State, Variable>>>>();

        for (int i = 0; i < f.getArity(); i++) {
            Variable var = variables.get(i);
            State childState = (i == childPosition) ? annotationStates.get(childAnnotation) : qbar;

            BiSymbol<RankedSymbol, Pair<State, Variable>> larg = new LeafSymbol<RankedSymbol, Pair<State, Variable>>(new Pair(childState, var));
            lhsArgs.add(tf.makeTreeFromSymbol(larg));
        }

        return tf.makeTreeFromSymbol(lf, lhsArgs);
    }

    private Tree<BiSymbol<RankedSymbol, Pair<State, Variable>>> convertLhs(Term lhs, Map<de.saar.chorus.term.Variable, Variable> variableMap) {
        if (lhs instanceof de.saar.chorus.term.Variable) {
            Variable v = new NamedVariable(Integer.toString(nextVariable), nextVariable - 1);
            nextVariable++;

            variableMap.put((de.saar.chorus.term.Variable) lhs, v);
            return tf.makeTreeFromSymbol(lsv(qbar, v));
        } else if (lhs instanceof Constant) {
            return tf.makeTreeFromSymbol(li(new StdNamedRankedSymbol(((Constant) lhs).getName(), 0)));
        } else if (lhs instanceof Compound) {
            Compound c = (Compound) lhs;
            List<Tree<BiSymbol<RankedSymbol, Pair<State, Variable>>>> sub = new ArrayList<Tree<BiSymbol<RankedSymbol, Pair<State, Variable>>>>();
            for (Term t : c.getSubterms()) {
                sub.add(convertLhs(t, variableMap));
            }

            return tf.makeTreeFromSymbol(li(new StdNamedRankedSymbol(c.getLabel(), c.getSubterms().size())), sub);
        } else {
            throw new UnsupportedOperationException("Encountered illegal term type: " + lhs);
        }
    }

    private Tree<BiSymbol<RankedSymbol, Variable>> convertRhs(Term rhs, Map<de.saar.chorus.term.Variable, Variable> variableMap) {
        if (rhs instanceof de.saar.chorus.term.Variable) {
            return tf.makeTreeFromSymbol(rv(variableMap.get((de.saar.chorus.term.Variable) rhs)));
        } else if (rhs instanceof Constant) {
            return tf.makeTreeFromSymbol(ri(new StdNamedRankedSymbol(((Constant) rhs).getName(), 0)));
        } else if (rhs instanceof Compound) {
            Compound c = (Compound) rhs;
            List<Tree<BiSymbol<RankedSymbol, Variable>>> sub = new ArrayList<Tree<BiSymbol<RankedSymbol, Variable>>>();

            for (Term t : c.getSubterms()) {
                sub.add(convertRhs(t, variableMap));
            }

            return tf.makeTreeFromSymbol(ri(new StdNamedRankedSymbol(c.getLabel(), c.getSubterms().size())), sub);
        } else {
            throw new UnsupportedOperationException("Encountered illegal term type: " + rhs);
        }
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
}
