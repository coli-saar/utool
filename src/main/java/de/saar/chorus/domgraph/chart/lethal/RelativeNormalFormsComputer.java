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
import de.uni_muenster.cs.sev.lethal.states.State;
import de.uni_muenster.cs.sev.lethal.symbol.common.RankedSymbol;
import de.uni_muenster.cs.sev.lethal.treeautomata.common.FTA;
import de.uni_muenster.cs.sev.lethal.treeautomata.easy.EasyFTA;
import de.uni_muenster.cs.sev.lethal.treeautomata.generic.GenFTA;
import de.uni_muenster.cs.sev.lethal.treeautomata.generic.GenFTARule;
import static de.uni_muenster.cs.sev.lethal.treeautomata.generic.GenFTAOps.*;
import java.util.Comparator;
import java.util.Set;

/**
 *
 * @author koller
 */
public class RelativeNormalFormsComputer {
    private static final boolean DEBUG=true;
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
        stopwatch.start("fta");
        EasyFTA chartFta = ChartToLethal.convertToFta(chart, graph, labels);
        stopwatch.report("fta", "Converted");

        if( verbose ) {
            System.out.println("Chart FTA:\n" + chartFta);
        }

        stopwatch.start("ctt");
        ContextTreeTransducer<RankedSymbol,RankedSymbol,State> ctt = rstt.convert(graph, labels);
        stopwatch.report("ctt", "CTT computed, " + ctt.getRules().size() + " rules");

        if(verbose) {
            System.out.println("\n\nContext tree transducer:\n" + ctt);
        }

        stopwatch.start("pre-image");
        FTA<RankedSymbol, PairState<State,String>, GenFTARule<RankedSymbol, PairState<State,String>>> preImage = ctt.computePreImage(chartFta);
        stopwatch.report("pre-image", "pre-image computed, " + preImage.getRules().size() + " rules");

        if( verbose ) {
            System.out.println("\n\nPre-image of chart under ctt:\n" + preImage);
            System.out.println("\n\nPre-image of chart under ctt (reduced):\n" + reduceFull(preImage));
        }

        GenFTA<RankedSymbol,PairState<State,String>> preduced = reduceFull(preImage);

        stopwatch.start("diff");
        GenFTA<RankedSymbol, PairState<State, String>> diff = differenceSpecialized(chartFta, preduced);
        stopwatch.report("diff", "Computed");

        GenFTA<RankedSymbol, PairState<State, String>> reduced = reduceFull(diff);

        if( verbose ) {
            System.out.println("\n\nDifference automaton:\n" + diff);
            System.out.println("\n\nDifference automaton, reduced:\n" + reduceFull(diff));
        }

        return reduced;
    }

    public RegularTreeGrammar<DecoratedNonterminal<SubgraphNonterminal,String>> reduceToChart(Chart chart, DomGraph graph, NodeLabels labels) {
        GenFTA<RankedSymbol, PairState<State, String>> fta = reduce(chart, graph, labels);
        return ChartToLethal.convertFtaToChart(fta, graph);
    }
}
