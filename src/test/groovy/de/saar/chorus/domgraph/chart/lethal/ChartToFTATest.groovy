/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.saar.chorus.domgraph.chart.lethal

import org.junit.*;
import de.saar.chorus.domgraph.graph.*;
import de.saar.chorus.domgraph.chart.*;
import de.saar.testingtools.TestingTools;
import de.uni_muenster.cs.sev.lethal.treeautomata.easy.*
import de.uni_muenster.cs.sev.lethal.parser.tree.TreeParser;
import de.uni_muenster.cs.sev.lethal.languages.*;
import de.uni_muenster.cs.sev.lethal.tree.common.*;

/**
 *
 * @author koller
 */
class ChartToFTATest {

    @Test
    public void testConvert() {
        DomGraph graph = new DomGraph();
        NodeLabels labels = new NodeLabels();

        TestingTools.decodeDomcon("[label(x every_1(x1)) label(y a_2(y2)) label(z love_3) dom(x1 z) dom(y2 z)]", graph, labels);

        Chart chart = new Chart();
        DomGraph preprocessed = graph.preprocess();
	assert ChartSolver.solve(preprocessed, chart) == true;

        EasyFTA fta = ChartToFTA.convert(chart, graph, labels);
        System.out.println(fta);

        assertTreeSetEquality(new RegularTreeLanguage(fta),
            ["a_2(every_1(love_3))", "every_1(a_2(love_3))"]);
    }

    @Test
    public void testConvertNonCompact() {
        DomGraph graph = new DomGraph();
        NodeLabels labels = new NodeLabels();

        TestingTools.decodeDomcon("[label(x f(x3 x2)) label(x3 a) label(x2 g(x1)) label(y h(y2 y3)) label(y3 c) label(z d) dom(x1 z) dom(y2 z)]", graph, labels);

        Chart chart = new Chart();
        DomGraph preprocessed = graph.preprocess();
	assert ChartSolver.solve(preprocessed, chart) == true;

        System.out.println("chart= " + chart);

        EasyFTA fta = ChartToFTA.convert(chart, graph, labels);
        System.out.println(fta);

        assertTreeSetEquality(new RegularTreeLanguage(EasyFTAOps.minimize(EasyFTAOps.determinize(fta))),
            ["f(a,g(h(d,c)))", "h(f(a,g(d)),c)"]);
    }

    private static void assertTreeSetEquality(Iterable<Tree> found, List<String> gold) {
        Set s1 = new HashSet();
        Set s2 = new HashSet();

        for( Tree t : found ) {
            s1.add(t);
        }

        for( String s : gold ) {
            s2.add(TreeParser.parseString(s));
        }

        assert s1.equals(s2) : "found: " + s1 + ", gold=" + s2;
    }
	
}

