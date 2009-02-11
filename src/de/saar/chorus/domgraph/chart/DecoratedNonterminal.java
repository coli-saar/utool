package de.saar.chorus.domgraph.chart;

import java.util.Set;

public class DecoratedNonterminal<E extends GraphBasedNonterminal, DecorationType> implements GraphBasedNonterminal {
	private E base;
	private DecorationType decoration;
	
	

	public DecoratedNonterminal(E base, DecorationType decoration) {
		super();
		this.base = base;
		this.decoration = decoration;
	}
	
	

	public E getBase() {
		return base;
	}



	public DecorationType getDecoration() {
		return decoration;
	}

	public void addNode(String node) {
		base.addNode(node);
	}

	public Set<String> getNodes() {
		return base.getNodes();
	}

	@Override
	public String toString() {
		return "<" + base.toString() + "," + decoration + ">";
	}


	
	public String toString(Set<String> roots) {
		return "<" + base.toString(roots) + "," + decoration.toString() + ">";
	}

}
