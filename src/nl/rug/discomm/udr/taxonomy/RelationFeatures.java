package nl.rug.discomm.udr.taxonomy;

import java.util.HashSet;
import java.util.Set;

/**
 * TODO Think another time about appropriateness of this.
 * (Maybe it should be a flexible XML file.)
 * 
 * @author Michaela Regneri
 *
 */
public class RelationFeatures {

	
	private static Set<Feature> features = new HashSet<Feature>();
	
	public static Set<Feature> getFeatures() {
		return features;
	}
	
	public static void addFeature(String name, ValueType ret) {
		features.add(new Feature(name,ret));
	}
	
	public enum RelationType {
		RST, SDRT;
	}
	
	public enum ValueType {
		BOOLEAN, STRING;
	}
	
	public static class Feature {
		private String name;
		private ValueType ret;
		
		public Feature(String n, ValueType v) {
			name = n;
			ret = v;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public ValueType getRet() {
			return ret;
		}

		public void setRet(ValueType ret) {
			this.ret = ret;
		}
		
		
	}
}
