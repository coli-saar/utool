package de.saar.chorus.domgraph.codec.basic;

import de.saar.chorus.domgraph.codec.*;
import de.saar.chorus.domgraph.graph.*;

import de.saar.testingtools.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

class ChainTest {
	 private InputCodec codec;
     private DomGraph graph;
     private NodeLabels labels;
     
     @Before
     public void setUp() {
         codec = new Chain();
         graph = new DomGraph();
         labels = new NodeLabels();
     }
     
     // reader construction
     @Test
     public void testOverridenReaderConstruction() throws Exception {
         assert codec.getReaderForSpecification("3").getClass() == StringReader.class;
     }
     
     @Test
     public void testReaderCorrectContents() throws Exception {
         Reader r = codec.getReaderForSpecification("-32aa7");
         StringBuffer buf = new StringBuffer();
         int c;
         
         while( (c = r.read()) != -1 ) {
             buf.append((char) c);
         }
         
         assert "-32aa7".equals(buf.toString());
     }
     
     
     // argument parsing
     @Test
     public void testNonNumericArgument() throws Exception {
    	 TestingTools.expectException(ParserException,
    			 { codec.decode(codec.getReaderForSpecification("xyzzy"), graph, labels) })
     }
     
     @Test
     public void testEmptyArgument() throws Exception {
    	 TestingTools.expectException(ParserException,
    			 { codec.decode(codec.getReaderForSpecification(""), graph, labels) })
     }
     
     @Test
     public void testNegativeArgument() throws Exception {
    	 TestingTools.expectException(MalformedDomgraphException,
				{ codec.decode(codec.getReaderForSpecification("-1"), graph, labels) })
     }

     @Test
     public void testZeroArgument() throws Exception {
    	 TestingTools.expectException(MalformedDomgraphException, 
				{ codec.decode(codec.getReaderForSpecification("0"), graph, labels) })
     }

     
     // generates correct graph and labels
     @Test
     public void testChain3correctness() throws Exception {
         DomGraph goldGraph = new DomGraph();
         NodeLabels goldLabels = new NodeLabels();
         
         codec.decode(codec.getReaderForSpecification("3"), graph, labels);
         
         goldGraph.addNode("y0", new NodeData(NodeType.LABELLED));
         goldLabels.addLabel("y0", "a0");
         goldGraph.addNode("x1", new NodeData(NodeType.LABELLED));
         goldLabels.addLabel("x1", "f1");
         goldGraph.addNode("xl1", new NodeData(NodeType.UNLABELLED));
         goldGraph.addNode("xr1", new NodeData(NodeType.UNLABELLED));
         goldGraph.addNode("y1", new NodeData(NodeType.LABELLED));
         goldLabels.addLabel("y1", "a1");
         goldGraph.addNode("x2", new NodeData(NodeType.LABELLED));
         goldLabels.addLabel("x2", "f2");
         goldGraph.addNode("xl2", new NodeData(NodeType.UNLABELLED));
         goldGraph.addNode("xr2", new NodeData(NodeType.UNLABELLED));
         goldGraph.addNode("y2", new NodeData(NodeType.LABELLED));
         goldLabels.addLabel("y2", "a2");
         goldGraph.addNode("x3", new NodeData(NodeType.LABELLED));
         goldLabels.addLabel("x3", "f3");
         goldGraph.addNode("xl3", new NodeData(NodeType.UNLABELLED));
         goldGraph.addNode("xr3", new NodeData(NodeType.UNLABELLED));
         goldGraph.addNode("y3", new NodeData(NodeType.LABELLED));
         goldLabels.addLabel("y3", "a3");

         goldGraph.addEdge("x1", "xl1", new EdgeData(EdgeType.TREE));
         goldGraph.addEdge("x1", "xr1", new EdgeData(EdgeType.TREE));
         goldGraph.addEdge("xl1", "y0", new EdgeData(EdgeType.DOMINANCE));
         goldGraph.addEdge("xr1", "y1", new EdgeData(EdgeType.DOMINANCE));
         goldGraph.addEdge("x2", "xl2", new EdgeData(EdgeType.TREE));
         goldGraph.addEdge("x2", "xr2", new EdgeData(EdgeType.TREE));
         goldGraph.addEdge("xl2", "y1", new EdgeData(EdgeType.DOMINANCE));
         goldGraph.addEdge("xr2", "y2", new EdgeData(EdgeType.DOMINANCE));
         goldGraph.addEdge("x3", "xl3", new EdgeData(EdgeType.TREE));
         goldGraph.addEdge("x3", "xr3", new EdgeData(EdgeType.TREE));
         goldGraph.addEdge("xl3", "y2", new EdgeData(EdgeType.DOMINANCE));
         goldGraph.addEdge("xr3", "y3", new EdgeData(EdgeType.DOMINANCE));
         
         assert DomGraph.isEqual(graph, labels, goldGraph, goldLabels);
     }
 }
