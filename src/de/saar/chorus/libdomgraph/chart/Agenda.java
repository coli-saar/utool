package de.saar.chorus.libdomgraph.chart;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import de.saar.chorus.libdomgraph.FragmentSet;
import de.saar.chorus.libdomgraph.SWIGTYPE_p_Node;

public class Agenda {
	
	private Stack<AgendaEntry> agenda;
	
	Agenda() {
		agenda = new Stack<AgendaEntry>();
	}
	
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
	
	public void clearAgenda() {
		agenda.clear();
	}
	
	public void addEntry(AgendaEntry newEntry) {
		agenda.push(newEntry);
	}
	public Collection<AgendaEntry> getAll() {
		return agenda;
	}
	
	public AgendaEntry viewTopmostEntry() {
		return agenda.peek();
	}
	
	public AgendaEntry getAndRemoveNext() {
		return agenda.pop();
	}
	
	public boolean isEmpty() {
		return agenda.isEmpty();
	}
	
	public int size() {
		return agenda.size();
	}
	
	public void addAll(Agenda second) {
		addAll(second.getAll());
	}
	
	public void addAll(Collection<AgendaEntry> newEntries) {
		agenda.addAll(newEntries);
	}
} 
