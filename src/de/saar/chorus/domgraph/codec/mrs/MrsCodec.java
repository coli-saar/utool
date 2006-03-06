
package de.saar.chorus.domgraph.codec.mrs;

import java.util.Set;
import java.util.Map;
import java.util.Collection;
import java.util.TreeSet;
import java.util.TreeMap;

import org._3pq.jgrapht.Edge;

import de.saar.chorus.domgraph.codec.MalformedDomgraphException;

import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.EdgeType;
import de.saar.chorus.domgraph.graph.EdgeData;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.domgraph.graph.NodeType;
import de.saar.chorus.domgraph.graph.NodeData;

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
	
	public void tellVariable(String name) throws MalformedDomgraphException
	{
		tell(name, Type.VARIABLE);
	}
	
	public void tellHandle(String name) throws MalformedDomgraphException
	{
		tell(name, Type.HANDLE);
	}
	
	private void tell(String name, Type type) throws MalformedDomgraphException
	{
		if (sig.containsKey(name) && sig.get(name) != type)
			throw new MalformedDomgraphException("Inconsistent use of " + name, ErrorCodes.NOT_WELLFORMED);
		sig.put(name, type);
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
	
	public void addNode(String node)
	{
		if (!graph.hasNode(node))
			graph.addNode(node, new NodeData(NodeType.UNLABELLED));	
	}
	
	public void addNode(String node, String label)
	{
		if (graph.hasNode(node)) {
			NodeData data = graph.getData(node);
			
			if (data.getType() == NodeType.LABELLED) {
				labels.addLabel(node, labels.getLabel(node) + "&" + label);
			} else {
				data.setType(NodeType.LABELLED);
				labels.addLabel(node, label);
			}
		} else {
			graph.addNode(node, new NodeData(NodeType.LABELLED));	
			labels.addLabel(node, label);
		}
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
	
	void addQuantifier(String node, String label, Map<String,String> attrs) throws MalformedDomgraphException
	{
		addNode(node, label);
		
		addTreeEdge(node, attrs.remove("RSTR"));
		addTreeEdge(node, attrs.remove("BODY"));
		
		String var = attrs.remove("ARG0");
		
		if (binder.containsKey(var))
			throw new MalformedDomgraphException("Variable " + var + " is used by distinct quantifiers", ErrorCodes.NOT_WELLFORMED);
		
		binder.put(var, node);
		
		if (attrs.size() > 0) 
			throw new MalformedDomgraphException("Illegal quantifier syntax", ErrorCodes.NOT_WELLFORMED);
	}
	
	void addRelation(String node, String label, Map<String,String> attrs)
	{
		addNode(node, label);
		
		for (Map.Entry<String,String> entry : attrs.entrySet()) {
			String attr = entry.getKey();
			String value = entry.getValue();
			
			if (!ignore(attr) && sig.containsKey(value)) {
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
		Collection<String> holes = graph.getHoles(top);
		Set<String> withoutTop = new TreeSet<String>(graph.getAllNodes());
		
		withoutTop.removeAll(top);
		
		for (Set<String> wcc : graph.wccs(withoutTop)) {
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
		for (String root : withoutTop) {
			if (graph.indeg(root) == 0) {
				addDomEdge(topnode, root);
			}
		}
	}
	
	void normalise()
	{
		for (String root : graph.getAllRoots()) {
			Collection<Edge> edges = graph.getOutEdges(root, EdgeType.DOMINANCE);
			
			if (edges.size() > 0) {
				Collection<String> holes = graph.getOpenHoles(root);
				
				if (holes.size() == 1) {
					for (Edge edge : edges) {
						for (String hole : holes) {
							addDomEdge(hole, (String) edge.getTarget());
						}
					}
								
					for (Edge edge : edges) {
						graph.remove(edge);
					}
				}
			}
		}
	}
	
	void setTopHandleAndFinish(String handle) throws MalformedDomgraphException
	{
		StringBuffer errorText = new StringBuffer();
			
		this.addBindingEdges();
		this.setTopHandle(handle);
		this.normalise();
		
		int errorCode = 0;
		
		if (! graph.isNormal()) {
			errorCode |= ErrorCodes.NOT_NORMAL;
			errorText.append("The graph is not normal.\n");
		}
		if (! graph.isLeafLabelled()) {
			errorCode |= ErrorCodes.NOT_LEAF_LABELLED;
			errorText.append("The graph is not leaf-labelled.\n");
		}
		if (! graph.isHypernormallyConnected()) {
			errorCode |= ErrorCodes.NOT_HYPERNORMALLY_CONNECTED;
			errorText.append("The graph is not hypernormally connected.\n");
		}
		if (errorCode != 0)
			throw new MalformedDomgraphException(errorText.toString(), errorCode);
	}
	
	boolean ignore(String attr)
	{
		return attr.equals("TPC") || attr.equals("PSV");
	}
}
