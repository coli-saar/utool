package de.saar.chorus.domgraph.chart;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.saar.chorus.domgraph.graph.DomEdge;

public class SolvedFormSpec {
	private List<DomEdge> domedges;
	private Map<String,String> subst; // maps holes to roots with which they must be plugged directly
	
	public SolvedFormSpec(List<DomEdge> domedges, Map<String,String> subst) {
		super();
		
		this.domedges = domedges;
		this.subst = subst;
	}
	
	public SolvedFormSpec() {
		domedges = new ArrayList<DomEdge>();
		subst = new HashMap<String, String>();
	}

	public List<DomEdge> getDomEdges() {
		return domedges;
	}

	public Map<String,String> getSubstitution() {
		return subst;
	}
	
	/*
	public void addDomEdge(DomEdge edge) {
		domedges.add(edge);
	}
	*/
	
	public void addAllDomEdges(Collection<DomEdge> edges) {
		domedges.addAll(edges);
	}
	
	/*
	public void setSubstitution(Map<String, String> subst) {
		this.subst = subst;
	}
	*/

	public void addSubstitution(Map<String, String> substitution) {
		this.subst.putAll(substitution);
	}
	
	public String toString() {
		return "(domedges: " + domedges + ", subst=" + subst + ")";
	}
	
}
