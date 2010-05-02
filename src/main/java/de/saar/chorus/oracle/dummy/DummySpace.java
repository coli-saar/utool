

package de.saar.chorus.oracle.dummy;

import java.util.LinkedList;

import de.saar.chorus.oracle.EvaluatingSearchSpace;

class DummySpace extends EvaluatingSearchSpace<String> {
	private LinkedList<String> stateQueue;
	
	DummySpace(DummyEvaluator eval) {
		super(eval);
		
		stateQueue = new LinkedList<String>();
	}
	
	public void addState(String id, String contents, String parentId) {
		super.addState(id, contents, parentId);
		
		stateQueue.addLast(id);
	}
	
	public String chooseGlobal() {
		return stateQueue.removeFirst();
	}
}


