package nl.rug.discomm.udr.taxonomy;

import java.util.HashMap;
import java.util.Map;

import nl.rug.discomm.udr.taxonomy.RelationFeatures.Feature;
import nl.rug.discomm.udr.taxonomy.RelationFeatures.RelationType;

public class RelationData implements Comparable<RelationData> {


	
	private Map<Feature, Object> features;
	
	private String name;
	private RelationType type;
	
	public RelationData(String name, RelationType t) {
		this.name = name;
		type = t;
		features = new HashMap<Feature,Object>();
	}
	
	public RelationType getType() {
		return type;
	}
	
	public void putFeature(Feature feature, Object val) {
		features.put(feature,val);
	}
	
	public boolean containsFeature(Feature feature) {
		return features.containsKey(feature);
	}
	
	public Object getValue(Feature feature) {
		if(! containsFeature(feature)) {
			return null;
		} 
		
		return features.get(feature);
	}
	
	public String getName() {
		return name;
	}



	public void setName(String name) {
		this.name = name;
	}
	
	void copyFeatures(RelationNode node) {
		node.addAllFeatures(features);
	}

	public boolean equals(Object obj) {
		if(! (obj instanceof RelationData) ) {
			return false;
		} else {
			RelationData node = (RelationData) obj;
			for(Feature feature : features.keySet()) {
				if( (node.getValue(feature) == null) &&
					(features.get(feature) == null) ) {
					continue;
				} else if( (node.getValue(feature) == null) ||
					(features.get(feature) == null) ) {
					return false;
				}
				
				if(! node.getValue(feature).equals(
						features.get(feature)) ) {
					return false;
				}
			}

			return true;
		}
	}

	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return super.hashCode();
	}

	/**
	 * This checks whether this <code>RelationNode</code> node is 
	 * <i>subsumed</i> by the given other <code>RelationNode</code>
	 * or the other way round.
	 * If this node is the more specific one, this returns 1.
	 * If the other node is the more specific one, this returns -1.
	 * 0 is returned if the nodes are equally specific or if there is
	 * no direct subsumption relation.
	 */
	public int compareTo(RelationData arg0) {
		boolean ushere = false; //found a feature not specified here
								// but specified in the other node 
		
		boolean usother = false; // ...the other way round
		
		//TODO compare String features??
		for(Feature feature : features.keySet()) {
			Object myval = features.get(feature);
			Object otherval = arg0.getValue(feature);
			
			if(myval == null ) {
				// feature is underspecified in this node
				if(otherval != null) {
					ushere = true;
				}
				
			} else if(otherval == null ) {
				// feature is specified here but not in the other node
				usother = true;
			} else if(! myval.equals(otherval)) {
				return 0;
			}
		}
		if(ushere == usother) {
			return 0;
		} else if(ushere) {
			System.out.println(arg0.name + " is subsumed by "  + name);
			return -1;
		} else {
			System.out.println(arg0.name + " is subsumed by "  + name);
			return 1;
		}
	}
}
