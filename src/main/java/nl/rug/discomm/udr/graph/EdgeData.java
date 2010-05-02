package nl.rug.discomm.udr.graph;

import de.saar.chorus.domgraph.graph.EdgeType;

public class EdgeData extends de.saar.chorus.domgraph.graph.EdgeData {
	
	private double weight;
	private int index;
	
	public EdgeData(double weight, int index) {
		super(EdgeType.DOMINANCE);
		this.weight = weight;
		this.index = index;
	}
	
	public EdgeData(EdgeType type, int index) {
		super(type);
		weight = 1.0;
		this.index = index;
	}
	
	
	public int getIndex() {
		return index;
	}
	
	public double getWeight() {
		return weight;
	}
}
