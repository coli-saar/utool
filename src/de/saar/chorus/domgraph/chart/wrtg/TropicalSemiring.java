package de.saar.chorus.domgraph.chart.wrtg;

public class TropicalSemiring implements Semiring<Integer> {

	public boolean isInDomain(Integer element) {
	
		return true;
	}

	public Integer mult(Integer first, Integer second) {
		return first + second;
	}

	public Integer add(Integer first, Integer second) {
		return Math.min(first, second);
	}

	public Integer one() {
		return 0;
	}

	public Integer zero() {
		return Integer.MAX_VALUE;
	}
	
	public Integer getMinimum(Integer first, Integer second) {
		return Math.min(first, second);
	}
	
	
	public Integer getBestCost() {
		return 0;
	}
	
}
