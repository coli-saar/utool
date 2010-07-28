/*
 * @(#)CommandListener.java created 21.07.2005
 * 
 * Copyright (c) 2005 Alexander Koller
 *  
 */
package de.saar.chorus.ubench;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import com.lowagie.text.DocumentException;

import de.saar.basic.ExportUtilities;
import de.saar.basic.GUIUtilities;
import de.saar.basic.GenericFileFilter;
import de.saar.basic.WaitingDialog;
import de.saar.basic.XMLFilter;
import de.saar.chorus.domgraph.chart.Chart;
import de.saar.chorus.domgraph.chart.SolvedFormIterator;
import de.saar.chorus.domgraph.chart.SolvedFormSpec;
import de.saar.chorus.domgraph.chart.lethal.Annotator;
import de.saar.chorus.domgraph.chart.lethal.EquivalenceRulesComparator;
import de.saar.chorus.domgraph.chart.lethal.RelativeNormalFormsComputer;
import de.saar.chorus.domgraph.chart.lethal.RewriteSystem;
import de.saar.chorus.domgraph.chart.lethal.RewritingSystemParser;
import de.saar.chorus.domgraph.codec.CodecManager;
import de.saar.chorus.domgraph.codec.MalformedDomgraphException;
import de.saar.chorus.domgraph.codec.MultiOutputCodec;
import de.saar.chorus.domgraph.codec.OutputCodec;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.domgraph.layout.LayoutAlgorithm;
import de.saar.chorus.domgraph.layout.LayoutException;
import de.saar.chorus.domgraph.layout.LayoutOptions;
import de.saar.chorus.domgraph.layout.PDFCanvas;
import de.saar.chorus.domgraph.layout.LayoutOptions.LabelType;
import de.saar.chorus.domgraph.utool.AbstractOptions;
import de.saar.chorus.domgraph.utool.server.ConnectionManager;
import de.saar.chorus.domgraph.utool.server.ConnectionManager.State;
import de.saar.chorus.ubench.Preferences.LayoutType;
import de.saar.chorus.ubench.jdomgraph.JDomGraph;

/**
 * The main <code>ActionListener</code> and <code>ItemListener</code> 
 * of Ubench's GUI. 
 * For file choosers, it provides some file filters and stores the 
 * last chosen path. 
 * 
 * @author Alexander Koller
 * @author Michaela Regneri
 *
 */
class CommandListener implements ActionListener,
        ItemListener, ConnectionManager.StateChangeListener {

    private List<GenericFileFilter> ffInputCodecs;
    private List<GenericFileFilter> ffOutputCodecs;
    private List<GenericFileFilter> ffMultiOutputCodecs;
    private Map<Object, String> eventSources;

    /**
     * Creates a new Instance of <code>CommandListener</code>.
     */
    CommandListener() {

        CodecManager codecman = Ubench.getInstance().getCodecManager();
        ConnectionManager.addListener(this);


        // initializing fields
        eventSources = new HashMap<Object, String>();

        ffInputCodecs = new ArrayList<GenericFileFilter>();
        ffOutputCodecs = new ArrayList<GenericFileFilter>();
        ffMultiOutputCodecs = new ArrayList<GenericFileFilter>();

        for (String codecname : codecman.getAllInputCodecs()) {
            String extension = codecman.getInputCodecExtension(codecname);

            if ((codecname != null) && (extension != null)) {
                ffInputCodecs.add(new GenericFileFilter(extension, codecname));
            }
        }

        for (String codecname : codecman.getAllOutputCodecs()) {
            String extension = codecman.getOutputCodecExtension(codecname);

            if ((codecname != null) && (extension != null)) {
                ffOutputCodecs.add(new GenericFileFilter(extension, codecname));
            }
        }

        for (String codecname : codecman.getAllMultiOutputCodecs()) {
            String extension = codecman.getOutputCodecExtension(codecname);

            if ((codecname != null) && (extension != null)) {
                ffMultiOutputCodecs.add(new GenericFileFilter(extension, codecname));
            }
        }

    }

    /**
     * Overwrites the <code>actionPerformed</code> method of
     * <code>ActionListener</code>.
     */
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();

        // no action command defined
        if (command == null) {

            // looking up the source of the action
            command = lookupEventSource(e.getSource());
        }

        // no command and no source
        if (command == null) {
            System.err.println("Undefined action command!");
            return;
        }


        /* Handling the known actions by identifying their command */
        try {
            if (command.equals("newTab")) {
                Ubench.getInstance().addJDomGraphTab(
                        "New Graph", new DomGraph(), new NodeLabels());

            } else if (command.equals("preferences")) {
                // show settings (so far only server settings)
                Ubench.getInstance().setPreferenceDialogVisible(true);
            } else if (command.equals("loadeqs")) {
                // load global equation system
                loadRnfc();
            } else if (command.equals("server")) {
                // start / stop server


                if (Ubench.getInstance().getMenuBar().
                        getServerButton().isSelected()) {
                    //start server
                    final AbstractOptions op = new AbstractOptions();
                    //	fetching server settings
                    op.setOptionLogging(ServerOptions.isLogging());
                    op.setLogWriter(ServerOptions.getLogwriter());
                    op.setOptionWarmup(ServerOptions.isWarmup());
                    op.setPort(ServerOptions.getPort());


                    new Thread() {

                        public void run() {
                            try {
                                ConnectionManager.startServer(op);
                            } catch (IOException ex) {
                                JOptionPane.showMessageDialog(Ubench.getInstance().getWindow(),
                                        ex.getMessage(),
                                        "Server Error",
                                        JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }.start();
                } else {
                    // stop server
                    ConnectionManager.stopServer();
                }
            } else if (command.equals("print")) {
                // print the graph
                WaitingDialog progress = new WaitingDialog("Printing Graph...",
                        Ubench.getInstance().getWindow());
                progress.beginTask();
                ExportUtilities.printComponent(Ubench.getInstance().getVisibleTab().getGraph());
                progress.endTask();
            } else {
                // loading any graph file
                if (command.equals("loadGXL")) {
                    final JCodecFileChooser fc = new JCodecFileChooser(
                            Ubench.getInstance().getLastPath().getAbsolutePath(),
                            JCodecFileChooser.Type.OPEN);

                    fc.addCodecFileFilters(ffInputCodecs);

                    fc.setCurrentDirectory(Ubench.getInstance().getLastPath());

                    int fcVal = fc.showOpenDialog(Ubench.getInstance().getWindow());

                    // proceeding the selected file
                    if (fcVal == JFileChooser.APPROVE_OPTION) {
                        final File file = fc.getSelectedFile();


                        // updating the last chosen path
                        Ubench.getInstance().setLastPath(file.getParentFile());

                        // a new thread for loading and layouting the
                        // graph
                        new Thread() {

                            public void run() {

                                // loading the graph and converting it to a
                                // JDomGraph
                                DomGraph theDomGraph = new DomGraph();
                                NodeLabels labels = new NodeLabels();
                                if (Ubench.getInstance().genericLoadGraph(file.getAbsolutePath(),
                                        theDomGraph, labels, fc.getCodecOptions())) {

                                    //	DomGraphTConverter conv = new DomGraphTConverter(graph);

                                    // setting up a new graph tab.
                                    // the graph is painted and shown at once.,
                                    Ubench.getInstance().addJDomGraphTab(file.getName(), theDomGraph, labels);
                                }
                            }
                        }.start();

                    }

                } else // exporting the visible graph to a file.
                if (command.equals("saveUtool")) {
                    JDomGraph graph = Ubench.getInstance().getVisibleTab().getGraph();

                    if (graph != null) {
                        JCodecFileChooser fc = new JCodecFileChooser(
                                Ubench.getInstance().getLastPath().getAbsolutePath(),
                                JCodecFileChooser.Type.EXPORT);
                        fc.addCodecFileFilters(ffOutputCodecs);

                        fc.setCurrentDirectory(Ubench.getInstance().getLastPath());
                        fc.setAcceptAllFileFilterUsed(false);

                        fc.setCurrentDirectory(Ubench.getInstance().getLastPath());

                        int fcVal = GUIUtilities.confirmFileOverwriting(fc, Ubench.getInstance().getWindow());
                        if (fcVal == JFileChooser.APPROVE_OPTION) {

                            File file = fc.getSelectedFile();

                            Ubench.getInstance().setLastPath(file.getParentFile());
                            String targetFile = file.getAbsolutePath();
                            String defaultExtension = ((GenericFileFilter) fc.getFileFilter()).getExtension();

                            if (!targetFile.endsWith(defaultExtension)) {
                                targetFile += defaultExtension;
                                file = new File(targetFile);
                            }




                            OutputCodec oc =
                                    Ubench.getInstance().getCodecManager().getOutputCodecForFilename(file.getName(), fc.getCodecOptions());
                            if (oc != null) {
                                try {
                                    FileWriter writer = new FileWriter(file);
                                    oc.print_header(writer);
                                    oc.encode(Ubench.getInstance().getVisibleTab().getDomGraph(),
                                            Ubench.getInstance().getVisibleTab().getNodeLabels(),
                                            writer);
                                    oc.print_footer(writer);
                                } catch (IOException ex) {
                                    JOptionPane.showMessageDialog(Ubench.getInstance().getWindow(),
                                            "The specified file cannot be created.",
                                            "Error during output",
                                            JOptionPane.ERROR_MESSAGE);
                                } catch (MalformedDomgraphException md) {
                                    JOptionPane.showMessageDialog(Ubench.getInstance().getWindow(),
                                            "The output codec doesn't support output of this graph:\n" + md,
                                            "Error during output",
                                            JOptionPane.ERROR_MESSAGE);
                                } catch (UnsupportedOperationException uE) {
                                    JOptionPane.showMessageDialog(Ubench.getInstance().getWindow(),
                                            uE.getMessage(),
                                            "Error during output",
                                            JOptionPane.ERROR_MESSAGE);
                                }
                            } else {
                                JOptionPane.showMessageDialog(Ubench.getInstance().getWindow(),
                                        "The filename extension of this file is not associated with any known output codec.",
                                        "Error during output",
                                        JOptionPane.ERROR_MESSAGE);
                            }

                        }

                    }

                } else if (command.equals("shut")) {

                    //closing the visible graph.
                    Ubench.getInstance().closeCurrentTab();
                } else if (command.equals("quit")) {

                    // closing the window
                    Ubench.getInstance().quit();

                } else if (command.startsWith("export-clipboard-")) {
                    String codecname = command.substring(17);
                    OutputCodec codec = Ubench.getInstance().getCodecManager().getOutputCodecForName(codecname, "");
                    StringWriter buf = new StringWriter();

                    try {
                        codec.print_header(buf);
                        codec.encode(Ubench.getInstance().getVisibleTab().getDomGraph(),
                                Ubench.getInstance().getVisibleTab().getNodeLabels(),
                                buf);
                        codec.print_footer(buf);

                        new MyClipboardOwner().setClipboardContents(buf.toString());
                    } catch (IOException e1) {
                        // highly unlikely unless the StringWriter ran out of memory or something
                        JOptionPane.showMessageDialog(Ubench.getInstance().getWindow(),
                                "An error occurred while writing into an internal buffer.",
                                "Error during output",
                                JOptionPane.ERROR_MESSAGE);
                    } catch (MalformedDomgraphException e1) {
                        JOptionPane.showMessageDialog(Ubench.getInstance().getWindow(),
                                "The output codec doesn't support output of this graph:\n" + e1,
                                "Error during output",
                                JOptionPane.ERROR_MESSAGE);
                    }

                } else if (command.startsWith("import-clipboard-")) {
                    final String codecname = command.substring(17);
                    String clip = new MyClipboardOwner().getClipboardContents();
                    final StringReader reader = new StringReader(clip);


                    new Thread() {

                        public void run() {
                            DomGraph graph = new DomGraph();
                            NodeLabels labels = new NodeLabels();

                            if (Ubench.getInstance().genericLoadGraph(reader, codecname, graph, labels, null)) {
                                Ubench.getInstance().addJDomGraphTab("(from clipboard)", graph, labels);
                            } else {
                                Ubench.getInstance().refresh();
                            }
                        }
                    }.start();
                } else if (command.equals("dup")) {

                    // duplicating the visible graph
                    if (Ubench.getInstance().getVisibleTab() != null) {
                        Ubench.getInstance().duplicateVisibleTab();


                    }
                } else if (command.equals("fit")) {

                    // fitting the visible graph to the window.
                    if (Ubench.getInstance().getVisibleTab() != null) {
                        Ubench.getInstance().getVisibleTab().fitGraph();
                    }
                } else if (command.equals("closeAll")) {

                    // close all tabs (but not the window)
                    Ubench.getInstance().closeAllTabs();
                } else if (command.equals("resL")) {

                    // resetting the layout
                    Ubench.getInstance().getVisibleTab().resetLayout();
                    Ubench.getInstance().resetSlider();
                } else if (command.equals("cSF")) {

                    // solve the visible graph
                    if ((Ubench.getInstance().getVisibleTab() != null)) {
                        Ubench.getInstance().showProgressBar();
                        ((JDomGraphTab) Ubench.getInstance().getVisibleTab()).solve();
                        Ubench.getInstance().refresh();
                    }
                } else if (command.equals("solvedFormDirectSelection")) {
                    // changed text field and pressed "return"

                    long no = 1;
                    no = Long.parseLong(((JSolvedFormTab) Ubench.getInstance().getVisibleTab()).getSolvedForm().getText());

                    if (!((no < 1) || (no > Ubench.getInstance().getVisibleTab().getSolvedForms()))) {
                        showSolvedFormWithIndex(no);
                    } else {
                        ((JSolvedFormTab) Ubench.getInstance().getVisibleTab()).resetSolvedFormText();
                    }
                } else if (command.equals("plus")) {
                    // ">" button in the status bar


                    long no = ((JSolvedFormTab) Ubench.getInstance().getVisibleTab()).getCurrentForm();
                    if (no < Ubench.getInstance().getVisibleTab().getSolvedForms()) {
                        no++;
                        showSolvedFormWithIndex(no);
                    }

                    Ubench.getInstance().refresh();
                } else if (command.equals("minus")) {
                    // "<" button in the status bar

                    long no = ((JSolvedFormTab) Ubench.getInstance().getVisibleTab()).getCurrentForm();
                    if (no > 1) {
                        no--;
                        showSolvedFormWithIndex(no);
                    }

                    Ubench.getInstance().refresh();

                } else if (command.equals("solve")) {
                    // "solve" button in the status bar
                    new Thread() {

                        public void run() {
                            Ubench.getInstance().showFirstSolvedForm();
                        }
                    }.start();

                } else if (command.equals("about")) {
                    Ubench.getInstance().displayAboutDialog();
                } else if (command.equals("loadExample")) {
                    // open one of the standard examples
                    // in the utool example directory
                    try {
                        ExampleViewer exview = new ExampleViewer();
                        exview.setVisible(true);
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(Ubench.getInstance().getWindow(),
                                ex.getMessage(),
                                "Error during example loading",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } else if (command.equals("saveAll")) {

                    // export the solved forms of a graph
                    JDomGraph graph = Ubench.getInstance().getVisibleTab().getGraph();

                    final JDomGraphTab tab;

                    if (Ubench.getInstance().getVisibleTab() instanceof JDomGraphTab) {
                        tab = (JDomGraphTab) Ubench.getInstance().getVisibleTab();
                    } else {
                        JOptionPane.showMessageDialog(Ubench.getInstance().getWindow(),
                                "I can't export the solved forms of a solved form.",
                                "Cannot export single solved form",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    if (graph != null) {
                        JCodecFileChooser fc = new JCodecFileChooser(
                                Ubench.getInstance().getLastPath().getAbsolutePath(),
                                JCodecFileChooser.Type.EXPORT_SOLVED_FORMS);

                        // only MultiOutputCodecs are suitable here
                        fc.addCodecFileFilters(ffMultiOutputCodecs);

                        fc.setSelectedFile(new File(Ubench.getInstance().
                                getVisibleTab().getDefaultName()
                                + "_solvedForms"
                                + ((GenericFileFilter) fc.getFileFilter()).getExtension()));

                        fc.setCurrentDirectory(Ubench.getInstance().getLastPath());



                        int fcVal = GUIUtilities.confirmFileOverwriting(fc, Ubench.getInstance().getWindow());


                        if (fcVal == JFileChooser.APPROVE_OPTION) {

                            // retrieving the selected file
                            File file = fc.getSelectedFile();
                            Ubench.getInstance().setLastPath(file.getParentFile());
                            String targetFile = file.getAbsolutePath();

                            String defaultExtension = ((GenericFileFilter) fc.getFileFilter()).getExtension();

                            final String targetfileName;
                            if (!targetFile.endsWith(defaultExtension)) {
                                targetfileName = targetFile + defaultExtension;

                            } else {
                                targetfileName = targetFile;
                            }

                            final File outputfile = new File(targetfileName);

                            // retrieving the output codec
                            final MultiOutputCodec oc =
                                    (MultiOutputCodec) Ubench.getInstance().getCodecManager().
                                    getOutputCodecForFilename(outputfile.getName(), fc.getCodecOptions());
                            new Thread() {

                                public void run() {



                                    // display a progress bar
                                    WaitingDialog progress = new WaitingDialog("Exporting solved forms",
                                            Ubench.getInstance().getWindow());
                                    progress.beginTask();



                                    // the recent graph
                                    DomGraph graph = Ubench.getInstance().
                                            getVisibleTab().getDomGraph();

                                    // retrieving the solved forms by
                                    // filling a new chart
                                    long start_solver = System.currentTimeMillis();

                                    if (!tab.isSolvedYet()) {
                                        tab.solve();
                                    }

                                    Chart chart = tab.getChart();

                                    if (chart == null) {
                                        JOptionPane.showMessageDialog(Ubench.getInstance().getWindow(),
                                                "An error occurred while computing the chart.",
                                                "Solver error", JOptionPane.ERROR_MESSAGE);
                                        return;
                                    }



                                    if (oc != null) {
                                        try {
                                            // enumerating the solved forms
                                            // to the file

                                            FileWriter writer = new FileWriter(outputfile);
                                            long count = 0;


                                            SolvedFormIterator it = new SolvedFormIterator(chart, graph);

                                            oc.print_header(writer);
                                            oc.print_start_list(writer);

                                            // enumerating the forms
                                            while (it.hasNext()) {
                                                SolvedFormSpec domedges = it.next();
                                                count++;


                                                if (count > 1) {
                                                    oc.print_list_separator(writer);
                                                }

                                                // let the outputcodec write the solved form
                                                // to a file
                                                oc.encode(graph.makeSolvedForm(domedges),
                                                        Ubench.getInstance().getVisibleTab().getNodeLabels().makeSolvedForm(domedges),
                                                        writer);

                                            }
                                            long end_extraction = System.currentTimeMillis();
                                            oc.print_end_list(writer);
                                            oc.print_footer(writer);

                                            // hiding progress bar
                                            progress.endTask();
                                            progress.setVisible(false);




                                            // new text
                                            long total_time = end_extraction - start_solver;
                                            String interTime = null;
                                            if (total_time > 0) {
                                                interTime = (int) Math.floor(count * 1000.0 / total_time) + " sfs/sec; ";
                                            }

                                            // statistics
                                            JOptionPane.showMessageDialog(Ubench.getInstance().getWindow(),
                                                    "Found " + count + " solved forms."
                                                    + System.getProperty("line.separator")
                                                    + "Total runtime: " + total_time + " ms (" + interTime
                                                    + 1000 * total_time / count + " microsecs/sf)",
                                                    "Solver Statistics",
                                                    JOptionPane.INFORMATION_MESSAGE);


                                        } catch (IOException ex) {
                                            JOptionPane.showMessageDialog(Ubench.getInstance().getWindow(),
                                                    "The specified file cannot be created.",
                                                    "Error during output",
                                                    JOptionPane.ERROR_MESSAGE);
                                        } catch (MalformedDomgraphException md) {
                                            JOptionPane.showMessageDialog(Ubench.getInstance().getWindow(),
                                                    "The output codec doesn't support output of this graph:\n" + md,
                                                    "Error during output",
                                                    JOptionPane.ERROR_MESSAGE);
                                        } catch (UnsupportedOperationException uE) {
                                            JOptionPane.showMessageDialog(Ubench.getInstance().getWindow(),
                                                    uE.getMessage(),
                                                    "Error during output",
                                                    JOptionPane.ERROR_MESSAGE);
                                        }
                                    }

                                }
                            }.run();


                        }



                    }
                } else if (command.equals(("chartView"))) {

                    // open a chart window with the visible
                    // graph's chart.
                    new Thread() {

                        public void run() {
                            Ubench.getInstance().getVisibleTab().displayChart();
                        }
                    }.run();

                } else if (command.equals("showcodecs")) {
                    // show a list of all codecs installed
                    Set<String> seen = new HashSet<String>();
                    CodecManager manager =
                            Ubench.getInstance().getCodecManager();

                    StringBuffer codecList = new StringBuffer();

                    // initialising a big HTML table
                    codecList.append("<html><table border=\"0\">"
                            + "<tr><th colspan=\"4\" align=\"left\">Input Codecs:</th></td>");

                    // insert the input codecs first
                    for (GenericFileFilter filter : ffInputCodecs) {
                        String codecname =
                                filter.getName();

                        if (!seen.contains(codecname)) {
                            seen.add(codecname);

                            // if a codec it's experimental,
                            // this is displayed
                            String exp =
                                    manager.isExperimentalInputCodec(codecname) ? "(EXPERIMENTAL!)" : "";

                            codecList.append("<tr><td>" + filter.getName() + "</td><td> ("
                                    + filter.getExtension()
                                    + ")</td><td></td><td>" + exp
                                    + "</td></tr>");
                        }
                    }
                    seen.clear();
                    codecList.append("<tr><td colspan=\"4\"></td></tr>");
                    codecList.append("<tr><th colspan=\"4\" align=\"left\">Output Codecs:</th></tr>");

                    // the output codecs
                    for (GenericFileFilter filter : ffOutputCodecs) {
                        String codecname =
                                filter.getName();

                        if (!seen.contains(codecname)) {
                            seen.add(codecname);

                            // experimental codec?
                            String exp =
                                    manager.isExperimentalOutputCodec(codecname) ? "  (EXPERIMENTAL!)" : "";

                            // multi-output-codec?
                            String multi =
                                    manager.isMultiOutputCodec(codecname)
                                    ? "[M]" : "";

                            codecList.append("<tr><td>" + filter.getName() + "</td><td> ("
                                    + filter.getExtension()
                                    + ")</td><td>" + multi
                                    + "</td><td>" + exp
                                    + "</td></tr>");
                        }
                    }

                    codecList.append("</table><br><br>[M]: Allows output of "
                            + "multiple graphs (applicable for solved form export)</html>");


                    JOptionPane.showMessageDialog(Ubench.getInstance().getWindow(),
                            codecList, "Codecs in Utool", JOptionPane.INFORMATION_MESSAGE);


                } else if (command.equals("pic")) {
                    // picture export

                    JFileChooser fc = new JFileChooser();
                    fc.setDialogTitle("Export USR as Image");


                    GenericFileFilter bmpFilter =
                            new GenericFileFilter("bmp", "*.bmp pictures");

                    fc.addChoosableFileFilter(bmpFilter);
                    fc.addChoosableFileFilter(new GenericFileFilter("jpeg", "*.jpeg pictures"));
                    fc.addChoosableFileFilter(new GenericFileFilter("png", "*.png pictures"));

                    fc.setFileFilter(bmpFilter);


                    fc.setCurrentDirectory(Ubench.getInstance().getLastPath());


                    int fcVal = GUIUtilities.confirmFileOverwriting(fc, Ubench.getInstance().getWindow());


//					proceed with a chosen file
                    if (fcVal == JFileChooser.APPROVE_OPTION) {

                        // resolving the selected file's path
                        File file = fc.getSelectedFile();
                        final String dir = file.getAbsolutePath();

                        // updating the last chosen path
                        Ubench.getInstance().setLastPath(file.getParentFile());

                        final String picDesc = ((GenericFileFilter) fc.getFileFilter()).getExtension();

                        // a new thread for printing the pdf.
                        // a progress bar will be visible while this
                        // thread runs.
                        new Thread() {

                            public void run() {

                                WaitingDialog progress = new WaitingDialog(
                                        "Exporting Image...", (JFrame) Ubench.getInstance().getWindow());
                                progress.beginTask();


                                try {
                                    JDomGraph toDraw = Ubench.getInstance().getVisibleTab().getGraph();
                                    ExportUtilities.exportPicture(toDraw, dir, picDesc);



                                } catch (IOException exc) {
                                    JOptionPane.showMessageDialog(Ubench.getInstance().getWindow(),
                                            "The output file can't be opened.",
                                            "Error while creating image",
                                            JOptionPane.ERROR_MESSAGE);
                                }
                                progress.endTask();

                            }
                        }.start();

                    }

                } else {
                    // PDF-Printing
                    if (command.equals("pdf")) {

                        // file chooser with PDF-filter
                        JFileChooser fc = new JFileChooser();
                        fc.setFileFilter(new PDFFilter());
                        fc.setDialogTitle("Export USR as PDF");

                        // if there was any path chosen before, the
                        // file chooser will start in the related directory

                        fc.setCurrentDirectory(Ubench.getInstance().getLastPath());


                        // configuring button and window texts
                        int fcVal = GUIUtilities.confirmFileOverwriting(fc, Ubench.getInstance().getWindow());
                        fc.setApproveButtonText("Print!");

                        // proceed with a chosen file
                        if (fcVal == JFileChooser.APPROVE_OPTION) {

                            // resolving the selected file's path
                            File file = fc.getSelectedFile();
                            String dir = file.getAbsolutePath();

                            // updating the last chosen path
                            Ubench.getInstance().setLastPath(file.getParentFile());

                            final String filepath;
                            // if the file was named withoud pdf-extension,
                            // the extension is added
                            if (dir.indexOf(".pdf") > 0) {
                                filepath = dir;
                            } else {
                                filepath = dir + ".pdf";
                            }


                            // a new thread for printing the pdf.
                            // a progress bar will be visible while this
                            // thread runs.
                            new Thread() {

                                public void run() {
                                    WaitingDialog progress = new WaitingDialog(
                                            "Exporting PDF...", (JFrame) Ubench.getInstance().getWindow());
                                    progress.beginTask();


                                    try {
                                        // the actual PDF-printing
                                        PDFCanvas canv = new PDFCanvas(filepath);
                                        LayoutAlgorithm al = Ubench.getInstance().getVisibleTab().getLayoutType().getLayout();
                                        al.layout(Ubench.getInstance().getVisibleTab().getDomGraph(),
                                                Ubench.getInstance().getVisibleTab().getNodeLabels(), canv,
                                                new LayoutOptions(Ubench.getInstance().getVisibleTab().getLabelType(),
                                                Preferences.isRemoveRedundantEdges()));
                                        canv.finish();

                                        //ExportUtilities.exportPDF(Ubench.getInstance().getVisibleTab().getGraph(), filepath);
                                    } catch (IOException io) {
                                        JOptionPane.showMessageDialog(Ubench.getInstance().getWindow(),
                                                "The output file can't be opened.",
                                                "PDF error",
                                                JOptionPane.ERROR_MESSAGE);
                                    } catch (DocumentException de) {
                                        JOptionPane.showMessageDialog(Ubench.getInstance().getWindow(),
                                                "The output file can't be opened.",
                                                "PDF error",
                                                JOptionPane.ERROR_MESSAGE);
                                    } catch (LayoutException e) {
                                        JOptionPane.showMessageDialog(Ubench.getInstance().getWindow(),
                                                "An error occurred during layout:\n" + e.getMessage(),
                                                "Layout error",
                                                JOptionPane.ERROR_MESSAGE);
                                    }
                                    progress.endTask();

                                }
                            }.start();
                        }

                    }
                }
            }
        } catch (Exception exc) {
            DomGraphUnhandledExceptionHandler.showErrorDialog(exc);

        }
    }

    /**
     * Overwrites the <code>itemStateChanged</code> method of
     * <code>ItemListener</code>.
     * This handles the events occuring when one of the
     * preference checkboxes is (de-)selected.
     */
    public void itemStateChanged(ItemEvent e) {
        String desc = lookupEventSource(e.getSource());
        try {
            // unknown event
            if (desc == null) {
                System.err.println("Unknown item state change event!");
            } else if (desc.equals("layoutchange")) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    String selectedItem = ((JMenuItem) e.getSource()).getText();

                    if (selectedItem.equals("JDomGraph layout")) {


                        if (Ubench.getInstance().getVisibleTab() != null) {
                            Ubench.getInstance().getVisibleTab().setLayoutType(LayoutType.JDOMGRAPH);
                        }

                    } else if (selectedItem.equals("Chart layout")) {
                        if (Ubench.getInstance().getVisibleTab() != null) {
                            Ubench.getInstance().getVisibleTab().setLayoutType(LayoutType.CHARTLAYOUT);

                        }
                    }

                }

            } else if (desc.equals("showLabels")) {

                // align preferences to selection state.
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    if (Ubench.getInstance().getVisibleTab() != null) {
                        Ubench.getInstance().getVisibleTab().setLabelType(LabelType.LABEL);
                    }
                }
            } else if (desc.equals("showNames")) {
//			align preferences to selection state.
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    if (Ubench.getInstance().getVisibleTab() != null) {
                        Ubench.getInstance().getVisibleTab().setLabelType(LabelType.NAME);
                    }
                }
            } else if (desc.equals("showBoth")) {
//			align preferences to selection state.
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    if (Ubench.getInstance().getVisibleTab() != null) {
                        Ubench.getInstance().getVisibleTab().setLabelType(LabelType.BOTH);
                    }
                }
            } else {

                // checkbox indicating whether graphs are
                // solved right after loading automatically
                if (desc.equals("countAndSolve")) {

                    // enable/disable menu items and change preferences
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        if (Ubench.getInstance().getMenuBar() != null) {
                            Ubench.getInstance().getMenuBar().setCountSfEnabled(false);
                        }
                        Preferences.setAutoCount(true);
                        if (Ubench.getInstance().getVisibleTab() != null) {
                            ((JDomGraphTab) Ubench.getInstance().getVisibleTab()).solve();
                            Ubench.getInstance().refresh();
                        }

                    } else {
                        if ((Ubench.getInstance().getMenuBar() != null)
                                && Ubench.getInstance().getVisibleTab() != null
                                && Ubench.getInstance().getVisibleTab().getClass() != JSolvedFormTab.class) {
                            Ubench.getInstance().getMenuBar().setCountSfEnabled(true);
                        }
                        Preferences.getInstance().setLayoutType(LayoutType.JDOMGRAPH);
                        Preferences.setAutoCount(false);
                    }
                } else {

                    // layout preferences concerning graph scaling
                    if (desc.equals("fitAll")) {

                        // change preferences and refresh the visible
                        // graph
                        if (e.getStateChange() == ItemEvent.SELECTED) {
                            Preferences.setFitToWindow(true);
                            if (Ubench.getInstance().getVisibleTab() != null) {
                                Ubench.getInstance().getVisibleTab().fitGraph();
                            }
                        } else {
                            Preferences.setFitToWindow(false);
                        }

                    } else if (desc.equals("autoreduce")) {
                        if (e.getStateChange() == ItemEvent.SELECTED) {
                            Ubench.getInstance().reduceAutomatically = true;
                            if (!Ubench.getInstance().isRelativeNormalFormsComputerLoaded() ) {
                                JOptionPane.showMessageDialog(Ubench.getInstance().getWindow(),
                                        "You must load a global equation system before Utool can"
                                        + System.getProperty("line.separator")
                                        + "automatically eliminate equivalences. You can select one"
                                        + System.getProperty("line.separator")
                                        + "in the following dialog.",
                                        "Please load an equation system",
                                        JOptionPane.INFORMATION_MESSAGE);
                                loadRnfc();
                            }

                        } else {
                            Ubench.getInstance().reduceAutomatically = false;
                        }
                    }
                }
            }

        } catch (Exception ex) {
            DomGraphUnhandledExceptionHandler.showErrorDialog(ex);
        }
    }

    private void loadRnfc() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Choose the equation system input file");
        fc.setFileFilter(new XMLFilter());

        fc.setCurrentDirectory(Ubench.getInstance().getLastPath());

        int fcVal = fc.showOpenDialog(Ubench.getInstance().getWindow());

        if (fcVal == JFileChooser.APPROVE_OPTION) {

            File file = fc.getSelectedFile();

            try {
                // obtain rewrite systems
                RewriteSystem weakening = new RewriteSystem(true);
                RewriteSystem equivalence = new RewriteSystem(false);
                Annotator annotator = new Annotator();
                RewritingSystemParser parser = new RewritingSystemParser();
                parser.read(new FileReader(file), weakening, equivalence, annotator);

                // build rnf computer and save it in the abstract options
                RelativeNormalFormsComputer rnfc = new RelativeNormalFormsComputer(annotator);
                rnfc.addRewriteSystem(weakening);
                rnfc.addRewriteSystem(equivalence, new EquivalenceRulesComparator());


                Ubench.getInstance().setRelativeNormalFormsComputer(rnfc, file.getName());
                for (JGraphTab tab : Ubench.getInstance().getTabs()) {
                    tab.enableGlobalEQS(true);
                }


            } catch (Exception ex) {
                JOptionPane.showMessageDialog(Ubench.getInstance().getWindow(),
                        "The rule system cannot be parsed."
                        + System.getProperty("line.separator")
                        + "Either the input file is not readable, or it contains syntax errors.",
                        "Error while loading equation system",
                        JOptionPane.ERROR_MESSAGE);
            }


        }
    }

    /**
     * TODO comment me!
     *
     * @param source
     * @param desc
     */
    void registerEventSource(Object source, String desc) {
        eventSources.put(source, desc);
    }

    /**
     * TODO comment me!
     *
     * @param source
     * @return
     */
    private String lookupEventSource(Object source) {
        return eventSources.get(source);
    }

    /**
     * A <code>FileFilter</code> that
     * accepts files with *.pdf-extension.
     *
     * @author Michaela Regneri
     *
     */
    private class PDFFilter extends FileFilter {

        /**
         * Overwrites the <code>accept</code> method
         * of <code>Filefilter</code>.
         *
         * @return true if the file has a pdf extension
         */
        public boolean accept(File f) {
            String fileName = f.getName();
            if (f.isDirectory()) {
                return true;
            }
            if (fileName.indexOf(".pdf") > 0) {
                return true;
            }
            return false;
        }

        /**
         * Overwrites the <code>getDescription</code>
         * method of <code>FileFilter</code>.
         *
         * @return just "PDF"
         */
        public String getDescription() {
            return "PDF";
        }
    }

    /**
     * Picks the solved form with the given number and
     * displays it. Should not be used to show the first form!
     * (It would close the related graph then.)
     *
     * @param no the number of the form to show.
     */
    void showSolvedFormWithIndex(long no) {

        // extracting the wanted solved form
        SolvedFormIterator solver = Ubench.getInstance().getVisibleTab().getSolvedFormIterator();
        SolvedFormSpec domEdges = solver.getSolvedForm((int) no - 1);
        NodeLabels labels = Ubench.getInstance().getVisibleTab().getNodeLabels().makeSolvedForm(domEdges);
        DomGraph nextForm = Ubench.getInstance().getVisibleTab().getSourceGraph().makeSolvedForm(domEdges);


        // converting the form to a JDomGraph

        JGraphTab tab = Ubench.getInstance().getVisibleTab();
        Ubench.getInstance().addSolvedFormTab(tab.getGraphName() + "  SF #" + no, nextForm,
                solver, no, tab.getSolvedForms(),
                tab.getSourceGraph(), labels, tab.getGraphName(), false);




    }

    /* (non-Javadoc)
     * @see de.saar.chorus.domgraph.utool.server.ConnectionManager.StateChangeListener#stateChanged(de.saar.chorus.domgraph.utool.server.ConnectionManager.State)
     */
    public void stateChanged(State newState) {

        JDomGraphMenu menu = Ubench.getInstance().getMenuBar();

        if (newState == ConnectionManager.State.RUNNING) {
            menu.getServerButton().setSelected(true);
        } else if (newState == ConnectionManager.State.STOPPED) {
            menu.getServerButton().setSelected(false);
        }
    }

    // Code adapted from http://www.javapractices.com/Topic82.cjp
    private static class MyClipboardOwner implements ClipboardOwner {

        public void lostOwnership(Clipboard arg0, Transferable arg1) {
            // do nothing
        }

        /**
         * Place a String on the clipboard, and make this class the
         * owner of the Clipboard's contents.
         */
        public void setClipboardContents(String aString) {
            StringSelection stringSelection = new StringSelection(aString);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, this);
        }

        /**
         * Get the String residing on the clipboard.
         *
         * @return any text found on the Clipboard; if none found, return an
         * empty String.
         */
        public String getClipboardContents() {
            String result = "";
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            //odd: the Object param of getContents is not currently used
            Transferable contents = clipboard.getContents(null);
            boolean hasTransferableText =
                    (contents != null)
                    && contents.isDataFlavorSupported(DataFlavor.stringFlavor);
            if (hasTransferableText) {
                try {
                    result = (String) contents.getTransferData(DataFlavor.stringFlavor);
                } catch (UnsupportedFlavorException ex) {
                    //highly unlikely since we are using a standard DataFlavor
                    System.out.println(ex);
                    ex.printStackTrace();
                } catch (IOException ex) {
                    System.out.println(ex);
                    ex.printStackTrace();
                }
            }
            return result;
        }
    }
}
