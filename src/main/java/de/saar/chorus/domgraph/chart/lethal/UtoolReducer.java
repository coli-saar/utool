/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.saar.chorus.domgraph.chart.lethal;

import de.saar.chorus.domgraph.chart.Chart;
import de.saar.chorus.domgraph.chart.ChartSolver;
import de.saar.chorus.domgraph.codec.CodecManager;
import de.saar.chorus.domgraph.codec.InputCodec;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.uni_muenster.cs.sev.lethal.languages.RegularTreeLanguage;
import de.uni_muenster.cs.sev.lethal.symbol.common.RankedSymbol;
import de.uni_muenster.cs.sev.lethal.tree.common.Tree;
import de.uni_muenster.cs.sev.lethal.treeautomata.common.FTA;
import java.io.File;
import java.io.FileReader;

/**
 *
 * @author koller
 */
public class UtoolReducer {
    public static void main(String[] args) throws Exception {
        CodecManager codecman = new CodecManager();
        codecman.registerAllDeclaredCodecs();

        // read USR and convert it to domgraph
        InputCodec codec = codecman.getInputCodecForFilename(args[0], "");
        DomGraph graph = new DomGraph();
        NodeLabels labels = new NodeLabels();
        codec.decode(new FileReader(new File(args[0])), graph, labels);

        // solve USR
        Chart chart = new Chart(labels);
        DomGraph preprocessed = graph.preprocess();
	boolean solvable = ChartSolver.solve(preprocessed, chart);
        if( !solvable ) {
            System.err.println("USR is not solvable, aborting.");
        } else {
            System.err.println("USR is solvable, " + chart.countSolvedForms() + " readings.");
        }

        // obtain rewrite systems
        RewritingSystemParser parser = new RewritingSystemParser();
        RewriteSystem weakening = new RewriteSystem();
        RewriteSystem equivalence = new RewriteSystem();
        Annotator annotator = new Annotator();
        parser.read(new FileReader(new File(args[1])), weakening, equivalence, annotator);

        // convert USR to FTA and reduce it
        RelativeNormalFormsComputer rnfc = new RelativeNormalFormsComputer(weakening, equivalence, annotator);
        FTA fta = rnfc.reduce(chart, graph, labels);
        RegularTreeLanguage<RankedSymbol> rtl = new RegularTreeLanguage(fta);
        for(Tree t : rtl) {
            System.out.println(t);
        }
    }
}
