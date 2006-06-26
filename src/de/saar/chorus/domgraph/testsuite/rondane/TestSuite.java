/*
 * @(#)TestSuite.java created 26.06.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.testsuite.rondane;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;

public class TestSuite {
    private RondaneGraphs ron = new RondaneGraphs();
    private RondaneGraphStatistics stats = new RondaneGraphStatistics();
    
    @DataProvider(name = "rondane-graphs")
    public Object[][] createRondaneGraphs() {
        Object[][] ret = new Object[ron.getIds().size()][];
        int i = 0;
        
        for( Integer id : ron.getIds() ) {
            ret[i++] = new Object[] { id };
        }
        
        return ret;
    }
    
    
    @Test(groups = {"DomgraphTestsuiteRondane"}, dataProvider = "rondane-graphs")
    public void testStatistics(Integer id) throws Exception {
        DomGraph graph = new DomGraph();
        NodeLabels labels = new NodeLabels();
        
        ron.getGraph(id, graph, labels);
        stats.testEquals(id, graph, labels);
    }
    
    
}
