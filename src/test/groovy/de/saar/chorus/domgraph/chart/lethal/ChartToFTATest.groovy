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
import de.uni_muenster.cs.sev.lethal.treeautomata.common.FTAProperties;
import de.uni_muenster.cs.sev.lethal.grammars.generic.GenRTG;
import de.uni_muenster.cs.sev.lethal.states.NamedState;
import de.uni_muenster.cs.sev.lethal.treeautomata.common.FTA
import de.uni_muenster.cs.sev.lethal.symbol.common.RankedSymbol;
import de.uni_muenster.cs.sev.lethal.treeautomata.generic.*
import de.uni_muenster.cs.sev.lethal.states.State;
import de.saar.chorus.contexttransducer.PairState;

import de.saar.testingtools.*;


/**
 *
 * @author koller
 */
class ChartToFTATest {

    @Test
    public void testConvert() {
        DomGraph graph = new DomGraph();
        NodeLabels labels = new NodeLabels();

        TestingTools.decodeDomcon("[label(x every(x1)) label(y a(y2)) label(z love) dom(x1 z) dom(y2 z)]", graph, labels);

        Chart chart = new Chart();
        DomGraph preprocessed = graph.preprocess();
	assert ChartSolver.solve(preprocessed, chart) == true;

        EasyFTA fta = ChartToLethal.convertToFta(chart, graph, labels);
//        System.out.println(fta);

        assertTreeSetEquality(new RegularTreeLanguage(fta),
            ["a_y(every_x(love_z))", "every_x(a_y(love_z))"]);
    }

    @Test
    public void testConvertNonCompact() {
        DomGraph graph = new DomGraph();
        NodeLabels labels = new NodeLabels();

        TestingTools.decodeDomcon("[label(x f(x3 x2)) label(x3 a) label(x2 g(x1)) label(y h(y2 y3)) label(y3 c) label(z d) dom(x1 z) dom(y2 z)]", graph, labels);

        Chart chart = new Chart();
        DomGraph preprocessed = graph.preprocess();
	assert ChartSolver.solve(preprocessed, chart) == true;

//        System.out.println("chart= " + chart);

        EasyFTA fta = ChartToLethal.convertToFta(chart, graph, labels);
//        System.out.println(fta);

        assertTreeSetEquality(new RegularTreeLanguage(fta),
            ["f_x(a_x3,g_x2(h_y(d_z,c_y3)))", "h_y(f_x(a_x3,g_x2(d_z)),c_y3)"]);
    }

    @Test
    public void testRondane1Rtg() {
        DomGraph graph = new DomGraph();
        NodeLabels labels = new NodeLabels();

        TestingTools.decodeDomcon(rondane1, graph, labels);

        Chart chart = new Chart();
        DomGraph preprocessed = graph.preprocess();
	assert ChartSolver.solve(preprocessed, chart) == true;

        System.out.println("chart= " + chart);

        GenRTG rtg = ChartToLethal.convertToRtg(chart, graph, labels);
        System.out.println(rtg);

        assert rtg.getStates().any { it.toString().equals("G(q_h35_h39_h44)")} : "states of rtg = " + rtg.getStates();

//        assert rtg.getStates().contains(new NamedState("q_h35_h39_h44"));
    }

    @Test
    public void testRondane1() {
        DomGraph graph = new DomGraph();
        NodeLabels labels = new NodeLabels();

        TestingTools.decodeDomcon(rondane1, graph, labels);

        Chart chart = new Chart();
        DomGraph preprocessed = graph.preprocess();
	assert ChartSolver.solve(preprocessed, chart) == true;

        System.out.println("chart= " + chart);

        EasyFTA fta = ChartToLethal.convertToFta(chart, graph, labels);
        System.out.println(fta);

        assert lang(fta).size() == 132 : "L(fta) has " + lang(fta).size() + " trees";
        assert fta.getRules().any { it.getDestState().toString().equals("q_h35_h39_h44") };
    }

    @Test
    public void testBackConversion() {
       DomGraph graph = new DomGraph();
        NodeLabels labels = new NodeLabels();

        TestingTools.decodeDomcon("[label(x f(x3 x2)) label(x3 a) label(x2 g(x1)) label(y h(y2 y3)) label(y3 c) label(z d) dom(x1 z) dom(y2 z)]", graph, labels);


        Chart chart = new Chart();
        DomGraph preprocessed = graph.preprocess();
	assert ChartSolver.solve(preprocessed, chart) == true;

        RewritingSystemParser parser = new RewritingSystemParser();
        RewriteSystem weakening = new RewriteSystem(true);
        RewriteSystem equivalence = new RewriteSystem(false);
        Annotator ann = new Annotator();
        parser.read(new StringReader(RewritingSystemParserTest.testRewriting), weakening, equivalence, ann);

        RelativeNormalFormsComputer rnfc = new RelativeNormalFormsComputer(ann);
        GenFTA<RankedSymbol, PairState<State, String>> fta = rnfc.reduce(chart, graph, labels);

        // no rewriting at all => output fta should have same language
        assertTreeSetEquality(new RegularTreeLanguage(fta),
            ["f_x(a_x3,g_x2(h_y(d_z,c_y3)))", "h_y(f_x(a_x3,g_x2(d_z)),c_y3)"]);

        System.err.println("before conversion: " + fta);

        RegularTreeGrammar<DecoratedNonterminal<SubgraphNonterminal,String>> outChart = ChartToLethal.convertFtaToChart(fta, graph);
        System.err.println("reconverted chart: " + outChart);

        SolvedFormIterator sfi = new SolvedFormIterator<DecoratedNonterminal<SubgraphNonterminal,String>>(outChart, graph);
        List sfs = TestingTools.collectIteratorValues(sfi);

        List goldSfs = [ [[["x1", "y"], ["y2", "z"]], [:]],   [[["y2", "x"], ["x1","z"]], [:]] ];

        assert TestingTools.solvedFormsEqual(sfs, goldSfs) : "[" + id + "] sfs = " + sfs;

        BigInteger count = outChart.countSolvedForms();
        assert count.equals(new BigInteger(2)) : "found " + count + " solved forms, expected 2";
    }

     private static Set lang(EasyFTA fta) throws Exception {
        if (FTAProperties.finiteLanguage(fta)) {
            Set ret = new HashSet();
            RegularTreeLanguage l = new RegularTreeLanguage(fta);
            Iterator it = l.iterator();

            while( it.hasNext() ) {
                ret.add(it.next());
            }

            return ret;
        } else {
            throw new Exception("fta's language is infinite");
        }
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


    private static String rondane1 = "[label(h1 'prpstn_m'(h3)) label(h6 '_the_q'(h8 h7)) label(h18 'proper_q'(h19 h20)) label(h21 named) label(h12 '_valley_n_of'(h10 h14)) label(h10 '_well+known_a_1') label(h14 '_historic_a_1') label(h28 'udef_q'(h29 h30)) label(h23 '_once_a_1') label(h32 '_the_q'(h34 h33)) label(h26 card) label(h39 '_the_q'(h41 h40)) label(h35 '_between_p') label(h50 'proper_q'(h51 h52)) label(h53 named) label(h44 '_of_p'(h42 h46)) label(h42 '_eastern_a_1') label(h46 '_western_a_1') dom(h7 h23) dom(h20 h12) dom(h33 h26) dom(h30 h23) dom(h40 h35) dom(h51 h53) dom(h41 h44) dom(h34 h35) dom(h29 h26) dom(h19 h21) dom(h8 h12) dom(h52 h44) dom(h3 h6) dom(h3 h18) dom(h3 h28) dom(h3 h32) dom(h3 h39) dom(h3 h50)]";
}

