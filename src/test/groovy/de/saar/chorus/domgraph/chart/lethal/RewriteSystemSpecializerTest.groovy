/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.saar.chorus.domgraph.chart.lethal

import org.junit.*

import de.saar.testingtools.*;
import de.saar.chorus.domgraph.graph.*;


/**
 *
 * @author koller
 */
class RewriteSystemSpecializerTest {
    @Test
    public void testSpecializing() throws Exception {
        RewritingSystemParser parser = new RewritingSystemParser();
        RewriteSystem weakening = new RewriteSystem(true);
        RewriteSystem equivalence = new RewriteSystem(false);
        Annotator ann = new Annotator();

        parser.read(new StringReader(RewritingSystemParserTest.testRewriting), weakening, equivalence, ann);


        DomGraph graph = new DomGraph();
        NodeLabels labels = new NodeLabels();
        TestingTools.decodeDomcon(RewriteSystemToTransducerTest.domgraph, graph, labels);

        RewriteSystemSpecializer specializer = new RewriteSystemSpecializer(graph, labels);
        System.out.println("Specialized weakening system:");
        System.out.println(specializer.specialize(weakening));
    }
}

