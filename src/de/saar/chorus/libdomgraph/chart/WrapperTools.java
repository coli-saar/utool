package de.saar.chorus.libdomgraph.chart;

import java.util.ArrayList;
import java.util.List;

import de.saar.chorus.libdomgraph.EdgeVector;
import de.saar.chorus.libdomgraph.FragmentSet;
import de.saar.chorus.libdomgraph.FragmentSetVector;
import de.saar.chorus.libdomgraph.NodeVector;
import de.saar.chorus.libdomgraph.SWIGTYPE_p_Edge;
import de.saar.chorus.libdomgraph.SWIGTYPE_p_Node;
import de.saar.chorus.libdomgraph.Split;
import de.saar.chorus.libdomgraph.SplitVector;

public class WrapperTools {

	public static List<Split> vectorToList(SplitVector spv) {
		
		List<Split> toReturn = new ArrayList<Split>();
		
		if( spv != null ) {
			for( int i = 0; i < spv.size(); i++ ) {
				toReturn.add(spv.get(i));
			}
		}
		return toReturn;
	}
	
public static List<FragmentSet> vectorToList(FragmentSetVector spv) {
		
		List<FragmentSet> toReturn = new ArrayList<FragmentSet>();
		
		if( spv != null ) {
			for( int i = 0; i < spv.size(); i++ ) {
				toReturn.add(spv.get(i));
			}
		}
		return toReturn;
	}

/**
 * Converts a List of FragmentSets to a FragmentSetVector
 * Please don't use me if it is avoidable...
 * 
 * @param spv
 * @return
 */
public static FragmentSetVector listToVector(List<FragmentSet> spv) {
	
	FragmentSetVector toReturn = new FragmentSetVector();
	
	if( spv != null ) {
		for( int i = 0; i < spv.size(); i++ ) {
			toReturn.add(spv.get(i));
		}
	}
	return toReturn;
}



public static List<SWIGTYPE_p_Node> vectorToList(NodeVector spv) {
	
	List<SWIGTYPE_p_Node> toReturn = new ArrayList<SWIGTYPE_p_Node>();
	
	if( spv != null ) {
		for( int i = 0; i < spv.size(); i++ ) {
			toReturn.add(spv.get(i));
		}
	}
	
	return toReturn;
}

public static List<SWIGTYPE_p_Edge> vectorToList(EdgeVector spv) {
	
	List<SWIGTYPE_p_Edge> toReturn = new ArrayList<SWIGTYPE_p_Edge>();
	
	if( spv != null ) {
		for( int i = 0; i < spv.size(); i++ ) {
			toReturn.add(spv.get(i));
		}
	}
	
	return toReturn;
}

}
