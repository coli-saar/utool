package de.saar.chorus.domgraph.utool.server;

import de.saar.chorus.domgraph.equivalence.EquationSystem;
import de.saar.chorus.domgraph.weakest.Annotator;
import de.saar.chorus.domgraph.weakest.RewriteSystem;

public class RewriteSystemsCache {    
	private EquationSystem previousEquationSystem;
	private RewriteSystem previousRewriteSystem;
	private Annotator previousAnnotator;
	
	public RewriteSystemsCache() {
	}

	synchronized public EquationSystem getPreviousEquationSystem() {
		return previousEquationSystem;
	}

	synchronized public void setPreviousEquationSystem(EquationSystem previousEquationSystem) {
		this.previousEquationSystem = previousEquationSystem;
	}

	synchronized public RewriteSystem getPreviousRewriteSystem() {
		return previousRewriteSystem;
	}

	synchronized public void setPreviousRewriteSystem(RewriteSystem previousRewriteSystem) {
		this.previousRewriteSystem = previousRewriteSystem;
	}

	synchronized public Annotator getPreviousAnnotator() {
		return previousAnnotator;
	}

	synchronized public void setPreviousAnnotator(Annotator previousAnnotator) {
		this.previousAnnotator = previousAnnotator;
	}
	
	
}
