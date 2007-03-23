package de.saar.chorus.domgraph.chart;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.saar.chorus.domgraph.graph.DomEdge;
import de.saar.chorus.term.Substitution;

public class SolvedFormSpec {
	private List<DomEdge> domedges;
	private Substitution subst;
	
	public SolvedFormSpec(List<DomEdge> domedges, Substitution subst) {
		super();
		
		this.domedges = domedges;
		this.subst = subst;
	}
	
	public SolvedFormSpec() {
		domedges = new ArrayList<DomEdge>();
		subst = null;
	}

	public List<DomEdge> getDomEdges() {
		return domedges;
	}

	public Substitution getSubstitution() {
		return subst;
	}
	
	public void addDomEdge(DomEdge edge) {
		domedges.add(edge);
	}
	
	public void addAllDomEdges(Collection<DomEdge> edges) {
		domedges.addAll(edges);
	}
	
	public void setSubstitution(Substitution subst) {
		this.subst = subst;
	}
	
}
