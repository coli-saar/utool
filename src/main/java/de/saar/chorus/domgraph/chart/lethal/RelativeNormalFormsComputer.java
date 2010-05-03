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

    public RelativeNormalFormsComputer(RewriteSystem weakening, RewriteSystem equivalence, Annotator annotator) {
        rstt = new RewriteSystemToTransducer(weakening, equivalence, annotator);
    }

    public FTA reduce(Chart chart, DomGraph graph, NodeLabels labels) {
        if(DEBUG) System.err.println("Converting chart to FTA ...");
        EasyFTA chartFta = ChartToFTA.convert(chart, graph, labels);

        if(DEBUG) System.err.println("Computing ctt ...");
        ContextTreeTransducer<RankedSymbol,RankedSymbol,State> ctt = rstt.convert(graph, labels);

        if(DEBUG) System.err.println("Computing pre-image ...");
        FTA preImage = ctt.computePreImage(chartFta);

        if(DEBUG) System.err.println("Computing complement and intersection ...");
        FTA reduced = intersectionTD(chartFta, complement(preImage));

        return reduced;
    }
}
