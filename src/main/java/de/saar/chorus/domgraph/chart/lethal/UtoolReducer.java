/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.chorus.domgraph.chart.lethal;

import de.saar.chorus.domgraph.chart.Chart;
import de.saar.chorus.domgraph.chart.ChartSolver;
import de.saar.chorus.domgraph.chart.SolverNotApplicableException;
import de.saar.chorus.domgraph.codec.CodecManager;
import de.saar.chorus.domgraph.codec.CodecRegistrationException;
import de.saar.chorus.domgraph.codec.InputCodec;
import de.saar.chorus.domgraph.codec.MalformedDomgraphException;
import de.saar.chorus.domgraph.codec.ParserException;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.DomGraph.PreprocessingException;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.uni_muenster.cs.sev.lethal.languages.RegularTreeLanguage;
import de.uni_muenster.cs.sev.lethal.symbol.common.RankedSymbol;
import de.uni_muenster.cs.sev.lethal.tree.common.Tree;
import de.uni_muenster.cs.sev.lethal.treeautomata.common.FTA;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 *
 * @author koller
 */
public class UtoolReducer {

    public static void main(String[] args) throws Exception {
        boolean verbose = false;
        String usrFilename = null;
        String rulesFilename = null;

        // parse command-line
        if( args.length == 3 ) {
            verbose = args[0].equals("--verbose");
            usrFilename = args[1];
            rulesFilename = args[2];
        } else if( args.length == 2 ) {
            usrFilename = args[0];
            rulesFilename = args[1];
        } else {
            System.out.println("Usage: UtoolReducer [--verbose] <usrFilename> <rulesFilename>");
            System.exit(1);
        }


        // read USR and convert it to domgraph
        DomGraph graph = new DomGraph();
        NodeLabels labels = new NodeLabels();
        load(usrFilename, graph, labels);

        // solve USR
        Chart chart = solve(labels, graph);

        // obtain rewrite systems
        RewriteSystem weakening = new RewriteSystem();
        RewriteSystem equivalence = new RewriteSystem();
        Annotator annotator = new Annotator();
        loadRewriteSystem( rulesFilename, weakening, equivalence, annotator);

        // convert USR to FTA and reduce it
        RelativeNormalFormsComputer rnfc = new RelativeNormalFormsComputer(weakening, equivalence, annotator);
        rnfc.setVerbose(verbose);
        FTA fta = rnfc.reduce(chart, graph, labels);
        RegularTreeLanguage<RankedSymbol> rtl = new RegularTreeLanguage(fta);
        long count = 0;
        for (Tree t : rtl) {
            count ++;
        }

        System.out.println("found " + count + " readings after reduction");
    }

    public static void loadRewriteSystem(String filename, RewriteSystem weakening, RewriteSystem equivalence, Annotator annotator) throws Exception {
        RewritingSystemParser parser = new RewritingSystemParser();
        parser.read(new FileReader(new File(filename)), weakening, equivalence, annotator);
    }

    public static Chart solve(NodeLabels labels, DomGraph graph) throws PreprocessingException, SolverNotApplicableException {
        Chart chart = new Chart(labels);
        DomGraph preprocessed = graph.preprocess();
        boolean solvable = ChartSolver.solve(preprocessed, chart);
        if (!solvable) {
            System.err.println("USR is not solvable, aborting.");
        } else {
            System.err.println("USR is solvable, " + chart.countSolvedForms() + " readings.");
        }
        return chart;
    }

    public static void load(String filename, DomGraph graph, NodeLabels labels) throws CodecRegistrationException, FileNotFoundException, IOException, ParserException, MalformedDomgraphException {
        CodecManager codecman = new CodecManager();
        codecman.registerAllDeclaredCodecs();

        // read USR and convert it to domgraph
        InputCodec codec = codecman.getInputCodecForFilename(filename, "");
        codec.decode(new FileReader(new File(filename)), graph, labels);
    }
}
