/*
 * @(#)LeonardoServerThread.java created 22.06.2005
 * 
 * Copyright (c) 2005 Alexander Koller
 *  
 */

package de.saar.chorus.ubench.gui;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.saar.chorus.ubench.JDomGraph;
import de.saar.chorus.ubench.Server;

public class LeonardoServerThread extends Thread {
    private int port;
    private Pattern openPattern, importPattern;
        
    public LeonardoServerThread(int port) {
        super();
        this.port = port;
        
        openPattern = Pattern.compile("\\s*<open\\s+filename\\s*=\\s*\"([^\"]+).*");
        importPattern = Pattern.compile("\\s*<import\\s+filename\\s*=\\s*\"([^\"]+).*");
        
        System.err.println("Leonardo is listening on port " + port);
    }
        
    public void run() {
        while( true ) {
            // accept connection
            final Server serv = new Server(port);
            
            // read a line from the socket
            String messageStr = serv.read();
            
            // "open" command
            Matcher mOpen = openPattern.matcher(messageStr);
            if( mOpen.matches() ) {
                String filename = mOpen.group(1);
                Main.addNewTab(Main.loadGraph(filename), filename, true, true);
            } else {
                Matcher mImport = importPattern.matcher(messageStr);
                if( mImport.matches() ) {
                    String filename = mImport.group(1);
                    JDomGraph graph = Main.importGraph(filename);
                    if( graph != null ) {
                        Main.addNewTab(graph, filename, true, true);
                    }
                }
            }
            
            serv.close();
        }
    }
}
