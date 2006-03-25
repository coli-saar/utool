/*
 * @(#)UbenchServerThread.java created 22.06.2005
 * 
 * Copyright (c) 2005 Alexander Koller
 *  
 */

package de.saar.chorus.ubench.gui;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.ubench.JDomGraph;
import de.saar.chorus.ubench.Server;

public class UbenchServerThread extends Thread {
    private int port;
    private Pattern openPattern;
        
    public UbenchServerThread(int port) {
        super();
        this.port = port;
        
        openPattern = Pattern.compile("\\s*<open\\s+filename\\s*=\\s*(?:\"|\')([^\"\']+).*");
        //importPattern = Pattern.compile("\\s*<import\\s+filename\\s*=\\s*\"|\'([^\"\']+).*");
        
        System.err.println(openPattern);
        
        System.err.println("Ubench is listening on port " + port);
    }
        
    public void run() {
        while( true ) {
            // accept connection
            final Server serv = new Server(port);
            
            // read a line from the socket
            String messageStr = serv.read();
            String filename = null;
            
            
        //    System.err.println("[Server] " + messageStr);
            
            // "open" command
            Matcher mOpen = openPattern.matcher(messageStr);
            if( mOpen.matches() ) {
                filename = mOpen.group(1);
            }
            
            /*
            Matcher mImport = importPattern.matcher(messageStr);
            if( mImport.matches() ) {
                filename = mImport.group(1);
            }
            */
            
            if( filename != null ) {
          //      System.err.println("[Server] open: " + filename);
                
                DomGraph aDomGraph = new DomGraph();
                NodeLabels labels = new NodeLabels();
                JDomGraph graph = Main.importGraph(filename, aDomGraph, labels);
                Main.addNewTab(graph, filename, aDomGraph, true, true, labels);
            } 
            
            serv.close();
        }
    }
}
