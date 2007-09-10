package de.saar.chorus.domgraph.codec.mrs;

import de.saar.chorus.domgraph.graph.*;
import de.saar.chorus.domgraph.codec.*;
import de.saar.chorus.domgraph.codec.domcon.*;
import de.saar.chorus.domgraph.codec.mrs.*;

import java.util.*;

import de.saar.testingtools.*;


class MrsCodecTest extends GroovyTestCase {
	private DomGraph graph, goldGraph;
    private NodeLabels labels, goldLabels;
    private InputCodec ozcodec;
    private InputCodec mrscodec;
    
	//  @Configuration(beforeTestMethod = true)
    public void setUp() {
        ozcodec = new DomconOzInputCodec();
        mrscodec = new MrsPrologInputCodec(Normalisation.nets, LabelStyle.plain);
        graph = new DomGraph();
        goldGraph = new DomGraph();
        labels = new NodeLabels();
        goldLabels = new NodeLabels();
    }
	
	// Stefan Mueller's problem MRS 1 of 10/08/2007
	public void testSM1() {
		mrscodec.decode(new StringReader("psoa(h1,e2,[ rel('one_q',h3,     [ attrval('ARG0',x4),       attrval('RSTR',h5),       attrval('BODY',h6)]), rel('compound_rel',h7,     [ attrval('ARG0',x4),       attrval('ARG1',x8)]), rel('udef_q',h9,     [ attrval('ARG0',x8),       attrval('RSTR',h10),       attrval('BODY',h11)]), rel('bag',h7,     [ attrval('ARG0',x4)]), rel('leather',h12,     [ attrval('ARG0',x8)]), rel('past',h13,     [ attrval('ARG0',e2),       attrval('ARG3',h14)]), rel('see',h14,     [ attrval('ARG0',e2),       attrval('ARG1',i15),       attrval('ARG2',x4)])], hcons([ qeq(h1,h3), qeq(h5,h7), qeq(h1,h9), qeq(h10,h12) ]))"),
				graph, labels);
		
		TestingTools.checkSolvedForms(graph,
				[ [[["h11","h3"],["h5","h7"],["h10","h12"],["h6","h13"]],[:]], [[["h11","h7"],["h5","h9"],["h6","h13"],["h10","h12"]],[:]] ]);
	}
	
	//  Stefan Mueller's problem MRS 2 of 10/08/2007
	public void testSM2() {
		mrscodec.decode(new StringReader("psoa(h1,e2, [ rel('one_q',h3,     [ attrval('ARG0',x4),       attrval('RSTR',h5),       attrval('BODY',h6)]), rel('compound_rel',h7,     [ attrval('ARG0',x4),       attrval('ARG1',x8)]), rel('udef_q',h9,     [ attrval('ARG0',x8),       attrval('RSTR',h10),       attrval('BODY',h7)]), rel('bag',h7,     [ attrval('ARG0',x4)]), rel('leather',h11,     [ attrval('ARG0',x8)]), rel('past',h12,     [ attrval('ARG0',e2),       attrval('ARG3',h13)]), rel('see',h13,     [ attrval('ARG0',e2),       attrval('ARG1',i14),       attrval('ARG2',x4)])], hcons([ qeq(h1,h3), qeq(h5,h7), qeq(h1,h9), qeq(h10,h11) ]))"),
				graph, labels);
		
		TestingTools.checkSolvedForms(graph,
				[ [[["h5","h9"],["h10","h11"],["h6","h12"]],[:]] ]);
	}

}
