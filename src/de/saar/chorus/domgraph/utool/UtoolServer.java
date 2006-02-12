/*
 * @(#)UtoolServer.java created 10.02.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.utool;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

import de.saar.chorus.domgraph.chart.Chart;
import de.saar.chorus.domgraph.chart.ChartSolver;
import de.saar.chorus.domgraph.chart.SolvedFormIterator;
import de.saar.chorus.domgraph.codec.InputCodec;
import de.saar.chorus.domgraph.codec.MalformedDomgraphException;
import de.saar.chorus.domgraph.codec.OutputCodec;
import de.saar.chorus.domgraph.equivalence.IndividualRedundancyElimination;
import de.saar.chorus.domgraph.equivalence.RedundancyEliminationSplitSource;
import de.saar.chorus.domgraph.graph.DomEdge;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.utool.AbstractOptions.Operation;
import de.saar.getopt.ConvenientGetopt;

public class UtoolServer {

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        ServerSocket ssock = null;
        int port;
        ConvenientGetopt getopt = new ConvenientGetopt("Utool Server", "java -jar UtoolServer.jar", "");
        XmlParser parser = new XmlParser();
        AbstractOptions options;
        
        boolean weaklyNormal = false;
        boolean normal = false;
        boolean compact = false;
        boolean compactifiable = false;
        DomGraph compactGraph = null;

        
        // parse cmd-line options
        getopt.addOption('p', "port", ConvenientGetopt.REQUIRED_ARGUMENT, "The port on which the server will listen", "2802");
        getopt.parse(args);
        
        port = Integer.parseInt(getopt.getValue('p'));
        
        // open server socket
        ssock = new ServerSocket(port);
        System.err.println("Listening on port " + port + "...");
        
        
        /*
         * TODO:
         *  - if other side disconnects the socket before it finishes writing,
         *    readLine() will throw a SocketException
         */
        while( true ) {
            // accept one connection
            System.err.print("Waiting for connection ... ");
            Socket sock = ssock.accept();
            System.err.println("accepted connection from " + sock);
            
            PrintWriter out = new PrintWriter(sock.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            String line = null;
            StringBuffer cmd = new StringBuffer();
            
            // read one command
            while( (line = in.readLine()) != null ) {
                System.err.println("read: " + line);
                cmd.append(line);
            }
            
            // parse the command
            // ANALOGOUS to Utool
            try {
                options = parser.parse(cmd.toString());
            } catch(AbstractOptionsParsingException e) {
                // TODO Should some errors be fatal, i.e. lead to termination
                // of the server?
                sendError(out, e.getExitcode(), e.comprehensiveErrorMessage());
                sock.close();
                continue;
            }
            
            // check statistics and compactify graph
            if( options.getOperation().requiresInput ) {
                weaklyNormal = options.getGraph().isWeaklyNormal();
                normal = options.getGraph().isNormal();
                compact = options.getGraph().isCompact();
                compactifiable = options.getGraph().isCompactifiable();
                
                // compactify if necessary
                compactGraph = options.getGraph().compactify();
            }            
            
            
            // now do something, depending on the specified operation
            switch(options.getOperation()) {
            case solve:
            case solvable:
                if( !weaklyNormal ) {
                    sendError(out, ExitCodes.ILLFORMED_GRAPH, "Cannot solve graphs that are not weakly normal!");
                    sock.close();
                    continue;
                }
                
                if( !compact && !compactifiable ) {
                    sendError(out, ExitCodes.ILLFORMED_GRAPH, "Cannot solve graphs that are not compact and not compactifiable!");
                    sock.close();
                    continue;
                }

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
                            + "chartsize='" + chart.size() + "' "
                            + "time='" + time_solver + "' />");
                } else {
                    // Operation = solve
                    if( !solvable ) {
                        out.println("<result solvable='false' count='0' "
                                + "chartsize='" + chart.size() + "' "
                                + "time-chart='" + time_solver + "' />");
                    } else {
                        StringWriter buf = new StringWriter();
                        long count = 0;
                        
                        // extract solved forms
                        try {
                            long start_extraction = System.currentTimeMillis();
                            SolvedFormIterator it = new SolvedFormIterator(chart,options.getGraph());
                            while( it.hasNext() ) {
                                List<DomEdge> domedges = it.next();
                                count++;
                                
                                if( !options.hasOptionNoOutput() ) {
                                    buf.write("  <solution string='");
                                    // TODO encode XML entities properly (esp. "'"!!)
                                    options.getOutputCodec().encode(options.getGraph(), domedges, options.getLabels(), buf);
                                    buf.write("' />\n");
                                }
                            }
                            long end_extraction = System.currentTimeMillis();
                            long time_extraction = end_extraction - start_extraction;
                            
                            out.println("<result solvable='true' count='" + count + "' "
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
                if( options.getOutputCodec().getType() != OutputCodec.Type.GRAPH ) {
                    sendError(out, ExitCodes.OUTPUT_CODEC_NOT_APPLICABLE,  "Output codec must be a graph codec!");
                    sock.close();
                    continue;
                }

                try {
                    out.print("<result usr='");
                    options.getOutputCodec().encode(options.getGraph(), null, options.getLabels(), out);
                    out.println("' />");
                } catch(MalformedDomgraphException e) {
                    sendError(out, ExitCodes.MALFORMED_DOMGRAPH_BASE_OUTPUT + e.getExitcode(), "This graph is not supported by the specified output codec.");
                    sock.close();
                    continue;
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
                
                out.println("<result code='" + programExitCode + "' "
                        + "weaklynormal='" + weaklyNormal + "' "
                        + "normal='" + normal + "' "
                        + "compact='" + compact + "' "
                        + "compactifiable='" + compactifiable + "' "
                        + "hypernormallyconnected='" + options.getGraph().isHypernormallyConnected() + "' "
                        + "leaflabelled='" + options.getGraph().isLeafLabelled() + "' "
                        + "/>");
                break;
                
            case help:
                out.println("<result help='" + helpString(options.getHelpArgument()) + "' />");
                break;
                
            case _displayCodecs:
                out.println("<result>");
                
                for( InputCodec codec : parser.getCodecManager().getAllInputCodecs()) {
                    out.print("  <codec name='" + codec.getName() + "' ");
                    
                    if( codec.getExtension() != null ) {
                        out.print("extension='" + codec.getExtension() + "' ");
                    }
                    
                    out.println("type='input' />");
                }
                
                for( OutputCodec codec : parser.getCodecManager().getAllOutputCodecs()) {
                    out.print("  <codec name='" + codec.getName() + "' ");
                    
                    if( codec.getExtension() != null ) {
                        out.print("extension='" + codec.getExtension() + "' ");
                    }
                    
                    out.println("type='output' />");
                }
                
                out.println("</result>");
                break;
                
            case _version:
                out.println("<result version='" + versionString() + "' />");
                break;
            }
        
            
            in.close();
            out.close();
            sock.close();
            
        }  // while(true)
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
        return "Utool/Java (The Swiss Army Knife of Underspecification), version 0.9\n"
        + "(running in server mode)\n"
        + "Created by the CHORUS project, SFB 378, Saarland University\n\n";
    }

}
