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
    private RewriteSystemToTransducer rstt;
    private static final Stopwatch stopwatch = new Stopwatch();

    public RelativeNormalFormsComputer(RewriteSystem weakening, RewriteSystem equivalence, Annotator annotator) {
        rstt = new RewriteSystemToTransducer(weakening, equivalence, annotator);
    }

    public FTA reduce(Chart chart, DomGraph graph, NodeLabels labels) {
        if(DEBUG) System.err.println("Converting chart to FTA ...");
        stopwatch.start("fta");
        EasyFTA chartFta = ChartToFTA.convert(chart, graph, labels);
        stopwatch.report("fta", "Converted");

        if(DEBUG) System.err.println("Computing ctt ...");
        stopwatch.start("ctt");
        ContextTreeTransducer<RankedSymbol,RankedSymbol,State> ctt = rstt.convert(graph, labels);
        stopwatch.report("ctt", "CTT computed");

        if(DEBUG) System.err.println("Computing pre-image ...");
        stopwatch.start("pre");
        FTA preImage = ctt.computePreImage(chartFta);
        stopwatch.report("pre", "Computed");

        /*
        FTA pre1 = reduceTopDown(preImage);
        System.err.println("reduced pre-image fta has " + pre1.getRules().size() + " rules");

        FTA reduced = determinizeBU(pre1);
         * 
         */

        if(DEBUG) System.err.println("Computing complement and intersection ...");
        stopwatch.start("complement");
        FTA a = complement(preImage);
        stopwatch.report("complement", "Computed complement");

        stopwatch.start("intersect");
        FTA reduced = intersectionTD(chartFta, a);
        stopwatch.report("intersect", "Computed intersection");

        return reduced;
    }
}
