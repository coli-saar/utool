package de.saar.testingtools;

import de.saar.chorus.domgraph.codec.*;
import de.saar.chorus.domgraph.codec.domcon.*;
import de.saar.chorus.domgraph.graph.*;

import java.io.*;

public class TestingTools {
	private static InputCodec dc = new DomconOzInputCodec();
	
	public static void decodeDomcon(String domcon, DomGraph graph, NodeLabels labels) {
		dc.decode(new StringReader(domcon), graph, labels);
	}
	
	
	public static void expectException(Class exceptionType, Closure someCode) {
		try {
			someCode();
			assert false;
		} catch(Exception e) {
			assert exceptionType.isInstance(e)
		}
	}
}