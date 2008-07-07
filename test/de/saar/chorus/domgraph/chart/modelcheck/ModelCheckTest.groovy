package de.saar.chorus.domgraph.chart.modelcheck

import de.saar.chorus.domgraph.graph.*;
import de.saar.chorus.domgraph.chart.*;
import de.saar.chorus.domgraph.chart.modelcheck.ModelCheck;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class ModelCheckTest {
	void testSomething() {
		
	}
	
	@Test
	public void testValidSolvedForm() {
		DomGraph g1 = new DomGraph();
		NodeLabels l1 = new NodeLabels();
		makeChain4(g1,l1);
		
		Chart c1= new Chart(l1);
		
		ChartSolver.solve(g1,c1)
		
		DomGraph sf = new DomGraph();
		NodeLabels l2 = new NodeLabels();
		makeGoodSolvedForm(sf,l2);
		
		Chart c2 = new Chart(l2);
		ChartSolver.solve(sf,c2);
		
		assert ModelCheck.subsumes(c1,g1,l1,c2,sf,l2) == true;
	}
	
	
	@Test
	public void testInvalidSomvedForm() {
		DomGraph g1 = new DomGraph();
		NodeLabels l1 = new NodeLabels();
		makeChain4(g1,l1);
		
		Chart c1= new Chart(l1);
		
		ChartSolver.solve(g1,c1)
		
		DomGraph sf = new DomGraph();
		NodeLabels l2 = new NodeLabels();
		makeBadSolvedForm(sf,l2);
		
		Chart c2 = new Chart(l2);
		ChartSolver.solve(sf,c2);
		
		assert ModelCheck.subsumes(c1,g1,l1,c2,sf,l2) == false;
	}
	
	@Test
	public void testChainEqualsRenamedChain() {
		DomGraph g1 = new DomGraph();
		NodeLabels l1 = new NodeLabels();
		makeChain4(g1,l1);
		
		Chart c1= new Chart(l1);
		
		ChartSolver.solve(g1,c1)
		
		DomGraph sf = new DomGraph();
		NodeLabels l2 = new NodeLabels();
		makeRenamedChain4(sf,l2);
		
		Chart c2 = new Chart(l2);
		ChartSolver.solve(sf,c2);
		
		assert ModelCheck.equals(c1,g1,l1,c2,sf,l2) == true;
	}
	
	
	@Test
	public void testRecognizeWrongVariableBinding() {
		DomGraph g1 = new DomGraph();
		NodeLabels l1 = new NodeLabels();
		makeVariableBoundChain4(g1,l1);
		
		Chart c1 = new Chart(l1);
		
		ChartSolver.solve(g1,c1)
		
		DomGraph sf = new DomGraph();
		NodeLabels l2 = new NodeLabels();
		makeWrongVariableBoundChain4(sf,l2);
		
		Chart c2 = new Chart(l2);
		ChartSolver.solve(sf,c2);
		
		assert ModelCheck.equals(c1,g1,l1,c2,sf,l2) == false;
	}
	
	/**
	 * TODO create testcase for wrong variable match
	 **/
	
	  void makeChain4(DomGraph graph, NodeLabels labels) {
        graph.clear();
        labels.clear();

        graph.addNode("y0", new NodeData(NodeType.LABELLED));
        labels.addLabel("y0", "a0");
        graph.addNode("x1", new NodeData(NodeType.LABELLED));
        labels.addLabel("x1", "f1");
        graph.addNode("xl1", new NodeData(NodeType.UNLABELLED));
        graph.addNode("xr1", new NodeData(NodeType.UNLABELLED));
        graph.addNode("y1", new NodeData(NodeType.LABELLED));
        labels.addLabel("y1", "a1");
        graph.addNode("x2", new NodeData(NodeType.LABELLED));
        labels.addLabel("x2", "f2");
        graph.addNode("xl2", new NodeData(NodeType.UNLABELLED));
        graph.addNode("xr2", new NodeData(NodeType.UNLABELLED));
        graph.addNode("y2", new NodeData(NodeType.LABELLED));
        labels.addLabel("y2", "a2");
        graph.addNode("x3", new NodeData(NodeType.LABELLED));
        labels.addLabel("x3", "f3");
        graph.addNode("xl3", new NodeData(NodeType.UNLABELLED));
        graph.addNode("xr3", new NodeData(NodeType.UNLABELLED));
        graph.addNode("y3", new NodeData(NodeType.LABELLED));
        labels.addLabel("y3", "a3");
        graph.addNode("x4", new NodeData(NodeType.LABELLED));
        labels.addLabel("x4", "f4");
        graph.addNode("xl4", new NodeData(NodeType.UNLABELLED));
        graph.addNode("xr4", new NodeData(NodeType.UNLABELLED));
        graph.addNode("y4", new NodeData(NodeType.LABELLED));
        labels.addLabel("y4", "a4");

        graph.addEdge("x1", "xl1", new EdgeData(EdgeType.TREE));
        graph.addEdge("x1", "xr1", new EdgeData(EdgeType.TREE));
        graph.addEdge("xl1", "y0", new EdgeData(EdgeType.DOMINANCE));
        graph.addEdge("xr1", "y1", new EdgeData(EdgeType.DOMINANCE));
        graph.addEdge("x2", "xl2", new EdgeData(EdgeType.TREE));
        graph.addEdge("x2", "xr2", new EdgeData(EdgeType.TREE));
        graph.addEdge("xl2", "y1", new EdgeData(EdgeType.DOMINANCE));
        graph.addEdge("xr2", "y2", new EdgeData(EdgeType.DOMINANCE));
        graph.addEdge("x3", "xl3", new EdgeData(EdgeType.TREE));
        graph.addEdge("x3", "xr3", new EdgeData(EdgeType.TREE));
        graph.addEdge("xl3", "y2", new EdgeData(EdgeType.DOMINANCE));
        graph.addEdge("xr3", "y3", new EdgeData(EdgeType.DOMINANCE));
        graph.addEdge("x4", "xl4", new EdgeData(EdgeType.TREE));
        graph.addEdge("x4", "xr4", new EdgeData(EdgeType.TREE));
        graph.addEdge("xl4", "y3", new EdgeData(EdgeType.DOMINANCE));
        graph.addEdge("xr4", "y4", new EdgeData(EdgeType.DOMINANCE));
    }
	  
  void makeRenamedChain4(DomGraph graph, NodeLabels labels) {
			 graph.clear();
		        labels.clear();

		        graph.addNode("ht0", new NodeData(NodeType.LABELLED));
		        labels.addLabel("ht0", "a0");
		        graph.addNode("h1", new NodeData(NodeType.LABELLED));
		        labels.addLabel("h1", "f1");
		        graph.addNode("hl1", new NodeData(NodeType.UNLABELLED));
		        graph.addNode("hr1", new NodeData(NodeType.UNLABELLED));
		        graph.addNode("ht1", new NodeData(NodeType.LABELLED));
		        labels.addLabel("ht1", "a1");
		        graph.addNode("h2", new NodeData(NodeType.LABELLED));
		        labels.addLabel("h2", "f2");
		        graph.addNode("hl2", new NodeData(NodeType.UNLABELLED));
		        graph.addNode("hr2", new NodeData(NodeType.UNLABELLED));
		        graph.addNode("ht2", new NodeData(NodeType.LABELLED));
		        labels.addLabel("ht2", "a2");
		        graph.addNode("h3", new NodeData(NodeType.LABELLED));
		        labels.addLabel("h3", "f3");
		        graph.addNode("hl3", new NodeData(NodeType.UNLABELLED));
		        graph.addNode("hr3", new NodeData(NodeType.UNLABELLED));
		        graph.addNode("ht3", new NodeData(NodeType.LABELLED));
		        labels.addLabel("ht3", "a3");
		        graph.addNode("h4", new NodeData(NodeType.LABELLED));
		        labels.addLabel("h4", "f4");
		        graph.addNode("hl4", new NodeData(NodeType.UNLABELLED));
		        graph.addNode("hr4", new NodeData(NodeType.UNLABELLED));
		        graph.addNode("ht4", new NodeData(NodeType.LABELLED));
		        labels.addLabel("ht4", "a4");

		        graph.addEdge("h1", "hl1", new EdgeData(EdgeType.TREE));
		        graph.addEdge("h1", "hr1", new EdgeData(EdgeType.TREE));
		        graph.addEdge("hl1", "ht0", new EdgeData(EdgeType.DOMINANCE));
		        graph.addEdge("hr1", "ht1", new EdgeData(EdgeType.DOMINANCE));
		        graph.addEdge("h2", "hl2", new EdgeData(EdgeType.TREE));
		        graph.addEdge("h2", "hr2", new EdgeData(EdgeType.TREE));
		        graph.addEdge("hl2", "ht1", new EdgeData(EdgeType.DOMINANCE));
		        graph.addEdge("hr2", "ht2", new EdgeData(EdgeType.DOMINANCE));
		        graph.addEdge("h3", "hl3", new EdgeData(EdgeType.TREE));
		        graph.addEdge("h3", "hr3", new EdgeData(EdgeType.TREE));
		        graph.addEdge("hl3", "ht2", new EdgeData(EdgeType.DOMINANCE));
		        graph.addEdge("hr3", "ht3", new EdgeData(EdgeType.DOMINANCE));
		        graph.addEdge("h4", "hl4", new EdgeData(EdgeType.TREE));
		        graph.addEdge("h4", "hr4", new EdgeData(EdgeType.TREE));
		        graph.addEdge("hl4", "ht3", new EdgeData(EdgeType.DOMINANCE));
		        graph.addEdge("hr4", "ht4", new EdgeData(EdgeType.DOMINANCE));
		}
  
  
  
  void makeWrongVariableBoundChain4(DomGraph graph, NodeLabels labels) {
		 graph.clear();
	        labels.clear();

	        graph.addNode("ht0", new NodeData(NodeType.LABELLED));
	        labels.addLabel("ht0", "a0");
	        graph.addNode("h1", new NodeData(NodeType.LABELLED));
	        labels.addLabel("h1", "f1");
	        graph.addNode("hl1", new NodeData(NodeType.UNLABELLED));
	        graph.addNode("hr1", new NodeData(NodeType.UNLABELLED));
	        graph.addNode("ht1", new NodeData(NodeType.LABELLED));
	        labels.addLabel("ht1", "a1");
	        graph.addNode("h2", new NodeData(NodeType.LABELLED));
	        labels.addLabel("h2", "f2");
	        graph.addNode("hl2", new NodeData(NodeType.UNLABELLED));
	        graph.addNode("hr2", new NodeData(NodeType.UNLABELLED));
	        graph.addNode("ht2", new NodeData(NodeType.LABELLED));
	        labels.addLabel("ht2", "a2h14");
	        graph.addNode("h3", new NodeData(NodeType.LABELLED));
	        labels.addLabel("h3", "f3h14h7");
	        graph.addNode("hl3", new NodeData(NodeType.UNLABELLED));
	        graph.addNode("hr3", new NodeData(NodeType.UNLABELLED));
	        graph.addNode("ht3", new NodeData(NodeType.LABELLED));
	        labels.addLabel("ht3", "a3h7");
	        graph.addNode("h4", new NodeData(NodeType.LABELLED));
	        labels.addLabel("h4", "f4");
	        graph.addNode("hl4", new NodeData(NodeType.UNLABELLED));
	        graph.addNode("hr4", new NodeData(NodeType.UNLABELLED));
	        graph.addNode("ht4", new NodeData(NodeType.LABELLED));
	        labels.addLabel("ht4", "a4");

	        graph.addEdge("h1", "hl1", new EdgeData(EdgeType.TREE));
	        graph.addEdge("h1", "hr1", new EdgeData(EdgeType.TREE));
	        graph.addEdge("hl1", "ht0", new EdgeData(EdgeType.DOMINANCE));
	        graph.addEdge("hr1", "ht1", new EdgeData(EdgeType.DOMINANCE));
	        graph.addEdge("h2", "hl2", new EdgeData(EdgeType.TREE));
	        graph.addEdge("h2", "hr2", new EdgeData(EdgeType.TREE));
	        graph.addEdge("hl2", "ht1", new EdgeData(EdgeType.DOMINANCE));
	        graph.addEdge("hr2", "ht2", new EdgeData(EdgeType.DOMINANCE));
	        graph.addEdge("h3", "hl3", new EdgeData(EdgeType.TREE));
	        graph.addEdge("h3", "hr3", new EdgeData(EdgeType.TREE));
	        graph.addEdge("hl3", "ht2", new EdgeData(EdgeType.DOMINANCE));
	        graph.addEdge("hr3", "ht3", new EdgeData(EdgeType.DOMINANCE));
	        graph.addEdge("h4", "hl4", new EdgeData(EdgeType.TREE));
	        graph.addEdge("h4", "hr4", new EdgeData(EdgeType.TREE));
	        graph.addEdge("hl4", "ht3", new EdgeData(EdgeType.DOMINANCE));
	        graph.addEdge("hr4", "ht4", new EdgeData(EdgeType.DOMINANCE));
	}
  
  
  void makeVariableBoundChain4(DomGraph graph, NodeLabels labels) {
		 graph.clear();
	        labels.clear();

	        graph.addNode("ht0", new NodeData(NodeType.LABELLED));
	        labels.addLabel("ht0", "a0");
	        graph.addNode("h1", new NodeData(NodeType.LABELLED));
	        labels.addLabel("h1", "f1");
	        graph.addNode("hl1", new NodeData(NodeType.UNLABELLED));
	        graph.addNode("hr1", new NodeData(NodeType.UNLABELLED));
	        graph.addNode("ht1", new NodeData(NodeType.LABELLED));
	        labels.addLabel("ht1", "a1");
	        graph.addNode("h2", new NodeData(NodeType.LABELLED));
	        labels.addLabel("h2", "f2");
	        graph.addNode("hl2", new NodeData(NodeType.UNLABELLED));
	        graph.addNode("hr2", new NodeData(NodeType.UNLABELLED));
	        graph.addNode("ht2", new NodeData(NodeType.LABELLED));
	        labels.addLabel("ht2", "a2h7");
	        graph.addNode("h3", new NodeData(NodeType.LABELLED));
	        labels.addLabel("h3", "f3h14h7");
	        graph.addNode("hl3", new NodeData(NodeType.UNLABELLED));
	        graph.addNode("hr3", new NodeData(NodeType.UNLABELLED));
	        graph.addNode("ht3", new NodeData(NodeType.LABELLED));
	        labels.addLabel("ht3", "a3h14");
	        graph.addNode("h4", new NodeData(NodeType.LABELLED));
	        labels.addLabel("h4", "f4");
	        graph.addNode("hl4", new NodeData(NodeType.UNLABELLED));
	        graph.addNode("hr4", new NodeData(NodeType.UNLABELLED));
	        graph.addNode("ht4", new NodeData(NodeType.LABELLED));
	        labels.addLabel("ht4", "a4");

	        graph.addEdge("h1", "hl1", new EdgeData(EdgeType.TREE));
	        graph.addEdge("h1", "hr1", new EdgeData(EdgeType.TREE));
	        graph.addEdge("hl1", "ht0", new EdgeData(EdgeType.DOMINANCE));
	        graph.addEdge("hr1", "ht1", new EdgeData(EdgeType.DOMINANCE));
	        graph.addEdge("h2", "hl2", new EdgeData(EdgeType.TREE));
	        graph.addEdge("h2", "hr2", new EdgeData(EdgeType.TREE));
	        graph.addEdge("hl2", "ht1", new EdgeData(EdgeType.DOMINANCE));
	        graph.addEdge("hr2", "ht2", new EdgeData(EdgeType.DOMINANCE));
	        graph.addEdge("h3", "hl3", new EdgeData(EdgeType.TREE));
	        graph.addEdge("h3", "hr3", new EdgeData(EdgeType.TREE));
	        graph.addEdge("hl3", "ht2", new EdgeData(EdgeType.DOMINANCE));
	        graph.addEdge("hr3", "ht3", new EdgeData(EdgeType.DOMINANCE));
	        graph.addEdge("h4", "hl4", new EdgeData(EdgeType.TREE));
	        graph.addEdge("h4", "hr4", new EdgeData(EdgeType.TREE));
	        graph.addEdge("hl4", "ht3", new EdgeData(EdgeType.DOMINANCE));
	        graph.addEdge("hr4", "ht4", new EdgeData(EdgeType.DOMINANCE));
	}
  
  public static void makeGoodSolvedForm(DomGraph graph, NodeLabels labels) {
	  graph.clear();
        labels.clear();

        graph.addNode("z0", new NodeData(NodeType.LABELLED));
        labels.addLabel("z0", "a0");
        graph.addNode("v1", new NodeData(NodeType.LABELLED));
        labels.addLabel("v1", "f1");
        graph.addNode("vl1", new NodeData(NodeType.UNLABELLED));
        graph.addNode("vr1", new NodeData(NodeType.UNLABELLED));
        graph.addNode("z1", new NodeData(NodeType.LABELLED));
        labels.addLabel("z1", "a1");
        graph.addNode("v2", new NodeData(NodeType.LABELLED));
        labels.addLabel("v2", "f2");
        graph.addNode("vl2", new NodeData(NodeType.UNLABELLED));
        graph.addNode("vr2", new NodeData(NodeType.UNLABELLED));
        graph.addNode("z2", new NodeData(NodeType.LABELLED));
        labels.addLabel("z2", "a2");
        graph.addNode("v3", new NodeData(NodeType.LABELLED));
        labels.addLabel("v3", "f3");
        graph.addNode("vl3", new NodeData(NodeType.UNLABELLED));
        graph.addNode("vr3", new NodeData(NodeType.UNLABELLED));
        graph.addNode("z3", new NodeData(NodeType.LABELLED));
        labels.addLabel("z3", "a3");
        graph.addNode("v4", new NodeData(NodeType.LABELLED));
        labels.addLabel("v4", "f4");
        graph.addNode("vl4", new NodeData(NodeType.UNLABELLED));
        graph.addNode("vr4", new NodeData(NodeType.UNLABELLED));
        graph.addNode("z4", new NodeData(NodeType.LABELLED));
        labels.addLabel("z4", "a4");

        graph.addEdge("v1", "vl1", new EdgeData(EdgeType.TREE));
        graph.addEdge("v1", "vr1", new EdgeData(EdgeType.TREE));
        graph.addEdge("v2", "vl2", new EdgeData(EdgeType.TREE));
        graph.addEdge("v2", "vr2", new EdgeData(EdgeType.TREE));
        graph.addEdge("v3", "vl3", new EdgeData(EdgeType.TREE));
        graph.addEdge("v3", "vr3", new EdgeData(EdgeType.TREE));
        graph.addEdge("v4", "vl4", new EdgeData(EdgeType.TREE));
        graph.addEdge("v4", "vr4", new EdgeData(EdgeType.TREE));
        graph.addEdge("vl1", "z0", new EdgeData(EdgeType.DOMINANCE));
        graph.addEdge("vr1", "v4", new EdgeData(EdgeType.DOMINANCE));
        graph.addEdge("vr4", "z4", new EdgeData(EdgeType.DOMINANCE));
        graph.addEdge("vl4", "v2", new EdgeData(EdgeType.DOMINANCE));
        graph.addEdge("vl2", "z1", new EdgeData(EdgeType.DOMINANCE));
        graph.addEdge("vr2", "v3", new EdgeData(EdgeType.DOMINANCE));
        graph.addEdge("vl3", "z2", new EdgeData(EdgeType.DOMINANCE));
        graph.addEdge("vr3", "z3", new EdgeData(EdgeType.DOMINANCE));

    }
  
  public static void makeBadSolvedForm(DomGraph graph, NodeLabels labels) {
	  graph.clear();
        labels.clear();

        graph.addNode("z0", new NodeData(NodeType.LABELLED));
        labels.addLabel("z0", "o0");
        graph.addNode("v1", new NodeData(NodeType.LABELLED));
        labels.addLabel("v1", "f1");
        graph.addNode("vl1", new NodeData(NodeType.UNLABELLED));
        graph.addNode("vr1", new NodeData(NodeType.UNLABELLED));
        graph.addNode("z1", new NodeData(NodeType.LABELLED));
        labels.addLabel("z1", "n1");
        graph.addNode("v2", new NodeData(NodeType.LABELLED));
        labels.addLabel("v2", "f2");
        graph.addNode("vl2", new NodeData(NodeType.UNLABELLED));
        graph.addNode("vr2", new NodeData(NodeType.UNLABELLED));
        graph.addNode("z2", new NodeData(NodeType.LABELLED));
        labels.addLabel("z2", "a2");
        graph.addNode("v3", new NodeData(NodeType.LABELLED));
        labels.addLabel("v3", "f3");
        graph.addNode("vl3", new NodeData(NodeType.UNLABELLED));
        graph.addNode("vr3", new NodeData(NodeType.UNLABELLED));
        graph.addNode("z3", new NodeData(NodeType.LABELLED));
        labels.addLabel("z3", "a3");
        graph.addNode("v4", new NodeData(NodeType.LABELLED));
        labels.addLabel("v4", "f4");
        graph.addNode("vl4", new NodeData(NodeType.UNLABELLED));
        graph.addNode("vr4", new NodeData(NodeType.UNLABELLED));
        graph.addNode("z4", new NodeData(NodeType.LABELLED));
        labels.addLabel("z4", "a4");

        graph.addEdge("v1", "vl1", new EdgeData(EdgeType.TREE));
        graph.addEdge("v1", "vr1", new EdgeData(EdgeType.TREE));
        graph.addEdge("v2", "vl2", new EdgeData(EdgeType.TREE));
        graph.addEdge("v2", "vr2", new EdgeData(EdgeType.TREE));
        graph.addEdge("v3", "vl3", new EdgeData(EdgeType.TREE));
        graph.addEdge("v3", "vr3", new EdgeData(EdgeType.TREE));
        graph.addEdge("v4", "vl4", new EdgeData(EdgeType.TREE));
        graph.addEdge("v4", "vr4", new EdgeData(EdgeType.TREE));
        graph.addEdge("vl1", "z0", new EdgeData(EdgeType.DOMINANCE));
        graph.addEdge("vr1", "v4", new EdgeData(EdgeType.DOMINANCE));
        graph.addEdge("vr4", "z4", new EdgeData(EdgeType.DOMINANCE));
        graph.addEdge("vl4", "v2", new EdgeData(EdgeType.DOMINANCE));
        graph.addEdge("vl2", "z1", new EdgeData(EdgeType.DOMINANCE));
        graph.addEdge("vr2", "v3", new EdgeData(EdgeType.DOMINANCE));
        graph.addEdge("vl3", "z2", new EdgeData(EdgeType.DOMINANCE));
        graph.addEdge("vr3", "z3", new EdgeData(EdgeType.DOMINANCE));
        
        

    }
  
		
}