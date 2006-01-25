package de.saar.chorus.libdomgraph.chart;

import de.saar.chorus.libdomgraph.FragmentSet;
import de.saar.chorus.libdomgraph.SWIGTYPE_p_Node;

public class AgendaEntry { //implements Map.Entry<SWIGTYPE_p_Node, FragmentSet>{
	
	SWIGTYPE_p_Node key;
	FragmentSet value;
	
	AgendaEntry(SWIGTYPE_p_Node source, FragmentSet target) {
		key = source;
		value = target;
	}
	
	
	/* (non-Javadoc)
	 * @see java.util.Map.Entry#getKey()
	 */
	public SWIGTYPE_p_Node getDominator() {
		// TODO Auto-generated method stub
		return key;
	}

	/* (non-Javadoc)
	 * @see java.util.Map.Entry#getValue()
	 */
	public FragmentSet getFragmentSet() {
		// TODO Auto-generated method stub
		return value;
	}

}
