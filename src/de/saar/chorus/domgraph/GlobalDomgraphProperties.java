/*
 * @(#)PropertiesDomGraph.java created 25.03.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class GlobalDomgraphProperties {
    private static Properties props = new Properties();
    
    static {
        InputStream in = 
            GlobalDomgraphProperties.class.getResourceAsStream("domgraph.properties");
        
        if( in != null ) {
            try {
                props.load(in);
            } catch (IOException e) {
                System.err.println("Couldn't open domgraph.properties! This means your distribution is broken.");
                props = new Properties(); 
            }
        }
    }
    
    
    public static String getVersion() {
        return (String) props.get("domgraph.version");
    }
    

}
