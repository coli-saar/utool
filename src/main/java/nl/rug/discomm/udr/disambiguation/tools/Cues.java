package nl.rug.discomm.udr.disambiguation.tools;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Cues {
	private static Map<String, Relation> markerToRelation;

	
	static {
		markerToRelation = new HashMap<String,Relation>();
	
		
		for(Relation rel : Relation.values()) {
			for(String marker : rel.getMarkers()) {
				markerToRelation.put(marker,rel);
			}
		}
		
	}
	
	
	// for convenience
	
	public static Relation getRelationForMarker(String cue) {
		return markerToRelation.get(cue);
	}
	
	public static Set<String> getDiscourseMarkers() {
		return markerToRelation.keySet();
	}
	
	
	public static List<Relation> getRelations() {
		return Arrays.asList(Relation.values());
	}
	
	public enum Relation {
		
		// taken from Simon C.-O. for English:
		PURPOSE {
			public Set<String> getMarkers() {
				Set<String> ret = new HashSet<String>();
				ret.add("in order to");
				ret.add("so that");
				return ret;
			}
			
			public String getName() {
				return "purpose";
			}
		}, 
		
		CONDITION {
			public Set<String> getMarkers() {
				Set<String> ret = new HashSet<String>();
				ret.add("if");
				ret.add("If");
				ret.add("Unless");
				ret.add("unless");
				ret.add("as long as");
				ret.add("As long as");
				return ret;
			}
			
			public String getName() {
				return "condition";
			}
		};
		
		public abstract Set<String> getMarkers();
		public abstract String getName();
	}

}
