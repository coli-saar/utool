package nl.rug.discomm.udr.chart;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import de.saar.chorus.domgraph.chart.GraphBasedNonterminal;

public class IntegerNonterminal extends ArrayList<Integer> implements GraphBasedNonterminal {

	private int leafIndex;
	
	public IntegerNonterminal(int left, int right) {
		super();
		add(left);
		add(right);
		leafIndex = -1;
	}
	
	public IntegerNonterminal( int leafIndex) {
		super();
		add(0);
		add(0);
		this.leafIndex = leafIndex;
		}
	
	public void addNode(String node) {
		
		String formatted = node.replaceAll("\\D", "");
		if(formatted.length() > 0) {
			int nodeindex = Integer.parseInt(formatted);
			if(nodeindex < get(0)) {
				set(0, nodeindex);
			} else if(nodeindex > get(1)) {
				set(1, nodeindex);
			}
		}
	}
	
	public int getLeftBorder() {
		return get(0);
	}
	
	public int getRightBorder() {
		return get(1);
	}
	private Set<String> convertToSubgraph() {
		Set<String> ret = new HashSet<String>();
		int left = get(0), right = get(1);
		ret.add( (left-1) + "y");
		for(int i = left; i <= right; i++) {
			ret.add(i + "x");
			ret.add(i + "xl");
			ret.add(i + "xr");
			ret.add(i + "y");
		}
		
		return ret;
	}

	public Set<String> getNodes() {
		return convertToSubgraph();
	}

	public String getRootIfSingleton() {
		// TODO Auto-generated method stub
		return leafIndex + "y";
	}

	public boolean isSingleton(Set<String> roots) {
		//TODO should this only return true for leaves?
		// there are no leaves in my chain charts...
		return leafIndex > -1;
	}

	public String toString(Set<String> roots) {
		// TODO Auto-generated method stub
		return "Subchain [" + get(0) + " - " + get(1) + "]";
	}

}
