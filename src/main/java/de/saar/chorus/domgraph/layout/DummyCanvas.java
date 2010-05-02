package de.saar.chorus.domgraph.layout;

import de.saar.chorus.domgraph.graph.NodeData;

public class DummyCanvas implements Canvas {
	public void drawDominanceEdge(String src, String tgt) {
		// TODO Auto-generated method stub
		
	}

	public void drawLightDominanceEdge(String src, String tgt) {
		// TODO Auto-generated method stub
		
	}

	public void drawNodeAt(int x, int y, String nodename, String label, NodeData data, String show) {
		// TODO Auto-generated method stub
		
	}

	public void drawTreeEdge(String src, String tgt) {
		// TODO Auto-generated method stub
		
	}

	public int getNodeHeight(String label) {
		return 10;
	}

	public int getNodeWidth(String label) {
		return 10*label.length();
	}

}
