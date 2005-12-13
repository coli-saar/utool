package de.saar.chorus.libdomgraph.chart;

import java.util.HashSet;
import java.util.Set;

import de.saar.chorus.libdomgraph.FragmentSet;
import de.saar.chorus.libdomgraph.SWIGTYPE_p_Node;

public class Agenda {
	private static Set<DomEdge> domEdges = new HashSet<DomEdge>();
	private static Set<AgendaEntry> agendaEntries = new HashSet<AgendaEntry>();
	
	
	public static DomEdge makeDomEdge (SWIGTYPE_p_Node source, 
									   SWIGTYPE_p_Node target) {
		
		DomEdge ret = new DomEdge(source, target);
		if( ! domEdges.contains(ret) )
			domEdges.add(ret);
		return ret;
	}
	
	public static void clearDomEdges() {
		domEdges.clear();
	}
	
	
	public static AgendaEntry makeAgendaEntry(SWIGTYPE_p_Node source,
											  FragmentSet target)  {
		AgendaEntry ret = new AgendaEntry(source, target);
		
		if( ! agendaEntries.contains(ret) )
			agendaEntries.add(ret);
		
		return ret;
	}
	
	public static void deleteAgendaEntries() {
		agendaEntries.clear();
	}
} 
