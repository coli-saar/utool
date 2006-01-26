/*
 * @(#)Main.java created 25.01.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.graph;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Set;

import de.saar.chorus.domgraph.chart.ChartSolver;
import de.saar.chorus.domgraph.chart.SolvedFormIterator;
import de.saar.chorus.domgraph.codec.gxl.GxlCodec;
import de.saar.chorus.domgraph.codec.basic.Chain;
import de.saar.chorus.domgraph.codec.InputCodec;
import de.saar.chorus.domgraph.codec.term.OzTermOutputCodec;

public class Main {
    public static void main(String[] args) throws Exception {
        DomGraph g = new DomGraph();
        NodeLabels l = new NodeLabels();
        
	//        GxlCodec codec = new GxlCodec();
	InputCodec codec = new Chain();
        
        codec.decode(args[0], g, l);
        
        ChartSolver solver = new ChartSolver(g);
        
        long start = System.currentTimeMillis();
        solver.solve();
        long end = System.currentTimeMillis();
        
        System.out.println("Chart:\n" + solver.getChart());
        System.out.println("Chart size: " + solver.getChart().size());
        System.out.println("Runtime: " + (end-start)  + "ms\n\n");
        
        
        System.out.println("solved forms:");
        SolvedFormIterator it = new SolvedFormIterator(solver.getChart());
        int num = 1;
        
        OzTermOutputCodec outcodec = new OzTermOutputCodec();
        
        while( it.hasNext() ) {
            Set<DomEdge> domedges = it.next();
            
            if( it.representsSolvedForm() ) {
                System.err.print((num++) + ": ");
                outcodec.encode(g, domedges, l, new OutputStreamWriter(System.err));
                System.err.println();
            } else {
                System.err.println("spurious sf");
                break;
            }
        }
    }
}
