/*
 * @(#)Main.java created 25.01.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.graph;

import de.saar.chorus.domgraph.chart.ChartSolver;
import de.saar.chorus.domgraph.codec.gxl.GxlCodec;

public class Main {
    public static void main(String[] args) throws Exception {
        DomGraph g = new DomGraph();
        NodeLabels l = new NodeLabels();
        
        GxlCodec codec = new GxlCodec();
        
        codec.decode(args[0], g, l);
        
        ChartSolver solver = new ChartSolver(g);
        
        long start = System.currentTimeMillis();
        solver.solve();
        long end = System.currentTimeMillis();
        
        System.out.println("Chart:\n" + solver.getChart());
        System.out.println("Runtime: " + (end-start)  + "ms");
        
    }
}
