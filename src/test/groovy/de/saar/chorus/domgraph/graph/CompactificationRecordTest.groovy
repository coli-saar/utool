/**
 * 
 */
package de.saar.chorus.domgraph.graph

import de.saar.chorus.domgraph.codec.domcon.*;
import de.saar.chorus.domgraph.codec.*;
import de.saar.testingtools.*;
import java.io.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(value=Parameterized.class)
public class CompactificationRecordTest {
	private DomGraph graph;
	private NodeLabels labels;
	private String id;
	private Map gold;
	
	@Parameters
	public static data() {
		return [ prepare("compact", "[label(x f(x1 x2))]", ["x":[[["x",0]], [["x",1]]]]),
		         prepare("two compacts", "[label(x f(x1 x2)) label(y g(y1 y2))]", ["x":[[["x",0]], [["x",1]]], "y":[[["y",0]], [["y",1]]]]),
		         prepare("depth 2", "[label(x f(x1 x2)) label(x1 g(x3 x4)) label(x2 h(x5))]",
		        		 ["x":[[["x",0],["x1",0]], [["x",0],["x1",1]], [["x",1],["x2",0]]]])
		];
	}
	
	@Test
	public void testCompactificationRecord() {
		//System.err.println("\n\n\nTest: " + id);
		
		CompactificationRecord record = new CompactificationRecord();
		DomGraph compact = graph.compactify(record);
		
		//System.err.println(record)
		
		gold.each { root, pathspecs ->
			pathspecs.eachWithIndex { pathspec, i ->
				List path = record.getRecord(root, i);
				for( j in 0..path.size()-1 ) {
					assert pathspec.get(j).get(0).equals(path.get(j).node);
					assert pathspec.get(j).get(1) == path.get(j).childIndex;
				}
			}
		}
	}
	
	CompactificationRecordTest(id, graphstr, gold) {
		this.id = id;
		this.gold = gold;
		
		graph = new DomGraph();
		labels = new NodeLabels();
		
		InputCodec ozcodec = new DomconOzInputCodec();
		ozcodec.decode(new StringReader(graphstr), graph, labels);
	}
	
	private static prepare(id, graphstr, gold) {
		return (Object[]) [id, graphstr, gold];
	}
	
}
