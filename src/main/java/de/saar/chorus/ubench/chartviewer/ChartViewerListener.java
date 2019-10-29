package de.saar.chorus.ubench.chartviewer;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import de.saar.basic.XMLFilter;
import de.saar.chorus.domgraph.chart.ConcreteRegularTreeGrammar;
import de.saar.chorus.domgraph.chart.GraphBasedNonterminal;
import de.saar.chorus.domgraph.chart.RegularTreeGrammar;
import de.saar.chorus.domgraph.chart.SolvedFormIterator;
import de.saar.chorus.domgraph.chart.SolvedFormSpec;
import de.saar.chorus.domgraph.chart.Split;
import de.saar.chorus.domgraph.chart.lethal.Annotator;
import de.saar.chorus.domgraph.chart.lethal.EquivalenceRulesComparator;
import de.saar.chorus.domgraph.chart.lethal.RelativeNormalFormsComputer;
import de.saar.chorus.domgraph.chart.lethal.RewriteSystem;
import de.saar.chorus.domgraph.chart.lethal.RewritingSystemParser;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.ubench.Ubench;

/**
 * This <code>ActionListener</code> processes all actions
 * of one (and only one) <code>ChartViewer</code>.
 *
 * @see de.saar.chorus.ubench.jdomgraph.chartviewer.ChartViewer
 * @author Michaela Regneri
 *
 */
public class ChartViewerListener implements ActionListener {

    // the chart viewer
    private final ChartViewer viewer;
    private RelativeNormalFormsComputer rnfc = null;
    private String rnfcName = null;

    /**
     * A new <code>ChartViewerListener</code>
     * initalised with its <code>ChartViewer</code>.
     *
     * @param cv the chart viewer
     */
    ChartViewerListener(ChartViewer cv) {
        viewer = cv;
    }

    /**
     * This processes all events occuring within
     * the <code>ChartViewer</code>.
     */
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();

        // redundancy elimination
        if (command.equals("elredglobal") || command.equals("elred")) {
            // if the graph is not normal, abort with error message
            if (!viewer.getDg().isNormal()) {
                JOptionPane.showMessageDialog(viewer,
                        "This is the chart of a graph which is not normal,"
                        + System.getProperty("line.separator")
                        + "so Utool cannot eliminate redundancies.",
                        "Server Error",
                        JOptionPane.ERROR_MESSAGE);

                return;
            }

            // if the graph is not hnc, abort with error message
            if (!viewer.getDg().isHypernormallyConnected()) {
                JOptionPane.showMessageDialog(viewer,
                        "This is the chart of a graph which is not hypernormally"
                        + System.getProperty("line.separator")
                        + "connected, so Utool cannot eliminate redundancies.",
                        "Server Error",
                        JOptionPane.ERROR_MESSAGE);

                return;
            }

            rnfc = null;
            rnfcName = null;

            // obtain the equation system and its name
            if (command.equals("elredglobal")) {
                // if the command was "reduce with global eq. system", get them from
                // Ubench
                rnfc = Ubench.getInstance().getRelativeNormalFormsComputer();
                rnfcName = Ubench.getInstance().getEqsname();
            } else {
                // otherwise, display a dialog that prompts for a filename and load
                // it from there
                loadRelativeNormalFormComputerRules(false);
            }

            // finally, reduce the chart and refresh the display
            if ((rnfc != null) && (rnfcName != null)) {
                viewer.reduceChart(rnfc, rnfcName);
                viewer.refreshChartWindow();
            }


        } else if (command.equals("delSplit")) {
            // a Split was deleted

            Split<GraphBasedNonterminal> selectedSplit = viewer.getSelectedSplit();
            if (selectedSplit != null) {
                RegularTreeGrammar<GraphBasedNonterminal> chartAsRtg = viewer.getChart();

                if (chartAsRtg instanceof ConcreteRegularTreeGrammar) {
                    ConcreteRegularTreeGrammar<GraphBasedNonterminal> chart = (ConcreteRegularTreeGrammar<GraphBasedNonterminal>) chartAsRtg;

                    try {
                        // remove the split from the chart itself
                        GraphBasedNonterminal subgraph = viewer.getSubgraphForMarkedSplit();
                        List<Split<GraphBasedNonterminal>> splits = new ArrayList<Split<GraphBasedNonterminal>>(chart.getSplitsFor(subgraph));

                        splits.remove(selectedSplit);
                        chart.setSplitsForSubgraph(subgraph, splits);
                        viewer.refreshChartWindow();

                    } catch (UnsupportedOperationException ex) {
                        // a split which may not been removed
                        JOptionPane.showMessageDialog(viewer,
                                "You cannot delete the selected Split."
                                + System.getProperty("line.separator")
                                + "There has to be at least one Split left for each subgraph.",
                                "Split not deleted",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(viewer,
                            "You cannot delete splits in this chart.",
                            "Split not deleted",
                            JOptionPane.ERROR_MESSAGE);
                }

            }


        } else if (command.equals("solvechart")) {
            // display the first solved form of the chart

            RegularTreeGrammar<GraphBasedNonterminal> chart = viewer.getChart();
            DomGraph firstForm = (DomGraph) viewer.getDg().clone();
            SolvedFormIterator<GraphBasedNonterminal> sfi = new SolvedFormIterator<GraphBasedNonterminal>(ConcreteRegularTreeGrammar.makeExplicit(chart), firstForm);
//                    (ConcreteRegularTreeGrammar<GraphBasedNonterminal>) chart.clone(), firstForm);
            SolvedFormSpec spec = sfi.next();
            firstForm = firstForm.makeSolvedForm(spec);
            NodeLabels labels = viewer.getLabels().makeSolvedForm(spec);




            Ubench.getInstance().addSolvedFormTab(viewer.getTitle() + "  SF #1",
                    firstForm, sfi, 1, chart.countSolvedForms().longValue(),
                    viewer.getDg(), labels, viewer.getTitle(), true);



        } else if (command.equals("resetchart")) {
            // display the original chart again

            viewer.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            viewer.resetChart();
            viewer.setCursor(Cursor.getDefaultCursor());
        } else if (command.equals("chartinfo")) {
            // display information on the chart

            viewer.showInfoPane();
        } else if (command.equals("closechart")) {
            // close the window

            viewer.setVisible(false);
        }

    }

    /**
     * This loads a xml file and reads the content
     * to a xml file.
     *
     * @param preliminary indicates whether or not to show the info message
     * @param eqs the equation system to fill
     * @return the (file) name of the equation system loaded
     */
    private void loadRelativeNormalFormComputerRules(boolean preliminary) {
        /** TODO Why would we ever want to display this warning dialog?? - AK **/
        if (preliminary) {
            JOptionPane.showMessageDialog(viewer,
                    "You have to specify a xml file that contains your equation system"
                    + System.getProperty("line.separator")
                    + " before Utool can eliminate equivalences.",
                    "Please load an equation system",
                    JOptionPane.INFORMATION_MESSAGE);
        }


        JFileChooser fc = new JFileChooser();

        fc.setDialogTitle("Select the equation system");
        fc.setFileFilter(new XMLFilter());
        fc.setCurrentDirectory(Ubench.getInstance().getLastPath());
        int fcVal = fc.showOpenDialog(viewer);

        if (fcVal == JFileChooser.APPROVE_OPTION) {

            viewer.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            File file = fc.getSelectedFile();

            try {
                // obtain rewrite systems
                RewriteSystem weakening = new RewriteSystem(true);
                RewriteSystem equivalence = new RewriteSystem(false);
                Annotator annotator = new Annotator();
                RewritingSystemParser parser = new RewritingSystemParser();
                parser.read(new FileReader(file), weakening, equivalence, annotator);

                // build rnf computer and save it in the abstract options
                rnfc = new RelativeNormalFormsComputer(annotator);
                rnfc.addRewriteSystem(weakening);
                rnfc.addRewriteSystem(equivalence, new EquivalenceRulesComparator());

                rnfcName = file.getName();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(viewer,
                        "The rule system cannot be parsed."
                        + System.getProperty("line.separator")
                        + "Either the input file is not readable, or it contains syntax errors.",
                        "Error while loading equation system",
                        JOptionPane.ERROR_MESSAGE);
            }


            Ubench.getInstance().setLastPath(file.getParentFile());
            viewer.setCursor(Cursor.getDefaultCursor());
            viewer.refreshTitleAndStatus();
        }
    }
}
