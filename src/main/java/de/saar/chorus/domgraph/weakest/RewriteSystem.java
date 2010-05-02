package de.saar.chorus.domgraph.weakest;

import java.util.HashSet;
import java.util.Set;

public class RewriteSystem {
	private Set<String> rules;
	
	public RewriteSystem() {
		rules = new HashSet<String>();
	}
	
	public void addRule(String llabel, int lhole, String rlabel, int rhole, String annotation) {
		rules.add(constructKey(llabel, lhole, rlabel, rhole, annotation));
	}
	
	public boolean hasRule(String llabel, int lhole, String rlabel, int rhole, String annotation) {
		return rules.contains(constructKey(llabel, lhole, rlabel, rhole, annotation));
	}
	
	private String constructKey(String llabel, int lhole, String rlabel, int rhole, String annotation) {
		return llabel + "/" + lhole + "-" + rlabel + "/" + rhole + "_" + annotation;
	}
}
