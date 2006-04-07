/*
 * @(#)UtoolServer.java created 10.02.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.utool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;

import de.saar.basic.XmlEncodingWriter;
import de.saar.chorus.domgraph.GlobalDomgraphProperties;
import de.saar.chorus.domgraph.chart.Chart;
import de.saar.chorus.domgraph.chart.ChartSolver;
import de.saar.chorus.domgraph.chart.SolvedFormIterator;
import de.saar.chorus.domgraph.codec.CodecManager;
import de.saar.chorus.domgraph.codec.MalformedDomgraphException;
import de.saar.chorus.domgraph.codec.OutputCodec;
import de.saar.chorus.domgraph.equivalence.IndividualRedundancyElimination;
import de.saar.chorus.domgraph.equivalence.RedundancyEliminationSplitSource;
import de.saar.chorus.domgraph.graph.DomEdge;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.utool.AbstractOptions.Operation;
import de.saar.chorus.ubench.gui.Ubench;


/**
 * The Utool main program for accessing the Domgraph functionality
 * in server mode. Utool ("Underspecification Tool") is the
 * Swiss Army Knife of Underspecification (Java version). This
 * version will accept commands in XML format from a socket. It
 * is started by calling the command-line version with command "server".<p>
 * 
 * The operation of this class is described in more detail in the
 * end-user documentation of Utool.
 * 
 * @author Alexander Koller
 *
 */

class UtoolServer {
    private static boolean logging = false;
    private static PrintWriter logTo = null;

    /**
     * @param args
     * @throws IOException 
     */
    public static void startServer(AbstractOptions cmdlineOptions) throws IOException { //throws Exception {
        ServerSocket ssock = null;
        int port;
        XmlParser parser = new XmlParser();
        AbstractOptions options;
        
        boolean weaklyNormal = false;
        boolean normal = false;
        boolean compact = false;
        boolean compactifiable = false;

        
        // parse cmd-line options
        logging = cmdlineOptions.hasOptionLogging();
        if( logging ) {
            logTo = cmdlineOptions.getLogWriter();
        }
        
        port = cmdlineOptions.getPort();
        
        
        // open server socket
        ssock = new ServerSocket(port);
        log("Listening on port " + port + "...");
        
        
        while( true ) {
            // accept one connection
            log("Waiting for connection ... ");
            Socket sock = ssock.accept();
            log("accepted connection from " + sock);
            
            PrintWriter out = new PrintWriter(sock.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            String line = null;
            StringBuffer cmd = new StringBuffer();
            
            // read one command
            try {
                while( (line = in.readLine()) != null ) {
                    log("read: " + line);
                    cmd.append(line);
                }
            } catch(SocketException e) {
                log("Client closed the socket prematurely, will ignore this request.");
                in.close();
                out.close();
                sock.close();
                continue;
            }
            
            // parse the command
            // ANALOGOUS to Utool
            
            long start = System.currentTimeMillis();
            
            try {
                options = parser.parse(cmd.toString());
            } catch(AbstractOptionsParsingException e) {
                // TODO Should some errors be fatal, i.e. lead to termination
                // of the server?
                sendError(out, e.getExitcode(), e.comprehensiveErrorMessage());
                sock.close();
                continue;
            }
            
            long afterParsing = System.currentTimeMillis();
            
            // check statistics and compactify graph
            if( options.getOperation().requiresInput ) {
                weaklyNormal = options.getGraph().isWeaklyNormal();
                normal = options.getGraph().isNormal();
                compact = options.getGraph().isCompact();
                compactifiable = options.getGraph().isCompactifiable();
            }            
            
            
            // now do something, depending on the specified operation
            switch(options.getOperation()) {
            case solve:
            case solvable:
                DomGraph compactGraph = null;
                
                if( !weaklyNormal ) {
                    sendError(out, ExitCodes.ILLFORMED_INPUT_GRAPH, "Cannot solve graphs that are not weakly normal!");
                    sock.close();
                    continue;
                }
                
                if( !compact && !compactifiable ) {
                    sendError(out, ExitCodes.ILLFORMED_INPUT_GRAPH, "Cannot solve graphs that are not compact and not compactifiable!");
                    sock.close();
                    continue;
                }
                
                // compactify if necessary
                compactGraph = options.getGraph().compactify();

                // compute chart
                long start_solver = System.currentTimeMillis();
                Chart chart = new Chart();
                
                ChartSolver solver;
                
                if( options.hasOptionEliminateEquivalence() ) {
                    solver = new ChartSolver(compactGraph, chart, 
                            new RedundancyEliminationSplitSource(
                                    new IndividualRedundancyElimination(compactGraph, 
                                            options.getLabels(), options.getEquations()), compactGraph));
                } else {
                    solver = new ChartSolver(compactGraph, chart); 
                }
                
                
                boolean solvable = solver.solve();
                long end_solver = System.currentTimeMillis();
                long time_solver = end_solver - start_solver;
                
                if( options.getOperation() == Operation.solvable ) {
                    // Operation = solvable
                    out.println("<result solvable='" + solvable + "' "
			    + "fragments='" + options.getGraph().getAllRoots().size() + "' "
                            + "count='" + chart.countSolvedForms() + "' "
                            + "chartsize='" + chart.size() + "' "
                            + "time='" + time_solver + "' />");
                } else {
                    // Operation = solve
                    if( !solvable ) {
                        out.println("<result solvable='false' count='0' "
			    + "fragments='" + options.getGraph().getAllRoots().size() + "' "
                                + "chartsize='" + chart.size() + "' "
                                + "time-chart='" + time_solver + "' />");
                    } else {
                        StringWriter buf = new StringWriter();
                        XmlEncodingWriter enc = new XmlEncodingWriter(buf);
                        long count = 0;
                        
                        // extract solved forms
                        try {
                            long start_extraction = System.currentTimeMillis();
                            SolvedFormIterator it = new SolvedFormIterator(chart,options.getGraph());
                            while( it.hasNext() ) {
                                List<DomEdge> domedges = it.next();
                                count++;
                                
                                if( !options.hasOptionNoOutput() ) {
                                    buf.append("  <solution string='");
                                    options.getOutputCodec().encode(options.getGraph(), domedges, options.getLabels(), enc);
                                    buf.append("' />\n");
                                }
                            }
                            long end_extraction = System.currentTimeMillis();
                            long time_extraction = end_extraction - start_extraction;
                            
                            out.println("<result solvable='true' count='" + count + "' "
			    + "fragments='" + options.getGraph().getAllRoots().size() + "' "
                                    + " chartsize='" + chart.size() + "' "
                                    + " time-chart='" + time_solver + "' "
                                    + " time-extraction='" + time_extraction + "' >");
                            out.print(buf.toString());
                            out.println("</result>");
                        } catch (MalformedDomgraphException e) {
                            sendError(out, e.getExitcode() + ExitCodes.MALFORMED_DOMGRAPH_BASE_OUTPUT, "Output of the solved forms of this graph is not supported by this output codec.");
                            sock.close();
                            continue;
                        }
                    }
                }
                break;
                
            
            case convert:
                if( options.hasOptionNoOutput() ) {
                    out.println("<result />");
                } else {
                    if( options.getOutputCodec().getType() != OutputCodec.Type.GRAPH ) {
                        sendError(out, ExitCodes.OUTPUT_CODEC_NOT_APPLICABLE,  "Output codec must be a graph codec!");
                        sock.close();
                        continue;
                    }
                    
                    try {
                        XmlEncodingWriter enc = new XmlEncodingWriter(out);
                        out.print("<result usr='");
                        options.getOutputCodec().encode(options.getGraph(), null, options.getLabels(), enc);
                        out.println("' />");
                    } catch(MalformedDomgraphException e) {
                        sendError(out, ExitCodes.MALFORMED_DOMGRAPH_BASE_OUTPUT + e.getExitcode(), "This graph is not supported by the specified output codec.");
                        sock.close();
                        continue;
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
                
                if( compactifiable ) {
                    programExitCode |= ExitCodes.CLASSIFY_COMPACTIFIABLE;
                }
                
                if( options.getGraph().isHypernormallyConnected() ) {
                    programExitCode |= ExitCodes.CLASSIFY_HN_CONNECTED;
                } 
                
                if( options.getGraph().isLeafLabelled() ) {
                    programExitCode |= ExitCodes.CLASSIFY_LEAF_LABELLED;
                }
                
                long afterEverything = System.currentTimeMillis();
                
                out.println("<result code='" + programExitCode + "' "
                		+ "time1='" + (afterParsing - start) + "' "
                		+ "time2='" + (afterEverything - afterParsing) + "' "
                        + "weaklynormal='" + weaklyNormal + "' "
                        + "normal='" + normal + "' "
                        + "compact='" + compact + "' "
                        + "compactifiable='" + compactifiable + "' "
                        + "hypernormallyconnected='" + options.getGraph().isHypernormallyConnected() + "' "
                        + "leaflabelled='" + options.getGraph().isLeafLabelled() + "' "
                        + "/>");
                break;
                
                
            case display:
                if( options.getGraph() != null ) {
                    if(Ubench.getInstance().addNewTab(
                            options.getInputName(),
                            options.getGraph(),
                            options.getLabels()))  {
                        out.println("<result code='0' />");
                    } else {
                        sendError(out, ExitCodes.GRAPH_DRAWING_ERROR, "An error occurred while drawing the graph.");
                    }
                } else {
                    Ubench.getInstance();
                    out.println("<result code='0' />");
                }
                break;

                
            case help:
                out.println("<result help='" + helpString(options.getHelpArgument()) + "' />");
                break;
                
            case _displayCodecs:
                out.println("<result>");
                
                for( Class codec : parser.getCodecManager().getAllInputCodecs()) {
                    displayOneCodec(codec, out, "input");
                }
                
                for( Class codec : parser.getCodecManager().getAllOutputCodecs()) {
                    displayOneCodec(codec, out, "output");
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
        
            
            in.close();
            out.close();
            sock.close();
            
        }  // while(true)
    }
    
    private static void displayOneCodec(Class codec, PrintWriter out, String type) {
        String name = CodecManager.getCodecName(codec);
        String ext = CodecManager.getCodecExtension(codec);
        
        out.print("  <codec name='" + name + "' ");
        
        if( ext != null ) {
            out.print("extension='" + ext + "' ");
        }
        
        out.println("type='" + type + "' />");
    }

    private static void log(String x) {
        if( logging ) {
            logTo.println(x);
        }
    }


    private static void sendError(PrintWriter out, int exitcode, String string) {
        out.println("<error code='" + exitcode + "' explanation='" + string + "' />");
    }

    // TODO - update the help info
    private static String helpString(Operation op) {
        StringBuffer ret = new StringBuffer();
        
        if( (op == null) || (op.longDescription == null) ) {
            ret.append("Usage: java -jar Utool.jar <subcommand> [options] [args]\n");
            ret.append("Type `utool help <subcommand>' for help on a specific subcommand.\n");
            ret.append("Type `utool --help-options' for a list of global options.\n");
            ret.append("Type `utool --display-codecs' for a list of supported codecs.\n\n");
            
            ret.append("Available subcommands:\n");
            for( Operation _op : Operation.values() ) {
                if( _op.shortDescription != null ) {
                    ret.append(String.format("    %1$-12s %2$s.\n",
                            _op, _op.shortDescription));
                }
            }

            ret.append("\nUtool/Java is the Swiss Army Knife of Underspecification (Java version).\n");
            ret.append("For more information, see http://www.coli.uni-sb.de/projects/chorus/utool/\n");
        } else {
            ret.append("utool " + op + ": " + op.shortDescription + ".\n");
            ret.append(op.longDescription + "\n");
        }
        
        return ret.toString();
    }
    
    private static String versionString() {
        return "Utool/Java (The Swiss Army Knife of Underspecification), version "
        + GlobalDomgraphProperties.getVersion() + "\n"
        + "(running in server mode)\n"
        + "Created by the CHORUS project, SFB 378, Saarland University\n\n";
    }

    
    
    
    

    /****************************************************************
     * UNIT TESTS
     ****************************************************************/
    
    /*
     * - codecs yield correct results (domcon-oz and chain)
     * - ensure correct communication; then try to skip socket communication
     *   and call things directly
     */
}
