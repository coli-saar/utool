package nl.rug.discomm.udr.taxonomy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import nl.rug.discomm.udr.taxonomy.RelationFeatures.Feature;

public class RelationTaxonymy {

	private static RelationTaxonymy instance;
	private RelationNode top;
	private Set<RelationNode> relationNodes;
	private HashMap<RelationNode, Set<RelationNode>> parentsToChildren;
	private String nl = System.getProperty("line.separator");
	
	
	public RelationTaxonymy() {
		relationNodes = new HashSet<RelationNode>();
		parentsToChildren = new HashMap<RelationNode, Set<RelationNode>>();
		
		RelationData topdata = new RelationData("top",null);
		for(Feature feature : RelationFeatures.getFeatures() ) {
			topdata.putFeature(feature, null);
		}
		top = new RelationNode(topdata);
		parentsToChildren.put(top, new HashSet<RelationNode>());
		relationNodes.add(top);
	}
	

	
	public void addRelation(RelationData data) {
	
		boolean placed = subsumptionDFS(top, data, new HashSet<RelationNode>());
		if(!placed) {
			// debug
			System.err.print("BŠh. :'( ");
		}
	}
	
	
	
	private boolean subsumptionDFS(RelationNode current, RelationData rel, Set<RelationNode> peer ) {
		int compare = current.compareToRelation(rel);
		
		if( compare == 0 && current.equalsRelation(rel) ) {
			// there is already a node sharing all the feat-val-pairs of
			// the new relation
			current.addRelation(rel);
			System.out.println("added to: " + current);
			return true;
		} else if( compare == 1 ) {
			// we arrive at a graph node which should be
			// some descendant of the new one. If we are 
			// here, there can't be a node sharing this relation's fv-pairs.
			Set<RelationNode> newchildren = new HashSet<RelationNode>();
			RelationNode newNode = new RelationNode(rel);
			parentsToChildren.put(newNode, newchildren);
			for(RelationNode sister : peer) {
				if(sister.compareToRelation(rel) == 1) {
					newchildren.add(sister);
				}
			}
			peer.removeAll(newchildren);
			peer.add(newNode);
			relationNodes.add(newNode);

			System.out.println("New Node: " + newNode);
			return true;
			
		} else if( compare == -1 ) {
			// we arrive at a graph node which should be
			// some ancestor of the new one. Go ahead.
			Set<RelationNode> sisters = parentsToChildren.get(current);
			for(RelationNode node : sisters) {
				if(subsumptionDFS(node, rel, sisters) ) {
					return true;
				}
			
			}
			
			// if we came here, the recent node is my direct parent
			// and the new Node will be a leaf.
			RelationNode newNode = new RelationNode(rel);
			System.out.println("New Node: " + newNode);
			parentsToChildren.put(newNode, new HashSet<RelationNode>());
			sisters.add(newNode);
			relationNodes.add(newNode);
			return true;
		} else {
			// we found some node which is not in some dominance relation to
			// the new one. No point in going ahead. ;)
			System.err.println(current);
			System.err.println(rel.getName());
			return false;
		}
		
	}
	
	public static RelationTaxonymy getInstance() {
		if(instance == null) {
			instance = new RelationTaxonymy();
		}
		
		return instance;
	}
	
	public String toString() {

		return parentsToChildren.toString();
		
	}
	
	public boolean isEmpty() {
		return relationNodes.isEmpty();
	}
	
	/**
	 * TODO debug me
	 * @param buffer
	 * @param current
	 */
	private void printDFS(StringBuffer buffer, RelationNode current) {
		boolean first = true;
		for(RelationNode n : parentsToChildren.get(current)) {
			if(first) {
				buffer.append("<ul>");
				first = false;
			}
			buffer.append("<li>" + n + "</li>" + nl);
			printDFS(buffer,n);
		}
		buffer.append("</ul>");
	}
	
}
