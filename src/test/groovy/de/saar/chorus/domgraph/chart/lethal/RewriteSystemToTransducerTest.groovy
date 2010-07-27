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
import de.saar.chorus.contexttransducer.*;

/**
 *
 * @author koller
 */
class RewriteSystemToTransducerTest {
    @Test
    public void testConversion() throws Exception {
        RewritingSystemParser parser = new RewritingSystemParser();
        RewriteSystem weakening = new RewriteSystem(true);
        RewriteSystem equivalence = new RewriteSystem(false);
        Annotator ann = new Annotator();

        DomGraph graph = new DomGraph();
        NodeLabels labels = new NodeLabels();
        TestingTools.decodeDomcon(domgraph, graph, labels);

        parser.read(new StringReader(RewritingSystemParserTest.testRewriting), weakening, equivalence, ann);
        RewriteSystemToTransducer converter = new RewriteSystemToTransducer(ann);
        converter.addRewriteSystem(weakening);
        ContextTreeTransducer<RankedSymbol,RankedSymbol,State> ctt = converter.convert(graph,labels);

        // some type 2 rules
        assert ctt.getRules().contains(parseRule("every_x(q_+:1,qbar:2) -> q_-, every_x(1,2)"));

        // some type 3 rules
        assert ctt.getRules().contains(parseRule("a_y(qbar:1,every_x(qbar:2,qbar:3)) -> q_+, every_x(2,a_y(1,3))"));
        assert ctt.getRules().contains(parseRule("every_x(qbar:1,a_y(qbar:2,qbar:3)) -> q_-, a_y(2,every_x(1,3))"));
    }

    @Test
    public void testConversionCrazyAnnotations() throws Exception {
        RewritingSystemParser parser = new RewritingSystemParser();
        RewriteSystem weakening = new RewriteSystem(true);
        RewriteSystem equivalence = new RewriteSystem(false);
        Annotator ann = new Annotator();

        DomGraph graph = new DomGraph();
        NodeLabels labels = new NodeLabels();
        TestingTools.decodeDomcon(domgraph, graph, labels);

        parser.read(new StringReader(testRewritingCrazyAnnotations), weakening, equivalence, ann);
        RewriteSystemToTransducer converter = new RewriteSystemToTransducer(ann);
        converter.addRewriteSystem(weakening);
        ContextTreeTransducer<RankedSymbol,RankedSymbol,State> ctt = converter.convert(graph,labels);


        System.out.println("Crazy ctt:");
        System.out.println(ctt);

        
        // some type 2 rules
        assert ctt.getRules().contains(parseRule("every_x(q_+:1,qbar:2) -> q_-, every_x(1,2)")); // this is simply annotation rule 3
        assert ctt.getRules().contains(parseRule("a_y(q_+:1,qbar:2) -> q_+, a_y(1,2)"));         // rule 1


        assert ctt.getRules().contains(parseRule("a_y(q_0:1,qbar:2) -> q_-, a_y(1,2)")); // these are rules for undefined transitions for "a"
        assert ctt.getRules().contains(parseRule("a_y(q_0:1,qbar:2) -> q_0, a_y(1,2)"));
        assert ! ctt.getRules().contains(parseRule("a_y(q_0:1,qbar:2) -> q_+, a_y(1,2)")); // this should be blocked because there is a rule for +:a
    }

    private static Rule parseRule(String x) {
        ContextTreeTransducer<RankedSymbol, RankedSymbol, State> ctt = ContextTreeTransducerParser.parseString(x);
        return ctt.getRules().get(0);
    }

    public static String domgraph = """
    [label(x every(x1 x2)) label(y a(y1 y2)) label(z loves) label(w1 man) label(w2 woman)
     dom(x1 w1) dom(y1 w2) dom(x2 z) dom(y2 z)]""";




    public static String testRewritingCrazyAnnotations = """

// weakening rules
[+] a(X, every(Y,Z)) -> every(Y, a(X,Z))
[-] every(Y, a(X,Z)) -> a(X, every(Y,Z))

// equivalence rules
pron_rel(X, *[Y]) = *[pron_rel(X,Y)]

// annotator
start annotation: +
neutral annotation: 0

+: a(+,+)

+: every(-,+)
-: every(+,-)

    """;
}

