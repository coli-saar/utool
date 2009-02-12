package de.saar.chorus.domgraph.weakest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Annotator {
	private Map<String,Map<String,List<String>>> rules;
	private String start;
	
	public Annotator() {
		rules = new HashMap<String, Map<String,List<String>>>();
		this.start = null;
	}
	
	public void setStart(String start) {
		this.start = start;
	}
	
	public String getStart() {
		return start;
	}
	
	public void addRule(String parentAnnotation, String label, String childAnnotation) {
		Map<String,List<String>> rulesHere = rules.get(parentAnnotation);
		if( rulesHere == null ) {
			rulesHere = new HashMap<String, List<String>>();
			rules.put(parentAnnotation,rulesHere);
		}
		
		List<String> annotationsHere = rulesHere.get(label);
		if( annotationsHere == null ) {
			annotationsHere = new ArrayList<String>();
			rulesHere.put(label, annotationsHere);
		}
		
		annotationsHere.add(childAnnotation);
	}
	
	public String getChildAnnotation(String parentAnnotation, String label, int hole) {
		assert rules.get(parentAnnotation) != null : "par " + parentAnnotation + "/" + label + "/" + hole;
		assert rules.get(parentAnnotation).get(label) != null : "lab " + parentAnnotation + "/" + label + "/" + hole;
		//assert rules.get(parentAnnotation).get(label).get(hole) != null : "hole " + parentAnnotation + "/" + label + "/" + hole;
		
		try {
			return rules.get(parentAnnotation).get(label).get(hole);
		} catch(Throwable e) {
			System.err.println(e);
			System.err.println("par " + parentAnnotation + "/" + label + "/" + hole);
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public String toString() {
		return rules.toString();
	}
}
