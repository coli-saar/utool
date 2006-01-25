/*
 * @(#)Main.java created 25.01.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.graph;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import de.saar.chorus.domgraph.codec.gxl.GxlCodec;

public class Main {
    public static void main(String[] args) throws Exception {
        DomGraph g = new DomGraph();
        NodeLabels l = new NodeLabels();
        
        GxlCodec codec = new GxlCodec();
        
        codec.decode(new FileReader(args[0]), g, l);
        
        List<Set<String>> w = new ArrayList<Set<String>>();
        g.wccs(w);
        System.err.println("wccs: " + w);
    }
}
