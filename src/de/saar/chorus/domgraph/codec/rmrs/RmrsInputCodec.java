package de.saar.chorus.domgraph.codec.rmrs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.EdgeData;
import de.saar.chorus.domgraph.graph.EdgeType;
import de.saar.chorus.domgraph.graph.NodeData;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.domgraph.graph.NodeType;

public class RmrsInputCodec {
    private final DomGraph graph;
    private final NodeLabels labels;

    private final Map<String,List<String>> children;

    private int gensymCounter;

    public RmrsInputCodec(DomGraph graph, NodeLabels labels) {
        super();
        this.graph = graph;
        this.labels = labels;

        children = new HashMap<String,List<String>>();

        gensymCounter = 1;
    }

    public void addEP(String x, String y, String p) {
        if( !graph.hasNode(x) ) {
            graph.addNode(x, new NodeData(NodeType.LABELLED));
            labels.addLabel(x, "&");
        }

        if( !graph.hasNode(y) ) {
            graph.addNode(y, new NodeData(NodeType.LABELLED));
        }

        graph.addEdge(x, y, new EdgeData(EdgeType.TREE));
        labels.addLabel(y, p);
    }

    public void addArgWithVar(String x, int i, String varname) {
        ensureMinArity(x, i);
        labels.addLabel(children.get(x).get(i), varname);
    }

    public void addArgWithHole(String x, int i, String holename) {
        ensureMinArity(x, i);
        children.get(x).remove(i);
        children.get(x).add(i, holename);
    }

    // qeq = dominance for now
    public void addQeq(String x, String y) {
        if( !graph.hasNode(x) ) {
            graph.addNode(x, new NodeData(NodeType.UNLABELLED));
        }

        if( !graph.hasNode(y) ) {
            graph.addNode(y, new NodeData(NodeType.LABELLED));
        }

        graph.addEdge(x, y, new EdgeData(EdgeType.DOMINANCE));
    }

    public void addVarInequality(String x, String y) {
        // ignored for now
    }

    public void addVarEquality(String x, String y) {
        // ignored for now
    }

    public void addNodeInequality(String x, String y) {
        // ignored for now
    }

    public void addNodeEquality(String x, String y) {
        // ignored for now
    }

    public void makeGraph() {
        for( String parent : children.keySet() ) {
            if( !graph.hasNode(parent) ) {
                graph.addNode(parent, new NodeData(NodeType.LABELLED));
            }

            for( String child : children.get(parent) ) {
                if( !graph.hasNode(child) ) {
                    graph.addNode(child, new NodeData(NodeType.LABELLED));
                }

                graph.addEdge(parent, child, new EdgeData(EdgeType.TREE));
            }
        }
    }


    // arity starts at 0
    private void ensureMinArity(String node, int arity) {
        List<String> c;

        if( children.containsKey(node)) {
            c = children.get(node);
        } else {
            c = new ArrayList<String>();
            children.put(node, c);
        }

        for( int i = c.size(); i <= arity; i++ ) {
            c.add(gensym());
        }
    }

    private String gensym() {
        return "gv" + (gensymCounter++);
    }
}
