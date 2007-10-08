package nl.rug.discomm.udr.taxonomy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import nl.rug.discomm.udr.taxonomy.RelationFeatures.Feature;
import de.saar.chorus.domgraph.graph.NodeData;
import de.saar.chorus.domgraph.graph.NodeType;

public class RelationNode extends NodeData implements Comparable<NodeData> {


	
	private Map<Feature, Object> features;
	
	private Set<RelationData> relations;
	
	public RelationNode(RelationData first) {
		super(NodeType.LABELLED);
		features = new HashMap<Feature, Object>();
		first.copyFeatures(this);
		relations = new HashSet<RelationData>();
		relations.add(first);
	}
	
	public boolean addRelation(RelationData rel) {
		if(! equalsRelation(rel)) {
			return false;
		} else {
			return relations.add(rel);
		}
	}
	
	public boolean equalsRelation(RelationData rel) {
		if(relations.isEmpty()) {
			return false;
		} else {
			RelationData someData = relations.iterator().next();
			return someData.equals(rel);
		}
	}
	
	public int compareToRelation(RelationData rel) {
		if(relations.isEmpty()) {
			return 0;
		} else {
			RelationData someData = relations.iterator().next();
			return someData.compareTo(rel);
		}
	}
	
	
	void addAllFeatures(Map<Feature,Object> avm) {
		features.putAll(avm);
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
	
	
	
	public String toString() {
		StringBuffer ret = new StringBuffer();
		String nl = System.getProperty("line.separator");
		for(RelationData data : relations) {
			ret.append(data.getName() + nl);
		}
		
		return ret.toString();
	}


	public boolean equals(Object obj) {
		if(! (obj instanceof RelationNode) ) {
			return false;
		} else {
			RelationNode node = (RelationNode) obj;
			for(Feature feature : features.keySet()) {
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
	public int compareTo(NodeData arg0) {
		
		
		if(arg0 instanceof RelationNode) {
			RelationNode rn = (RelationNode) arg0;
			boolean ushere = false; //found a feature not specified here
			// but specified in the other node 

			boolean usother = false; // ...the other way round

			//TODO compare String features??
			for(Feature feature : features.keySet()) {
				if(features.get(feature) == null &&
						(! ushere ) ) {
					// feature is underspecified in this node
					if(rn.getValue(feature) != null) {
						//feature is specified in the other node
						if(usother) {
							// no relation subsumes the other one
							return 0;
						}
						ushere = true;
					}

				} else if(rn.getValue(feature) == null && (! usother)) {
					// feature is specified here but not in the other node
					if(ushere) {
						// no relation subsumes the other one
						return 0;
					}
					usother = true;
				}
			}
			if(ushere == usother) {
				return 0;
			} else if(ushere) {
				return -1;
			} else {
				return 1;
			}
		} else {
			return 0;
		}
	} 

}
