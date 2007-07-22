/*
 * @(#)JTagTree.java created 25.08.2005
 * 
 * Copyright (c) 2005 Alexander Koller
 *  
 */

package de.saar.chorus.tag.viewer;

import java.awt.Color;

import org.jgraph.graph.AttributeMap;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.GraphModel;
import org.jgraph.util.JGraphUtilities;

import de.saar.chorus.XTAGLexicon.AnchorNode;
import de.saar.chorus.XTAGLexicon.FootNode;
import de.saar.chorus.XTAGLexicon.InnerNode;
import de.saar.chorus.XTAGLexicon.Node;
import de.saar.chorus.XTAGLexicon.SubstitutionNode;
import de.saar.chorus.XTAGLexicon.TerminalNode;
import de.saar.chorus.XTAGLexicon.Tree;
import de.saar.chorus.jgraph.improvedjgraph.ImprovedJGraph;
import de.saar.chorus.jgraph.improvedjgraph.layout.treelayout.GecodeTreeLayout;

public class JTagTree extends ImprovedJGraph<NodeType,NodeData,Object,EdgeData> {
    public JTagTree(Tree tree) {
        super();
        
        setName(tree.getName());
        
        extractNodesAndEdges(tree.getRoot());
        computeAdjacency();
    }
    
    private DefaultGraphCell extractNodesAndEdges(Node node) {
        String name = node.getName();
        NodeType type;
        NodeData data;
        DefaultGraphCell ret;
        
        if( node instanceof InnerNode ) {
            type = NodeType.internal;
        } else if( node instanceof AnchorNode ) {
            type = NodeType.anchor;
        } else if( node instanceof TerminalNode ) {
            type = NodeType.terminal;
            name = node.getCat();
            if( name.equals("")) {
                name = "(eps)";
            }
        } else if( node instanceof SubstitutionNode ) {
            type = NodeType.subst;
        } else if( node instanceof FootNode ) {
            type = NodeType.foot;
        } else {
            System.err.println("Undefined node type: " + node);
            type = null;
        }
        
        data = new NodeData(type, name);
        ret = addNode(name, data);
        
        if( type == NodeType.internal ) {
            for( Node child : node.getChildren() ) {
                addEdge(new EdgeData(), ret, extractNodesAndEdges(child));
            }
        }
        
        return ret;
    }
    
    protected AttributeMap defaultNodeAttributes(NodeType type) {
        GraphModel model = getModel();
        AttributeMap map = new AttributeMap();

        switch(type) {
        case anchor:
            GraphConstants.setForeground(map, Color.blue);
            break;
        case terminal:
            GraphConstants.setForeground(map, Color.cyan);
            break;
        case subst:
            GraphConstants.setForeground(map, Color.red);
            break;
        case foot:
            GraphConstants.setForeground(map, Color.green);
            break;
        case internal:
            GraphConstants.setForeground(map, Color.black);
            break;
        }
        

        
        GraphConstants.setBounds(map, map.createRect(0, 0, 30, 30));
        
        GraphConstants.setBackground(map, Color.white);
        GraphConstants.setFont(map, nodeFont);
        GraphConstants.setOpaque(map, true);


        return map;
    }

    protected AttributeMap defaultEdgeAttributes(Object type) {
        AttributeMap solidEdge = new AttributeMap();
        GraphConstants.setLineEnd(solidEdge, GraphConstants.ARROW_NONE);
        GraphConstants.setEndSize(solidEdge, 10);
        GraphConstants.setLineWidth(solidEdge, 1.7f);
        return solidEdge;
    }

    public void computeLayout() {
        JGraphUtilities.applyLayout(this, new GecodeTreeLayout(this));
    }
    
    

}
