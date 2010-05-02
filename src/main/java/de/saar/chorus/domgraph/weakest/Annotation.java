package de.saar.chorus.domgraph.weakest;

public class Annotation {
	private String previousNode;
	private String annotation;
	
	public Annotation(String previousNode, String annotation) {
		super();
		this.previousNode = previousNode;
		this.annotation = annotation;
	}
	
	public String getPreviousNode() {
		return previousNode;
	}
	
	public String getAnnotation() {
		return annotation;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((annotation == null) ? 0 : annotation.hashCode());
		result = prime * result
				+ ((previousNode == null) ? 0 : previousNode.hashCode());
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
		Annotation other = (Annotation) obj;
		if (annotation == null) {
			if (other.annotation != null)
				return false;
		} else if (!annotation.equals(other.annotation))
			return false;
		if (previousNode == null) {
			if (other.previousNode != null)
				return false;
		} else if (!previousNode.equals(other.previousNode))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return previousNode + ":" + annotation;
	}
}
