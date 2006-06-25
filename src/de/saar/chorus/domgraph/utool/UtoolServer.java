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
import de.saar.chorus.domgraph.chart.OneSplitSource;
import de.saar.chorus.domgraph.chart.SolvedFormIterator;
import de.saar.chorus.domgraph.codec.CodecManager;
import de.saar.chorus.domgraph.codec.MalformedDomgraphException;
import de.saar.chorus.domgraph.equivalence.IndividualRedundancyElimination;
import de.saar.chorus.domgraph.equivalence.RedundancyEliminationSplitSource;
import de.saar.chorus.domgraph.graph.DomEdge;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.EdgeData;
import de.saar.chorus.domgraph.graph.EdgeType;
import de.saar.chorus.domgraph.graph.NodeData;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.domgraph.graph.NodeType;
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
        
        
        // warm up if requested
        if( cmdlineOptions.hasOptionWarmup() ) {
            warmup();
        }
        
        
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
            case solvable:
                if( options.hasOptionNochart() ) {
                    long start_solver = System.currentTimeMillis();
                    boolean solvable = OneSplitSource.isGraphSolvable(options.getGraph());
                    long end_solver = System.currentTimeMillis();
                    long time_solver = end_solver - start_solver;
      
                    out.println("<result solvable='" + solvable + "' "
                            + "fragments='" + options.getGraph().getAllRoots().size() + "' "
                                        + "time='" + time_solver + "' />");
                    break;
                }
                    
                // intentional fall-through for the non-"nochart" case
                

            case solve:
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
                boolean solvable;

                if( options.hasOptionEliminateEquivalence() ) {
                    solvable = ChartSolver.solve(compactGraph, chart, 
                            new RedundancyEliminationSplitSource(
                                    new IndividualRedundancyElimination(compactGraph, 
                                            options.getLabels(), options.getEquations()), compactGraph));
                } else {
                    solvable = ChartSolver.solve(compactGraph, chart); 
                }
                
                
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
    
    /**
     * Warms up the server after it has been started. This exercises the
     * most time-critical methods in the solver, in order to make sure the
     * JVM compiles them to native code and your later commands are executed
     * more efficiently.<p>
     * 
     * At the moment, the warmup command enumerates all solved forms of the
     * pure chain of length 10, three times.
     * 
     */
    private static void warmup() {
        final int PASSES = 3;
        DomGraph graph = new DomGraph();
        NodeLabels labels = new NodeLabels();
        makeWarmupGraph(graph, labels);
        
        if( logging ) {
            logTo.print("Warming up the server (" + PASSES + " passes): ");
        }
        
        for( int i = 0; i < PASSES; i++ ) {
            Chart chart = new Chart();
            logTo.print((i+1) + " ");
            logTo.flush();
            
            ChartSolver.solve(graph, chart);
            SolvedFormIterator it = new SolvedFormIterator(chart,graph);
            while( it.hasNext() ) {
                it.next();
            }
        }
        
        logTo.println("done.");
        logTo.flush();
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


    public static void makeWarmupGraph(DomGraph graph, NodeLabels labels) {
        graph.clear();
        labels.clear();

        graph.addNode("y0", new NodeData(NodeType.LABELLED));
        labels.addLabel("y0", "a0");
        graph.addNode("x1", new NodeData(NodeType.LABELLED));
        labels.addLabel("x1", "f1");
        graph.addNode("xl1", new NodeData(NodeType.UNLABELLED));
        graph.addNode("xr1", new NodeData(NodeType.UNLABELLED));
        graph.addNode("y1", new NodeData(NodeType.LABELLED));
        labels.addLabel("y1", "a1");
        graph.addNode("x2", new NodeData(NodeType.LABELLED));
        labels.addLabel("x2", "f2");
        graph.addNode("xl2", new NodeData(NodeType.UNLABELLED));
        graph.addNode("xr2", new NodeData(NodeType.UNLABELLED));
        graph.addNode("y2", new NodeData(NodeType.LABELLED));
        labels.addLabel("y2", "a2");
        graph.addNode("x3", new NodeData(NodeType.LABELLED));
        labels.addLabel("x3", "f3");
        graph.addNode("xl3", new NodeData(NodeType.UNLABELLED));
        graph.addNode("xr3", new NodeData(NodeType.UNLABELLED));
        graph.addNode("y3", new NodeData(NodeType.LABELLED));
        labels.addLabel("y3", "a3");
        graph.addNode("x4", new NodeData(NodeType.LABELLED));
        labels.addLabel("x4", "f4");
        graph.addNode("xl4", new NodeData(NodeType.UNLABELLED));
        graph.addNode("xr4", new NodeData(NodeType.UNLABELLED));
        graph.addNode("y4", new NodeData(NodeType.LABELLED));
        labels.addLabel("y4", "a4");
        graph.addNode("x5", new NodeData(NodeType.LABELLED));
        labels.addLabel("x5", "f5");
        graph.addNode("xl5", new NodeData(NodeType.UNLABELLED));
        graph.addNode("xr5", new NodeData(NodeType.UNLABELLED));
        graph.addNode("y5", new NodeData(NodeType.LABELLED));
        labels.addLabel("y5", "a5");
        graph.addNode("x6", new NodeData(NodeType.LABELLED));
        labels.addLabel("x6", "f6");
        graph.addNode("xl6", new NodeData(NodeType.UNLABELLED));
        graph.addNode("xr6", new NodeData(NodeType.UNLABELLED));
        graph.addNode("y6", new NodeData(NodeType.LABELLED));
        labels.addLabel("y6", "a6");
        graph.addNode("x7", new NodeData(NodeType.LABELLED));
        labels.addLabel("x7", "f7");
        graph.addNode("xl7", new NodeData(NodeType.UNLABELLED));
        graph.addNode("xr7", new NodeData(NodeType.UNLABELLED));
        graph.addNode("y7", new NodeData(NodeType.LABELLED));
        labels.addLabel("y7", "a7");
        graph.addNode("x8", new NodeData(NodeType.LABELLED));
        labels.addLabel("x8", "f8");
        graph.addNode("xl8", new NodeData(NodeType.UNLABELLED));
        graph.addNode("xr8", new NodeData(NodeType.UNLABELLED));
        graph.addNode("y8", new NodeData(NodeType.LABELLED));
        labels.addLabel("y8", "a8");
        graph.addNode("x9", new NodeData(NodeType.LABELLED));
        labels.addLabel("x9", "f9");
        graph.addNode("xl9", new NodeData(NodeType.UNLABELLED));
        graph.addNode("xr9", new NodeData(NodeType.UNLABELLED));
        graph.addNode("y9", new NodeData(NodeType.LABELLED));
        labels.addLabel("y9", "a9");
        graph.addNode("x10", new NodeData(NodeType.LABELLED));
        labels.addLabel("x10", "f10");
        graph.addNode("xl10", new NodeData(NodeType.UNLABELLED));
        graph.addNode("xr10", new NodeData(NodeType.UNLABELLED));
        graph.addNode("y10", new NodeData(NodeType.LABELLED));
        labels.addLabel("y10", "a10");
        graph.addNode("x11", new NodeData(NodeType.LABELLED));
        labels.addLabel("x11", "f11");
        graph.addNode("xl11", new NodeData(NodeType.UNLABELLED));
        graph.addNode("xr11", new NodeData(NodeType.UNLABELLED));
        graph.addNode("y11", new NodeData(NodeType.LABELLED));
        labels.addLabel("y11", "a11");
        graph.addNode("x12", new NodeData(NodeType.LABELLED));
        labels.addLabel("x12", "f12");
        graph.addNode("xl12", new NodeData(NodeType.UNLABELLED));
        graph.addNode("xr12", new NodeData(NodeType.UNLABELLED));
        graph.addNode("y12", new NodeData(NodeType.LABELLED));
        labels.addLabel("y12", "a12");

        graph.addEdge("x1", "xl1", new EdgeData(EdgeType.TREE));
        graph.addEdge("x1", "xr1", new EdgeData(EdgeType.TREE));
        graph.addEdge("xl1", "y0", new EdgeData(EdgeType.DOMINANCE));
        graph.addEdge("xr1", "y1", new EdgeData(EdgeType.DOMINANCE));
        graph.addEdge("x2", "xl2", new EdgeData(EdgeType.TREE));
        graph.addEdge("x2", "xr2", new EdgeData(EdgeType.TREE));
        graph.addEdge("xl2", "y1", new EdgeData(EdgeType.DOMINANCE));
        graph.addEdge("xr2", "y2", new EdgeData(EdgeType.DOMINANCE));
        graph.addEdge("x3", "xl3", new EdgeData(EdgeType.TREE));
        graph.addEdge("x3", "xr3", new EdgeData(EdgeType.TREE));
        graph.addEdge("xl3", "y2", new EdgeData(EdgeType.DOMINANCE));
        graph.addEdge("xr3", "y3", new EdgeData(EdgeType.DOMINANCE));
        graph.addEdge("x4", "xl4", new EdgeData(EdgeType.TREE));
        graph.addEdge("x4", "xr4", new EdgeData(EdgeType.TREE));
        graph.addEdge("xl4", "y3", new EdgeData(EdgeType.DOMINANCE));
        graph.addEdge("xr4", "y4", new EdgeData(EdgeType.DOMINANCE));
        graph.addEdge("x5", "xl5", new EdgeData(EdgeType.TREE));
        graph.addEdge("x5", "xr5", new EdgeData(EdgeType.TREE));
        graph.addEdge("xl5", "y4", new EdgeData(EdgeType.DOMINANCE));
        graph.addEdge("xr5", "y5", new EdgeData(EdgeType.DOMINANCE));
        graph.addEdge("x6", "xl6", new EdgeData(EdgeType.TREE));
        graph.addEdge("x6", "xr6", new EdgeData(EdgeType.TREE));
        graph.addEdge("xl6", "y5", new EdgeData(EdgeType.DOMINANCE));
        graph.addEdge("xr6", "y6", new EdgeData(EdgeType.DOMINANCE));
        graph.addEdge("x7", "xl7", new EdgeData(EdgeType.TREE));
        graph.addEdge("x7", "xr7", new EdgeData(EdgeType.TREE));
        graph.addEdge("xl7", "y6", new EdgeData(EdgeType.DOMINANCE));
        graph.addEdge("xr7", "y7", new EdgeData(EdgeType.DOMINANCE));
        graph.addEdge("x8", "xl8", new EdgeData(EdgeType.TREE));
        graph.addEdge("x8", "xr8", new EdgeData(EdgeType.TREE));
        graph.addEdge("xl8", "y7", new EdgeData(EdgeType.DOMINANCE));
        graph.addEdge("xr8", "y8", new EdgeData(EdgeType.DOMINANCE));
        graph.addEdge("x9", "xl9", new EdgeData(EdgeType.TREE));
        graph.addEdge("x9", "xr9", new EdgeData(EdgeType.TREE));
        graph.addEdge("xl9", "y8", new EdgeData(EdgeType.DOMINANCE));
        graph.addEdge("xr9", "y9", new EdgeData(EdgeType.DOMINANCE));
        graph.addEdge("x10", "xl10", new EdgeData(EdgeType.TREE));
        graph.addEdge("x10", "xr10", new EdgeData(EdgeType.TREE));
        graph.addEdge("xl10", "y9", new EdgeData(EdgeType.DOMINANCE));
        graph.addEdge("xr10", "y10", new EdgeData(EdgeType.DOMINANCE));
        graph.addEdge("x11", "xl11", new EdgeData(EdgeType.TREE));
        graph.addEdge("x11", "xr11", new EdgeData(EdgeType.TREE));
        graph.addEdge("xl11", "y10", new EdgeData(EdgeType.DOMINANCE));
        graph.addEdge("xr11", "y11", new EdgeData(EdgeType.DOMINANCE));
        graph.addEdge("x12", "xl12", new EdgeData(EdgeType.TREE));
        graph.addEdge("x12", "xr12", new EdgeData(EdgeType.TREE));
        graph.addEdge("xl12", "y11", new EdgeData(EdgeType.DOMINANCE));
        graph.addEdge("xr12", "y12", new EdgeData(EdgeType.DOMINANCE));
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
