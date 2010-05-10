/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.chorus.domgraph.chart.lethal;

import de.saar.chorus.contexttransducer.ContextTreeTransducer;
import de.saar.chorus.domgraph.chart.Chart;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.uni_muenster.cs.sev.lethal.states.State;
import de.uni_muenster.cs.sev.lethal.symbol.common.RankedSymbol;
import de.uni_muenster.cs.sev.lethal.treeautomata.common.FTA;
import de.uni_muenster.cs.sev.lethal.treeautomata.easy.EasyFTA;
import static de.uni_muenster.cs.sev.lethal.treeautomata.generic.GenFTAOps.*;

/**
 *
 * @author koller
 */
public class RelativeNormalFormsComputer {
    private static final boolean DEBUG=true;
    private boolean verbose = false;
    private RewriteSystemToTransducer rstt;
    private static final Stopwatch stopwatch = new Stopwatch();

    public RelativeNormalFormsComputer(RewriteSystem weakening, RewriteSystem equivalence, Annotator annotator) {
        rstt = new RewriteSystemToTransducer(weakening, equivalence, annotator);
    }

    public void setVerbose(boolean v) {
        verbose = v;
    }

    public FTA reduce(Chart chart, DomGraph graph, NodeLabels labels) {
        if(DEBUG) System.err.println("Converting chart to FTA ...");
        stopwatch.start("fta");
        EasyFTA chartFta = ChartToFTA.convert(chart, graph, labels);
        stopwatch.report("fta", "Converted");

        if( verbose ) {
            System.out.println("Chart FTA:\n" + chartFta);
        }

        if(DEBUG) System.err.println("Computing ctt ...");
        stopwatch.start("ctt");
        ContextTreeTransducer<RankedSymbol,RankedSymbol,State> ctt = rstt.convert(graph, labels);
        stopwatch.report("ctt", "CTT computed");

        if(verbose) {
            System.out.println("\n\nContext tree transducer:\n" + ctt);
        }

        if(DEBUG) System.err.println("Computing pre-image ...");
        stopwatch.start("pre");
        FTA preImage = ctt.computePreImage(chartFta);
        stopwatch.report("pre", "Computed");

        if( verbose ) {
            System.out.println("\n\nPre-image of chart under ctt:\n" + preImage);
            System.out.println("\n\nPre-image of chart under ctt (reduced):\n" + reduceFull(preImage));
        }

        if(DEBUG) System.err.println("Computing difference automaton ...");
        stopwatch.start("diff");
        FTA diff = differenceSpecialized(chartFta, preImage);
        stopwatch.report("diff", "Computed");

        FTA reduced = reduceFull(diff);

        if( verbose ) {
            System.out.println("\n\nDifference automaton:\n" + diff);
            System.out.println("\n\nDifference automaton, reduced:\n" + reduceFull(diff));
        }

        return reduced;
    }
}
