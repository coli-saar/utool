/*
 * @(#)RondaneGraphStatisticsMockup.java created 26.06.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.testsuite.rondane;

import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import de.saar.chorus.domgraph.chart.Chart;
import de.saar.chorus.domgraph.chart.ChartSolver;
import de.saar.chorus.domgraph.chart.SolverNotApplicableException;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.domgraph.graph.DomGraph.PreprocessingException;

public class RondaneGraphStatisticsBase {
    Map<Integer,BigInteger> readings = new HashMap<Integer,BigInteger>();
    Map<Integer,Map<String,Boolean>> properties = new HashMap<Integer,Map<String,Boolean>>();
    
    public RondaneGraphStatisticsBase() {
        Class c = getClass();
        Method[] allMethods = c.getMethods();
        
        for( Method m : allMethods ) {
            if( m.getName().startsWith("makeGraphData")) {
                try {
                    m.invoke(this);
                } catch (Exception e) {
                    
                }
            }
        }
    }

    public static Map<String,Boolean> classifyGraph(DomGraph graph) {
        Map<String,Boolean> ret = new HashMap<String,Boolean>();
        
        ret.put("compact", Boolean.valueOf(graph.isCompact()));
        ret.put("hnc", Boolean.valueOf(graph.isHypernormallyConnected()));
        ret.put("normal", Boolean.valueOf(graph.isNormal()));
        ret.put("weaklynormal", Boolean.valueOf(graph.isWeaklyNormal()));
        
        return ret;
    }
    
    public static BigInteger getNumSolvedForms(DomGraph graph) {
    	try {
    		Chart c = new Chart();
    		ChartSolver.solve(graph.preprocess(), c);
    		return c.countSolvedForms();
    	} catch(SolverNotApplicableException e) {
    		return null;
    	} catch (PreprocessingException e) {
    		return BigInteger.ZERO;
		}
    }
    
    public void testEquals(int id, DomGraph graph, NodeLabels labels) {
        BigInteger gold = readings.get(id);
        BigInteger here = getNumSolvedForms(graph); 
        assert gold.equals(here) :
            "Sentence " + id + " has wrong number of solved forms (" + here + " rather than " + gold + ")";
        
        Map<String,Boolean> goldProps = properties.get(id);
        Map<String,Boolean> hereProps = classifyGraph(graph);
        assert goldProps.equals(hereProps) :
            "Sentence " + id + " has wrong properties (" + hereProps + " rather than " + goldProps + ")";
    }
}
