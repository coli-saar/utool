package de.saar.chorus.domgraph.chart.wrtg;

public class LikelihoodSemiring implements Semiring<Double> {

	public boolean isInDomain(Double element) {
		return (0 <= element) && (element <= 1.0);
	}

	public Double semiringProduct(Double first, Double second) {
		return first*second;
	}

	public Double semiringSum(Double first, Double second) {
		return first + second;
	}

	public Double getProductIdentityElement() {
		return 1.0;
	}

	public Double getSumIdentityElement() {
		return 0.0;
	}
	
	public Double getMinimum(Double first, Double second) {
		return Math.min(first, second);
	}
	
	public int compare(Double first, Double second) {
		return first.compareTo(second);
	}
	public Double getBestCost() {
		return 1.0;
	}

}
