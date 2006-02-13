package de.saar.chorus.domgraph.chart;

import java.util.Set;


class AgendaEntry { 
	
    String key;
	Set<String> value;
	
	AgendaEntry(String source, Set<String> target) {
		key = source;
		value = target;
	}
	
	
	/* (non-Javadoc)
	 * @see java.util.Map.Entry#getKey()
	 */
	public String getDominator() {
		// TODO Auto-generated method stub
		return key;
	}

	/* (non-Javadoc)
	 * @see java.util.Map.Entry#getValue()
	 */
	public Set<String> getFragmentSet() {
		// TODO Auto-generated method stub
		return value;
	}

    public String toString() {
        return "<Ag dom="+key+", fs=" + value + ">";
    }
}
