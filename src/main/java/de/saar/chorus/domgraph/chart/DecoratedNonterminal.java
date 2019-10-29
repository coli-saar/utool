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
		return base.toString() + "/" + decoration;
	}


	
	public String toString(Set<String> roots) {
		return base.toString(roots) + "/" + decoration.toString();
	}



	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((base == null) ? 0 : base.hashCode());
		result = prime * result
				+ ((decoration == null) ? 0 : decoration.hashCode());
		return result;
	}



	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DecoratedNonterminal other = (DecoratedNonterminal) obj;
		if (base == null) {
			if (other.base != null)
				return false;
		} else if (!base.equals(other.base))
			return false;
		if (decoration == null) {
			if (other.decoration != null)
				return false;
		} else if (!decoration.equals(other.decoration))
			return false;
		return true;
	}

	
}
