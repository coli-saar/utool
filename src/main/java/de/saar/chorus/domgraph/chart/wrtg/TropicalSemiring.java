package de.saar.chorus.domgraph.chart.wrtg;

public class TropicalSemiring implements Semiring<Double> {

	public boolean isInDomain(Double element) {
		return true;
	}

	public Double mult(Double first, Double second) {
		return first + second;
	}

	public Double add(Double first, Double second) {
		return Math.min(first, second);
	}

	public Double one() {
		return 0.0;
	}

	public Double zero() {
		return Double.MAX_VALUE;
	}
	
	public Double maxElement() {
		return 0.0;
	}
	
}
