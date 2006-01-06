package de.saar.chorus.libdomgraph.chart;

import java.util.Map;

import de.saar.chorus.libdomgraph.DomEdgePair;
import de.saar.chorus.libdomgraph.SWIGTYPE_p_Node;

public class JDomEdge implements Map.Entry<SWIGTYPE_p_Node, SWIGTYPE_p_Node> {

	private SWIGTYPE_p_Node key;
	private SWIGTYPE_p_Node value;
	
	JDomEdge(SWIGTYPE_p_Node source, SWIGTYPE_p_Node target) {
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
	
	public DomEdgePair toDomEdgePair() {
		return new DomEdgePair(key, value);
	}

	/* (non-Javadoc)
	 * @see java.util.Map.Entry#getValue()
	 */
	public SWIGTYPE_p_Node getValue() {
		// TODO Auto-generated method stub
		return value;
	}

	/* (non-Javadoc)
	 * @see java.util.Map.Entry#setValue(V)
	 */
	public SWIGTYPE_p_Node setValue(SWIGTYPE_p_Node arg0) {
		value =  arg0;
		return arg0;
	}

}
