package nl.rug.discomm.udr.structurecheck;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Utilities {

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
