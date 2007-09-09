package de.saar.chorus.domgraph.codec.holesem;

import de.saar.chorus.domgraph.graph.*;
import de.saar.chorus.domgraph.codec.*;
import de.saar.chorus.domgraph.codec.domcon.*;
import de.saar.chorus.domgraph.codec.holesem.*;

import java.util.*;

import de.saar.testingtools.*;


class HolesemCodecTest extends GroovyTestCase {
    private DomGraph graph, goldGraph;
    private NodeLabels labels, goldLabels;
    private InputCodec ozcodec;
    private InputCodec holesemcodec;
    
	//  @Configuration(beforeTestMethod = true)
    public void setUp() {
        ozcodec = new DomconOzInputCodec();
        holesemcodec = new HolesemComsemInputCodec();
        graph = new DomGraph();
        goldGraph = new DomGraph();
        labels = new NodeLabels();
        goldLabels = new NodeLabels();
    }

	// holesemantics-14.hs.pl example: make sure it can be decoded and decodes into
	// the correct graph
	public void testHolesem14() {
		ozcodec.decode(new StringReader("[label('_B' know(hs13 hs14)) label('_D' and('_I' '_H')) label('_E' all(hs1 '_F')) label('_F' imp('_D' '_C')) label(hs1 '_G') label('_I' man(hs6)) label('_J' in(hs4 hs5)) label('_L' restaurant(hs3)) label('_M' some(hs2 '_N')) label('_N' and('_L' '_K')) label(hs2 '_O') label(hs3 '_O') label(hs4 '_O') label(hs5 '_G') label(hs6 '_G') label('_Q' and('_V' '_U')) label('_R' some(hs7 '_S')) label('_S' and('_Q' '_P')) label(hs7 '_T') label('_V' woman(hs12)) label('_W' with(hs10 hs11)) label('_Y' car(hs9)) label('_Z' some(hs8 '_A1')) label('_A1' and('_Y' '_X')) label(hs8 '_B1') label(hs9 '_B1') label(hs10 '_B1') label(hs11 '_T') label(hs12 '_T') label(hs13 '_T') label(hs14 '_G') dom('_C' '_B') dom('_H' '_J') dom('_K' '_J') dom('_P' '_B') dom('_U' '_W') dom('_X' '_W')]"), goldGraph, goldLabels);
		holesemcodec.decode(new StringReader("some(_A,and(hole(_A),some(_B,and(label(_B),some(_C,some(_D,some(_E,some(_F,some(_G,and(hole(_C),and(label(_D),and(label(_E),and(label(_F),and(all(_E,_G,_F),and(imp(_F,_D,_C),and(leq(_B,_C),and(leq(_E,_A),and(some(_H,some(_I,some(_J,and(hole(_H),and(label(_I),and(label(_J),and(and(_D,_I,_H),and(leq(_D,_A),and(leq(_J,_H),and(some(_K,some(_L,some(_M,some(_N,some(_O,and(hole(_K),and(label(_L),and(label(_M),and(label(_N),and(some(_M,_O,_N),and(and(_N,_L,_K),and(leq(_J,_K),and(leq(_M,_A),and(and(pred1(_L,restaurant,_O),leq(_L,_A)),and(pred2(_J,in,_O,_G),leq(_J,_A)))))))))))))))),and(pred1(_I,man,_G),leq(_I,_A)))))))))))),some(_P,some(_Q,some(_R,some(_S,some(_T,and(hole(_P),and(label(_Q),and(label(_R),and(label(_S),and(some(_R,_T,_S),and(and(_S,_Q,_P),and(leq(_B,_P),and(leq(_R,_A),and(some(_U,some(_V,some(_W,and(hole(_U),and(label(_V),and(label(_W),and(and(_Q,_V,_U),and(leq(_Q,_A),and(leq(_W,_U),and(some(_X,some(_Y,some(_Z,some(_A1,some(_B1,and(hole(_X),and(label(_Y),and(label(_Z),and(label(_A1),and(some(_Z,_B1,_A1),and(and(_A1,_Y,_X),and(leq(_W,_X),and(leq(_Z,_A),and(and(pred1(_Y,car,_B1),leq(_Y,_A)),and(pred2(_W,with,_B1,_T),leq(_W,_A)))))))))))))))),and(pred1(_V,woman,_T),leq(_V,_A)))))))))))),and(pred2(_B,know,_T,_G),leq(_B,_A))))))))))))))))))))))))))))))))))"), graph, labels);
		
		assert DomGraph.isEqual(graph, labels, goldGraph, goldLabels);
	}
    
	// holesemantics-14.hs.pl example minus one dom edge: not hnc
	public void testHolesem14nonHnc() {
		TestingTools.expectException(MalformedDomgraphException,
				{ holesemcodec.decode(new StringReader("some(_A,and(hole(_A),some(_B,and(label(_B),some(_C,some(_D,some(_E,some(_F,some(_G,and(hole(_C),and(label(_D),and(label(_E),and(label(_F),and(all(_E,_G,_F),and(imp(_F,_D,_C),and(leq(_B,_C),and(leq(_E,_A),and(some(_H,some(_I,some(_J,and(hole(_H),and(label(_I),and(label(_J),and(and(_D,_I,_H),and(leq(_D,_A),and(leq(_J,_H),and(some(_K,some(_L,some(_M,some(_N,some(_O,and(hole(_K),and(label(_L),and(label(_M),and(label(_N),and(some(_M,_O,_N),and(and(_N,_L,_K),and(leq(_J,_K),and(leq(_M,_A),and(and(pred1(_L,restaurant,_O),leq(_L,_A)),and(pred2(_J,in,_O,_G),leq(_J,_A)))))))))))))))),and(pred1(_I,man,_G),leq(_I,_A)))))))))))),some(_P,some(_Q,some(_R,some(_S,some(_T,and(hole(_P),and(label(_Q),and(label(_R),and(label(_S),and(some(_R,_T,_S),and(and(_S,_Q,_P),and(leq(_B,_P),and(leq(_R,_A),and(some(_U,some(_V,some(_W,and(hole(_U),and(label(_V),and(label(_W),and(and(_Q,_V,_U),and(leq(_Q,_A),and(some(_X,some(_Y,some(_Z,some(_A1,some(_B1,and(hole(_X),and(label(_Y),and(label(_Z),and(label(_A1),and(some(_Z,_B1,_A1),and(and(_A1,_Y,_X),and(leq(_W,_X),and(leq(_Z,_A),and(and(pred1(_Y,car,_B1),leq(_Y,_A)),and(pred2(_W,with,_B1,_T),leq(_W,_A)))))))))))))))),and(pred1(_V,woman,_T),leq(_V,_A))))))))))),and(pred2(_B,know,_T,_G),leq(_B,_A))))))))))))))))))))))))))))))))))"), graph, labels) }
		);
	}
	
	// holesemantics-14.hs.pl example with a parse error
	public void testHolesem14parseError() {
		TestingTools.expectException(ParserException,
				{ holesemcodec.decode(new StringReader("some(_A,and(hole(_A),some(_B,and(label(_B),some(_C,some(_D,some(_E,some(_F,some(_G,and(hole(_C),and(label(_D),and(label(_E),and(label(_F),and(all(_E,_G,_F),and(imp(_F,_D,_C),and(leq(_B,_C),and(leq(_E,_A),and(some(_H,some(_I,some(_J,and(hole(_H),and(label(_I),and(label(_J),and(and(_D,_I,_H),and(leq(_D,_A),and(leq(_J,_H),and(some(_K,some(_L,some(_M,some(_N,some(_O,and(hole(_K),and(label(_L),and(label(_M),and(label(_N),and(some(_M,_O,_N),and(and(_N,_L,_K),and(leq(_J,_K),and(leq(_M,_A),and(and(pred1(_L,restaurant,_O),leq(_L,_A)),and(pred2(_J,in,_O,_G),leq(_J,_A)))))))))))))))),and(pred1(_I,man,_G),leq(_I,_A)))))))))))),some(_P,some(_Q,some(_R,some(_S,some(_T,and(hole(_P),and(label(_Q),and(label(_R),and(label(_S),and(some(_R,_T,_S),and(and(_S,_Q,_P),and(leq(_B,_P),and(leq(_R,_A),and(some(_U,some(_V,some(_W,and(hole(_U),and(label(_V),and(label(_W),and(and(_Q,_V,_U),and(leq(_Q,_A),and(leq(_W,_U),and(some(_X,some(_Y,some(_Z,some(_A1,some(_B1,and(hole(_X),and(label(_Y),and(label(_Z),and(label(_A1),and(some(_Z,_B1,_A1),and(and(_A1,_Y,_X),and(leq(_W,_X),and(leq(_Z,_A),and(and(pred1(_Y,car,_B1),leq(_Y,_A)),and(pred2(_W,with,_B1,_T),leq(_W,_A)))))))))))))))),and(pred1(_V,woman,_T),leq(_V,_A)))))))))))),and(pred2(_B,know,_T,_G),leq(_B,_A)))))))))))))))))))))))))))))))))"), graph, labels) }
		);
	}
    
    // multiple empty top fragments: should throw an exception
    public void testMultipleTopFragmentsException() {
    	TestingTools.expectException(MalformedDomgraphException,
    			{ holesemcodec.decode(new StringReader("some(_A,and(hole(_A),some(_B,and(label(_B),some(_C,some(_D,some(_E,some(_F,some(_G,and(hole(_C),and(label(_D),and(label(_E),and(label(_F),and(all(_E,_G,_F),and(imp(_F,_D,_C),and(leq(_B,_C),and(leq(_E,_A),and(some(_H,some(_I,some(_J,and(hole(_H),and(label(_I),and(label(_J),and(and(_D,_I,_H),and(leq(_D,_A),and(leq(_J,_H),and(some(_K,some(_L,some(_M,some(_N,some(_O,and(hole(_K),and(label(_L),and(label(_M),and(label(_N),and(some(_M,_O,_N),and(and(_N,_L,_K),and(leq(_J,_K),and(leq(_M,_A),and(and(pred1(_L,restaurant,_O),leq(_L,_A)),and(pred2(_J,in,_O,_G),leq(_J,_A)))))))))))))))),and(pred1(_I,man,_G),leq(_I,_A)))))))))))),some(_P,some(_Q,some(_R,some(_S,some(_T,and(hole(_P),and(label(_Q),and(label(_R),and(label(_S),and(some(_R,_T,_S),and(and(_S,_Q,_P),and(leq(_B,_P),and(leq(_R,_A),and(some(_U,some(_V,some(_W,and(hole(_U),and(label(_V),and(label(_W),and(and(_Q,_V,_U),and(leq(_Q,_A),and(leq(_W,_U),and(some(_X,some(_Y,some(_Z,some(_A1,some(_B1,and(hole(_X),and(label(_Y),and(label(_Z),and(label(_A1),and(some(_Z,_B1,_A1),and(and(_A1,_Y,_X),and(leq(_W,_X),and(leq(_Z,_A),and(and(pred1(_Y,car,_B1),leq(_Y,_A)),and(pred2(_W,with,_B1,_T),leq(_W,_A)))))))))))))))),and(pred1(_V,woman,_T),leq(_V,_A)))))))))))),and(pred2(_B,know,_T,_G),and(leq(_B,_A),leq(_B,_A2)))))))))))))))))))))))))))))))))))"), graph, labels) }
    	);
    }
	
	// empty fragment with both incoming and outgoing dominance edges: should throw an exception
    public void testNontrivialEmptyFragmentException() {
    	TestingTools.expectException(MalformedDomgraphException,
    			{ holesemcodec.decode(new StringReader("some(_A,and(hole(_A),some(_B,and(label(_B),some(_C,some(_D,some(_E,some(_F,some(_G,and(hole(_C),and(label(_D),and(label(_E),and(label(_F),and(all(_E,_G,_F),and(imp(_F,_D,_C),and(leq(_B,_C),and(leq(_E,_A),and(some(_H,some(_I,some(_J,and(hole(_H),and(label(_I),and(label(_J),and(and(_D,_I,_H),and(leq(_D,_A),and(leq(_J,_H),and(some(_K,some(_L,some(_M,some(_N,some(_O,and(hole(_K),and(label(_L),and(label(_M),and(label(_N),and(some(_M,_O,_N),and(and(_N,_L,_K),and(leq(_J,_K),and(leq(_M,_A),and(and(pred1(_L,restaurant,_O),leq(_L,_A)),and(pred2(_J,in,_O,_G),leq(_J,_A)))))))))))))))),and(pred1(_I,man,_G),leq(_I,_A)))))))))))),some(_P,some(_Q,some(_R,some(_S,some(_T,and(hole(_P),and(label(_Q),and(label(_R),and(label(_S),and(some(_R,_T,_S),and(and(_S,_Q,_P),and(leq(_B,_P),and(leq(_R,_A),and(some(_U,some(_V,some(_W,and(hole(_U),and(label(_V),and(label(_W),and(and(_Q,_V,_U),and(leq(_Q,_A),and(leq(_W,_U),and(some(_X,some(_Y,some(_Z,some(_A1,some(_B1,and(hole(_X),and(label(_Y),and(label(_Z),and(label(_A1),and(some(_Z,_B1,_A1),and(and(_A1,_Y,_X),and(leq(_W,_X),and(leq(_Z,_A),and(and(pred1(_Y,car,_B1),leq(_Y,_A)),and(pred2(_W,with,_B1,_T),leq(_W,_A)))))))))))))))),and(pred1(_V,woman,_T),leq(_V,_A)))))))))))),and(pred2(_B,know,_T,_G),and(leq(_B,_A),  and(and(_A1,_A2,_A3),leq(_A,_A2))))))))))))))))))))))))))))))))))))"), graph, labels) }
    	);
    }
}
