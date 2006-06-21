/*
 * @(#)Chain.java created 26.01.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.codec.basic;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.CharBuffer;

import org.testng.annotations.Configuration;
import org.testng.annotations.ExpectedExceptions;
import org.testng.annotations.Test;

import de.saar.chorus.domgraph.codec.InputCodec;
import de.saar.chorus.domgraph.codec.MalformedDomgraphException;
import de.saar.chorus.domgraph.codec.ParserException;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.EdgeData;
import de.saar.chorus.domgraph.graph.EdgeType;
import de.saar.chorus.domgraph.graph.NodeData;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.domgraph.graph.NodeType;


/**
 * An input codec that generates pure chains. Pure chains
 * are normal dominance graphs in which upper and lower fragments
 * alternate in a zig-zag shape (see e.g. <a href="http://www.coli.uni-saarland.de/~koller/showpaper.php?id=thesis">
 * Koller's Ph.D. thesis</a>, Chapter 6.4). They correspond to
 * the predicate-argument structure of (possibly nested) NPs
 * and (possibly transitive) verbs, and as such are a convenient
 * type of graphs for benchmarks.<p>
 * 
 * 
 * @author Alexander Koller
 *
 */
public class Chain extends InputCodec {
    public static String getName() {
        return "chain";
    }
    
    public static String getExtension() {
        return null;
    }
    
    public static int ERROR_CHAIN_LENGTH = 1;
    
    public void decode(Reader inputStream, DomGraph graph, NodeLabels labels) 
    throws IOException, ParserException, MalformedDomgraphException {
        CharBuffer buf = CharBuffer.allocate(10);
        int chainLength = -1;
        String lengthAsString;
        
        inputStream.read(buf);
        
        buf.limit(buf.position());
        buf.rewind();
        lengthAsString = buf.toString();
                
        try {
            chainLength = Integer.parseInt(lengthAsString);
        } catch(NumberFormatException e) {
            throw new ParserException(e);
        }

        if( chainLength < 1 ) {
            throw new MalformedDomgraphException("You must specify a numeric chain length of at least 1!", ERROR_CHAIN_LENGTH);
        }
        
        makeChain(chainLength, graph, labels);
    }
    
    /** 
     * Determines a <code>Reader</code> from which the USR specified
     * by the <code>spec</code> will be read. In the context of the
     * <code>Chain</code> input codec, the <code>spec</code> must be
     * a string representing a number, which is taken to specify
     * the length of the chain to generate. (This is different than
     * the implementation in {@link de.saar.chorus.domgraph.codec.InputCodec},
     * where <code>spec</code> is read as a filename.)
     * 
     * @param spec a string specifying the length of the chain
     * @return a <code>StringReader</code> for the given string
     */
    public Reader getReaderForSpecification(String spec) {
        return new StringReader(spec);
    }

    private void makeChain(int length, DomGraph graph, NodeLabels labels) {
    	String upper_root, upper_lefthole, upper_righthole;
    	String lower;
        
        
        graph.clear();
    	
    	lower = "y0";
    	graph.addNode("y0", new NodeData(NodeType.LABELLED));
    	labels.addLabel("y0", "a0");
    	
    	for( int i = 1; i <= length; i++ ) {
    		// upper fragment
    		upper_root = "x" + i;
    		upper_lefthole = "xl" + i;
    		upper_righthole = "xr" + i;
    		
    		graph.addNode(upper_root, new NodeData(NodeType.LABELLED));
    		labels.addLabel(upper_root, "f" + i);
    		
    		graph.addNode(upper_lefthole, new NodeData(NodeType.UNLABELLED));
    		graph.addNode(upper_righthole, new NodeData(NodeType.UNLABELLED));
    		
    		graph.addEdge(upper_root, upper_lefthole, new EdgeData(EdgeType.TREE));
    		graph.addEdge(upper_root, upper_righthole, new EdgeData(EdgeType.TREE));
    		
    		// dominance edge to previous lower fragment
    		graph.addEdge(upper_lefthole, lower, new EdgeData(EdgeType.DOMINANCE));
    		
    		// lower fragment
    		lower = "y" + i;
    		graph.addNode(lower, new NodeData(NodeType.LABELLED));
        	labels.addLabel(lower, "a" + i);
    		
    		// dominance edge to new lower fragment
        	graph.addEdge(upper_righthole, lower, new EdgeData(EdgeType.DOMINANCE));
    	}
    }
    
    
    
    
    /****************************************************************
     * UNIT TESTS
     ****************************************************************/
    

    @Test(groups = {"Domgraph"})
    public class UnitTests {
        private InputCodec codec;
        private DomGraph graph;
        private NodeLabels labels;
        
        @Configuration(beforeTestMethod = true)
        public void setup() {
            codec = new Chain();
            graph = new DomGraph();
            labels = new NodeLabels();
        }
        
        
        // reader construction
        public void overridenReaderConstruction() throws Exception {
            assert codec.getReaderForSpecification("3").getClass()
                        == StringReader.class;
        }
        
        public void readerCorrectContents() throws Exception {
            Reader r = codec.getReaderForSpecification("-32aa7");
            StringBuffer buf = new StringBuffer();
            int c;
            
            while( (c = r.read()) != -1 ) {
                buf.append((char) c);
            }
            
            assert "-32aa7".equals(buf.toString());
        }
        
        
        // argument parsing
        
        @ExpectedExceptions(ParserException.class)
        public void nonNumericArgument() throws Exception {
            codec.decode(codec.getReaderForSpecification("xyzzy"), graph, labels);
            assert false;
        }
        
        @ExpectedExceptions(ParserException.class)
        public void emptyArgument() throws Exception {
            codec.decode(codec.getReaderForSpecification(""), graph, labels);
            assert false;
        }
        
        @ExpectedExceptions(MalformedDomgraphException.class)
        public void negativeArgument() throws Exception {
            codec.decode(codec.getReaderForSpecification("-1"), graph, labels);
            assert false;
        }

        @ExpectedExceptions(MalformedDomgraphException.class)
        public void zeroArgument() throws Exception {
            codec.decode(codec.getReaderForSpecification("0"), graph, labels);
            assert false;
        }

        
        // generates correct graph and labels
        
        public void chain3correctness() throws Exception {
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

}
