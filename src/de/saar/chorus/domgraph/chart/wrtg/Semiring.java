package de.saar.chorus.domgraph.chart.wrtg;

public interface Semiring<E extends Comparable<E>> {
		public E add(E first, E second);
		public E mult(E first, E second);
		public E zero();
		public E one();
		
		
		public boolean isInDomain(E element);
		
}
