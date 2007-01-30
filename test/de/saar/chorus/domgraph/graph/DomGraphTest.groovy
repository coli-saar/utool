package de.saar.chorus.domgraph.graph;

import de.saar.chorus.domgraph.codec.*;
import de.saar.chorus.domgraph.codec.domcon.*;

import java.util.*;


class DomGraphTest extends GroovyTestCase {
	 
	        private DomGraph graph;
	        private NodeLabels labels;
	        private InputCodec ozcodec;
	        
	        // @Configuration(beforeTestMethod = true)
	        public void setUp() {
	            ozcodec = new DomconOzInputCodec();
	            graph = new DomGraph();
	            labels = new NodeLabels();
	        }

	        // unrestricted wccs, 1 wcc
	        public void testWccsTest1() throws Exception {
	             ozcodec.decode(
	                     new StringReader("[label(a f(b c)) dom(b d) dom(e a) label(c g(f)) " 
	                             + "dom(f g) dom(h g)]"),
	                     graph, labels);
	             
	             assert new HashSet(graph.wccs()).
	             	equals(new HashSet([new HashSet(["a", "b", "c", "d", "e", "f", "g", "h"])]));
	             
	             
	             
	             /*
	             
	             
	             Set<Set<String>> result = new HashSet<Set<String>>(graph.wccs());
	             
	             Set<String> goldWcc = 
	                 TestTools.makeSet(new String[] { "a", "b", "c", "d", "e", "f", "g", "h" });
	             Set<Set<String>> gold = new HashSet<Set<String>>(1);
	             gold.add(goldWcc);
	             
	             assert result.equals(gold);
	             */
	        }

	        // unrestricted wccs, 2 wccs
	        public void testWccsTest2() throws Exception {
	            ozcodec.decode(
	                    new StringReader("[label(a f(b c)) dom(b d) dom(e a) label(c g(f)) " 
	                            + " dom(h g)]"),
	                    graph, labels);
	            
	            assert new HashSet(graph.wccs()).
	            	equals(new HashSet([new HashSet(["a", "b", "c", "d", "e", "f"]), new HashSet(["g", "h"])]))
	            
	            /*
	            Set<Set<String>> result = new HashSet<Set<String>>(graph.wccs());
	            
	            Set gold = //(Set<Set<String>>)
	                TestTools.makeSet(new Set[] {
	                        TestTools.makeSet(new String[] { "a", "b", "c", "d", "e", "f" }),
	                        TestTools.makeSet(new String[] { "g", "h" })
	                });
	            
	            assert result.equals(gold);
	            */
	       }
	        
	        // restricted wccs, 2 wccs
	        public void testRestrictedWccs2() throws Exception {
	           ozcodec.decode(
	                   new StringReader("[label(a f(b c)) dom(b d) dom(e a) label(c g(f)) " 
	                           + " dom(f g) dom(h g)]"),
	                   graph, labels);
	           
	           assert new HashSet(graph.wccs(new HashSet(["a", "b", "c", "d", "e", "g", "h"]))).
	           		equals(new HashSet([new HashSet(["a", "b", "c", "d", "e"]), new HashSet(["g", "h"])]))
	           
	           		/*
	           Set<Set<String>> result = 
	               new HashSet<Set<String>>(graph.wccs(TestTools.makeSet(new String[] { "a", "b", "c", "d", "e", "g", "h" })));
	           
	           Set gold = // (Set<Set<String>>)
	           TestTools.makeSet(new Set[] {
	                   TestTools.makeSet(new String[] { "a", "b", "c", "d", "e" }),
	                   TestTools.makeSet(new String[] { "g", "h" })
	           });
	       
	           assert result.equals(gold);
	           */
	       }
	       
	        // restricted wccs (deletes second wcc -> 1 wcc left)
	        public void testRestrictedWccs1() throws Exception {
	           ozcodec.decode(
	                   new StringReader("[label(a f(b c)) dom(b d) dom(e a) label(c g(f)) " 
	                           + " dom(h g)]"),
	                   graph, labels);
	           
	           assert new HashSet(graph.wccs(new HashSet(["a", "b", "c", "d", "e", "f"]))).
	           		equals(new HashSet([new HashSet(["a", "b", "c", "d", "e", "f"])]))
	           
	           /*
	           Set<Set<String>> result = 
	               new HashSet<Set<String>>(graph.wccs(TestTools.makeSet(new String[] { "a", "b", "c", "d", "e", "f" })));
	           
	           Set gold =  // (Set<Set<String>>)
	           TestTools.makeSet(new Set[] {
	                   TestTools.makeSet(new String[] { "a", "b", "c", "d", "e", "f" }),
	           });
	       
	           assert result.equals(gold);
	           */
	       }

	        // wccs restricted to empty node set -> empty wcc set
	        public void testRestrictedWccs0() throws Exception {
	           ozcodec.decode(
	                   new StringReader("[label(a f(b c)) dom(b d) dom(e a) label(c g(f)) " 
	                           + " dom(h g)]"),
	                   graph, labels);
	           
	           assert graph.wccs(new HashSet([])).isEmpty();
	           	
	           
	           		/*
	           Set<Set<String>> result = 
	               new HashSet<Set<String>>(graph.wccs(new HashSet<String>()));
	           
	           assert result.isEmpty();
	           */
	       }

	    

}
