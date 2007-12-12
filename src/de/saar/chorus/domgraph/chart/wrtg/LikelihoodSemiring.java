package de.saar.chorus.domgraph.chart.wrtg;

public class LikelihoodSemiring implements Semiring<Double> {

	public boolean isInDomain(Double element) {
		return (0 <= element) && (element <= 1.0);
	}

	public Double mult(Double first, Double second) {
		return first*second;
	}

	public Double add(Double first, Double second) {
		return first + second;
	}

	public Double one() {
		return 1.0;
	}

	public Double zero() {
		return 0.0;
	}
	
	public Double getMinimum(Double first, Double second) {
		return Math.min(first, second);
	}
	
	public Double getBestCost() {
		return 1.0;
	}

}
