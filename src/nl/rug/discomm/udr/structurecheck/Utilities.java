package nl.rug.discomm.udr.structurecheck;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Utilities {
	
	
	public static <E,T> void addToMapList(Map<E, List<T>> map, E key, T value) {
		List<T> values;
		if( map.containsKey(key) ) {
			values = map.get(key);
		} else {
			values = new ArrayList<T>();
			map.put(key, values);
		}
		values.add(value);
	}
	
	public static BigInteger catalanNumber(long n) {
		
		if(n == 0) {
			return BigInteger.ONE;
		}
		
		
		
		BigInteger ret = BigInteger.ONE;
		
		// note: bigN = 2 * first
		BigInteger bigN = BigInteger.valueOf(n);
		bigN = bigN.add(BigInteger.ONE);
		bigN = bigN.add(BigInteger.ONE);
		
	
		BigInteger N = BigInteger.valueOf(n);
		BigInteger divisor = BigInteger.ONE;
		
		for(BigInteger j = BigInteger.valueOf(2); j.compareTo(N) <= 0; j = j.add(BigInteger.ONE)) {
			
			ret = ret.multiply(bigN);
			bigN = bigN.add(BigInteger.ONE);
			divisor = divisor.multiply(j);
		}
		
		ret = ret.divide(divisor);

		
		
		return ret;
	}
	
	public static int[][] merge(int[][] first, int[][] second, int boundary) {
		
		int[][] ret = new int[first.length][];
		
		for(int i = 0; i< boundary; i++) {
			ret[i] = first[i].clone();
		}
		
		for(int i = boundary; i < ret.length; i++) {
			ret[i] = second[i].clone();
		}
		
		/*System.err.println("Root: " + ret[0][0]);
		for(int i = 1; i< ret.length; i++) {

			System.err.println(ret[i][0] + "<--- " + i + " --->" + ret[i][1]);
		}
		System.err.println();*/
		return ret;
		
	}
	
	
	/**
	 * Copied from http://forum.java.sun.com/thread.jspa?threadID=5160824&messageID=9612398
	 * 
	 * 
	 * @param array
	 * @return
	 */
	 public static int[][] cloneIntArrays(int[][] array)
	    {
	        // Create shallow clone
	        int[][] clonedArray = array.clone();
	        
	        // Deepen the shallow clone
	        for (int index = 0; index < clonedArray.length; index++)
	            clonedArray[index] = clonedArray[index].clone();
	        
	        return clonedArray;
	    }

	public static <T> Map<T, Double> addMapsDouble(Map<T, ? extends Double> first, Map<T, ? extends Double> second) {
		Map<T, Double> ret = new HashMap<T, Double>();
		
		Set<T> keys = first.keySet();
		
		for( T obj : keys ) {
			double f = 0;
			double s = 0;
			if( first.containsKey(obj) ) {
				if(! first.get(obj).isNaN() ) {
					f += first.get(obj);
				}
			}
			if( second.containsKey(obj) ) {
				if(! second.get(obj).isNaN()) {
					s += second.get(obj);
				}
			}
			ret.put(obj, f+s);
		}
		
		for( T obj : second.keySet() ) {
			if(! ret.containsKey(obj) ) {
				double f = 0;
				double s = 0;
				if( first.containsKey(obj) ) {
					if(! first.get(obj).isNaN() ) {
						f += first.get(obj);
					}
				}
				if( second.containsKey(obj) ) {
					if(! second.get(obj).isNaN()) {
						s += second.get(obj);
					}
				}
				ret.put(obj, f+s);
			}
		}
		
		return ret;
	}

	public static <T> void countUp(Map<T,Integer> counts, T obj) {
		if(counts.containsKey(obj)) {
			int old = counts.get(obj);
			old++;
			counts.remove(obj);
			counts.put(obj, old);
		} else {
			counts.put(obj,1);
		}
	}

}
