/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.saar.chorus.domgraph.chart.lethal
import org.junit.*
import de.uni_muenster.cs.sev.lethal.symbol.common.*
import de.uni_muenster.cs.sev.lethal.tree.common.*
import de.uni_muenster.cs.sev.lethal.states.State;
import de.saar.testingtools.*;
import de.saar.chorus.domgraph.graph.*;
import de.saar.chorus.contexttransducer.ContextTreeTransducer

/**
 *
 * @author koller
 */
class RewriteSystemToTransducerTest {
    @Test
    public void testParsing() throws Exception {
        RewritingSystemParser parser = new RewritingSystemParser();
        RewriteSystem weakening = new RewriteSystem(true);
        RewriteSystem equivalence = new RewriteSystem(false);
        Annotator ann = new Annotator();

        DomGraph graph = new DomGraph();
        NodeLabels labels = new NodeLabels();
        TestingTools.decodeDomcon(domgraph, graph, labels);

        parser.read(new StringReader(RewritingSystemParserTest.testRewriting), weakening, equivalence, ann);
        RewriteSystemToTransducer converter = new RewriteSystemToTransducer(weakening, equivalence, ann);
        ContextTreeTransducer<RankedSymbol,RankedSymbol,State> ctt = converter.convert(graph,labels);

        System.out.println(ctt);
    }

    public static String domgraph = """
    [label(x every(x1 x2)) label(y a(y1 y2)) label(z loves) label(w1 man) label(w2 woman)
     dom(x1 w1) dom(y1 w2) dom(x2 z) dom(y2 z)]""";
}

