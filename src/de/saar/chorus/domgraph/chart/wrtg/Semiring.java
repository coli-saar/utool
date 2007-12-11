package de.saar.chorus.domgraph.chart.wrtg;

public interface Semiring<E> {
		public E semiringSum(E first, E second);
		public E semiringProduct(E first, E second);
		public E getSumIdentityElement();
		public E getProductIdentityElement();
		public E getMinimum(E first, E second);
		public int compare(E first, E second);
		public boolean isInDomain(E element);
		public E getBestCost();
}
