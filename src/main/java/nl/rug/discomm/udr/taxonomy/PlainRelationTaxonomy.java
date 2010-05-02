package nl.rug.discomm.udr.taxonomy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PlainRelationTaxonomy {

	private static final String TOP = "top";
	
	private Map<String, Set<String>> parentsToChildren;
	private Map<String, Set<String>> childrenToParents;
	
	public PlainRelationTaxonomy() {
		parentsToChildren = new HashMap<String, Set<String>>();
		childrenToParents = new HashMap<String, Set<String>>();
		
		
		parentsToChildren.put(TOP, new HashSet<String>());
	}
	
	public void addRelation(String name, Set<String> parents, Set<String> children) {
		if(parents.isEmpty()) {
			// toplevel relation
			addAsChild(name, TOP, children);
		} else {
			for(String father : parents) {
				addAsChild(name, father, children);
			}
		}
		
		if(! parentsToChildren.containsKey(name) ) {
			parentsToChildren.put(name, children);
		}
	}
	
	private void addAsChild(String child, String father, Set<String> children) {
		
		
		if(! parentsToChildren.containsKey(father)) {
			System.err.println("Father not there? Adding father under top.");
			addAsChild(father, TOP, new HashSet<String>());
			parentsToChildren.put(father, new HashSet<String>());
			
			/*Set<String> set = new HashSet<String>();
			set.add(TOP);
			childrenToParents.put(father, set);*/
		}
		
		Set<String> sisters = parentsToChildren.get(father);
		
		if(! sisters.contains(child)) {
			sisters.add(child);
		} 
		
		
		sisters.removeAll(children);
		
	}
	
	private String getIndent(String sign, int count) {
		String ret = "";
		for(int i = 1; i <= count; i++) {
			ret += sign;
		}
		return ret;
	}
	
	public String toString() {
		StringBuffer ret = new StringBuffer();
		
		ret.append(recString(TOP, 0));
		
		return ret.toString();
	}
	private StringBuffer recString(String parent, int depth) {
		
		StringBuffer ret = new StringBuffer();
		
		ret.append(getIndent("*", depth));
		ret.append(" " + parent);
	
		
		if( parentsToChildren.containsKey(parent)) {
			ret.append(System.getProperty("line.separator"));
			for(String child : parentsToChildren.get(parent) ) {
				ret.append(recString(child, depth +1));
				ret.append(System.getProperty("line.separator"));
			}
		} else {
			// assume a leaf
			ret.append("     |||");
		}
		
		ret.append(System.getProperty("line.separator"));
		return ret;
	}
	
	
	
	public boolean isToplevelRelation(String relation) {
		return parentsToChildren.get(TOP).contains(relation);
	}
	
	
	public boolean subsumes(String upper, String lower) {
		return recSubsumes(upper, lower, new HashSet<String>());
	}
	
	private boolean recSubsumes(String current, String lower, Set<String> visited) {
		if(current.equals(lower)) {
			// a relation subsumes itself
			return true;
		}
		if(! visited.contains(current) ) {
			visited.add(current);
			
			if(! parentsToChildren.containsKey(current)) {
				return false;
			}
			
			Set<String> children = parentsToChildren.get(current);
			if(children.contains(lower)) {
				return true;
			}
			for(String child : children) {
				if(recSubsumes(child, lower, visited)) {
					return true;
				}
			}
			return false;
			
		}
		return false;
	}
	
	
	
	
}
