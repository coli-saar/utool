package de.saar.chorus.domgraph.equivalence;

import de.saar.chorus.domgraph.graph.*;
import de.saar.chorus.domgraph.chart.*;
import de.saar.chorus.domgraph.codec.*;
import de.saar.chorus.domgraph.codec.domcon.*;

import java.util.*;

import de.saar.testingtools.*;

class RedundancyEliminationTest extends GroovyTestCase {
	private DomGraph graph;
    private NodeLabels labels;
    private InputCodec ozcodec;
    private Chart chart;
    
	//  @Configuration(beforeTestMethod = true)
    public void setUp() {
        ozcodec = new DomconOzInputCodec();
        graph = new DomGraph();
        labels = new NodeLabels();
        chart = new Chart();
    }
	
	// Stefan Mueller's bug, #274
	public void testSM1() {
		ozcodec.decode(new StringReader("[label(h3 '[one_q ARG0:x4, BODY:h6, RSTR:h5]'(h5 h6)) label(h7 '[compound_rel ARG0:x4, ARG1:x8]&[bag ARG0:x4]') label(h9 '[udef_q ARG0:x8, BODY:h7, RSTR:h10]'(h10 h7)) label(h11 '[leather ARG0:x8]') label(h12 '[past ARG0:e2, ARG3:h13]'(h13)) label(h13 '[see ARG0:e2, ARG1:i14, ARG2:x4]') dom(h5 h7) dom(h10 h11) dom(h6 h12)]"),
				graph, labels);
		graph = graph.preprocess();
		
		RedundancyElimination elim = new IndividualRedundancyElimination(graph, labels, makeEqSystem(eqSystemSM));

		ChartSolver.solve(graph,chart);
		elim.eliminate(chart);
		
		// TODO ensure that we got the right result here, e.g. by specifying the solved forms
		// of the chart
		assert true;
	}
	
	
	// TODO ensure that on-the-fly redundancy elimination (building chart with a different
	// split source) gives the same results as computing the chart and then eliminating redundancy
	
	
	
	
	///////////////////////////////////////////////////////////////////////////////
	
	
	
	private EquationSystem makeEqSystem(String eqs) {
		EquationSystem ret = new EquationSystem();
		
		try {
			ret.read(new StringReader(eqs));
		} catch(Exception e) {
			
		}
		
		return ret;
	}
	
	
	
	
	///////////////////////////////////////////////////////////////////////////////
	
	private static String eqSystemSM = "<?xml version='1.0' ?> "+
		"<equivalences style='Core-B-Gram'> " +
		"<!-- Version 2006-09-10 -->" +
		"	<equivalencegroup>" +
		"		<!-- implicit_q_rel -->" +
		"		<quantifier label='def_q'  hole='1'/>" +
		"		<quantifier label='def_q'  hole='1'/>" +
		"		<quantifier label='udef_q' hole='1'/>" +


		"		<!-- demonstrative_q_rel -->" +
		"		<quantifier label='demonstrative_q' hole='1'/>" +
		"		<quantifier label='demonstrative_q' hole='1'/>" +

		"		<!-- some_q_rel -->" +
		"		<quantifier label='some_q' hole='0' />" +
		"		<quantifier label='some_q' hole='1' />" +
		"		<quantifier label='one_q' hole='0' />" +
		"		<quantifier label='one_q' hole='1' />" +
		"	</equivalencegroup>" +


		"	<equivalencegroup>" +
		"       <quantifier label='every_q' hole='1' />" +
		"		<quantifier label='each_q'  hole='1' />" +
		"	</equivalencegroup>" +

			
		"	<permutesWithEverything label='proper_q'  hole='1' />" +
		"	<permutesWithEverything label='pronoun_q' hole='1' />" +
		"</equivalences>";
}
