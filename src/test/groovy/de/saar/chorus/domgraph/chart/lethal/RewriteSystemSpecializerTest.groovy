/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.saar.chorus.domgraph.chart.lethal

import org.junit.*

import de.saar.testingtools.*;
import de.saar.chorus.domgraph.graph.*;
import java.util.*;
import de.saar.chorus.term.*;
import de.saar.chorus.domgraph.chart.lethal.RewriteSystem.Rule;

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
        
        RewriteSystem sWeakening = specializer.specialize(weakening);
        assert sWeakening.getAllRules().contains(parseWeakeningRule("[+] a_y(X, every_x(Y,Z)) -> every_x(Y, a_y(X,Z))")) : sWeakening.getAllRules();
    }

    @Test
    public void testSpecializingWildcard() throws Exception {
        RewritingSystemParser parser = new RewritingSystemParser();
        RewriteSystem weakening = new RewriteSystem(true);
        RewriteSystem equivalence = new RewriteSystem(false);
        Annotator ann = new Annotator();

        parser.read(new StringReader(RewritingSystemParserTest.testRewriting), weakening, equivalence, ann);


        DomGraph graph = new DomGraph();
        NodeLabels labels = new NodeLabels();
        TestingTools.decodeDomcon(domgraphWithWildcard, graph, labels);

        RewriteSystemSpecializer specializer = new RewriteSystemSpecializer(graph, labels);

        RewriteSystem sEquivalence = specializer.specialize(equivalence, new DummyComparator());
        assert sEquivalence.getAllRules().contains(parseEquivalenceRule("pron_rel_x(X, every_y(WW1, Y)) = every_y(WW1, pron_rel_x(X,Y))")) : sEquivalence.getAllRules();
    }

    private static Rule parseEquivalenceRule(String rule) {
        RewritingSystemParser parser = new RewritingSystemParser();
        RewriteSystem weakening = new RewriteSystem(true);
        RewriteSystem equivalence = new RewriteSystem(false);
        Annotator ann = new Annotator();

        parser.read(new StringReader(rule), weakening, equivalence, ann);

        Rule ret = equivalence.getAllRules().get(0);
        ret.ordered = true;

        return ret;
    }

    private static Rule parseWeakeningRule(String rule) {
        RewritingSystemParser parser = new RewritingSystemParser();
        RewriteSystem weakening = new RewriteSystem(true);
        RewriteSystem equivalence = new RewriteSystem(false);
        Annotator ann = new Annotator();

        parser.read(new StringReader(rule), weakening, equivalence, ann);

        return weakening.getAllRules().get(0);
    }


    public static final String domgraphWithWildcard = "[label(x pron_rel(x1 x2)) label(y every(y1 y2)) label(w1 foo) label(w2 bar) label(z loves) dom(x1 w1) dom(y1 w2) dom(x2 z) dom(y2 z)]";
}


class DummyComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            return 0;
        }
    }
