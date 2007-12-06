/*
 * @(#)ServerThread.java created 12.09.2006
 *
 * Copyright (c) 2006 Alexander Koller
 *
 */

package de.saar.chorus.domgraph.utool.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;

import de.saar.basic.Logger;
import de.saar.basic.LoggingWriter;
import de.saar.basic.XmlEncodingWriter;
import de.saar.chorus.domgraph.GlobalDomgraphProperties;
import de.saar.chorus.domgraph.chart.Chart;
import de.saar.chorus.domgraph.chart.ChartSolver;
import de.saar.chorus.domgraph.chart.OneSplitSource;
import de.saar.chorus.domgraph.chart.RegularTreeGrammar;
import de.saar.chorus.domgraph.chart.SolvedFormIterator;
import de.saar.chorus.domgraph.chart.SolvedFormSpec;
import de.saar.chorus.domgraph.chart.SolverNotApplicableException;
import de.saar.chorus.domgraph.codec.CodecManager;
import de.saar.chorus.domgraph.codec.MalformedDomgraphException;
import de.saar.chorus.domgraph.equivalence.rtg.QuantifierMarkedNonterminal;
import de.saar.chorus.domgraph.equivalence.rtg.RtgRedundancyElimination;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.utool.AbstractOptions;
import de.saar.chorus.domgraph.utool.AbstractOptionsParsingException;
import de.saar.chorus.domgraph.utool.ExitCodes;
import de.saar.chorus.domgraph.utool.AbstractOptions.Operation;
import de.saar.chorus.ubench.Ubench;

class ServerThread extends Thread {
    private final PrintWriter out;
    private final BufferedReader in;
    private final Logger logger;
    private final Socket socket;
    private XmlParser parser ;



    ServerThread(Socket socket, Logger logger) throws IOException {
        this.logger = logger;
        this.socket = socket;

        out = new PrintWriter(new LoggingWriter(new OutputStreamWriter(socket.getOutputStream()), logger, "Sent: "), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    @Override
    public void run() {
        parser = new XmlParser(logger);
        AbstractOptions options = null;

        try {
            try {
                options = parser.parse(in);
            } catch (AbstractOptionsParsingException e) {
                sendError(out, e.getExitcode(), e.comprehensiveErrorMessage());

                socket.close();
                return;
            }

            processCommand(options);

            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            logger.log("An I/O exception occurred while processing a command in the server:");
            logger.log(e.toString());
        }

    }

    private void processCommand(AbstractOptions options) throws IOException {
        boolean weaklyNormal = false;
        boolean normal = false;
        boolean compact = false;
        boolean triviallyUnsolvable = false;



        // The following code was copied verbatim from the old UtoolServer,
        // and is probably widely analogous to the code in Utool.


        long afterParsing = System.currentTimeMillis();

        // check statistics
        if( options.getOperation().requiresInput ) {
            weaklyNormal = options.getGraph().isWeaklyNormal();
            normal = options.getGraph().isNormal();
            compact = options.getGraph().isCompact();
        }


        // for solving operations (but not convert or display), preprocess the graph
        switch(options.getOperation()) {
        case solvable:
        case solve:
        	triviallyUnsolvable = ! options.preprocessGraph();
        }

        // if it turned out that the graph is trivially unsolvable, report that
        if( triviallyUnsolvable ) {
        	 switch(options.getOperation()) {
             case solvable:
            	 if( options.hasOptionNochart() ) {
            		 out.println("<result solvable='false' "
             				+ "fragments='" + options.getGraph().getAllRoots().size() + "' "
             				+ "time='0' />");
            	 } else {
            		 out.println("<result solvable='false' "
              				+ "fragments='" + options.getGraph().getAllRoots().size() + "' "
              				+ "count='0' "
              				+ "chartsize='0' "
              				+ "time='0' />");
            	 }

            	 return;

             case solve:
            	 out.println("<result solvable='false' count='0' "
         					+ "fragments='" + options.getGraph().getAllRoots().size() + "' "
         					+ "chartsize='0' "
         					+ "time-chart='0' />");
            	 return;
        	 }
        }

        // now do something, depending on the specified operation
        switch(options.getOperation()) {
        case solvable:
            if( options.hasOptionNochart() ) {
            	try {
            		long start_solver = System.currentTimeMillis();
            		boolean solvable = OneSplitSource.isGraphSolvable(options.getGraph());
            		long end_solver = System.currentTimeMillis();
            		long time_solver = end_solver - start_solver;

            		out.println("<result solvable='" + solvable + "' "
            				+ "fragments='" + options.getGraph().getAllRoots().size() + "' "
            				+ "time='" + time_solver + "' />");
            	} catch(SolverNotApplicableException e) {
            		sendError(out, ExitCodes.SOLVER_NOT_APPLICABLE,
            				"The solver is not applicable to this graph: " + e.getMessage());
            	}

                break;
            }

            // intentional fall-through for the non-"nochart" case


        case solve:
            DomGraph graph = options.getGraph();

            // compute chart
            long start_solver = System.nanoTime();
            Chart chart = new Chart(options.getLabels());
            RegularTreeGrammar reducedChart;
            boolean solvable;

            try {

            	if( options.hasOptionEliminateEquivalence() ) {
            	    /*
            		solvable = ChartSolver.solve(graph, chart,
            				new RedundancyEliminationSplitSource(
            						new IndividualRedundancyElimination(graph,
            								options.getLabels(), options.getEquations()), graph));
            								*/


            	    solvable = ChartSolver.solve(graph, chart);
            	    reducedChart = new RegularTreeGrammar<QuantifierMarkedNonterminal>();
            	    new RtgRedundancyElimination(graph, options.getLabels(), options.getEquations()).eliminate(chart, reducedChart);



/*
            	    reducedChart = new RegularTreeGrammar<QuantifierMarkedNonterminal>();
            	    solvable = ChartSolver.solve(graph, reducedChart,
                            new RtgRedundancyEliminationSplitSource(
                                    new RtgRedundancyElimination(graph,
                                            options.getLabels(), options.getEquations()), graph));
                                            */


            	} else {
            		solvable = ChartSolver.solve(graph, chart);
            		reducedChart = chart;
            	}


            	long end_solver = System.nanoTime();
            	long time_solver = (end_solver - start_solver)/1000000L;

            	if( options.getOperation() == Operation.solvable ) {
            		// Operation = solvable
            		out.println("<result solvable='" + solvable + "' "
            				+ "fragments='" + options.getGraph().getAllRoots().size() + "' "
            				+ "count='" + reducedChart.countSolvedForms() + "' "
            				+ "chartsize='" + reducedChart.size() + "' "
            				+ "time='" + time_solver + "' />");
            	} else {
            		// Operation = solve
            		if( !solvable ) {
            			out.println("<result solvable='false' count='0' "
            					+ "fragments='" + options.getGraph().getAllRoots().size() + "' "
            					+ "chartsize='" + reducedChart.size() + "' "
            					+ "time-chart='" + time_solver + "' />");
            		} else {
            			StringWriter buf = new StringWriter();
            			XmlEncodingWriter enc = new XmlEncodingWriter(buf);
            			long count = 0;

            			// extract solved forms
            			try {
            				long start_extraction = System.currentTimeMillis();
            				SolvedFormIterator it = new SolvedFormIterator(reducedChart,options.getGraph());
            				while( it.hasNext() ) {
            					SolvedFormSpec domedges = it.next();
            					count++;

            					if( !options.hasOptionNoOutput() ) {
            						buf.append("  <solution string='");
            						options.getOutputCodec().encode(options.getGraph().makeSolvedForm(domedges), options.getLabels().makeSolvedForm(domedges), enc);
            						buf.append("' />\n");
            					}
            				}
            				long end_extraction = System.currentTimeMillis();
            				long time_extraction = end_extraction - start_extraction;

            				out.println("<result solvable='true' count='" + count + "' "
            						+ "fragments='" + options.getGraph().getAllRoots().size() + "' "
            						+ " chartsize='" + reducedChart.size() + "' "
            						+ " time-chart='" + time_solver + "' "
            						+ " time-extraction='" + time_extraction + "' >");
            				out.print(buf.toString());
            				out.println("</result>");
            			} catch (MalformedDomgraphException e) {
            				sendError(out, e.getExitcode() + ExitCodes.MALFORMED_DOMGRAPH_BASE_OUTPUT, "Output of the solved forms of this graph is not supported by this output codec.");
            				return;
            			}
            		}
            	}
            } catch(SolverNotApplicableException e) {
            	sendError(out, ExitCodes.SOLVER_NOT_APPLICABLE,
            			"The solver is not applicable to this graph: " + e.getMessage());
            }

            break;


        case convert:
            if( options.hasOptionNoOutput() ) {
                out.println("<result />");
            } else {
                try {
                    XmlEncodingWriter enc = new XmlEncodingWriter(out);
                    out.print("<result usr='");
                    options.getOutputCodec().encode(options.getGraph(), options.getLabels(), enc);
                    out.println("' />");
                } catch(MalformedDomgraphException e) {
                    sendError(out, ExitCodes.MALFORMED_DOMGRAPH_BASE_OUTPUT + e.getExitcode(), "This graph is not supported by the specified output codec.");
                    return;
                }
            }

            break;


        case classify:
            int programExitCode = 0;

            if( weaklyNormal ) {
                programExitCode |= ExitCodes.CLASSIFY_WEAKLY_NORMAL;
            }

            if( normal ) {
                programExitCode |= ExitCodes.CLASSIFY_NORMAL;
            }

            if( compact ) {
                programExitCode |= ExitCodes.CLASSIFY_COMPACT;
            }


            if( options.getGraph().isHypernormallyConnected() ) {
                programExitCode |= ExitCodes.CLASSIFY_HN_CONNECTED;
            }

            if( options.getGraph().isLeafLabelled() ) {
                programExitCode |= ExitCodes.CLASSIFY_LEAF_LABELLED;
            }

            long afterEverything = System.currentTimeMillis();

            out.println("<result code='" + programExitCode + "' "
//                  meaningless now                    + "time1='" + (afterParsing - start) + "' "
                    + "time2='" + (afterEverything - afterParsing) + "' "
                    + "weaklynormal='" + weaklyNormal + "' "
                    + "normal='" + normal + "' "
                    + "compact='" + compact + "' "
                    + "hypernormallyconnected='" + options.getGraph().isHypernormallyConnected() + "' "
                    + "leaflabelled='" + options.getGraph().isLeafLabelled() + "' "
                    + "/>");
            break;


        case display:

        	if( options.getGraph() != null ) {
                if(Ubench.getInstance().addJDomGraphTab(
                        options.getInputName(),
                        options.getGraph(),
                        options.getLabels()))  {
                	Ubench.getInstance().getWindow().toFront();
                    out.println("<result code='0' />");
                } else {
                    sendError(out, ExitCodes.GRAPH_DRAWING_ERROR, "An error occurred while drawing the graph.");
                }
            } else {
                Ubench.getInstance();
                Ubench.getInstance().getWindow().toFront();
                out.println("<result code='0' />");
            }
        	Ubench.getInstance().getWindow().toFront();
            break;


        case help:
            out.println("<result help='" + helpString(options.getHelpArgument()) + "' />");
            break;

        case _displayCodecs:
            out.println("<result>");

            for( String codecname : parser.getCodecManager().getAllInputCodecs()) {
                displayOneInputCodec(codecname, out);
            }

            for( String codecname : parser.getCodecManager().getAllOutputCodecs()) {
                displayOneOutputCodec(codecname, out);
            }

            out.println("</result>");
            break;

        case _version:
            out.println("<result version='" + versionString() + "' />");
            break;

        case server:
        case _helpOptions:
            // other operations not supported by the server
        }


    }

    private static void sendError(PrintWriter out, int exitcode, String string) {
        out.println("<error code='" + exitcode + "' explanation='" + string + "' />");
    }

    private static String helpString(Operation op) {
        StringBuffer ret = new StringBuffer();

        if( (op == null) || (op.longDescription == null) ) {
            ret.append("\nUtool is the Swiss Army Knife of Underspecification (Java version).\n");
            ret.append("For more information, see " + GlobalDomgraphProperties.getHomepage());
        } else {
            ret.append("utool " + op + ": " + op.shortDescription + ".\n");
            ret.append(op.longDescription + "\n");
        }

        return ret.toString();
    }

    private static String versionString() {
        return "Utool (The Swiss Army Knife of Underspecification), version "
        + GlobalDomgraphProperties.getVersion() + "\n"
        + "(running in server mode)\n"
        + "Created by the CHORUS project, SFB 378, Saarland University\n\n";
    }


    /*
    private static void displayOneCodec(Class codec, PrintWriter out, String type) {
        String name = CodecManager.getCodecName(codec);
        String ext = CodecManager.getCodecExtension(codec);

        out.print("  <codec name='" + name + "' ");

        if( ext != null ) {
            out.print("extension='" + ext + "' ");
        }

        out.println("type='" + type + "' />");
    }
    */

    private void displayOneInputCodec(String codec, PrintWriter out) {
        CodecManager manager = parser.getCodecManager();
        String ext = manager.getInputCodecExtension(codec);

        out.print("  <codec name='" + codec + "' ");

        if( ext != null ) {
            out.print("extension='" + ext + "' ");
        }

        out.println("type='input' />");
    }

    private void displayOneOutputCodec(String codec, PrintWriter out) {
        CodecManager manager = parser.getCodecManager();
        String ext = manager.getOutputCodecExtension(codec);

        out.print("  <codec name='" + codec + "' ");

        if( ext != null ) {
            out.print("extension='" + ext + "' ");
        }

        out.println("type='output' />");
    }

    public void closeSocket() throws IOException {
        socket.close();

    }
}


/*
 * TODO:
 * - what happens if the client closes the socket before we're finished parsing?
 */