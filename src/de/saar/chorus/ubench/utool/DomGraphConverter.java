/*
 * @(#)DomGraphConverter.java created 19.06.2005
 * 
 * Copyright (c) 2005 Alexander Koller
 *  
 */

package de.saar.chorus.ubench.utool;


import org.jgraph.graph.DefaultGraphCell;

import de.saar.chorus.libdomgraph.DomGraph;
import de.saar.chorus.libdomgraph.DomSolver;
import de.saar.chorus.libdomgraph.NodeLabels;
import de.saar.chorus.ubench.JDomGraph;


public class DomGraphConverter {
    private DomGraph graph;
    private NodeLabels labels;
    private JDomGraph jgraph;
    private DomSolver solver;
    
    public DomGraphConverter(DomSolver solver, DomGraph graph) {
        this.graph = graph;
        this.labels = solver.getLabels();
        this.solver = solver;
    }
    
    public JDomGraph toJDomGraph() {
        jgraph = new JDomGraph();
        
        for( int i = 0; i < solver.num_nodes(graph); i++ ) {
            String name = solver.getNodename(graph, i);
            de.saar.chorus.libdomgraph.NodeData data = solver.getNodeData(graph, name);
            de.saar.chorus.ubench.NodeData leoData = toLeonardoNodeData(data); 
            jgraph.addNode(leoData);
        }
        
        for( int i = 0; i < solver.num_edges(graph); i++ ) {
            de.saar.chorus.libdomgraph.EdgeData data = solver.getEdgeData(graph, i);
            de.saar.chorus.ubench.EdgeData leoData = toLeonardoEdgeData(data); 
            DefaultGraphCell src = jgraph.getNodeForName(solver.getEdgeSource(graph, i));
            DefaultGraphCell tgt = jgraph.getNodeForName(solver.getEdgeTarget(graph, i));
            jgraph.addEdge(leoData, src, tgt);
        }
        
        return jgraph;
    }
    
    de.saar.chorus.ubench.NodeData toLeonardoNodeData(de.saar.chorus.libdomgraph.NodeData data) {
        de.saar.chorus.ubench.NodeType type;
        String name = data.getName();
        
        if( data.getType() == de.saar.chorus.libdomgraph.NodeType.LABELLED ) {
            type = de.saar.chorus.ubench.NodeType.labelled;
            return new de.saar.chorus.ubench.NodeData(type, data.getName(),
                        labels.getLabel(name), jgraph);
        } else if( data.getType() == de.saar.chorus.libdomgraph.NodeType.UNLABELLED ) {
            type = de.saar.chorus.ubench.NodeType.unlabelled;
            return new de.saar.chorus.ubench.NodeData(type, data.getName(), jgraph);
        } else {
            System.err.println("Error: Unknown node type");
            return null;
        }
    }
    
    de.saar.chorus.ubench.EdgeData toLeonardoEdgeData(de.saar.chorus.libdomgraph.EdgeData data) {
        de.saar.chorus.ubench.EdgeType type;
        String name = data.getName();
        
        if( data.getType() == de.saar.chorus.libdomgraph.EdgeType.DOMINANCE_EDGE ) {
            type = de.saar.chorus.ubench.EdgeType.dominance;
        } else if( data.getType() == de.saar.chorus.libdomgraph.EdgeType.TREE_EDGE ) {
            type = de.saar.chorus.ubench.EdgeType.solid;
        } else {
            System.err.println("Error: Unknown edge type");
            return null;
        }
     
        return new de.saar.chorus.ubench.EdgeData(type, name, jgraph);
    }
}
