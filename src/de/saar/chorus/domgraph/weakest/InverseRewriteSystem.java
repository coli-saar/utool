package de.saar.chorus.domgraph.weakest;

public class InverseRewriteSystem extends RewriteSystem {
	private RewriteSystem original;

	public InverseRewriteSystem(RewriteSystem original) {
		super();
		this.original = original;
	}
	
	public void addRule(String llabel, int lhole, String rlabel, int rhole,
			String annotation) {
		original.addRule(rlabel, rhole, llabel, lhole, annotation);
	}

	public boolean hasRule(String llabel, int lhole, String rlabel, int rhole,
			String annotation) {
		return original.hasRule(rlabel, rhole, llabel, lhole, annotation);
	}

	public String toString() {
		return original.toString();
	}
}
