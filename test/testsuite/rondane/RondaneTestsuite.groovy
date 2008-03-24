package testsuite.rondane;

import java.io.*;
import java.util.zip.*;

import de.saar.chorus.domgraph.codec.*;
import de.saar.chorus.domgraph.graph.*;
import de.saar.chorus.domgraph.utool.*;
import de.saar.chorus.domgraph.chart.*;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(value=Parameterized.class)
class RondaneTestsuite {
	private CodecManager manager;
	
	private DomGraph graph, goldGraph;
	private NodeLabels labels, goldLabels;
	
	private InputCodec domconInputCodec;
	
	
	def id, codecname, usr, code, solvableExpected, numSfs, domgraphUsr, expectedError, chartsize;
	
	public RondaneTestsuite(id, codecname, usr, domgraphUsr, code, solvable, numSfs, expectedError, chartsize) {
	    this.id = id;
	    this.codecname = codecname;
	    this.usr = usr;
	    this.code = code;
	    this.solvableExpected = solvable;
	    this.numSfs = numSfs;
	    this.domgraphUsr = domgraphUsr;
	    this.expectedError = expectedError;
	    this.chartsize = chartsize;
	}
	
	
	@Test
	public void testTestsuite() {
	    manager = new CodecManager();
        manager.registerAllDeclaredCodecs();
        
        domconInputCodec = manager.getInputCodecForName("domcon-oz", [:]);
        
        graph = new DomGraph();
        goldGraph = new DomGraph();
        
        labels = new NodeLabels();
        goldLabels = new NodeLabels();
        
        
        
        boolean exception = false, solvable = true;
        def codec = manager.getInputCodecForName(codecname, [:]);
		
		graph.clear();
		labels.clear();
		
		try {
			codec.decode(new StringReader(usr), graph, labels);
			exception = false;
		} catch(ParserException e) {
			assert expectedError == ExitCodes.PARSING_ERROR_INPUT_GRAPH : ("Unexpected parsing error for testcase ${id}: " + e.toString());
			exception = true;
		} catch(MalformedDomgraphException e) {
			assert expectedError == ExitCodes.MALFORMED_DOMGRAPH_BASE_INPUT + e.getExitcode() : ("Unexpected semantic error for testcase ${id}: " + e.toString());
			exception = true;
		} catch(IOException e) {
			// shouldn't happen
		}
		
		if( !exception ) {
			domconInputCodec.decode(new StringReader(domgraphUsr), goldGraph, goldLabels);
			
			assert DomGraph.isEqual(graph, labels, goldGraph, goldLabels) : "Testcase ${id} doesn't match gold graph";
			
			assertClassifyCorrect();
			assertSolveCorrect();
		}
	}
	
    
    
	@Parameters
    public static List<Object[]>  data() {
	    String filename = "projects/Domgraph/testsuites/rondane-jul06-2008-03-24.xml.gz";
		
		def testsuite = new XmlSlurper().parse(new GZIPInputStream(new FileInputStream(filename)));
		List ret = [];
		
		testsuite.usr.each {
		    ret.add(
		    (Object[])
		    [it.@id.text(), it.@codec.text(), it.@string.text(), it.domgraph.@string.text(),
		     it.classify.@code.text(), it.solve.@solvable.text(), it.solve.@count.text(),
		     myParseInt(it.domgraph.@error.text()), myParseInt(it.solve.@chartsize.text())]);
		};
		
		return ret;
	}

	
	private static int myParseInt(String it) {
	    if( it == "" ) {
	        return 0;
	    } else {
	        return Integer.parseInt(it);
	    }
	}
	
	private void assertSolveCorrect() {
		Chart chart = new Chart();
		boolean solvable = ChartSolver.solve(graph,chart);
		
		if( solvableExpected == 'false' ) {
			assert !solvable : "Expected testcase ${id} to be unsolvable";
		} else {
			assert solvable : "Expected testcase ${id} to be solvable";
			assert chart.size() == chartsize :  ("Testcase ${id} doesn't match expected chart size (found: " + chart.size() + ", expected: " + it.solve.@chartsize.text() + ")");
			assert chart.countSolvedForms() == new BigInteger(numSfs) : "Testcase ${id} doesn't match expected number of solved forms";
		}
	}

	
	private void assertClassifyCorrect() {
		int classification = 0;
		int goldClassification = Integer.parseInt(code);
		
		if( graph.isWeaklyNormal() ) {
        	classification |= ExitCodes.CLASSIFY_WEAKLY_NORMAL;
    	}

    	if( graph.isNormal() ) {
        	classification |= ExitCodes.CLASSIFY_NORMAL;
    	}
    
    	if( graph.isCompact() ) {
        	classification |= ExitCodes.CLASSIFY_COMPACT;
    	}
    	
    	if( graph.isHypernormallyConnected() ) {
    		classification |= ExitCodes.CLASSIFY_HN_CONNECTED;
    	}
    	
    	if( graph.isLeafLabelled() ) {
    		classification |= ExitCodes.CLASSIFY_LEAF_LABELLED;
    	}
    	
    	assert classification == goldClassification : "Testcase ${id} incorrectly classified (as ${classification} rather than ${goldClassification})";
	}

}
