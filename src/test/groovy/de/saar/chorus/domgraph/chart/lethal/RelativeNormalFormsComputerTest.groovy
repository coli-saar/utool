/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.saar.chorus.domgraph.chart.lethal
import org.junit.*
import de.saar.chorus.domgraph.graph.*;
import de.saar.chorus.domgraph.chart.*;
import de.saar.testingtools.TestingTools;
import de.uni_muenster.cs.sev.lethal.treeautomata.easy.*
import de.uni_muenster.cs.sev.lethal.treeautomata.generic.*
import de.uni_muenster.cs.sev.lethal.parser.tree.TreeParser;
import de.uni_muenster.cs.sev.lethal.languages.*;
import de.uni_muenster.cs.sev.lethal.tree.common.*;
import de.uni_muenster.cs.sev.lethal.treeautomata.common.FTA

import de.uni_muenster.cs.sev.lethal.treeautomata.generic.GenFTAOps;

/**
 *
 * @author koller
 */
class RelativeNormalFormsComputerTest {
    @Test
    public void test1() {
        DomGraph graph = new DomGraph();
        NodeLabels labels = new NodeLabels();
        TestingTools.decodeDomcon(RewriteSystemToTransducerTest.domgraph, graph, labels);

        Chart chart = new Chart();
        DomGraph preprocessed = graph.preprocess();
	assert ChartSolver.solve(preprocessed, chart) == true;

        RewritingSystemParser parser = new RewritingSystemParser();
        RewriteSystem weakening = new RewriteSystem(true);
        RewriteSystem equivalence = new RewriteSystem(false);
        Annotator ann = new Annotator();
        parser.read(new StringReader(RewritingSystemParserTest.testRewriting), weakening, equivalence, ann);

        RelativeNormalFormsComputer rnfc = new RelativeNormalFormsComputer(weakening, equivalence, ann);
        FTA fta = rnfc.reduce(chart, graph, labels);

        System.out.println("\n\n\nreduced fta: " + GenFTAOps.reduceFull(fta));

        ChartToFTATest.assertTreeSetEquality(new RegularTreeLanguage(fta),
            ["every_x(man_w1,a_y(woman_w2,loves_z))"]);
    }
}

