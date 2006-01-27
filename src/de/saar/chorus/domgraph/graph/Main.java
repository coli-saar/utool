/*
 * @(#)Main.java created 25.01.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.graph;

import java.io.OutputStreamWriter;
import java.util.Set;

import de.saar.chorus.domgraph.chart.Chart;
import de.saar.chorus.domgraph.chart.ChartSolver;
import de.saar.chorus.domgraph.chart.SolvedFormIterator;
import de.saar.chorus.domgraph.codec.InputCodec;
import de.saar.chorus.domgraph.codec.basic.Chain;
import de.saar.chorus.domgraph.codec.term.OzTermOutputCodec;

public class Main {
    public static void main(String[] args) throws Exception {
        DomGraph g = new DomGraph();
        NodeLabels l = new NodeLabels();
        
	//        GxlCodec codec = new GxlCodec();
        InputCodec codec = new Chain();
        codec.decode(args[0], g, l);
        
        /*
        Set<String> nodes = new HashSet<String>();
        nodes.addAll(g.getAllNodes());
        nodes.remove("x2");
        
        System.err.println(g.wccsOfSubgraph(nodes));
        System.exit(0);
        */
        
        
        
        
        Chart chart = new Chart();
        ChartSolver solver = new ChartSolver(g, chart);
        
        long start = System.currentTimeMillis();
        solver.solve();
        long end = System.currentTimeMillis();
        
        //System.err.println("Chart:\n" + chart);
        System.err.println("Chart size: " + chart.size());
        System.err.println("Runtime: " + (end-start)  + "ms\n\n");
        
        
        //displayAllSolvedForms(chart, g, l);
        timeAllSolvedForms(chart, g, l);
    }

	/**
	 * @param chart
	 * @param g
	 * @param l
	 */
	private static void timeAllSolvedForms(Chart chart, DomGraph g, NodeLabels l) {
		long start = System.currentTimeMillis();
		long count = 0;
		SolvedFormIterator it = new SolvedFormIterator(chart);
		
		while( it.hasNext() ) {
			it.next();
			count++;
		}
		
		long end = System.currentTimeMillis();
		System.err.println("Enumerated " + count + " sfs in " + (end-start) + " ms");
	}

	/**
	 * @param chart
	 * @param l 
	 * @param g 
	 */
	private static void displayAllSolvedForms(Chart chart, DomGraph g, NodeLabels l)
	throws Exception
	{
        System.out.println("solved forms:");
        SolvedFormIterator it = new SolvedFormIterator(chart);
        int num = 1;
        
        OzTermOutputCodec outcodec = new OzTermOutputCodec();
        
        while( it.hasNext() ) {
            Set<DomEdge> domedges = it.next();
            
            System.err.print((num++) + ": ");
            outcodec.encode(g, domedges, l, new OutputStreamWriter(System.err));
            System.err.println();
        }
		
	}
}
