package de.saar.chorus.domgraph.graph;

import de.saar.chorus.domgraph.codec.*;
import de.saar.chorus.domgraph.codec.domcon.*;
import de.saar.chorus.domgraph.chart.*;

import java.util.*;

import de.saar.testingtools.*;


class DomGraphTest extends GroovyTestCase {
	 
	        private DomGraph graph;
	        private NodeLabels labels;
	        private InputCodec ozcodec;
	        private Chart chart;
	        
	        // @Configuration(beforeTestMethod = true)
	        public void setUp() {
	            ozcodec = new DomconOzInputCodec();
	            graph = new DomGraph();
	            labels = new NodeLabels();
	            chart = new Chart();
	        }

	        // unrestricted wccs, 1 wcc
	        public void testWccsTest1() throws Exception {
	             ozcodec.decode(
	                     new StringReader("[label(a f(b c)) dom(b d) dom(e a) label(c g(f)) " 
	                             + "dom(f g) dom(h g)]"),
	                     graph, labels);
	             
	             assert new HashSet(graph.wccs()).
	             	equals(new HashSet([new HashSet(["a", "b", "c", "d", "e", "f", "g", "h"])]));
	        }

	        // unrestricted wccs, 2 wccs
	        public void testWccsTest2() throws Exception {
	            ozcodec.decode(
	                    new StringReader("[label(a f(b c)) dom(b d) dom(e a) label(c g(f)) " 
	                            + " dom(h g)]"),
	                    graph, labels);
	            
	            assert new HashSet(graph.wccs()).
	            	equals(new HashSet([new HashSet(["a", "b", "c", "d", "e", "f"]), new HashSet(["g", "h"])]))
	       }
	        
	        // restricted wccs, 2 wccs
	        public void testRestrictedWccs2() throws Exception {
	           ozcodec.decode(
	                   new StringReader("[label(a f(b c)) dom(b d) dom(e a) label(c g(f)) " 
	                           + " dom(f g) dom(h g)]"),
	                   graph, labels);
	           
	           assert new HashSet(graph.wccs(new HashSet(["a", "b", "c", "d", "e", "g", "h"]))).
	           		equals(new HashSet([new HashSet(["a", "b", "c", "d", "e"]), new HashSet(["g", "h"])]))
	       }
	       
	        // restricted wccs (deletes second wcc -> 1 wcc left)
	        public void testRestrictedWccs1() throws Exception {
	           ozcodec.decode(
	                   new StringReader("[label(a f(b c)) dom(b d) dom(e a) label(c g(f)) " 
	                           + " dom(h g)]"),
	                   graph, labels);
	           
	           assert new HashSet(graph.wccs(new HashSet(["a", "b", "c", "d", "e", "f"]))).
	           		equals(new HashSet([new HashSet(["a", "b", "c", "d", "e", "f"])]))
	       }

	        // wccs restricted to empty node set -> empty wcc set
	        public void testRestrictedWccs0() throws Exception {
	           ozcodec.decode(
	                   new StringReader("[label(a f(b c)) dom(b d) dom(e a) label(c g(f)) " 
	                           + " dom(h g)]"),
	                   graph, labels);
	           
	           assert graph.wccs(new HashSet([])).isEmpty();
	       }

	        
	        
	        /*********** test cases for makeSolvedForm ***************/
	        public void testNormalMakeSolvedForms() {
	        	compareSolvedForms("[label(x f(x1)) dom(x1 y) label(y a)]",
	        			["[label(x f(x1)) dom(x1 y) label(y a)]"]);
	        }
	        
	        public void testCrossEdgeMakeSolvedForms() {
	        	compareSolvedForms("[label(x f(x1)) label(y g(y1)) label(z a) dom(x1 z) dom(y1 z) dom(y x1)]",
	        			["[label(x f(x1)) label(y g(y1)) label(z a) dom(y1 x) dom(x1 z)]",
	        			 "[label(x f(y)) label(y g(y1)) label(z a) dom(y1 z)]"]);
	        }
	        
	        public void testThreeUpperFragments() {
	        	compareSolvedForms("[label(x f(x1)) label(y g(y1)) label(z h(z1)) label(w a) dom(x y1) dom(y x1) dom(z y1) dom(y z1) dom(x1 w) dom(y1 w) dom(z1 w)]",
	        			["[label(x f(y)) label(y g(z)) label(z h(z1)) label(w a) dom(z1 w)]",
	        			 "[label(z h(y)) label(y g(x)) label(x f(x1)) label(w a) dom(x1 w)]"])
	        }
	        
	        
	        /*********** test cases for make(Weakly)NormalBackbone ***********/
	        public void testBackboneOfNormalGraph() {
	        	backboneTest("[label(x f(y)) dom(y z) label(z a)]", 
	        			"[label(x f(y)) dom(y z) label(z a)]", 
	        			"[label(x f(y)) dom(y z) label(z a)]");
	        }

	        public void testBackboneOfWeaklyNormalGraph() {
	        	backboneTest("[label(x f(y)) dom(y z) label(z a) dom(x w) label(w b)]",
	        			"[label(x f(y)) dom(y z) label(z a) dom(x w) label(w b)]",
	        			"[label(x f(y)) dom(y z) label(z a) label(w b)]");
	        }

	        public void testBackboneOfCrossEdgeGraph() {
	        	backboneTest("[label(x f(y)) dom(y z) label(z a) dom(x w) label(w b) label(x1 g(x2)) dom(x1 y)]",
	        			"[label(x f(y)) dom(y z) label(z a) dom(x w) label(w b) label(x1 g(x2)) ]",
	        			"[label(x f(y)) dom(y z) label(z a) label(w b) label(x1 g(x2))]");
	        }
	        
	        
	        /*********** test cases for graph classification ***********/
	        public void testWeaklyNormalEmptyTopFragment() {
	        	decode("[dom(x y) dom(x z) label(y a) label(z a)]");
	        	assert !graph.isWeaklyNormal();
	        }
	        
	        public void testWeaklyNormalEmptyFragment() {
	        	decode("[label(u f(v)) dom(v x) dom(x y) label(y a)]");
	        	assert !graph.isWeaklyNormal();
	        }

	        // In the old hnc test for unsolvable graphs (isHncSlow), the order in which dom edges
	        // were visited could prevent correct recognition of hnc (#222).
	        public void testUnsolvableHnc1() {
	        	decode("[label(n0 f(n1 n2)) label(n3 a) label(n4 b) dom(n1 n3) dom(n2 n4) dom(n1 n4)]");
	        	assert graph.isHypernormallyConnected();
	        }
	        
	        // In the old hnc test for unsolvable graphs (isHncSlow), the order in which dom edges
	        // were visited could prevent correct recognition of hnc (#222).
	        public void testUnsolvableHnc2() {
	        	decode("[label(n0 f(n1 n2)) label(n3 a) label(n4 b) dom(n1 n3) dom(n2 n4) dom(n2 n3)]");
	        	assert graph.isHypernormallyConnected();
	        }
	        
	        // This graph used to crash the hnc test because it runs into an endless loop trying to
	        // compute a normal backbone which is normal (ticket #270).
	        public void testNonNormalizableHnc() {
	        	decode("[label(x f(y)) label(y g(x))]");
	        	assert !graph.isHypernormallyConnected();
	        }
	        
	        public void testNotHnc1() {
	        	decode("[label(n0 f(n1 n2 n2b)) label(n3 a) label(n4 b) label(n5 c) dom(n1 n3) dom(n1 n4) dom(n2 n5) dom(n2b n5)]");
	        	assert !graph.isHypernormallyConnected();
	        }
	        
	        public void testEmptyFragment() {
	    		// a graph with an empty fragment
	    		decode("[label(x f(x1)) dom(x1 x2) dom(x2 y) label(y a)]");
	    		assert graph.hasEmptyFragments();
	    	}
	    	
	    	public void testWellFormed() {
	    		// a non-well-formed graph in the sense of Bodirsky et al. 04
	    		decode("[label(x a) label(y b) dom(x y)]");
	    		assert !graph.isWellFormed();
	    	}
	    	
	    	public void testWellFormed2() {
	    		// a non-well-formed graph in the sense of Bodirsky et al. 04
	    		decode("[label(x f(x1 x2)) label(x1 a1) label(x2 a2) label(y b) dom(x y)]");
	    		assert !graph.isWellFormed();
	    	}
	    	
			// ensure that the output codec refuses to encode graphs whose labelling information
	    	// is inconsistent with the NodeLabels object (see #169).
	    	public void testNonWellLabeled1() {
	    		graph.addNode("x", new NodeData(NodeType.LABELLED));
	    		graph.addNode("y", new NodeData(NodeType.LABELLED));
	    		graph.addNode("z", new NodeData(NodeType.LABELLED));
	    		
	    		graph.addEdge("x", "y", new EdgeData(EdgeType.TREE));
	    		graph.addEdge("y", "z", new EdgeData(EdgeType.DOMINANCE));
	    		
	    		labels.addLabel("x", "f");
	    		labels.addLabel("z", "a");
	    		
	    		assert !graph.isLabellingConsistent(labels);
	    		
	    	}
	    	
	    	public void testNonWellLabeled2() {
	    		graph.addNode("x", new NodeData(NodeType.UNLABELLED));
	    		graph.addNode("y", new NodeData(NodeType.UNLABELLED));
	    		graph.addNode("z", new NodeData(NodeType.LABELLED));
	    		
	    		graph.addEdge("x", "y", new EdgeData(EdgeType.TREE));
	    		graph.addEdge("y", "z", new EdgeData(EdgeType.DOMINANCE));
	    		
	    		labels.addLabel("x", "f");
	    		labels.addLabel("z", "a");
	    		
	    		assert !graph.isLabellingConsistent(labels);
	    	}
	    	
	    	public void testNonWellLabeled3() {
	    		graph.addNode("x", new NodeData(NodeType.LABELLED));
	    		graph.addNode("y", new NodeData(NodeType.UNLABELLED));
	    		graph.addNode("z", new NodeData(NodeType.LABELLED));
	    		
	    		graph.addEdge("x", "y", new EdgeData(EdgeType.TREE));
	    		graph.addEdge("y", "z", new EdgeData(EdgeType.DOMINANCE));
	    		
	    		labels.addLabel("x", "f");
	    		labels.addLabel("z", "a");
	    		
	    		assert graph.isLabellingConsistent(labels);
	    	}
	    	
	    	
	    	
	    	
	    	
	        
	        
	        //////////////////////////////////////////////////////////////////////////////////////////
	        
	        private void decode(String clls) {
	        	ozcodec.decode(new StringReader(clls), graph, labels);
	        }
	        
	        
	        private void backboneTest(String original, String wnBackbone, String nBackbone) {
	        	DomGraph originalGraph = new DomGraph(), nGraph = new DomGraph(), wnGraph = new DomGraph();
	        	NodeLabels labels = new NodeLabels();
	        	
	        	TestingTools.decodeDomcon(original, originalGraph, labels);
	        	
				DomGraph backup = (DomGraph) originalGraph.clone();
	        	nGraph = originalGraph.makeNormalBackbone();
	        	wnGraph = originalGraph.makeWeaklyNormalBackbone();
	        	
	        	TestingTools.assertDomgraphEquals(nGraph, labels, nBackbone, "normal backbone incorrect");
	        	TestingTools.assertDomgraphEquals(wnGraph, labels, wnBackbone, "weakly normal backbone incorrect");
	        	assert DomGraph.isEqual(originalGraph, labels, backup, labels) : "graph changed during backbone computation";
	        }
	        
	        
	        
	        private void compareSolvedForms(String graphstring, List gold_sf_strings) {
	        	TestingTools.decodeDomcon(graphstring, graph, labels);
	    		
	    		assert ChartSolver.solve(graph, chart) == true;
	    		
	    		SolvedFormIterator sfi = new SolvedFormIterator(chart, graph);
	    		List sfs = TestingTools.collectIteratorValues(sfi);
	    		
	    		List sfs_domgraphs = sfs.collect { graph.makeSolvedForm(it) };
	    		List sfs_nodelabels = sfs.collect { labels.makeSolvedForm(it) };
	    		
	    		List gold_domgraphs = new ArrayList();
	    		List gold_nodelabels = new ArrayList();
	    		makeGoldGraphs(gold_sf_strings, gold_domgraphs, gold_nodelabels);
	    		
	    		assert equalGraphs(sfs_domgraphs, sfs_nodelabels, gold_domgraphs, gold_nodelabels);
	        }
	        
	        private void makeGoldGraphs(List domcons, List domgraphs, List nodelabels) {
	        	domcons.each {
	        		DomGraph g = new DomGraph();
	        		NodeLabels l = new NodeLabels();
	        		
	        		TestingTools.decodeDomcon(it, g, l);
	        		
	        		domgraphs.add(g);
	        		nodelabels.add(l);
	        	}
	        }
	        
	        private boolean equalGraphs(List sfs_domgraphs, List sfs_nodelabels, List gold_domgraphs, List gold_nodelabels) {
	        	if( sfs_domgraphs.size() != gold_domgraphs.size() ) {
	        		return false;
	        	}
	        	
	        	for( i in 0..<(sfs_domgraphs.size()) ) {
	        		DomGraph sfs_graph = sfs_domgraphs.get(i);
	        		NodeLabels sfs_labels = sfs_nodelabels.get(i);
	        		boolean found = false;
	        		
	        		for( j in 0..<(gold_domgraphs.size()) ) {
	        			if( DomGraph.isEqual(sfs_graph, sfs_labels, gold_domgraphs.get(j), gold_nodelabels.get(j)) ) {
	        				found = true;
	        				break;
	        			}
	        		}
	        		
	        		if( !found ) {
	        			return false;
	        		}
	        	}
	        	
	        	return true;
	        }
	    

}
