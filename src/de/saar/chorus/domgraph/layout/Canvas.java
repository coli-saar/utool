package de.saar.chorus.domgraph.layout;

import de.saar.chorus.domgraph.graph.NodeData;

public interface Canvas {
	
	public int getNodeWidth(String label);
	public int getNodeHeight(String label);
	
	public void drawNodeAt(int x, int y, String nodename, String label, NodeData data, String show);
	
	public void drawTreeEdge(String src, String tgt);
	public void drawDominanceEdge(String src, String tgt);
	public void drawLightDominanceEdge(String src, String tgt);
}
