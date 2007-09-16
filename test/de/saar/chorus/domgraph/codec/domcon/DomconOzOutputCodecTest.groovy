package de.saar.chorus.domgraph.codec.domcon;

import de.saar.chorus.domgraph.graph.*;
import de.saar.chorus.domgraph.codec.*;
import de.saar.chorus.domgraph.codec.domcon.*;
import de.saar.chorus.domgraph.codec.basic.*;
import de.saar.chorus.domgraph.codec.holesem.*;
import de.saar.chorus.domgraph.codec.mrs.*;

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
	
	// a rather embarrassing former bug, in which only the first symbol of each
	// label would be checked for characters that had to be quote-escaped
	public void testReversibilityRondane1() {
		InputCodec mrscodec = new MrsPrologInputCodec(Normalisation.nets, LabelStyle.plain);
		mrscodec.decode(new StringReader("psoa(h1,e2,[rel('prpstn_m',h1,[attrval('ARG0',e2),attrval('MARG',h3),attrval('PSV',u4),attrval('TPC',u5)]),rel('_the_q',h6,[attrval('ARG0',x8),attrval('RSTR',h9),attrval('BODY',h7)]),rel('_well+known_a_1',h10,[attrval('ARG0',e11),attrval('ARG1',x8)]),rel('_and_c',h12,[attrval('ARG0',e15),attrval('L-HNDL',h10),attrval('L-INDEX',e11),attrval('R-HNDL',h14),attrval('R-INDEX',e13)]),rel('_historic_a_1',h14,[attrval('ARG0',e13),attrval('ARG1',x8)]),rel('compound',h12,[attrval('ARG0',e17),attrval('ARG1',x8),attrval('ARG2',x16)]),rel('proper_q',h18,[attrval('ARG0',x16),attrval('RSTR',h19),attrval('BODY',h20)]),rel('named',h21,[attrval('ARG0',x16),attrval('CARG','aurlandsdalen')]),rel('_valley_n_of',h12,[attrval('ARG0',x8),attrval('ARG1',i22)]),rel('_be_v_id',h23,[attrval('ARG0',e2),attrval('ARG1',x8),attrval('ARG2',x24)]),rel('_once_a_1',h23,[attrval('ARG0',e25),attrval('ARG1',e2)]),rel('part_of',h26,[attrval('ARG0',x24),attrval('ARG1',x27)]),rel('udef_q',h28,[attrval('ARG0',x24),attrval('RSTR',h29),attrval('BODY',h30)]),rel('card',h26,[attrval('ARG0',i31),attrval('ARG1',x24),attrval('CARG','1')]),rel('_the_q',h32,[attrval('ARG0',x27),attrval('RSTR',h34),attrval('BODY',h33)]),rel('_main_a_1',h35,[attrval('ARG0',e36),attrval('ARG1',x27)]),rel('_route_n_1',h35,[attrval('ARG0',x27)]),rel('_between_p',h35,[attrval('ARG0',e37),attrval('ARG1',x27),attrval('ARG2',x38)]),rel('_the_q',h39,[attrval('ARG0',x38),attrval('RSTR',h41),attrval('BODY',h40)]),rel('_eastern_a_1',h42,[attrval('ARG0',e43),attrval('ARG1',x38)]),rel('_and_c',h44,[attrval('ARG0',e47),attrval('L-HNDL',h42),attrval('L-INDEX',e43),attrval('R-HNDL',h46),attrval('R-INDEX',e45)]),rel('_western_a_1',h46,[attrval('ARG0',e45),attrval('ARG1',x38)]),rel('_part_n_1',h44,[attrval('ARG0',x38)]),rel('_of_p',h44,[attrval('ARG0',e48),attrval('ARG1',x38),attrval('ARG2',x49)]),rel('proper_q',h50,[attrval('ARG0',x49),attrval('RSTR',h51),attrval('BODY',h52)]),rel('named',h53,[attrval('ARG0',x49),attrval('CARG','norway')])],hcons([qeq(h51,h53),qeq(h41,h44),qeq(h34,h35),qeq(h29,h26),qeq(h19,h21),qeq(h9,h12),qeq(h3,h23)]))"), graph, labels);
		assertReversibility(graph, labels);
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
		
		TestingTools.expectException(MalformedDomgraphException,
				{ outputcodec.encode(graph, labels, new StringWriter()); });
	}
	
	public void testNonWellLabeled2() {
		graph.addNode("x", new NodeData(NodeType.UNLABELLED));
		graph.addNode("y", new NodeData(NodeType.UNLABELLED));
		graph.addNode("z", new NodeData(NodeType.LABELLED));
		
		graph.addEdge("x", "y", new EdgeData(EdgeType.TREE));
		graph.addEdge("y", "z", new EdgeData(EdgeType.DOMINANCE));
		
		labels.addLabel("x", "f");
		labels.addLabel("z", "a");
		
		TestingTools.expectException(MalformedDomgraphException,
				{ outputcodec.encode(graph, labels, new StringWriter()); });
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
