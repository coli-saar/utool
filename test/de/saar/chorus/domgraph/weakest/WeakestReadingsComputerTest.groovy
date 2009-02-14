/**
 * 
 */
package de.saar.chorus.domgraph.weakest


import de.saar.chorus.domgraph.graph.*;
import de.saar.chorus.domgraph.chart.*;
import de.saar.chorus.domgraph.codec.domcon.*;
import de.saar.chorus.domgraph.codec.*;
import de.saar.chorus.domgraph.equivalence.*;


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
    private RewriteSystem rewriteSystem;
    private Annotator annotator;
    private RegularTreeGrammar<DecoratedNonterminal<SubgraphNonterminal,Annotation>> out;
    private List goldSfs;
    private String id;
    private EquationSystem eqsys;
    
    
    @Parameters
    public static data() {
        return [prepareFOL("EA", "[label(x1 every(x2 x3)) label(y1 a(y2 y3)) label(z1 foo) label(z2 bar) label(z3 baz) dom(x2 z1) dom(y2 z2) dom(x3 z3) dom(y3 z3)]",
                			[ [[["x2","z1"], ["x3", "y1"], ["y2","z2"], ["y3", "z3"]],[:]] ]),
                prepareFOL("EEA", "[label(x1 every(x2 x3)) label(y1 a(y2 y3)) label(y3 a(y4 y5)) label(z1 foo) label(z2 bar) label(z3 baz) label(z4 bazz) dom(x2 z1) dom(y2 z2) dom(x3 z3) dom(y4 z4) dom(y5 z3)]",
                		[ [[["x2","z1"], ["x3", "y1"], ["y2","z2"], ["y4","z4"], ["y5", "z3"]],[:]] ]),
                prepareFOL("thesis c-", "[label(x1 every(x2 x3)) label(y1 every(y2 y3)) label(a1 not(a2)) label(z1 b) label(z2 b) label(z3 b) dom(a2 x1) dom(a2 y1) dom(x2 z1) dom(x3 z2) dom(y2 z2) dom(y3 z3)]",
                		[ [[["a2", "x1"], ["x2", "z1"], ["x3", "y1"], ["y2", "z2"], ["y3", "z3"]],[:]] ]),
                prepareFOL("thesis c+", "[label(x1 every(x2 x3)) label(y1 every(y2 y3)) label(z1 b) label(z2 b) label(z3 b) dom(x2 z1) dom(x3 z2) dom(y2 z2) dom(y3 z3)]",
                   		[ [[["y3", "z3"], ["y2", "x1"], ["x2", "z1"], ["x3", "z2"]],[:]] ]),
                prepareFOL("weakening + equiv", "[label(x1 every(x2 x3)) label(y1 a(y2 y3)) label(y3 every(y4 y5)) label(z1 foo) label(z2 bar) label(z3 baz) label(z4 bazz) dom(x2 z1) dom(y2 z2) dom(x3 z3) dom(y4 z4) dom(y5 z3)]",
                   		[ [[["x2","z1"], ["x3", "y1"], ["y2","z2"], ["y4","z4"], ["y5", "z3"]],[:]] ]),
                prepareFOL("only equiv", "[label(x1 every(x2 x3)) label(y1 every(y2 y3)) label(y3 every(y4 y5)) label(z1 foo) label(z2 bar) label(z3 baz) label(z4 bazz) dom(x2 z1) dom(y2 z2) dom(x3 z3) dom(y4 z4) dom(y5 z3)]",
                		[[[["x2", "z1"], ["x3", "y1"], ["y2", "z2"], ["y4", "z4"], ["y5", "z3"]],[:]], [[["y2", "z2"], ["y4", "z4"], ["y5", "x1"], ["x2", "z1"], ["x3", "z3"]],[:]]]),
                prepareFOLnoEquiv("null equiv", "[label(x1 every(x2 x3)) label(y1 a(y2 y3)) label(y3 every(y4 y5)) label(z1 foo) label(z2 bar) label(z3 baz) label(z4 bazz) dom(x2 z1) dom(y2 z2) dom(x3 z3) dom(y4 z4) dom(y5 z3)]",
                   		[[[["x2", "z1"], ["x3", "y1"], ["y2", "z2"], ["y4", "z4"], ["y5", "z3"]],[:]], [[["y2", "z2"], ["y4", "z4"], ["y5", "x1"], ["x2", "z1"], ["x3", "z3"]],[:]]]),
                prepareFG("non-cpt polarity 1", "[label(x1 not(x2)) label(x2 g(x3)) label(y1 f(y2)) label(z b) dom(x3 z) dom(y2 z)]",
                		[[[["x3", "y1"], ["y2", "z"]],[:]] ]),
                prepareFG("non-cpt polarity 2", "[label(x1 not(x2)) label(x2 f(x3)) label(y1 g(y2)) label(z b) dom(x3 z) dom(y2 z)]",
                		[[[["x3", "y1"], ["y2", "z"]],[:]], [[["y2", "x1"], ["x3", "z"]],[:]]])
                ];
    }
    
    @Test
	public void testWeakestSolvedForms() {
    	//System.err.println("\n\n\nTest: " + id);
    	
		graph = graph.preprocess();
		chart.clear();
		ChartSolver.solve(graph,chart);
		
		RtgFreeFragmentAnalyzer analyzer = new RtgFreeFragmentAnalyzer(chart);
		analyzer.analyze();
		WeakestReadingsRtg filter = new WeakestReadingsRtg(graph,labels,analyzer,rewriteSystem,annotator,eqsys);
		filter.intersect(chart,out);
		out.cleanup();

		SolvedFormIterator sfi = new SolvedFormIterator<DecoratedNonterminal<SubgraphNonterminal,Annotation>>(out, graph);
		List sfs = TestingTools.collectIteratorValues(sfi);
		
		/*
		if( !TestingTools.solvedFormsEqual(sfs, goldSfs) ) {
			System.err.println("wrong rtg!!" + out);
		}
		*/
		
		assert TestingTools.solvedFormsEqual(sfs, goldSfs) : "[" + id + "] sfs = " + sfs;
	}

	
    WeakestReadingsComputerTest(id, graphstr, intendedSolvedForms, rewriteSystem, eqsys, annotator) {
        ozcodec = new DomconOzInputCodec();
        graph = new DomGraph();
        labels = new NodeLabels();
        
        ozcodec.decode(new StringReader(graphstr), graph, labels);
        
        chart = new Chart();
        out = new ConcreteRegularTreeGrammar<DecoratedNonterminal<SubgraphNonterminal,Annotation>>();
        this.rewriteSystem = rewriteSystem;
        this.eqsys = eqsys;
        this.annotator = annotator;
        goldSfs = intendedSolvedForms;
        this.id = id;
    }
    

    
    static Object[] prepareFOL(id, graphstr, intendedSolvedForms) {
    	RewriteSystem rs = new RewriteSystem();
    	EquationSystem eq;
    	Annotator ann = new Annotator();
    	
    	try {
			new RewriteSystemParser().read(new StringReader(rewriteSystemFol), ann, rs);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
		
		eq = makeEqSystem(eqSystemFOL);
    	
        return (Object[]) [id, graphstr, intendedSolvedForms, rs, eq, ann];
    }
    
    static Object[] prepareFOLnoEquiv(id, graphstr, intendedSolvedForms) {
    	RewriteSystem rs = new RewriteSystem();
    	Annotator ann = new Annotator();
    	
    	try {
			new RewriteSystemParser().read(new StringReader(rewriteSystemFol), ann, rs);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
		
		return (Object[]) [id, graphstr, intendedSolvedForms, rs, null, ann];
    }
    
    static Object[] prepareFG(id, graphstr, intendedSolvedForms) {
    	RewriteSystem rs = new RewriteSystem();
    	Annotator ann = new Annotator();
    	
    	try {
			new RewriteSystemParser().read(new StringReader(rewriteSystemFG), ann, rs);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
		
		return (Object[]) [id, graphstr, intendedSolvedForms, rs, null, ann];
    }
    

	public static RewriteSystem makeRewriteSystem(String eqs) {
		RewriteSystem ret = new RewriteSystem();
		
		try {
			ret.read(new StringReader(eqs));
		} catch(Exception e) {
			
		}
		
		return ret;
	}
	
	public static EquationSystem makeEqSystem(String eqs) {
		EquationSystem ret = new EquationSystem();
		
		try {
			ret.read(new StringReader(eqs));
		} catch(Exception e) {
			throw new RuntimeException(e);
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
				<rule llabel="every" lhole="0" rlabel="every" rhole="1" annotation="-" />
				<rule llabel="every" lhole="1" rlabel="every" rhole="0" annotation="+" />
				<!-- etc. -->
			</rewriting>
		</rewrite-system>
''';

	public static String eqSystemFOL = '''<?xml version="1.0" ?> 
	<equivalences style="FOL"> 
		<equivalencegroup>
	       <quantifier label="a" hole="1" />
	       <quantifier label="a" hole="0" />
	   </equivalencegroup>
	   <equivalencegroup>
	       <quantifier label="every" hole="1" />
	   </equivalencegroup>
	   <permutesWithEverything label="permute" hole="0" />
	</equivalences>
	''';
	
	public static String rewriteSystemFG = '''<?xml version="1.0" ?>
		<rewrite-system>
			<annotator initial="+">
				<rule annotation="+" label="not"> <hole annotation="-" /> </rule>
				<rule annotation="-" label="not"> <hole annotation="+" /> </rule>
				<rule annotation="+" label="f"> <hole annotation="+" /> </rule>
				<rule annotation="-" label="f"> <hole annotation="-" /> </rule>
				<rule annotation="+" label="g"> <hole annotation="+" /> </rule>
				<rule annotation="-" label="g"> <hole annotation="-" /> </rule>
			</annotator>
			
			<rewriting>
				<rule llabel="f" lhole="0" rlabel="not" rhole="0" annotation="+" />
				<rule llabel="g" lhole="0" rlabel="not" rhole="0" annotation="+" />
				<rule llabel="g" lhole="0" rlabel="f" rhole="0" annotation="+" />
				<rule llabel="f" lhole="0" rlabel="g" rhole="0" annotation="-" />
			</rewriting>
		</rewrite-system>
			''';
	
}
