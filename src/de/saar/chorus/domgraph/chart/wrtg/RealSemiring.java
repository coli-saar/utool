package de.saar.chorus.domgraph.chart.wrtg;

public class RealSemiring implements Semiring<Double> {

	public Double add(Double first, Double second) {
		// TODO Auto-generated method stub
		return first + second;
	}

	public boolean isInDomain(Double element) {
		// TODO Auto-generated method stub
		return true;
	}

	public Double mult(Double first, Double second) {
		// TODO Auto-generated method stub
		return first * second;
	}

	public Double one() {
		// TODO Auto-generated method stub
		return 1.0;
	}
	
	public Double maxElement() {
		return Double.MAX_VALUE;
	}

	public Double zero() {
		// TODO Auto-generated method stub
		return 0.0;
	}

}
