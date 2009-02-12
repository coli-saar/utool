/**
 * 
 */
package de.saar.chorus.domgraph.weakest


import de.saar.chorus.domgraph.graph.*;
import de.saar.chorus.domgraph.chart.*;
import de.saar.chorus.domgraph.codec.domcon.*;
import de.saar.chorus.domgraph.codec.*;

import de.saar.testingtools.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(value=Parameterized.class)
public class WeakestReadingsComputerTest {
	private DomGraph graph;
    private NodeLabels labels;
    private InputCodec ozcodec;
    private Chart chart;
    private RewriteSystem eqsys;
    private Annotator annotator;
    private RegularTreeGrammar<DecoratedNonterminal<SubgraphNonterminal,Annotation>> out;
    private List goldSfs;
    private String id;
    
    
    
    @Parameters
    public static data() {
        return [prepareFOL("EA", "[label(x1 every(x2 x3)) label(y1 a(y2 y3)) label(z1 foo) label(z2 bar) label(z3 baz) dom(x2 z1) dom(y2 z2) dom(x3 z3) dom(y3 z3)]",
                [ [[["x2","z1"], ["x3", "y1"], ["y2","z2"], ["y3", "z3"]],[:]] ])
                ];
    }
    
    @Test
	public void testWeakestSolvedForms() {
    	System.err.println("\n\n\nTest: " + id);
    	
		graph = graph.preprocess();
		chart.clear();
		ChartSolver.solve(graph,chart);
		
		RtgFreeFragmentAnalyzer analyzer = new RtgFreeFragmentAnalyzer(chart);
		analyzer.analyze();
		WeakestReadingsRtg filter = new WeakestReadingsRtg(graph,labels,analyzer,eqsys,annotator);
		filter.intersect(chart,out);

		SolvedFormIterator sfi = new SolvedFormIterator<DecoratedNonterminal<SubgraphNonterminal,Annotation>>(out, graph);
		List sfs = TestingTools.collectIteratorValues(sfi);
		
		if( !TestingTools.solvedFormsEqual(sfs, goldSfs) ) {
			System.err.println("wrong rtg!!" + out);
		}
		
		assert TestingTools.solvedFormsEqual(sfs, goldSfs) : "[" + id + "] sfs = " + sfs;
	}

	
    WeakestReadingsComputerTest(id, graphstr, intendedSolvedForms, eqsys, annotator) {
        ozcodec = new DomconOzInputCodec();
        graph = new DomGraph();
        labels = new NodeLabels();
        
        ozcodec.decode(new StringReader(graphstr), graph, labels);
        
        chart = new Chart();
        out = new ConcreteRegularTreeGrammar<DecoratedNonterminal<SubgraphNonterminal,Annotation>>();
        this.eqsys = eqsys;
        this.annotator = annotator;
        goldSfs = intendedSolvedForms;
        this.id = id;
    }
    

    
    static Object[] prepareFOL(id, graphstr, intendedSolvedForms) {
    	RewriteSystem rs = new RewriteSystem();
    	Annotator ann = new Annotator();
    	
    	try {
			new RewriteSystemParser().read(new StringReader(rewriteSystemFol), ann, rs);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
    	
        return (Object[]) [id, graphstr, intendedSolvedForms, rs, ann];
    }
    

	public static RewriteSystem makeRewriteSystem(String eqs) {
		RewriteSystem ret = new RewriteSystem();
		
		try {
			ret.read(new StringReader(eqs));
		} catch(Exception e) {
			
		}
		
		return ret;
	}
	
	public static String rewriteSystemFol = '''<?xml version="1.0" ?>
		<rewrite-system>
			<annotator initial="+">
				<rule annotation="+" label="a"> <hole annotation="+" /> <hole annotation="+" /> </rule>
				<rule annotation="-" label="a"> <hole annotation="-" /> <hole annotation="-" /> </rule>
				<rule annotation="+" label="every"> <hole annotation="-" /> <hole annotation="+" /> </rule>
				<rule annotation="-" label="every"> <hole annotation="+" /> <hole annotation="-" /> </rule>
				<rule annotation="+" label="not"> <hole annotation="-" /> </rule>
				<rule annotation="-" label="not"> <hole annotation="+" /> </rule>
			</annotator>

			<rewriting>
				<rule llabel="a" lhole="1" rlabel="every" rhole="1" annotation="+" />
				<rule llabel="every" lhole="1" rlabel="a" rhole="1" annotation="-" />
				<rule llabel="not" lhole="0" rlabel="a" rhole="1" annotation="+" />
				<rule llabel="every" lhole="1" rlabel="not" rhole="0" annotation="+" />
				<!-- etc. -->
			</rewriting>
		</rewrite-system>
''';
	
}
