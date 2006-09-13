/*
 * @(#)ConnectionManager.java created 12.09.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.utool.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import de.saar.chorus.domgraph.chart.Chart;
import de.saar.chorus.domgraph.chart.ChartSolver;
import de.saar.chorus.domgraph.chart.SolvedFormIterator;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.EdgeData;
import de.saar.chorus.domgraph.graph.EdgeType;
import de.saar.chorus.domgraph.graph.NodeData;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.domgraph.graph.NodeType;
import de.saar.chorus.domgraph.utool.AbstractOptions;


/**
 * The Utool main program for accessing the Domgraph functionality
 * in server mode. Utool ("Underspecification Tool") is the
 * Swiss Army Knife of Underspecification (Java version). This
 * version will accept commands in XML format from a socket. It
 * is started by calling the command-line version with command "server".<p>
 * 
 * The operation of this class is described in more detail in the
 * end-user documentation of Utool.<p>
 * 
 * Technically, the class <code>ConnectionManager</code> is only
 * responsible for accepting a new socket connection. It then
 * starts a <code>ServerThread</code>, which does all the work
 * of XML parsing and domgraph computations.
 * 
 * @author Alexander Koller
 *
 */

public class ConnectionManager {
    private static Logger logger;

    public static void startServer(AbstractOptions cmdlineOptions) throws IOException { //throws Exception {
        ServerSocket ssock = null;
        int port;
        
        logger = new Logger(cmdlineOptions.hasOptionLogging(), cmdlineOptions.getLogWriter());
        port = cmdlineOptions.getPort();
        
        
        // warm up if requested
        if( cmdlineOptions.hasOptionWarmup() ) {
            warmup();
        }
        
        
        // open server socket
        ssock = new ServerSocket(port);
        logger.log("Listening on port " + port + "...");
        
        
        while( true ) {
            // accept one connection
            logger.log("Waiting for connection ... ");
            Socket sock = ssock.accept();
            logger.log("accepted connection from " + sock);
            
            ServerThread.startServerThread(sock, logger);
        }

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
        
        logger.log("Warming up the server (" + PASSES + " passes) ... ");
        
        for( int i = 0; i < PASSES; i++ ) {
            Chart chart = new Chart();
            logger.log("  - pass " + (i+1));
            
            ChartSolver.solve(graph, chart);
            SolvedFormIterator it = new SolvedFormIterator(chart,graph);
            while( it.hasNext() ) {
                it.next();
            }
        }
        
        
        logger.log("Utool is now warmed up.");
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
    

}
