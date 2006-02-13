/*
 * @(#)OneSplitSource.java created 13.02.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.chart;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.saar.chorus.domgraph.graph.DomGraph;

public class OneSplitSource extends SplitSource {

    public OneSplitSource(DomGraph graph) {
        super(graph);
    }

    protected Iterator<Split> computeSplits(Set<String> subgraph) {
        SplitComputer sc = new SplitComputer(graph);
        List<Split> ret = new ArrayList<Split>();
        List<String> potentialFreeRoots = computePotentialFreeRoots(subgraph);

        for( String root : potentialFreeRoots ) {
            try {
                Split split = sc.computeSplit(root, subgraph);
                ret.add(split);
                return ret.iterator();
            } catch (RootNotFreeException e) {
                // if the root was not free, do nothing
            }
        }

        return ret.iterator();
    }

    public static boolean isGraphSolvable(DomGraph graph) {
        DomGraph cpt = graph.compactify();
        Chart chart = new Chart();
        ChartSolver solver = new ChartSolver(cpt, chart, new OneSplitSource(graph));
        
        return solver.solve();
    }
}
