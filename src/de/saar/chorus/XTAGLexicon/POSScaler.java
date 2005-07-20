package de.saar.chorus.XTAGLexicon;

import java.util.*;

/**
 * Diese Klasse erstellt eine Map, die 
 * von den POS in syntax.xml auf die POS in
 * morphology.xml abbildet.
 * Ausnahme ist N: N kann in morphology.xml
 * sowohl Pron als auch N sein, 
 * deshalb muss darauf separat getestet werden.
 */

public class POSScaler {

    private Map<String,String> scaler;

    public POSScaler (){
	scaler = new HashMap<String,String>();
	scaler.put("V","V");
	scaler.put("A","A");
	scaler.put("Ad","Adv");
	scaler.put("P","Prep");
	scaler.put("P1","Prep");
	scaler.put("P2","Prep");
	scaler.put("P3","Prep");
	scaler.put("P4","Prep");
	scaler.put("D","Det");
    }

    public String map(String pos) {
	return scaler.get(pos);
    }

}

