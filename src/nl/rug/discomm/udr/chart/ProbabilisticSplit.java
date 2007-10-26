package nl.rug.discomm.udr.chart;

import java.util.Set;

import de.saar.chorus.domgraph.chart.Split;

public class ProbabilisticSplit extends Split {
	
	private double likelyhood;
	
	public ProbabilisticSplit(String root) {
		super(root);
		likelyhood = 1.0;
	}
	
	public ProbabilisticSplit(Split split) {
		super(split.getRootFragment());
		for(String hole : split.getAllDominators()) {
			for(Set<String> wcc : split.getWccs(hole)) {
				addWcc(hole, wcc);
			}
		}
		likelyhood = 1.0;
	}
	
	public ProbabilisticSplit(String root, double likelyhood) {
		super(root);
		this.likelyhood = likelyhood;
	}

	public double getLikelyhood() {
		return likelyhood;
	}

	public void setLikelyhood(double likelyhood) {
		this.likelyhood = likelyhood;
	}
	
	
}
