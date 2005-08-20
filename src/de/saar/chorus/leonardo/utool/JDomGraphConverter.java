/*
 * @(#)JDomGraphConverter.java created 20.06.2005
 * 
 * Copyright (c) 2005 Alexander Koller
 *  
 */

package de.saar.chorus.leonardo.utool;

import java.util.HashMap;
import java.util.Map;

import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphCell;

import de.saar.chorus.leonardo.JDomGraph;
import de.saar.chorus.libdomgraph.DomGraph;
import de.saar.chorus.libdomgraph.DomSolver;
import de.saar.chorus.libdomgraph.NodeLabels;

public class JDomGraphConverter {
     DomSolver solver;
    
    public JDomGraphConverter(DomSolver solver) {
        this.solver = solver;
        solver.setEmptyGraph();
    }
 
    public void toDomGraph(JDomGraph jgraph) {
        for( DefaultGraphCell node : jgraph.getNodes() ) {
            de.saar.chorus.leonardo.NodeData data = jgraph.getNodeData(node);
            
            solver.newNode(solver.getGraph(), data.getName(), toUtoolNodeType(data.getType()));
            
            if( data.getType() == de.saar.chorus.leonardo.NodeType.labelled ) {
                solver.addLabel(data.getName(), data.getLabel());
            }
        }
        
        for( DefaultEdge edge : jgraph.getEdges() ) {
            solver.newEdge(solver.getGraph(),
                            jgraph.getNodeData(jgraph.getSourceNode(edge)).getName(),
                            jgraph.getNodeData(jgraph.getTargetNode(edge)).getName(),
                            jgraph.getEdgeData(edge).getName(),
                            toUtoolEdgeType(jgraph.getEdgeData(edge).getType()));
        }
    }
    
    int toUtoolEdgeType(de.saar.chorus.leonardo.EdgeType type) {
        if( type == de.saar.chorus.leonardo.EdgeType.dominance ) {
            return de.saar.chorus.libdomgraph.EdgeType.DOMINANCE_EDGE;
        } else if( type == de.saar.chorus.leonardo.EdgeType.solid ) {
            return de.saar.chorus.libdomgraph.EdgeType.TREE_EDGE;
        } else {
            System.err.println("Error: Edge data has wrong type!");
            return -1;
        }        
    }
    
    int toUtoolNodeType(de.saar.chorus.leonardo.NodeType type) {
        if( type == de.saar.chorus.leonardo.NodeType.labelled ) {
            return de.saar.chorus.libdomgraph.NodeType.LABELLED;
        } else if( type == de.saar.chorus.leonardo.NodeType.unlabelled ) {
            return de.saar.chorus.libdomgraph.NodeType.UNLABELLED;
        } else {
            System.err.println("Error: Node data has wrong type!");
            return -1;
        }        
    }
    
    de.saar.chorus.libdomgraph.NodeData toUtoolNodeData(de.saar.chorus.leonardo.NodeData data) {
        String name = data.getName();
        
        if( data.getType() == de.saar.chorus.leonardo.NodeType.labelled ) {
            solver.addLabel(name, data.getLabel());
            return new de.saar.chorus.libdomgraph.NodeData(name, de.saar.chorus.libdomgraph.NodeType.LABELLED);
        } else if( data.getType() == de.saar.chorus.leonardo.NodeType.unlabelled ) {
            return new de.saar.chorus.libdomgraph.NodeData(name, de.saar.chorus.libdomgraph.NodeType.UNLABELLED);
        } else {
            System.err.println("Error: Node data has wrong type!");
            return null;
        }
    }
    
    de.saar.chorus.libdomgraph.EdgeData toUtoolEdgeData(de.saar.chorus.leonardo.EdgeData data) {
        String name = data.getName();
        
        if( data.getType() == de.saar.chorus.leonardo.EdgeType.dominance ) {
            return new de.saar.chorus.libdomgraph.EdgeData(name, de.saar.chorus.libdomgraph.EdgeType.DOMINANCE_EDGE );
        } else if( data.getType() == de.saar.chorus.leonardo.EdgeType.solid ) {
            return new de.saar.chorus.libdomgraph.EdgeData(name, de.saar.chorus.libdomgraph.EdgeType.TREE_EDGE );
        } else {
            System.err.println("Error: Edge data has wrong type!");
            return null;
        }
    }

    public DomGraph getGraph() {
        return solver.getGraph();
    }

    public NodeLabels getLabels() {
        return solver.getLabels();
    }
}
