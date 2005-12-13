package de.saar.chorus.libdomgraph.chart;

import java.util.Map;

import de.saar.chorus.libdomgraph.FragmentSet;
import de.saar.chorus.libdomgraph.SWIGTYPE_p_Node;

public class AgendaEntry implements Map.Entry<SWIGTYPE_p_Node, FragmentSet>{
	
	SWIGTYPE_p_Node key;
	FragmentSet value;
	
	AgendaEntry(SWIGTYPE_p_Node source, FragmentSet target) {
		key = source;
		value = target;
	}
	
	
	/* (non-Javadoc)
	 * @see java.util.Map.Entry#getKey()
	 */
	public SWIGTYPE_p_Node getKey() {
		// TODO Auto-generated method stub
		return key;
	}

	/* (non-Javadoc)
	 * @see java.util.Map.Entry#getValue()
	 */
	public FragmentSet getValue() {
		// TODO Auto-generated method stub
		return value;
	}

	/* (non-Javadoc)
	 * @see java.util.Map.Entry#setValue(V)
	 */
	public FragmentSet setValue(FragmentSet arg0) {
			value = arg0;
		return arg0;
	}

}
