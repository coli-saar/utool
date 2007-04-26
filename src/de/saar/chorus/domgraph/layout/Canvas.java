package de.saar.chorus.domgraph.layout;

public interface Canvas {
	public int getNodeWidth(String label);
	public int getNodeHeight(String label);
	
	public void drawNodeAt(int x, int y, String nodename, String label);
	
	public void drawTreeEdge(String src, String tgt);
	public void drawDominanceEdge(String src, String tgt);
}
