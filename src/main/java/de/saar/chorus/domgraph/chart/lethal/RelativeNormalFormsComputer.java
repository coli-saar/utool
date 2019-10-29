/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.chorus.domgraph.chart.lethal;

import de.saar.chorus.contexttransducer.ContextTreeTransducer;
import de.saar.chorus.contexttransducer.PairState;
import de.saar.chorus.domgraph.chart.Chart;
import de.saar.chorus.domgraph.chart.DecoratedNonterminal;
import de.saar.chorus.domgraph.chart.RegularTreeGrammar;
import de.saar.chorus.domgraph.chart.SubgraphNonterminal;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.term.Term;
import de.uni_muenster.cs.sev.lethal.languages.RegularTreeLanguage;
import de.uni_muenster.cs.sev.lethal.states.State;
import de.uni_muenster.cs.sev.lethal.symbol.common.RankedSymbol;
import de.uni_muenster.cs.sev.lethal.tree.common.Tree;
import de.uni_muenster.cs.sev.lethal.treeautomata.common.AbstractFTA;
import de.uni_muenster.cs.sev.lethal.treeautomata.common.FTA;
import de.uni_muenster.cs.sev.lethal.treeautomata.common.FTARule;
import de.uni_muenster.cs.sev.lethal.treeautomata.easy.EasyFTA;
import de.uni_muenster.cs.sev.lethal.treeautomata.generic.GenFTA;
import de.uni_muenster.cs.sev.lethal.treeautomata.generic.GenFTARule;
import static de.uni_muenster.cs.sev.lethal.treeautomata.generic.GenFTAOps.*;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author koller
 */
public class RelativeNormalFormsComputer {

    private static final boolean DEBUG = true;
    private boolean verbose = false;
    private RewriteSystemToTransducer rstt;
    private static final Stopwatch stopwatch = new Stopwatch();

    public RelativeNormalFormsComputer(Annotator annotator) {
        rstt = new RewriteSystemToTransducer(annotator);
    }

    public void addRewriteSystem(RewriteSystem trs) {
        rstt.addRewriteSystem(trs);
    }

    public void addRewriteSystem(RewriteSystem trs, Comparator<Term> comparator) {
        rstt.addRewriteSystem(trs, comparator);
    }

    public void setVerbose(boolean v) {
        verbose = v;
    }

    public GenFTA<RankedSymbol, PairState<State, String>> reduce(Chart chart, DomGraph graph, NodeLabels labels) {
        System.err.println("\n--- start reducing ---");

        stopwatch.start("fta");
        EasyFTA chartFta = ChartToLethal.convertToFta(chart, graph, labels);
        stopwatch.report("fta", "Converted");

        if (verbose) {
            System.out.println("Chart FTA:\n" + chartFta);
        }

        stopwatch.start("ctt");
        ContextTreeTransducer<RankedSymbol, RankedSymbol, State> ctt = rstt.convert(graph, labels);
        stopwatch.report("ctt", "CTT computed, " + ctt.getRules().size() + " rules");

        if (verbose) {
            System.out.println("\n\nContext tree transducer:\n" + ctt);
        }

        stopwatch.start("pre-image");
        FTA<RankedSymbol, PairState<State, String>, GenFTARule<RankedSymbol, PairState<State, String>>> preImage = ctt.computePreImage(chartFta);
        stopwatch.report("pre-image", "pre-image computed, " + preImage.getRules().size() + " rules");

        if (verbose) {
            System.out.println("\n\nPre-image of chart under ctt:\n" + preImage);
            System.out.println("\n\nPre-image of chart under ctt (reduced):\n" + reduceFull(preImage));
        }

        GenFTA<RankedSymbol, PairState<State, String>> preduced = reduceFull(preImage);

//        System.err.println("chart fta describes " + countFtaTrees(chartFta));
//        System.err.println("pre-image automaton describes " + countFtaTrees((AbstractFTA) preImage) + " trees");
//        System.err.println("reduced pre-image automaton describes " + countFtaTrees(preduced) + " trees");

//        AbstractFTA intersection = determinize(intersectionBU(chartFta, preduced));
//        System.err.println("reduced pre-image intersect chart describes " + countFtaTrees(intersection) + " trees");

        stopwatch.start("diff");
        GenFTA<RankedSymbol, PairState<State, String>> diff = differenceSpecialized(chartFta, preduced);
        stopwatch.report("diff", "Computed");

        GenFTA<RankedSymbol, PairState<State, String>> reduced = reduceFull(diff);
//        System.err.println("result fta describes " + countFtaTrees(reduced));

        if (verbose) {
            System.out.println("\n\nDifference automaton:\n" + diff);
            System.out.println("\n\nDifference automaton, reduced:\n" + reduceFull(diff));
        }

        System.err.println("--- done reducing ---");

        return reduced;
    }

    public static long theircountFtaTrees(FTA fta) {
        RegularTreeLanguage<RankedSymbol> rtl = new RegularTreeLanguage(fta);
        long count = 0;
        for (Tree t : rtl) {
//            System.out.println("result tree #" + (count+1) + ": " + t);
            count++;
        }

        return count;
    }

    // NB only use this for deterministic automata, otherwise trees can be counted twice
    public static long countFtaTrees(AbstractFTA fta) {
        Map<State, Long> count = new HashMap<State, Long>();
        List<? extends FTARule> rules = fta.getRulesInBottomUpOrder();

        assert new HashSet(rules).equals(new HashSet(fta.getRules()));



        for (FTARule rule : rules) {
            long countHere = 1;
            List<? extends State> srcStates = rule.getSrcStates();

//            System.err.print(rule.toString() + ": ");

            for (State s : srcStates) {
                countHere *= count.get(s);
//                System.err.print(count.get(s) + "*");
            }

            long countForDestState = 0;
            if (count.containsKey(rule.getDestState())) {
                countForDestState = count.get(rule.getDestState());
            }
            countForDestState += countHere;
            count.put(rule.getDestState(), countForDestState);

//            System.err.println(" = " + countHere + " for " + rule.getDestState() + " (now " + countForDestState + ")");


        }

        long ret = 0;
        Set<? extends State> finalStates = fta.getFinalStates();
        for (State f : finalStates) {
//            System.err.println("final state " + f + ": " + count.get(f) + " trees");
            ret += count.get(f);
        }

//        System.err.println("tree counts per state: " + count);

        return ret;
    }

    public RegularTreeGrammar<DecoratedNonterminal<SubgraphNonterminal, String>> reduceToChart(Chart chart, DomGraph graph, NodeLabels labels) {
        GenFTA<RankedSymbol, PairState<State, String>> fta = reduce(chart, graph, labels);
        RegularTreeGrammar<DecoratedNonterminal<SubgraphNonterminal, String>> ret = ChartToLethal.convertFtaToChart(fta, graph);
        return ret;
    }
}
