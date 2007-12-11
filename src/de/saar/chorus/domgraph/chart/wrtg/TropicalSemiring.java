package de.saar.chorus.domgraph.chart.wrtg;

public class TropicalSemiring implements Semiring<Integer> {

	public boolean isInDomain(Integer element) {
	
		return true;
	}

	public Integer semiringProduct(Integer first, Integer second) {
		return first + second;
	}

	public Integer semiringSum(Integer first, Integer second) {
		return Math.min(first, second);
	}

	public Integer getProductIdentityElement() {
		return 0;
	}

	public Integer getSumIdentityElement() {
		return Integer.MAX_VALUE;
	}
	
	public Integer getMinimum(Integer first, Integer second) {
		return Math.min(first, second);
	}
	
	public int compare(Integer first, Integer second) {
		return first.compareTo(second);
	}
	public Integer getBestCost() {
		return 0;
	}
	
}
