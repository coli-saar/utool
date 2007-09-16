package testsuite.rondane;

import java.io.*;
import java.util.zip.*;

import de.saar.chorus.domgraph.codec.*;
import de.saar.chorus.domgraph.graph.*;
import de.saar.chorus.domgraph.utool.*;
import de.saar.chorus.domgraph.chart.*;

class RondaneTestsuite extends GroovyTestCase {
	private CodecManager manager;
	
	private DomGraph graph, goldGraph;
	private NodeLabels labels, goldLabels;
	
	private InputCodec domconInputCodec;
    
    // @Configuration(beforeSuite = true)
    public void setUp() throws Exception {
        manager = new CodecManager();
        manager.registerAllDeclaredCodecs();
        
        domconInputCodec = manager.getInputCodecForName("domcon-oz", [:]);
        
        graph = new DomGraph();
        goldGraph = new DomGraph();
        
        labels = new NodeLabels();
        goldLabels = new NodeLabels();
    }
    
	void testRondaneTestsuite() {
		String filename = "projects/Domgraph/testsuites/rondane-mrs-jul06.xml.gz";
		
		def testsuite = new XmlSlurper().parse(new GZIPInputStream(new FileInputStream(filename)));
		testsuite.usr.each {
			def id = it.@id.text();
			def codecname = it.@codec.text();
			def usr = it.@string.text();
			boolean exception = false, solvable = true;
			
			println "Checking testcase ${id} ..."

			def codec = manager.getInputCodecForName(codecname, [:]);
			
			graph.clear();
			labels.clear();
			
			try {
				codec.decode(new StringReader(usr), graph, labels);
				exception = false;
			} catch(ParserException e) {
				assert getError(it) == ExitCodes.PARSING_ERROR_INPUT_GRAPH : "Expected parsing error for testcase ${id}";
				exception = true;
			} catch(MalformedDomgraphException e) {
				assert getError(it) == ExitCodes.MALFORMED_DOMGRAPH_BASE_INPUT + e.getExitcode() : "Expected semantic error for testcase ${id}";
				exception = true;
			} catch(IOException e) {
				// shouldn't happen
			}
			
			if( !exception ) {
				domconInputCodec.decode(new StringReader(it.domgraph.@string.text()), goldGraph, goldLabels);
				assert DomGraph.isEqual(graph, labels, goldGraph, goldLabels) : "Testcase ${id} doesn't match gold graph";
				
				assertClassifyCorrect(it);
				assertSolveCorrect(it, graph);
			}
		}
		
		assert true;
	}
	
	private int getError(def it) {
		return Integer.parseInt(it.domgraph.@error.text());
	}
	
	
	private void assertSolveCorrect(def it, DomGraph graph) {
		Chart chart = new Chart();
		boolean solvable = ChartSolver.solve(graph,chart);
		
		if( it.solve.@solvable.text() == 'false' ) {
			assert !solvable : "Expected testcase ${id} to be unsolvable";
		} else {
			assert solvable : "Expected testcase ${id} to be solvable";
			assert chart.size() == Integer.parseInt(it.solve.@chartsize.text()) : "Testcase ${id} doesn't match expected chart size";
			assert chart.countSolvedForms() == new BigInteger(it.solve.@count.text()) : "Testcase ${id} doesn't match expected number of solved forms";
		}
	}

	
	private void assertClassifyCorrect(def it) {
		int classification = 0;
		int goldClassification = Integer.parseInt(it.classify.@code.text());
		
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
