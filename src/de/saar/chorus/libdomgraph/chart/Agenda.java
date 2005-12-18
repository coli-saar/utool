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
	
		
	public void clear() {
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
		for( AgendaEntry entry : newEntries ) {
			agenda.push(entry);
		}
	}
} 
