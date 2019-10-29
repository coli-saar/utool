package de.saar.chorus.domgraph.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompactificationRecord {
	private Map<String,List<List<NodeChildPair>>> records;
	private DomGraph compact;
	
	public CompactificationRecord() {
		records = new HashMap<String, List<List<NodeChildPair>>>();
	}
	
	public void addRecord(String root, List<NodeChildPair> path) {
		List<List<NodeChildPair>> hereRecords = records.get(root);
		
		if( hereRecords == null ) {
			hereRecords = new ArrayList<List<NodeChildPair>>();
			records.put(root, hereRecords);
		}
		
		hereRecords.add(new ArrayList<NodeChildPair>(path));
	}
	
	public List<NodeChildPair> getRecord(String root, int hole) {
		return records.get(root).get(hole);
	}
	
	public int getNumberOfChildren(String root) {
		return records.get(root).size();
	}
	
	public void setCompactGraph(DomGraph compact) {
		this.compact = compact;
	}
	
	public DomGraph getCompactGraph() {
		return compact;
	}
	
	
	public static class NodeChildPair {
		public String node;
		public int childIndex;
		
		public NodeChildPair(String node, int childIndex) {
			super();
			this.node = node;
			this.childIndex = childIndex;
		}
		
		@Override
		public String toString() {
			return node + ":" + childIndex;
		}
	}
	
	@Override
	public String toString() {
		return records.toString();
	}
}
