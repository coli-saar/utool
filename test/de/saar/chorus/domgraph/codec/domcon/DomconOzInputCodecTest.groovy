package de.saar.chorus.domgraph.codec.domcon;

import de.saar.chorus.domgraph.graph.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

class DomconOzInputCodecTest  {
    @Test
	public void testTrivialDomEdge() throws Exception {
		DomGraph graph = new DomGraph();
		NodeLabels labels = new NodeLabels();
		
		DomGraph myGraph = new DomGraph();
		NodeLabels myLabels = new NodeLabels();
		
		DomconOzInputCodec codec = new DomconOzInputCodec();
		codec.decode(new StringReader("[label(x f(y)) dom(x y)]"),
		 myGraph, myLabels);
		
		assert myGraph.outdeg("x", EdgeType.TREE) == 1;
		assert myGraph.outdeg("x", EdgeType.DOMINANCE) == 1;

		graph.addNode("x", new NodeData(NodeType.LABELLED));
		labels.addLabel("x", "f");
		graph.addNode("y", new NodeData(NodeType.UNLABELLED));
		graph.addEdge("x", "y", new EdgeData(EdgeType.TREE));
		graph.addEdge("x", "y", new EdgeData(EdgeType.DOMINANCE));
		
		assert DomGraph.isEqual(graph, labels, myGraph, myLabels);
	}
	
    @Test
	public void testChain3() throws Exception {
    		DomGraph graph = new DomGraph();
    		NodeLabels labels = new NodeLabels();
    		
    		DomGraph myGraph = new DomGraph();
    		NodeLabels myLabels = new NodeLabels();
    		
    		DomconOzInputCodec codec = new DomconOzInputCodec();
    		codec.decode(new StringReader("[label(y0 a0) label(x1 f1(xl1 xr1)) label(y1 a1) label(x2 f2(xl2 xr2))\n"
    		 + "label(y2 a2) label(x3 f3(xl3 xr3)) label(y3 a3) dom(xl1 y0) \n"
    		 + "dom(xr1 y1) dom(xl2 y1) dom(xr2 y2) dom(xl3 y2) dom(xr3 y3)]"),
    		 myGraph, myLabels);
    		
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
	        
	        assert DomGraph.isEqual(graph, labels, myGraph, myLabels);
    	}
    
    @Test
	public void testEmptyGraph() {
		DomGraph graph = new DomGraph();
		NodeLabels labels = new NodeLabels();
		
		DomGraph myGraph = new DomGraph();
		NodeLabels myLabels = new NodeLabels();
		
		DomconOzInputCodec codec = new DomconOzInputCodec();
		codec.decode(new StringReader("[]"), myGraph, myLabels);
		
		assert DomGraph.isEqual(graph, labels, myGraph, myLabels);
	}
}
