package de.saar.chorus.domgraph.codec.domcon;

import de.saar.chorus.domgraph.graph.*;
import de.saar.chorus.domgraph.codec.*;
import de.saar.chorus.domgraph.codec.domcon.*;
import de.saar.chorus.domgraph.codec.basic.*;
import de.saar.chorus.domgraph.codec.holesem.*;

import java.util.*;
import java.io.*;

import de.saar.testingtools.*;




class DomconOzOutputCodecTest extends GroovyTestCase {
	private DomGraph graph, goldGraph;
    private NodeLabels labels, goldLabels;
    private InputCodec ozcodec;
    
    private OutputCodec outputcodec;
    
	//  @Configuration(beforeTestMethod = true)
    public void setUp() {
        ozcodec = new DomconOzInputCodec();
        outputcodec = new DomconOzOutputCodec();
        graph = new DomGraph();
        goldGraph = new DomGraph();
        labels = new NodeLabels();
        goldLabels = new NodeLabels();
    }
	
	// ensure that input codec can decode the output of the output codec correctly:
	// on the chain of length 4 
	public void testReversibilityChain4() {
    	InputCodec chain = new Chain();
		chain.decode(new StringReader("4"), graph, labels);
		assertReversibility(graph, labels);
	}

	
	// ensure that input codec can decode the output of the output codec correctly:
	// on the graph from the holesemantics-14.hs.pl example. 
	public void testReversibilityHolesem14() {
		InputCodec holesemcodec = new HolesemComsemInputCodec();
		holesemcodec.decode(new StringReader("some(_A,and(hole(_A),some(_B,and(label(_B),some(_C,some(_D,some(_E,some(_F,some(_G,and(hole(_C),and(label(_D),and(label(_E),and(label(_F),and(all(_E,_G,_F),and(imp(_F,_D,_C),and(leq(_B,_C),and(leq(_E,_A),and(some(_H,some(_I,some(_J,and(hole(_H),and(label(_I),and(label(_J),and(and(_D,_I,_H),and(leq(_D,_A),and(leq(_J,_H),and(some(_K,some(_L,some(_M,some(_N,some(_O,and(hole(_K),and(label(_L),and(label(_M),and(label(_N),and(some(_M,_O,_N),and(and(_N,_L,_K),and(leq(_J,_K),and(leq(_M,_A),and(and(pred1(_L,restaurant,_O),leq(_L,_A)),and(pred2(_J,in,_O,_G),leq(_J,_A)))))))))))))))),and(pred1(_I,man,_G),leq(_I,_A)))))))))))),some(_P,some(_Q,some(_R,some(_S,some(_T,and(hole(_P),and(label(_Q),and(label(_R),and(label(_S),and(some(_R,_T,_S),and(and(_S,_Q,_P),and(leq(_B,_P),and(leq(_R,_A),and(some(_U,some(_V,some(_W,and(hole(_U),and(label(_V),and(label(_W),and(and(_Q,_V,_U),and(leq(_Q,_A),and(leq(_W,_U),and(some(_X,some(_Y,some(_Z,some(_A1,some(_B1,and(hole(_X),and(label(_Y),and(label(_Z),and(label(_A1),and(some(_Z,_B1,_A1),and(and(_A1,_Y,_X),and(leq(_W,_X),and(leq(_Z,_A),and(and(pred1(_Y,car,_B1),leq(_Y,_A)),and(pred2(_W,with,_B1,_T),leq(_W,_A)))))))))))))))),and(pred1(_V,woman,_T),leq(_V,_A)))))))))))),and(pred2(_B,know,_T,_G),leq(_B,_A))))))))))))))))))))))))))))))))))"), graph, labels);
		assertReversibility(graph, labels);
	}
	
	
	
	
	////////////////////////////////////////////////////////////////////////
	
	private void assertReversibility(DomGraph graph, NodeLabels labels) {
		StringWriter writer = new StringWriter();
		DomGraph decodedGraph = new DomGraph();
		NodeLabels decodedLabels = new NodeLabels();
		
		outputcodec.encode(graph, labels, writer);
		ozcodec.decode(new StringReader(writer.toString()), decodedGraph, decodedLabels);
		
		assert DomGraph.isEqual(graph, labels, decodedGraph, decodedLabels);
	}
}
