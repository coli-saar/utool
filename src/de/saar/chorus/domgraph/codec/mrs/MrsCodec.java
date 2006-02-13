package de.saar.chorus.domgraph.codec.mrs;

import java.util.*;
import org._3pq.jgrapht.*;
import de.saar.chorus.domgraph.graph.*;

class MrsCodec {

    // maps variables to handles of non-quantifiers 
    private Map<String,Set<String>> bound;

    // maps variables to the handle of the corresponding quantifier
    private Map<String,String> binder;

    public enum Type {
	VARIABLE,
	HANDLE
    }

    private Map<String,Type> sig;

    private DomGraph graph;

    private NodeLabels labels;

    //
    //
    //
    
    public MrsCodec(DomGraph graph, NodeLabels labels)
    {
	this.graph = graph;
	this.labels = labels;
	this.sig = new TreeMap<String,Type>();
	this.binder = new TreeMap<String,String>();
	this.bound = new TreeMap<String,Set<String>>();
    }

    public void tellVariable(String name)
    {
	tell(name, Type.VARIABLE);
    }

    public void tellHandle(String name)
    {
	tell(name, Type.HANDLE);
    }

    private void tell(String name, Type type)
    {
	if (sig.containsKey(name)) {
	    // XXX -- throw new MalformedInputException() if sig(name) != type
	} else {
	    sig.put(name, type);
	}
    }

    public void addDomEdge(String source, String target)
    {
	addEdge(source, target, EdgeType.DOMINANCE);
    }

    public void addTreeEdge(String source, String target)
    {
	addEdge(source, target, EdgeType.TREE);
    }
    
    public void addEdge(String source, String target, EdgeType type)
    {
	if (!graph.hasNode(source))
	    graph.addNode(source, new NodeData(NodeType.UNLABELLED));

	if (!graph.hasNode(target))
	    graph.addNode(target, new NodeData(NodeType.UNLABELLED));

	graph.addEdge(source, target, new EdgeData(type));
    }
    
    public void addBindingEdges()
    {
	for (Map.Entry<String,Set<String>> entry : bound.entrySet()) {
	    String node1 = binder.get(entry.getKey());
	    
	    for (String node2 : entry.getValue()) {
		if (! graph.reachable(node1, node2)) {
		    addDomEdge(node1, graph.getRoot(node2));
		}
	    }
	}
    }
    
    void addQuantifier(String node, String label, Map<String,String> attrs)
    {
	if (!graph.hasNode(node)) {
	    graph.addNode(node, new NodeData(NodeType.LABELLED));
	} else {
	    // XXX -- throw an exception
	}

	String otherLabel = labels.getLabel(node);

	if (otherLabel == null) {
	    labels.addLabel(node, label);
	} else {
	    labels.addLabel(node, otherLabel + " & " + label);
	}

	addTreeEdge(node, attrs.remove("RSTR"));
	addTreeEdge(node, attrs.remove("BODY"));

	String var = attrs.remove("ARG0");

	if (binder.containsKey(var)) {
	    // XXX -- raise an exception
	} else {
	    binder.put(var, node);
	}

	// XXX -- check that attrs is now empty 
    }

    void addRelation(String node, String label, Map<String,String> attrs)
    {
	if (!graph.hasNode(node)) {
	    graph.addNode(node, new NodeData(NodeType.LABELLED));
	} else {
	    graph.getData(node).setType(NodeType.LABELLED);
	}

	String otherLabel = labels.getLabel(node);

	if (otherLabel == null) {
	    labels.addLabel(node, label);
	} else {
	    labels.addLabel(node, otherLabel + " & " + label);
	}

	for (Map.Entry<String,String> entry : attrs.entrySet()) {
	    String attr = entry.getKey();
	    String value = entry.getValue();

	    if (sig.containsKey(value)) {
		switch (sig.get(value)) {
		case VARIABLE:
		    Set<String> nodes = bound.get(value);
		    if (nodes == null) 
			bound.put(value, nodes = new TreeSet<String>());
		    nodes.add(node);
		    break;
		case HANDLE:
		    addTreeEdge(node, value);
		    break;
		}
	    }
	}
    }

    void setTopHandle(String topnode)
    {
	Set<String> top = graph.getFragment(topnode);
	Set<String> holes = graph.getHoles(top);
	Set<String> all = new HashSet<String>(graph.getAllNodes());

	all.removeAll(top);

	for (Set<String> wcc : graph.wccsOfSubgraph(all)) {
	    for (String node : wcc) {
		for (String parent : graph.getParents(node, EdgeType.DOMINANCE)) {

		    if (holes.contains(parent)) {

			for (String root : graph.getAllRoots(wcc)) {
			    if (graph.indeg(root, EdgeType.DOMINANCE) == 0) {
				addDomEdge(parent, root);
			    }
			}
		    }
		}
	    }
	}
    }

    void normalise()
    {
	for (String root : graph.getAllRoots()) {
	    List<Edge> edges = graph.getOutEdges(root, EdgeType.DOMINANCE);
	    
	    if (edges.size() > 0) {
		Set<String> holes = graph.getOpenHoles(root);
		
		if (holes.size() == 1) {
		    for (Edge edge : edges) {
			for (String hole : holes) {
			    addDomEdge(hole, (String) edge.getTarget());
			}
		    }
		} 

		for (Edge edge : edges) {
		    graph.remove(edge);
		}
	    }
	}
    }

    void finish()
    {
	normalise();
    }
}
