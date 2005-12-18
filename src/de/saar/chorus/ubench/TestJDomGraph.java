/*
 * Created on 28.07.2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package de.saar.chorus.ubench;

import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import org.jgraph.graph.AttributeMap;
import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.GraphConstants;
import org.jgraph.util.JGraphUtilities;

/**
 * @author Alexander Koller
 *
 */
public class TestJDomGraph extends TestCase {
    
    /*
     * TODO Testcases:
     *  - computeFragments
     */

    private JDomGraph graph;
    private Set<String> expectedNodeNames;
    private Set<String> expectedEdgeNames;
    
	/*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		graph = new JDomGraph();
		
		expectedNodeNames = new HashSet<String>();
	    expectedNodeNames.add("X");
	    expectedNodeNames.add("X1");
	    expectedNodeNames.add("Y");
	    
	    expectedEdgeNames = new HashSet<String>();
	    expectedEdgeNames.add("x-x1");
	    expectedEdgeNames.add("x1-y");
	}
	
	public void testInitiallyEmpty() {
	    assertEquals(0, graph.getNodes().size());
	}
	
	
	
	
	/** sample data contains correct nodes and edges **/
	
	public void testSampledataNodeCount() {
	    graph.addSampleData();
	    assertEquals("Sample data contains 3 nodes", 3, graph.getNodes().size());
	}

	public void testSampledataNodeNames() {
	    graph.addSampleData();
	    
	    Set<DefaultGraphCell> nodes = graph.getNodes();
	    for( DefaultGraphCell node : nodes ) {
	        NodeData data = (NodeData) node.getUserObject();
	        
	        expectedNodeNames.remove(data.getName());
	    }
	    
	    assertTrue(expectedNodeNames.isEmpty());
	}
	
	public void testSampledataEdgeCount() {
	    graph.addSampleData();
	    assertEquals(2, graph.getEdges().size());
	}
	
	public void testSampledataEdgeNames() {
	    graph.addSampleData();
	    
	    Set<DefaultEdge> edges = graph.getEdges();
	    for( DefaultEdge edge : edges ) {
	        expectedEdgeNames.remove(graph.getEdgeData(edge).getName());
	    }
	    
	    assertTrue(expectedEdgeNames.isEmpty());
	}	
	
	
	/** my own nodes and edges fields reflect the true graph model correctly **/
	
	private void myNodesAreCorrect() {
	    // nodes field == nodes in graph model
	    Set<DefaultGraphCell> nodes = graph.getNodes();
	    Object[] modelNodes = JGraphUtilities.getVertices(graph, JGraphUtilities.getAll(graph));
	    
	    assertEquals("Same number of nodes", modelNodes.length, nodes.size());
	    
	    Set<String> myNames = new HashSet<String>();
	    for( DefaultGraphCell node : nodes ) {
	        myNames.add(graph.getNodeData(node).getName());
	    }
	    
	    for( int i = 0; i < modelNodes.length; i++ ) {
	        DefaultGraphCell node = (DefaultGraphCell) modelNodes[i];
	        String name = graph.getNodeData(node).getName();
	        
	        myNames.remove(name);
	    }
	    
	    assertTrue("Same node names", myNames.isEmpty());	    
	}
	
	public void testMyNodesAreCorrect() {
	    graph.addSampleData();
	    
	    myNodesAreCorrect();
	}
	
	private void myEdgesAreCorrect() {
	   Set<DefaultEdge> edges = graph.getEdges();
	    Object[] modelEdges = JGraphUtilities.getEdges(graph);
	    
	    assertEquals("Same number of edges", modelEdges.length, edges.size());
	    
	    Set<String> myNames = new HashSet<String>();
	    for( DefaultEdge edge : edges ) {
	        myNames.add(graph.getEdgeData(edge).getName() );
	    }
	    
	    for( int i = 0; i < modelEdges.length; i++ ) {
	        DefaultEdge edge = (DefaultEdge) modelEdges[i];
	        myNames.remove(graph.getEdgeData(edge).getName());
	    }
	    
	    assertTrue("Same edge names", myNames.isEmpty());
	}
	
	public void testMyEdgesAreCorrect() {
	    graph.addSampleData();
	    myEdgesAreCorrect();
	}
	
	
	
	/** accessor functions for nodes and edges are correct **/
	
	public void testGetNodeData() {
	    graph.addSampleData();
	    
	    DefaultGraphCell someNode = graph.getNodes().iterator().next();
	    assertEquals(graph.getNodeData(someNode),
	            	 someNode.getUserObject());
	}
	
	public void testGetEdgeData() {
	    graph.addSampleData();
	    
	    DefaultEdge someEdge = graph.getEdges().iterator().next();
	    assertEquals(graph.getEdgeData(someEdge), someEdge.getUserObject());
	}
	
	public void testGetNodeForName() {
	    graph.addSampleData();
	    
	    DefaultGraphCell x = graph.getNodeForName("X");
	    
	    assertTrue("Node is not null", x != null );
	    assertTrue("Node has correct name", graph.getNodeData(x).getName().equals("X"));
	}
	
	
	/** adding nodes and edges**/
	
	public void testAddNode() {
	    graph.addSampleData();
	    
	    NodeData data = new NodeData(NodeType.labelled, "z", "ff", graph);
	    DefaultGraphCell newNode = graph.addNode(data);
	    
	    AttributeMap attrs = newNode.getAttributes();
	    
	    // new node is in the graph (graph model and my own model)
	    assertEquals("Graph with new node now has 4 nodes", 4, graph.getNodes().size());
	    myNodesAreCorrect();
	    
	    // new node has attributes (checking "correct" attributes seems pointless)
	    assertTrue("New node has attributes", attrs != null );
	    
	    // new node has correct data
	    assertTrue("New node has correct NodeData", graph.getNodeData(newNode) == data);
	}

	private void newEdgeIsCorrect(DefaultEdge newEdge, EdgeData data) {
	    // new edge is in the graph (graph model and my own model)
	    assertEquals("Graph with new edge now has 3 edges", 3, graph.getEdges().size());
	    myEdgesAreCorrect();

	    // new edge has correct data
	    assertTrue("New edge has correct EdgeData", graph.getEdgeData(newEdge) == data);
	}
	
	public void testAddSolidEdge() {
	    graph.addSampleData();
	    
	    EdgeData data = new EdgeData(EdgeType.solid, "edge1", graph);
	    DefaultEdge newEdge = graph.addEdge(data, graph.getNodeForName("X1"), graph.getNodeForName("Y"));
	    
	    newEdgeIsCorrect(newEdge, data);
	    
	    // new edge has correct attributes
	    AttributeMap map = newEdge.getAttributes();
	    
	    assertTrue("New edge has attributes", map != null );
	    assertTrue("New edge is solid", GraphConstants.getDashPattern(map) == null);
	}
	
	public void testAddDomEdge() {
	    graph.addSampleData();
	    
	    EdgeData data = new EdgeData(EdgeType.dominance, "edge2", graph);
	    DefaultEdge newEdge = graph.addEdge(data, graph.getNodeForName("Y"), graph.getNodeForName("X"));
	    
	    newEdgeIsCorrect(newEdge, data);
	    
	    // new edge has correct attributes
	    AttributeMap map = newEdge.getAttributes();
	    
	    assertTrue("New edge has attributes", map != null );
	    
	    float[] pattern = GraphConstants.getDashPattern(map);
	    assertTrue("New edge has dash pattern", pattern != null);
	    assertEquals("Dash pattern has size 2", 2, pattern.length);
	    assertTrue("Dash pattern is correct", (pattern[0] == 3) && (pattern[1] == 3) );
	}



	/** fragments **/
	
	public void testComputeFragments() {
	    graph.addSampleData();
	    graph.computeFragments();
	    
	    Set<Fragment> frags = graph.getFragments();
	    
	    assertEquals("Graph has two fragments", 2, frags.size());

	    boolean[] foundFragment = new boolean[2];
	    foundFragment[0] = foundFragment[1] = false;
	    
	    for( Fragment f : frags ) {
	        Set<DefaultGraphCell> nodes = f.getNodes();
	        
	        // TODO Could perform more sophisticated fragment checking here
	        
	        switch(nodes.size()) {
	        	case 1: foundFragment[0] = true; 
	        	break;
	        	
	        	case 2: foundFragment[1] = true;
	        	break;
	        }
	    }
	    
	    assertTrue("Graph has the correct fragments", foundFragment[0] && foundFragment[1]);
	}
}
